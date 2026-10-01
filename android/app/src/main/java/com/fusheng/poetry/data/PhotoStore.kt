package com.fusheng.poetry.data

// 照片落盘：解码 → EXIF 摆正 → 按存储质量档位缩宽压缩 → filesDir/photos/<photoId>.jpg
// 档位：0 高清(2000,q90) / 1 标准(1600,q80) / 2 省空间(1080,q70)
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

object PhotoStore {
    private fun params(quality: Int): Pair<Int, Int> = when (quality) {
        0 -> 2000 to 90
        2 -> 1080 to 70
        else -> 1600 to 80
    }

    fun save(context: Context, uri: Uri): String {
        val (maxWidth, jpegQuality) = params(SettingsStore.photoQuality(context))
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2

        val src = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }!!

        val rotation = resolver.openInputStream(uri)!!.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        val upright = if (rotation != 0f) {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(rotation) }, true)
        } else src

        val final = if (upright.width > maxWidth) {
            Bitmap.createScaledBitmap(upright, maxWidth, upright.height * maxWidth / upright.width, true)
        } else upright

        val photoId = UUID.randomUUID().toString()
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        File(dir, "$photoId.jpg").outputStream().use { out ->
            final.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
        }
        return photoId
    }

    fun file(context: Context, photoId: String): File =
        File(File(context.filesDir, "photos"), "$photoId.jpg")
}
