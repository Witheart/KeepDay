package com.example.keepday.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 系统日历里的每日打卡提醒。 */
data class CalendarReminderSetting(
    val hour: Int = 20,
    val minute: Int = 0,
    val created: Boolean = false
) {
    /** 形如 "20:00"。 */
    val label: String
        get() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

object SettingsStore {

    private const val PREFS_NAME = "keepday_settings"
    private const val KEY_PENDING_FIRST = "sort_pending_first"
    private const val KEY_CAL_HOUR = "calendar_reminder_hour"
    private const val KEY_CAL_MINUTE = "calendar_reminder_minute"
    private const val KEY_CAL_CREATED = "calendar_reminder_created"
    private const val KEY_THEME_MODE = "theme_mode"

    private var prefs: SharedPreferences? = null

    /** 是否把今天还没打卡的习惯排在前面。 */
    private val _pendingFirst = MutableStateFlow(true)
    val pendingFirst: StateFlow<Boolean> = _pendingFirst.asStateFlow()

    private val _calendarReminder = MutableStateFlow(CalendarReminderSetting())
    val calendarReminder: StateFlow<CalendarReminderSetting> = _calendarReminder.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = storage
        _pendingFirst.value = storage.getBoolean(KEY_PENDING_FIRST, true)
        _themeMode.value = ThemeMode.fromName(storage.getString(KEY_THEME_MODE, null))
        _calendarReminder.value = CalendarReminderSetting(
            hour = storage.getInt(KEY_CAL_HOUR, 20),
            minute = storage.getInt(KEY_CAL_MINUTE, 0),
            created = storage.getBoolean(KEY_CAL_CREATED, false)
        )
    }

    fun updateThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun updatePendingFirst(enabled: Boolean) {
        _pendingFirst.value = enabled
        prefs?.edit()?.putBoolean(KEY_PENDING_FIRST, enabled)?.apply()
    }

    fun updateCalendarReminder(hour: Int, minute: Int, created: Boolean) {
        val setting = CalendarReminderSetting(
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            created = created
        )
        _calendarReminder.value = setting
        prefs?.edit()
            ?.putInt(KEY_CAL_HOUR, setting.hour)
            ?.putInt(KEY_CAL_MINUTE, setting.minute)
            ?.putBoolean(KEY_CAL_CREATED, setting.created)
            ?.apply()
    }
}
