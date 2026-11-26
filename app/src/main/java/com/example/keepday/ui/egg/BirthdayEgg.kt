package com.example.keepday.ui.egg

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sin
import kotlin.random.Random

/**
 * 生日彩蛋：国历 11 月 27 日，或农历十月二十六。
 */
object BirthdayEgg {

    private const val MONTH = 11
    private const val DAY = 27
    private const val LUNAR_MONTH = 10
    private const val LUNAR_DAY = 26

    fun isGregorianBirthday(date: LocalDate): Boolean =
        date.monthValue == MONTH && date.dayOfMonth == DAY

    /** 农历十月二十六；device 上通过 android.icu 换算，异常时按不匹配处理。 */
    fun isLunarBirthday(date: LocalDate): Boolean = runCatching {
        val calendar = android.icu.util.ChineseCalendar()
        calendar.time = java.util.Date.from(
            date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        )
        val month = calendar.get(android.icu.util.ChineseCalendar.MONTH) + 1
        val day = calendar.get(android.icu.util.ChineseCalendar.DAY_OF_MONTH)
        month == LUNAR_MONTH && day == LUNAR_DAY
    }.getOrDefault(false)

    fun isBirthday(date: LocalDate): Boolean =
        isGregorianBirthday(date) || isLunarBirthday(date)
}

private data class Mote(
    val x: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val drift: Float,
    val phase: Float,
    val color: Color
)

private val motesPalette = listOf(
    Color(0xFFFFC2D4),
    Color(0xFFFFE3B0),
    Color(0xFFC9B8FF),
    Color(0xFFFFD9EC)
)

private fun fade(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)

/**
 * 全屏生日动画：深色夜空里光点缓缓上升，标题逐层浮现，约 8 秒后自动淡出。
 * 点击任意位置可提前关闭。
 */
@Composable
fun BirthdayCelebration(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val motes = remember {
        val random = Random(seed = 1127)
        List(46) {
            Mote(
                x = random.nextFloat(),
                startY = random.nextFloat(),
                size = 1.5f + random.nextFloat() * 3.5f,
                speed = 0.10f + random.nextFloat() * 0.22f,
                drift = (random.nextFloat() - 0.5f) * 0.08f,
                phase = random.nextFloat() * 6.28f,
                color = motesPalette[random.nextInt(motesPalette.size)]
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 8200, easing = LinearEasing))
        onFinished()
    }

    // 深色全屏动画期间把系统栏图标切成浅色，关闭后还原
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = runCatching {
            val window = (view.parent as DialogWindowProvider).window
            WindowCompat.getInsetsController(window, view)
        }.getOrNull()
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                if (previousStatus != null) controller.isAppearanceLightStatusBars = previousStatus
                if (previousNav != null) controller.isAppearanceLightNavigationBars = previousNav
            }
        }
    }

    Dialog(
        onDismissRequest = onFinished,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val t = progress.value
        val overlayAlpha = fade(t, 0f, 0.10f) * (1f - fade(t, 0.90f, 1f))
        val nameAlpha = fade(t, 0.08f, 0.22f) * (1f - fade(t, 0.90f, 1f))
        val wishAlpha = fade(t, 0.18f, 0.32f) * (1f - fade(t, 0.90f, 1f))
        val noteAlpha = fade(t, 0.30f, 0.44f) * (1f - fade(t, 0.90f, 1f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF241634),
                            Color(0xFF150E22),
                            Color(0xFF0B0712)
                        ),
                        radius = 1200f
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinished
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 光点缓慢上升
                motes.forEach { mote ->
                    val travelled = (t * mote.speed * 3f + mote.startY) % 1f
                    val y = size.height * (1.06f - travelled)
                    val x = size.width * (mote.x + mote.drift * sin(t * 5f + mote.phase))
                    val alpha = 0.20f * overlayAlpha * (0.4f + 0.6f * sin(t * 6f + mote.phase))
                        .coerceIn(0f, 1f)
                    drawCircle(
                        color = mote.color.copy(alpha = alpha.coerceAtLeast(0f)),
                        radius = mote.size.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // 交错的星芒
                repeat(10) { index ->
                    val phase = index * 0.63f
                    val sparkleX = size.width * (0.12f + 0.076f * index)
                    val sparkleY = size.height * (0.22f + 0.09f * sin(phase + t * 2f))
                    val twinkle = (sin(t * 14f + phase) + 1f) / 2f
                    val alpha = 0.5f * overlayAlpha * twinkle
                    val armLength = (3f + 5f * twinkle).dp.toPx()
                    drawLine(
                        color = Color(0xFFFFF3E0).copy(alpha = alpha.coerceIn(0f, 1f)),
                        start = Offset(sparkleX - armLength, sparkleY),
                        end = Offset(sparkleX + armLength, sparkleY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFFFF3E0).copy(alpha = alpha.coerceIn(0f, 1f)),
                        start = Offset(sparkleX, sparkleY - armLength),
                        end = Offset(sparkleX, sparkleY + armLength),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // 光环跟着名字走：圆环绘制在这个 Box 自己的画布上，
                // 名字由 Box 居中排版，所以两者天然同心，不会错位
                Box(
                    modifier = Modifier
                        .size(216.dp)
                        .drawBehind {
                            val ringRadius = size.minDimension / 2f * 0.92f
                            val ringCenter = center
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x3DFFC2D4),
                                        Color(0x14FFC2D4),
                                        Color.Transparent
                                    ),
                                    center = ringCenter,
                                    radius = ringRadius * 1.45f
                                ),
                                radius = ringRadius * 1.45f,
                                center = ringCenter
                            )
                            drawCircle(
                                color = Color(0xFFFFC2D4).copy(alpha = 0.38f * overlayAlpha),
                                radius = ringRadius,
                                center = ringCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "霓霓",
                        color = Color(0xFFFFF4F8).copy(alpha = nameAlpha),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "生 日 快 乐",
                    color = Color(0xFFFFD9E6).copy(alpha = wishAlpha),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "愿每一个认真记下的日子，都被温柔以待",
                    color = Color(0xFFE7DFF5).copy(alpha = noteAlpha),
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
