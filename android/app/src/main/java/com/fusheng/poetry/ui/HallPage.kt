package com.fusheng.poetry.ui

// 浮生馆（定稿 §5.1，实现参考 fix.css .work-land3/.work-port3）：
// 纸面无卡片，图片按原图方向三版式（横/竖/方），月份分隔竖排，日期朱砂印
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fusheng.poetry.data.ExhibitWithPoem
import com.fusheng.poetry.data.FushengDb
import com.fusheng.poetry.data.PhotoStore
import com.fusheng.poetry.ui.theme.Ink
import com.fusheng.poetry.ui.theme.Ink2
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.KaiFont
import com.fusheng.poetry.ui.theme.Line
import com.fusheng.poetry.ui.theme.LineStrong
import com.fusheng.poetry.ui.theme.PaperDeep
import com.fusheng.poetry.ui.theme.PaperHi
import com.fusheng.poetry.ui.theme.SansFont
import com.fusheng.poetry.ui.theme.Seal
import com.fusheng.poetry.ui.theme.SealText
import com.fusheng.poetry.ui.theme.SerifFont
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun HallPage(onOpenExhibit: (String) -> Unit, onCompose: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { FushengDb.get(context).dao() }
    val cards by dao.exhibitCards().collectAsState(initial = emptyList())

    LazyColumn(
        Modifier
            .fillMaxSize()
            .paperFibers(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 96.dp),
    ) {
        item(key = "masthead") { Masthead("浮生馆", "一卷 · " + cnNum(cards.size) + "件") }
        if (cards.isEmpty()) {
            item(key = "empty") { EmptyHall(onCompose) }
        }
        // 按月分组（createdAt ISO → 本地时区归月），组内保 DESC
        val groups = cards.groupBy { card ->
            val local = LocalDateTime.ofInstant(Instant.parse(card.exhibit.createdAt), ZoneId.systemDefault())
            "${local.year} 年 ${local.monthValue} 月"
        }
        groups.forEach { (month, items) ->
            item(key = "month-$month") {
                val local = LocalDateTime.ofInstant(Instant.parse(items.first().exhibit.createdAt), ZoneId.systemDefault())
                MonthDivider("${yearCn(local.year)}年${cnNum(local.monthValue)}月")
            }
            itemsIndexed(items, key = { _, it -> it.exhibit.id }) { index, card ->
                val topGap = if (index == 0) 16.dp else 24.dp
                Box(Modifier.padding(top = topGap)) {
                    WorkLeaf(card) { onOpenExhibit(card.exhibit.id) }
                }
            }
        }
    }
}

// 月份分隔：竖排月份（2 字/列，列从右到左，对齐视觉稿「月年二二/九六〇」网格）+ 发丝线
@Composable
private fun MonthDivider(monthCn: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalRTL {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                monthCn.chunked(2).forEach { col ->
                    VerticalChars(col, 10.sp, Ink3, 0.22f, SansFont)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Line))
    }
}

// 竖排小字（sans）：索引类目、叶码
@Composable
fun VerticalText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, color: Color, spacingEm: Float, modifier: Modifier = Modifier) {
    VerticalChars(text, fontSize, color, spacingEm, SansFont, modifier)
}

@Composable
internal fun CompositionLocalRTL(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
        content = content,
    )
}

// 作品叶：三版式（定稿 §4）
@Composable
private fun WorkLeaf(card: ExhibitWithPoem, onClick: () -> Unit) {
    val context = LocalContext.current
    var ratio by remember(card.exhibit.id) { mutableStateOf<Float?>(null) }
    val photoFile = remember(card.exhibit.photoId) { PhotoStore.file(context, card.exhibit.photoId) }
    val poemLines = remember(card.exhibit.focusLine) { splitPoemLines(card.exhibit.focusLine) }
    val meta = remember(card) {
        val local = LocalDateTime.ofInstant(Instant.parse(card.exhibit.createdAt), ZoneId.systemDefault())
        Triple(
            dayCn(local.dayOfMonth),
            "%04d.%02d.%02d".format(local.year, local.monthValue, local.dayOfMonth),
            card.poem?.let { "${it.author}  ${it.title}" } ?: "",
        )
    }

    val r = ratio
    Column(Modifier.clickable(onClick = onClick)) {
        when {
            r == null -> PlaceholderPlate(photoFile, onDims = { w, h ->
                if (w > 0 && h > 0) ratio = w.toFloat() / h
            })
            r > 1.2f -> LandscapeLeaf(card, photoFile, r, poemLines, meta)
            else -> PortraitLeaf(card, photoFile, r, poemLines, meta)
        }
    }
}

// 图片图版（定稿 §4.6）：直角无装饰，轻旧色（saturate .9 + 5% 纸色叠加）
@Composable
private fun Plate(photoFile: java.io.File, description: String, modifier: Modifier) {
    Box(modifier) {
        AsyncImage(
            model = photoFile,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setToSaturation(0.9f) }),
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(PaperDeep.copy(alpha = 0.08f)))
    }
}

