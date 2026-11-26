package com.example.keepday.data.weight

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 一条体重记录。内部统一用 kg 存储，展示时再换算成斤。 */
data class WeightEntry(
    val epochDay: Long,
    val kg: Float,
    val recordedAt: Long
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)

    /** 实际录入的日期，与 [date] 不同说明是补录。 */
    val recordedDate: LocalDate
        get() = Instant.ofEpochMilli(recordedAt).atZone(ZoneId.systemDefault()).toLocalDate()

    val isBackfilled: Boolean get() = recordedDate != date
}

/** 体重打卡的规则配置。 */
data class WeightSetting(
    val strict: Boolean = false,
    val allowBackfill: Boolean = true,
    val goalKg: Float? = null,
    /** 连续记录天数的计数起点，断签后重置。 */
    val streakResetDay: Long? = null,
    /** 开始记录的日期，用于判断漏签范围。 */
    val createdDay: Long = LocalDate.now().toEpochDay()
) {
    val goalJin: Float? get() = goalKg?.times(2f)
}

/** 一段时间窗口内的统计结果。 */
data class WeightStats(
    val changeJin: Float? = null,
    val minEntry: WeightEntry? = null,
    val maxEntry: WeightEntry? = null,
    val recordedDays: Int = 0,
    val averageKg: Float? = null
)

object WeightUnits {
    const val JIN_PER_KG = 2f

    fun kgToJin(kg: Float): Float = kg * JIN_PER_KG

    fun jinToKg(jin: Float): Float = jin / JIN_PER_KG

    /** 保留一位小数，例如 "63.4"。 */
    fun format(value: Float): String = String.format("%.1f", value)

    /** 带符号的差值，例如 "-0.4" / "+1.2"。 */
    fun formatDelta(value: Float): String {
        val text = String.format("%.1f", kotlin.math.abs(value))
        return when {
            value > 0.05f -> "+$text"
            value < -0.05f -> "-$text"
            else -> "0.0"
        }
    }
}

object WeightMath {

    fun entryAt(entries: Collection<WeightEntry>, date: LocalDate): WeightEntry? =
        entries.firstOrNull { it.epochDay == date.toEpochDay() }

    /** 从 [today] 往回数连续记录天数。今天没记录时从昨天开始数。 */
    fun streakAt(
        entries: Collection<WeightEntry>,
        setting: WeightSetting,
        today: LocalDate = LocalDate.now()
    ): Int {
        val days = entries.mapTo(mutableSetOf()) { it.epochDay }
        val floor = setting.streakResetDay
        var cursor = today
        if (!days.contains(cursor.toEpochDay())) cursor = cursor.minusDays(1)
        var streak = 0
        while (days.contains(cursor.toEpochDay()) &&
            (floor == null || cursor.toEpochDay() >= floor)
        ) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestStreak(entries: Collection<WeightEntry>): Int {
        if (entries.isEmpty()) return 0
        val sorted = entries.map { it.epochDay }.sorted()
        var best = 1
        var run = 1
        for (index in 1 until sorted.size) {
            run = if (sorted[index] == sorted[index - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** 严格模式：从计数起点到今天之前有一天没记录，就算断签。 */
    fun strictResetDue(
        entries: Collection<WeightEntry>,
        setting: WeightSetting,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        if (!setting.strict) return false
        val days = entries.mapTo(mutableSetOf()) { it.epochDay }
        val start = setting.streakResetDay ?: setting.createdDay
        val yesterday = today.minusDays(1).toEpochDay()
        if (start > yesterday) return false
        return (start..yesterday).any { it !in days }
    }

    /** 能否记录该日期：不能是未来；关闭补录后只能是今天。 */
    fun canRecord(
        date: LocalDate,
        setting: WeightSetting,
        today: LocalDate = LocalDate.now()
    ): Boolean = !date.isAfter(today) && (setting.allowBackfill || date == today)

    /**
     * 统计窗口内的数据。
     * @param windowDays 为 null 表示全部历史。
     */
    fun statsFor(
        entries: List<WeightEntry>,
        today: LocalDate,
        windowDays: Int?
    ): WeightStats {
        val startDay = windowDays?.let { today.minusDays(it.toLong() - 1).toEpochDay() }
        val window = entries
            .filter { startDay == null || it.epochDay >= startDay }
            .sortedBy { it.epochDay }
        if (window.isEmpty()) return WeightStats()
        val first = window.first()
        val last = window.last()
        val change = if (window.size >= 2) {
            WeightUnits.kgToJin(last.kg - first.kg)
        } else {
            null
        }
        return WeightStats(
            changeJin = change,
            minEntry = window.minByOrNull { it.kg },
            maxEntry = window.maxByOrNull { it.kg },
            recordedDays = window.size,
            averageKg = window.map { it.kg }.average().toFloat()
        )
    }

    /** 与 N 天前的记录相比（找不到就用窗口内最早的一条）。 */
    fun changeAgainstPrevious(
        entries: List<WeightEntry>,
        today: LocalDate,
        daysAgo: Int
    ): Float? {
        val target = entryAt(entries, today) ?: entries.maxByOrNull { it.epochDay } ?: return null
        val reference = entries.firstOrNull {
            it.epochDay == today.minusDays(daysAgo.toLong()).toEpochDay()
        } ?: return null
        return WeightUnits.kgToJin(target.kg - reference.kg)
    }
}
