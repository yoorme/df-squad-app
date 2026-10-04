package com.yoorme.squadsignup.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yoorme.squadsignup.core.AppIconManager
import com.yoorme.squadsignup.core.DeviceRegisterRequest
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.core.Servers
import com.yoorme.squadsignup.notify.PushManager
import com.yoorme.squadsignup.notify.PollWorker
import com.yoorme.squadsignup.ui.announcements.AnnouncementDetailScreen
import com.yoorme.squadsignup.ui.announcements.AnnouncementEditScreen
import com.yoorme.squadsignup.ui.announcements.AnnouncementsScreen
import com.yoorme.squadsignup.ui.auth.LoginScreen
import com.yoorme.squadsignup.ui.auth.RegisterScreen
import com.yoorme.squadsignup.ui.admin.AdminInvitationsScreen
import com.yoorme.squadsignup.ui.admin.AdminTagsScreen
import com.yoorme.squadsignup.ui.admin.AdminUsersScreen
import com.yoorme.squadsignup.ui.events.AssignScreen
import com.yoorme.squadsignup.ui.events.EventDetailContent
import com.yoorme.squadsignup.ui.events.EventDetailScreen
import com.yoorme.squadsignup.ui.events.EventEditScreen
import com.yoorme.squadsignup.ui.events.EventsScreen
import com.yoorme.squadsignup.ui.me.AppearanceScreen
import com.yoorme.squadsignup.ui.me.MeScreen
import com.yoorme.squadsignup.ui.me.NotificationSettingsScreen
import com.yoorme.squadsignup.ui.members.MemberDetailScreen
import com.yoorme.squadsignup.ui.members.MembersScreen
import com.yoorme.squadsignup.ui.theme.SquadMotion
import kotlinx.coroutines.launch

// ============ 页面转场：M3 共享轴 X（进入位移 20% + 淡入，退出更短更快） ============

private val pageEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { it / 5 },
        animationSpec = tween(SquadMotion.Medium2, easing = SquadMotion.EmphasizedDecelerate),
    ) + fadeIn(tween(SquadMotion.Short4, easing = SquadMotion.StandardDecelerate))
}

private val pageExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { -it / 5 },
        animationSpec = tween(SquadMotion.Short4, easing = SquadMotion.EmphasizedAccelerate),
    ) + fadeOut(tween(SquadMotion.Short4, easing = SquadMotion.StandardAccelerate))
}

private val pagePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { -it / 5 },
        animationSpec = tween(SquadMotion.Medium2, easing = SquadMotion.EmphasizedDecelerate),
    ) + fadeIn(tween(SquadMotion.Short4, easing = SquadMotion.StandardDecelerate))
}

private val pagePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { it / 5 },
        animationSpec = tween(SquadMotion.Short4, easing = SquadMotion.EmphasizedAccelerate),
    ) + fadeOut(tween(SquadMotion.Short4, easing = SquadMotion.StandardAccelerate))
}

