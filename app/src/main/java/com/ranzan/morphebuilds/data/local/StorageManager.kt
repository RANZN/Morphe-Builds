package com.ranzan.morphebuilds.data.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.CleanableFile
import java.io.File

class StorageManager(private val context: Context) {

    /**
     * Safe app-specific download folder that is guaranteed to be writable on all Android versions
     * without needing runtime storage permissions.
     */
    fun getDefaultDownloadDir(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val morpheDir = File(baseDir, "MorpheBuilds")
        if (!morpheDir.exists()) {
            morpheDir.mkdirs()
        }
        return morpheDir
    }

    fun scanFiles(folderUriString: String?): List<CleanableFile> {
        val result = mutableListOf<CleanableFile>()
        val seenNames = mutableSetOf<String>()

        // 1. Scan user-selected SAF folder if configured
        if (!folderUriString.isNullOrBlank()) {
            try {
                val treeUri = folderUriString.toUri()
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.isDirectory) {
                    docDir.listFiles().forEach { doc ->
                        val name = doc.name ?: return@forEach
                        if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
                            var size = doc.length()
                            if (size <= 0L) {
                                size = queryFileSize(doc.uri)
                            }
                            // Clean up zero-byte orphan files from failed downloads
                            if (size <= 0L) {
                                try {
                                    doc.delete()
                                } catch (_: Exception) {}
                                return@forEach
                            }
                            seenNames.add(name.lowercase())
                            result.add(
                                parseToCleanable(
                                    name = name,
                                    uriString = doc.uri.toString(),
                                    size = size,
                                    lastModified = doc.lastModified()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Scan app-specific downloads folder
        val defaultDir = getDefaultDownloadDir()
        defaultDir.listFiles()?.forEach { file ->
            val name = file.name
            if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
                // Clean up zero-byte orphan files
                if (file.length() <= 0L) {
                    try {
                        file.delete()
                    } catch (_: Exception) {}
                    return@forEach
                }

                if (!seenNames.contains(name.lowercase())) {
                    seenNames.add(name.lowercase())
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    result.add(
                        parseToCleanable(
                            name = name,
                            uriString = fileUri.toString(),
                            size = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }

        return result
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx != -1) cursor.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseToCleanable(name: String, uriString: String, size: Long, lastModified: Long): CleanableFile {
        val lower = name.lowercase()
        val isModule = lower.endsWith(".zip") || lower.contains("-module")
        val appType = AppType.detectFromFileName(name)
        val versionRegex = Regex("v?(\\d+(?:\\.\\d+)+(?:-[a-zA-Z0-9.]+)*)")
        val version = versionRegex.find(name)?.value ?: "unknown"

        return CleanableFile(
            name = name,
            uriString = uriString,
            sizeBytes = size,
            appType = appType,
            version = version,
            isModule = isModule,
            lastModified = lastModified,
            isOldVersion = false
        )
    }

    fun deleteFile(uriString: String): Boolean {
        return try {
            val uri = uriString.toUri()
            when {
                uri.scheme == "content" -> {
                    if (uri.authority == "${context.packageName}.fileprovider") {
                        val fileName = uri.lastPathSegment ?: ""
                        val file = File(getDefaultDownloadDir(), fileName)
                        if (file.exists()) {
                            file.delete()
                        } else {
                            try {
                                context.contentResolver.delete(uri, null, null) > 0
                            } catch (_: Exception) {
                                false
                            }
                        }
                    } else {
                        val docFile = DocumentFile.fromSingleUri(context, uri)
                        if (docFile != null && docFile.exists()) {
                            docFile.delete()
                        } else {
                            try {
                                context.contentResolver.delete(uri, null, null) > 0
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }
                }
                uri.scheme == "file" -> {
                    val path = uri.path ?: return false
                    File(path).delete()
                }
                else -> {
                    File(uriString).delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
