package com.fusheng.poetry.ui

// 词库浏览（定稿 §5.2 按索引）：朝代 / 作者 / 体裁三书签 → 值列表 → 诗列表
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.data.CorpusPoem
import com.fusheng.poetry.data.CorpusRepo
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

private val INDEX_TYPES = listOf("朝代", "作者", "体裁")

// 顺序类目（常用在前）
private val DYNASTY_ORDER = listOf(
    "先秦", "两汉", "魏晋", "南北朝", "隋代", "唐代", "五代", "宋代", "金朝", "元代", "明代", "清代", "近现代",
)
private val GENRE_ORDER = listOf("诗", "词", "曲", "文", "赋", "其他")

@Composable
fun CorpusBrowserPage(initialType: String = "朝代", onOpenPoem: (poemKey: String) -> Unit) {
    val context = LocalContext.current
    val corpus by produceState<List<CorpusPoem>?>(initialValue = null) {
        value = CorpusRepo.loadAsync(context)
    }
    var type by remember { mutableStateOf(if (initialType in INDEX_TYPES) initialType else "朝代") }
    var value by remember { mutableStateOf<String?>(null) }

    // 诗列表 → 返回值列表
    BackHandler(enabled = value != null) { value = null }

    Column(Modifier.fillMaxSize().background(Paper)) {
        // 三书签（§6.8：顶部 4dp 圆角，底边贴线；选中朱砂底）
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
            INDEX_TYPES.forEach { t ->
                val selected = t == type
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .height(30.dp)
                        .background(if (selected) Seal else PaperHi, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .clickable {
                            type = t
                            value = null
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        t,
                        fontFamily = SansFont,
                        fontSize = 10.sp,
                        letterSpacing = 0.08.em,
                        color = if (selected) SealText else Ink2,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(LineStrong))
        when (val all = corpus) {
            null -> CenterHint("词库装帧中…")
            else -> {
                val grouped = remember(all, type) { groupBy(all, type) }
                val v = value
                if (v == null) {
                    ValueList(grouped, type) { value = it }
                } else {
                    PoemList(v, grouped[v].orEmpty(), onOpenPoem)
                }
            }
        }
    }
}

@Composable
private fun ValueList(grouped: Map<String, List<CorpusPoem>>, type: String, onOpen: (String) -> Unit) {
    val keys = remember(grouped, type) { sortKeys(grouped, type) }
    LazyColumn(
        Modifier.fillMaxSize().paperFibers(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 40.dp),
    ) {
        items(keys, key = { "$type-$it" }) { k ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(k) }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(k, fontFamily = SerifFont, fontSize = 17.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(
                    "${cnNum(grouped[k]!!.size)} 首",
                    fontFamily = SansFont,
                    fontSize = 11.sp,
                    letterSpacing = 0.1.em,
                    color = Ink3,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        }
    }
}

@Composable
private fun PoemList(title: String, list: List<CorpusPoem>, onOpenPoem: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().paperFibers(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 40.dp),
    ) {
        item(key = "head") {
            Text(
                title,
                fontFamily = SerifFont,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = Ink,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
            )
        }
        items(list, key = { it.id }) { poem ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPoem(poem.id) }
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
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        }
    }
}

@Composable
private fun CenterHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, fontFamily = KaiFont, fontSize = 14.sp, color = Ink3, textAlign = TextAlign.Center)
    }
}

private fun groupBy(all: List<CorpusPoem>, type: String): Map<String, List<CorpusPoem>> = when (type) {
    "朝代" -> all.groupBy { it.dynasty.ifEmpty { "其他" } }
    "作者" -> all.groupBy { it.author.ifEmpty { "佚名" } }
    else -> all.groupBy { it.tags.firstOrNull()?.ifEmpty { null } ?: "其他" }
}

private fun sortKeys(grouped: Map<String, List<CorpusPoem>>, type: String): List<String> {
    val keys = grouped.keys.toList()
    return when (type) {
        "朝代" -> keys.sortedWith(compareBy({ k: String -> DYNASTY_ORDER.indexOf(k).let { if (it < 0) 99 else it } }, { k: String -> k }))
        "体裁" -> keys.sortedWith(compareBy({ k: String -> GENRE_ORDER.indexOf(k).let { if (it < 0) 99 else it } }, { k: String -> k }))
        else -> keys.sortedWith(compareByDescending<String> { grouped[it]!!.size }.thenBy { it }) // 作者按数量
    }
}