@Composable
fun Root(
    store: SessionStore,
    repo: Repo,
    windowWidth: WindowWidthSizeClass,
    initialToken: String?,
    pendingOpen: String?,
    pendingId: String?,
    onPendingHandled: () -> Unit,
) {
    val context = LocalContext.current
    val token by store.token.collectAsState(initial = initialToken)
    val user by store.user.collectAsState(initial = null)
    val serverId by store.serverId.collectAsState(initial = null)
    var showRegister by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    // 桌面图标跟随所选战队（mmr/yfd/default）
    LaunchedEffect(serverId) {
        AppIconManager.applyForServer(context, serverId)
    }

    // 登录成功后调度轮询 + 上报极光设备绑定
    LaunchedEffect(token) {
        if (token != null) {
            // WorkManager 初始化异常不应拖垮界面；调度失败时后台轮询缺席，但不影响使用
            runCatching { PollWorker.schedule(context) }
            // 等极光 SDK 完成注册（启动初期 registrationId 为空），最长约 60 秒
            repeat(30) {
                val rid = PushManager.registrationId(context)
                if (rid.isNotBlank()) {
                    runCatching {
                        repo.registerDevice(DeviceRegisterRequest(registrationId = rid))
                    }
                    return@LaunchedEffect
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    // 通知点击跳转
    val navController = rememberNavController()
    LaunchedEffect(pendingOpen, pendingId, token) {
        if (token != null && pendingOpen != null && pendingId != null) {
            val route = if (pendingOpen == "event") "event/$pendingId" else "ann/$pendingId"
            navController.navigate(route)
            onPendingHandled()
        }
    }

    if (token == null) {
        if (showRegister) {
            RegisterScreen(
                store = store,
                repo = repo,
                initialServerId = serverId,
                onRegistered = { _, _, _ -> showRegister = false },
                onBack = { showRegister = false },
            )
        } else {
            LoginScreen(
                store = store,
                repo = repo,
                initialServerId = serverId,
                onLoggedIn = { showRegister = false },
                onGoRegister = { showRegister = true },
            )
        }
        return
    }

    NavHost(
        navController,
        startDestination = "main",
        enterTransition = pageEnter,
        exitTransition = pageExit,
        popEnterTransition = pagePopEnter,
        popExitTransition = pagePopExit,
    ) {
        composable("main") {
            MainTabs(
                store = store,
                repo = repo,
                navController = navController,
                session = user,
                windowWidth = windowWidth,
                refresh = refresh,
                onRefresh = { refresh++ },
            )
        }
        composable("event/{id}") { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            EventDetailScreen(
                repo = repo,
                eventId = id,
                isAdmin = user?.isAdmin == true,
                myUserId = user?.id ?: "",
                serverBase = Servers.byId(serverId).baseUrl,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onEdit = { navController.navigate("eventEdit/$id") },
                onAssign = { navController.navigate("assign/$id") },
                onChanged = { refresh++ },
            )
        }
        composable("eventEdit/{eventId}") { entry ->
            val eventId = entry.arguments?.getString("eventId")?.takeIf { it != "new" }
            EventEditScreen(
                repo = repo,
                eventId = eventId,
                onDone = { refresh++; navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable("assign/{eventId}") { entry ->
            val eventId = entry.arguments?.getString("eventId") ?: return@composable
            AssignScreen(
                repo = repo,
                eventId = eventId,
                onDone = { refresh++; navController.popBackStack() },
            )
        }
        composable("ann/{id}") { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            AnnouncementDetailScreen(
                repo = repo,
                announcementId = id,
                isAdmin = user?.isAdmin == true,
                myUserId = user?.id,
                server = Servers.byId(serverId),
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("annEdit/$id") },
            )
        }
        composable("annEdit/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.takeIf { it != "new" }
            AnnouncementEditScreen(
                repo = repo,
                announcementId = id,
                onDone = { refresh++; navController.popBackStack() },
            )
        }
        composable("member/{id}") { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            MemberDetailScreen(
                repo = repo,
                memberId = id,
                isAdmin = user?.isAdmin == true,
                onBack = { navController.popBackStack() },
            )
        }
        composable("notifSettings") {
            NotificationSettingsScreen(repo = repo, onBack = { navController.popBackStack() })
        }
        composable("adminUsers") {
            AdminUsersScreen(repo = repo, onBack = { navController.popBackStack() })
        }
        composable("adminInvites") {
            AdminInvitationsScreen(repo = repo, onBack = { navController.popBackStack() })
        }
        composable("adminTags") {
            AdminTagsScreen(repo = repo, onBack = { navController.popBackStack() })
        }
        composable("appearance") {
            AppearanceScreen(store = store, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainTabs(
    store: SessionStore,
    repo: Repo,
    navController: NavHostController,
    session: com.yoorme.squadsignup.core.SessionUser?,
    windowWidth: WindowWidthSizeClass,
    refresh: Int,
    onRefresh: () -> Unit,
) {
    val serverId by store.serverId.collectAsState(initial = null)
    val server = Servers.byId(serverId)
    // 记住所在标签页：从详情页返回后仍停在原位置（如：成员 → 队员详情 → 返回 = 成员）
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            // M3 Expressive 短导航栏：选中项展开为「图标 + 文字」胶囊
            ShortNavigationBar {
                listOf(
                    Triple("赛事", Icons.Default.EmojiEvents, 0),
                    Triple("公告", Icons.Default.Campaign, 1),
                    Triple("成员", Icons.Default.Groups, 2),
                    Triple("我的", Icons.Default.Person, 3),
                ).forEach { (label, icon, index) ->
                    val selected = tab == index
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        iconPosition = if (selected) NavigationItemIconPosition.Start
                        else NavigationItemIconPosition.Top,
                    )
                }
            }
        },
    ) { padding ->
        // M3 fade-through：旧页 90ms 淡出，新页自 92% 放大淡入
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(210, easing = SquadMotion.EmphasizedDecelerate)) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(210, easing = SquadMotion.EmphasizedDecelerate),
                    )).togetherWith(fadeOut(tween(90, easing = SquadMotion.StandardAccelerate)))
            },
            label = "mainTab",
        ) { currentTab ->
        when (currentTab) {
            0 -> if (windowWidth >= WindowWidthSizeClass.Medium) {
                // 平板/横屏：列表 + 详情双栏
                var selectedEvent by rememberSaveable { mutableStateOf<String?>(null) }
                Row(Modifier.padding(padding).fillMaxSize()) {
                    Column(Modifier.weight(0.42f)) {
                        EventsScreen(
                            repo = repo,
                            isAdmin = session?.isAdmin == true,
                            openEvent = { selectedEvent = it },
                            createEvent = { navController.navigate("eventEdit/new") },
                            refreshToken = refresh,
                        )
                    }
                    Column(Modifier.weight(0.58f)) {
                        selectedEvent?.let { id ->
                            EventDetailContent(
                                repo = repo,
                                eventId = id,
                                isAdmin = session?.isAdmin == true,
                                myUserId = session?.id ?: "",
                                serverBase = server.baseUrl,
                                onBack = null,
                                onDeleted = { selectedEvent = null },
                                onEdit = { navController.navigate("eventEdit/$id") },
                                onAssign = { navController.navigate("assign/$id") },
                                onChanged = onRefresh,
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.padding(padding)) {
                    EventsScreen(
                        repo = repo,
                        isAdmin = session?.isAdmin == true,
                        openEvent = { navController.navigate("event/$it") },
                        createEvent = { navController.navigate("eventEdit/new") },
                        refreshToken = refresh,
                    )
                }
            }
            1 -> Box(Modifier.padding(padding)) {
                AnnouncementsScreen(
                    repo = repo,
                    isAdmin = session?.isAdmin == true,
                    openAnnouncement = { navController.navigate("ann/$it") },
                    createAnnouncement = { navController.navigate("annEdit/new") },
                    refreshToken = refresh,
                )
            }
            2 -> Box(Modifier.padding(padding)) {
                MembersScreen(repo = repo, openMember = { navController.navigate("member/$it") })
            }
            3 -> Box(Modifier.padding(padding)) {
                MeScreen(
                    repo = repo,
                    store = store,
                    session = session,
                    onOpenNotificationSettings = { navController.navigate("notifSettings") },
                    onOpenAppearance = { navController.navigate("appearance") },
                    onOpenAdmin = { },
                    onOpenUsers = { navController.navigate("adminUsers") },
                    onOpenInvitations = { navController.navigate("adminInvites") },
                    onOpenTags = { navController.navigate("adminTags") },
                    onSwitchServer = {
                        scope.launch {
                            // 切换战队即退出当前登录（不同站点账号体系独立）
                            // 先向旧站点解绑推送设备（此时 store 仍指向旧站点/旧 token），
                            // 否则旧战队账号的推送会继续发到本机
                            runCatching {
                                repo.unregisterDevice(PushManager.registrationId(context))
                            }
                            // DataStore 写失败不应崩溃，最多这次没清干净
                            runCatching { store.clear() }
                        }
                    },
                    onLogout = {
                        scope.launch {
                            // 先解绑推送设备，再清除本地会话
                            runCatching {
                                repo.unregisterDevice(PushManager.registrationId(context))
                            }
                            runCatching { store.clear() }
                        }
                    },
                    refreshToken = refresh,
                )
            }
        }
        }
    }
}
