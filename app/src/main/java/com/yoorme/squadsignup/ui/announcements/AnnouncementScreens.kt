@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yoorme.squadsignup.ui.announcements

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yoorme.squadsignup.SquadApp
import com.yoorme.squadsignup.core.AnnouncementDetail
import com.yoorme.squadsignup.core.AnnouncementSummary
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.AuthRequiredException
import com.yoorme.squadsignup.core.ImagePreparer
import com.yoorme.squadsignup.core.ImageSaver
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SquadServer
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.ConfirmDialog
import com.yoorme.squadsignup.ui.components.ContentState
import com.yoorme.squadsignup.ui.components.ContentStateTransition
import com.yoorme.squadsignup.ui.components.EmptyBox
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.FullScreenImageViewer
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
    var tab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var list by remember { mutableStateOf<List<AnnouncementSummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 请求发在 LaunchedEffect 的协程里（而不是转 scope.launch）：
    // tab/refresh 变化时旧请求随 key 一起取消，避免旧响应覆盖新数据
    suspend fun loadOnce() {
        val status = if (tab == 1 && isAdmin) "archived" else "normal"
        try {
            list = repo.announcements(status)
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }
    fun load() { scope.launch { loadOnce() } }
    LaunchedEffect(tab, isAdmin, refreshToken) { loadOnce() }

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
            ContentStateTransition(
                state = when {
                    error != null -> ContentState.ERROR
                    l == null -> ContentState.LOADING
                    l.isEmpty() -> ContentState.EMPTY
                    else -> ContentState.CONTENT
                },
                modifier = Modifier.fillMaxSize(),
            ) { state ->
                when (state) {
                    ContentState.ERROR -> ErrorBox(error ?: "", retry = { load() })
                    ContentState.LOADING -> LoadingBox()
                    ContentState.EMPTY -> EmptyBox("暂无公告")
                    ContentState.CONTENT -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(l.orEmpty(), key = { it.id }) { a ->
                            Card(
                                onClick = { openAnnouncement(a.id) },
                                modifier = Modifier.fillMaxWidth().animateItem(),
                            ) {
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
    val context = LocalContext.current

    // ---- 图片：双击全屏查看 / 长按或按钮保存到相册 ----
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }

    val doSave: (String) -> Unit = { url ->
        scope.launch {
            val ext = url.substringAfterLast('.', "").takeIf { it.length in 1..5 } ?: "jpg"
            val result = ImageSaver.save(context, url, "announcement_${System.currentTimeMillis()}.$ext")
            Toast.makeText(context, result.getOrElse { it.message ?: "保存失败" }, Toast.LENGTH_SHORT).show()
        }
    }
    // Android 9 及以下写公共相册目录需要存储权限（10+ 免权限，不会走到这里）
    val savePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val url = pendingSaveUrl
        pendingSaveUrl = null
        if (granted && url != null) doSave(url)
        else Toast.makeText(context, "未授予存储权限，无法保存", Toast.LENGTH_SHORT).show()
    }

    fun startSave(url: String) {
        val needsPermission = ImageSaver.needsLegacyPermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingSaveUrl = url
            savePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doSave(url)
        }
    }

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
                            MarkdownText(
                                markdown = d.contentMarkdown,
                                baseUrl = server.baseUrl,
                                onImageDoubleTap = { viewerUrl = it },
                                onImageLongPress = { startSave(it) },
                            )
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

    // 全屏图片查看（双击正文图片打开；BackHandler 优先于页面返回）
    viewerUrl?.let { url ->
        FullScreenImageViewer(
            url = url,
            onDismiss = { viewerUrl = null },
            onDownload = { startSave(url) },
        )
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

// 与网站管理端保持一致：最多 20 张；单张 >5MB 由 ImagePreparer 自动压缩
private const val MAX_ANNOUNCEMENT_IMAGES = 20

private fun extOfMime(mime: String): String = when {
    mime.contains("png", true) -> "png"
    mime.contains("gif", true) -> "gif"
    mime.contains("webp", true) -> "webp"
    mime.contains("bmp", true) -> "bmp"
    else -> "jpg"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementEditScreen(
    repo: Repo,
    announcementId: String?,
    serverBase: String,
    onDone: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    // 本次编辑中已上传、尚未保存的图片：离开页面时清理，避免残留在服务器 tmp 目录
    var sessionUploads by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(announcementId != null) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    /** 离开编辑页：未保存时在应用级作用域里清理本次上传的 tmp 图片（保存成功则服务端已迁移，无需删） */
    fun leave(saved: Boolean) {
        val orphans = if (saved) emptyList() else sessionUploads
        sessionUploads = emptyList()
        if (orphans.isNotEmpty()) {
            SquadApp.appScope.launch {
                for (p in orphans) runCatching { repo.deleteUpload(p) }
            }
        }
        onDone()
    }

    // 系统返回键同样走清理逻辑；保存进行中不响应，避免打断已发出的请求
    BackHandler { if (!saving) leave(saved = false) }

    LaunchedEffect(announcementId) {
        if (announcementId != null) {
            try {
                val d = repo.announcementDetail(announcementId)
                title = d.title
                content = d.contentMarkdown
                images = d.images.sortedBy { it.sortOrder }.map { it.path }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    // 系统相册/图片选择（Photo Picker，无需存储权限）
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        if (images.size + uris.size > MAX_ANNOUNCEMENT_IMAGES) {
            error = "最多 $MAX_ANNOUNCEMENT_IMAGES 张图片"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            uploading = true; error = null; notice = null
            var uploaded = 0
            for (uri in uris) {
                // ≤5MB 原样上传；>5MB 自动压缩并纠正 EXIF 方向
                val picked = ImagePreparer.prepare(context, uri)
                if (picked.isFailure) {
                    error = picked.exceptionOrNull()?.message ?: "图片读取失败"
                    break
                }
                val (bytes, mime) = picked.getOrThrow()
                try {
                    val path = repo.uploadImage(bytes, "image.${extOfMime(mime)}", mime)
                    images = images + path
                    sessionUploads = sessionUploads + path
                    // 与网站管理端一致：上传后立即插入正文末尾（服务端保存时迁移 tmp→正式并改写路径）
                    content = "$content\n\n![图片]($path)\n"
                    uploaded++
                } catch (_: AuthRequiredException) {
                    // 会话失效：Repo 已清本地会话，界面会自动回登录页
                    uploading = false
                    return@launch
                } catch (e: ApiException) {
                    error = e.message
                    break
                } catch (e: Exception) {
                    error = "上传失败，请检查网络后重试"
                    break
                }
            }
            if (uploaded > 0) notice = "已上传 $uploaded 张图片，保存公告后正式生效"
            uploading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (announcementId == null) "发布公告" else "编辑公告") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(
                        onClick = { if (!saving) leave(saved = false) },
                    ) {
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

                // ---- 图片上传（插入正文 + 随公告保存；服务端负责 tmp→正式迁移与删除清理）----
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (images.size >= MAX_ANNOUNCEMENT_IMAGES) {
                                error = "最多 $MAX_ANNOUNCEMENT_IMAGES 张图片"
                            } else {
                                pickImages.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        enabled = !uploading && !saving,
                    ) {
                        if (uploading) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("上传中…")
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("添加图片")
                        }
                    }
                    Text(
                        "最多 $MAX_ANNOUNCEMENT_IMAGES 张，单张不超过 5MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images, key = { it }) { path ->
                            Box {
                                AsyncImage(
                                    model = serverBase.trimEnd('/') + path,
                                    contentDescription = "公告图片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                                        .clickable {
                                            images = images.filterNot { it == path }
                                            // 与网站端一致：同时移除正文中的引用行（保存时服务端删除不再引用的文件）
                                            content = content.replace(
                                                Regex("\\n*!\\[[^\\]]*\\]\\(${Regex.escape(path)}\\)\\n*"),
                                                "\n",
                                            )
                                            // 本次会话刚上传、还没保存的图：立即删掉服务器上的 tmp 文件
                                            if (path in sessionUploads) {
                                                sessionUploads = sessionUploads - path
                                                SquadApp.appScope.launch {
                                                    runCatching { repo.deleteUpload(path) }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "移除图片",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                notice?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = {
                        if (title.isBlank() || content.isBlank()) {
                            error = "标题与正文不能为空"; return@Button
                        }
                        saving = true; error = null
                        scope.launch {
                            try {
                                if (announcementId == null) repo.createAnnouncement(title.trim(), content, images)
                                else repo.updateAnnouncement(announcementId, title.trim(), content, images)
                                // 保存成功：服务端已把 tmp 图迁移到正式目录，无需清理
                                leave(saved = true)
                            } catch (_: AuthRequiredException) {
                                // 会话失效：Repo 已清本地会话，界面会自动回登录页
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
                    enabled = !saving && !uploading,
                ) { Text(if (announcementId == null) "发布" else "保存") }
            }
        }
    }
}
