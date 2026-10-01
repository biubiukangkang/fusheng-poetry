package com.fusheng.poetry.ui

// 竖排诗（定稿 §3.3 §10.3）：列从右到左、字从上到下，标点占一个字位
// 每字占固定格 cell=字号×(1+字距)（对齐 CSS vertical-rl 的 letter-spacing 沿竖轴语义）
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.ui.theme.Ink
import com.fusheng.poetry.ui.theme.SerifFont

// 诗句拆列：标点留在行尾（「人间有味，是清欢」→ 2 列）
fun splitPoemLines(s: String): List<String> {
    val acc = StringBuilder()
    s.forEach { ch ->
        acc.append(ch)
        if (ch in "，。？！；、") acc.append(' ') // 行分隔哨兵
    }
    return acc.split(' ').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf(s) }
}

// 汉字数字：印章与小字用（廿九、二〇二六年九月、一千二百首）
internal val CN_DIGITS = "〇一二三四五六七八九"

fun cnNum(n: Int): String = when {
    n < 10 -> CN_DIGITS[n].toString()
    n < 20 -> "十" + (if (n % 10 > 0) CN_DIGITS[n % 10].toString() else "")
    n < 100 -> CN_DIGITS[n / 10] + "十" + (if (n % 10 > 0) CN_DIGITS[n % 10].toString() else "")
    n < 1000 -> CN_DIGITS[n / 100] + "百" + when {
        n % 100 == 0 -> ""
        n % 100 < 10 -> "零" + CN_DIGITS[n % 100]
        else -> cnNum(n % 100)
    }
    n < 10000 -> CN_DIGITS[n / 1000] + "千" + when {
        n % 1000 == 0 -> ""
        n % 1000 < 100 -> "零" + cnNum(n % 1000)
        else -> cnNum(n % 1000)
    }
    else -> n.toString()
}

// 日印：1-10 一…十，11-19 十一…，20 二十，21-29 廿一…，30 三十，31 卅一
fun dayCn(d: Int): String = when {
    d == 20 -> "二十"
    d == 30 -> "三十"
    d == 31 -> "卅一"
    d in 21..29 -> "廿" + CN_DIGITS[d % 10]
    else -> cnNum(d)
}

fun yearCn(y: Int): String = y.toString().map { CN_DIGITS[it - '0'] }.joinToString("")

/** 竖排字符列：每字一格（格高=字号×(1+字距)），格内居中 */
@Composable
fun VerticalChars(
    text: String,
    fontSize: TextUnit,
    color: Color,
    spacingEm: Float,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
) {
    val fontScale = LocalDensity.current.fontScale
    val cellDp = fontSize.value * fontScale * (1 + spacingEm)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        text.forEach { ch ->
            Box(
                Modifier.height(cellDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ch.toString(),
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = weight,
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        color = color,
                    ),
                )
            }
        }
    }
}

@Composable
fun VerticalPoem(
    lines: List<String>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = Ink,
    letterSpacingEm: Float = 0.18f,
    lineHeightFactor: Float = 1.28f, // 行距即列距（竖排行高是列间距）
    maxColumns: Int = Int.MAX_VALUE, // focus line 最多两列，全文进诗详情
    weight: FontWeight = FontWeight.Normal,
) {
    val colGap = with(LocalDensity.current) { (fontSize.value * (lineHeightFactor - 1f)).dp }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(colGap),
            verticalAlignment = Alignment.Top,
        ) {
            // Rtl 下第一个子项在最右 = 第一句
            lines.take(maxColumns).forEach { line ->
                VerticalChars(line, fontSize, color, letterSpacingEm, SerifFont, weight = weight)
            }
        }
    }
}
