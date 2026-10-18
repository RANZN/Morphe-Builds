package com.ranzan.morphebuilds.core.utils

import android.os.Build

enum class Architecture(
    val id: String,
    val displayName: String,
    val aliases: List<String>
) {
    ARM64_V8A("arm64-v8a", "arm64-v8a", listOf("arm64-v8a", "arm64", "aarch64")),
    ARM_V7A("arm-v7a", "arm-v7a", listOf("arm-v7a", "armeabi-v7a", "armv7", "armeabi")),
    X86_64("x86_64", "x86_64", listOf("x86_64", "x64")),
    X86("x86", "x86", listOf("x86")),
    ALL("all", "Universal (all)", listOf("all", "universal"));

    companion object {
        fun detectDeviceArch(): Architecture {
            val supportedAbis = Build.SUPPORTED_ABIS ?: emptyArray()
            for (abi in supportedAbis) {
                val lower = abi.lowercase()
                when {
                    lower.contains("arm64") || lower.contains("aarch64") -> return ARM64_V8A
                    lower.contains("arm") || lower.contains("v7") -> return ARM_V7A
                    lower.contains("x86_64") || lower.contains("x64") -> return X86_64
                    lower.contains("x86") -> return X86
                }
            }
            return ARM64_V8A
        }

        fun fromString(value: String): Architecture {
            val lower = value.lowercase().trim()
            return when {
                lower.contains("arm64") || lower.contains("aarch64") -> ARM64_V8A
                lower.contains("arm-v7") || lower.contains("armeabi") || lower.contains("armv7") || lower == "arm" -> ARM_V7A
                lower.contains("x86_64") || lower.contains("x64") -> X86_64
                lower.contains("x86") -> X86
                else -> ALL
            }
        }

        fun isMatch(targetArchId: String, assetArchString: String): Boolean {
            val targetLower = targetArchId.lowercase().trim()
            val assetLower = assetArchString.lowercase().trim()

            if (targetLower == "all" || targetLower == "universal") return true
            if (assetLower == "all" || assetLower == "universal") return true

            val targetArch = fromString(targetArchId)
            val assetArch = fromString(assetArchString)

            return targetArch == assetArch
        }
    }
}
