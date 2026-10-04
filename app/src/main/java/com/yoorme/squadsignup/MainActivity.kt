package com.yoorme.squadsignup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.core.ThemeMode
import com.yoorme.squadsignup.notify.Notifier
import com.yoorme.squadsignup.ui.Root
import com.yoorme.squadsignup.ui.theme.SquadTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MainActivity : ComponentActivity() {

    // 通知点击的跳转意图（singleTask 下通过 onNewIntent 送达）
    private val pendingOpen = mutableStateOf<String?>(null)
    private val pendingId = mutableStateOf<String?>(null)

    // 主题偏好（读取完成前保持启动画面，避免先默认色再跳动态色的闪烁）
    private val themeMode = mutableStateOf<ThemeMode?>(null)
    // 会话首读结果：作为 Root 里 token 状态的初值，避免已登录用户冷启动先闪一下登录页
    private val initialToken = mutableStateOf<String?>(null)
    private val sessionReady = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.createChannels(this)
        requestNotificationPermission()
        readIntent(intent)

        val store = SessionStore(applicationContext)
        val repo = Repo(store)

        splash.setKeepOnScreenCondition { themeMode.value == null || !sessionReady.value }
        lifecycleScope.launch {
            themeMode.value = runCatching {
                withTimeout(2000) { store.themeMode.first() }
            }.getOrDefault(ThemeMode.DEFAULT)
            initialToken.value = runCatching {
                withTimeout(2000) { store.token.first() }
            }.getOrNull()
            sessionReady.value = true
        }

        setContent {
            SquadTheme(themeMode = themeMode.value ?: ThemeMode.DEFAULT) {
                val windowSizeClass = calculateWindowSizeClass(this)
                Root(
                    store = store,
                    repo = repo,
                    windowWidth = windowSizeClass.widthSizeClass,
                    initialToken = initialToken.value,
                    pendingOpen = pendingOpen.value,
                    pendingId = pendingId.value,
                    onPendingHandled = {
                        pendingOpen.value = null
                        pendingId.value = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    // Android 13+ 通知权限需要运行时请求，否则推送到达也不显示
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun readIntent(intent: Intent?) {
        val open = intent?.getStringExtra(Notifier.EXTRA_OPEN) ?: return
        pendingOpen.value = open
        pendingId.value = intent.getStringExtra(Notifier.EXTRA_TARGET_ID)
    }
}
