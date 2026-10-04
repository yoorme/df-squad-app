package com.yoorme.squadsignup.ui.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.AuthRequiredException
import com.yoorme.squadsignup.core.MemberProfile
import com.yoorme.squadsignup.core.MePatchRequest
import com.yoorme.squadsignup.core.OptionsResponse
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.core.SessionUser
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LoadingBox
import com.yoorme.squadsignup.ui.components.PrefixedInput
import kotlinx.coroutines.launch

@Composable
fun MeScreen(
    repo: Repo,
    store: SessionStore,
    session: SessionUser?,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenInvitations: () -> Unit,
    onOpenTags: () -> Unit,
    onSwitchServer: () -> Unit,
    onLogout: () -> Unit,
    refreshToken: Int,
) {
    var me by remember { mutableStateOf<MemberProfile?>(null) }
    var options by remember { mutableStateOf<OptionsResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // 保存类操作的错误单独提示，避免像加载失败那样整页替换成 ErrorBox
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 请求发在 LaunchedEffect 的协程里：refresh 变化时旧请求随 key 取消，避免旧响应覆盖新数据
    suspend fun loadOnce() {
        try {
            me = repo.me()
            options = repo.options()
            // 角色以服务端为准（提权/降权后本地会话同步刷新）
            store.updateRole(me!!.role)
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }
    fun load() { scope.launch { loadOnce() } }
    LaunchedEffect(refreshToken) { loadOnce() }

    if (error != null) return ErrorBox(error ?: "", retry = { load() })
    if (me == null || options == null) return LoadingBox()

    val profile = me!!
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(profile.nickname, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(profile.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (profile.role == "ADMIN") "管理员" else "队员",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "加入于 ${TimeFmt.full(profile.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- 个人资料 ----
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("个人资料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NicknameEditor(repo, store, profile) { load() }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                TagEditor(
                    title = "职责",
                    all = options!!.duties.map { it.id to it.name },
                    selected = profile.duties.map { it.id },
                ) { ids -> scope.launch {
                    try {
                        repo.patchMe(MePatchRequest(dutyIds = ids)); load()
                        saveError = null
                    } catch (_: AuthRequiredException) {
                        // 会话失效：Repo 已清本地会话，界面会自动回登录页
                    } catch (e: ApiException) {
                        saveError = e.message
                    } catch (e: Exception) {
                        saveError = "网络错误"
                    }
                } }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AbilityEditorRow(
                    all = options!!.abilities,
                    selected = profile.abilities.map { it.id },
                ) { ids -> scope.launch {
                    try {
                        repo.patchMe(MePatchRequest(abilityIds = ids)); load()
                        saveError = null
                    } catch (_: AuthRequiredException) {
                        // 会话失效：Repo 已清本地会话，界面会自动回登录页
                    } catch (e: ApiException) {
                        saveError = e.message
                    } catch (e: Exception) {
                        saveError = "网络错误"
                    }
                } }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                TagEditor(
                    title = "擅长干员",
                    all = options!!.operators.map { it.id to it.name },
                    selected = profile.operators.map { it.id },
                ) { ids -> scope.launch {
                    try {
                        repo.patchMe(MePatchRequest(operatorIds = ids)); load()
                        saveError = null
                    } catch (_: AuthRequiredException) {
                        // 会话失效：Repo 已清本地会话，界面会自动回登录页
                    } catch (e: ApiException) {
                        saveError = e.message
                    } catch (e: Exception) {
                        saveError = "网络错误"
                    }
                } }
                if (saveError != null) {
                    Text(saveError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ---- 密码 ----
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                PasswordEditor(repo) { }
            }
        }

        // ---- 管理 ----
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                MenuRow("通知设置", "新比赛 / 临近提醒 / 公告", onOpenNotificationSettings)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                MenuRow("外观", "主题色 / 动态取色", onOpenAppearance)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                MenuRow("切换战队站点", null, onSwitchServer)
                if (session?.isAdmin == true) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    MenuRow("用户管理", null, onOpenUsers)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    MenuRow("邀请码管理", null, onOpenInvitations)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    MenuRow("标签维护", null, onOpenTags)
                }
            }
        }

        // ---- 退出登录 ----
        Card(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("退出登录") }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NicknameEditor(repo: Repo, store: SessionStore, profile: MemberProfile, onSaved: () -> Unit) {
    val prefix = profile.teamPrefix ?: ""
    var editing by remember { mutableStateOf(false) }
    // key 用 profile.nickname：保存成功/外部改名后重新取当前昵称，避免「取消」后残留草稿
    var nickname by rememberSaveable(profile.nickname) { mutableStateOf(profile.nickname) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (!editing) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("昵称：${profile.nickname}", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { editing = true }) { Text("修改昵称") }
        }
    } else {
        PrefixedInput(
            value = nickname,
            onValueChange = { nickname = it },
            prefix = prefix.removeSuffix("丨"),
            placeholder = "请输入昵称",
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    busy = true; err = null
                    scope.launch {
                        try {
                            repo.patchMe(MePatchRequest(nickname = nickname.trim()))
                            store.updateNickname(nickname.trim())
                            editing = false
                            onSaved()
                        } catch (_: AuthRequiredException) {
                            // 会话失效：Repo 已清本地会话，界面会自动回登录页
                        } catch (e: ApiException) {
                            err = e.message
                        } catch (e: Exception) {
                            err = "网络错误"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) { Text("保存") }
            TextButton(onClick = { editing = false }) { Text("取消") }
        }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}


@Composable
private fun PasswordEditor(repo: Repo, onSaved: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var oldPwd by rememberSaveable { mutableStateOf("") }
    var newPwd by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("修改密码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { editing = !editing }) { Text(if (editing) "收起" else "修改") }
        }
        if (editing) {
            OutlinedTextField(
                value = oldPwd, onValueChange = { oldPwd = it },
                label = { Text("当前密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newPwd, onValueChange = { newPwd = it },
                label = { Text("新密码（至少 6 位）") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("确认新密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (ok) Text("密码已修改", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (oldPwd.isBlank()) { err = "请输入当前密码"; return@Button }
                    if (newPwd.length < 6) { err = "新密码至少 6 位"; return@Button }
                    if (newPwd != confirm) { err = "两次密码不一致"; return@Button }
                    busy = true; err = null; ok = false
                    scope.launch {
                        try {
                            repo.patchMe(MePatchRequest(password = newPwd, oldPassword = oldPwd))
                            ok = true
                            oldPwd = ""; newPwd = ""; confirm = ""
                            onSaved()
                        } catch (e: ApiException) {
                            err = e.message
                        } catch (e: Exception) {
                            err = "网络错误"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) { Text("提交修改") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(
    title: String,
    all: List<Pair<String, String>>,
    selected: List<String>,
    onSave: (List<String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(selected) }

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (selected.isEmpty()) {
                    Text("未填写", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val idToName = all.toMap()
                        selected.forEach { id ->
                            AssistChip(onClick = {}, label = { Text(idToName[id] ?: id) })
                        }
                    }
                }
            }
            OutlinedButton(onClick = { draft = selected; open = true }) { Text("编辑") }
        }
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    all.forEach { (id, name) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(name)
                            androidx.compose.material3.Checkbox(
                                checked = id in draft,
                                onCheckedChange = { checked ->
                                    draft = if (checked) draft + id else draft - id
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false; onSave(draft) }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("取消") } },
        )
    }
}

// 能力二级选择：先选方向（步兵/载具），再勾选具体能力；二级含 取消/保存/返回
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityEditorRow(
    all: List<com.yoorme.squadsignup.core.Ability>,
    selected: List<String>,
    onSave: (List<String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var step by remember { androidx.compose.runtime.mutableIntStateOf(0) } // 0=选方向 1=选能力
    var category by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(selected) }
    val idToName = all.associate { it.id to it.name }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("能力", style = MaterialTheme.typography.bodyLarge)
            if (selected.isEmpty()) {
                Text("未填写", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    selected.forEach { id ->
                        AssistChip(onClick = {}, label = { Text(idToName[id] ?: id) })
                    }
                }
            }
        }
        OutlinedButton(onClick = { draft = selected; category = null; step = 0; open = true }) { Text("编辑") }
    }

    if (open) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(if (step == 0) "选择方向" else "选择能力") },
            text = {
                if (step == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("INFANTRY" to "步兵方向", "VEHICLE" to "载具方向").forEach { (cat, label) ->
                            OutlinedButton(
                                onClick = { category = cat; step = 1 },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(label) }
                        }
                    }
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        all.filter { it.category == category }.forEach { a ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(a.name)
                                androidx.compose.material3.Checkbox(
                                    checked = a.id in draft,
                                    onCheckedChange = { checked ->
                                        draft = if (checked) draft + a.id else draft - a.id
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (step == 1) {
                    TextButton(onClick = { open = false; onSave(draft) }) { Text("保存") }
                } else {
                    TextButton(onClick = { open = false }) { Text("取消") }
                }
            },
            dismissButton = {
                if (step == 1) {
                    TextButton(onClick = { step = 0 }) { Text("返回") }
                } else {
                    TextButton(onClick = { open = false }) { Text("关闭") }
                }
            },
        )
    }
}

