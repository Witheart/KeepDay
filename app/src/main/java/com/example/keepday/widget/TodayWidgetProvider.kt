package com.example.keepday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.keepday.MainActivity
import com.example.keepday.R
import com.example.keepday.data.Habit
import com.example.keepday.data.HabitStore
import java.time.LocalDate

/**
 * 桌面小组件：展示今日打卡进度和待完成的习惯。
 * 每行习惯都可直接点击打卡（无需打开应用）；点空白处打开应用。
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

        private val rowContainers = intArrayOf(
            R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3, R.id.widget_row_4
        )
        private val rowEmojis = intArrayOf(
            R.id.widget_row_1_emoji, R.id.widget_row_2_emoji,
            R.id.widget_row_3_emoji, R.id.widget_row_4_emoji
        )
        private val rowNames = intArrayOf(
            R.id.widget_row_1_name, R.id.widget_row_2_name,
            R.id.widget_row_3_name, R.id.widget_row_4_name
        )
        private val rowStatuses = intArrayOf(
            R.id.widget_row_1_status, R.id.widget_row_2_status,
            R.id.widget_row_3_status, R.id.widget_row_4_status
        )

        private const val COLOR_DONE = 0xFFFFE0A8.toInt()
        private const val COLOR_PENDING = 0xCCFFFFFF.toInt()
        private const val COLOR_TEXT = 0xFFFFFFFF.toInt()

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

            val empty = habits.isEmpty()
            views.setViewVisibility(R.id.widget_empty, if (empty) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_hint, if (empty) View.GONE else View.VISIBLE)

            for (index in 0 until MAX_ROWS) {
                val habit = ordered.getOrNull(index)
                if (habit == null) {
                    views.setViewVisibility(rowContainers[index], View.GONE)
                    continue
                }
                val isDone = habit.isDone(today)
                views.setViewVisibility(rowContainers[index], View.VISIBLE)
                views.setTextViewText(rowEmojis[index], habit.emoji)
                views.setTextViewText(rowNames[index], habit.name)
                views.setTextColor(rowNames[index], COLOR_TEXT)
                views.setTextViewText(rowStatuses[index], if (isDone) "✓" else "○")
                views.setTextColor(
                    rowStatuses[index],
                    if (isDone) COLOR_DONE else COLOR_PENDING
                )
                // 每行单独绑定打卡动作，data 保证 PendingIntent 互不覆盖
                views.setOnClickPendingIntent(
                    rowContainers[index],
                    togglePendingIntent(context, habit.id)
                )
            }

            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            return views
        }

        private fun togglePendingIntent(context: Context, habitId: String): PendingIntent {
            val intent = Intent(context, WidgetCheckInReceiver::class.java).apply {
                action = WidgetCheckInReceiver.ACTION_TOGGLE
                putExtra(WidgetCheckInReceiver.EXTRA_HABIT_ID, habitId)
                data = Uri.parse("keepday://habit/$habitId")
            }
            return PendingIntent.getBroadcast(
                context,
                habitId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
