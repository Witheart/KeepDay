package com.example.keepday.data

import com.example.keepday.data.weight.WeightStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * 备份文件格式（JSON）：
 * {
 *   "app": "KeepDay", "schema": 1, "exportedAt": 时间戳,
 *   "pendingFirst": true,
 *   "calendarReminder": { "hour": 20, "minute": 0, "created": true },
 *   "habits": [ ... ]
 * }
 */
object BackupManager {

    private const val APP_TAG = "KeepDay"
    private const val SCHEMA_VERSION = 1

    data class ImportResult(
        val success: Boolean,
        val habitCount: Int = 0,
        val message: String? = null
    )

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("schema", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("pendingFirst", SettingsStore.pendingFirst.value)
        root.put("themeMode", SettingsStore.themeMode.value.name)
        val calendar = SettingsStore.calendarReminder.value
        root.put(
            "calendarReminder",
            JSONObject().apply {
                put("hour", calendar.hour)
                put("minute", calendar.minute)
                put("created", calendar.created)
            }
        )
        root.put("habits", JSONArray(HabitStore.exportHabitsJson()))
        root.put("weight", JSONObject(WeightStore.exportJson()))
        return root.toString(2)
    }

    fun importJson(raw: String): ImportResult {
        val root = runCatching { JSONObject(raw) }.getOrNull()
            ?: return ImportResult(false, message = "备份内容不是有效的 JSON")
        if (root.optString("app") != APP_TAG) {
            return ImportResult(false, message = "这不是 KeepDay 的备份文件")
        }
        val habits = root.optJSONArray("habits")
            ?: return ImportResult(false, message = "备份里没有习惯数据")
        if (!HabitStore.importHabitsJson(habits.toString())) {
            return ImportResult(false, message = "习惯数据解析失败")
        }
        root.optJSONObject("weight")?.let { weight ->
            WeightStore.importJson(weight.toString())
        }
        if (root.has("pendingFirst")) {
            SettingsStore.updatePendingFirst(root.optBoolean("pendingFirst", true))
        }
        if (root.has("themeMode")) {
            SettingsStore.updateThemeMode(ThemeMode.fromName(root.optString("themeMode")))
        }
        root.optJSONObject("calendarReminder")?.let { calendar ->
            SettingsStore.updateCalendarReminder(
                hour = calendar.optInt("hour", 20),
                minute = calendar.optInt("minute", 0),
                created = calendar.optBoolean("created", false)
            )
        }
        return ImportResult(true, habits.length())
    }

    /** 预览备份内容里的习惯数量，用于恢复前的确认提示。 */
    fun peekHabitCount(raw: String): Int =
        runCatching { JSONObject(raw).optJSONArray("habits")?.length() ?: 0 }.getOrDefault(0)
}
