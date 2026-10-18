package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.first

data class RepositoryValidationResult(
    val owner: String,
    val repo: String,
    val releaseCount: Int,
    val latestTag: String?,
    val assetCount: Int
)

class ValidateRepositoryUseCase(
    private val releaseRepository: ReleaseRepository
) {
    suspend operator fun invoke(url: String, token: String?): Result<RepositoryValidationResult> {
        val dummySettings = AppSettings()
        val path = dummySettings.extractRepoPath(url)
            ?: return Result.failure(IllegalArgumentException("Invalid GitHub repository URL: $url"))

        val flowResult = releaseRepository.getReleases(
            owner = path.first,
            repo = path.second,
            token = token
        ).first()

        return flowResult.map { releases ->
            val latest = releases.firstOrNull()
            RepositoryValidationResult(
                owner = path.first,
                repo = path.second,
                releaseCount = releases.size,
                latestTag = latest?.tag,
                assetCount = latest?.builds?.size ?: 0
            )
        }
    }
}