@Composable
private fun PlaceholderPlate(photoFile: java.io.File, onDims: (Int, Int) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(PaperHi),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photoFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { state ->
                val d = state.result.drawable
                onDims(d.intrinsicWidth, d.intrinsicHeight)
            },
        )
    }
}

// 横图版式：图满版心按原比例；下方左竖排诗、右注记/出处/日期印
@Composable
private fun LandscapeLeaf(
    card: ExhibitWithPoem,
    photoFile: java.io.File,
    ratio: Float,
    poemLines: List<String>,
    meta: Triple<String, String, String>,
) {
    val displayRatio = if (ratio > 2.2f) 2.2f else ratio // 极宽全景封顶，轻微居中裁剪
    Column {
        Plate(photoFile, card.exhibit.focusLine, Modifier.fillMaxWidth().aspectRatio(displayRatio))
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 竖排容器随长句增高（下限 128dp = 设计稿短句示例值）
            val poemH = 128.dp.coerceAtLeast((poemLines.maxOf { it.length } * 16f * 1.18f).dp)
            VerticalPoem(
                poemLines,
                fontSize = 16.sp,
                lineHeightFactor = 1.3f,
                // focus line 最多两列，全文进夜展/详情（定稿 §3.3）
                maxColumns = 2,
                modifier = Modifier.width(54.dp).height(poemH),
            )
            Column(Modifier.weight(1f)) {
                NoteText(card.exhibit.note)
                MetaText(meta.third)
                SealLine(meta.first, meta.second, Modifier.padding(top = 10.dp))
            }
        }
    }
}

// 竖图 / 方图版式：左图（版心的 60% / 76%，对应设计稿 360 稿的 190/240），右侧竖排诗 + 注记 + 日期印
@Composable
private fun PortraitLeaf(
    card: ExhibitWithPoem,
    photoFile: java.io.File,
    ratio: Float,
    poemLines: List<String>,
    meta: Triple<String, String, String>,
) {
    val square = ratio >= 0.9f
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val imgWidth = maxWidth * (if (square) 240f / 316f else 190f / 316f)
        val imgHeight = imgWidth / ratio
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Plate(
                photoFile,
                card.exhibit.focusLine,
                if (imgHeight > 280.dp) {
                    // 高封顶 280dp，宽度按比例反推（§4.4）
                    Modifier.width(280.dp * ratio).height(280.dp)
                } else {
                    Modifier.width(imgWidth).height(imgHeight)
                },
            )
        Column(Modifier.weight(1f)) {
            val poemH = 160.dp.coerceAtLeast((poemLines.maxOf { it.length } * 16f * 1.18f).dp)
            VerticalPoem(
                poemLines,
                fontSize = 16.sp,
                lineHeightFactor = 1.3f,
                maxColumns = 2,
                modifier = Modifier.height(poemH),
            )
                NoteText(card.exhibit.note, Modifier.padding(top = 10.dp))
                MetaText(meta.third, Modifier.padding(top = 2.dp))
                SealLine(meta.first, meta.second, Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun NoteText(note: String, modifier: Modifier = Modifier) {
    if (note.isEmpty()) return
    Text(
        note,
        fontFamily = KaiFont,
        fontSize = 13.sp,
        lineHeight = 1.75.em,
        color = Ink2,
        maxLines = 3,
        modifier = modifier,
    )
}

@Composable
private fun MetaText(meta: String, modifier: Modifier = Modifier) {
    if (meta.isEmpty()) return
    Text(
        meta,
        fontFamily = SansFont,
        fontSize = 10.sp,
        letterSpacing = 0.13.em,
        color = Ink3,
        modifier = modifier,
    )
}

// 印章行：日期方印 + 发丝线 + 数字日期（fix.css .seal-line3）
@Composable
private fun SealLine(dayCnStr: String, dateNum: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(22.dp)
                .background(Seal, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(dayCnStr, fontFamily = KaiFont, fontSize = 10.sp, color = SealText)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Line))
        Spacer(Modifier.width(8.dp))
        Text(
            dateNum,
            fontFamily = SansFont,
            fontSize = 9.sp,
            letterSpacing = 0.12.em,
            color = Ink3,
        )
    }
}

// 空状态（定稿 §6.10）：朱砂描边圆 + 楷体文案 + 次按钮
@Composable
private fun EmptyHall(onCompose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .border(1.dp, Seal, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("馆", fontFamily = KaiFont, fontSize = 20.sp, color = Seal)
        }
        Text(
            "还没有入馆的浮生",
            fontFamily = KaiFont,
            fontSize = 15.sp,
            lineHeight = 1.9.em,
            color = Ink2,
            modifier = Modifier.padding(top = 16.dp),
        )
        Box(
            Modifier
                .padding(top = 18.dp)
                .border(1.dp, LineStrong, RoundedCornerShape(2.dp))
                .clickable(onClick = onCompose)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text("＋ 记录这一刻", fontFamily = SansFont, fontSize = 12.sp, letterSpacing = 0.14.em, color = Ink2)
        }
    }
}
