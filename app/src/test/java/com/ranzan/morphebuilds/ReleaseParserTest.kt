package com.ranzan.morphebuilds

import com.ranzan.morphebuilds.data.remote.ReleaseParser
import com.ranzan.morphebuilds.data.remote.model.GitHubAssetDto
import com.ranzan.morphebuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.morphebuilds.domain.model.AppType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    @Test
    fun testParseYouTubeApkAsset() {
        val asset = GitHubAssetDto(
            id = 1234,
            name = "youtube-morphe-v21.20.405-all.apk",
            size = 145000000,
            browserDownloadUrl = "https://github.com/j-hc/revanced-magisk-module/releases/download/20221113/youtube-morphe-v21.20.405-all.apk"
        )

        val build = ReleaseParser.parseAsset(asset, "20221113", "2026-10-17T16:00:00Z")
        assertNotNull(build)
        assertEquals(AppType.YOUTUBE, build?.appType)
        assertEquals("v21.20.405", build?.version)
        assertEquals("all", build?.architecture)
        assertFalse(build?.isModule ?: true)
        assertTrue(build?.isApk ?: false)
    }

    @Test
    fun testParseMusicModuleAsset() {
        val asset = GitHubAssetDto(
            id = 5678,
            name = "music-morphe-module-v9.20.53-arm64-v8a.zip",
            size = 80000000,
            browserDownloadUrl = "https://github.com/j-hc/revanced-magisk-module/releases/download/20221113/music-morphe-module-v9.20.53-arm64-v8a.zip"
        )

        val build = ReleaseParser.parseAsset(asset, "20221113", "2026-10-17T16:00:00Z")
        assertNotNull(build)
        assertEquals(AppType.YOUTUBE_MUSIC, build?.appType)
        assertEquals("v9.20.53", build?.version)
        assertEquals("arm64-v8a", build?.architecture)
        assertTrue(build?.isModule ?: false)
        assertFalse(build?.isApk ?: true)
    }

    @Test
    fun testParseRedditAndTwitterAssets() {
        val redditAsset = GitHubAssetDto(
            id = 111,
            name = "reddit-morphe-v2026.24.0-all.apk",
            size = 60000000,
            browserDownloadUrl = "https://github.com/test/reddit.apk"
        )
        val redditBuild = ReleaseParser.parseAsset(redditAsset, "20221113", "")
        assertNotNull(redditBuild)
        assertEquals(AppType.REDDIT, redditBuild?.appType)
        assertEquals("v2026.24.0", redditBuild?.version)

        val twitterAsset = GitHubAssetDto(
            id = 222,
            name = "twitter-piko-v12.34.0-prod.01-all.apk",
            size = 148000000,
            browserDownloadUrl = "https://github.com/test/twitter.apk"
        )
        val twitterBuild = ReleaseParser.parseAsset(twitterAsset, "20221116", "")
        assertNotNull(twitterBuild)
        assertEquals(AppType.TWITTER, twitterBuild?.appType)
        assertEquals("v12.34.0-prod.01", twitterBuild?.version)
    }
}
