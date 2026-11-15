package com.example.keepday.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import com.example.keepday.MainActivity
import com.example.keepday.R
import com.example.keepday.data.HabitStore
import java.time.LocalDate

/**
 * 2×2 紧凑小组件：环形进度 + 今日完成数。
 * RemoteViews 不支持 Canvas，所以圆环是先在内存里画成 Bitmap 再塞进 ImageView 的。
 */
class CompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = buildViews(context)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    companion object {

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CompactWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { id -> manager.updateAppWidget(id, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            HabitStore.init(context)
            val today = LocalDate.now()
            HabitStore.refreshStrict(today)
            val habits = HabitStore.habits.value
            val done = habits.count { it.isDone(today) }
            val ratio = if (habits.isEmpty()) 0f else done.toFloat() / habits.size

            val views = RemoteViews(context.packageName, R.layout.widget_today_compact)
            views.setImageViewBitmap(R.id.widget_compact_ring, ringBitmap(context, ratio))
            views.setTextViewText(R.id.widget_compact_count, "$done/${habits.size}")
            views.setTextViewText(
                R.id.widget_compact_label,
                when {
                    habits.isEmpty() -> "点一下添加习惯"
                    done == habits.size -> "今天全部完成 🎉"
                    else -> "还剩 ${habits.size - done} 个"
                }
            )
            views.setOnClickPendingIntent(R.id.widget_compact_root, openApp(context))
            return views
        }

        private fun openApp(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun ringBitmap(
            context: Context,
            ratio: Float,
            sizeDp: Int = 86,
            strokeDp: Int = 9
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val size = (sizeDp * density).toInt().coerceAtLeast(1)
            val stroke = strokeDp * density
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val inset = stroke / 2f
            val rect = RectF(inset, inset, size - inset, size - inset)

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = 0x3DFFFFFF
            }
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)

            val progress = ratio.coerceIn(0f, 1f)
            if (progress > 0f) {
                val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    strokeCap = Paint.Cap.ROUND
                    shader = LinearGradient(
                        0f, 0f, size.toFloat(), size.toFloat(),
                        0xFFFFE0A8.toInt(), 0xFFFFFFFF.toInt(), Shader.TileMode.CLAMP
                    )
                }
                canvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)
            }
            return bitmap
        }
    }
}
