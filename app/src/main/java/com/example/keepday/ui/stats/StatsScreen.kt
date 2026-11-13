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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import java.time.LocalDate

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
            item { SectionHeader(title = "最近 7 天", count = 0, trailing = "完成率趋势") }
            item { TrendCard(habits = habits, today = today) }

            item { SectionHeader(title = "各习惯完成率", count = habits.size, trailing = "最近 30 天") }
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        habits.sortedByDescending { it.completionRate(today) }.forEachIndexed { index, habit ->
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
