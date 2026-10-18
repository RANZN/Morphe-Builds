package com.ranzan.morphebuilds

import com.ranzan.morphebuilds.core.utils.Architecture
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureHelperTest {

    @Test
    fun testArchitectureMatching() {
        // Universal 'all' matches target arch
        assertTrue(Architecture.isMatch("arm64-v8a", "all"))
        assertTrue(Architecture.isMatch("arm-v7a", "all"))
        assertTrue(Architecture.isMatch("all", "arm64-v8a"))

        // Exact matches
        assertTrue(Architecture.isMatch("arm64-v8a", "arm64-v8a"))
        assertTrue(Architecture.isMatch("arm-v7a", "arm-v7a"))

        // Alias matches
        assertTrue(Architecture.isMatch("arm64-v8a", "arm64"))
        assertTrue(Architecture.isMatch("arm-v7a", "armeabi-v7a"))

        // Incompatible archs
        assertFalse(Architecture.isMatch("arm-v7a", "arm64-v8a"))
        assertFalse(Architecture.isMatch("x86_64", "arm64-v8a"))
    }
}
