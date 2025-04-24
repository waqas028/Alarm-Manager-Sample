package com.waqas028.alarm_manager_sample

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import com.waqas028.alarm_manager_sample.alarm_triger.AlarmTriggerActivity
import com.waqas028.alarm_manager_sample.utils.AlarmType
import com.waqas028.alarm_manager_sample.utils.alarmTypeDetails
import com.waqas028.alarm_manager_sample.utils.showConfirmation
import com.waqas028.alarm_manager_sample.utils.showTimePicker
import com.waqas028.alarm_manager_sample.utils.timeToString
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSchedulerScreen() {
    val context = LocalContext.current
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var showRepeatOptions by remember { mutableStateOf(false) }
    var selectedAlarmType by remember { mutableStateOf<AlarmType?>(null) }
    var scheduledTime by remember { mutableStateOf<Calendar?>(null) }
    var repeatOption by remember { mutableStateOf(RepeatOption.ONCE) }
    var selectedDays by remember { mutableStateOf<List<Int>>(emptyList()) }
    var hasActiveAlarm by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e("AlarmManager", "Exact alarms are not allowed. Prompting user.")
            requestExactAlarmPermission(context)
        }
    }

    val alarmTypes = listOf(
        AlarmType(
            id = 1,
            name = "SET",
            description = "Basic alarm that may be delayed by system optimizations",
            method = { time, pendingIntent, _, _ ->
                alarmManager.set(AlarmManager.RTC_WAKEUP, time.timeInMillis, pendingIntent)
            }
        ),
        AlarmType(
            id = 2,
            name = "SET_EXACT",
            description = "More precise alarm, but may still be delayed in Doze mode",
            method = { time, pendingIntent, _, _ ->
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time.timeInMillis, pendingIntent)
            }
        ),
        AlarmType(
            id = 3,
            name = "SET_EXACT_ALLOW_IDLE",
            description = "Most precise alarm, works even in Doze mode (requires permission)",
            method = { time, pendingIntent, _, _ ->
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    time.timeInMillis,
                    pendingIntent
                )
            }
        ),
        AlarmType(
            id = 4,
            name = "SET_REPEATING",
            description = "Repeats at fixed intervals (not precise, inexact after API 19)",
            supportsRepeating = true,
            method = { time, pendingIntent, repeat, days ->
                when (repeat) {
                    RepeatOption.DAILY -> {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            time.timeInMillis,
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                        )
                    }
                    RepeatOption.WEEKLY -> {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            time.timeInMillis,
                            AlarmManager.INTERVAL_DAY * 7,
                            pendingIntent
                        )
                    }
                    RepeatOption.CUSTOM -> {
                        days.forEach { dayIndex ->
                            val alarmTime = time.clone() as Calendar
                            // Convert day index to Calendar.DAY_OF_WEEK (Sunday=1, Saturday=7)
                            val dayOfWeek = when (dayIndex) {
                                0 -> Calendar.SUNDAY    // Sun
                                1 -> Calendar.MONDAY    // Mon
                                2 -> Calendar.TUESDAY   // Tue
                                3 -> Calendar.WEDNESDAY // Wed
                                4 -> Calendar.THURSDAY // Thu
                                5 -> Calendar.FRIDAY    // Fri
                                6 -> Calendar.SATURDAY  // Sat
                                else -> Calendar.SUNDAY
                            }

                            // Set to next occurrence of this day
                            alarmTime.set(Calendar.DAY_OF_WEEK, dayOfWeek)
                            if (alarmTime.before(Calendar.getInstance())) {
                                alarmTime.add(Calendar.DAY_OF_YEAR, 7)
                            }

                            // Set exact alarm for each custom day
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.timeInMillis,
                                pendingIntent
                            )
                        }
                    }
                    else -> { // Once
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            time.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            }
        ),
        AlarmType(
            id = 5,
            name = "SET_WINDOW",
            description = "Alarm delivery within a time window (API 19+)",
            method = { time, pendingIntent, _, _ ->
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    time.timeInMillis,
                    60000, // 1 minute window
                    pendingIntent
                )
            }
        ),
        AlarmType(
            id = 6,
            name = "SET_ALLOW_IDLE",
            description = "Alarm that will be allowed to trigger even in idle mode",
            method = { time, pendingIntent, _, _ ->
                AlarmManagerCompat.setAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    time.timeInMillis,
                    pendingIntent
                )
            }
        ),
        AlarmType(
            id = 7,
            name = "SET_ALARM_CLOCK",
            description = "Shows in alarm clock UI and triggers even in Doze (API 21+)",
            method = { time, pendingIntent, _, _ ->
                val info = AlarmManager.AlarmClockInfo(
                    time.timeInMillis,
                    pendingIntent
                )
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        )
    )

    // Listen for alarm triggers to reset active alarm state
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "ALARM_TRIGGERED") {
                    hasActiveAlarm = false
                    selectedAlarmType = null
                    scheduledTime = null
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("ALARM_TRIGGERED"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Alarm Scheduler") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Select Alarm Type:",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(alarmTypes) { alarmType ->
                    AlarmTypeCard(
                        alarmType = alarmType,
                        enabled = !hasActiveAlarm,
                        onClick = {
                            selectedAlarmType = alarmType
                            showTimePicker(context) { hour, minute ->
                                val calendar = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, hour)
                                    set(Calendar.MINUTE, minute)
                                    // Clear seconds and milliseconds for exact timing
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                scheduledTime = calendar
                                if (alarmType.supportsRepeating) {
                                    showRepeatOptions = true
                                } else {
                                    // Directly schedule non-repeating alarms
                                    val pendingIntent = AlarmScheduler.createPendingIntent(context, alarmType)
                                    alarmType.method(calendar, pendingIntent, repeatOption, selectedDays)

                                    hasActiveAlarm = true
                                    showConfirmation(context, "${alarmType.name} alarm set for ${timeToString(calendar)}")
                                }
                            }
                        }
                    )
                }
            }

            scheduledTime?.let { time ->
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Scheduled: ${time.time}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Type: ${selectedAlarmType?.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "Repeat: ${
                            when (repeatOption) {
                                RepeatOption.ONCE -> "Once"
                                RepeatOption.DAILY -> "Daily"
                                RepeatOption.WEEKLY -> "Weekly"
                                RepeatOption.CUSTOM -> "Custom days: ${
                                    selectedDays.joinToString {
                                        when (it) {
                                            0 -> "Sun"; 1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"
                                            4 -> "Thu"; 5 -> "Fri"; else -> "Sat"
                                        }
                                    }
                                }"
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                AlarmScheduler.cancelAlarm(context, alarmManager, selectedAlarmType)
                                hasActiveAlarm = false
                                scheduledTime = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Cancel Alarm")
                        }
                    }
                }
            }
        }
    }

    if (showRepeatOptions) {
        RepeatOptionsDialog(
            onDismiss = { showRepeatOptions = false },
            onConfirm = { option, days ->
                repeatOption = option
                selectedDays = days
                showRepeatOptions = false

                // Schedule the alarm
                selectedAlarmType?.let { alarmType ->
                    val pendingIntent = AlarmScheduler.createPendingIntent(context, alarmType)

                    if (repeatOption == RepeatOption.CUSTOM) {
                        // Handle custom days
                        days.forEach { dayOfWeek ->
                            val alarmTime = scheduledTime!!.clone() as Calendar
                            alarmTime.set(Calendar.DAY_OF_WEEK, dayOfWeek + 1) // Calendar.SUNDAY = 1
                            if (alarmTime.before(Calendar.getInstance())) {
                                alarmTime.add(Calendar.DAY_OF_YEAR, 7)
                            }
                            // Clear seconds and milliseconds
                            alarmTime.set(Calendar.SECOND, 0)
                            alarmTime.set(Calendar.MILLISECOND, 0)
                            alarmType.method(alarmTime, pendingIntent, repeatOption, selectedDays)
                        }
                    } else {
                        // Clear seconds and milliseconds
                        scheduledTime!!.set(Calendar.SECOND, 0)
                        scheduledTime!!.set(Calendar.MILLISECOND, 0)
                        alarmType.method(scheduledTime!!, pendingIntent, repeatOption, selectedDays)
                    }

                    hasActiveAlarm = true
                    showConfirmation(context, "${alarmType.name} alarm set for ${timeToString(scheduledTime!!)}")
                }
            }
        )
    }
}

