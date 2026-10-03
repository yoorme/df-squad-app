package com.yoorme.squadsignup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yoorme.squadsignup.core.Repo
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.notify.Notifier
import com.yoorme.squadsignup.ui.Root
import com.yoorme.squadsignup.ui.theme.SquadTheme

class MainActivity : ComponentActivity() {

    // 通知点击的跳转意图（singleTask 下通过 onNewIntent 送达）
    private val pendingOpen = mutableStateOf<String?>(null)
    private val pendingId = mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.createChannels(this)
        requestNotificationPermission()
        readIntent(intent)

        setContent {
            SquadTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val store = remember { SessionStore(applicationContext) }
                val repo = remember { Repo(store) }
                Root(
                    store = store,
                    repo = repo,
                    windowWidth = windowSizeClass.widthSizeClass,
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
