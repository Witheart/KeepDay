package com.example.keepday.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.data.Habit
import com.example.keepday.ui.theme.AccentGradient
import java.time.LocalDate

/** 带渐变填充的圆角进度条。 */
@Composable
fun GradientProgressBar(
    progress: Float,
    trackColor: Color,
    fillColors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "progress"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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
fun EmojiBadge(emoji: String, accent: AccentGradient, size: Dp = 46.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(
                Brush.linearGradient(
                    listOf(accent.start.copy(alpha = 0.22f), accent.end.copy(alpha = 0.12f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.48f).sp)
    }
}

/** 最近 7 天的迷你打卡条，最后一条是今天。 */
@Composable
fun WeekStrip(
    habit: Habit,
    today: LocalDate,
    accent: AccentGradient,
    modifier: Modifier = Modifier
) {
    val flags = habit.recentDays(today)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
                    isToday -> base.border(1.dp, accent.start.copy(alpha = 0.55f), shape)
                    else -> base.background(MaterialTheme.colorScheme.surfaceVariant)
                }
            )
        }
    }
}

@Composable
fun CheckButton(done: Boolean, accent: AccentGradient, onClick: () -> Unit) {
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

@Composable
fun Dot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
fun StreakBadge(days: Int) {
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
fun HeaderStat(label: String, value: String) {
    Column {
        Text(text = value, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
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
        if (trailing != null) {
            if (onTrailingClick != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onTrailingClick)
                ) {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            Text(text = emoji, fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onAction)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
            )
        }
    }
}
