package com.example.keepday.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.keepday.data.Habit
import com.example.keepday.ui.components.EmojiBadge
import com.example.keepday.ui.components.EmptyState
import com.example.keepday.ui.components.GradientProgressBar
import com.example.keepday.ui.components.HeaderStat
import com.example.keepday.ui.components.SectionHeader
import com.example.keepday.ui.theme.accentOf
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun StatsScreen(
    habits: List<Habit>,
    today: LocalDate,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val doneToday = habits.count { it.isDone(today) }
    val todayRate = if (habits.isEmpty()) 0f else doneToday.toFloat() / habits.size
    val totalCheckIns = habits.sumOf { it.totalDays }
    val bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0
    val yearStats = yearStats(habits, today.year)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "统计",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "看看坚持带来的变化",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OverviewCard(
                todayRate = todayRate,
                doneToday = doneToday,
                total = habits.size,
                totalCheckIns = totalCheckIns,
                bestStreak = bestStreak
            )
        }

        if (habits.isEmpty()) {
            item {
                EmptyState(
                    emoji = "📊",
                    title = "还没有数据",
                    subtitle = "先添加一个习惯，坚持几天就能看到趋势",
                    actionLabel = "去添加习惯",
                    onAction = onAdd
                )
            }
        } else {
            item { SectionHeader(title = "${today.year} 年", count = 0, trailing = "年度概览") }
            item { YearSummaryCard(yearStats = yearStats, year = today.year) }
            item { MonthHeatmapCard(habits = habits, year = today.year) }

            item { SectionHeader(title = "最近 7 天", count = 0, trailing = "完成率趋势") }
            item { TrendCard(habits = habits, today = today) }

            item { SectionHeader(title = "打卡时间", count = 0, trailing = "按打卡时刻统计") }
            item { TimeDistributionCard(habits = habits) }

            item { SectionHeader(title = "各习惯完成率", count = habits.size, trailing = "最近 30 天") }
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        habits.sortedByDescending { it.completionRate(today) }
                            .forEachIndexed { index, habit ->
                                HabitRateRow(habit = habit, today = today)
                                if (index != habits.lastIndex) {
                                    Spacer(Modifier.height(14.dp))
                                }
                            }
                    }
                }
            }
        }
    }
}

/* ------------------------------ 数据聚合 ------------------------------ */

private data class YearStats(
    val totalCheckIns: Int = 0,
    val activeDays: Int = 0,
    val bestMonth: Int = 0,
    val bestMonthCount: Int = 0,
    val averagePerActiveDay: Float = 0f
)

/** 某天完成的习惯数（epochDay → 次数）。 */
private fun dailyCounts(habits: List<Habit>): Map<Long, Int> {
    val counts = mutableMapOf<Long, Int>()
    habits.forEach { habit ->
        habit.days.forEach { day -> counts[day] = (counts[day] ?: 0) + 1 }
    }
    return counts
}

private fun yearStats(habits: List<Habit>, year: Int): YearStats {
    if (habits.isEmpty()) return YearStats()
    val counts = dailyCounts(habits)
    var total = 0
    var active = 0
    val months = IntArray(12)
    counts.forEach { (day, value) ->
        val date = LocalDate.ofEpochDay(day)
        if (date.year == year) {
            total += value
            active += 1
            months[date.monthValue - 1] += value
        }
    }
    val bestMonth = months.indices.maxByOrNull { months[it] } ?: 0
    return YearStats(
        totalCheckIns = total,
        activeDays = active,
        bestMonth = if (months[bestMonth] == 0) 0 else bestMonth + 1,
        bestMonthCount = months[bestMonth],
        averagePerActiveDay = if (active == 0) 0f else total.toFloat() / active
    )
}

/* ------------------------------- 卡片 -------------------------------- */

@Composable
private fun OverviewCard(
    todayRate: Float,
    doneToday: Int,
    total: Int,
    totalCheckIns: Int,
    bestStreak: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF6C5CE7), Color(0xFF4B3FD1), Color(0xFF2F2A8F))
                )
            )
            .padding(22.dp)
    ) {
        Column {
            Text(
                text = "今日完成率 ${(todayRate * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (total == 0) "还没有习惯" else "$total 个习惯中完成了 $doneToday 个",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            GradientProgressBar(
                progress = todayRate,
                trackColor = Color.White.copy(alpha = 0.22f),
                fillColors = listOf(Color(0xFFFFE0A8), Color(0xFFFFF7E6))
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeaderStat(label = "累计打卡", value = "$totalCheckIns 次")
                HeaderStat(label = "习惯", value = "$total 个")
                HeaderStat(label = "最长连续", value = "${bestStreak}天")
            }
        }
    }
}

