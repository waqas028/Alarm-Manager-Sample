package com.waqas028.alarm_manager_sample

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.waqas028.alarm_manager_sample.utils.AlarmType

object AlarmScheduler {
    fun cancelAlarm(context: Context, alarmManager: AlarmManager, alarmType: AlarmType?) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ALARM_ACTION_${alarmType?.id}"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmType?.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun createPendingIntent(context: Context, alarmType: AlarmType): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ALARM_ACTION_${alarmType.id}"
            putExtra("ALARM_TYPE", alarmType.name)
            putExtra("ALARM_NAME", "Scheduled Alarm")
        }

        return PendingIntent.getBroadcast(
            context,
            alarmType.id.hashCode(), // Unique request code based on type
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
        }
        context.startActivity(intent)
    }
}