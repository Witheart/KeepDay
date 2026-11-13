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
 * @param days 已打卡的日期，用 [LocalDate.toEpochDay] 存储，便于排序与比较。
 */
data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val accentIndex: Int,
    val weekGoal: Int = 7,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val days: Set<Long> = emptySet()
) {
    fun isDone(date: LocalDate): Boolean = days.contains(date.toEpochDay())

    /** 累计打卡天数。 */
    val totalDays: Int get() = days.size

    /** 第一次打卡的日期，从未打卡则为 null。 */
    val firstDay: LocalDate? get() = days.minOrNull()?.let(LocalDate::ofEpochDay)

    /**
     * 当前连续打卡天数。今天还没打卡时，从昨天开始往前数，
     * 这样早上打开应用不会看到"连续天数归零"。
     */
    val currentStreak: Int
        get() {
            var cursor = LocalDate.now()
            if (!isDone(cursor)) cursor = cursor.minusDays(1)
            var streak = 0
            while (isDone(cursor)) {
                streak++
                cursor = cursor.minusDays(1)
            }
            return streak
        }

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
        (weekDoneCount(today).toFloat() / weekGoal.coerceIn(1, 7)).coerceIn(0f, 1f)

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
        val created = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val windowStart = today.minusDays(window.toLong() - 1)
        val start = if (created.isAfter(windowStart)) created else windowStart
        val span = ChronoUnit.DAYS.between(start, today).toInt() + 1
        if (span <= 0 || start.isAfter(today)) return 0f
        val hits = (0 until span).count { isDone(start.plusDays(it.toLong())) }
        return hits.toFloat() / span
    }
}
