package com.ranzan.morphebuilds

import com.ranzan.morphebuilds.core.utils.FormatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatHelperTest {

    @Test
    fun testVersionComparison() {
        // v21.20.405 is newer than v21.19.0
        assertTrue(FormatHelper.compareVersions("v21.20.405", "v21.19.0") > 0)

        // v21.19.0 is older than v21.20.405
        assertTrue(FormatHelper.compareVersions("v21.19.0", "v21.20.405") < 0)

        // Equal versions
        assertEquals(0, FormatHelper.compareVersions("v9.20.53", "9.20.53"))

        // Date-based semver: 2026.24.0 > 2026.23.0
        assertTrue(FormatHelper.compareVersions("2026.24.0", "2026.23.0") > 0)
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", FormatHelper.formatBytes(0))
        assertEquals("1 KB", FormatHelper.formatBytes(1024))
        assertEquals("141.7 MB", FormatHelper.formatBytes(148582711))
    }
}
