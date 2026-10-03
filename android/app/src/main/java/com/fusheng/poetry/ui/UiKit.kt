package com.fusheng.poetry.ui

// 共享小组件（定稿 §6）：墨条按钮 / 次按钮 / 印章 / 圆钮 / 搜索行 / 区块标题
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.ui.theme.Ink
import com.fusheng.poetry.ui.theme.Ink2
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.KaiFont
import com.fusheng.poetry.ui.theme.Line
import com.fusheng.poetry.ui.theme.LineStrong
import com.fusheng.poetry.ui.theme.Paper
import com.fusheng.poetry.ui.theme.PaperHi
import com.fusheng.poetry.ui.theme.SansFont
import com.fusheng.poetry.ui.theme.Seal
import com.fusheng.poetry.ui.theme.SealText
import com.fusheng.poetry.ui.theme.SerifFont

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = SansFont,
        fontSize = 10.sp,
        letterSpacing = 0.22.em,
        color = Ink3,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

// 页面大题（定稿 §6.2 masthead）：26sp 宋体 + 右侧小字
@Composable
fun Masthead(title: String, sub: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            fontFamily = SerifFont,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            fontSize = 26.sp,
            letterSpacing = 0.16.em,
            color = Ink,
        )
        Box(Modifier.weight(1f))
        Text(
            sub,
            fontFamily = SansFont,
            fontSize = 10.sp,
            letterSpacing = 0.18.em,
            color = Ink3,
            modifier = Modifier.padding(bottom = 5.dp),
        )
    }
}

// 主按钮：墨条（定稿 §6.5）
@Composable
fun InkButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(if (enabled) Ink else Ink.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = SansFont,
            fontSize = 15.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            letterSpacing = 0.32.em,
            textAlign = TextAlign.Center,
            color = PaperHi,
        )
    }
}

// 次按钮：透明底细描边（定稿 §6.5）
@Composable
fun SubButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .height(38.dp)
            .border(1.dp, if (enabled) LineStrong else Line, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = SansFont,
            fontSize = 12.sp,
            letterSpacing = 0.14.em,
            color = if (enabled) Ink2 else Ink3.copy(alpha = 0.6f),
        )
    }
}

// 小标签（定稿 §6.8）：20dp 高，笺底次墨字
@Composable
fun MiniTag(text: String) {
    Box(
        Modifier
            .background(PaperHi, RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Text(text, fontFamily = SansFont, fontSize = 10.sp, letterSpacing = 0.06.em, color = Ink2)
    }
}

// 方印（定稿 §6.7）：Seal 底笺色字，楷体
@Composable
fun SealSquare(
    char: String,
    sizeDp: Int,
    fontSize: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(sizeDp.dp)
            .background(Seal, RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(char, fontFamily = KaiFont, fontSize = fontSize.sp, color = SealText)
    }
}

// 金印：透明底金描边金字（存图）
@Composable
fun SealGold(char: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(30.dp)
            .border(1.dp, com.fusheng.poetry.ui.theme.Gold, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(char, fontFamily = KaiFont, fontSize = 12.sp, color = com.fusheng.poetry.ui.theme.Gold)
    }
}

// 圆钮（定稿 §6.6）：拾 / 已收藏；未拾细描边墨字，已拾墨底纸字
@Composable
fun CircleKnob(
    char: String,
    picked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 32,
    enabled: Boolean = !picked,
) {
    Box(
        modifier
            .size(sizeDp.dp)
            .background(if (picked) Ink else Color.Transparent, CircleShape)
            .border(1.dp, if (picked) Ink else LineStrong, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            char,
            fontFamily = SerifFont,
            fontSize = (sizeDp * 0.41f).sp,
            color = if (picked) PaperHi else Ink,
        )
    }
}

// 墨线搜索行（定稿 §6.4）：48dp 高，底线焦点变墨
@Composable
fun SearchRow(
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    tail: String? = null,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Paper)
            .padding(bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 放大镜 16dp
                androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
                    val s = size.width / 24f
                    val style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.6f * s,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawCircle(Ink3, radius = 6f * s, center = androidx.compose.ui.geometry.Offset(11f * s, 11f * s), style = style)
                    drawLine(Ink3, androidx.compose.ui.geometry.Offset(16f * s, 16f * s), androidx.compose.ui.geometry.Offset(20f * s, 20f * s), strokeWidth = 1.6f * s, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = SansFont, fontSize = 14.sp, letterSpacing = 0.06.em, color = Ink),
                    cursorBrush = SolidColor(Ink),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                placeholder,
                                fontFamily = SansFont,
                                fontSize = 14.sp,
                                letterSpacing = 0.06.em,
                                color = Ink3,
                            )
                        }
                        inner
                    },
                )
                if (tail != null) {
                    Text(
                        tail,
                        fontFamily = SansFont,
                        fontSize = 10.sp,
                        letterSpacing = 0.14.em,
                        color = Ink3,
                    )
                }
            }
            // 底线：焦点变墨（画在行底）
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (focused) Ink else LineStrong),
            )
        }
    }
}
