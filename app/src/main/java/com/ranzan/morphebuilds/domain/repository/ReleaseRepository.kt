package com.ranzan.morphebuilds.domain.repository

import com.ranzan.morphebuilds.domain.model.Release
import kotlinx.coroutines.flow.Flow

interface ReleaseRepository {
    fun getReleases(owner: String, repo: String, token: String? = null): Flow<Result<List<Release>>>
}
