package com.example.keepday.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.keepday.data.ReminderSetting
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 用 AlarmManager 每天触发一次提醒。
 * 采用 setInexactRepeating，不需要精确闹钟权限，系统会做少量合并，
 * 对"每天提醒打卡"这种场景足够。
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "com.example.keepday.action.REMINDER"
    private const val REQUEST_CODE = 1001

    fun apply(context: Context, setting: ReminderSetting) {
        if (setting.enabled) {
            schedule(context, setting.hour, setting.minute)
        } else {
            cancel(context)
        }
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerMillis(hour, minute)
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_DAY,
                alarmIntent(context)
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarmManager.cancel(alarmIntent(context)) }
    }

    private fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 今天该时间点若已过去，则从明天开始。 */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
