package com.fusheng.poetry.data

// 通用设置（SharedPreferences）：默认署名 + 照片存储质量
import android.content.Context

object SettingsStore {
    private const val PREFS = "settings"
    private const val KEY_PHOTO_QUALITY = "photo_quality"
    private const val KEY_PEN_NAME = "pen_name"

    // 质量档：0 保画质(2000,q90) / 1 旧标准档(1600,q80，仅存量数据) / 2 省空间(1080,q70)
    // 定稿 §5.5 UI 只暴露 保画质 / 省空间 两档；历史值 1 归入保画质展示
    fun photoQuality(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_PHOTO_QUALITY, 0)

    fun setPhotoQuality(context: Context, quality: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PHOTO_QUALITY, quality).apply()
    }

    fun penName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PEN_NAME, "浮生客") ?: "浮生客"

    fun setPenName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PEN_NAME, name).apply()
    }
}
