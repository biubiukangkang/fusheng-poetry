package com.fusheng.poetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.data.CorpusRepo
import com.fusheng.poetry.data.CorpusPoem
import com.fusheng.poetry.data.FushengDb
import com.fusheng.poetry.data.PoemEntity
import com.fusheng.poetry.ui.AnthologyPage
import com.fusheng.poetry.ui.BottomBar
import com.fusheng.poetry.ui.ComposePage
import com.fusheng.poetry.ui.CorpusBrowserPage
import com.fusheng.poetry.ui.ExhibitNightPage
import com.fusheng.poetry.ui.HallPage
import com.fusheng.poetry.ui.MePage
import com.fusheng.poetry.ui.PoemDetailPage
import com.fusheng.poetry.ui.PoemDetailUi
import com.fusheng.poetry.ui.SeekPage
import com.fusheng.poetry.ui.Tab
import com.fusheng.poetry.ui.TopBar
import com.fusheng.poetry.ui.theme.FuShengTheme
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.Paper
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuShengTheme {
                Root()
            }
        }
    }
}

// 导航层级：tab 页 → 词库浏览 → 词库详情 / 诗集详情；顶层创作页
// 底栏任意键切走时清掉全部覆盖层
@Composable
private fun Root() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { FushengDb.get(context).dao() }
    val pickedPoems by dao.pickedPoems().collectAsState(initial = emptyList())
    val exhibitCards by dao.exhibitCards().collectAsState(initial = emptyList())
    val corpus by produceState<List<CorpusPoem>>(initialValue = emptyList()) {
        value = CorpusRepo.loadAsync(context)
    }

    var tab by rememberSaveable { mutableStateOf(Tab.HALL) }
    var composingPreset by rememberSaveable { mutableStateOf<String?>(null) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var exhibitDetailId by rememberSaveable { mutableStateOf<String?>(null) } // 作品夜展
    var detailPoemId by rememberSaveable { mutableStateOf<String?>(null) } // 诗集详情
    var browsing by rememberSaveable { mutableStateOf<String?>(null) } // 词库浏览（携带索引类型）
    var corpusDetailKey by rememberSaveable { mutableStateOf<String?>(null) } // 词库详情

    fun closeOverlays() {
        composing = false
        composingPreset = null
        exhibitDetailId = null
        detailPoemId = null
        browsing = null
        corpusDetailKey = null
    }
    BackHandler(enabled = composing || exhibitDetailId != null || detailPoemId != null || browsing != null || corpusDetailKey != null) {
        when {
            composing -> composing = false
            exhibitDetailId != null -> exhibitDetailId = null
            corpusDetailKey != null -> corpusDetailKey = null
            browsing != null -> browsing = null
            else -> detailPoemId = null
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            val overlay = composing || exhibitDetailId != null || detailPoemId != null || browsing != null || corpusDetailKey != null
            if (overlay) {
                TopBar(
                    title = when {
                        composing -> "创作"
                        exhibitDetailId != null -> "展卷"
                        corpusDetailKey != null -> "诗文"
                        detailPoemId != null -> "诗文"
                        else -> "词库"
                    },
                    night = exhibitDetailId != null,
                    showBack = true,
                    onBack = {
                        when {
                            composing -> composing = false
                            exhibitDetailId != null -> exhibitDetailId = null
                            corpusDetailKey != null -> corpusDetailKey = null
                            browsing != null -> browsing = null
                            else -> detailPoemId = null
                        }
                    },
                )
            } else {
                // tab 页无顶栏（设计定稿 §5）：只衬状态栏纸色
                Box(
                    Modifier
                        .background(Paper)
                        .padding(WindowInsets.statusBars.asPaddingValues()),
                )
            }
        },
        bottomBar = {
            BottomBar(
                tab,
                onTab = {
                    closeOverlays()
                    tab = it
                },
                onPlus = {
                    detailPoemId = null
                    browsing = null
                    corpusDetailKey = null
                    composingPreset = null
                    composing = true
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                composing -> ComposePage(
                    presetPoemId = composingPreset,
                    onDone = {
                        composing = false
                        composingPreset = null
                        detailPoemId = null
                    },
                )
                // 诗集详情（可从词库详情拾入后跳转而来，此处仅诗集诗）
                detailPoemId != null -> {
                    val poem = pickedPoems.find { it.id == detailPoemId }
                    if (poem != null) {
                        PoemDetailPage(
                            detail = PoemDetailUi(
                                poem.poemKey,
                                poem.title,
                                poem.author,
                                poem.dynasty,
                                poem.content,
                                poem.tags.split(",").filter { it.isNotBlank() },
                            ),
                            actionLabel = "用 此 诗 入 馆",
                            actionEnabled = true,
                            onAction = {
                                composingPreset = poem.id
                                composing = true
                            },
                            onTagsChange = { tags ->
                                scope.launch { dao.updatePoemTags(poem.id, tags.joinToString(",")) }
                            },
                            onRemove = {
                                val pid = poem.id
                                detailPoemId = null
                                scope.launch { dao.removePoem(pid, Instant.now().toString()) }
                            },
                        )
                    }
                }
                // 作品夜展（浮生馆二级）
                exhibitDetailId != null -> {
                    val card = exhibitCards.find { it.exhibit.id == exhibitDetailId }
                    if (card != null) {
                        ExhibitNightPage(
                            card = card,
                            onBack = { exhibitDetailId = null },
                            onRemove = {
                                exhibitDetailId = null
                                scope.launch { dao.removeExhibit(card.exhibit.id, Instant.now().toString()) }
                            },
                        )
                    }
                }
                // 词库详情（拾入诗集；已拾置灰）
                corpusDetailKey != null -> {
                    val cp = corpus.find { it.id == corpusDetailKey }
                    if (cp != null) {
                        val already = pickedPoems.any { it.poemKey == cp.id }
                        PoemDetailPage(
                            detail = PoemDetailUi(
                                cp.id,
                                cp.title,
                                cp.author,
                                cp.dynasty,
                                cp.lines.joinToString("\n"),
                                cp.tags,
                            ),
                            actionLabel = if (already) "已 拾 入 诗 集" else "拾 入 诗 集",
                            actionEnabled = !already,
                            onAction = {
                                scope.launch { dao.insertPoem(cp.toEntity()) }
                            },
                        )
                    }
                }
                browsing != null -> CorpusBrowserPage(initialType = browsing!!, onOpenPoem = { corpusDetailKey = it })
                else -> when (tab) {
                    Tab.HALL -> HallPage(
                        onOpenExhibit = { exhibitDetailId = it },
                        onCompose = {
                            composingPreset = null
                            composing = true
                        },
                    )
                    Tab.SEEK -> SeekPage(
                        onOpenPoem = { corpusDetailKey = it },
                        onBrowse = { browsing = it },
                    )
                    Tab.ANTHOLOGY -> AnthologyPage(
                        onGoSeek = { tab = Tab.SEEK },
                        onOpenDetail = { poemId -> detailPoemId = poemId },
                        onOpenCompose = { poemId ->
                            composingPreset = poemId
                            composing = true
                        },
                    )
                    Tab.ME -> MePage()
                }
            }
        }
    }
}

private fun CorpusPoem.toEntity() = PoemEntity(
    id = UUID.randomUUID().toString(),
    poemKey = id,
    title = title,
    author = author,
    dynasty = dynasty,
    content = lines.joinToString("\n"),
    focusLine = focusLine,
    tags = tags.joinToString(","),
    createdAt = Instant.now().toString(),
)

@Composable
private fun Placeholder(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = 14.sp,
            letterSpacing = 0.06.em,
            color = Ink3,
        )
    }
}
