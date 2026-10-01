package com.fusheng.poetry.ui

// 觅诗（定稿 §5.2，实现参考 03-觅诗.html）：
// 墨线搜索行 + 今日一句（左卡片小字右竖排）+ 按索引三行 + 搜索结果列表条目
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.data.CorpusPoem
import com.fusheng.poetry.data.CorpusRepo
import com.fusheng.poetry.data.FushengDb
import com.fusheng.poetry.data.PoemEntity
import com.fusheng.poetry.ui.theme.Ink
import com.fusheng.poetry.ui.theme.Ink2
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.KaiFont
import com.fusheng.poetry.ui.theme.Line
import com.fusheng.poetry.ui.theme.SansFont
import com.fusheng.poetry.ui.theme.SerifFont
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun SeekPage(onOpenPoem: (poemKey: String) -> Unit, onBrowse: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { FushengDb.get(context).dao() }
    val corpus by produceState<List<CorpusPoem>>(initialValue = emptyList()) {
        value = CorpusRepo.loadAsync(context)
    }
    val pickedPoems by dao.pickedPoems().collectAsState(initial = emptyList())
    val pickedKeys = remember(pickedPoems) { pickedPoems.map { it.poemKey }.toSet() }

    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    val hits = remember(corpus, trimmed) {
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            corpus.filter { p ->
                p.focusLine.contains(trimmed, ignoreCase = true) ||
                    p.title.contains(trimmed, ignoreCase = true) ||
                    p.author.contains(trimmed, ignoreCase = true)
            }
        }
    }

    fun pick(poem: CorpusPoem) {
        scope.launch {
            dao.insertPoem(
                PoemEntity(
                    id = UUID.randomUUID().toString(),
                    poemKey = poem.id,
                    title = poem.title,
                    author = poem.author,
                    dynasty = poem.dynasty,
                    content = poem.lines.joinToString("\n"),
                    focusLine = poem.focusLine,
                    tags = poem.tags.joinToString(","),
                    createdAt = Instant.now().toString(),
                ),
            )
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .paperFibers(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 96.dp),
    ) {
        item(key = "masthead") { Masthead("觅诗", "词库 ${corpus.size} 首") }
        item(key = "search") {
            SearchRow(
                query = query,
                onQuery = { query = it },
                placeholder = "搜一句诗 / 作者 / 诗题",
                tail = if (corpus.isEmpty()) null else corpus.size.toString(),
            )
        }
        when {
            trimmed.isNotEmpty() -> {
                if (hits.isEmpty()) {
                    item(key = "nohit") {
                        Text(
                            "没有找到，换一句试试",
                            fontFamily = KaiFont,
                            fontSize = 15.sp,
                            lineHeight = 1.9.em,
                            color = Ink2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                        )
                    }
                } else {
                    items(hits, key = { "hit-" + it.id }) { poem ->
                        PoemRow(poem, pickedKeys.contains(poem.id), onClick = { onOpenPoem(poem.id) }, onPick = { pick(poem) })
                    }
                }
            }
            corpus.isEmpty() -> item(key = "loading") {
                Text(
                    "词库装帧中…",
                    fontFamily = KaiFont,
                    fontSize = 14.sp,
                    color = Ink3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                )
            }
            else -> {
                val daily = corpus[LocalDate.now().dayOfYear % corpus.size]
                item(key = "daily") { DailyOne(daily, pickedKeys.contains(daily.id)) { pick(daily) } }
                item(key = "index-label") { SectionLabel("按索引") }
                item(key = "index") {
                    Column {
                        IndexRow("朝代", "先秦 汉 魏晋 唐 宋 元 明 清", onBrowse)
                        IndexRow("作者", "李白 杜甫 苏轼 李清照 王维", onBrowse)
                        IndexRow("体裁", "诗 词 曲 文 赋 其他", onBrowse)
                    }
                }
            }
        }
    }
}

// 今日一句（fix.css 03 布局）：左列小字 + 拾印，右竖排 20sp
@Composable
private fun DailyOne(poem: CorpusPoem, picked: Boolean, onPick: () -> Unit) {
    val today = LocalDate.now()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            SectionLabel("今日一句", modifier = Modifier.padding(top = 0.dp))
            Text(
                "${today.year} / %02d / %02d".format(today.monthValue, today.dayOfMonth),
                fontFamily = SansFont,
                fontSize = 10.sp,
                letterSpacing = 0.2.em,
                color = Ink3,
            )
            Text(
                poem.focusLine,
                fontFamily = KaiFont,
                fontSize = 14.sp,
                lineHeight = 1.85.em,
                color = Ink2,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "${poem.author}  ${poem.title}",
                fontFamily = SansFont,
                fontSize = 10.sp,
                letterSpacing = 0.12.em,
                color = Ink3,
                modifier = Modifier.padding(top = 10.dp),
            )
            Box(Modifier.padding(top = 14.dp)) {
                // 已拾：墨底纸字（§6.3）
                if (picked) {
                    Box(
                        Modifier
                            .width(22.dp)
                            .height(22.dp)
                            .background(Ink, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("拾", fontFamily = KaiFont, fontSize = 10.sp, color = com.fusheng.poetry.ui.theme.PaperHi)
                    }
                } else {
                    SealSquare("拾", 22, 10, onClick = onPick)
                }
            }
        }
        Spacer(Modifier.width(22.dp))
        VerticalPoem(
            splitPoemLines(poem.focusLine),
            fontSize = 20.sp,
            maxColumns = 2,
            modifier = Modifier.width(60.dp).height(196.dp).clipToBounds(),
        )
    }
}

// 按索引行（fix.css .index-row）：竖排类目 + 示例值 + ›
@Composable
private fun IndexRow(label: String, values: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerticalText(label, 10.sp, Ink3, 0.22f, Modifier.height(46.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                values,
                fontFamily = SerifFont,
                fontSize = 15.sp,
                letterSpacing = 0.06.em,
                color = Ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text("›", fontFamily = SansFont, fontSize = 15.sp, color = Ink3)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

// 列表条目（定稿 §6.3）：诗句 18sp 单行省略 + 出处，右侧圆钮拾
@Composable
private fun PoemRow(poem: CorpusPoem, picked: Boolean, onClick: () -> Unit, onPick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                poem.focusLine,
                fontFamily = SerifFont,
                fontSize = 18.sp,
                lineHeight = 1.65.em,
                letterSpacing = 0.06.em,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${poem.author}  ${poem.title}",
                fontFamily = SansFont,
                fontSize = 11.sp,
                letterSpacing = 0.1.em,
                color = Ink3,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Box(Modifier.padding(start = 14.dp)) {
            CircleKnob("拾", picked, onPick)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}
