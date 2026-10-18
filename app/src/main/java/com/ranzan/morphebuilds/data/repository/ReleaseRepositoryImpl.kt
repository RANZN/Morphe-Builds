package com.ranzan.morphebuilds.data.repository

import com.ranzan.morphebuilds.data.remote.GitHubApiService
import com.ranzan.morphebuilds.data.remote.ReleaseParser
import com.ranzan.morphebuilds.domain.model.Release
import com.ranzan.morphebuilds.domain.repository.ReleaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ReleaseRepositoryImpl(
    private val apiService: GitHubApiService
) : ReleaseRepository {

    override fun getReleases(
        owner: String,
        repo: String,
        token: String?
    ): Flow<Result<List<Release>>> = flow {
        try {
            val dtoList = apiService.getReleases(owner, repo, token)
            val releases = dtoList.map { ReleaseParser.toDomainRelease(it) }
            emit(Result.success(releases))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
