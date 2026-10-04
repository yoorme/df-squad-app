package com.yoorme.squadsignup.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yoorme.squadsignup.MainActivity
import com.yoorme.squadsignup.R

object Notifier {
    const val CHANNEL_EVENTS = "events"
    const val CHANNEL_ANNOUNCEMENTS = "announcements"

    const val EXTRA_OPEN = "open"          // event | announcement
    const val EXTRA_TARGET_ID = "targetId"

    fun createChannels(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EVENTS, "赛事通知", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "新比赛发布与比赛临近提醒" }
        )
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ANNOUNCEMENTS, "公告通知", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "战队新公告" }
        )
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun tapIntent(context: Context, open: String, id: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN, open)
            id?.let { putExtra(EXTRA_TARGET_ID, it) }
        }
        return PendingIntent.getActivity(
            context,
            (open + id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notify(context: Context, channel: String, title: String, text: String, open: String, targetId: String?, notificationId: Int) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tapIntent(context, open, targetId))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // 极端情况下权限在 canNotify() 检查与发送之间被撤销：忽略本次通知
        }
    }
}
