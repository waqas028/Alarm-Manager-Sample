package com.waqas028.alarm_manager_sample.alarm_triger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waqas028.alarm_manager_sample.AlarmReceiver
import com.waqas028.alarm_manager_sample.R
import com.waqas028.alarm_manager_sample.requestExactAlarmPermission
import com.waqas028.alarm_manager_sample.ui.theme.AlarmManagerSampleTheme
import java.util.Calendar
import java.util.Locale

class AlarmTriggerActivity : ComponentActivity() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private val viewModel: AlarmViewModel by viewModels()
    private var vibrator: Vibrator? = null
    private var vibrationPattern: LongArray = longArrayOf(1000, 1000) // 1s on, 1s off

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "PrayerAlarm::AlarmLock"
        ).apply {
            acquire(10 * 60 * 1000L /*10 minutes*/)
        }

        // Start alarm sound and vibration
        startAlarmSound()
        startVibration()

        setContent {
            AlarmManagerSampleTheme {
                AlarmTriggerUI(
                    viewModel = viewModel,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm(this) }
                )
            }
        }
    }

    private fun startAlarmSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.azan).apply {
            isLooping = true
            setVolume(1.0f, 1.0f)
            setOnPreparedListener { start() }
        }
        viewModel.mediaPlayer = mediaPlayer
    }

    private fun snoozeAlarm(context: Context) {
        viewModel.fadeOutAlarm()
        stopVibration()
        rescheduleAlarm(context)
        finish()
    }

    private fun startVibration() {
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    vibrationPattern,
                    0 // Repeat indefinitely
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(vibrationPattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun rescheduleAlarm(context: Context) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "ALARM_ACTION_"
            putExtra("ALARM_TYPE", "SET_ALARM_CLOCK_SNOOZE")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmManager", "Exact alarms are not allowed. Prompting user.")
                requestExactAlarmPermission(context)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(
            calendar.timeInMillis,
            pendingIntent
        )
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun dismissAlarm() {
        viewModel.fadeOutAlarm()
        stopVibration()
        finish()
    }

    override fun onPause() {
        super.onPause()
        wakeLock.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopAlarm()
        stopVibration()
    }
}


@Composable
fun AlarmTriggerUI(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    viewModel: AlarmViewModel
) {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var volume by remember { mutableFloatStateOf(currentVolume.toFloat() / maxVolume) }

    LaunchedEffect(Unit) {
        viewModel.mediaPlayer?.setVolume(volume, volume)
    }

    LaunchedEffect(volume) {
        val newVolume = (volume * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        viewModel.mediaPlayer?.setVolume(volume, volume)
    }

    // Get time using java.util.Calendar for pre-API 26 compatibility
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
    }
    val formattedTime = remember {
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        String.format(Locale.getDefault(), "%02d:%02d %s", if (hour == 0) 12 else hour, minute, amPm)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Time Display
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alarm Time",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Volume Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "0%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..1f
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Dismiss")
                }

                Button(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text("Snooze (5 min)")
                }
            }
        }
    }
}