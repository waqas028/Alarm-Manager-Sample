package com.waqas028.alarm_manager_sample.utils

import android.app.PendingIntent
import android.content.Context
import android.widget.Toast
import com.waqas028.alarm_manager_sample.RepeatOption
import java.util.Calendar
import java.util.Locale

fun showConfirmation(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

fun showTimePicker(
    context: Context,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    android.app.TimePickerDialog(
        context,
        { _, hour, minute ->
            onTimeSelected(hour, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

fun timeToString(calendar: Calendar): String {
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}

data class AlarmType(
    val id: Int,
    val name: String,
    val description: String,
    val supportsRepeating: Boolean = false,
    val method: (Calendar, PendingIntent, RepeatOption, List<Int>) -> Unit
)

data class AlarmTypeInfo(
    val type: String,
    val trigger: String,
    val doze: String,
    val limitations: String = "",
    val lockScreen: String,
    val useWhen: String
)

val alarmTypeDetails: Map<String, AlarmTypeInfo> = mapOf(
    "SET" to AlarmTypeInfo(
        type = "Basic alarm",
        trigger = "May be delayed by system optimizations",
        doze = "❌ Might not fire in Doze",
        lockScreen = "❌ No lock screen visibility",
        useWhen = "Low-priority background reminders"
    ),
    "SET_EXACT" to AlarmTypeInfo(
        type = "Precise alarm",
        trigger = "Fires at exact time (except in Doze)",
        doze = "❌ Might be deferred in Doze",
        lockScreen = "❌ Not visible on lock screen",
        useWhen = "Alarms that must be on-time outside Doze"
    ),
    "SET_EXACT_ALLOW_IDLE" to AlarmTypeInfo(
        type = "Most precise alarm",
        trigger = "Triggers exactly, even in Doze",
        doze = "✅ Bypasses Doze",
        lockScreen = "❌ Not visible on lock screen",
        useWhen = "Critical one-time alarms"
    ),
    "SET_REPEATING" to AlarmTypeInfo(
        type = "Repeating alarm",
        trigger = "May be inexact after API 19",
        doze = "❌ Likely deferred",
        limitations = "Interval can't be too short (min ~15 min from Android KitKat+).",
        lockScreen = "❌ Not visible",
        useWhen = "Repeating tasks like daily syncs"
    ),
    "SET_WINDOW" to AlarmTypeInfo(
        type = "Windowed alarm",
        trigger = "Triggers within a flexible window",
        doze = "❌ Might be delayed",
        lockScreen = "❌ Not visible",
        useWhen = "Non-critical alarms with time flexibility"
    ),
    "SET_ALLOW_IDLE" to AlarmTypeInfo(
        type = "Idle-allowed alarm",
        trigger = "May trigger in idle",
        doze = "✅ Allowed in Doze",
        lockScreen = "❌ Not visible",
        useWhen = "Time-sensitive alarms in idle mode"
    ),
    "SET_ALARM_CLOCK" to AlarmTypeInfo(
        type = "User-visible alarm",
        trigger = "Fires exactly, even in Doze",
        doze = "✅ Bypasses Doze",
        lockScreen = "✅ Shows on lock screen",
        useWhen = "Clock, prayer, or critical user alarms"
    )
)