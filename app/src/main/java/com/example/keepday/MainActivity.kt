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
}
