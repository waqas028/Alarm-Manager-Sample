# Android AlarmManager Service Guide 🚀

This repository contains practical examples and guidance on how to use `AlarmManager` in Android. I'm currently working on building robust alarm handling using different types of alarms. This is both a learning project and a reference guide for anyone interested in managing scheduled tasks in Android efficiently.

## 📌 About AlarmManager

`AlarmManager` is a system service that allows you to schedule your app to run at a specific time or interval — even if the app is not currently running.

It can be used to:
- Schedule tasks like notifications or background work
- Trigger services or broadcasts at defined times
- Handle time-based actions in your app

---

## Alarm Recevier
```kotlin
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
```

## Alarm Scheduler
```kotlin
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

```

## 🔍 Alarm Types and Use-Cases

Here are the main types of alarms supported by `AlarmManager` and when to use each:

### ⏱ 1. `RTC` (Real Time Clock)
- **Use when:** You want to trigger something at an exact clock time.
- **Behavior:** Will not wake the device if it’s asleep.
- **Method:** `set()`, `setExact()`

```kotlin
alarmManager.set(
    AlarmManager.RTC,
    triggerTimeInMillis,
    pendingIntent
)
```

---

### ⏰ 2. `RTC_WAKEUP`
- **Use when:** Same as RTC, but you need the device to wake up if it’s asleep.
- **Behavior:** Wakes up the device.

```kotlin
alarmManager.set(
    AlarmManager.RTC_WAKEUP,
    triggerTimeInMillis,
    pendingIntent
)
```

---

### 💩 3. `ELAPSED_REALTIME`
- **Use when:** You want to schedule a task based on uptime (since boot), not wall clock.
- **Behavior:** Won’t wake the device.

```kotlin
alarmManager.set(
    AlarmManager.ELAPSED_REALTIME,
    SystemClock.elapsedRealtime() + delayInMillis,
    pendingIntent
)
```

---

### 💩⏰ 4. `ELAPSED_REALTIME_WAKEUP`
- **Use when:** Same as above, but should wake the device.

```kotlin
alarmManager.set(
    AlarmManager.ELAPSED_REALTIME_WAKEUP,
    SystemClock.elapsedRealtime() + delayInMillis,
    pendingIntent
)
```

---

## 🛡 Best Practices

- Prefer **`setExactAndAllowWhileIdle()`** if you need accuracy during Doze Mode.
- Avoid repeating alarms when exact timing is not required.
- Use **WorkManager** or **JobScheduler** if your task is network-bound or battery-sensitive.

---

## 📂 Features Being Handled

- Handling alarm setup and cancel
- Managing exact vs. inexact alarms
- Wakeup vs. non-wakeup alarms
- Persisting alarms across reboots
- Handling alarm triggering via BroadcastReceiver or ForegroundService

---

## 🧠 Learning in Progress

I'm actively learning and exploring different edge cases, including:
- Doze mode compatibility
- Repeating alarms with proper cancellation
- Working with AlarmManager across Android versions

---

## 🤝 Contributions Welcome!

This project is meant to be a community-driven resource.

If you have improvements, more use-cases, or want to help handle additional scenarios — feel free to contribute!

---

## 🌍 Let's Connect

[![Email](https://img.shields.io/badge/Email-waqaswaseem679@gmail.com-red?style=flat&logo=gmail)](mailto:waqaswaseem679@gmail.com)  
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?style=flat&logo=linkedin)](https://www.linkedin.com/in/muhammad-waqas-4399361a3)  
[![GitHub](https://img.shields.io/badge/GitHub-waqas028-black?style=flat&logo=github)](https://github.com/waqas028)  
[![Skype](https://img.shields.io/badge/Skype-live%3Awaqasyaqeen420-00aff0?style=flat&logo=skype)](https://join.skype.com/invite/p4ckdyAOrsCs)  
[![WhatsApp](https://img.shields.io/badge/WhatsApp-Chat-25D366?style=flat&logo=whatsapp)](https://wa.me/+923045593294)  
[![Google Developer](https://img.shields.io/badge/Google%20Developer-Profile-blue?style=flat&logo=google)](https://g.dev/MuhammadWaqasDev)
