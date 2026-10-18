package com.ranzan.morphebuilds.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("assets") val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
