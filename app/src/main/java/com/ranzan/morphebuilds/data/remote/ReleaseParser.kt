package com.ranzan.morphebuilds.data.remote

import com.ranzan.morphebuilds.data.remote.model.GitHubAssetDto
import com.ranzan.morphebuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.Release

object ReleaseParser {

    private val preciseVersionRegex = Regex("-(?:v)?([0-9]+(?:\\.[0-9]+)+(?:-[a-zA-Z0-9.]+)*?)(?:-(?:icon|noicon))?(?:-(?:all|arm64-v8a|arm-v7a|armeabi-v7a|x86_64|x86))?\\.(?:apk|zip)", RegexOption.IGNORE_CASE)
    private val fallbackVersionRegex = Regex("v?(\\d+(?:\\.\\d+)+(?:-[a-zA-Z0-9.]+)*)", RegexOption.IGNORE_CASE)

    fun toDomainRelease(dto: GitHubReleaseDto): Release {
        val builds = dto.assets.mapNotNull { asset ->
            parseAsset(asset, dto.tagName, dto.publishedAt ?: "")
        }

        return Release(
            tag = dto.tagName,
            name = dto.name ?: dto.tagName,
            publishedAt = dto.publishedAt ?: "",
            htmlUrl = dto.htmlUrl ?: "",
            body = dto.body ?: "",
            builds = builds
        )
    }

    fun parseAsset(asset: GitHubAssetDto, releaseTag: String, publishedAt: String): AppBuild? {
        val fileName = asset.name
        val lower = fileName.lowercase()

        // Only process .apk and .zip files
        if (!lower.endsWith(".apk") && !lower.endsWith(".zip")) {
            return null
        }

        val isModule = lower.endsWith(".zip") || lower.contains("-module")
        val appType = AppType.detectFromFileName(fileName)

        // Extract version
        val preciseMatch = preciseVersionRegex.find(fileName)
        val version = if (preciseMatch != null) {
            "v" + preciseMatch.groupValues[1]
        } else {
            val fallbackMatch = fallbackVersionRegex.find(fileName)
            fallbackMatch?.value ?: releaseTag
        }

        // Extract architecture
        val architecture = when {
            lower.contains("arm64-v8a") || lower.contains("arm64") -> "arm64-v8a"
            lower.contains("arm-v7a") || lower.contains("armeabi-v7a") || lower.contains("armv7") -> "arm-v7a"
            lower.contains("x86_64") || lower.contains("x64") -> "x86_64"
            lower.contains("x86") -> "x86"
            lower.contains("all") || lower.contains("universal") -> "all"
            else -> "all"
        }

        // Custom Title if MicroG or OTHER
        val customTitle = when (appType) {
            AppType.MICROG -> {
                when {
                    lower.contains("-noicon") -> "MicroG-RE (No Icon)"
                    lower.contains("-icon") -> "MicroG-RE (With Icon)"
                    else -> "MicroG-RE"
                }
            }
            AppType.OTHER -> {
                fileName.substringBefore("-v")
                    .substringBefore("-module")
                    .replace("-", " ")
                    .replaceFirstChar { it.uppercase() }
            }
            else -> null
        }


        return AppBuild(
            id = "${releaseTag}_${asset.id}_${asset.name}",
            appType = appType,
            customTitle = customTitle,
            version = version,
            isModule = isModule,
            architecture = architecture,
            fileName = asset.name,
            downloadUrl = asset.browserDownloadUrl,
            sizeBytes = asset.size,
            releaseTag = releaseTag,
            publishedAt = publishedAt
        )
    }
}
