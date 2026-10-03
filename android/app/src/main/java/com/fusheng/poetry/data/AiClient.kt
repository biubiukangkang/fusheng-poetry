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

    private const val SYSTEM_PROMPT =
        "你是精通古诗词的助手。根据用户描述的生活场景，创作 3 组两句一组的古风短句，" +
            "意境贴合场景、语言自然。只输出 JSON：" +
            "{\"verses\":[\"第一组两句用逗号连接\",\"第二组…\",\"第三组…\"]}，" +
            "每组不超过 14 字，不要任何解释和其他内容。"

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

    suspend fun seekVerses(scene: String, style: String = "贴意"): List<String> = withContext(Dispatchers.IO) {
        val styleHint = when (style) {
            "豪放" -> "风格偏豪放：气象开阔、笔力雄健。"
            "婉约" -> "风格偏婉约：柔婉细腻、情致含蓄。"
            else -> "" // 贴意：默认贴合场景即可
        }
        val client = newClient()
        try {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT + styleHint))
                    put(JSONObject().put("role", "user").put("content", scene))
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

            parseVerses(content)
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

    /** 优先解析 JSON；模型输出不守格式时按行/分号降级切分 */
    internal fun parseVerses(content: String): List<String> {
        val text = content.trim().trimIndent()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) {
            try {
                val verses = JSONObject(text.substring(start, end + 1)).getJSONArray("verses")
                val list = (0 until verses.length()).map { verses.getString(it).trim() }
                    .filter { it.isNotEmpty() }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // 落到降级切分
            }
        }
        return text.split("\n", "；", ";")
            .map { it.trim().trim('*', '-', '·', ' ', '"', '「', '」') }
            .filter { it.isNotEmpty() && !it.startsWith("{") }
            .distinct()
            .take(6)
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
