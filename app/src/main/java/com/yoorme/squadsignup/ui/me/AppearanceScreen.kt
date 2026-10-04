package com.yoorme.squadsignup.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoorme.squadsignup.core.SessionStore
import com.yoorme.squadsignup.core.ThemeMode
import com.yoorme.squadsignup.ui.theme.DarkColors
import com.yoorme.squadsignup.ui.theme.LightColors
import com.yoorme.squadsignup.ui.theme.dynamicColorSupported
import kotlinx.coroutines.launch

/**
 * 外观设置：主题色在「默认（网站品牌色）」与「动态取色（Android 12+ 跟随壁纸）」间切换。
 * 明暗模式仍跟随系统。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(store: SessionStore, onBack: () -> Unit) {
    val themeMode by store.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DEFAULT)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()

    // 两种模式的预览色（当前明暗方案下）；Build 判断内联便于 lint 校验 API 31 门槛
    val defaultSwatches = swatchesOf(if (dark) DarkColors else LightColors)
    val dynamicSwatches = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        swatchesOf(
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        )
    } else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "主题色",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Card(Modifier.fillMaxWidth()) {
                ThemeOptionRow(
                    selected = themeMode == ThemeMode.DEFAULT,
                    title = "默认",
                    subtitle = "战队品牌色，与网站配色一致",
                    swatches = defaultSwatches,
                    onSelect = { scope.launch { store.setThemeMode(ThemeMode.DEFAULT) } },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ThemeOptionRow(
                    selected = themeMode == ThemeMode.DYNAMIC,
                    title = "动态取色",
                    subtitle = if (dynamicColorSupported) {
                        "跟随壁纸自动生成整套配色（Android 12+）"
                    } else {
                        "当前系统不支持，需要 Android 12 及以上"
                    },
                    swatches = dynamicSwatches,
                    enabled = dynamicColorSupported,
                    onSelect = { scope.launch { store.setThemeMode(ThemeMode.DYNAMIC) } },
                )
            }

            Spacer(Modifier.height(2.dp))
            Text(
                "预览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "队",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("主要文字", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "次要文字（onSurfaceVariant）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = {}) { Text("按钮") }
                }
            }
            Text(
                "明暗模式跟随系统设置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    swatches: List<Color>,
    onSelect: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            swatches.forEach { Swatch(it) }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}

private fun swatchesOf(scheme: androidx.compose.material3.ColorScheme): List<Color> = listOf(
    scheme.primary,
    scheme.primaryContainer,
    scheme.secondaryContainer,
    scheme.tertiaryContainer,
)
