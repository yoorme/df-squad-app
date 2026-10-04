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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.EventDetail
import com.yoorme.squadsignup.core.EventSummary
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SquadMember
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.ConfirmDialog
import com.yoorme.squadsignup.ui.components.ContentState
import com.yoorme.squadsignup.ui.components.ContentStateTransition
import com.yoorme.squadsignup.ui.components.EmptyBox
import com.yoorme.squadsignup.ui.components.ErrorBox
import com.yoorme.squadsignup.ui.components.LoadingBox
import kotlinx.coroutines.launch

// ============ 赛事卡片 ============

@Composable
fun EventCard(event: EventSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("正式 ${event.totalRegistered}/${event.requiredCount} · 替补 ${event.totalSubstitutes}") },
                )
                if (event.map != null) AssistChip(onClick = {}, label = { Text(event.map.name) })
                if (event.format != null) AssistChip(onClick = {}, label = { Text(event.format) })
                if (event.status == "UPCOMING") {
                    TimeFmt.remainingLabel(event.eventTime)?.let { remaining ->
                        AssistChip(onClick = {}, label = { Text(remaining) })
                    }
                }
                TimeFmt.weekday(event.eventTime)?.let { weekday ->
                    AssistChip(onClick = {}, label = { Text(weekday) })
                }
            }
            if (event.myRegistration != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (event.myRegistration.isSubstitute) "我已报名（替补）" else "我已报名",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ============ 赛事列表 ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    repo: Repo,
    isAdmin: Boolean,
    openEvent: (String) -> Unit,
    createEvent: () -> Unit,
    refreshToken: Int,
) {
    // 记住所在筛选页：从详情返回后不跳回“即将进行”
    var tab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var events by remember { mutableStateOf<List<EventSummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 请求发在 LaunchedEffect 的协程里（而不是转 scope.launch）：
    // tab/refresh 变化时旧请求随 key 一起取消，避免旧响应覆盖新数据
    suspend fun loadOnce() {
        val status = if (tab == 0) "UPCOMING" else "ARCHIVED"
        try {
            events = repo.events(status)
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }
    fun load() { scope.launch { loadOnce() } }
    LaunchedEffect(tab, refreshToken) { loadOnce() }

    Scaffold(
        // 外层 MainTabs 的 Scaffold 已处理状态栏内边距，这里置 0 避免顶部双重留白
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (isAdmin && tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = createEvent,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("创建比赛") },
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, onClick = { tab = 0 }, text = { Text("即将进行") })
                Tab(tab == 1, onClick = { tab = 1 }, text = { Text("已结束") })
            }
            val list = events
            ContentStateTransition(
                state = when {
                    error != null -> ContentState.ERROR
                    list == null -> ContentState.LOADING
                    list.isEmpty() -> ContentState.EMPTY
                    else -> ContentState.CONTENT
                },
                modifier = Modifier.fillMaxSize(),
            ) { state ->
                when (state) {
                    ContentState.ERROR -> ErrorBox(error ?: "", retry = { load() })
                    ContentState.LOADING -> LoadingBox()
                    ContentState.EMPTY -> EmptyBox(if (tab == 0) "暂无即将进行的比赛" else "暂无历史比赛")
                    ContentState.CONTENT -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list.orEmpty(), key = { it.id }) { e ->
                            EventCard(
                                event = e,
                                onClick = { openEvent(e.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============ 赛事详情（手机端带顶栏） ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    repo: Repo,
    eventId: String,
    isAdmin: Boolean,
    myUserId: String,
    serverBase: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEdit: (String) -> Unit,
    onAssign: (String) -> Unit,
    onChanged: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("赛事", maxLines = 1) },
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
        EventDetailContent(
            repo = repo,
            eventId = eventId,
            isAdmin = isAdmin,
            myUserId = myUserId,
            serverBase = serverBase,
            onBack = null,
            onDeleted = onDeleted,
            onEdit = onEdit,
            onAssign = onAssign,
            onChanged = onChanged,
            modifier = Modifier.padding(padding),
        )
    }
}

// ============ 赛事详情 ============

@Composable
fun EventDetailContent(
    repo: Repo,
    eventId: String,
    isAdmin: Boolean,
    myUserId: String,
    serverBase: String,
    onBack: (() -> Unit)?,
    onDeleted: (() -> Unit)? = null,
    onEdit: (String) -> Unit,
    onAssign: (String) -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var detail by remember { mutableStateOf<EventDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf<Boolean?>(null) }
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

    fun act(block: suspend () -> String?) {
        busy = true
        scope.launch {
            try {
                val msg = block()
                message = msg
                load()
                onChanged()
            } catch (e: ApiException) {
                message = e.message
            } catch (e: Exception) {
                message = "网络错误"
            } finally {
                busy = false
            }
        }
    }

    val d = detail
    when {
        error != null -> ErrorBox(error ?: "", modifier, retry = { load() })
        d == null -> LoadingBox(modifier)
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 手机端独立打开时显示返回箭头（双栏模式下 onBack 为空，不显示）
            if (onBack != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Text(d.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (d.map != null) AssistChip(onClick = {}, label = { Text(d.map.name) })
                if (d.format != null) AssistChip(onClick = {}, label = { Text(d.format) })
                AssistChip(
                    onClick = {},
                    label = { Text("正式 ${d.totalRegistered}/${d.requiredCount} · 替补 ${d.totalSubstitutes}") },
                )
                if (d.status == "UPCOMING") {
                    TimeFmt.remainingLabel(d.eventTime)?.let { remaining ->
                        AssistChip(onClick = {}, label = { Text(remaining) })
                    }
                }
                TimeFmt.weekday(d.eventTime)?.let { weekday ->
                    AssistChip(onClick = {}, label = { Text(weekday) })
                }
            }
            if (d.opponent != null) {
                Spacer(Modifier.height(4.dp))
                Text("对手：${d.opponent}", style = MaterialTheme.typography.bodyMedium)
            }

            // ---- 我的报名状态 ----
            Spacer(Modifier.height(12.dp))
            val mine = d.myRegistration
            // 详情的 members 均带 userId，用它定位自己的报名记录 ID
            val myRegId = (d.squads.flatMap { it.members } + d.substitutes)
                .firstOrNull { it.userId == myUserId }?.registrationId
            if (mine != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            if (mine.isSubstitute) "当前状态：替补" else "当前状态：已报名",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (myRegId != null) act { repo.cancelRegistration(myRegId); "已取消报名" }
                            },
                            enabled = !busy && d.status == "UPCOMING" && myRegId != null,
                        ) { Text("取消报名") }
                    }
                }
            }

            // ---- 分队 ----
            Spacer(Modifier.height(12.dp))
            for (squad in d.squads) {
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${squad.index} 队 · ${squad.nature.name}",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${squad.members.size}/${squad.capacity}",
                                color = if (squad.members.size >= squad.capacity)
                                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (mine == null && d.status == "UPCOMING" && squad.members.size < squad.capacity) {
                            Button(
                                onClick = {
                                    act {
                                        val r = repo.registerEvent(d.id, squad.id, false)
                                        r.message ?: "报名成功"
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) { Text("加入 ${squad.index} 队") }
                        }
                        for (m in squad.members) {
                            MemberLine(m)
                        }
                        if (squad.members.isEmpty()) {
                            Text("暂无队员", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ---- 替补 ----
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("替补", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            "${d.substitutes.size}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (mine == null && d.status == "UPCOMING") {
                        Button(
                            onClick = {
                                act {
                                    val r = repo.registerEvent(d.id, null, true)
                                    r.message
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) { Text("加入替补") }
                    }
                    for (m in d.substitutes) MemberLine(m)
                    if (d.substitutes.isEmpty()) {
                        Text("暂无替补", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ---- 管理员操作 ----
            if (isAdmin) {
                Spacer(Modifier.height(16.dp))
                Text("管理员操作", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAssign(d.id) }, enabled = !busy) { Text("分队调整") }
                    OutlinedButton(onClick = { onEdit(d.id) }, enabled = !busy) { Text("编辑") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { confirmArchive = d.status == "UPCOMING" },
                        enabled = !busy,
                    ) { Text(if (d.status == "UPCOMING") "归档" else "恢复") }
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除比赛",
            message = "确定删除该比赛吗？所有报名记录将一并删除，不可恢复。",
            confirmText = "删除", danger = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                val id = d?.id ?: return@ConfirmDialog
                busy = true
                scope.launch {
                    try {
                        repo.deleteEvent(id)
                        onChanged()
                        onDeleted?.invoke() // 手机端返回列表；双栏端清空详情
                    } catch (e: ApiException) {
                        message = e.message
                    } catch (e: Exception) {
                        message = "网络错误"
                    } finally {
                        busy = false
                    }
                }
            },
        )
    }
    confirmArchive?.let { toArchive ->
        ConfirmDialog(
            title = if (toArchive) "归档比赛" else "恢复比赛",
            message = if (toArchive) "归档后比赛移入「已结束」列表，队员不能再报名。" else "恢复后比赛重新出现在「即将进行」列表。",
            onDismiss = { confirmArchive = null },
            onConfirm = {
                confirmArchive = null
                act {
                    repo.manageEvent(
                        com.yoorme.squadsignup.core.EventManageRequest(
                            id = d?.id ?: "",
                            status = if (toArchive) "ARCHIVED" else "UPCOMING",
                        )
                    )
                    null
                }
            },
        )
    }
}

@Composable
private fun MemberLine(m: SquadMember) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(m.nickname, fontWeight = FontWeight.Medium)
        val tags = m.duties.map { it.name } + m.abilities.map { it.name }
        if (tags.isNotEmpty()) {
            Text(
                "  ${tags.take(4).joinToString(" · ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
