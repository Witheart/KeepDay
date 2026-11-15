package com.example.keepday.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * 把打卡提醒交给系统日历：
 * 写入一条「每天重复」的事件并附带 0 分钟提醒，由系统日历应用负责到点通知。
 * 好处是 KeepDay 被划掉、被清理甚至卸载，提醒依然生效。
 */
object CalendarReminder {

    private const val TAG = "CalendarReminder"
    private const val TITLE = "KeepDay 打卡提醒"
    private const val MARKER = "keepday_daily_reminder"
    private const val DURATION_MINUTES = 15L

    private val PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    fun permissions(): Array<String> = PERMISSIONS

    fun hasPermission(context: Context): Boolean = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** 已存在的提醒事件 id，没有则返回 null。 */
    fun existingEventId(context: Context): Long? {
        if (!hasPermission(context)) return null
        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                "${CalendarContract.Events.DESCRIPTION} = ?",
                arrayOf(MARKER),
                null
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
        } catch (t: Throwable) {
            Log.e(TAG, "query failed", t)
            null
        }
    }

    /** 创建或更新每日提醒事件。 */
    fun createOrUpdate(context: Context, hour: Int, minute: Int): Boolean {
        if (!hasPermission(context)) return false
        val calendarId = writableCalendarId(context)
        if (calendarId == null) {
            Log.w(TAG, "no writable calendar")
            return false
        }
        deleteExisting(context)

        val start = LocalDate.now()
            .atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val end = start + DURATION_MINUTES * 60_000L

        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, TITLE)
                put(CalendarContract.Events.DESCRIPTION, MARKER)
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, end)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.RRULE, "FREQ=DAILY")
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.ALL_DAY, 0)
            }
            val uri = context.contentResolver.insert(
                CalendarContract.Events.CONTENT_URI, values
            ) ?: return false
            val eventId = ContentUris.parseId(uri)

            val reminder = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 0)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "insert failed", t)
            false
        }
    }

    fun deleteExisting(context: Context): Boolean {
        if (!hasPermission(context)) return false
        val id = existingEventId(context) ?: return false
        return try {
            context.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id),
                null,
                null
            ) > 0
        } catch (t: Throwable) {
            Log.e(TAG, "delete failed", t)
            false
        }
    }

    /**
     * 没有日历权限、或设备上没有可写日历时，退回系统日历的“新建事件”界面，
     * 由用户确认保存（预填标题、时间、每天重复）。
     */
    fun openInsertIntent(context: Context, hour: Int, minute: Int): Boolean {
        val start = LocalDate.now()
            .atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, TITLE)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + DURATION_MINUTES * 60_000L)
            putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
            putExtra(CalendarContract.Events.HAS_ALARM, 1)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "no calendar app", t)
            false
        }
    }

    /** 找第一个可写日历，优先本地日历，避免污染账号同步日历。 */
    private fun writableCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        return try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null
            )?.use { cursor ->
                var fallback: Long? = null
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val accountType = cursor.getString(2) ?: ""
                    if (accountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL, ignoreCase = true)) {
                        return id
                    }
                    if (fallback == null) fallback = id
                }
                fallback
            }
        } catch (t: Throwable) {
            Log.e(TAG, "calendar query failed", t)
            null
        }
    }
}
