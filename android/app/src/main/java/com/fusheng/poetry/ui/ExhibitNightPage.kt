package com.fusheng.poetry.ui

// 作品夜展（定稿 §5.1 二级，实现参考 02-作品夜展.html + jz.css .scroll-frame）：
// 夜墨底上一卷笺面，左侧旧金细线，存图为金印，移出为破坏性文字按钮
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fusheng.poetry.data.ExhibitWithPoem
import com.fusheng.poetry.data.PhotoStore
import com.fusheng.poetry.ui.theme.Gold
import com.fusheng.poetry.ui.theme.Ink2
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.KaiFont
import com.fusheng.poetry.ui.theme.Line
import com.fusheng.poetry.ui.theme.Night
import com.fusheng.poetry.ui.theme.Seal
import com.fusheng.poetry.ui.theme.SealDeep
import com.fusheng.poetry.ui.theme.SealText
import com.fusheng.poetry.ui.theme.SansFont
import com.fusheng.poetry.ui.theme.SerifFont
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val ScrollPaper = Color(0xFFF0EDE0) // 卷面笺色（夜色下提亮一档）

@Composable
fun ExhibitNightPage(card: ExhibitWithPoem, onBack: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 夜页状态栏亮字，离开恢复
    DisposableEffect(Unit) {
        val activity = context as ComponentActivity
        activity.enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()))
        onDispose {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            )
        }
    }

    val photoFile = remember(card.exhibit.photoId) { PhotoStore.file(context, card.exhibit.photoId) }
    var ratio by remember(card.exhibit.id) { mutableStateOf<Float?>(null) }
    val poemLines = remember(card.exhibit.focusLine) { splitPoemLines(card.exhibit.focusLine) }
    val local = remember(card) {
        LocalDateTime.ofInstant(Instant.parse(card.exhibit.createdAt), ZoneId.systemDefault())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Night)
            .paperFibers(night = true)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
            ScrollFrame(photoFile, card, poemLines, local, ratio) { w, h ->
                if (w > 0 && h > 0) ratio = w.toFloat() / h
            }
        }
        // 金印存图：48dp 热区裹 29dp 印
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .border(1.dp, Gold, RoundedCornerShape(4.dp))
                    .clickable {
                        val poem = card.poem
                        scope.launch(Dispatchers.IO) {
                            ShareImage.share(
                                context = context,
                                photoFile = photoFile,
                                focusLine = card.exhibit.focusLine,
                                createdAt = card.exhibit.createdAt,
                                meta = poem?.let { "${it.author} · ${it.title}" } ?: "",
                                note = card.exhibit.note,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("存图", fontFamily = KaiFont, fontSize = 12.sp, color = Gold)
            }
        }
        Text(
            "移出浮生馆",
            fontFamily = SansFont,
            fontSize = 12.sp,
            letterSpacing = 0.14.em,
            color = SealDeep,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 32.dp)
                .fillMaxWidth()
                .clickable(onClick = onRemove),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// 卷面：304dp 笺 + 1px 发丝边 + 左侧旧金细线
@Composable
private fun ScrollFrame(
    photoFile: java.io.File,
    card: ExhibitWithPoem,
    poemLines: List<String>,
    local: LocalDateTime,
    ratio: Float?,
    onDims: (Int, Int) -> Unit,
) {
    Box(
        Modifier
            .width(304.dp)
            .background(ScrollPaper)
            .border(1.dp, Line)
            .paperFibers(),
    ) {
        // 左侧旧金细线（上下留 18dp）
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .fillMaxHeight()
                .width(1.dp)
                .padding(vertical = 18.dp)
                .background(Gold.copy(alpha = 0.45f)),
        )
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 24.dp)) {
            // 图：卷面内宽 260dp 按原比例
            val r = ratio
            val imgModifier = if (r != null && r > 0f) {
                Modifier.fillMaxWidth().aspectRatio(if (r > 2.2f) 2.2f else r)
            } else {
                Modifier.fillMaxWidth().height(200.dp)
            }
            Box(imgModifier) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = card.exhibit.focusLine,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setToSaturation(0.9f) }),
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { state ->
                        val d = state.result.drawable
                        onDims(d.intrinsicWidth, d.intrinsicHeight)
                    },
                )
                Box(Modifier.fillMaxSize().background(Color(0xFF171B19).copy(alpha = 0.04f)))
            }
            Row(
                Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                VerticalPoem(
                    poemLines,
                    fontSize = 23.sp,
                    lineHeightFactor = 1.28f,
                    maxColumns = 2, // focus line 最多两列（§3.3）
                    modifier = Modifier.height(236.dp).clipToBounds(),
                    color = Ink2,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        card.exhibit.note,
                        fontFamily = KaiFont,
                        fontSize = 14.sp,
                        lineHeight = 1.85.em,
                        color = Ink2,
                    )
                    Text(
                        card.poem?.let { "${it.author}  ${it.title}" } ?: "",
                        fontFamily = SansFont,
                        fontSize = 11.sp,
                        letterSpacing = 0.12.em,
                        color = Ink3,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DaySeal(dayCn(local.dayOfMonth))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "%04d.%02d.%02d".format(local.year, local.monthValue, local.dayOfMonth),
                            fontFamily = SansFont,
                            fontSize = 10.sp,
                            letterSpacing = 0.16.em,
                            color = Ink3,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySeal(dayCnStr: String) {
    Box(
        Modifier
            .size(22.dp)
            .background(Seal, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(dayCnStr, fontFamily = KaiFont, fontSize = 10.sp, color = SealText)
    }
}
