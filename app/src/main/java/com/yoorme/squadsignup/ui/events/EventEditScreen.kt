package com.yoorme.squadsignup.ui.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yoorme.squadsignup.core.ApiException
import com.yoorme.squadsignup.core.EventManageRequest
import com.yoorme.squadsignup.core.EventCreateRequest
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.TagsResponse
import com.yoorme.squadsignup.core.TimeFmt
import com.yoorme.squadsignup.ui.components.LabeledTextField
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 创建 / 编辑比赛（管理员）
// eventArg: null=创建；非空=编辑已有比赛（eventTime 为北京时间 "yyyy-MM-dd'T'HH:mm"）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    repo: Repo,
    eventId: String?,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    var tags by remember { mutableStateOf<TagsResponse?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var natureId by remember { mutableStateOf<String?>(null) }
    var nameId by remember { mutableStateOf<String?>(null) }
    var customName by remember { mutableStateOf("") }
    var useCustom by remember { mutableStateOf(false) }
    var mapId by remember { mutableStateOf<String?>(null) }
    var format by remember { mutableStateOf<String?>(null) }
    var opponent by remember { mutableStateOf("") }
    // 默认值与网页端一致：要求人数 20、名称默认第一个标签、分队性质默认第一项
    var requiredCount by remember { mutableStateOf("20") }
    var squadNatures by remember { mutableStateOf<List<String>>(emptyList()) }
    var eventDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var editingSquadIds by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // squadId to natureId
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val scope = rememberCoroutineScope()

    // 加载标签 + 编辑模式下的现有数据
    LaunchedEffect(eventId) {
        try {
            tags = repo.tags()
            if (eventId != null) {
                val d = repo.eventDetail(eventId)
                natureId = d.nature.id
                nameId = d.name?.id
                useCustom = d.customName != null
                customName = d.customName ?: ""
                mapId = d.map?.id
                format = d.format
                opponent = d.opponent ?: ""
                requiredCount = d.requiredCount.toString()
                eventDateTime = TimeFmt.parse(d.eventTime)?.atZone(ZoneId.of("Asia/Shanghai"))?.toLocalDateTime()
                editingSquadIds = d.squads.map { it.id to it.nature.id }
                squadNatures = d.squads.map { it.nature.id }
            } else {
                // 与网页端一致：名称默认选中第一个标签（有标签时）
                if (nameId == null) nameId = tags?.names?.firstOrNull()?.id
            }
            if (natureId == null) natureId = tags?.natures?.firstOrNull()?.id
        } catch (e: Exception) {
            loadError = e.message
        }
    }

    // 分队数 = ceil(required / 4)（满足 n*4 >= required 且差值 < 4）
    val squadCount = maxOf(1, ((requiredCount.toIntOrNull() ?: 0) + 3) / 4)

    // 编辑模式：服务端 PATCH /api/events/manage 不支持改「要求人数」与增删分队，
    // 因此分队行数按现有分队数渲染（而不是按 requiredCount 推算），人数也改为只读展示
    val squadRows = if (eventId != null) maxOf(1, editingSquadIds.size) else squadCount

    // 与网页端一致：新建时分队性质自动填充第一个性质；人数变化增减分队时同步补齐
    LaunchedEffect(tags, squadCount, eventId) {
        if (eventId != null) return@LaunchedEffect
        val first = tags?.squadNatures?.firstOrNull()?.id ?: return@LaunchedEffect
        val next = List(squadCount) { i -> squadNatures.getOrNull(i) ?: first }
        if (next != squadNatures) squadNatures = next
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "创建比赛" else "编辑比赛") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "取消",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            loadError != null -> Column(Modifier.padding(padding).padding(16.dp)) {
                Text(loadError ?: "", color = MaterialTheme.colorScheme.error)
            }
            tags == null -> Column(
                Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            else -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 比赛时间
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        eventDateTime?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            ?: "选择比赛时间（北京时间）"
                    )
                }

                // 赛事性质
                Text("赛事性质", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags!!.natures.forEach { n ->
                        FilterChip(
                            selected = natureId == n.id,
                            onClick = { natureId = n.id },
                            label = { Text(n.name) },
                        )
                    }
                }

                // 赛事名称
                Text("赛事名称", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !useCustom && nameId == null, onClick = { useCustom = false; nameId = null }, label = { Text("未知") })
                    tags!!.names.forEach { n ->
                        FilterChip(selected = !useCustom && nameId == n.id, onClick = { useCustom = false; nameId = n.id }, label = { Text(n.name) })
                    }
                    FilterChip(selected = useCustom, onClick = { useCustom = true; nameId = null }, label = { Text("其他") })
                }
                if (useCustom) {
                    LabeledTextField("自定义名称", customName, { customName = it }, placeholder = "如：内部对抗赛")
                }

                // 地图
                Text("地图（可选）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mapId == null, onClick = { mapId = null }, label = { Text("不选择") })
                    tags!!.maps.forEach { m ->
                        FilterChip(selected = mapId == m.id, onClick = { mapId = m.id }, label = { Text(m.name) })
                    }
                }

                // 赛制
                Text("赛制（可选）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BO3", "BO5", "R2").forEach { f ->
                        FilterChip(selected = format == f, onClick = { format = if (format == f) null else f }, label = { Text(f) })
                    }
                }

                // 对手 + 人数
                LabeledTextField("对手", opponent, { opponent = it }, placeholder = "对手战队名")
                if (eventId == null) {
                    LabeledTextField("要求人数", requiredCount, { requiredCount = it.filter { c -> c.isDigit() }.take(3) })
                } else {
                    Text("要求人数：$requiredCount（创建后不可修改）", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "将分为 $squadRows 支队伍（每队上限 4 人）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 各分队性质
                Text("分队性质", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val natures = tags!!.squadNatures
                repeat(squadRows) { idx ->
                    val current = squadNatures.getOrNull(idx)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text("${idx + 1} 队", Modifier.width(44.dp), style = MaterialTheme.typography.bodyMedium)
                        natures.forEach { n ->
                            FilterChip(
                                selected = current == n.id,
                                onClick = {
                                    squadNatures = List(squadRows) { i ->
                                        if (i == idx) n.id else squadNatures.getOrNull(i) ?: natures.firstOrNull()?.id.orEmpty()
                                    }
                                },
                                label = { Text(n.name) },
                            )
                        }
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Button(
                    onClick = {
                        val dt = eventDateTime
                        if (dt == null) { error = "请选择赛事时间"; return@Button }
                        val nid = natureId
                        if (nid == null) { error = "请选择赛事性质"; return@Button }
                        // 校验顺序与文案与网页端一致
                        if (!useCustom && nameId == null) { error = "请选择赛事名称"; return@Button }
                        if (useCustom && customName.trim().isEmpty()) { error = "请输入自定义名称（至少 1 个字符）"; return@Button }
                        val count = requiredCount.trim().toIntOrNull()
                        if (count == null || count <= 0) { error = "要求人数必须是非空正整数"; return@Button }
                        if (squadNatures.size < squadRows || squadNatures.take(squadRows).any { it.isEmpty() }) {
                            error = "请为每支分队选择性质"; return@Button
                        }
                        if (opponent.isBlank()) { error = "请输入对手"; return@Button }
                        saving = true; error = null
                        scope.launch {
                            try {
                                val timeStr = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                                if (eventId == null) {
                                    repo.createEvent(
                                        EventCreateRequest(
                                            eventTime = timeStr,
                                            natureId = nid,
                                            nameId = if (!useCustom) nameId else null,
                                            customName = if (useCustom) customName.trim() else null,
                                            mapId = mapId,
                                            opponent = opponent.trim(),
                                            requiredCount = count,
                                            squadNatures = squadNatures.take(squadCount),
                                            format = format,
                                        )
                                    )
                                } else {
                                    repo.manageEvent(
                                        EventManageRequest(
                                            id = eventId,
                                            natureId = nid,
                                            nameId = if (!useCustom) nameId else null,
                                            customName = if (useCustom) customName.trim() else null,
                                            mapId = mapId,
                                            opponent = opponent.trim(),
                                            format = format,
                                            eventTime = timeStr,
                                            squads = editingSquadIds.mapIndexed { i, (sid, _) ->
                                                com.yoorme.squadsignup.core.SquadNatureUpdate(
                                                    sid,
                                                    squadNatures.getOrNull(i)
                                                        ?: editingSquadIds.getOrNull(i)?.second
                                                        ?: tags?.squadNatures?.firstOrNull()?.id.orEmpty(),
                                                )
                                            },
                                        )
                                    )
                                }
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
                ) { Text(if (eventId == null) "创建" else "保存") }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()
                        pickedDate = date
                        val initialTime = eventDateTime?.toLocalTime() ?: LocalTime.of(19, 0)
                        eventDateTime = LocalDateTime.of(date, initialTime)
                        showDatePicker = false
                    }
                }) { Text("下一步") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // 选完日期后选择时间
    pickedDate?.let { _ ->
        val timeState = rememberTimePickerState(
            initialHour = eventDateTime?.hour ?: 19,
            initialMinute = eventDateTime?.minute ?: 0,
            is24Hour = true,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickedDate = null },
            title = { Text("选择时间") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickedDate ?: return@TextButton
                    eventDateTime = LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute))
                    pickedDate = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickedDate = null }) { Text("取消") } },
        )
    }
}
