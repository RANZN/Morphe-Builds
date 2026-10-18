package com.ranzan.morphebuilds.domain.usecase

import com.ranzan.morphebuilds.domain.repository.CleanupScheduler

class ScheduleCleanupUseCase(
    private val cleanupScheduler: CleanupScheduler
) {
    operator fun invoke(intervalDays: Int) {
        cleanupScheduler.scheduleCleanup(intervalDays)
    }
}
