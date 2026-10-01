package com.fusheng.poetry.ui

// 导出图（定稿 §5.1 作品夜展版式）：夜墨底 + 笺面卷轴（左金线）+ 竖排宋体诗 +
// 楷体注记 + 朱砂日期印；字体用内置 Noto Serif SC / 霞鹜文楷
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.fusheng.poetry.R
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

object ShareImage {
    private const val W = 1080
    private const val S = 3f // 设计稿 dp → px 倍率（360dp 稿 × 3 = 1080）

    private val NIGHT = Color.parseColor("#171B19")
    private val SCROLL_PAPER = Color.parseColor("#F0EDE0")
    private val INK = Color.parseColor("#1C211E")
    private val INK2 = Color.parseColor("#4A5149")
    private val INK3 = Color.parseColor("#6F776D")
    private val SEAL = Color.parseColor("#B33A2E")
    private val SEAL_TEXT = Color.parseColor("#FFF8F1")
    private val GOLD = Color.parseColor("#A88452")
    private val HAIRLINE = Color.parseColor("#241C211E")

    /** 生成夜展版式导出图并拉起系统分享面板（createdAt ISO，用于日期印） */
    fun share(
        context: Context,
        photoFile: File,
        focusLine: String,
        meta: String,
        note: String,
        createdAt: String? = null,
    ) {
        val file = generate(context, photoFile, focusLine, meta, note, createdAt)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "存为图片"))
    }

    private fun generate(
        context: Context,
        photoFile: File,
        focusLine: String,
        meta: String,
        note: String,
        createdAt: String?,
    ): File {
        val serif = ResourcesCompat.getFont(context, R.font.noto_serif_sc_vf) ?: Typeface.SERIF
        val kai = ResourcesCompat.getFont(context, R.font.lxgw_wenkai) ?: serif
        val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // 卷面几何（×3）
        val margin = (28 * S).toInt() // 卷面左右边距
        val frameW = W - margin * 2 // 912
        val padH = (22 * S).toInt() // 内边距水平
        val padTop = (20 * S).toInt()
        val padBottom = (24 * S).toInt()
        val imgW = frameW - padH * 2 // 780
        val gap = (18 * S).toInt() // 图与文字区
        val poemFont = (23 * S) // 竖排字号
        val poemColGap = poemFont * 0.28f
        val poemAreaH = (236 * S).toInt()

        val photo = BitmapFactory.decodeFile(photoFile.absolutePath)
        var imgH = (200 * S).toInt()
        if (photo != null) {
            val ratio = (photo.width.toFloat() / photo.height).coerceIn(1f / 2.2f, 2.2f)
            imgH = (imgW / ratio).toInt().coerceAtMost((300 * S).toInt())
        }

        val frameH = padTop + imgH + gap + poemAreaH + padBottom
        val nightTop = (44 * S).toInt()
        val nightBottom = (40 * S).toInt()
        val H = nightTop + frameH + nightBottom

        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(NIGHT)

        // 卷面：笺色 + 发丝边 + 左侧旧金细线
        val frameL = margin.toFloat()
        val frameT = nightTop.toFloat()
        val framePaint = Paint().apply { color = SCROLL_PAPER }
        canvas.drawRect(frameL, frameT, frameL + frameW, frameT + frameH, framePaint)
        canvas.drawRect(
            frameL, frameT, frameL + 1f, frameT + frameH,
            Paint().apply { color = HAIRLINE },
        )
        val goldLine = Paint().apply { color = GOLD; alpha = 115 }
        canvas.drawRect(frameL + 30f, frameT + 54f, frameL + 31f, frameT + frameH - 54f, goldLine)

        val innerL = frameL + padH
        var y = frameT + padTop

        // 图片：按原比例，直角，轻降饱和
        if (photo != null) {
            val cm = ColorMatrix()
            cm.setSaturation(0.9f)
            val saturate = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            val scale = max(imgW.toFloat() / photo.width, imgH.toFloat() / photo.height)
            val dw = photo.width * scale
            val dh = photo.height * scale
            canvas.save()
            canvas.clipRect(innerL, y, innerL + imgW, y + imgH)
            canvas.drawBitmap(photo, null, RectF(innerL + (imgW - dw) / 2, y + (imgH - dh) / 2, innerL + (imgW - dw) / 2 + dw, y + (imgH - dh) / 2 + dh), saturate)
            canvas.restore()
            photo.recycle()
        }
        y += imgH + gap

        // 竖排诗：列右到左、字上到下（卷面文字区左半 64dp 列区）
        val poemLines = splitPoemColumns(focusLine)
        val poemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK2
            textSize = poemFont
            typeface = serif
        }
        val poemAreaW = poemLines.size * poemFont + (poemLines.size - 1) * poemColGap
        var colX = innerL + poemAreaW - poemFont / 2f // 最右列起点
        poemLines.forEach { line ->
            var chY = y + poemFont * 0.8f
            line.forEach { ch ->
                canvas.drawText(ch.toString(), colX, chY, poemPaint)
                chY += poemFont * 1.18f
            }
            colX -= poemFont + poemColGap
        }

        // 右侧：注记（楷体）、出处、日期印行
        val textL = innerL + poemAreaW + (18 * S)
        val textW = frameL + frameW - padH - textL
        var ty = y + poemFont * 0.4f
        if (note.isNotEmpty()) {
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK2
                textSize = 14 * S
                typeface = kai
            }
            wrapText(note, notePaint, textW).forEach { lineText ->
                canvas.drawText(lineText, textL, ty, notePaint)
                ty += 14 * S * 1.85f
            }
            ty += 10 * S
        }
        if (meta.isNotEmpty()) {
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK3
                textSize = 11 * S
                typeface = sans
                letterSpacing = 0.12f * 11 * S / 10f
            }
            canvas.drawText(meta, textL, ty, metaPaint)
            ty += 14 * S
        }
        // 日期印 + 数字日期（贴文字区底部）
        val local = createdAt?.let {
            runCatching { LocalDateTime.ofInstant(Instant.parse(it), ZoneId.systemDefault()) }.getOrNull()
        }
        if (local != null) {
            val seal = 22 * S
            val sealY = frameT + frameH - padBottom - seal
            val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SEAL }
            canvas.drawRoundRect(RectF(textL, sealY, textL + seal, sealY + seal), 4 * S, 4 * S, sealPaint)
            val sealText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SEAL_TEXT
                textSize = 10 * S
                typeface = kai
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(dayCn(local.dayOfMonth), textL + seal / 2f, sealY + seal / 2f + 3.5f * S, sealText)
            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK3
                textSize = 10 * S
                typeface = sans
                letterSpacing = 0.16f * 10 * S / 10f
            }
            canvas.drawText(
                "%04d.%02d.%02d".format(local.year, local.monthValue, local.dayOfMonth),
                textL + seal + 10 * S,
                sealY + seal / 2f + 3.5f * S,
                datePaint,
            )
        }

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "share-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()
        return file
    }

    /** 竖排列拆分：按标点断列，最多 3 列（超出截断，全文在详情） */
    internal fun splitPoemColumns(verse: String): List<String> = splitPoemLines(verse).take(3)

    /** 按像素宽度折行 */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        text.forEach { ch ->
            cur.append(ch)
            if (paint.measureText(cur.toString()) > maxWidth) {
                cur.deleteCharAt(cur.length - 1)
                out.add(cur.toString())
                cur = StringBuilder().append(ch)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out.ifEmpty { listOf(text) }
    }

    /** ≤14 字单行；否则在 14 字内找标点断行（保留备用） */
    internal fun splitVerse(verse: String): List<String> {
        if (verse.length <= 14) return listOf(verse)
        val cut = verse.take(14)
        val punct = max(cut.lastIndexOf('，'), max(cut.lastIndexOf('。'), cut.lastIndexOf('；')))
        val at = if (punct > 7) punct + 1 else 14
        return listOf(verse.take(at), verse.drop(at))
    }
}
