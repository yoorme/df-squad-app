package com.yoorme.squadsignup.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Material 3 动效令牌。
 * 缓动曲线与网站 globals.css 的 --md-sys-motion-easing-* 是同一组贝塞尔参数，
 * 保证 App 与网页观感一致。
 *
 * 约定：空间类动画（位移、尺寸）用弹簧；效果类动画（透明度、颜色）用短时长缓动。
 */
object SquadMotion {

    // ---- 缓动曲线（--md-sys-motion-easing-*）----
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    // ---- 时长（--md-sys-motion-duration-*，取常用档位）----
    const val Short4 = 200   // 200ms：淡出/短效果
    const val Medium2 = 300  // 300ms：页面转场
    const val Long2 = 500    // 500ms：大范围共享轴

    /** 空间动画：位移、尺寸变化（轻微回弹的弹簧） */
    fun <T> spatial(stiffness: Float = 700f): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = stiffness)

    /** 效果动画入场：透明度/颜色（减速曲线） */
    fun <T> effectsIn(durationMillis: Int = Short4): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = EmphasizedDecelerate)

    /** 效果动画出场：透明度/颜色（加速曲线，更快） */
    fun <T> effectsOut(durationMillis: Int = Short4): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = EmphasizedAccelerate)
}
