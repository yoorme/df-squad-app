package com.yoorme.squadsignup.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// 服务端时间均为 ISO8601（UTC）；展示统一用北京时间（与网站一致）
object TimeFmt {

    private val ZH = ZoneId.of("Asia/Shanghai")

    fun parse(iso: String?): Instant? = try {
        Instant.parse(iso)
    } catch (e: Exception) {
        try {
            java.time.OffsetDateTime.parse(iso).toInstant()
        } catch (e2: Exception) {
            null
        }
    }

    private val WEEKDAY = DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)

    private fun fmt(when1: Instant, pattern: String): String =
        DateTimeFormatter.ofPattern(pattern).withZone(ZH).format(when1)

    fun full(iso: String?): String = parse(iso)?.let { fmt(it, "yyyy-MM-dd HH:mm") } ?: "--"
    fun short(iso: String?): String = parse(iso)?.let { fmt(it, "MM-dd HH:mm") } ?: "--"

    fun minutesUntil(iso: String?, now: Instant = Instant.now()): Int? =
        parse(iso)?.let { ChronoUnit.MINUTES.between(now, it).toInt() }

    /** 剩余时间展示：>=1天显示天，>=1小时显示小时，否则显示分钟；已开始返回 null */
    fun remainingLabel(iso: String?, now: Instant = Instant.now()): String? {
        val minutes = minutesUntil(iso, now) ?: return null
        if (minutes <= 0) return null
        val days = minutes / (60 * 24)
        val hours = minutes / 60
        return when {
            days >= 1 -> "剩余 ${days} 天"
            hours >= 1 -> "剩余 ${hours} 小时"
            else -> "剩余 ${minutes} 分钟"
        }
    }

    /** 星期几，例如 星期一 */
    fun weekday(iso: String?): String? =
        parse(iso)?.let { WEEKDAY.withZone(ZH).format(it) }
}
