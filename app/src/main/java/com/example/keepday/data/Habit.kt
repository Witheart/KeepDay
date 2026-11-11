package com.example.keepday.data

import java.time.LocalDate

/**
 * 一个需要每天坚持的习惯。
 *
 * @param days 已打卡的日期，用 [LocalDate.toEpochDay] 存储，便于排序与比较。
 */
data class Habit(
    val id: String,
    val name: String,
    val emoji: String,
    val accentIndex: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val days: Set<Long> = emptySet()
) {
    fun isDone(date: LocalDate): Boolean = days.contains(date.toEpochDay())

    /** 累计打卡天数。 */
    val totalDays: Int get() = days.size

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
}
