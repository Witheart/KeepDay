package com.example.keepday.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.data.Habit
import com.example.keepday.ui.components.CheckButton
import com.example.keepday.ui.components.Dot
import com.example.keepday.ui.components.EmojiBadge
import com.example.keepday.ui.components.EmptyState
import com.example.keepday.ui.components.GradientProgressBar
import com.example.keepday.ui.components.HeaderStat
import com.example.keepday.ui.components.SectionHeader
import com.example.keepday.ui.components.StreakBadge
import com.example.keepday.ui.components.WeekStrip
import com.example.keepday.ui.theme.AccentGradient
import com.example.keepday.ui.theme.KeepDayTheme
import com.example.keepday.ui.theme.accentOf
import java.time.LocalDate

private val weekdayNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

@Composable
fun TodayScreen(
    habits: List<Habit>,
    today: LocalDate,
    onToggle: (Habit) -> Unit,
    onOpenDetail: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePin: (Habit) -> Unit,
    onMove: (Habit, Int) -> Unit,
    pendingFirst: Boolean = true,
    modifier: Modifier = Modifier
) {
    val doneToday = habits.count { it.isDone(today) }
    val allDone = habits.isNotEmpty() && doneToday == habits.size
    val bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0
    // 置顶的习惯永远在最前；组内未完成的优先，已完成的自动下沉
    val displayHabits = habits.sortedWith(
        compareByDescending<Habit> { it.pinned }
            .thenBy { pendingFirst && it.isDone(today) }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TopBar(onOpenSettings = onOpenSettings)
        }
        item {
            TodayHeader(
                today = today,
                total = habits.size,
                doneToday = doneToday,
                bestStreak = bestStreak,
                allDone = allDone
            )
        }
        item { SectionHeader(title = "我的习惯", count = habits.size, trailing = "每天一点点") }

        if (habits.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🌱",
                    title = "还没有习惯",
                    subtitle = "添加一个小目标，从今天开始坚持",
                    actionLabel = "添加第一个习惯",
                    onAction = onAdd
                )
            }
        } else {
            items(items = displayHabits, key = { it.id }) { habit ->
                HabitCard(
                    modifier = Modifier.animateItem(),
                    habit = habit,
                    today = today,
                    onToggle = { onToggle(habit) },
                    onOpenDetail = { onOpenDetail(habit) },
                    onEdit = { onEdit(habit) },
                    onDelete = { onDelete(habit) },
                    onTogglePin = { onTogglePin(habit) },
                    onMoveUp = { onMove(habit, -1) },
                    onMoveDown = { onMove(habit, 1) }
                )
            }
        }
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "KeepDay",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "打卡 · 坚持",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚙", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayHeader(
    today: LocalDate,
    total: Int,
    doneToday: Int,
    bestStreak: Int,
    allDone: Boolean
) {
    val progress = if (total == 0) 0f else doneToday.toFloat() / total
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7C6BFF), Color(0xFF5B4BE0), Color(0xFF3F2FB8))
                )
            )
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = todayTitle(today),
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.width(8.dp))
                if (bestStreak > 0) {
                    StreakBadge(bestStreak)
                }
                Spacer(Modifier.weight(1f))
                if (allDone) {
                    CelebrationSparkles()
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    allDone -> "今天全部完成 🎉"
                    total == 0 -> "从第一个习惯开始"
                    else -> "今日已完成 $doneToday / $total"
                },
                color = Color.White,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    allDone -> "了不起，剩下的时间留给自己"
                    total == 0 -> "添加一个想坚持的小目标吧"
                    else -> "别断掉连续记录，今天还剩 ${total - doneToday} 个"
                },
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(18.dp))

            GradientProgressBar(
                progress = progress,
                trackColor = Color.White.copy(alpha = 0.22f),
                fillColors = listOf(Color(0xFFFFE0A8), Color(0xFFFFF7E6))
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeaderStat(label = "习惯", value = total.toString())
                HeaderStat(label = "今日完成", value = doneToday.toString())
                HeaderStat(label = "最长连续", value = "${bestStreak}天")
            }
        }
    }
}

@Composable
private fun HabitCard(
    modifier: Modifier = Modifier,
    habit: Habit,
    today: LocalDate,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val accent = accentOf(habit.accentIndex)
    val done = habit.isDone(today)
    var menuExpanded by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (done) accent.start.copy(alpha = 0.09f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(280),
        label = "cardContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (done) accent.start.copy(alpha = 0.34f) else MaterialTheme.colorScheme.outline,
        animationSpec = tween(280),
        label = "cardBorder"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (done) 0.dp else 1.dp
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clickable(onClick = onOpenDetail)
                    .padding(
                        PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 16.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiBadge(emoji = habit.emoji, accent = accent)
                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (habit.pinned) {
                            Spacer(Modifier.width(4.dp))
                            Text(text = "📌", fontSize = 11.sp)
                        }
                        if (habit.strict) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "严格",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (habit.currentStreak > 0) "🔥 连续 ${habit.currentStreak} 天" else "今天开始",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) accent.start else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Dot()
                        Text(
                            text = "本周 ${habit.weekDoneCount(today)}/${habit.effectiveWeekGoal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    WeekStrip(habit = habit, today = today, accent = accent)
                }

                Spacer(Modifier.width(12.dp))
                CheckButton(done = done, accent = accent, onClick = onToggle)
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                Text(
                    text = "⋯",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(top = 2.dp, end = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true }
                        .padding(top = 2.dp),
                    fontWeight = FontWeight.Bold
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text(if (habit.pinned) "取消置顶" else "置顶") },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("上移") },
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("下移") },
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** 全部完成时在右上角闪烁的小星光，克制但有反馈。 */
@Composable
private fun CelebrationSparkles() {
    val transition = rememberInfiniteTransition(label = "sparkles")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "✨", fontSize = 14.sp, modifier = Modifier.alpha(alpha))
        Spacer(Modifier.width(2.dp))
        Text(text = "✨", fontSize = 10.sp, modifier = Modifier.alpha(alpha * 0.8f))
    }
}

private fun todayTitle(today: LocalDate): String {
    val weekday = weekdayNames[(today.dayOfWeek.value - 1).coerceIn(0, 6)]
    return "${today.monthValue}月${today.dayOfMonth}日 · $weekday"
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F5FB, heightDp = 900)
@Composable
private fun TodayScreenPreview() {
    val today = LocalDate.now()
    val sample = listOf(
        Habit(
            id = "1",
            name = "早起喝一杯水",
            emoji = "💧",
            accentIndex = 0,
            weekGoal = 7,
            days = setOf(
                today.toEpochDay(),
                today.minusDays(1).toEpochDay(),
                today.minusDays(2).toEpochDay(),
                today.minusDays(5).toEpochDay()
            )
        ),
        Habit(
            id = "2",
            name = "阅读 20 分钟",
            emoji = "📚",
            accentIndex = 4,
            weekGoal = 5,
            days = setOf(today.minusDays(1).toEpochDay())
        ),
        Habit(id = "3", name = "运动打卡", emoji = "🏃", accentIndex = 2, weekGoal = 3)
    )
    KeepDayTheme {
        TodayScreen(
            habits = sample,
            today = today,
            onToggle = {},
            onOpenDetail = {},
            onEdit = {},
            onDelete = {},
            onAdd = {},
            onOpenSettings = {},
            onTogglePin = {},
            onMove = { _, _ -> }
        )
    }
}
