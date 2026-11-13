package com.example.keepday.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.keepday.MainActivity
import com.example.keepday.R
import com.example.keepday.data.HabitStore
import java.time.LocalDate

/** 每日提醒：根据当天完成情况发出通知。 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderScheduler.ACTION_REMINDER) return

        HabitStore.init(context)
        val habits = HabitStore.habits.value
        val today = LocalDate.now()
        val pending = habits.count { !it.isDone(today) }

        val title: String
        val text: String
        when {
            habits.isEmpty() -> {
                title = "还没有习惯"
                text = "花一分钟添加一个小目标，从今天开始坚持"
            }

            pending == 0 -> {
                title = "今天的习惯都完成了 🎉"
                text = "连续坚持的记录又多了一天，很棒"
            }

            else -> {
                title = "还有 $pending 个习惯没打卡"
                text = "现在花几分钟完成它们，别让连续记录断掉"
            }
        }

        notify(context, title, text)
    }

    private fun notify(context: Context, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日打卡提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "每天提醒完成当天的习惯打卡" }
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private companion object {
        const val CHANNEL_ID = "daily_reminder"
        const val NOTIFICATION_ID = 2001
    }
}
