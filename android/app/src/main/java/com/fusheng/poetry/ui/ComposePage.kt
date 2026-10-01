package com.fusheng.poetry.ui

// 创作页（定稿 §5.3，实现参考 04-创作.html）：
// 照片 218×158 编辑框 → 笺纸注记 → 配诗三枚方印（觅 AI / 集 诗集 / 写 自写）→ 入馆墨条
// AI 按注记配诗（贴意/豪放/婉约 + 换一换）；自写可补全残句
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.fusheng.poetry.data.AiClient
import com.fusheng.poetry.data.ExhibitEntity
import com.fusheng.poetry.data.FushengDb
import com.fusheng.poetry.data.PhotoStore
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
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePage(presetPoemId: String? = null, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { FushengDb.get(context).dao() }
    val picked by dao.pickedPoems().collectAsState(initial = emptyList())

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var note by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("ai") } // ai(觅) | pick(集) | write(写)
    var selectedPoemId by remember { mutableStateOf(presetPoemId) }
    var aiStyle by remember { mutableStateOf("贴意") }
    var aiVerses by remember { mutableStateOf<List<String>>(emptyList()) }
    var aiIndex by remember { mutableIntStateOf(0) }
    var aiBusy by remember { mutableStateOf(false) }
    var writeTitle by remember { mutableStateOf("") }
    var writeAuthor by remember { mutableStateOf("") }
    var writeDynasty by remember { mutableStateOf("") }
    var writeLines by remember { mutableStateOf("") }
    var completing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showPoemPicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) photoUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok -> if (ok) cameraUri?.let { photoUri = it } }

    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun seek() {
        if (note.isBlank()) {
            error = "先写一句注记，AI 才好配诗"
            return
        }
        aiBusy = true
        error = null
        scope.launch {
            aiVerses = try {
                AiClient.seekVerses(note.trim(), aiStyle)
            } catch (e: Exception) {
                error = "觅句失败：${e.message}"
                emptyList()
            }
            aiIndex = 0
            aiBusy = false
        }
    }

    fun submitAiComplete() {
        val hint = listOf(writeTitle, writeAuthor, writeLines)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        if (hint.isEmpty()) return
        completing = true
        error = null
        scope.launch {
            try {
                val r = AiClient.completePoem(hint)
                writeTitle = r.title
                writeAuthor = r.author
                writeDynasty = r.dynasty
                writeLines = r.lines.joinToString("\n")
            } catch (e: Exception) {
                error = "补全失败：${e.message}"
            } finally {
                completing = false
            }
        }
    }

    fun submit() {
        val photo = photoUri
        if (photo == null) {
            error = "请先选择照片"
            return
        }
        submitting = true
        error = null
        scope.launch {
            try {
                var poem = picked.find { it.id == selectedPoemId }
                if (mode == "write") {
                    val title = writeTitle.trim()
                    val lines = writeLines.trim()
                    if (title.isEmpty() || lines.isEmpty()) {
                        error = "请填写诗题和诗句"
                        submitting = false
                        return@launch
                    }
                    val written = PoemEntity(
                        id = UUID.randomUUID().toString(),
                        poemKey = "",
                        title = title,
                        author = writeAuthor.trim().ifEmpty { "佚名" },
                        dynasty = writeDynasty.trim(),
                        content = lines,
                        focusLine = lines.split("\n").first(),
                        tags = "",
                        createdAt = Instant.now().toString(),
                    )
                    dao.insertPoem(written)
                    poem = written
                }
                val chosen = poem
                if (chosen == null) {
                    error = "请先配一首诗"
                    submitting = false
                    return@launch
                }
                val photoId = PhotoStore.save(context, photo)
                dao.insertExhibit(
                    ExhibitEntity(
                        id = UUID.randomUUID().toString(),
                        photoId = photoId,
                        note = note.trim(),
                        poemId = chosen.id,
                        focusLine = chosen.focusLine,
                        source = if (mode == "ai") "ai" else "manual",
                        createdAt = Instant.now().toString(),
                    ),
                )
                onDone()
            } catch (e: Exception) {
                error = "入馆失败，请重试"
            } finally {
                submitting = false
            }
        }
    }

    fun submitAiVerse() {
        val photo = photoUri
        if (aiVerses.isEmpty()) {
            error = "先觅一句"
            return
        }
        val verse = aiVerses[aiIndex % aiVerses.size]
        if (photo == null) {
            error = "请先选择照片"
            return
        }
        submitting = true
        error = null
        scope.launch {
            try {
                val written = PoemEntity(
                    id = UUID.randomUUID().toString(),
                    poemKey = "",
                    title = note.trim().take(12).ifEmpty { "AI 觅句" },
                    author = "浮生客",
                    dynasty = "",
                    content = verse,
                    focusLine = verse,
                    tags = "",
                    createdAt = Instant.now().toString(),
                )
                dao.insertPoem(written)
                val photoId = PhotoStore.save(context, photo)
                dao.insertExhibit(
                    ExhibitEntity(
                        id = UUID.randomUUID().toString(),
                        photoId = photoId,
                        note = note.trim(),
                        poemId = written.id,
                        focusLine = verse,
                        source = "ai",
                        createdAt = Instant.now().toString(),
                    ),
                )
                onDone()
            } catch (e: Exception) {
                error = "入馆失败，请重试"
            } finally {
                submitting = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .paperFibers()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        // 一、生活照：218×158 编辑框 + 竖排标签
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .width(218.dp)
                    .height(158.dp)
                    .let { m ->
                        if (photoUri == null) {
                            m
                                .drawBehind {
                                    drawRoundRect(
                                        color = Ink3.copy(alpha = 0.5f),
                                        cornerRadius = CornerRadius(0f),
                                        style = Stroke(
                                            width = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                                        ),
                                    )
                                }
                                .clickable { showPicker = true }
                        } else {
                            m
                        }
                    },
            ) {
                val uri = photoUri
                if (uri == null) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        // 朱砂圆＋
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Seal, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("＋", color = SealText, fontSize = 18.sp, fontFamily = SansFont)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("拍照 或 从相册选择", fontFamily = SansFont, fontSize = 11.sp, letterSpacing = 0.08.em, color = Ink3)
                    }
                } else {
                    AsyncImage(
                        model = uri,
                        contentDescription = "预览",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        "重选",
                        fontFamily = SansFont,
                        fontSize = 12.sp,
                        color = Ink,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Paper.copy(alpha = 0.85f))
                            .clickable { showPicker = true }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            VerticalText("生活照", 11.sp, Ink3, 0.22f, Modifier.height(78.dp))
        }

        // 二、写一句注记：笺纸输入（楷体 15sp 行高 1.85 + 淡横线）
        SectionLabel("写一句注记", modifier = Modifier.padding(top = 16.dp))
        NotePaper(
            value = note,
            onValueChange = { if (it.length <= 100) note = it },
            placeholder = "加班晚归，\n楼下的桂花开了。",
            minHeight = 92.dp,
        )

        // 三、配一首诗：左侧竖排建议 + 右侧出处/选印/三枚方印
        SectionLabel("配一首诗")
        Row(verticalAlignment = Alignment.Top) {
            // 左：竖排建议（AI 有结果时显示当前组）
            val verseLines = if (mode == "ai" && aiVerses.isNotEmpty()) {
                splitPoemLines(aiVerses[aiIndex % aiVerses.size])
            } else {
                val sel = selectedPoemId?.let { id -> picked.find { it.id == id } }
                when {
                    sel != null -> splitPoemLines(sel.focusLine)
                    mode == "pick" -> listOf("从诗集选一首")
                    else -> listOf("写下属于你的句子")
                }
            }
            VerticalPoem(
                verseLines,
                fontSize = 18.sp,
                lineHeightFactor = 1.3f,
                modifier = Modifier.width(84.dp).height(214.dp),
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                // 出处 + 选印
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val meta = if (mode == "ai" && aiVerses.isNotEmpty()) {
                        "AI 觅句 · ${aiStyle}"
                    } else {
                        selectedPoemId?.let { id -> picked.find { it.id == id } }?.let { "${it.author}  ${it.title}" } ?: "还没选诗"
                    }
                    Text(
                        meta,
                        fontFamily = SansFont,
                        fontSize = 11.sp,
                        letterSpacing = 0.1.em,
                        color = Ink3,
                        modifier = Modifier.weight(1f),
                    )
                    if (mode == "ai" && aiVerses.isNotEmpty()) {
                        CircleKnob("选", picked = true, onClick = {}) // 当前展示组即选中
                    }
                    if (mode == "pick" && selectedPoemId != null) {
                        SealSquare("选", 22, 10)
                    }
                }
                // AI 风格签 + 换一换
                if (mode == "ai") {
                    Row(
                        Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf("贴意", "豪放", "婉约").forEach { s ->
                            StyleSign(s, aiStyle == s) {
                                aiStyle = s
                                if (aiVerses.isNotEmpty()) seek() // 切风格即重新觅句
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "换一换",
                            fontFamily = SansFont,
                            fontSize = 12.sp,
                            color = Ink2,
                            modifier = Modifier
                                .clickable(enabled = aiVerses.isNotEmpty() && !aiBusy) {
                                    aiIndex = (aiIndex + 1) % aiVerses.size
                                }
                                .padding(4.dp),
                        )
                    }
                    if (aiBusy) {
                        Text(
                            "觅句中…",
                            fontFamily = KaiFont,
                            fontSize = 13.sp,
                            color = Ink3,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    } else if (aiVerses.isEmpty()) {
                        Text(
                            "按注记为你配诗，选一种风格开始",
                            fontFamily = KaiFont,
                            fontSize = 13.sp,
                            lineHeight = 1.8.em,
                            color = Ink3,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                // 三枚模式方印
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    ModeSeal("觅", "AI 配诗", mode == "ai") { mode = "ai" }
                    ModeSeal("集", "从诗集选", mode == "pick") {
                        mode = "pick"
                        showPoemPicker = true
                    }
                    ModeSeal("写", "自己写", mode == "write") { mode = "write" }
                }
                // 自写输入
                if (mode == "write") {
                    Column(Modifier.padding(top = 14.dp)) {
                        NotePaper(writeTitle, { writeTitle = it }, "诗题", minHeight = 48.dp, fontSize = 14.sp, serif = false)
                        NotePaper(writeAuthor, { writeAuthor = it }, "作者（可留空）", minHeight = 48.dp, fontSize = 14.sp, serif = false)
                        NotePaper(
                            writeLines,
                            { writeLines = it },
                            "诗句，一行一句",
                            minHeight = 92.dp,
                            fontSize = 15.sp,
                        )
                        SubButton(
                            if (completing) "补全中…" else "补全残句",
                            enabled = !completing,
                            onClick = { submitAiComplete() },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        error?.let {
            Text(
                it,
                fontFamily = SansFont,
                fontSize = 12.sp,
                color = SealDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        val canSubmit = !submitting && photoUri != null && when (mode) {
            "pick" -> selectedPoemId != null
            "ai" -> aiVerses.isNotEmpty()
            else -> true
        }
        InkButton(if (submitting) "入 馆 中" else "入 馆", enabled = canSubmit) {
            if (mode == "ai") submitAiVerse() else submit()
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            containerColor = PaperHi,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
            ) {
                PickerItem("拍照") {
                    showPicker = false
                    launchCamera()
                }
                PickerItem("从相册选择") {
                    showPicker = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            }
        }
    }

    if (showPoemPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPoemPicker = false },
            containerColor = PaperHi,
        ) {
            var query by remember { mutableStateOf("") }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    "从诗集选一首",
                    fontFamily = SerifFont,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Ink,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                SearchRow(query, { query = it }, "在我的诗集里找")
                val hits = picked.filter { p ->
                    query.isBlank() ||
                        p.focusLine.contains(query, ignoreCase = true) ||
                        p.title.contains(query, ignoreCase = true) ||
                        p.author.contains(query, ignoreCase = true)
                }
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .padding(top = 8.dp),
                ) {
                    items(hits, key = { it.id }) { poem ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPoemId = poem.id
                                    showPoemPicker = false
                                }
                                .padding(vertical = 14.dp),
                        ) {
                            Text(
                                poem.focusLine,
                                fontSize = 17.sp,
                                fontFamily = SerifFont,
                                color = Ink,
                                maxLines = 1,
                            )
                            Text(
                                "${poem.author}  ${poem.title}",
                                fontSize = 11.sp,
                                fontFamily = SansFont,
                                letterSpacing = 0.1.em,
                                color = Ink3,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    }
                    if (hits.isEmpty()) {
                        item {
                            Text(
                                "没有找到，换一句试试",
                                fontFamily = KaiFont,
                                fontSize = 14.sp,
                                color = Ink3,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// 风格签：书签式小签（选中朱砂底）
@Composable
private fun StyleSign(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Seal else PaperHi, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontFamily = SansFont,
            fontSize = 11.sp,
            letterSpacing = 0.08.em,
            color = if (selected) SealText else Ink2,
        )
    }
}

// 模式方印 42dp + 下方标签（fix.css .mode-seal）
@Composable
private fun ModeSeal(char: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(42.dp)
                .background(if (active) Seal else Color.Transparent, RoundedCornerShape(5.dp))
                .border(1.dp, if (active) Seal else LineStrong, RoundedCornerShape(5.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                char,
                fontFamily = KaiFont,
                fontSize = 14.sp,
                color = if (active) SealText else Ink2,
            )
        }
        Text(
            label,
            fontFamily = SansFont,
            fontSize = 10.sp,
            letterSpacing = 0.08.em,
            color = Ink3,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// 笺纸输入：楷体（可选）+ 底部淡横线（fix.css .note-kai.ruled）
@Composable
private fun NotePaper(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    serif: Boolean = true,
) {
    val lineColor = Line
    val lineSpacing = with(androidx.compose.ui.platform.LocalDensity.current) { (fontSize.value * 1.85f).dp.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .background(PaperHi)
            .drawBehind {
                // 底部淡横线，按行高间隔
                var y = lineSpacing
                while (y < size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += lineSpacing
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .heightIn(min = minHeight),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = if (serif) KaiFont else SansFont,
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.85f).sp,
                letterSpacing = 0.03.em,
                color = Ink2,
            ),
            cursorBrush = SolidColor(Seal),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        fontFamily = if (serif) KaiFont else SansFont,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 1.85f).sp,
                        letterSpacing = 0.03.em,
                        color = Ink3.copy(alpha = 0.7f),
                    )
                }
                inner
            },
        )
    }
}

@Composable
private fun PickerItem(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontFamily = SansFont,
        fontSize = 15.sp,
        color = Ink,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
