package com.example.keepday.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * 习惯数据的唯一入口：内存中维护一个 [StateFlow]，写入时同步落盘到 SharedPreferences。
 * 数据量很小（几十条习惯、几千条日期），JSON 足以胜任，无需引入数据库依赖。
 */
object HabitStore {

    private const val PREFS_NAME = "keepday_store"
    private const val KEY_HABITS = "habits"
    private const val KEY_VERSION = "schema_version"
    private const val SCHEMA_VERSION = 2

    private var prefs: SharedPreferences? = null

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = storage
        _habits.value = decode(storage.getString(KEY_HABITS, null))
    }

    fun find(id: String): Habit? = _habits.value.firstOrNull { it.id == id }

    /** @return 是否添加成功（名称为空或重名时返回 false）。 */
    fun add(name: String, emoji: String, accentIndex: Int, weekGoal: Int = 7): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_habits.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            emoji = emoji,
            accentIndex = accentIndex,
            weekGoal = weekGoal.coerceIn(1, 7)
        )
        persist(_habits.value + habit)
        return true
    }

    /** 编辑已有习惯，名称重复时返回 false。 */
    fun edit(id: String, name: String, emoji: String, accentIndex: Int, weekGoal: Int): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_habits.value.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) return false
        persist(
            _habits.value.map { habit ->
                if (habit.id == id) {
                    habit.copy(
                        name = trimmed,
                        emoji = emoji,
                        accentIndex = accentIndex,
                        weekGoal = weekGoal.coerceIn(1, 7)
                    )
                } else {
                    habit
                }
            }
        )
        return true
    }

    /** @return 被删除的习惯，便于界面做"撤销"。 */
    fun delete(id: String): Habit? {
        val removed = _habits.value.firstOrNull { it.id == id } ?: return null
        persist(_habits.value.filterNot { it.id == id })
        return removed
    }

    /** 撤销删除，按创建时间插回原位。 */
    fun restore(habit: Habit) {
        if (_habits.value.any { it.id == habit.id }) return
        val restored = (_habits.value + habit).sortedBy { it.createdAt }
        persist(restored)
    }

    fun toggle(id: String, date: LocalDate = LocalDate.now()) {
        // 不允许给未来补卡
        if (date.isAfter(LocalDate.now())) return
        val epochDay = date.toEpochDay()
        persist(
            _habits.value.map { habit ->
                if (habit.id != id) {
                    habit
                } else if (habit.days.contains(epochDay)) {
                    habit.copy(days = habit.days - epochDay)
                } else {
                    habit.copy(days = habit.days + epochDay)
                }
            }
        )
    }

    fun clearAll() {
        persist(emptyList())
    }

    private fun persist(habits: List<Habit>) {
        _habits.value = habits
        prefs?.edit()
            ?.putString(KEY_HABITS, encode(habits))
            ?.putInt(KEY_VERSION, SCHEMA_VERSION)
            ?.apply()
    }

    private fun encode(habits: List<Habit>): String {
        val array = JSONArray()
        habits.forEach { habit ->
            val item = JSONObject()
            item.put("id", habit.id)
            item.put("name", habit.name)
            item.put("emoji", habit.emoji)
            item.put("accentIndex", habit.accentIndex)
            item.put("weekGoal", habit.weekGoal)
            item.put("createdAt", habit.createdAt)
            val days = JSONArray()
            habit.days.sorted().forEach(days::put)
            item.put("days", days)
            array.put(item)
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<Habit> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val daysJson = item.optJSONArray("days") ?: JSONArray()
                val days = buildSet {
                    for (dayIndex in 0 until daysJson.length()) {
                        add(daysJson.getLong(dayIndex))
                    }
                }
                Habit(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    emoji = item.optString("emoji", "✅"),
                    accentIndex = item.optInt("accentIndex", 0),
                    weekGoal = item.optInt("weekGoal", 7),
                    createdAt = item.optLong("createdAt", 0L),
                    days = days
                )
            }.sortedBy { it.createdAt }
        }.getOrDefault(emptyList())
    }
}
