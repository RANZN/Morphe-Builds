package com.ranzan.morphebuilds.domain.model

data class CleanableFile(
    val name: String,
    val uriString: String,
    val sizeBytes: Long,
    val appType: AppType,
    val version: String,
    val isModule: Boolean,
    val lastModified: Long,
    val isOldVersion: Boolean
)
