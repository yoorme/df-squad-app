package com.yoorme.squadsignup.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.AdminUser
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.MemberProfile
import com.yoorme.squadsignup.core.PinyinSearch
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.ConfirmDialog
import com.yoorme.squadsignup.ui.components.EmptyBox
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LoadingBox
import com.yoorme.squadsignup.ui.components.SearchField
import kotlinx.coroutines.launch

@Composable
fun MembersScreen(repo: Repo, openMember: (String) -> Unit) {
    var members by remember { mutableStateOf<List<MemberProfile>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                members = repo.members()
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "搜索昵称/用户名（支持中文与拼音）",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        val source = members
        when {
            error != null -> ErrorBox(error ?: "", retry = { load() })
            source == null -> LoadingBox()
            else -> {
                val filtered = source.filter {
                    PinyinSearch.matches(query, it.nickname) || PinyinSearch.matches(query, it.username)
                }
                if (filtered.isEmpty()) {
                    EmptyBox(if (query.isBlank()) "暂无队员" else "未找到匹配的队员")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { m ->
                            Card(onClick = { openMember(m.id) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Row {
                                        Text(
                                            m.nickname,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        if (m.role == "ADMIN") {
                                            Text(
                                                "  管理员",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    Text(
                                        m.username,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    repo: Repo,
    memberId: String,
    isAdmin: Boolean,
    onBack: () -> Unit,
) {
    var member by remember { mutableStateOf<MemberProfile?>(null) }
    var adminInfo by remember { mutableStateOf<AdminUser?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var resetOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                member = repo.member(memberId)
                adminInfo = if (isAdmin) repo.adminUsers().find { it.id == memberId } else null
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(memberId, isAdmin) { load() }

    fun act(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                load()
            } catch (e: ApiException) {
                message = e.message
            } catch (e: Exception) {
                message = "网络错误"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("队员信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            error != null -> ErrorBox(error ?: "", Modifier.padding(padding), retry = { load() })
            member == null -> LoadingBox(Modifier.padding(padding))
            else -> {
                val m = member!!
                Column(
                    Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                ) {
                    Text(m.nickname, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        m.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${if (m.role == "ADMIN") "管理员" else "队员"} · 加入于 ${TimeFmt.full(m.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))

                    TagSection("职责", m.duties.map { it.name })
                    TagSection("能力", m.abilities.map { it.name })
                    TagSection("擅长干员", m.operators.map { it.name })

                    if (isAdmin) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "管理员操作",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                act { repo.patchUser(m.id, if (m.role == "ADMIN") "MEMBER" else "ADMIN", null) }
                            }) { Text(if (m.role == "ADMIN") "设为队员" else "设为管理员") }
                            if (adminInfo != null) {
                                OutlinedButton(onClick = {
                                    act { repo.patchUser(m.id, null, !(adminInfo?.disabled ?: false)) }
                                }) { Text(if (adminInfo?.disabled == true) "启用" else "禁用") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { resetOpen = true }) { Text("重置密码") }
                            OutlinedButton(onClick = { deleteOpen = true }) {
                                Text("删除账号", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        message?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (resetOpen) {
        var pwd by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resetOpen = false },
            title = { Text("重置「${member?.nickname ?: ""}」的密码") },
            text = {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("新密码（至少 6 位）") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pwd.length >= 6) {
                        resetOpen = false
                        act { repo.resetPassword(memberId, pwd) }
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { resetOpen = false }) { Text("取消") } },
        )
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = "删除账号",
            message = "确定删除「${member?.nickname ?: ""}」吗？其报名记录将被清除，发布的公告/赛事/邀请码将转交给当前管理员。",
            confirmText = "删除",
            danger = true,
            onDismiss = { deleteOpen = false },
            onConfirm = {
                deleteOpen = false
                scope.launch {
                    try {
                        repo.deleteUser(memberId)
                        onBack()
                    } catch (e: ApiException) {
                        message = e.message
                    } catch (e: Exception) {
                        message = "网络错误"
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(title: String, values: List<String>) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (values.isEmpty()) {
                Text(
                    "未填写",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    values.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                }
            }
        }
    }
}