@Composable
private fun YearSummaryCard(yearStats: YearStats, year: Int) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                YearStat(label = "打卡次数", value = "${yearStats.totalCheckIns}")
                YearStat(label = "活跃天数", value = "${yearStats.activeDays}")
                YearStat(
                    label = "最活跃月份",
                    value = if (yearStats.bestMonth == 0) "—" else "${yearStats.bestMonth} 月"
                )
            }
            if (yearStats.bestMonth != 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "$year 年 ${yearStats.bestMonth} 月共打卡 ${yearStats.bestMonthCount} 次，" +
                        "有记录的日子平均每天 ${String.format("%.1f", yearStats.averagePerActiveDay)} 次",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun YearStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 一年的月度热力图：行是月份，列是日期，颜色深浅表示当天完成的习惯数。 */
@Composable
private fun MonthHeatmapCard(habits: List<Habit>, year: Int) {
    val counts = dailyCounts(habits)
    val maxCount = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val baseColor = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            for (month in 1..12) {
                val days = YearMonth.of(year, month).lengthOfMonth()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${month}月",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(30.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (day in 1..31) {
                            val color = if (day > days) {
                                Color.Transparent
                            } else {
                                val count = counts[LocalDate.of(year, month, day).toEpochDay()] ?: 0
                                if (count == 0) emptyColor
                                else baseColor.copy(alpha = 0.25f + 0.75f * (count.toFloat() / maxCount))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
                if (month != 12) Spacer(Modifier.height(3.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "颜色越深表示当天完成的习惯越多",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendCard(habits: List<Habit>, today: LocalDate) {
    val dates = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = dates.map { date ->
        if (habits.isEmpty()) 0f else habits.count { it.isDone(date) }.toFloat() / habits.size
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val slot = size.width / values.size
                val barWidth = slot * 0.42f
                values.forEachIndexed { index, value ->
                    val left = slot * index + (slot - barWidth) / 2f
                    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = radius
                    )
                    val filledHeight = size.height * value.coerceIn(0f, 1f)
                    if (filledHeight > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(primary, secondary)),
                            topLeft = Offset(left, size.height - filledHeight),
                            size = Size(barWidth, filledHeight),
                            cornerRadius = radius
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                dates.forEach { date ->
                    Text(
                        text = "${date.monthValue}/${date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val hourBuckets = listOf(
    "凌晨" to (0 until 6),
    "早晨" to (6 until 9),
    "上午" to (9 until 12),
    "下午" to (12 until 18),
    "傍晚" to (18 until 21),
    "夜间" to (21 until 24)
)

@Composable
private fun TimeDistributionCard(habits: List<Habit>) {
    val counts = IntArray(hourBuckets.size)
    var recorded = 0
    habits.forEach { habit ->
        habit.dayTimes.values.forEach { millis ->
            val hour = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour
            val index = hourBuckets.indexOfFirst { hour in it.second }
            if (index >= 0) {
                counts[index] += 1
                recorded += 1
            }
        }
    }
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            hourBuckets.forEachIndexed { index, bucket ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bucket.first,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(track)
                    ) {
                        if (counts[index] > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(counts[index].toFloat() / max)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(primary, secondary)))
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${counts[index]}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(22.dp),
                        textAlign = TextAlign.End
                    )
                }
                if (index != hourBuckets.lastIndex) Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (recorded == 0) "还没有记录打卡时刻，从这次打卡开始就会统计"
                else "共统计 $recorded 次打卡时刻",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HabitRateRow(habit: Habit, today: LocalDate) {
    val accent = accentOf(habit.accentIndex)
    val rate = habit.completionRate(today)
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmojiBadge(emoji = habit.emoji, accent = accent, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(rate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.start
                )
            }
            Spacer(Modifier.height(6.dp))
            GradientProgressBar(
                progress = rate,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                fillColors = listOf(accent.start, accent.end),
                height = 7.dp
            )
        }
    }
}
