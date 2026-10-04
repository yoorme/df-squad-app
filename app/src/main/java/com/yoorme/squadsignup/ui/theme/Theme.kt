package com.yoorme.squadsignup.ui.theme

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.yoorme.squadsignup.core.ThemeMode

// ============ 默认配色（品牌色） ============
// 与网站 df-squad-web/src/app/globals.css 的静态回退配色逐角色对应
// （网站「从战队图标取色」失败时即回落到这套颜色）。修改需与网站同步。

internal val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = Color(0xFF001D36),
    inversePrimary = Color(0xFFA0C9FF),
    secondary = Color(0xFF575E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31101D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE6E6EB),
    onSurfaceVariant = Color(0xFF44464F),
    surfaceBright = Color(0xFFFCFCFF),
    surfaceDim = Color(0xFFDCDCE1),
    surfaceContainer = Color(0xFFF1F1F5),
    surfaceContainerHigh = Color(0xFFECECF1),
    surfaceContainerHighest = Color(0xFFE6E6EB),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF),
    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF2F0F4),
    scrim = Color(0xFF000000),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA0C9FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD6E7FF),
    inversePrimary = Color(0xFF1A73E8),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293042),
    secondaryContainer = Color(0xFF3F4759),
    onSecondaryContainer = Color(0xFFDBE2F9),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF482533),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE5E1E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE5E1E6),
    surfaceVariant = Color(0xFF44464F),
    onSurfaceVariant = Color(0xFFC4C6CF),
    surfaceBright = Color(0xFF35353A),
    surfaceDim = Color(0xFF121214),
    surfaceContainer = Color(0xFF232327),
    surfaceContainerHigh = Color(0xFF2E2E32),
    surfaceContainerHighest = Color(0xFF39393D),
    surfaceContainerLow = Color(0xFF1F1F23),
    surfaceContainerLowest = Color(0xFF0E0E11),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44464F),
    inverseSurface = Color(0xFFE5E1E6),
    inverseOnSurface = Color(0xFF303034),
    scrim = Color(0xFF000000),
)

/** 动态取色（跟随壁纸）仅 Android 12+ 可用 */
val dynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun SquadTheme(
    themeMode: ThemeMode = ThemeMode.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Build 版本判断内联，便于 lint 校验动态取色的 API 门槛（31+）
    val target = when {
        themeMode == ThemeMode.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = animateColorScheme(target), content = content)
}

/**
 * 主题切换平滑过渡：整套 ColorScheme 逐角色做 420ms 颜色动画。
 * 切换「默认 ↔ 动态取色」或系统明暗切换时整屏渐变，而不是硬切。
 * （做法参考 NeriPlayer 的 animateColorScheme）
 */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val t = updateTransition(target, label = "colorScheme")
    val spec = tween<Color>(420, easing = SquadMotion.EmphasizedDecelerate)

    val primary by t.animateColor(transitionSpec = { spec }) { it.primary }
    val onPrimary by t.animateColor(transitionSpec = { spec }) { it.onPrimary }
    val primaryContainer by t.animateColor(transitionSpec = { spec }) { it.primaryContainer }
    val onPrimaryContainer by t.animateColor(transitionSpec = { spec }) { it.onPrimaryContainer }
    val inversePrimary by t.animateColor(transitionSpec = { spec }) { it.inversePrimary }
    val secondary by t.animateColor(transitionSpec = { spec }) { it.secondary }
    val onSecondary by t.animateColor(transitionSpec = { spec }) { it.onSecondary }
    val secondaryContainer by t.animateColor(transitionSpec = { spec }) { it.secondaryContainer }
    val onSecondaryContainer by t.animateColor(transitionSpec = { spec }) { it.onSecondaryContainer }
    val tertiary by t.animateColor(transitionSpec = { spec }) { it.tertiary }
    val onTertiary by t.animateColor(transitionSpec = { spec }) { it.onTertiary }
    val tertiaryContainer by t.animateColor(transitionSpec = { spec }) { it.tertiaryContainer }
    val onTertiaryContainer by t.animateColor(transitionSpec = { spec }) { it.onTertiaryContainer }
    val error by t.animateColor(transitionSpec = { spec }) { it.error }
    val onError by t.animateColor(transitionSpec = { spec }) { it.onError }
    val errorContainer by t.animateColor(transitionSpec = { spec }) { it.errorContainer }
    val onErrorContainer by t.animateColor(transitionSpec = { spec }) { it.onErrorContainer }
    val background by t.animateColor(transitionSpec = { spec }) { it.background }
    val onBackground by t.animateColor(transitionSpec = { spec }) { it.onBackground }
    val surface by t.animateColor(transitionSpec = { spec }) { it.surface }
    val onSurface by t.animateColor(transitionSpec = { spec }) { it.onSurface }
    val surfaceVariant by t.animateColor(transitionSpec = { spec }) { it.surfaceVariant }
    val onSurfaceVariant by t.animateColor(transitionSpec = { spec }) { it.onSurfaceVariant }
    val surfaceBright by t.animateColor(transitionSpec = { spec }) { it.surfaceBright }
    val surfaceDim by t.animateColor(transitionSpec = { spec }) { it.surfaceDim }
    val surfaceContainer by t.animateColor(transitionSpec = { spec }) { it.surfaceContainer }
    val surfaceContainerHigh by t.animateColor(transitionSpec = { spec }) { it.surfaceContainerHigh }
    val surfaceContainerHighest by t.animateColor(transitionSpec = { spec }) { it.surfaceContainerHighest }
    val surfaceContainerLow by t.animateColor(transitionSpec = { spec }) { it.surfaceContainerLow }
    val surfaceContainerLowest by t.animateColor(transitionSpec = { spec }) { it.surfaceContainerLowest }
    val outline by t.animateColor(transitionSpec = { spec }) { it.outline }
    val outlineVariant by t.animateColor(transitionSpec = { spec }) { it.outlineVariant }
    val inverseSurface by t.animateColor(transitionSpec = { spec }) { it.inverseSurface }
    val inverseOnSurface by t.animateColor(transitionSpec = { spec }) { it.inverseOnSurface }

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
    )
}
