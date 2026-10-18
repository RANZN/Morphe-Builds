package com.ranzan.morphebuilds.domain.repository

import com.ranzan.morphebuilds.domain.model.AppType

interface PackageInspectorRepository {
    fun getInstalledVersion(packageName: String): String?
    fun findInstalledVersionForApp(appType: AppType): String?
}
