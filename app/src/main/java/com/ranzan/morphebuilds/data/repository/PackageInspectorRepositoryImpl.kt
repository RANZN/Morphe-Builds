package com.ranzan.morphebuilds.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.repository.PackageInspectorRepository

class PackageInspectorRepositoryImpl(
    private val context: Context
) : PackageInspectorRepository {

    override fun getInstalledVersion(packageName: String): String? {
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    override fun findInstalledVersionForApp(appType: AppType): String? {
        for (pkg in appType.knownPackageNames) {
            val version = getInstalledVersion(pkg)
            if (!version.isNullOrBlank()) {
                return version
            }
        }
        return null
    }
}
