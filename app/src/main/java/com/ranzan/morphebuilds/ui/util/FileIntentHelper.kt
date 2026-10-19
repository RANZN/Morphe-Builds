package com.ranzan.morphebuilds.ui.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileIntentHelper {

    fun createInstallIntent(context: Context, uriString: String): Intent {
        val uri = uriString.toUri()
        val fileProviderUri = resolveToFileProviderUri(context, uri, "install_temp.apk")

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileProviderUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Grant read permission explicitly to package installer targets
            grantPermissionToTargets(context, this, fileProviderUri)
        }
    }

    fun createShareIntent(context: Context, uriString: String, isModule: Boolean): Intent {
        val uri = uriString.toUri()
        val fallback = if (isModule) "module_temp.zip" else "share_temp.apk"
        val fileProviderUri = resolveToFileProviderUri(context, uri, fallback)
        val mimeType = if (isModule) "application/zip" else "application/vnd.android.package-archive"

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileProviderUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            grantPermissionToTargets(context, this, fileProviderUri)
        }
    }

    private fun resolveToFileProviderUri(context: Context, uri: Uri, fallbackName: String): Uri {
        val fileProviderAuthority = "${context.packageName}.fileprovider"

        // 1. If it's already a FileProvider URI from our app, verify the file exists and is not 0 bytes
        if (uri.scheme == "content" && uri.authority == fileProviderAuthority) {
            val file = resolveFileFromFileProviderUri(context, uri)
            if (file != null && file.exists() && file.length() == 0L) {
                throw IOException("Installer APK is empty (0 bytes). Please re-download.")
            }
            return uri
        }

        // 2. If it's a file:// scheme, resolve via FileProvider directly if the file exists and has content
        if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) {
                    if (file.length() == 0L) {
                        throw IOException("Installer APK is empty (0 bytes). Please re-download.")
                    }
                    try {
                        return FileProvider.getUriForFile(context, fileProviderAuthority, file)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 3. For external content:// URIs (e.g. SAF DocumentFile):
        // Third-party apps like PackageInstaller cannot be granted permissions directly from SAF DocumentFile
        // by our app (causes "UID does not have permission to content://" SecurityException).
        // Copy the stream safely to app external cache or internal cache and verify content.
        val displayName = queryDisplayName(context, uri) ?: fallbackName
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "shared_installers").apply { mkdirs() }
        val targetFile = File(cacheDir, safeName)

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open input file for installation: $uri")

        val tempFile = File(cacheDir, "$safeName.tmp")
        if (tempFile.exists()) tempFile.delete()

        var bytesCopied = 0L
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                bytesCopied = input.copyTo(output)
                output.flush()
                try {
                    output.fd.sync()
                } catch (_: Exception) {}
            }
        }

        if (bytesCopied <= 0L || !tempFile.exists() || tempFile.length() <= 0L) {
            tempFile.delete()
            throw IOException("Installer APK is empty (0 bytes). Please re-download.")
        }

        if (targetFile.exists()) targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        return FileProvider.getUriForFile(context, fileProviderAuthority, targetFile)
    }

    private fun resolveFileFromFileProviderUri(context: Context, uri: Uri): File? {
        val path = uri.path ?: return null
        return when {
            path.startsWith("/external_app_files/") -> {
                val rel = path.removePrefix("/external_app_files/")
                val base = context.getExternalFilesDir(null) ?: return null
                File(base, rel)
            }
            path.startsWith("/internal_cache/") -> {
                val rel = path.removePrefix("/internal_cache/")
                File(context.cacheDir, rel)
            }
            path.startsWith("/external_cache/") -> {
                val rel = path.removePrefix("/external_cache/")
                val base = context.externalCacheDir ?: context.cacheDir
                File(base, rel)
            }
            else -> null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun grantPermissionToTargets(context: Context, intent: Intent, contentUri: Uri) {
        try {
            val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo?.packageName ?: continue
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
