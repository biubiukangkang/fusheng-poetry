@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.fusheng.poetry.ui.theme

// 设计定稿（输出/浮生诗集_定稿/设计定稿.md §3.2-3.3）：纸墨手卷色板 + 字体
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.fusheng.poetry.R

// 色彩令牌（定稿 §3.2）
val Paper = Color(0xFFD9D6C3) // 纸 · 全局背景
val PaperHi = Color(0xFFEAE7D8) // 笺 · 卷面、纸片、浮层
val PaperDeep = Color(0xFFC8C5B0) // 旧纸 · 辅助底
val Ink = Color(0xFF1C211E) // 墨 · 主文字、主按钮
val Ink2 = Color(0xFF4A5149) // 次墨 · 注记、次级正文
val Ink3 = Color(0xFF6F776D) // 元信息 · 日期、出处、占位
val Seal = Color(0xFFB33A2E) // 朱砂 · 印章、加号、选中
val SealDeep = Color(0xFF8E2A22) // 深朱砂 · 按下、破坏性
val Gold = Color(0xFFA88452) // 旧金 · 木轴细线、金印
val Night = Color(0xFF171B19) // 夜墨 · 底栏、夜展背景
val NightLine = Color(0x29EAE7D8) // 夜线 rgba(234,231,216,.16)
val Line = Color(0x241C211E) // 发丝线 rgba(28,33,30,.14)
val LineStrong = Color(0x421C211E) // 强线 rgba(28,33,30,.26)
val SealText = Color(0xFFFFF8F1) // 印上文字
val BarIdle = Color(0xFF9AA093) // 底栏未激活
val BarActive = Color(0xFFF0EDE0) // 底栏激活

// 宋体：本机同款 Noto Serif SC 可变字体（OFL），设计稿渲染一致
val SerifFont = FontFamily(
    Font(R.font.noto_serif_sc_vf, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.noto_serif_sc_vf, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.noto_serif_sc_vf, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.noto_serif_sc_vf, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

// 楷体：霞鹜文楷（OFL），注记 / 印章 / 空状态
val KaiFont = FontFamily(Font(R.font.lxgw_wenkai, FontWeight.Normal))

// 黑体：系统默认（Android 内置思源黑体系，定稿 §10.4 允许）
val SansFont = FontFamily.Default

private val PaperColors = lightColorScheme(
    primary = Ink,
    onPrimary = PaperHi,
    secondary = Ink2,
    onSecondary = PaperHi,
    tertiary = Seal,
    onTertiary = SealText,
    background = Paper,
    onBackground = Ink,
    surface = PaperHi,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = Ink2,
    outline = LineStrong,
    error = SealDeep,
)

@Composable
fun FuShengTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PaperColors, content = content)
}
