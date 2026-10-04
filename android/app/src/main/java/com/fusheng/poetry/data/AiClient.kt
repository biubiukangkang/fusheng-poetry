package com.fusheng.poetry.data

// AI 觅句：直连 agnes-ai（OpenAI 兼容 /chat/completions，标准 Bearer）
// 密钥经 local.properties 注入 BuildConfig，不进 Git
import com.fusheng.poetry.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object AiClient {
    private const val BASE_URL = "https://apihub.agnes-ai.com/v1"
    private const val MODEL = "agnes-3.0-flash"

    // AI 生成慢是常态：OkHttp 默认读超时 10s 会误杀慢响应，放宽到 30s
    private fun newClient() = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    // 觅诗两段式：先从注记提炼意象关键词做本地粗筛，再让 AI 只在候选清单里精选
    // ——AI 不能自己编诗，结果必定来自词库（详情页译文注释可用）
    private const val KEYWORDS_PROMPT =
        "用户写了一句生活注记。请提炼 4-6 个最适合用来检索古诗词的意象关键词" +
            "（单字或双字词，如：雨、月、桂花、归、夜、酒、花、江、雪、离别、思乡），" +
            "覆盖场景的核心意象。只输出 JSON：{\"keywords\":[\"雨\",\"夜\"]}，不要任何解释。"

    private const val PICK_PROMPT_HEAD =
        "下面是词库候选古诗词清单（编号|诗题|作者|名句）。用户的生活注记是：「"
    private const val PICK_PROMPT_TAIL =
        "。请从清单中挑出 3 首最贴合注记意境的。只输出 JSON：" +
            "{\"ids\":[\"编号\",\"编号\",\"编号\"]}，按贴合度从高到低排序，只能用清单里的编号，不要任何解释。"

    private const val COMPLETE_PROMPT =
        "你是古诗词词典。用户只记得一首真实古诗词的片段（诗题、作者或其中一句），" +
            "请补全这首诗。必须是真实存在的古诗词，宁可认不出也不能编造；" +
            "确实无法确定时只输出 {\"notFound\":true}。能确定时只输出 JSON：" +
            "{\"title\":\"诗题\",\"author\":\"作者\",\"dynasty\":\"朝代\",\"lines\":[\"一句一行\"]}，不要任何解释。"

    /** AI 补全结果：一首真实古诗词的完整信息 */
    data class PoemComplete(
        val title: String,
        val author: String,
        val dynasty: String,
        val lines: List<String>,
    )

    /** 按注记从词库觅诗：两段式（关键词粗筛 → AI 精选），返回词库原诗（保 AI 排序） */
    suspend fun seekPoems(
        scene: String,
        style: String,
        corpus: List<CorpusPoem>,
    ): List<CorpusPoem> = withContext(Dispatchers.IO) {
        val keywords = requestKeywords(scene)
        val candidates = rankCandidates(scene, keywords, corpus)
        if (candidates.isEmpty()) throw IOException("没觅到贴合的意象，换个说法试试")
        val picked = pickFromCandidates(scene, style, candidates)
        if (picked.isEmpty()) throw IOException("没觅到贴合的，多写点细节再试")
        picked
    }

    private suspend fun requestKeywords(scene: String): List<String> = withContext(Dispatchers.IO) {
        val content = chatJson(KEYWORDS_PROMPT, scene)
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return@withContext defaultKeywords(scene)
        try {
            val arr = JSONObject(content.substring(start, end + 1)).getJSONArray("keywords")
            (0 until arr.length()).map { arr.getString(it).trim() }
                .filter { it.isNotEmpty() }
                .take(6)
        } catch (e: Exception) {
            defaultKeywords(scene)
        }
    }

    /** AI 没给出可用关键词时，把注记按 2 字滑窗切成兜底关键词 */
    private fun defaultKeywords(scene: String): List<String> {
        val s = scene.filter { !it.isWhitespace() }.take(12)
        return (0 until maxOf(1, s.length - 1)).map { s.substring(it, minOf(it + 2, s.length)) }.distinct().take(6)
    }

    /** 关键词在词库里打分排序：focusLine 命中 3 分、题/作者 2 分、全诗 1 分；取前 60 首 */
    internal fun rankCandidates(
        scene: String,
        keywords: List<String>,
        corpus: List<CorpusPoem>,
    ): List<CorpusPoem> {
        if (keywords.isEmpty()) return emptyList()
        val scored = ArrayList<Pair<Int, CorpusPoem>>()
        for (p in corpus) {
            var score = 0
            for (kw in keywords) {
                if (p.focusLine.contains(kw, ignoreCase = true)) score += 3
                if (p.title.contains(kw, ignoreCase = true) || p.author.contains(kw, ignoreCase = true)) score += 2
                if (p.lines.any { it.contains(kw, ignoreCase = true) }) score += 1
                if (p.tags.any { it.contains(kw, ignoreCase = true) }) score += 1
            }
            if (score > 0) scored.add(score to p)
        }
        return scored.sortedByDescending { it.first }.take(60).map { it.second }
    }

    private suspend fun pickFromCandidates(
        scene: String,
        style: String,
        candidates: List<CorpusPoem>,
    ): List<CorpusPoem> = withContext(Dispatchers.IO) {
        val styleHint = when (style) {
            "豪放" -> "如有贴合，优先气象开阔、笔力雄健的。"
            "婉约" -> "如有贴合，优先柔婉细腻、情致含蓄的。"
            else -> ""
        }
        val listing = candidates.mapIndexed { i, p -> "${i + 1}|${p.title}|${p.author}|${p.focusLine}" }
            .joinToString("\n")
        val content = chatJson(PICK_PROMPT_HEAD + scene + "」。" + styleHint + PICK_PROMPT_TAIL, listing)
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return@withContext emptyList()
        val ids = try {
            val arr = JSONObject(content.substring(start, end + 1)).getJSONArray("ids")
            (0 until arr.length()).map { arr.getString(it).trim() }
        } catch (e: Exception) {
            emptyList()
        }
        // AI 返回的编号是 1 起始的清单序号；容错：也接受词库 id 本身
        ids.mapNotNull { raw ->
            val n = raw.toIntOrNull()
            if (n != null && n in 1..candidates.size) candidates[n - 1]
            else candidates.find { it.id == raw }
        }.distinctBy { it.id }.take(3)
    }

    private suspend fun chatJson(system: String, user: String): String = withContext(Dispatchers.IO) {
        val client = newClient()
        try {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", user))
                })
            }
            val resp = client.post("$BASE_URL/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.AGNES_API_KEY}")
                header(HttpHeaders.ContentType, "application/json")
                setBody(body.toString())
            }
            if (!resp.status.isSuccess()) throw IOException("AI 接口异常：HTTP ${resp.status.value}")
            JSONObject(resp.bodyAsText())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        } finally {
            client.close()
        }
    }

    /** 按片段线索补全一首真实古诗词；认不出时抛 IOException */
    suspend fun completePoem(hint: String): PoemComplete = withContext(Dispatchers.IO) {
        val client = newClient()
        try {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", COMPLETE_PROMPT))
                    put(JSONObject().put("role", "user").put("content", hint))
                })
            }
            val resp = client.post("$BASE_URL/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.AGNES_API_KEY}")
                header(HttpHeaders.ContentType, "application/json")
                setBody(body.toString())
            }
            if (!resp.status.isSuccess()) throw IOException("AI 接口异常：HTTP ${resp.status.value}")
            val content = JSONObject(resp.bodyAsText())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")

            parseComplete(content) ?: throw IOException("没认出这首诗，多给点线索再试")
        } finally {
            client.close()
        }
    }

    /** 解析补全结果；notFound / 缺关键字段 / 格式坏都返回 null */
    internal fun parseComplete(content: String): PoemComplete? {
        val text = content.trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val o = JSONObject(text.substring(start, end + 1))
            if (o.optBoolean("notFound", false)) return null
            val lines = o.getJSONArray("lines").let { l ->
                (0 until l.length()).map { l.getString(it).trim() }.filter { it.isNotEmpty() }
            }
            val title = o.getString("title").trim()
            if (title.isEmpty() || lines.isEmpty()) return null
            PoemComplete(
                title = title,
                author = o.optString("author", "").trim().ifEmpty { "佚名" },
                dynasty = o.optString("dynasty", "").trim(),
                lines = lines,
            )
        } catch (e: Exception) {
            null
        }
    }
}
