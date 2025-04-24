package com.waqas028.alarm_manager_sample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.waqas028.alarm_manager_sample.alarm_triger.AlarmTriggerActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val action = intent.action
            if (action != null && action.startsWith("ALARM_ACTION_")){
                val alarmName = intent.getStringExtra("ALARM_NAME") ?: "Alarm"
                val alarmType = intent.getStringExtra("ALARM_TYPE") ?: "Alarm"

                if (alarmType.contains("SET_ALARM_CLOCK")){
                    val alarmIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("ALARM_TIME", System.currentTimeMillis())
                        putExtra("ALARM_TYPE", alarmType)
                    }

                    // Add FLAG_ACTIVITY_NO_USER_ACTION to prevent touch events during unlock
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    }

                    context.startActivity(alarmIntent)
                } else {
                    // Start the ringtone playing service
                    val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
                        putExtra("ALARM_NAME", alarmName)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
                Log.d("AlarmReceiver", "Alarm triggered at " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                    Date()
                ))
                context.sendBroadcast(Intent("ALARM_TRIGGERED").apply {
                    setPackage(context.packageName)
                })
            }
        }
    }
}

class AlarmRingtoneService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private var alarmName: String = ""

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL",
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // No default sound, as we play our own ringtone
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alarmName = intent?.getStringExtra("ALARM_NAME") ?: "Alarm Triggered"
        createNotificationChannel()
        val cancelIntent = Intent(this, CancelAlarmReceiver::class.java).apply {
            putExtra("ALARM_NAME", alarmName)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            alarmName.hashCode() + 9999,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", if (hour == 0) 12 else hour, minute, amPm)

        val notification = NotificationCompat.Builder(this, "ALARM_CHANNEL")
            .setContentTitle("Alarm Time: $formattedTime")
            .setContentText("It's time for $alarmName")
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.baseline_close_24, "Dismiss", cancelPendingIntent)
            .setDeleteIntent(cancelPendingIntent)  // Cancel on swipe
            .build()

        startForeground(1, notification)

        // Play ringtone
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        // Stop service after 30 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 30 * 1000)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AlarmChannel"
            val descriptionText = "Channel for Alarm Manager"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarm_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

//cancel alarm service
class CancelAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Stop the AlarmRingtoneService
        val serviceIntent = Intent(context, AlarmRingtoneService::class.java)
        context.stopService(serviceIntent)
    }
}