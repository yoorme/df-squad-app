package com.yoorme.squadsignup.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 开机重启轮询任务（WorkManager 默认不跨重启持久执行入队）
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PollWorker.schedule(context)
        }
    }
}
