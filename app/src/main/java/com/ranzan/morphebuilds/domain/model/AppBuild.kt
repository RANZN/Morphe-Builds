package com.ranzan.morphebuilds.domain.model

data class AppBuild(
    val id: String,
    val appType: AppType,
    val customTitle: String? = null,
    val version: String,
    val isModule: Boolean,
    val architecture: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val releaseTag: String,
    val publishedAt: String
) {
    val displayName: String
        get() = customTitle ?: appType.title

    val isApk: Boolean
        get() = !isModule && fileName.endsWith(".apk", ignoreCase = true)

    val formatBadge: String
        get() = if (isModule) "MAGISK MODULE" else "APK"
}
