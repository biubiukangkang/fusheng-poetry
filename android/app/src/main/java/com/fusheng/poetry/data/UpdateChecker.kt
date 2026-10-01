package com.fusheng.poetry.data

// 检查更新：拉 GitHub latest release 的 tag 与 BuildConfig.VERSION_NAME 对比
// 宸哥的发布仓库建好后把 REPO 改成实际值即可生效
import android.util.Log
import com.fusheng.poetry.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object UpdateChecker {
    private const val REPO = "biubiukangkang/fusheng-poetry"
    private const val TAG = "UpdateChecker"

    data class Result(val latestVersion: String?, val releaseUrl: String?)

    /** 远端失败/无网络返回 null；版本相同则 latestVersion 为当前版本 */
    suspend fun check(): Result = withContext(Dispatchers.IO) {
        val client = HttpClient(OkHttp)
        try {
            val resp = client.get("https://api.github.com/repos/$REPO/releases/latest")
            if (!resp.status.isSuccess()) return@withContext Result(null, null)
            val json = JSONObject(resp.bodyAsText())
            val tag = json.optString("tag_name", "").removePrefix("v")
            val url = json.optString("html_url", "https://github.com/$REPO/releases")
            if (tag.isEmpty()) Result(null, null) else Result(tag, url)
        } catch (e: Exception) {
            Log.w(TAG, "check failed", e)
            Result(null, null)
        } finally {
            client.close()
        }
    }

    fun isNewer(latest: String, current: String = BuildConfig.VERSION_NAME): Boolean {
        val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
