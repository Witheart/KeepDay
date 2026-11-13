package com.example.keepday.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.data.Habit
import com.example.keepday.ui.components.EmojiBadge
import com.example.keepday.ui.components.GradientProgressBar
import com.example.keepday.ui.components.WeekStrip
import com.example.keepday.ui.theme.AccentGradient
import com.example.keepday.ui.theme.accentOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun HabitDetailScreen(
    habit: Habit,
    today: LocalDate,
    onBack: () -> Unit,
    onToggleDate: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentOf(habit.accentIndex)
    var month by remember(habit.id) { mutableStateOf(YearMonth.from(today)) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "←", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "习惯详情",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "编辑",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        item { HeroCard(habit = habit, today = today, accent = accent) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(label = "当前连续", value = "${habit.currentStreak} 天", modifier = Modifier.weight(1f))
                StatCard(label = "最长连续", value = "${habit.bestStreak} 天", modifier = Modifier.weight(1f))
                StatCard(label = "累计打卡", value = "${habit.totalDays} 天", modifier = Modifier.weight(1f))
            }
        }

        item { CompletionCard(habit = habit, today = today, accent = accent) }

        item {
            CalendarCard(
                habit = habit,
                today = today,
                month = month,
                accent = accent,
                onMonthChange = { month = it },
                onToggleDate = onToggleDate
            )
        }

        item {
            Text(
                text = "删除这个习惯",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onDelete)
                    .padding(vertical = 14.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroCard(habit: Habit, today: LocalDate, accent: AccentGradient) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji = habit.emoji, accent = accent, size = 54.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (habit.weekGoal == 7) "每天坚持" else "每周目标 ${habit.weekGoal} 天",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥 ${habit.currentStreak}",
                    style = MaterialTheme.typography.displaySmall,
                    color = accent.start
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "天连续",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                WeekStrip(habit = habit, today = today, accent = accent)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "本周 ${habit.weekDoneCount(today)}/${habit.weekGoal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                GradientProgressBar(
                    progress = habit.weekProgress(today),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    fillColors = listOf(accent.start, accent.end),
                    height = 8.dp
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
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
}

@Composable
private fun CompletionCard(habit: Habit, today: LocalDate, accent: AccentGradient) {
    val rate = habit.completionRate(today, window = 30)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "最近 30 天完成率",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(rate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent.start
                )
            }
            Spacer(Modifier.height(10.dp))
            GradientProgressBar(
                progress = rate,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                fillColors = listOf(accent.start, accent.end),
                height = 10.dp
            )
        }
    }
}

@Composable
private fun CalendarCard(
    habit: Habit,
    today: LocalDate,
    month: YearMonth,
    accent: AccentGradient,
    onMonthChange: (YearMonth) -> Unit,
    onToggleDate: (LocalDate) -> Unit
) {
    val doneDays = habit.monthDoneDays(month)
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val totalDays = month.lengthOfMonth()
    val cells: List<Int?> = List(firstOffset) { null } + (1..totalDays).map { it }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.CHINA)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                MonthNavButton(text = "‹", enabled = true) { onMonthChange(month.minusMonths(1)) }
                Spacer(Modifier.width(6.dp))
                MonthNavButton(
                    text = "›",
                    enabled = month.isBefore(YearMonth.from(today))
                ) { onMonthChange(month.plusMonths(1)) }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (day != null) {
                                val date = month.atDay(day)
                                DayCell(
                                    day = day,
                                    done = doneDays.contains(day),
                                    isToday = date == today,
                                    enabled = !date.isAfter(today),
                                    accent = accent,
                                    onClick = { onToggleDate(date) }
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "本月已打卡 ${doneDays.size} 天 · 点按日期可补卡或取消",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthNavButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    done: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    accent: AccentGradient,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .padding(3.dp)
            .size(36.dp)
            .clip(shape)
            .then(
                when {
                    done -> Modifier.background(Brush.linearGradient(listOf(accent.start, accent.end)))
                    isToday -> Modifier.border(1.5.dp, accent.start, shape)
                    else -> Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$day",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (done || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                done -> Color.White
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                isToday -> accent.start
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontSize = 12.sp
        )
    }
}
