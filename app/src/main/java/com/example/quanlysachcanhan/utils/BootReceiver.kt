package com.example.quanlysachcanhan.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pref = PreferenceManager(context)
            if (pref.reminderEnabled) {
                ReadingReminderHelper.scheduleReminder(
                    context,
                    pref.reminderHour,
                    pref.reminderMinute
                )
            }
        }
    }
}
