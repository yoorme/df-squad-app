@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yoorme.squadsignup.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.AdminTag
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.InvitationCode
import com.yoorme.squadsignup.core.PinyinSearch
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.TagMutation
import com.yoorme.squadsignup.core.AdminUser
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.ConfirmDialog
import com.yoorme.squadsignup.ui.components.EmptyBox
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LoadingBox
import com.yoorme.squadsignup.ui.components.SearchField
import kotlinx.coroutines.launch

// ============ 用户管理 ============

@Composable
fun AdminUsersScreen(repo: Repo, onBack: () -> Unit) {
    var users by remember { mutableStateOf<List<AdminUser>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var resetTarget by remember { mutableStateOf<AdminUser?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminUser?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                users = repo.adminUsers()
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(Unit) { load() }

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
                title = { Text("用户管理") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            error != null -> ErrorBox(error ?: "", Modifier.padding(padding), retry = { load() })
            users == null -> LoadingBox(Modifier.padding(padding))
            else -> {
                val filtered = users!!.filter {
                    PinyinSearch.matches(query, it.nickname) || PinyinSearch.matches(query, it.username)
                }
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SearchField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "搜索昵称/用户名（支持中文与拼音）",
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                if (query.isBlank()) "暂无用户" else "未找到匹配的用户",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { u ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "${u.nickname}${if (u.disabled) "（已禁用）" else ""}",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                "${u.username} · ${u.registrationsCount} 次报名",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        AssistChip(
                                            onClick = { act { repo.patchUser(u.id, if (u.role == "ADMIN") "MEMBER" else "ADMIN", null) } },
                                            label = { Text(if (u.role == "ADMIN") "管理员" else "队员") },
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { act { repo.patchUser(u.id, null, !u.disabled) } }) {
                                            Text(if (u.disabled) "启用" else "禁用")
                                        }
                                        TextButton(onClick = { resetTarget = u }) { Text("重置密码") }
                                        TextButton(onClick = { deleteTarget = u }) {
                                            Text("删除", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    message?.let {
                        item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }

    resetTarget?.let { u ->
        var pwd by rememberSaveable { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { resetTarget = null },
            title = { Text("重置「${u.nickname}」的密码") },
            text = {
                OutlinedTextField(
                    value = pwd, onValueChange = { pwd = it },
                    label = { Text("新密码（至少 6 位）") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pwd.length >= 6) {
                        val target = u
                        resetTarget = null
                        act { repo.resetPassword(target.id, pwd) }
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { resetTarget = null }) { Text("取消") } },
        )
    }

    deleteTarget?.let { u ->
        ConfirmDialog(
            title = "删除账号",
            message = "确定删除「${u.nickname}」吗？其报名记录将被清除，发布的公告/赛事/邀请码将转交给当前管理员。",
            confirmText = "删除", danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                val target = u
                deleteTarget = null
                act { repo.deleteUser(target.id) }
            },
        )
    }
}

// ============ 邀请码管理 ============

@Composable
fun AdminInvitationsScreen(repo: Repo, onBack: () -> Unit) {
    var codes by remember { mutableStateOf<List<InvitationCode>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var maxUses by rememberSaveable { mutableStateOf("1") }
    var message by remember { mutableStateOf<String?>(null) }
    var created by remember { mutableStateOf<InvitationCode?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                codes = repo.invitations()
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("邀请码") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("生成新邀请码", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = maxUses,
                            onValueChange = { maxUses = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("可使用次数") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = {
                            val n = maxUses.toIntOrNull()
                            if (n == null || n <= 0) {
                                message = "次数必须是正整数"
                            } else {
                                scope.launch {
                                    try {
                                        created = repo.createInvitation(n)
                                        load()
                                    } catch (e: Exception) {
                                        message = e.message
                                    }
                                }
                            }
                        }) { Text("生成") }
                    }
                    created?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "新邀请码：${it.code}（剩余 ${it.remaining} 次）",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    message?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            when {
                error != null -> ErrorBox(error ?: "")
                codes == null -> LoadingBox()
                codes!!.isEmpty() -> EmptyBox("暂无邀请码")
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(codes!!, key = { it.id }) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.code, fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "已用 ${c.usedCount}/${c.maxUses} · 剩余 ${c.remaining} · ${TimeFmt.short(c.createdAt)} 由 ${c.createdBy.nickname} 创建",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(onClick = {
                                        scope.launch {
                                            try {
                                                repo.deleteInvitation(c.id)
                                                load()
                                            } catch (e: Exception) {
                                                message = e.message
                                            }
                                        }
                                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ 标签维护 ============

private val TAG_TYPES = listOf(
    "ability" to "能力",
    "duty" to "职责",
    "operator" to "干员",
    "nature" to "赛事性质",
    "name" to "赛事名称",
    "squadNature" to "分队性质",
    "map" to "地图",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTagsScreen(repo: Repo, onBack: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("ability") }
    var tags by remember { mutableStateOf<List<AdminTag>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    var createCategory by rememberSaveable { mutableStateOf("INFANTRY") }
    var renameTarget by remember { mutableStateOf<AdminTag?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminTag?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                tags = repo.adminTags(type)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(type) { load() }

    fun mutate(op: String, id: String?, name: String? = null, disabled: Boolean? = null, category: String? = null) {
        scope.launch {
            try {
                repo.mutateTag(TagMutation(type = type, op = op, id = id, name = name, disabled = disabled, category = category))
                load()
                message = null
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
                title = { Text("标签维护") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { createCategory = "INFANTRY"; createOpen = true }) { Text("新增") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = TAG_TYPES.indexOfFirst { it.first == type }) {
                TAG_TYPES.forEach { (t, label) ->
                    Tab(type == t, onClick = { type = t }, text = { Text(label) })
                }
            }
            when {
                error != null -> ErrorBox(error ?: "", retry = { load() })
                tags == null -> LoadingBox()
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(tags!!, key = { it.id }) { tag ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${tag.name}${if (tag.disabled) "（已禁用）" else ""}",
                                            fontWeight = FontWeight.Medium,
                                        )
                                        if (tag.category != null) {
                                            Text(
                                                if (tag.category == "INFANTRY") "步兵" else "载具",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(start = 6.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        "被使用 ${tag.usedCount} 次",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { renameTarget = tag }) { Text("重命名") }
                                TextButton(onClick = {
                                    mutate("toggleDisable", tag.id, disabled = !tag.disabled)
                                }) { Text(if (tag.disabled) "启用" else "禁用") }
                                TextButton(onClick = { deleteTarget = tag }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    message?.let {
                        item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }

    if (createOpen) {
        var name by rememberSaveable { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { createOpen = false },
            title = { Text("新增标签") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                    if (type == "ability") {
                        Text("方向", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = createCategory == "INFANTRY",
                                onClick = { createCategory = "INFANTRY" },
                                label = { Text("步兵方向") },
                            )
                            FilterChip(
                                selected = createCategory == "VEHICLE",
                                onClick = { createCategory = "VEHICLE" },
                                label = { Text("载具方向") },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        createOpen = false
                        mutate("create", null, name = name.trim(), category = if (type == "ability") createCategory else null)
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { createOpen = false }) { Text("取消") } },
        )
    }

    renameTarget?.let { tag ->
        var name by rememberSaveable { mutableStateOf(tag.name) }
        var category by remember(tag.id) { mutableStateOf(tag.category ?: "INFANTRY") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名「${tag.name}」") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
                    if (type == "ability") {
                        Text("方向", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = category == "INFANTRY",
                                onClick = { category = "INFANTRY" },
                                label = { Text("步兵方向") },
                            )
                            FilterChip(
                                selected = category == "VEHICLE",
                                onClick = { category = "VEHICLE" },
                                label = { Text("载具方向") },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val categoryChanged = type == "ability" && category != (tag.category ?: "INFANTRY")
                    if (name.isNotBlank() && (name != tag.name || categoryChanged)) {
                        renameTarget = null
                        mutate("update", tag.id, name = name.trim(), category = if (type == "ability") category else null)
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } },
        )
    }

    deleteTarget?.let { tag ->
        ConfirmDialog(
            title = "删除标签",
            message = "确定删除「${tag.name}」吗？被引用的赛事/用户标签将同步清除该标签。",
            confirmText = "删除", danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                val t = tag
                deleteTarget = null
                mutate("delete", t.id)
            },
        )
    }
}
