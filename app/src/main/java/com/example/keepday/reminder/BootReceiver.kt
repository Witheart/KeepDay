package com.example.keepday.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.keepday.data.SettingsStore

/** 重启后闹钟会被系统清空，开机时按用户设置重新排一次。 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        SettingsStore.init(context)
        val setting = SettingsStore.reminder.value
        if (setting.enabled) {
            ReminderScheduler.schedule(context, setting.hour, setting.minute)
        }
    }
}
