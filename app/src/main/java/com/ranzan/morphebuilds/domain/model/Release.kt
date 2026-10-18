package com.ranzan.morphebuilds.domain.model

data class Release(
    val tag: String,
    val name: String,
    val publishedAt: String,
    val htmlUrl: String,
    val body: String,
    val builds: List<AppBuild>
)
