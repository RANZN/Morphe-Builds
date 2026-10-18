package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.model.AppSettings
import com.ranzan.morphebuilds.domain.model.Release
import com.ranzan.morphebuilds.domain.repository.ReleaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class GetReleasesUseCase(
    private val releaseRepository: ReleaseRepository
) {
    operator fun invoke(settings: AppSettings): Flow<Result<List<Release>>> {
        val mainPath = settings.extractRepoPath(settings.repoUrl)
            ?: return flow {
                emit(Result.failure(IllegalArgumentException("Invalid GitHub repository URL: ${settings.repoUrl}")))
            }

        val microGPath = settings.extractRepoPath(AppSettings.MICROG_REPO_URL)

        val mainFlow = releaseRepository.getReleases(
            owner = mainPath.first,
            repo = mainPath.second,
            token = settings.githubToken
        )

        // If main repo is already MicroG-RE, just return main flow
        if (microGPath == null || (mainPath.first.equals(microGPath.first, ignoreCase = true) && mainPath.second.equals(microGPath.second, ignoreCase = true))) {
            return mainFlow
        }

        val microGFlow = releaseRepository.getReleases(
            owner = microGPath.first,
            repo = microGPath.second,
            token = settings.githubToken
        )

        return combine(mainFlow, microGFlow) { mainResult, microGResult ->
            val combinedReleases = mutableListOf<Release>()
            var primaryError: Throwable? = null

            mainResult.fold(
                onSuccess = { combinedReleases.addAll(it) },
                onFailure = { primaryError = it }
            )

            microGResult.fold(
                onSuccess = { combinedReleases.addAll(it) },
                onFailure = { /* Don't fail entire screen if secondary fails */ }
            )

            if (combinedReleases.isNotEmpty()) {
                Result.success(combinedReleases)
            } else if (primaryError != null) {
                Result.failure(primaryError!!)
            } else {
                microGResult
            }
        }
    }
}
