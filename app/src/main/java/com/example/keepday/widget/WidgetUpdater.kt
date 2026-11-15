package com.example.keepday.widget

import android.content.Context

/** 统一刷新所有尺寸的桌面组件。 */
object WidgetUpdater {
    fun updateAll(context: Context) {
        TodayWidgetProvider.updateAll(context)
        CompactWidgetProvider.updateAll(context)
    }
}
