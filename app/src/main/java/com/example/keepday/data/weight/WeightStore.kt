package com.example.keepday.data.weight

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 体重记录的仓库。每天最多一条，规则与习惯打卡一致：
 * 关闭补录后只能记今天；严格模式下漏记一天会清零"连续记录天数"，但历史数据保留。
 */
object WeightStore {

    private const val PREFS_NAME = "keepday_weight"
    private const val KEY_DATA = "weight_data"

    private var prefs: SharedPreferences? = null

    private val _entries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val entries: StateFlow<List<WeightEntry>> = _entries.asStateFlow()

    private val _setting = MutableStateFlow(WeightSetting())
    val setting: StateFlow<WeightSetting> = _setting.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = storage
        decode(storage.getString(KEY_DATA, null))
        refreshStrict()
    }

    fun entryAt(date: LocalDate): WeightEntry? = WeightMath.entryAt(_entries.value, date)

    fun streak(today: LocalDate = LocalDate.now()): Int =
        WeightMath.streakAt(_entries.value, _setting.value, today)

    /**
     * 记录某天体重。已有记录会被覆盖。
     * @return false 表示日期不合法（未来日期，或已关闭补录时的历史日期）。
     */
    fun record(date: LocalDate, kg: Float): Boolean {
        if (kg <= 0f) return false
        if (!WeightMath.canRecord(date, _setting.value)) return false
        val day = date.toEpochDay()
        val updated = _entries.value.filterNot { it.epochDay == day } +
            WeightEntry(epochDay = day, kg = kg, recordedAt = System.currentTimeMillis())
        persistEntries(updated.sortedBy { it.epochDay })
        return true
    }

    fun remove(date: LocalDate): Boolean {
        val day = date.toEpochDay()
        if (_entries.value.none { it.epochDay == day }) return false
        persistEntries(_entries.value.filterNot { it.epochDay == day })
        return true
    }

    fun updateStrict(strict: Boolean) {
        val current = _setting.value
        persistSetting(
            current.copy(
                strict = strict,
                // 关闭严格模式时清掉断签起点，重新开启时从今天算
                streakResetDay = if (strict) current.streakResetDay else null
            )
        )
    }

    /** 关闭补录（单向，不可再开启）。 */
    fun disableBackfill() {
        val current = _setting.value
        if (!current.allowBackfill) return
        persistSetting(current.copy(allowBackfill = false))
    }

    fun updateGoal(kg: Float?) {
        persistSetting(_setting.value.copy(goalKg = kg))
    }

    /** 漏记清算：严格模式下断签只清零连续天数。 */
    fun refreshStrict(today: LocalDate = LocalDate.now()) {
        if (WeightMath.strictResetDue(_entries.value, _setting.value, today)) {
            persistSetting(_setting.value.copy(streakResetDay = today.toEpochDay()))
        }
    }

    fun exportJson(): String = encode()

    fun importJson(raw: String): Boolean {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        if (!root.has("entries")) return false
        decode(raw)
        save()
        return true
    }

    /* ----------------------------- 持久化 ----------------------------- */

    private fun persistEntries(entries: List<WeightEntry>) {
        _entries.value = entries
        save()
    }

    private fun persistSetting(setting: WeightSetting) {
        _setting.value = setting
        save()
    }

    private fun save() {
        prefs?.edit()?.putString(KEY_DATA, encode())?.apply()
    }

    private fun encode(): String {
        val root = JSONObject()
        val setting = _setting.value
        root.put(
            "setting",
            JSONObject().apply {
                put("strict", setting.strict)
                put("allowBackfill", setting.allowBackfill)
                setting.goalKg?.let { put("goalKg", it.toDouble()) }
                setting.streakResetDay?.let { put("streakResetDay", it) }
                put("createdDay", setting.createdDay)
            }
        )
        val array = JSONArray()
        _entries.value.sortedBy { it.epochDay }.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("day", entry.epochDay)
                    put("kg", entry.kg.toDouble())
                    put("at", entry.recordedAt)
                }
            )
        }
        root.put("entries", array)
        return root.toString()
    }

    private fun decode(raw: String?) {
        if (raw.isNullOrBlank()) return
        runCatching {
            val root = JSONObject(raw)
            val settingJson = root.optJSONObject("setting")
            if (settingJson != null) {
                _setting.value = WeightSetting(
                    strict = settingJson.optBoolean("strict", false),
                    allowBackfill = settingJson.optBoolean("allowBackfill", true),
                    goalKg = if (settingJson.has("goalKg")) {
                        settingJson.optDouble("goalKg").toFloat()
                    } else {
                        null
                    },
                    streakResetDay = if (settingJson.has("streakResetDay")) {
                        settingJson.optLong("streakResetDay")
                    } else {
                        null
                    },
                    createdDay = settingJson.optLong(
                        "createdDay",
                        LocalDate.now().toEpochDay()
                    )
                )
            }
            val array = root.optJSONArray("entries") ?: JSONArray()
            _entries.value = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                WeightEntry(
                    epochDay = item.getLong("day"),
                    kg = item.getDouble("kg").toFloat(),
                    recordedAt = item.optLong("at", 0L)
                )
            }.sortedBy { it.epochDay }
        }
    }
}
