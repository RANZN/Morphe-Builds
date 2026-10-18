package com.ranzan.morphebuilds.domain.model

data class AppSettings(
    val repoUrl: String = DEFAULT_REPO_URL,
    val preferredArch: String = "auto",
    val appArchOverrides: Map<String, String> = emptyMap(),
    val downloadFolderUri: String? = null,
    val downloadFolderPath: String? = null,
    val autoCleanOldVersions: Boolean = true,
    val cleanupIntervalDays: Int = 7, // 1, 7, 30 days (or 0 for disabled)
    val showApks: Boolean = true,
    val showModules: Boolean = false,
    val githubToken: String? = null,
    val isSetupCompleted: Boolean = false
) {
    companion object {
        const val DEFAULT_REPO_URL = "https://github.com/j-hc/revanced-magisk-module/releases/"
        const val MICROG_REPO_URL = "https://github.com/MorpheApp/MicroG-RE/releases/"
    }

    /**
     * Resolves owner and repo from the URL.
     * E.g. "https://github.com/j-hc/revanced-magisk-module/releases/" -> ("j-hc", "revanced-magisk-module")
     */
    fun extractRepoPath(url: String = repoUrl): Pair<String, String>? {
        val clean = url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix("/releases")
            .removeSuffix("/releases/")

        val parts = clean.split("/")
        return if (parts.size >= 2) {
            Pair(parts[0], parts[1])
        } else {
            null
        }
    }
}
