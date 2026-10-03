package com.yoorme.squadsignup

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import com.yoorme.squadsignup.notify.Notifier
import com.yoorme.squadsignup.notify.PollWorker
import com.yoorme.squadsignup.notify.PushManager

class SquadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        // 极光的 :pushcore 独立进程也会创建 Application；
        // WorkManager 仅主进程初始化，轮询调度只在主进程执行
        if (isMainProcess()) {
            // 登录后 App 内会再次调度（游客状态下轮询会自动空转跳过）
            PollWorker.schedule(this)
        }
        // 初始化极光推送（厂商通道就绪后，App 不运行也能收到通知）
        PushManager.init(this)
    }

    private fun isMainProcess(): Boolean {
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val am = getSystemService(ACTIVITY_SERVICE) as? ActivityManager
            am?.runningAppProcesses?.firstOrNull { it.pid == Process.myPid() }?.processName
        }
        return name == null || name == packageName
    }
}
