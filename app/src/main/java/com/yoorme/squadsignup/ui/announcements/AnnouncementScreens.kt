@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yoorme.squadsignup.ui.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.AnnouncementDetail
import com.yoorme.squadsignup.core.AnnouncementSummary
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SquadServer
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.ConfirmDialog
import com.yoorme.squadsignup.ui.components.EmptyBox
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LabeledTextField
import com.yoorme.squadsignup.ui.components.LoadingBox
import com.yoorme.squadsignup.ui.components.MarkdownText
import kotlinx.coroutines.launch

// ============ 公告列表 ============

@Composable
fun AnnouncementsScreen(
    repo: Repo,
    isAdmin: Boolean,
    openAnnouncement: (String) -> Unit,
    createAnnouncement: () -> Unit,
    refreshToken: Int,
) {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    var list by remember { mutableStateOf<List<AnnouncementSummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        val status = if (tab == 1 && isAdmin) "archived" else "normal"
        scope.launch {
            try {
                list = repo.announcements(status)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(tab, isAdmin, refreshToken) { load() }

    Scaffold(
        // 外层已处理状态栏内边距，置 0 避免顶部双重留白
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (isAdmin && tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = createAnnouncement,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("发布公告") },
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (isAdmin) {
                SecondaryTabRow(selectedTabIndex = tab) {
                    Tab(tab == 0, onClick = { tab = 0 }, text = { Text("公告") })
                    Tab(tab == 1, onClick = { tab = 1 }, text = { Text("已归档") })
                }
            }
            val l = list
            when {
                error != null -> ErrorBox(error ?: "", retry = { load() })
                l == null -> LoadingBox()
                l.isEmpty() -> EmptyBox("暂无公告")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(l, key = { it.id }) { a ->
                        Card(onClick = { openAnnouncement(a.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        a.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (!a.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 6.dp)
                                                .size(8.dp)
                                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${a.author.nickname} · ${TimeFmt.short(a.createdAt)} · ${a.commentCount} 条评论",
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

// ============ 公告详情 ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    repo: Repo,
    announcementId: String,
    isAdmin: Boolean,
    myUserId: String?,
    server: SquadServer,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    var detail by remember { mutableStateOf<AnnouncementDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmArchive by remember { mutableStateOf<Boolean?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            try {
                detail = repo.announcementDetail(announcementId)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
    LaunchedEffect(announcementId) { load() }

    fun act(block: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                block()
                load()
            } catch (e: ApiException) {
                message = e.message
            } catch (e: Exception) {
                message = "网络错误"
            } finally {
                busy = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("公告", maxLines = 1) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    if (isAdmin && detail != null) {
                        TextButton(onClick = { onEdit(announcementId) }) { Text("编辑") }
                        TextButton(onClick = { confirmArchive = !(detail?.isArchived ?: false) }) {
                            Text(if (detail?.isArchived == true) "恢复" else "归档")
                        }
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            error != null -> ErrorBox(error ?: "", Modifier.padding(padding), retry = { load() })
            detail == null -> LoadingBox(Modifier.padding(padding))
            else -> {
                val d = detail!!
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    // 公告内容独立区块
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(d.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${d.author?.nickname ?: "管理员"} · ${TimeFmt.full(d.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (d.isArchived) {
                                Spacer(Modifier.height(4.dp))
                                Text("已归档", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            MarkdownText(d.contentMarkdown, server.baseUrl)
                            if (d.images.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("附件图片 ${d.images.size} 张（已包含在正文中）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 评论：不使用卡片，改为聊天气泡
                    Spacer(Modifier.height(16.dp))
                    Text("评论（${d.comments.size}）", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    for (c in d.comments) {
                        CommentBubble(
                            comment = c,
                            isMine = c.isMine || (myUserId != null && c.user.id == myUserId),
                            isAdmin = isAdmin,
                            onDelete = { act { repo.deleteComment(c.id) } },
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // 发表评论
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("写下你的评论（500 字以内）") },
                        minLines = 2,
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (commentText.isBlank()) return@Button
                            act {
                                repo.postComment(announcementId, commentText.trim())
                                commentText = ""
                            }
                        },
                        enabled = !busy && commentText.isNotBlank(),
                    ) { Text("发表评论") }

                    message?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    confirmArchive?.let { toArchive ->
        ConfirmDialog(
            title = if (toArchive) "归档公告" else "恢复公告",
            message = if (toArchive) "归档后普通队员将看不到该公告。" else "恢复后公告重新对全员可见。",
            onDismiss = { confirmArchive = null },
            onConfirm = {
                confirmArchive = null
                act { repo.archiveAnnouncement(announcementId, toArchive) }
            },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "删除公告",
            message = "确定删除该公告及全部评论吗？",
            confirmText = "删除", danger = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    try {
                        repo.deleteAnnouncement(announcementId)
                        onBack()
                    } catch (e: Exception) {
                        message = e.message
                    }
                }
            },
        )
    }
}

@Composable
private fun CommentBubble(
    comment: com.yoorme.squadsignup.core.Comment,
    isMine: Boolean,
    isAdmin: Boolean,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Text(
            comment.user.nickname,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
        )
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = shape,
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        TimeFmt.full(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isMine || isAdmin) {
                        Text(
                            "删除",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .clickable(onClick = onDelete),
                        )
                    }
                }
            }
        }
    }
}

// ============ 公告编辑 ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementEditScreen(
    repo: Repo,
    announcementId: String?,
    onDone: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(announcementId != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(announcementId) {
        if (announcementId != null) {
            try {
                val d = repo.announcementDetail(announcementId)
                title = d.title
                content = d.contentMarkdown
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (announcementId == null) "发布公告" else "编辑公告") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onDone) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "取消",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                LoadingBox()
            } else {
                LabeledTextField("标题", title, { title = it })
                Column {
                    Text("正文（支持 Markdown）", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("# 标题\n\n正文内容…\n\n- 列表项\n- **加粗** 文本") },
                        minLines = 10,
                    )
                }
                // 图片：请在网站管理端上传后引用其路径；App 端先支持纯文本编辑
                Text(
                    "提示：图片请先在网站管理端上传获取路径后，以 ![描述](/uploads/xx) 形式引用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = {
                        if (title.isBlank() || content.isBlank()) {
                            error = "标题与正文不能为空"; return@Button
                        }
                        saving = true; error = null
                        scope.launch {
                            try {
                                if (announcementId == null) repo.createAnnouncement(title.trim(), content)
                                else repo.updateAnnouncement(announcementId, title.trim(), content)
                                onDone()
                            } catch (e: ApiException) {
                                error = e.message
                            } catch (e: Exception) {
                                error = "网络错误，请重试"
                            } finally {
                                saving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !saving,
                ) { Text(if (announcementId == null) "发布" else "保存") }
            }
        }
    }
}
