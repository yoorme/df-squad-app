package com.yoorme.squadsignup.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.core.TimeFmt
import java.time.Instant
import java.util.concurrent.TimeUnit

// 轮询兜底通知：App 不在前台/被杀死时，由 WorkManager 定期唤醒检查
//   1. 新比赛发布     → 通知（开关：notifyNewEvent）
//   2. 新公告发布     → 通知（开关：notifyAnnouncement）
//   3. 已报名比赛临近 → 通知（开关：notifyEventReminder，提前量 reminderLeadMinutes）
// 注意：国产 ROM 的省电策略可能延迟或拦截 WorkManager 任务；
//       配置极光推送（JPUSH_APPKEY）后可由厂商通道送达，彻底摆脱该限制（见 docs/JPush接入指南.md）。
class PollWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = SessionStore(applicationContext)
        val repo = Repo(store)
        try {
            if (store.currentToken() == null) return Result.success()

            val settings = repo.notificationSettings()

            // 上报/刷新极光设备绑定（幂等；SDK 晚于登录注册成功也能在下一轮补上）
            val rid = PushManager.registrationId(applicationContext)
            if (rid.isNotBlank()) {
                runCatching {
                    repo.registerDevice(
                        com.yoorme.squadsignup.core.DeviceRegisterRequest(
                            registrationId = rid,
                            model = android.os.Build.MODEL,
                            appVersion = "1.1.0",
                        )
                    )
                }
            }

            val now = Instant.now()

            // 1) 赛事：新比赛通知 + 临近提醒
            val events = runCatching { repo.events("UPCOMING") }.getOrDefault(emptyList())
            val lastEventSeen = store.lastEventSeen()
            val newestEvent = events.maxByOrNull { it.createdAt }
            if (newestEvent != null) {
                if (lastEventSeen != null && newestEvent.createdAt > lastEventSeen) {
                    // 服务端按 eventTime 倒序返回（不是 createdAt），必须自己按发布时间排序，
                    // 否则 take(3) 可能漏掉真正新发布的比赛，而水位线已被推高导致永久不通知
                    val fresh = events.filter { it.createdAt > lastEventSeen }
                        .sortedByDescending { it.createdAt }
                        .take(3)
                    if (settings.notifyNewEvent) {
                        for (e in fresh) {
                            Notifier.notify(
                                applicationContext, Notifier.CHANNEL_EVENTS,
                                "新比赛发布", e.title, "event", e.id,
                                notificationId = ("ev:" + e.id).hashCode()
                            )
                        }
                    }
                }
                store.markEventSeen(newestEvent.createdAt)
            }

            // 2) 公告：新公告通知
            val announcements = runCatching { repo.announcements("normal") }.getOrDefault(emptyList())
            val lastAnnSeen = store.lastAnnouncementSeen()
            val newestAnn = announcements.maxByOrNull { it.createdAt }
            if (newestAnn != null) {
                if (lastAnnSeen != null && newestAnn.createdAt > lastAnnSeen && settings.notifyAnnouncement) {
                    val fresh = announcements.filter { it.createdAt > lastAnnSeen }.take(3)
                    for (a in fresh) {
                        Notifier.notify(
                            applicationContext, Notifier.CHANNEL_ANNOUNCEMENTS,
                            "新公告", a.title, "announcement", a.id,
                            notificationId = ("an:" + a.id).hashCode()
                        )
                    }
                }
                store.markAnnouncementSeen(newestAnn.createdAt)
            }

            // 3) 已报名比赛的临近提醒
            if (settings.notifyEventReminder) {
                for (e in events) {
                    val mine = e.myRegistration ?: continue
                    val eventInstant = TimeFmt.parse(e.eventTime) ?: continue
                    if (!eventInstant.isAfter(now)) continue
                    val minutesLeft = TimeFmt.minutesUntil(e.eventTime) ?: continue
                    if (minutesLeft > settings.reminderLeadMinutes) continue

                    val key = "reminder:${e.id}:${settings.reminderLeadMinutes}"
                    if (key in store.notifiedKeys()) continue
                    Notifier.notify(
                        applicationContext, Notifier.CHANNEL_EVENTS,
                        "比赛即将开始",
                        "${e.title} 将于 ${formatMinutes(minutesLeft)} 后开始（你已报名）",
                        "event", e.id,
                        notificationId = key.hashCode()
                    )
                    store.addNotifiedKey(key)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            // 网络失败等场景静默结束，等待下一轮
            return Result.success()
        }
    }

    private fun formatMinutes(minutes: Int): String =
        if (minutes >= 60) "${minutes / 60} 小时" else "$minutes 分钟"

    companion object {
        const val WORK_NAME = "squad-poll"

        // 15 分钟是 WorkManager 周期任务的最小间隔
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
