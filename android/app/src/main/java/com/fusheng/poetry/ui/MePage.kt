package com.fusheng.poetry.ui

// 我（定稿 §5.5，实现参考 06-我.html）：
// 朱砂方印浮生客 + 书写（默认署名 / 照片存储质量两档）+ 关于（数据 / 版本与更新）
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fusheng.poetry.BuildConfig
import com.fusheng.poetry.data.FushengDb
import com.fusheng.poetry.data.SettingsStore
import com.fusheng.poetry.data.UpdateChecker
import com.fusheng.poetry.ui.theme.Ink
import com.fusheng.poetry.ui.theme.Ink2
import com.fusheng.poetry.ui.theme.Ink3
import com.fusheng.poetry.ui.theme.KaiFont
import com.fusheng.poetry.ui.theme.Line
import com.fusheng.poetry.ui.theme.Paper
import com.fusheng.poetry.ui.theme.PaperHi
import com.fusheng.poetry.ui.theme.SansFont
import com.fusheng.poetry.ui.theme.Seal
import com.fusheng.poetry.ui.theme.SealText
import com.fusheng.poetry.ui.theme.SerifFont
import kotlinx.coroutines.launch

// 质量两档（定稿 §5.5）：值 0=保画质 / 2=省空间；历史档 1 归入保画质
private val QUALITY_OPTIONS = listOf(0 to "保画质", 2 to "省空间")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MePage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { FushengDb.get(context).dao() }
    val exhibits by dao.exhibitCards().collectAsState(initial = emptyList())
    val poems by dao.pickedPoems().collectAsState(initial = emptyList())

    var penName by remember { mutableStateOf(SettingsStore.penName(context)) }
    var quality by remember { mutableStateOf(SettingsStore.photoQuality(context)) }
    var showPenName by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var updateUrl by remember { mutableStateOf<String?>(null) }

    fun checkUpdate() {
        when {
            checking -> Unit
            updateUrl != null -> runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
            }
            else -> {
                checking = true
                scope.launch {
                    val r = UpdateChecker.check()
                    updateMsg = when {
                        r.latestVersion == null -> "暂时连不上"
                        UpdateChecker.isNewer(r.latestVersion) -> "发现新版 v${r.latestVersion}"
                        else -> "已是最新"
                    }
                    updateUrl = if (r.latestVersion != null && UpdateChecker.isNewer(r.latestVersion)) r.releaseUrl else null
                    checking = false
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .paperFibers()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 48.dp),
    ) {
        Masthead("我", "自用 · 本地")

        // 个人：朱砂方印 + 署名 + 统计
        Row(
            Modifier.padding(top = 14.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(Seal, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("客", fontFamily = KaiFont, fontSize = 19.sp, color = SealText)
            }
            Spacer(Modifier.size(18.dp))
            Column {
                Text(
                    penName,
                    fontFamily = SerifFont,
                    fontSize = 22.sp,
                    letterSpacing = 0.12.em,
                    color = Ink,
                )
                Text(
                    "入馆 ${exhibits.size} 件 · 拾诗 ${poems.size} 首",
                    fontFamily = SansFont,
                    fontSize = 10.sp,
                    letterSpacing = 0.14.em,
                    color = Ink3,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        GroupLabel("书写")
        SettingRow("默认署名", penName, valueColor = Seal) { showPenName = true }
        SettingRow("照片存储质量", QUALITY_OPTIONS.firstOrNull { it.first == quality }?.second ?: "保画质") { showQuality = true }

        GroupLabel("关于")
        SettingRow("数据", "全本地 · 不上云", clickable = false)
        SettingRow(
            "版本与更新",
            when {
                checking -> "检查中…"
                updateMsg != null -> updateMsg!!
                else -> "v${BuildConfig.VERSION_NAME}"
            },
        ) { checkUpdate() }

        Text(
            "数据只保存在这台设备上",
            fontFamily = SansFont,
            fontSize = 10.sp,
            letterSpacing = 0.18.em,
            color = Ink3,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        )
    }

    // 署名编辑抽屉
    if (showPenName) {
        var name by remember { mutableStateOf(penName) }
        ModalBottomSheet(onDismissRequest = { showPenName = false }, containerColor = PaperHi) {
            Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("默认署名", fontFamily = SerifFont, fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(14.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 12) name = it },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = SerifFont, fontSize = 16.sp, color = Ink),
                    cursorBrush = SolidColor(Seal),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth().height(44.dp).background(Paper), contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) Text("浮生客", fontFamily = SerifFont, fontSize = 15.sp, color = Ink3)
                            inner
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                InkButton("记下", enabled = name.isNotBlank()) {
                    penName = name.trim()
                    SettingsStore.setPenName(context, penName)
                    showPenName = false
                }
            }
        }
    }

    // 质量选择抽屉
    if (showQuality) {
        ModalBottomSheet(onDismissRequest = { showQuality = false }, containerColor = PaperHi) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("照片存储质量", fontFamily = SerifFont, fontSize = 16.sp, color = Ink)
                Text(
                    "入馆照片按此档位压缩后保存",
                    fontFamily = SansFont,
                    fontSize = 12.sp,
                    color = Ink3,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(10.dp))
                QUALITY_OPTIONS.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                quality = value
                                SettingsStore.setPhotoQuality(context, value)
                                showQuality = false
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, fontFamily = SansFont, fontSize = 15.sp, color = Ink)
                        Spacer(Modifier.weight(1f))
                        if (quality == value || (quality == 1 && value == 0)) {
                            Text("●", fontSize = 12.sp, color = Seal)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        fontFamily = SansFont,
        fontSize = 10.sp,
        letterSpacing = 0.22.em,
        color = Ink3,
        modifier = Modifier.padding(top = 22.dp, bottom = 4.dp),
    )
}

// 设置行（fix.css .setting-row）：52dp 高，底线分隔
@Composable
private fun SettingRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = Ink3,
    clickable: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = SansFont, fontSize = 15.sp, letterSpacing = 0.05.em, color = Ink)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value == "检查中…") {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Ink3)
                Spacer(Modifier.size(8.dp))
            }
            Text(value, fontFamily = SansFont, fontSize = 13.sp, letterSpacing = 0.04.em, color = valueColor)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}
