package com.example.keepday.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E3FF),
    onPrimaryContainer = Color(0xFF221B5C),
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Amber,
    onTertiary = Color.White,
    background = PaperLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    error = Color(0xFFE5484D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB5A8FF),
    onPrimary = Color(0xFF241C5C),
    primaryContainer = Color(0xFF3A2F8F),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = Color(0xFF4EDBCE),
    onSecondary = Color(0xFF003731),
    tertiary = Color(0xFFFFC08A),
    onTertiary = Color(0xFF3A2000),
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    error = Color(0xFFFF6B6B)
)

@Composable
fun KeepDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 默认关闭动态取色，保证品牌配色稳定，不被壁纸颜色冲淡
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
