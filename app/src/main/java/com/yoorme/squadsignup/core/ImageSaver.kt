package com.yoorme.squadsignup.core

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 把公告图片保存到本机相册。
 * - Android 10+：MediaStore 写入 Pictures/战队报名，无需任何权限
 * - Android 9-：写公共 Pictures 目录，需要 WRITE_EXTERNAL_STORAGE（调用方负责申请后调用）
 */
object ImageSaver {

    const val ALBUM_DIR = "战队报名"

    /** Android 9 及以下写公共目录需要存储权限；10+ 不需要 */
    fun needsLegacyPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** 成功返回可直接展示的提示语；失败返回可展示的原因 */
    suspend fun save(context: Context, url: String, fileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = fetch(url)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaMediaStore(context, fileName, bytes)
                } else {
                    saveLegacy(context, fileName, bytes)
                }
                Result.success("已保存到相册（$ALBUM_DIR）")
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("保存失败：${e.message ?: "未知错误"}"))
            }
        }

    private fun fetch(url: String): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            // 服务端限制单图 ≤5MB，这里再加一道内存保护
            val limit = 20 * 1024 * 1024
            val out = java.io.ByteArrayOutputStream()
            conn.inputStream.use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > limit) throw IllegalStateException("图片过大")
                }
            }
            return out.toByteArray()
        } finally {
            conn.disconnect()
        }
    }

    private fun saveViaMediaStore(context: Context, fileName: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeFor(fileName))
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$ALBUM_DIR",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法创建相册文件")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("无法写入相册文件")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, fileName: String, bytes: ByteArray) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            ALBUM_DIR,
        )
        if (!dir.exists() && !dir.mkdirs()) throw IllegalStateException("无法创建相册目录")
        var file = File(dir, fileName)
        if (file.exists()) file = File(dir, "${System.currentTimeMillis()}_$fileName")
        file.writeBytes(bytes)
        // 通知相册索引新文件（Android 9 及以下不会自动扫描）
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    private fun mimeFor(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }
}
