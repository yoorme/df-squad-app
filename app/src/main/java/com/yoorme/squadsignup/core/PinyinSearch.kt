package com.yoorme.squadsignup.core

import android.icu.text.Transliterator
import java.text.Normalizer

/**
 * 轻量中文拼音搜索工具：
 * - 命中原文（含中文）直接返回
 * - 命中全拼（如 zhangsan）
 * - 命中首字母（如 zs）
 *
 * 使用 Android ICU 自带的 Han-Latin 转换，不额外引入第三方依赖。
 */
object PinyinSearch {
    private val transliterator: Transliterator? by lazy {
        runCatching { Transliterator.getInstance("Han-Latin") }.getOrNull()
    }

    private fun normalizeLatin(input: String): String {
        val lower = input.lowercase()
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        val builder = StringBuilder(decomposed.length)
        decomposed.forEach { ch ->
            if (ch.category != CharCategory.NON_SPACING_MARK) builder.append(ch)
        }
        return builder.toString()
    }

    fun pinyin(text: String): String =
        normalizeLatin(transliterator?.transliterate(text) ?: text)

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
