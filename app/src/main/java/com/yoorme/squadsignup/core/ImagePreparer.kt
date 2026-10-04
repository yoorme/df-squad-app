package com.yoorme.squadsignup.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 公告图片上传前的本地处理：
 * - ≤5MB：原样返回（与网站端行为一致：不重编码、不丢 EXIF）
 * - >5MB：解码 → 按 EXIF 纠正方向 → 等比缩到长边 2048 → 压 JPEG，直到 ≤5MB
 *
 * 手机拍照普遍超过服务端 5MB 硬限制，所以超限时自动压缩而不是直接拒绝。
 * 输出统一为 JPEG（服务端按魔数嗅探真实格式，扩展名不影响）。
 */
object ImagePreparer {

    const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024
    private const val MAX_DIMENSION = 2048
    private const val QUALITY = 85
    private const val QUALITY_FALLBACK = 78

    /** 返回 (可上传字节, MIME)；失败时 Result 携带可直接展示的中文原因 */
    fun prepare(context: Context, uri: Uri): Result<Pair<ByteArray, String>> = try {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val raw = context.contentResolver.openInputStream(uri)
            ?.use { readAtMost(it, MAX_UPLOAD_BYTES + 1) }
            ?: return Result.failure(IllegalArgumentException("无法读取所选图片"))
        if (raw.size <= MAX_UPLOAD_BYTES) {
            Result.success(raw to mime)
        } else if (mime.contains("gif", true)) {
            // 动图无法用位图压缩（只会保留第一帧），明确拒绝而不是悄悄毁掉动图
            Result.failure(IllegalArgumentException("GIF 超过 5MB 且无法自动压缩，请换一张"))
        } else {
            Result.success(compress(context, uri) to "image/jpeg")
        }
    } catch (e: Exception) {
        Result.failure(IllegalArgumentException("处理图片失败：${e.message ?: "未知错误"}"))
    }

    /** 最多读 limit 字节：用于判断是否超限，避免把超大文件整个读进内存 */
    private fun readAtMost(stream: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        while (out.size() < limit) {
            val n = stream.read(buf, 0, minOf(buf.size, limit - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun compress(context: Context, uri: Uri): ByteArray {
        var bitmap = decodeScaled(context, uri)
            ?: throw IllegalArgumentException("无法解码该图片（格式可能不受支持）")
        bitmap = applyExifOrientation(context, uri, bitmap)
        if (bitmap.hasAlpha()) bitmap = drawOnWhite(bitmap)

        compressJpeg(bitmap, QUALITY)?.let { return it }

        // 仍超限：精确缩到长边 2048 再压一次（2048 长边 q78 通常只有几百 KB）
        val scaled = scaleToMax(bitmap, MAX_DIMENSION)
        if (scaled !== bitmap) bitmap.recycle()
        return compressJpeg(scaled, QUALITY_FALLBACK)
            ?: throw IllegalArgumentException("图片过大，压缩后仍超过 5MB，请换一张")
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return if (out.size() <= MAX_UPLOAD_BYTES) out.toByteArray() else null
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    /** 竖拍照片的像素数据是横向的，方向信息只存在 EXIF 里；重编码时必须手动纠正 */
    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleToMax(bitmap: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxSide
        return bitmap.scale(
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
        )
    }

    /** 转 JPEG 会丢 alpha，透明 PNG 直接压会变黑底，先铺白 */
    private fun drawOnWhite(bitmap: Bitmap): Bitmap {
        val out = createBitmap(bitmap.width, bitmap.height)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        bitmap.recycle()
        return out
    }
}
