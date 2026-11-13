package com.example.keepday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.keepday.data.HabitStore
import com.example.keepday.ui.HabitScreen
import com.example.keepday.ui.theme.KeepDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HabitStore.init(this)
        setContent {
            KeepDayTheme {
                KeepDayApp()
            }
        }
    }
}

@Composable
private fun KeepDayApp() {
    val habits by HabitStore.habits.collectAsState()
    HabitScreen(
        habits = habits,
        onToggle = { HabitStore.toggle(it) },
        onAddHabit = { name, emoji, accentIndex -> HabitStore.add(name, emoji, accentIndex) },
        onDelete = { HabitStore.delete(it) }
    )
}
