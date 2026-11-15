package com.example.keepday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.example.keepday.data.HabitStore
import com.example.keepday.data.SettingsStore
import com.example.keepday.data.ThemeMode
import com.example.keepday.ui.KeepDayApp
import com.example.keepday.ui.theme.KeepDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitStore.init(this)
        SettingsStore.init(this)
        setContent {
            val mode by SettingsStore.themeMode.collectAsState()
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // 手动切换明暗时，状态栏/导航栏图标颜色也要跟着反转
            LaunchedEffect(darkTheme) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            KeepDayTheme(darkTheme = darkTheme) {
                KeepDayApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 长时间挂在后台再回来时，跨天漏打需要立刻清算
        HabitStore.refreshStrict()
    }
}
