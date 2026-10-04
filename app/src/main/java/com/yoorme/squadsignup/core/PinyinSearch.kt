package com.yoorme.squadsignup.core

import android.icu.text.Transliterator
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.Normalizer

/**
 * 轻量中文拼音搜索工具：
 * - 命中原文（含中文）直接返回
 * - 命中全拼（如 zhangsan）
 * - 命中首字母（如 zs）
 *
 * 使用 Android ICU 自带的 Han-Latin 转换，不额外引入第三方依赖。
 * 注意：android.icu.text.Transliterator 从 Android 10（API 29）才有；
 * 在 API 26–28 的机器上拼音转换不可用，匹配会自动退化为「原文包含」，
 * 不会崩溃（若需要覆盖老机型，可考虑引入 tinypinyin 之类的字表库）。
 */
object PinyinSearch {

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun icuTransliterate(text: String): String =
        Transliterator.getInstance("Han-Latin").transliterate(text)

    private fun normalizeLatin(input: String): String {
        val lower = input.lowercase()
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        decomposed.forEach { ch ->
            if (ch.category != CharCategory.NON_SPACING_MARK) builder.append(ch)
        }
        return builder.toString()
    }

    fun pinyin(text: String): String {
        // API 29 以下没有 ICU Transliterator：跳过转换，只做拉丁字母归一化
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return normalizeLatin(text)
        val converted = runCatching { icuTransliterate(text) }.getOrDefault(text)
        return normalizeLatin(converted)
    }

    private fun compact(text: String): String = text.replace(" ", "")

    fun matches(query: String, target: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (target.contains(q, ignoreCase = true)) return true

        val py = pinyin(target)
        val qCompact = compact(q.lowercase())
        if (compact(py).contains(qCompact)) return true

        val initials = py.split(' ')
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.lowercaseChar() }
            .joinToString("")
        return initials.contains(qCompact)
    }
}
