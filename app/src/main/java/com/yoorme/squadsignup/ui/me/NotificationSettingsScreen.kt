package com.yoorme.squadsignup.ui.me

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.NotificationSettings
import com.yoorme.squadsignup.core.NotificationSettingsPatch
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LoadingBox
import com.yoorme.squadsignup.ui.components.SettingSwitchRow
import kotlinx.coroutines.launch

private val LEAD_OPTIONS = listOf(15, 30, 60, 120)

// 通知设置：三项独立开关 + 提前提醒时间
// 设置保存在服务端（按用户），推送与轮询提醒都按此执行
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(repo: Repo, onBack: () -> Unit) {
    var settings by remember { mutableStateOf<NotificationSettings?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                settings = repo.notificationSettings()
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(Unit) { load() }

    fun update(patch: NotificationSettingsPatch) {
        val current = settings ?: return
        // 乐观更新 + 服务端确认
        settings = NotificationSettings(
            userId = current.userId,
            notifyNewEvent = patch.notifyNewEvent ?: current.notifyNewEvent,
            notifyEventReminder = patch.notifyEventReminder ?: current.notifyEventReminder,
            reminderLeadMinutes = patch.reminderLeadMinutes ?: current.reminderLeadMinutes,
            notifyAnnouncement = patch.notifyAnnouncement ?: current.notifyAnnouncement,
        )
        scope.launch {
            try {
                settings = repo.patchNotificationSettings(patch)
            } catch (e: ApiException) {
                error = e.message
                load()
            } catch (e: Exception) {
                error = "网络错误，稍后自动重试"
                load()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        when {
            error != null && settings == null -> ErrorBox(error ?: "", Modifier.padding(padding), retry = { load() })
            settings == null -> LoadingBox(Modifier.padding(padding))
            else -> {
                val s = settings!!
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        Column {
                            SettingSwitchRow(
                                title = "新比赛通知",
                                subtitle = "管理员发布新比赛时推送提醒",
                                checked = s.notifyNewEvent,
                            ) { update(NotificationSettingsPatch(notifyNewEvent = it)) }
                            SettingSwitchRow(
                                title = "比赛临近提醒",
                                subtitle = "已报名的比赛开始前推送提醒",
                                checked = s.notifyEventReminder,
                            ) { update(NotificationSettingsPatch(notifyEventReminder = it)) }
                            SettingSwitchRow(
                                title = "新公告通知",
                                subtitle = "发布新公告时推送提醒",
                                checked = s.notifyAnnouncement,
                            ) { update(NotificationSettingsPatch(notifyAnnouncement = it)) }
                        }
                    }

                    if (s.notifyEventReminder) {
                        Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("提前提醒时间", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "已报名比赛开始前多久提醒你",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                LEAD_OPTIONS.forEach { minutes ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = s.reminderLeadMinutes == minutes,
                                            onClick = { update(NotificationSettingsPatch(reminderLeadMinutes = minutes)) },
                                        )
                                        Text("${minutes} 分钟前")
                                    }
                                }
                            }
                        }
                    }

                    Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("送达说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "当前版本由 App 定期（最短 15 分钟）检查并弹出本地通知；部分手机省电策略可能延迟送达，请允许本应用后台运行。" +
                                    "接入厂商推送通道后，App 不运行也能即时收到通知。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}
