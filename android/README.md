# android/ · 浮生诗集 App

Kotlin + Jetpack Compose 原生 App，日常唯一入口与数据真源。离线全功能：创作入馆、浮生馆作品流、作品夜展、觅诗（每日一句 + 全库搜索 + 按索引浏览）、诗集（标签 + 整理）、我（署名/存储质量/检查更新）、导出图。

**v4.0.0（2026-09-30）**：按 `输出/浮生诗集_定稿/` 全量重构 UI——概念「手卷 / 笺纸 / 竖排诗 / 朱砂印 / 木轴底栏」。数据层（Room poems/exhibits）与 v3 兼容，旧数据直接可用。**设计权威文件在仓库根 `输出/浮生诗集_定稿/`，改 UI 前先读 `设计定稿.md`**。

## 构建

```bash
cd android
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk（约 40MB：词库 16MB + 内置字体 33MB）
```

模拟器运行用 Android Studio 或 android-emulator 工具链（AVD：kezhao-test）。Windows 的中文路径与 Gradle 坑见根目录 `踩坑日志.md`。

## 字体（内置，不依赖系统）

`app/src/main/res/font/`：

- `noto_serif_sc_vf.ttf` — 思源宋体可变字体（OFL），诗/UI 标题；Compose `FontVariation` 按 400/500/600/700 取字重
- `lxgw_wenkai.ttf` — 霞鹜文楷（OFL），注记/印章/空状态

字体的获取链路（GitHub 直连全挂时的替代源）见 `踩坑日志.md`。

## 密钥（不进 Git）

`android/local.properties`（.gitignore 已覆盖）：

```properties
AGNES_API_KEY=sk-xxx     # AI 配诗/AI 补全，构建时注入 BuildConfig
```

接口：`https://apihub.agnes-ai.com/v1`，OpenAI 兼容 Bearer，模型 `agnes-3.0-flash`（`data/AiClient.kt`：`seekVerses(scene, style)` 三风格觅句、`completePoem` 补全残句）。

## 代码结构

- `MainActivity.kt` — 五键壳 + 导航层（夜展/诗集详情/词库浏览/词库详情覆盖层，底栏任意键清全部）
- `ui/theme/Theme.kt` — 色板令牌（纸/笺/墨/朱砂/夜墨/旧金…）+ 三字体族（Serif/Kai/Sans）
- `ui/VerticalPoem.kt` — 竖排诗组件（列右→左、字上→下，Rtl Row + 逐字 Text，lineHeight 1em）+ 汉字数字/拆句工具
- `ui/Paper.kt` — 纸纤维噪点 Modifier（纸面 multiply / 夜面 screen）
- `ui/UiKit.kt` — 共享组件：Masthead / SectionLabel / InkButton / SubButton / SealSquare / SealGold / CircleKnob / SearchRow / MiniTag
- `ui/AppShell.kt` — 顶栏（纸色/夜色双态）+ 木轴底栏（四线性图标手写 Path + 朱砂方印加号浮起）
- `ui/HallPage.kt` — 浮生馆：横/竖/方图三版式（按原图比例定容器，横 316dp 宽、竖 190dp 宽高封顶 280、方 240）、月份竖排分隔、日期朱砂印、空状态
- `ui/ExhibitNightPage.kt` — 作品夜展：Night 底 + 笺面卷轴（左金线）+ 金印存图 + 移出
- `ui/SeekPage.kt` — 觅诗：墨线搜索行、今日一句（左小字右竖排）、按索引三行、结果列表条目
- `ui/CorpusBrowserPage.kt` — 词库浏览：朝代/作者/体裁三书签 → 值列表 → 诗列表（页内 BackHandler 两层返回）
- `ui/ComposePage.kt` — 创作：照片 218×158 编辑框、笺纸注记（楷体+横线）、三枚方印配诗、AI 风格签+换一换、自写+补全残句、入馆墨条
- `ui/AnthologyPage.kt` — 诗集：标签书签筛选、诗叶（竖排叶码）、整理模式（圆形选择点 + 底部批量打标签/移出 + 确认弹窗）
- `ui/PoemDetailPage.kt` — 诗详情：宋体 20sp 行高 2.05、译/背/赏 圆钮跳块、解析楷体、诗集态带标签加/删 + 移出
- `ui/MePage.kt` — 我：朱砂方印客、书写（默认署名/照片存储质量两档）、关于（数据/版本与更新）
- `ui/ShareImage.kt` — 导出图：夜展卷面版式 Canvas 绘制（用内置字体真渲染）
- `data/` — LocalDb（Room：poems/exhibits，deletedAt 墓碑，`updatePoemTags` 标签整理）、PhotoStore（按质量档压缩）、CorpusRepo（assets/corpus.json **3327 首**带译文/注释/赏析）、SettingsStore（署名 + 质量档）、UpdateChecker、AiClient

## 遗留待办

- **UpdateChecker.REPO 是占位符 `"weichen/fusheng-poetry"`**——宸哥建好 GitHub 发布仓库后改成实际值，「我 → 版本与更新」才生效
- 真机安装：直接装 `app-debug.apk` 覆盖安装（不丢数据），无任何联网配置要改
- AI 配诗（三风格）只验证了编译与 UI 流，**真网络请求未验**（模拟器会话未接 agnes-ai）
- 字体可子集化瘦身（当前全量 33MB，APK 约 40MB；自用可接受，若发 Release 建议按词库+常用字子集化）
- 竖排长诗（诗详情全文）当前横排展示；定稿 §10.3 的 Canvas 自绘纵排为可选增强
