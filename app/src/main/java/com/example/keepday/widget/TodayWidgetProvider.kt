package com.example.keepday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.keepday.MainActivity
import com.example.keepday.R
import com.example.keepday.data.Habit
import com.example.keepday.data.HabitStore
import java.time.LocalDate

/**
 * 桌面小组件：展示今日打卡进度和待完成的习惯。
 * 数据变化时由应用主动刷新；另外每 30 分钟系统会拉一次，用于跨天后自动更新。
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildViews(context)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    companion object {

        private const val MAX_ROWS = 4

        /** 供应用内部在数据变化后刷新所有已添加的组件。 */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TodayWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            HabitStore.init(context)
            val today = LocalDate.now()
            val habits = HabitStore.habits.value
            val ordered = habits.sortedWith(
                compareByDescending<Habit> { it.pinned }.thenBy { it.isDone(today) }
            )
            val done = habits.count { it.isDone(today) }

            val views = RemoteViews(context.packageName, R.layout.widget_today)
            views.setTextViewText(
                R.id.widget_progress_text,
                if (habits.isEmpty()) "0/0" else "$done/${habits.size}"
            )
            val percent = if (habits.isEmpty()) 0 else done * 100 / habits.size
            views.setProgressBar(R.id.widget_progress, 100, percent, false)

            val content = when {
                habits.isEmpty() -> "还没有习惯\n点一下，添加第一个目标"
                else -> buildString {
                    ordered.take(MAX_ROWS).forEach { habit ->
                        val mark = if (habit.isDone(today)) "✓" else "○"
                        append(mark).append(' ').append(habit.emoji).append(' ')
                            .append(habit.name).append('\n')
                    }
                    if (ordered.size > MAX_ROWS) {
                        append("…还有 ").append(ordered.size - MAX_ROWS).append(" 个")
                    }
                }.trimEnd('\n')
            }
            views.setTextViewText(R.id.widget_list, content)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }
    }
}
