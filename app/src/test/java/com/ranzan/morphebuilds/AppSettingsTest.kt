package com.ranzan.morphebuilds

import com.ranzan.morphebuilds.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppSettingsTest {

    @Test
    fun testExtractRepoPath() {
        val settingsDefault = AppSettings(repoUrl = "https://github.com/j-hc/revanced-magisk-module/releases/")
        val path1 = settingsDefault.extractRepoPath()
        assertNotNull(path1)
        assertEquals("j-hc", path1?.first)
        assertEquals("revanced-magisk-module", path1?.second)

        val settingsShort = AppSettings(repoUrl = "custom-user/my-repo")
        val path2 = settingsShort.extractRepoPath()
        assertNotNull(path2)
        assertEquals("custom-user", path2?.first)
        assertEquals("my-repo", path2?.second)

        val settingsNoReleases = AppSettings(repoUrl = "https://github.com/someone/cool-app")
        val path3 = settingsNoReleases.extractRepoPath()
        assertNotNull(path3)
        assertEquals("someone", path3?.first)
        assertEquals("cool-app", path3?.second)

        // MicroG-RE repo extraction
        val microGPath = settingsDefault.extractRepoPath(AppSettings.MICROG_REPO_URL)
        assertNotNull(microGPath)
        assertEquals("MorpheApp", microGPath?.first)
        assertEquals("MicroG-RE", microGPath?.second)

        // Verify default format preference: APKs only
        assertEquals(true, settingsDefault.showApks)
        assertEquals(false, settingsDefault.showModules)
    }
}
