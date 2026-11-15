package com.example.keepday.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val startY: Float,
    val size: Float,
    val delay: Float,
    val drift: Float,
    val phase: Float,
    val color: Color,
    val isRect: Boolean
)

private val celebrationPalette = listOf(
    Color(0xFF7C6BFF),
    Color(0xFF00C2A8),
    Color(0xFFFFB25E),
    Color(0xFFFF7A9C),
    Color(0xFF4FA8FF),
    Color(0xFFFFE0A8)
)

/**
 * 全部打卡完成时的庆祝动画：一片纸屑从上往下落，边落边淡出。
 * 纯 Compose Canvas 绘制，不依赖第三方库，播放约 2.6 秒后自动结束。
 */
@Composable
fun CelebrationOverlay(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val random = Random(seed = 20261115)
        List(56) {
            Particle(
                x = random.nextFloat(),
                startY = -random.nextFloat() * 0.3f,
                size = 4f + random.nextFloat() * 6f,
                delay = random.nextFloat() * 0.35f,
                drift = 0.4f + random.nextFloat() * 1.2f,
                phase = random.nextFloat() * 6.28f,
                color = celebrationPalette[random.nextInt(celebrationPalette.size)],
                isRect = random.nextBoolean()
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 2600, easing = LinearEasing))
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val overall = progress.value
        particles.forEach { particle ->
            val t = ((overall - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val y = size.height * (particle.startY + t * 1.15f)
            val sway = sin(t * 7f + particle.phase) * 18.dp.toPx() * particle.drift
            val x = size.width * particle.x + sway
            val alpha = (1f - t * t).coerceIn(0f, 1f)
            val radius = particle.size.dp.toPx()
            if (particle.isRect) {
                rotate(degrees = t * 420f + particle.phase * 30f, pivot = Offset(x, y)) {
                    drawRect(
                        color = particle.color.copy(alpha = alpha),
                        topLeft = Offset(x - radius, y - radius / 2f),
                        size = Size(radius * 2f, radius * 1.1f)
                    )
                }
            } else {
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
