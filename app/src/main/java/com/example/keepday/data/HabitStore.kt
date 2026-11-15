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
    private const val SCHEMA_VERSION = 3

    private var prefs: SharedPreferences? = null

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    /** 严格模式断签清零的提示队列，界面消费后清除。 */
    private val _resetNotices = MutableStateFlow<List<String>>(emptyList())
    val resetNotices: StateFlow<List<String>> = _resetNotices.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = storage
        _habits.value = decode(storage.getString(KEY_HABITS, null))
        refreshStrict()
    }

    fun find(id: String): Habit? = _habits.value.firstOrNull { it.id == id }

    /** @return 是否添加成功（名称为空或重名时返回 false）。 */
    fun add(
        name: String,
        emoji: String,
        accentIndex: Int,
        weekGoal: Int = 7,
        strict: Boolean = false
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_habits.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        val habit = Habit(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            emoji = emoji,
            accentIndex = accentIndex,
            weekGoal = weekGoal.coerceIn(1, 7),
            strict = strict,
            // 刚开启严格模式不算断签，只有真的漏打才会写入清零点
            streakResetDay = null
        )
        persist(_habits.value + habit)
        return true
    }

    /** 编辑已有习惯，名称重复时返回 false。 */
    fun edit(
        id: String,
        name: String,
        emoji: String,
        accentIndex: Int,
        weekGoal: Int,
        strict: Boolean
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (_habits.value.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) return false
        persist(
            _habits.value.map { habit ->
                if (habit.id != id) {
                    habit
                } else {
                    habit.copy(
                        name = trimmed,
                        emoji = emoji,
                        accentIndex = accentIndex,
                        weekGoal = weekGoal.coerceIn(1, 7),
                        strict = strict,
                        // 关闭时清除断签点；新开启时保持"未断签"，已有的连续天数不受影响
                        streakResetDay = if (strict && habit.strict) habit.streakResetDay else null
                    )
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

    /** 撤销删除，尽量插回原来的位置。 */
    fun restore(habit: Habit, index: Int = Int.MAX_VALUE) {
        if (_habits.value.any { it.id == habit.id }) return
        val target = _habits.value.toMutableList()
        target.add(index.coerceIn(0, target.size), habit)
        persist(target)
    }

    /** 置顶 / 取消置顶。 */
    fun togglePin(id: String) {
        persist(
            _habits.value.map { habit ->
                if (habit.id == id) habit.copy(pinned = !habit.pinned) else habit
            }
        )
    }

    /**
     * 在同组（置顶 / 非置顶）内上下移动一格。
     * @param offset -1 上移，+1 下移。
     */
    fun move(id: String, offset: Int) {
        val list = _habits.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return
        val pinned = list[from].pinned
        var to = from + offset
        // 只与同组的相邻项交换，避免一次移动跨过置顶分界线
        while (to in list.indices && list[to].pinned != pinned) {
            to += offset
        }
        if (to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        persist(list)
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
        _resetNotices.value = emptyList()
    }

    /**
     * 检查严格模式的习惯是否漏打：只要从计数起点到今天之前有一天没打卡，
     * 就把"连续天数"清零（把计数起点重置为今天），历史打卡记录保持不变。
     * 应用启动、回到前台、桌面组件刷新时都会调用，保证离开应用期间漏打也能被处理。
     */
    fun refreshStrict(today: LocalDate = LocalDate.now()) {
        val todayEpoch = today.toEpochDay()
        val resetNames = mutableListOf<String>()

        val updated = _habits.value.map { habit ->
            if (!habit.strictResetDue(today)) return@map habit
            resetNames += habit.name
            habit.copy(streakResetDay = todayEpoch)
        }

        if (resetNames.isNotEmpty()) {
            persist(updated)
            _resetNotices.value = _resetNotices.value + resetNames
        }
    }

    fun consumeResetNotices() {
        _resetNotices.value = emptyList()
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
            item.put("pinned", habit.pinned)
            item.put("strict", habit.strict)
            habit.streakResetDay?.let { item.put("streakResetDay", it) }
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
                    pinned = item.optBoolean("pinned", false),
                    strict = item.optBoolean("strict", false),
                    // 旧版本的 strictSince 是"生效日"而非"断签日"，升级时丢弃，
                    // 避免历史数据被误判成断签
                    streakResetDay = if (item.has("streakResetDay")) item.optLong("streakResetDay") else null,
                    createdAt = item.optLong("createdAt", 0L),
                    days = days
                )
            }
        }.getOrDefault(emptyList())
    }
}
