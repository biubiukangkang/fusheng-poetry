package com.fusheng.poetry.ui

// 纸纤维噪点（定稿 §3.5）：140dp 平铺纹理，纸面 multiply 暗点 / 夜面 screen 亮点
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.random.Random

private const val TILE = 140

@Composable
fun Modifier.paperFibers(night: Boolean = false): Modifier {
    val brush = remember(night) {
        val bmp = Bitmap.createBitmap(TILE, TILE, Bitmap.Config.ARGB_8888)
        val max = if (night) 0.18f else 0.055f
        val rgb = if (night) 0xEA else 0x1C // 夜面亮点用笺色，纸面暗点用墨色
        for (y in 0 until TILE) {
            for (x in 0 until TILE) {
                val a = (Random.nextFloat() * max * 255).toInt()
                bmp.setPixel(x, y, (a shl 24) or (rgb shl 16) or (rgb shl 8) or rgb)
            }
        }
        ShaderBrush(BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
    }
    return drawWithContent {
        drawContent()
        drawRect(brush, blendMode = if (night) BlendMode.Screen else BlendMode.Multiply)
    }
}
