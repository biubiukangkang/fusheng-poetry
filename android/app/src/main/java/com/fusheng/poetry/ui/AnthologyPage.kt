package com.fusheng.poetry.ui

// 诗集（定稿 §5.4，实现参考 05-诗集.html）：
// 书签标签筛选 + 诗叶列表（竖排叶码）+ 整理模式（批量打标签 / 移出）
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.data.FushengDb
import com.fusheng.poetry.data.PoemEntity
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
import com.fusheng.poetry.ui.theme.SealDeep
import com.fusheng.poetry.ui.theme.SealText
import com.fusheng.poetry.ui.theme.SerifFont
import java.time.Instant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnthologyPage(onGoSeek: () -> Unit, onOpenDetail: (poemId: String) -> Unit, onOpenCompose: (poemId: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { FushengDb.get(context).dao() }
    val poems by dao.pickedPoems().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var activeTag by remember { mutableStateOf<String?>(null) } // null=全部；"__none__"=未归类
    var organizing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    val allTags = remember(poems) {
        poems.flatMap { it.tags.split(",") }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val shown = remember(poems, activeTag, query) {
        poems.filter { p ->
            val tagOk = when (activeTag) {
                null -> true
                "__none__" -> p.tags.split(",").filter { it.isNotBlank() }.isEmpty()
                else -> p.tags.split(",").contains(activeTag)
            }
            val q = query.trim()
            val qOk = q.isEmpty() ||
                p.focusLine.contains(q, ignoreCase = true) ||
                p.title.contains(q, ignoreCase = true) ||
                p.author.contains(q, ignoreCase = true)
            tagOk && qOk
        }
    }

    fun exitOrganize() {
        organizing = false
        selected = emptySet()
    }
    fun toggle(id: String) {
        selected = if (selected.contains(id)) selected - id else selected + id
    }
    BackHandler(enabled = organizing) { exitOrganize() }

    Box(Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .paperFibers(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = if (organizing) 130.dp else 96.dp),
        ) {
            item(key = "masthead") {
                Row(verticalAlignment = Alignment.Bottom) {
                    Masthead("诗集", "已拾 ${poems.size} 首", Modifier.weight(1f))
                    Text(
                        if (organizing) "完成" else "整理",
                        fontFamily = SansFont,
                        fontSize = 12.sp,
                        letterSpacing = 0.14.em,
                        color = Ink2,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clickable { if (organizing) exitOrganize() else organizing = true }
                            .padding(6.dp),
                    )
                }
            }
            item(key = "search") {
                SearchRow(query, { query = it }, "在我的诗集里找", tail = if (poems.isEmpty()) null else poems.size.toString())
            }
            if (poems.isEmpty()) {
                item(key = "empty") { EmptyAnthology(onGoSeek) }
            } else {
                item(key = "bookmarks") {
                    Row(
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        BookmarkSign("全部", activeTag == null) { activeTag = null }
                        allTags.forEach { t -> BookmarkSign(t, activeTag == t) { activeTag = if (activeTag == t) null else t } }
                        BookmarkSign("未归类", activeTag == "__none__") { activeTag = if (activeTag == "__none__") null else "__none__" }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                items(shown, key = { it.id }) { poem ->
                    PoemLeaf(
                        poem = poem,
                        leafNo = cnNum(shown.indexOf(poem) + 1),
                        organizing = organizing,
                        checked = selected.contains(poem.id),
                        onToggleCheck = { toggle(poem.id) },
                        onClick = {
                            if (organizing) toggle(poem.id) else onOpenDetail(poem.id)
                        },
                    )
                }
            }
        }

        // 整理模式底部操作条（§5.4：打标签 / 移出诗集）
        if (organizing) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Paper)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "已选 ${selected.size} 首",
                    fontFamily = SansFont,
                    fontSize = 12.sp,
                    color = Ink3,
                    modifier = Modifier.weight(1f),
                )
                SubButton("打标签", onClick = { showTagSheet = true }, enabled = selected.isNotEmpty())
                Spacer(Modifier.width(10.dp))
                Text(
                    "移出诗集",
                    fontFamily = SansFont,
                    fontSize = 13.sp,
                    letterSpacing = 0.1.em,
                    color = if (selected.isEmpty()) Ink3.copy(alpha = 0.5f) else SealDeep,
                    modifier = Modifier
                        .clickable(enabled = selected.isNotEmpty()) { showRemoveConfirm = true }
                        .padding(8.dp),
                )
            }
        }
    }

    // 批量打标签抽屉：已有标签复选 + 新建
    if (showTagSheet) {
        var tagSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
        var newTag by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showTagSheet = false }, containerColor = PaperHi) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("给 ${selected.size} 首诗打标签", fontFamily = SerifFont, fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(14.dp))
                if (allTags.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        allTags.forEach { t ->
                            val on = tagSelection.contains(t)
                            Box(
                                Modifier
                                    .padding(end = 8.dp)
                                    .background(if (on) Seal else Paper, RoundedCornerShape(3.dp))
                                    .clickable { tagSelection = if (on) tagSelection - t else tagSelection + t }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(t, fontFamily = SansFont, fontSize = 11.sp, color = if (on) SealText else Ink2)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Box(
                    Modifier.fillMaxWidth().height(40.dp).background(Paper),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // placeholder 叠放在外层：与输入文字同容器时 Compose 1.7.6 不渲染输入内容
                    BasicTextField(
                        value = newTag,
                        onValueChange = { if (it.length <= 8) newTag = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = SansFont, fontSize = 14.sp, color = Ink),
                        cursorBrush = SolidColor(Seal),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    )
                    if (newTag.isEmpty()) {
                        Text("新标签（最多 8 字）", fontFamily = SansFont, fontSize = 13.sp, color = Ink3, modifier = Modifier.padding(horizontal = 10.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                InkButton("打上标签", enabled = tagSelection.isNotEmpty() || newTag.isNotBlank()) {
                    scope.launch {
                        val add = tagSelection + listOf(newTag.trim()).filter { it.isNotEmpty() }
                        selected.forEach { id ->
                            val poem = poems.find { it.id == id } ?: return@forEach
                            val merged = (poem.tags.split(",") + add)
                                .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                                .joinToString(",")
                            dao.updatePoemTags(id, merged)
                        }
                        showTagSheet = false
                        exitOrganize()
                    }
                }
            }
        }
    }

    // 批量移出确认（破坏性 SealDeep）
    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = PaperHi,
            shape = RoundedCornerShape(8.dp),
            title = { Text("移出 ${selected.size} 首诗？", fontFamily = SerifFont, fontSize = 17.sp, color = Ink) },
            text = {
                Text(
                    "移出后不再出现在诗集，已入馆的作品不受影响。",
                    fontFamily = SansFont,
                    fontSize = 14.sp,
                    lineHeight = 1.7.em,
                    color = Ink2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val at = Instant.now().toString()
                        selected.forEach { dao.removePoem(it, at) }
                        showRemoveConfirm = false
                        exitOrganize()
                    }
                }) { Text("移出", fontFamily = SansFont, fontSize = 14.sp, color = SealDeep) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("留着", fontFamily = SansFont, fontSize = 14.sp, color = Ink2)
                }
            },
        )
    }
}

