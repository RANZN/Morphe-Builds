package com.ranzan.morphebuilds.domain.repository

interface CleanupScheduler {
    fun scheduleCleanup(intervalDays: Int)
}
