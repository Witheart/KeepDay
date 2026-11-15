package com.example.keepday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.keepday.data.HabitStore
import com.example.keepday.data.SettingsStore
import com.example.keepday.ui.KeepDayApp
import com.example.keepday.ui.theme.KeepDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitStore.init(this)
        SettingsStore.init(this)
        setContent {
            KeepDayTheme {
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