// 诗叶（fix.css .leaf）：竖排叶码 + 诗句 + 出处 + 标签
@Composable
private fun PoemLeaf(
    poem: PoemEntity,
    leafNo: String,
    organizing: Boolean,
    checked: Boolean,
    onToggleCheck: () -> Unit,
    onClick: () -> Unit,
) {
    val tags = poem.tags.split(",").filter { it.isNotBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (organizing) {
            Box(Modifier.padding(top = 4.dp, end = 8.dp)) {
                // 勾选是开关：选中后仍可点，才能取消
                CircleKnob(if (checked) "选" else "", checked, onToggleCheck, enabled = true)
            }
        }
        VerticalText(leafNo, 12.sp, Ink3, 0.12f, Modifier.height(52.dp).width(18.dp))
        Spacer(Modifier.width(14.dp))
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
            if (tags.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.take(3).forEach { MiniTag(it) }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

// 书签（fix.css .bookmark）：顶部 4dp 圆角，底边贴线，选中朱砂
@Composable
private fun BookmarkSign(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 6.dp)
            .height(28.dp)
            .background(if (selected) Seal else PaperHi, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = SansFont,
            fontSize = 10.sp,
            letterSpacing = 0.08.em,
            color = if (selected) SealText else Ink2,
        )
    }
}

@Composable
private fun EmptyAnthology(onGoSeek: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "还没有拾诗\n去觅诗里找一句",
            fontFamily = KaiFont,
            fontSize = 15.sp,
            lineHeight = 1.9.em,
            color = Ink2,
            textAlign = TextAlign.Center,
        )
        SubButton("去觅诗", onClick = onGoSeek, modifier = Modifier.padding(top = 18.dp))
    }
}
