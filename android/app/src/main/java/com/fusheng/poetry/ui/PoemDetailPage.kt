package com.fusheng.poetry.ui

// 诗详情（定稿 §5.2）：纸面无卡片，正文宋体 20sp 行高 2.05，
// 译/注/赏 圆钮跳块，解析楷体 14sp，底部墨条动作
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.data.CorpusAnnotation
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
import com.fusheng.poetry.ui.theme.SealDeep
import com.fusheng.poetry.ui.theme.SerifFont
import kotlinx.coroutines.launch

/** 详情页通用数据：poemKey 非空时从词库带出解析与标签 */
data class PoemDetailUi(
    val poemKey: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String,
    val tags: List<String> = emptyList(),
)

// 解析块 -> 圆钮单字
private fun knobChar(name: String): String = when {
    name.startsWith("译文") || name.startsWith("注释") -> "译"
    name.startsWith("创作背景") -> "背"
    name.startsWith("赏析") -> "赏"
    name.startsWith("简析") -> "简"
    else -> name.take(1)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PoemDetailPage(
    detail: PoemDetailUi,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    onTagsChange: ((List<String>) -> Unit)? = null, // 诗集详情：标签可加/删
    onRemove: (() -> Unit)? = null, // 诗集详情：移出诗集
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val annotations by produceState(initialValue = emptyList<CorpusAnnotation>(), detail.poemKey) {
        value = if (detail.poemKey.isEmpty()) {
            emptyList()
        } else {
            CorpusRepo.loadAsync(context).find { it.id == detail.poemKey }?.annotations.orEmpty()
        }
    }
    var activeKnob by remember { mutableIntStateOf(0) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Paper)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().paperFibers(),
        ) {
            item(key = "poem") {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        detail.title,
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        lineHeight = 1.5.em,
                        color = Ink,
                    )
                    Text(
                        listOfNotNull(
                            detail.author.ifEmpty { "佚名" },
                            detail.dynasty.takeIf { it.isNotEmpty() },
                        ).joinToString("　"),
                        fontFamily = SansFont,
                        fontSize = 11.sp,
                        letterSpacing = 0.12.em,
                        color = Ink3,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        detail.content,
                        fontFamily = SerifFont,
                        fontSize = 20.sp,
                        lineHeight = 2.05.em,
                        color = Ink,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    if (detail.tags.isNotEmpty()) {
                        Row(
                            Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            detail.tags.forEach { tag -> MiniTag(tag) }
                        }
                    }
                    // 标签加/删（诗集详情）
                    if (onTagsChange != null) {
                        Row(
                            Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            // 点击已有标签删除
                            detail.tags.forEach { tag ->
                                Box(
                                    Modifier
                                        .background(PaperHi, RoundedCornerShape(3.dp))
                                        .clickable { onTagsChange(detail.tags - tag) }
                                        .padding(horizontal = 7.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "$tag ×",
                                        fontFamily = SansFont,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.06.em,
                                        color = Ink2,
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .border(1.dp, LineStrong, RoundedCornerShape(3.dp))
                                    .clickable { showTagSheet = true }
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                            ) {
                                Text("＋ 加标签", fontFamily = SansFont, fontSize = 10.sp, letterSpacing = 0.06.em, color = Ink2)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
            annotations.forEachIndexed { bi, block ->
                item(key = "ann-$bi") {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(com.fusheng.poetry.ui.theme.Line))
                        Text(
                            block.name,
                            fontFamily = SansFont,
                            fontSize = 10.sp,
                            letterSpacing = 0.22.em,
                            color = Ink3,
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                        )
                        block.subs.forEach { sub ->
                            sub.label?.let {
                                Text(
                                    it,
                                    fontFamily = SerifFont,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = Ink2,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                            sub.paragraphs.forEach { p ->
                                Text(
                                    p,
                                    fontFamily = KaiFont,
                                    fontSize = 14.sp,
                                    lineHeight = 1.9.em,
                                    color = Ink2,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
            item(key = "tail") { Spacer(Modifier.height(110.dp)) }
        }

        // 顶部圆钮（译/注/赏/背）：40dp，激活 Seal
        if (annotations.isNotEmpty()) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 22.dp, top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                annotations.take(4).forEachIndexed { i, block ->
                    val active = i == activeKnob
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(if (active) Seal.copy(alpha = 0.12f) else Paper.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, if (active) Seal else LineStrong, CircleShape)
                            .clickable {
                                activeKnob = i
                                scope.launch { listState.animateScrollToItem(i + 1) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            knobChar(block.name),
                            fontFamily = SerifFont,
                            fontSize = 14.sp,
                            color = if (active) Seal else Ink,
                        )
                    }
                }
            }
        }

        // 底部墨条动作 + 移出诗集（诗集详情）：纸底衬避免压在解析文字上
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Paper.copy(alpha = 0.94f))
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 16.dp),
        ) {
            InkButton(actionLabel, enabled = actionEnabled, onClick = onAction)
            if (onRemove != null) {
                Text(
                    "移出诗集",
                    fontFamily = SansFont,
                    fontSize = 12.sp,
                    letterSpacing = 0.14.em,
                    color = SealDeep,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRemoveConfirm = true }
                        .padding(top = 6.dp, bottom = 2.dp),
                )
            }
        }
    }

    // 加标签抽屉
    if (showTagSheet && onTagsChange != null) {
        var newTag by remember { mutableStateOf("") }
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showTagSheet = false },
            containerColor = PaperHi,
        ) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("加标签", fontFamily = SerifFont, fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().height(40.dp).background(Paper),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // placeholder 叠放在外层：与输入文字同容器时 Compose 1.7.6 不渲染输入内容
                    androidx.compose.foundation.text.BasicTextField(
                        value = newTag,
                        onValueChange = { if (it.length <= 8) newTag = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = SansFont,
                            fontSize = 14.sp,
                            color = Ink,
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Seal),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    )
                    if (newTag.isEmpty()) {
                        Text("新标签（最多 8 字）", fontFamily = SansFont, fontSize = 13.sp, color = Ink3, modifier = Modifier.padding(horizontal = 10.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                InkButton("加上", enabled = newTag.isNotBlank()) {
                    onTagsChange((detail.tags + newTag.trim()).distinct())
                    showTagSheet = false
                }
            }
        }
    }

    // 移出确认
    if (showRemoveConfirm && onRemove != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            containerColor = PaperHi,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            title = { Text("移出这首诗？", fontFamily = SerifFont, fontSize = 17.sp, color = Ink) },
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
                androidx.compose.material3.TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemove()
                }) { Text("移出", fontFamily = SansFont, fontSize = 14.sp, color = SealDeep) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("留着", fontFamily = SansFont, fontSize = 14.sp, color = Ink2)
                }
            },
        )
    }
}
