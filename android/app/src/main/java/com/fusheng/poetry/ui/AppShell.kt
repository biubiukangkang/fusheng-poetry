package com.fusheng.poetry.ui

// 顶栏 + 木轴底栏五键（定稿 §5 §6.1，实现参考 jz.css .nav/.topbar/.plus-seal）
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.ui.theme.BarActive
import com.fusheng.poetry.ui.theme.BarIdle
import com.fusheng.poetry.ui.theme.Gold
import com.fusheng.poetry.ui.theme.Ink
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.Line
import com.fusheng.poetry.ui.theme.Night
import com.fusheng.poetry.ui.theme.NightLine
import com.fusheng.poetry.ui.theme.Paper
import com.fusheng.poetry.ui.theme.PaperHi
import com.fusheng.poetry.ui.theme.Seal
import com.fusheng.poetry.ui.theme.SealDeep
import com.fusheng.poetry.ui.theme.SealText
import com.fusheng.poetry.ui.theme.SansFont
import com.fusheng.poetry.ui.theme.SerifFont

// 五键：中间「＋」纯加号无文字，点击进创作页（不参与 tab 切换）
enum class Tab(val label: String, val pageTitle: String) {
    HALL("浮生馆", "浮生诗集"),
    SEEK("觅诗", "觅诗"),
    ANTHOLOGY("诗集", "诗集"),
    ME("我", "我"),
}

// 线性图标（定稿 §3.6）：viewport 24，stroke 1.45，圆角端点；几何对齐实现参考 SVG
private enum class Icon { HALL, SEEK, ANTHOLOGY, ME }

@Composable
private fun StrokeIcon(icon: Icon, color: Color, sizeDp: Float = 22f) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val s = size.width / 24f
        val style = Stroke(width = 1.45f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            Icon.HALL -> {
                // 房子 + 门：M4 20V10l8-5 8 5v10 M8 20v-6h8v6
                val p = Path()
                p.moveTo(4 * s, 20 * s); p.lineTo(4 * s, 10 * s)
                p.lineTo(12 * s, 5 * s); p.lineTo(20 * s, 10 * s)
                p.lineTo(20 * s, 20 * s)
                val door = Path()
                door.moveTo(8 * s, 20 * s); door.lineTo(8 * s, 14 * s)
                door.lineTo(16 * s, 14 * s); door.lineTo(16 * s, 20 * s)
                drawPath(p, color, style = style)
                drawPath(door, color, style = style)
            }
            Icon.SEEK -> {
                // 放大镜：circle(11,11,6) + M16 16l4 4
                drawCircle(color, radius = 6 * s, center = Offset(11 * s, 11 * s), style = style)
                val p = Path()
                p.moveTo(16 * s, 16 * s); p.lineTo(20 * s, 20 * s)
                drawPath(p, color, style = style)
            }
            Icon.ANTHOLOGY -> {
                // 书册：M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H7a3 3 0 0 0-3 3V5.5z + M7 18h13
                val p = Path()
                p.arcTo(androidx.compose.ui.geometry.Rect(4f * s, 3f * s, 6.5f * s, 5.5f * s), 180f, 90f, true)
                p.lineTo(20 * s, 3 * s)
                p.lineTo(20 * s, 18 * s)
                p.lineTo(7 * s, 18 * s)
                p.arcTo(androidx.compose.ui.geometry.Rect(4f * s, 18f * s, 7f * s, 21f * s), 270f, -90f, false)
                p.lineTo(4 * s, 5.5f * s)
                val spine = Path()
                spine.moveTo(7 * s, 18 * s); spine.lineTo(20 * s, 18 * s)
                drawPath(p, color, style = style)
                drawPath(spine, color, style = style)
            }
            Icon.ME -> {
                // 人：circle(12,8,3.2) + 肩弧
                drawCircle(color, radius = 3.2f * s, center = Offset(12 * s, 8 * s), style = style)
                val p = Path()
                p.moveTo(5.5f * s, 20 * s)
                p.cubicTo(6.4f * s, 16.5f * s, 8.9f * s, 14.7f * s, 12 * s, 14.7f * s)
                p.cubicTo(15.1f * s, 14.7f * s, 17.6f * s, 16.5f * s, 18.5f * s, 20 * s)
                drawPath(p, color, style = style)
            }
        }
    }
}

// 二级页顶栏（定稿 §6.2 topbar）：48dp，发丝线，夜展页可切夜色
@Composable
fun TopBar(title: String, showBack: Boolean, onBack: () -> Unit, night: Boolean = false) {
    val fg = if (night) PaperHi else Ink
    val lineColor = if (night) NightLine else Line
    Column(
        Modifier
            .background(if (night) Night else Paper)
            .padding(WindowInsets.statusBars.asPaddingValues()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (showBack) {
                Text(
                    "‹",
                    fontFamily = SansFont,
                    fontSize = 20.sp,
                    color = fg,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(onClick = onBack)
                        .padding(start = 20.dp, end = 12.dp),
                )
            }
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SerifFont,
                letterSpacing = 0.18.em,
                textAlign = TextAlign.Center,
                color = fg,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor),
        )
    }
}

@Composable
fun BottomBar(current: Tab, onTab: (Tab) -> Unit, onPlus: () -> Unit) {
    Column(
        Modifier
            .background(Night)
            .padding(WindowInsets.navigationBars.asPaddingValues()),
    ) {
        // 顶线：旧金 50%
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Gold.copy(alpha = 0.5f)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top,
        ) {
            RollerTab(Tab.HALL, Icon.HALL, current == Tab.HALL, onTab)
            RollerTab(Tab.SEEK, Icon.SEEK, current == Tab.SEEK, onTab)
            PlusSlot(onClick = onPlus)
            RollerTab(Tab.ANTHOLOGY, Icon.ANTHOLOGY, current == Tab.ANTHOLOGY, onTab)
            RollerTab(Tab.ME, Icon.ME, current == Tab.ME, onTab)
        }
    }
}

@Composable
private fun RollerTab(tab: Tab, icon: Icon, active: Boolean, onTab: (Tab) -> Unit) {
    val color = if (active) BarActive else BarIdle
    Column(
        Modifier
            .width(52.dp)
            .clickable { onTab(tab) }
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StrokeIcon(icon, color)
        Text(
            tab.label,
            fontFamily = SansFont,
            fontSize = 10.sp,
            letterSpacing = 0.06.em,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
        )
    }
}

// 加号方印（§6.1）：52dp 朱砂方印 + 4dp 夜色描边 + 1dp 旧金外圈，浮出木轴 18dp
@Composable
private fun PlusSlot(onClick: () -> Unit) {
    Box(
        Modifier.width(56.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .offset(y = (-18).dp)
                .size(62.dp)
                .border(1.dp, Gold.copy(alpha = 0.65f), RoundedCornerShape(13.dp))
                .padding(1.dp)
                .background(Night, RoundedCornerShape(12.dp))
                .padding(4.dp)
                .background(Seal, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) {
                val s = size.width
                val w = 1.7f * s / 24f
                drawLine(SealText, Offset(s / 2f, s * 0.2f), Offset(s / 2f, s * 0.8f), strokeWidth = w, cap = StrokeCap.Round)
                drawLine(SealText, Offset(s * 0.2f, s / 2f), Offset(s * 0.8f, s / 2f), strokeWidth = w, cap = StrokeCap.Round)
            }
        }
    }
}
