package com.example.keepday.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.keepday.data.HabitStore

/** 桌面小组件里点某一行时触发，直接完成/取消当天的打卡。 */
class WidgetCheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TOGGLE) return
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        HabitStore.init(context)
        HabitStore.toggle(habitId)
        WidgetUpdater.updateAll(context)
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.keepday.action.WIDGET_TOGGLE"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
