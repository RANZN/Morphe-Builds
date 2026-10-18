package com.ranzan.morphebuilds.data.remote

import com.ranzan.morphebuilds.data.remote.model.GitHubReleaseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun getReleases(
        owner: String,
        repo: String,
        token: String? = null,
        perPage: Int = 10
    ): List<GitHubReleaseDto> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/releases?per_page=$perPage"

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "MorpheBuilds-Android-App")

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            response.close()
            when (response.code) {
                404 -> throw IOException("Repository not found: $owner/$repo. Please check your repository URL.")
                403 -> {
                    if (responseBody.contains("rate limit", ignoreCase = true)) {
                        throw IOException("GitHub API rate limit exceeded. Add a GitHub Personal Access Token in Settings to bypass.")
                    } else {
                        throw IOException("Access denied (403) to $owner/$repo.")
                    }
                }
                else -> throw IOException("GitHub API returned error ${response.code}: ${response.message}")
            }
        }

        val bodyString = response.body?.string() ?: throw IOException("Empty response body from GitHub")
        response.close()

        json.decodeFromString<List<GitHubReleaseDto>>(bodyString)
    }
}
