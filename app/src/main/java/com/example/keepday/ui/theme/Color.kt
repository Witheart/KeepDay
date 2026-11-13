package com.example.keepday.ui.theme

import androidx.compose.ui.graphics.Color

// 品牌主色：紫罗兰 + 青绿，冷色打底，暖色做强调
val Violet = Color(0xFF6C5CE7)
val VioletDeep = Color(0xFF4B3FD1)
val Teal = Color(0xFF11B3A5)
val Amber = Color(0xFFFFA45B)

// 浅色主题
val PaperLight = Color(0xFFF6F5FB)
val SurfaceLight = Color(0xFFFFFFFF)
val InkLight = Color(0xFF1B1931)
val MutedLight = Color(0xFF6F6B8A)
val OutlineLight = Color(0xFFE6E2F3)
val SurfaceVariantLight = Color(0xFFEDEAF9)

// 深色主题
val PaperDark = Color(0xFF0E0D16)
val SurfaceDark = Color(0xFF191725)
val InkDark = Color(0xFFF2F0FC)
val MutedDark = Color(0xFFA5A1C0)
val OutlineDark = Color(0xFF2A2740)
val SurfaceVariantDark = Color(0xFF232134)

/** 习惯卡片使用的渐变色对。 */
data class AccentGradient(val start: Color, val end: Color)

val HabitAccents = listOf(
    AccentGradient(Color(0xFF7C6BFF), Color(0xFFAD95FF)),
    AccentGradient(Color(0xFF00C2A8), Color(0xFF54D98C)),
    AccentGradient(Color(0xFFFFB25E), Color(0xFFFF8A5B)),
    AccentGradient(Color(0xFFFF7A9C), Color(0xFFFFA6BE)),
    AccentGradient(Color(0xFF4FA8FF), Color(0xFF6ED0FF)),
    AccentGradient(Color(0xFFB08CFF), Color(0xFFF0A6FF))
)

fun accentOf(index: Int): AccentGradient = HabitAccents[index.mod(HabitAccents.size)]
