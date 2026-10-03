package com.yoorme.squadsignup.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.yoorme.squadsignup.core.AssignMove
import com.yoorme.squadsignup.core.EventDetail
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SquadMember
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LoadingBox
import kotlinx.coroutines.launch

// 管理员分队调整：点击队员 → 选择目标分队/替补
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignScreen(
    repo: Repo,
    eventId: String,
    onDone: () -> Unit,
) {
    var detail by remember { mutableStateOf<EventDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<SquadMember?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                detail = repo.eventDetail(eventId)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(eventId) { load() }

    fun move(registrationId: String, targetSquadId: String?) {
        scope.launch {
            try {
                repo.assign(eventId, listOf(AssignMove(registrationId, targetSquadId)))
                selected = null
                load()
            } catch (e: ApiException) {
                message = e.message
                selected = null
            } catch (e: Exception) {
                message = "网络错误"
                selected = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分队调整") },
                navigationIcon = { TextButton(onClick = onDone) { Text("完成") } },
            )
        },
    ) { padding ->
        when {
            error != null -> ErrorBox(error ?: "", Modifier.padding(padding))
            detail == null -> LoadingBox(Modifier.padding(padding))
            else -> {
                val d = detail!!
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(d.squads, key = { it.id }) { squad ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "${squad.index} 队 · ${squad.nature.name}（${squad.members.size}/${squad.capacity}）",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(6.dp))
                                squad.members.forEach { m ->
                                    TextButton(onClick = { selected = m }) {
                                        Text(m.nickname)
                                    }
                                }
                                if (squad.members.isEmpty()) {
                                    Text("空", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    items(listOf("subs")) { _ ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("替补（${d.substitutes.size}）", fontWeight = FontWeight.SemiBold)
                                d.substitutes.forEach { m ->
                                    TextButton(onClick = { selected = m }) {
                                        Text(m.nickname)
                                    }
                                }
                                if (d.substitutes.isEmpty()) {
                                    Text("空", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    message?.let {
                        item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }

                // 移动对话框
                selected?.let { member ->
                    AlertDialog(
                        onDismissRequest = { selected = null },
                        title = { Text("移动「${member.nickname}」") },
                        text = {
                            Column {
                                d.squads.forEach { squad ->
                                    val isCurrent = squad.members.any { it.registrationId == member.registrationId }
                                    val full = squad.members.size >= squad.capacity
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        TextButton(
                                            onClick = { move(member.registrationId, squad.id) },
                                            enabled = !isCurrent && (!full || isCurrent),
                                        ) {
                                            Text(
                                                "${squad.index} 队 · ${squad.nature.name}" +
                                                    if (isCurrent) "（当前）" else if (full) "（已满）" else "",
                                            )
                                        }
                                    }
                                }
                                if (!member.let { d.substitutes.any { s -> s.registrationId == it.registrationId } }) {
                                    TextButton(onClick = { move(member.registrationId, null) }) {
                                        Text("移入替补")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { selected = null }) { Text("取消") }
                        },
                    )
                }
            }
        }
    }
}
