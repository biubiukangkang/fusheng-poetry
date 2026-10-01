package com.fusheng.poetry.data

// 词库：assets/corpus.json（50 首精选 + gushiwen 名篇扩充共 3300+ 首，带译文/注释/赏析）
// 文件 16MB，必须经 loadAsync 在 IO 线程解析；结果进程内缓存
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class CorpusSub(
    val label: String?, // 小节标题：译文 / 注释；无标题为 null
    val paragraphs: List<String>,
)

/** 解析区块：译文及注释 / 创作背景 / 赏析（源头 gushiwen 数据集 sons 字段） */
data class CorpusAnnotation(
    val name: String,
    val subs: List<CorpusSub>,
)

data class CorpusPoem(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val lines: List<String>,
    val focusLine: String,
    val tags: List<String>,
    val annotations: List<CorpusAnnotation> = emptyList(),
)

object CorpusRepo {
    @Volatile
    private var cache: List<CorpusPoem>? = null
    private val mutex = Mutex()

    /** 兼容保留：已有缓存时直接用；无缓存时在调用线程解析（仅限小词库/已预热场景） */
    fun load(context: Context): List<CorpusPoem> = cache ?: parse(context)

    /** 后台解析全词库，完成后缓存 */
    suspend fun loadAsync(context: Context): List<CorpusPoem> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: withContext(Dispatchers.IO) { parse(context) }.also { cache = it }
        }
    }

    private fun parse(context: Context): List<CorpusPoem> {
        val json = context.assets.open("corpus.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = ArrayList<CorpusPoem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                CorpusPoem(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    author = o.getString("author"),
                    dynasty = o.getString("dynasty"),
                    lines = o.getJSONArray("lines").let { l -> (0 until l.length()).map { l.getString(it) } },
                    focusLine = o.getString("focusLine"),
                    tags = o.getJSONArray("tags").let { l -> (0 until l.length()).map { l.getString(it) } },
                    annotations = o.optJSONArray("annotations")?.let { aarr ->
                        (0 until aarr.length()).map { ai ->
                            val a = aarr.getJSONObject(ai)
                            CorpusAnnotation(
                                name = a.getString("name"),
                                subs = a.getJSONArray("subs").let { sarr ->
                                    (0 until sarr.length()).map { si ->
                                        val s = sarr.getJSONObject(si)
                                        CorpusSub(
                                            label = s.optString("label", "").ifEmpty { null },
                                            paragraphs = s.getJSONArray("paragraphs")
                                                .let { parr -> (0 until parr.length()).map { parr.getString(it) } },
                                        )
                                    }
                                },
                            )
                        }
                    } ?: emptyList(),
                ),
            )
        }
        return list
    }
}
