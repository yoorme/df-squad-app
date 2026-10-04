package com.yoorme.squadsignup.notify

import android.content.Context
import android.content.Intent
import cn.jpush.android.api.NotificationMessage
import cn.jpush.android.service.JPushMessageReceiver
import com.yoorme.squadsignup.MainActivity
import com.yoorme.squadsignup.core.ApiClient
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

// 极光推送事件回调：registration_id 上报 + 通知点击深链
class PushMessageReceiver : JPushMessageReceiver() {

    // 点击通知栏：直达对应赛事/公告
    override fun onNotifyMessageOpened(context: Context?, message: NotificationMessage?) {
        val extrasJson = message?.notificationExtras ?: return
        val obj = runCatching { ApiClient.json.parseToJsonElement(extrasJson).jsonObject }.getOrNull() ?: return
        val type = (obj["type"] as? JsonPrimitive)?.content
        val id = ((obj["eventId"] ?: obj["announcementId"]) as? JsonPrimitive)?.content ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(Notifier.EXTRA_OPEN, if (type == "NEW_ANNOUNCEMENT") "announcement" else "event")
            putExtra(Notifier.EXTRA_TARGET_ID, id)
        }
        context?.startActivity(intent)
    }
}