@Composable
private fun AlarmTypeCard(
    alarmType: AlarmType,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val details = alarmTypeDetails[alarmType.name]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { if (enabled) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarmType.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alarmType.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded && details != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    InfoRow("Type", details.type)
                    InfoRow("Trigger", details.trigger)
                    InfoRow("Doze Mode", details.doze)
                    if (details.limitations.isNotEmpty()) InfoRow("limitations", details.limitations)
                    InfoRow("Lock Screen", details.lockScreen)
                    InfoRow("Use When", details.useWhen)
                }
            }

            if (!enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "An alarm is already scheduled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 100.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (RepeatOption, List<Int>) -> Unit
) {
    var selectedOption by remember { mutableStateOf(RepeatOption.ONCE) }
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedDays = remember { mutableStateListOf<Int>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Repeat Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                RepeatOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedOption = option
                                if (option != RepeatOption.CUSTOM) {
                                    selectedDays.clear()
                                }
                            }
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = {
                                selectedOption = option
                                if (option != RepeatOption.CUSTOM) {
                                    selectedDays.clear()
                                }
                            }
                        )
                        Text(
                            text = when (option) {
                                RepeatOption.ONCE -> "Once"
                                RepeatOption.DAILY -> "Daily"
                                RepeatOption.WEEKLY -> "Weekly"
                                RepeatOption.CUSTOM -> "Custom days"
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (selectedOption == RepeatOption.CUSTOM) {
                    Text(
                        "Select days:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            val isSelected = selectedDays.contains(index)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedDays.remove(index)
                                    } else {
                                        selectedDays.add(index)
                                    }
                                },
                                label = { Text(day) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedOption, selectedDays) },
                        enabled = selectedOption != RepeatOption.CUSTOM || selectedDays.isNotEmpty()
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

enum class RepeatOption {
    ONCE, DAILY, WEEKLY, CUSTOM
}