package com.ranzan.morphebuilds.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream

object FileIntentHelper {

    fun createInstallIntent(context: Context, uriString: String): Intent {
        val uri = uriString.toUri()
        val contentUri: Uri = when {
            uri.scheme == "file" -> {
                val file = File(uri.path ?: "")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            uri.scheme == "content" && uri.authority?.contains("documents") == true -> {
                // For SAF Document URIs, some OEM package installers cannot resolve them directly across processes.
                // Copy to app cache to ensure 100% reliable installation via FileProvider.
                try {
                    val cacheFile = File(context.cacheDir, "install_temp.apk")
                    if (cacheFile.exists()) cacheFile.delete()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                } catch (_: Exception) {
                    uri
                }
            }
            else -> uri
        }

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createShareIntent(context: Context, uriString: String, isModule: Boolean): Intent {
        val uri = uriString.toUri()
        val contentUri: Uri = if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            uri
        }

        val mimeType = if (isModule) "application/zip" else "application/vnd.android.package-archive"
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
