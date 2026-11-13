package com.example.keepday.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.data.Habit
import com.example.keepday.ui.theme.AccentGradient
import com.example.keepday.ui.theme.HabitAccents
import com.example.keepday.ui.theme.KeepDayTheme
import com.example.keepday.ui.theme.accentOf
import java.time.DayOfWeek
import java.time.LocalDate

private val emojiChoices = listOf(
    "💧", "🏃", "📚", "🧘", "🌙", "💪", "🥗", "✍️", "🎸", "🧹", "🎯", "☀️"
)

private val weekdayNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

@Composable
fun HabitScreen(
    habits: List<Habit>,
    onToggle: (String) -> Unit,
    onAddHabit: (name: String, emoji: String, accentIndex: Int) -> Boolean,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    val today = LocalDate.now()
    val doneToday = habits.count { it.isDone(today) }
    val allDone = habits.isNotEmpty() && doneToday == habits.size
    val bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(22.dp),
                icon = { Text("＋", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = { Text("添加习惯", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TodayHeader(
                    today = today,
                    total = habits.size,
                    doneToday = doneToday,
                    bestStreak = bestStreak,
                    allDone = allDone
                )
            }
            item { SectionHeader(count = habits.size) }

            if (habits.isEmpty()) {
                item { EmptyState(onAdd = { showAddDialog = true }) }
            } else {
                items(items = habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        today = today,
                        onToggle = { onToggle(habit.id) },
                        onDeleteRequest = { habitToDelete = habit }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, emoji, accent ->
                val added = onAddHabit(name, emoji, accent)
                if (added) showAddDialog = false
                added
            }
        )
    }

    habitToDelete?.let { habit ->
        DeleteHabitDialog(
            habit = habit,
            onDismiss = { habitToDelete = null },
            onConfirm = {
                onDelete(habit.id)
                habitToDelete = null
            }
        )
    }
}

/* ------------------------------- 顶部概览 ------------------------------- */

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
private fun StreakBadge(days: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Text(
            text = "🔥 最长 $days 天",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HeaderStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun GradientProgressBar(
    progress: Float,
    trackColor: Color,
    fillColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "progress"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(radius, radius)
        )
        val filledWidth = size.width * animated
        if (filledWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(fillColors),
                topLeft = Offset.Zero,
                size = Size(filledWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

@Composable
private fun SectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "我的习惯",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.width(8.dp))
        if (count > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "每天一点点",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* --------------------------------- 卡片 --------------------------------- */

@Composable
private fun HabitCard(
    habit: Habit,
    today: LocalDate,
    onToggle: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val accent = accentOf(habit.accentIndex)
    val done = habit.isDone(today)
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (done) 0.dp else 1.dp
    ) {
        Box {
            Row(
                modifier = Modifier.padding(
                    PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 16.dp)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiBadge(emoji = habit.emoji, accent = accent)

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (habit.currentStreak > 0) "🔥 连续 ${habit.currentStreak} 天" else "今天开始",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) accent.start else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Dot()
                        Text(
                            text = "累计 ${habit.totalDays} 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    WeekStrip(habit = habit, today = today, accent = accent, doneToday = done)
                }

                Spacer(Modifier.width(12.dp))

                CheckButton(done = done, accent = accent, onClick = onToggle)
            }

            Text(
                text = "✕",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteRequest)
                    .padding(top = 5.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Dot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun EmojiBadge(emoji: String, accent: AccentGradient) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.start.copy(alpha = 0.22f), accent.end.copy(alpha = 0.12f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

@Composable
private fun WeekStrip(habit: Habit, today: LocalDate, accent: AccentGradient, doneToday: Boolean) {
    val flags = habit.recentDays(today)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        flags.forEachIndexed { index, done ->
            val isToday = index == flags.lastIndex
            val shape = RoundedCornerShape(4.dp)
            val base = Modifier
                .width(16.dp)
                .height(7.dp)
                .clip(shape)
            Box(
                modifier = when {
                    done -> base.background(Brush.horizontalGradient(listOf(accent.start, accent.end)))
                    isToday -> base.border(
                        width = 1.dp,
                        color = accent.start.copy(alpha = if (doneToday) 1f else 0.5f),
                        shape = shape
                    )

                    else -> base.background(MaterialTheme.colorScheme.surfaceVariant)
                }
            )
        }
    }
}

@Composable
private fun CheckButton(done: Boolean, accent: AccentGradient, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (done) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "checkScale"
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .then(
                if (done) {
                    Modifier.background(Brush.linearGradient(listOf(accent.start, accent.end)))
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (done) "✓" else "＋",
            color = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ------------------------------- 空状态 -------------------------------- */

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌱", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "还没有习惯",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "添加一个小目标，从今天开始坚持",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAdd,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "添加第一个习惯", fontWeight = FontWeight.SemiBold)
        }
    }
}

/* -------------------------------- 弹窗 --------------------------------- */

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, accentIndex: Int) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(emojiChoices.first()) }
    var accentIndex by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "新建习惯",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    singleLine = true,
                    isError = error != null,
                    placeholder = { Text("例如：早起喝一杯水") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "选个图标",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = emojiChoices) { item ->
                        EmojiOption(
                            emoji = item,
                            selected = item == emoji,
                            onClick = { emoji = item }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "选个颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitAccents.forEachIndexed { index, accent ->
                        ColorOption(
                            accent = accent,
                            selected = index == accentIndex,
                            onClick = { accentIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ok = onConfirm(name, emoji, accentIndex)
                    if (!ok) error = "名称不能为空，也不能和已有习惯重名"
                }
            ) {
                Text(text = "创建", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun EmojiOption(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}

@Composable
private fun ColorOption(accent: AccentGradient, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent.start, accent.end)))
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun DeleteHabitDialog(habit: Habit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "删除习惯",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "「${habit.name}」的 ${habit.totalDays} 天打卡记录会被一起删除，无法恢复。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/* -------------------------------- 工具函数 ------------------------------- */

private fun todayTitle(today: LocalDate): String {
    val weekday = weekdayNames[(today.dayOfWeek.value - 1).coerceIn(0, 6)]
    return "${today.monthValue}月${today.dayOfMonth}日 · $weekday"
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F5FB, heightDp = 900)
@Composable
private fun HabitScreenPreview() {
    val today = LocalDate.now()
    val sample = listOf(
        Habit(
            id = "1",
            name = "早起喝一杯水",
            emoji = "💧",
            accentIndex = 0,
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
            days = setOf(today.minusDays(1).toEpochDay())
        ),
        Habit(id = "3", name = "运动打卡", emoji = "🏃", accentIndex = 2)
    )
    KeepDayTheme {
        HabitScreen(
            habits = sample,
            onToggle = {},
            onAddHabit = { _, _, _ -> true },
            onDelete = {}
        )
    }
}
