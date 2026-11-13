package com.example.keepday.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 每日提醒设置。 */
data class ReminderSetting(
    val enabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0
) {
    /** 形如 "21:00"。 */
    val label: String
        get() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

object SettingsStore {

    private const val PREFS_NAME = "keepday_settings"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val KEY_PENDING_FIRST = "sort_pending_first"

    private var prefs: SharedPreferences? = null

    private val _reminder = MutableStateFlow(ReminderSetting())
    val reminder: StateFlow<ReminderSetting> = _reminder.asStateFlow()

    /** 是否把今天还没打卡的习惯排在前面。 */
    private val _pendingFirst = MutableStateFlow(true)
    val pendingFirst: StateFlow<Boolean> = _pendingFirst.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = storage
        _reminder.value = ReminderSetting(
            enabled = storage.getBoolean(KEY_ENABLED, false),
            hour = storage.getInt(KEY_HOUR, 21),
            minute = storage.getInt(KEY_MINUTE, 0)
        )
        _pendingFirst.value = storage.getBoolean(KEY_PENDING_FIRST, true)
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        val setting = ReminderSetting(enabled, hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        _reminder.value = setting
        prefs?.edit()
            ?.putBoolean(KEY_ENABLED, setting.enabled)
            ?.putInt(KEY_HOUR, setting.hour)
            ?.putInt(KEY_MINUTE, setting.minute)
            ?.apply()
    }

    fun updatePendingFirst(enabled: Boolean) {
        _pendingFirst.value = enabled
        prefs?.edit()?.putBoolean(KEY_PENDING_FIRST, enabled)?.apply()
    }
}
