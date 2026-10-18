package com.ranzan.morphebuilds

import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.Release
import com.ranzan.morphebuilds.domain.repository.CleanupScheduler
import com.ranzan.morphebuilds.domain.repository.ReleaseRepository
import com.ranzan.morphebuilds.domain.usecase.ScheduleCleanupUseCase
import com.ranzan.morphebuilds.domain.usecase.ValidateRepositoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanArchitectureTest {

    @Test
    fun testScheduleCleanupUseCaseDelegatesToScheduler() {
        var scheduledDays = -1
        val fakeScheduler = object : CleanupScheduler {
            override fun scheduleCleanup(intervalDays: Int) {
                scheduledDays = intervalDays
            }
        }

        val useCase = ScheduleCleanupUseCase(fakeScheduler)
        useCase(7)
        assertEquals(7, scheduledDays)

        useCase(30)
        assertEquals(30, scheduledDays)
    }

    @Test
    fun testValidateRepositoryUseCaseSuccess() = runBlocking {
        val fakeRelease = Release(
            tag = "v1.0.0",
            name = "Initial Release",
            publishedAt = "2026-01-01T00:00:00Z",
            htmlUrl = "https://github.com/test-owner/test-repo/releases/tag/v1.0.0",
            body = "Release notes",
            builds = listOf(
                AppBuild(
                    id = "v1.0.0_1",
                    appType = AppType.YOUTUBE,
                    customTitle = null,
                    version = "1.0.0",
                    isModule = false,
                    architecture = "arm64-v8a",
                    fileName = "youtube-arm64-v8a.apk",
                    downloadUrl = "https://example.com/youtube.apk",
                    sizeBytes = 50_000_000L,
                    releaseTag = "v1.0.0",
                    publishedAt = "2026-01-01"
                )
            )
        )

        val fakeRepo = object : ReleaseRepository {
            override fun getReleases(owner: String, repo: String, token: String?): Flow<Result<List<Release>>> {
                return flowOf(Result.success(listOf(fakeRelease)))
            }
        }

        val useCase = ValidateRepositoryUseCase(fakeRepo)
        val result = useCase("https://github.com/test-owner/test-repo/releases", null)

        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals("test-owner", data.owner)
        assertEquals("test-repo", data.repo)
        assertEquals(1, data.releaseCount)
        assertEquals("v1.0.0", data.latestTag)
        assertEquals(1, data.assetCount)
    }

    @Test
    fun testValidateRepositoryUseCaseInvalidUrl() = runBlocking {
        val fakeRepo = object : ReleaseRepository {
            override fun getReleases(owner: String, repo: String, token: String?): Flow<Result<List<Release>>> {
                return flowOf(Result.success(emptyList()))
            }
        }

        val useCase = ValidateRepositoryUseCase(fakeRepo)
        val result = useCase("invalid-url-without-slash", null)

        assertTrue(result.isFailure)
    }
}
