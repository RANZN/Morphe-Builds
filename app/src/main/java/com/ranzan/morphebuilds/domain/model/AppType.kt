package com.ranzan.morphebuilds.domain.model

enum class AppType(
    val title: String,
    val knownPackageNames: List<String>,
    val defaultIconName: String
) {
    YOUTUBE(
        title = "YouTube",
        knownPackageNames = listOf(
            "app.revanced.android.youtube",
            "com.google.android.youtube"
        ),
        defaultIconName = "youtube"
    ),
    YOUTUBE_MUSIC(
        title = "YouTube Music",
        knownPackageNames = listOf(
            "app.revanced.android.apps.youtube.music",
            "com.google.android.apps.youtube.music"
        ),
        defaultIconName = "music"
    ),
    REDDIT(
        title = "Reddit",
        knownPackageNames = listOf(
            "app.revanced.android.reddit",
            "com.reddit.frontpage"
        ),
        defaultIconName = "reddit"
    ),
    TWITTER(
        title = "Twitter / X",
        knownPackageNames = listOf(
            "app.revanced.android.twitter",
            "com.twitter.android"
        ),
        defaultIconName = "twitter"
    ),
    MICROG(
        title = "MicroG-RE",
        knownPackageNames = listOf(
            "app.revanced.android.gms",
            "com.mgoogle.android.gms",
            "com.vanced.android.gms"
        ),
        defaultIconName = "key"
    ),
    OTHER(
        title = "Module / App",
        knownPackageNames = emptyList(),
        defaultIconName = "android"
    );

    companion object {
        fun detectFromFileName(fileName: String): AppType {
            val lower = fileName.lowercase()
            return when {
                lower.startsWith("youtube") || lower.contains("youtube") -> YOUTUBE
                lower.startsWith("music") || lower.contains("music") -> YOUTUBE_MUSIC
                lower.startsWith("reddit") || lower.contains("reddit") -> REDDIT
                lower.startsWith("twitter") || lower.contains("twitter") || lower.contains("piko") -> TWITTER
                lower.startsWith("microg") || lower.contains("microg") -> MICROG
                else -> OTHER
            }
        }
    }
}

