package com.example.keepday.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 一个需要坚持的习惯。
 *
 * @param weekGoal 每周目标打卡次数（1~7），7 表示每天都打卡。
 * @param strict 严格模式：必须每天打卡，漏打一天"连续天数"清零（打卡历史仍然保留）。
 * @param streakResetDay 断签发生的日期（epochDay）。只有真正漏打过才会被设置，
 *                       用来把断签之前的记录排除在连续天数之外；从未断签时为 null。
 * @param days 已打卡的日期，用 [LocalDate.toEpochDay] 存储，便于排序与比较。
 */
data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val accentIndex: Int,
    val weekGoal: Int = 7,
    val pinned: Boolean = false,
    val strict: Boolean = false,
    val streakResetDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val days: Set<Long> = emptySet()
) {
    fun isDone(date: LocalDate): Boolean = days.contains(date.toEpochDay())

    /** 累计打卡天数。 */
    val totalDays: Int get() = days.size

    /** 首次打卡日期，从未打卡则为 null。 */
    val firstDay: LocalDate? get() = days.minOrNull()?.let(LocalDate::ofEpochDay)

    /** 创建日期。 */
    val createdDate: LocalDate
        get() = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

    /** 实际生效的每周目标：严格模式下强制每天。 */
    val effectiveWeekGoal: Int get() = if (strict) 7 else weekGoal.coerceIn(1, 7)

    /**
     * 从 [today] 往回数连续打卡天数。
     * 今天还没打卡时从昨天开始数，这样早上打开应用不会看到"连续天数归零"。
     * 正常情况只看实际打卡记录，补卡也能接上；只有严格模式真的断过签，
     * 才会以 [streakResetDay] 为界，把断签之前的记录排除在外。
     */
    fun streakAt(today: LocalDate = LocalDate.now()): Int {
        val floor = streakResetDay
        var cursor = today
        if (!isDone(cursor)) cursor = cursor.minusDays(1)
        var streak = 0
        while (isDone(cursor) && (floor == null || cursor.toEpochDay() >= floor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** 当前连续打卡天数。 */
    val currentStreak: Int get() = streakAt()

    /** 历史最长连续打卡天数。 */
    val bestStreak: Int
        get() {
            if (days.isEmpty()) return 0
            val sorted = days.sorted()
            var best = 1
            var run = 1
            for (index in 1 until sorted.size) {
                run = if (sorted[index] == sorted[index - 1] + 1) run + 1 else 1
                if (run > best) best = run
            }
            return best
        }

    /** 最近 [count] 天（含今天）的打卡情况，索引 0 是最早的一天。 */
    fun recentDays(today: LocalDate = LocalDate.now(), count: Int = 7): List<Boolean> =
        (count - 1 downTo 0).map { offset -> isDone(today.minusDays(offset.toLong())) }

    /** 本周（周一起算）已打卡次数。 */
    fun weekDoneCount(today: LocalDate = LocalDate.now()): Int {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L..6L).count { isDone(monday.plusDays(it)) }
    }

    /** 本周目标完成进度，0f~1f。 */
    fun weekProgress(today: LocalDate = LocalDate.now()): Float =
        (weekDoneCount(today).toFloat() / effectiveWeekGoal).coerceIn(0f, 1f)

    /**
     * 严格模式下是否该清零：从生效日（未设置则用创建日）到今天之前，
     * 只要有任意一天没打卡就判定断签。
     */
    fun strictResetDue(today: LocalDate = LocalDate.now()): Boolean {
        if (!strict) return false
        val since = streakResetDay ?: createdDate.toEpochDay()
        val yesterday = today.minusDays(1).toEpochDay()
        if (since > yesterday) return false
        return (since..yesterday).any { day -> day !in days }
    }

    /** 指定月份里打过卡的"日"，用于月历热力图。 */
    fun monthDoneDays(month: YearMonth): Set<Int> =
        days.mapNotNull { epochDay ->
            val date = LocalDate.ofEpochDay(epochDay)
            if (YearMonth.from(date) == month) date.dayOfMonth else null
        }.toSet()

    /**
     * 最近 [window] 天的完成率，最早不早于创建日期，避免新习惯显示 0%。
     */
    fun completionRate(today: LocalDate = LocalDate.now(), window: Int = 30): Float {
        // 严格模式只清连续天数，历史打卡记录仍然有效，完成率按真实历史计算
        val anchor = createdDate
        val windowStart = today.minusDays(window.toLong() - 1)
        val start = if (anchor.isAfter(windowStart)) anchor else windowStart
        val span = ChronoUnit.DAYS.between(start, today).toInt() + 1
        if (span <= 0 || start.isAfter(today)) return 0f
        val hits = (0 until span).count { isDone(start.plusDays(it.toLong())) }
        return hits.toFloat() / span
    }
}
