package com.example.keepday.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.data.weight.WeightEntry
import com.example.keepday.data.weight.WeightMath
import com.example.keepday.data.weight.WeightSetting
import com.example.keepday.data.weight.WeightUnits
import com.example.keepday.ui.components.EmptyState
import com.example.keepday.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")

private val windowOptions = listOf(
    "近 7 天" to 7,
    "近 30 天" to 30,
    "近 90 天" to 90,
    "全部" to null
)

@Composable
fun WeightScreen(
    entries: List<WeightEntry>,
    setting: WeightSetting,
    today: LocalDate,
    onRecord: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayEntry = WeightMath.entryAt(entries, today)
    val lastEntry = entries.maxByOrNull { it.epochDay }
    val streak = WeightMath.streakAt(entries, setting, today)
    var windowDays by remember { mutableStateOf<Int?>(30) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "体重",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                if (streak > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "连续记录 $streak 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
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

        item {
            TodayWeightCard(
                todayEntry = todayEntry,
                yesterdayEntry = WeightMath.entryAt(entries, today.minusDays(1)),
                today = today,
                onRecord = { onRecord(today) }
            )
        }

        if (entries.isEmpty()) {
            item {
                EmptyState(
                    emoji = "⚖️",
                    title = "还没有体重记录",
                    subtitle = "记录今天的体重，之后就能看到趋势和变化",
                    actionLabel = "记录第一笔",
                    onAction = { onRecord(today) }
                )
            }
        } else {
            item {
                SectionHeader(
                    title = "趋势",
                    count = 0,
                    trailing = when {
                        lastEntry == null -> ""
                        lastEntry.epochDay != today.toEpochDay() ->
                            "最近一次 ${dateFormatter.format(lastEntry.date)}"

                        else -> "今天已记录"
                    }
                )
            }
            item {
                WindowSelector(selected = windowDays, onSelect = { windowDays = it })
            }
            item {
                WeightChartCard(
                    entries = entries,
                    setting = setting,
                    today = today,
                    windowDays = windowDays,
                    onPickDate = onRecord
                )
            }
            item {
                WeightStatsCard(
                    entries = entries,
                    setting = setting,
                    today = today,
                    windowDays = windowDays,
                    streak = streak
                )
            }
        }
    }
}

@Composable
private fun TodayWeightCard(
    todayEntry: WeightEntry?,
    yesterdayEntry: WeightEntry?,
    today: LocalDate,
    onRecord: () -> Unit
) {
    val deltaJin = if (todayEntry != null && yesterdayEntry != null) {
        WeightUnits.kgToJin(todayEntry.kg - yesterdayEntry.kg)
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7C6BFF), Color(0xFF5B4BE0), Color(0xFF3F2FB8))
                )
            )
            .padding(22.dp)
    ) {
        Column {
            Text(
                text = "今日体重",
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = todayEntry?.let { WeightUnits.format(WeightUnits.kgToJin(it.kg)) } ?: "--",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "斤",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = todayEntry?.let {
                    "今天 " + Instant.ofEpochMilli(it.recordedAt)
                        .atZone(ZoneId.systemDefault())
                        .format(timeFormatter) + " 记录" +
                        if (it.isBackfilled) "（补录）" else ""
                } ?: "还没记录今天的数据",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )
            deltaJin?.let { delta ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = when {
                            delta < -0.05f -> "比昨天 ↓ ${WeightUnits.format(-delta)} 斤"
                            delta > 0.05f -> "比昨天 ↑ ${WeightUnits.format(delta)} 斤"
                            else -> "与昨天持平"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.clickable(onClick = onRecord)
            ) {
                Text(
                    text = if (todayEntry == null) "记录今天的体重" else "修改今天的体重",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun WindowSelector(selected: Int?, onSelect: (Int?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        windowOptions.forEach { (label, days) ->
            val isSelected = selected == days
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary
                    )
                } else {
                    null
                },
                modifier = Modifier.clickable { onSelect(days) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun WeightChartCard(
    entries: List<WeightEntry>,
    setting: WeightSetting,
    today: LocalDate,
    windowDays: Int?,
    onPickDate: (LocalDate) -> Unit
) {
    val windowStart = windowDays?.let { today.minusDays(it.toLong() - 1) }
        ?: entries.minByOrNull { it.epochDay }?.date
        ?: today
    val visible = entries.filter { !it.date.isBefore(windowStart) }.sortedBy { it.epochDay }
    val span = ChronoUnit.DAYS.between(windowStart, today).toInt().coerceAtLeast(1)

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val goalColor = MaterialTheme.colorScheme.tertiary
    val fillTop = primary.copy(alpha = 0.22f)
    val fillBottom = primary.copy(alpha = 0.02f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (visible.isEmpty()) {
                Text(
                    text = "这段时间还没有记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val values = visible.map { it.kg }
            val goal = setting.goalKg
            val minValue = (values + listOfNotNull(goal)).min()
            val maxValue = (values + listOfNotNull(goal)).max()
            val padding = ((maxValue - minValue) * 0.15f).coerceAtLeast(0.4f)
            val lower = minValue - padding
            val upper = maxValue + padding

            Row {
                Column(
                    modifier = Modifier.height(150.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = WeightUnits.format(WeightUnits.kgToJin(upper)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = WeightUnits.format(WeightUnits.kgToJin(lower)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp)
                        .pointerInput(visible, span, windowStart) {
                            detectTapGestures { offset ->
                                val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                val date = windowStart.plusDays((ratio * span).roundToInt().toLong())
                                if (!date.isAfter(today)) onPickDate(date)
                            }
                        }
                ) {
                    fun xOf(entry: WeightEntry): Float {
                        val offsetDays = ChronoUnit.DAYS.between(windowStart, entry.date).toFloat()
                        return (offsetDays / span) * size.width
                    }

                    fun yOf(kg: Float): Float =
                        size.height - ((kg - lower) / (upper - lower)) * size.height

                    // 横向参考线
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = size.height * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    // 目标线
                    goal?.let { goalKg ->
                        drawLine(
                            color = goalColor.copy(alpha = 0.8f),
                            start = Offset(0f, yOf(goalKg)),
                            end = Offset(size.width, yOf(goalKg)),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }

                    if (visible.size >= 2) {
                        val linePath = Path()
                        val areaPath = Path()
                        visible.forEachIndexed { index, entry ->
                            val x = xOf(entry)
                            val y = yOf(entry.kg)
                            if (index == 0) {
                                linePath.moveTo(x, y)
                                areaPath.moveTo(x, size.height)
                                areaPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                areaPath.lineTo(x, y)
                            }
                        }
                        areaPath.lineTo(xOf(visible.last()), size.height)
                        areaPath.close()
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(listOf(fillTop, fillBottom))
                        )
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(listOf(primary, secondary)),
                            style = Stroke(width = 3f)
                        )
                    }

                    visible.forEach { entry ->
                        val isToday = entry.epochDay == today.toEpochDay()
                        val center = Offset(xOf(entry), yOf(entry.kg))
                        drawCircle(
                            color = Color.White,
                            radius = if (isToday) 6f else 4.5f,
                            center = center
                        )
                        drawCircle(
                            color = if (isToday) secondary else primary,
                            radius = if (isToday) 4f else 3f,
                            center = center
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = dateFormatter.format(windowStart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "今天",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "点图表任意位置可以补录那天的体重",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (setting.goalKg != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "虚线为目标 ${WeightUnits.format(WeightUnits.kgToJin(setting.goalKg))} 斤",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun WeightStatsCard(
    entries: List<WeightEntry>,
    setting: WeightSetting,
    today: LocalDate,
    windowDays: Int?,
    streak: Int
) {
    val stats = WeightMath.statsFor(entries, today, windowDays)
    val yesterdayDelta = WeightMath.changeAgainstPrevious(entries, today, 1)
    val weekDelta = WeightMath.changeAgainstPrevious(entries, today, 7)
    val latest = entries.maxByOrNull { it.epochDay }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (windowDays == null) "全部记录的统计" else "近 $windowDays 天的统计",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            stats.changeJin?.let { change ->
                StatLine(
                    label = "体重变化",
                    value = when {
                        change < -0.05f -> "下降 ${WeightUnits.format(-change)} 斤"
                        change > 0.05f -> "上升 ${WeightUnits.format(change)} 斤"
                        else -> "基本持平"
                    },
                    highlight = true
                )
            }
            yesterdayDelta?.let {
                StatLine(label = "较昨天", value = "${WeightUnits.formatDelta(it)} 斤")
            }
            weekDelta?.let {
                StatLine(label = "较 7 天前", value = "${WeightUnits.formatDelta(it)} 斤")
            }
            stats.minEntry?.let {
                StatLine(
                    label = "最低",
                    value = "${WeightUnits.format(WeightUnits.kgToJin(it.kg))} 斤 · " +
                        dateFormatter.format(it.date)
                )
            }
            stats.maxEntry?.let {
                StatLine(
                    label = "最高",
                    value = "${WeightUnits.format(WeightUnits.kgToJin(it.kg))} 斤 · " +
                        dateFormatter.format(it.date)
                )
            }
            StatLine(label = "记录天数", value = "${stats.recordedDays} 天")
            StatLine(label = "连续记录", value = "$streak 天")
            StatLine(label = "最长连续", value = "${WeightMath.bestStreak(entries)} 天")

            val goal = setting.goalKg
            if (goal != null && latest != null) {
                val diff = WeightUnits.kgToJin(latest.kg - goal)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (diff > 0.05f) {
                        "距离目标还差 ${WeightUnits.format(diff)} 斤，继续加油"
                    } else {
                        "已经达成目标，比目标低 ${WeightUnits.format(-diff)} 斤 🎉"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
    }
}
