package com.ranzan.morphebuilds.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.ranzan.morphebuilds.data.remote.ReleaseParser
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.CleanableFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import androidx.core.net.toUri

data class DownloadDestination(
    val outputStream: OutputStream,
    val finalUri: String,
    val displayPath: String,
    val isDocumentFile: Boolean
)

class StorageManager(private val context: Context) {

    fun getDefaultDownloadDir(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val morpheDir = File(publicDownloads, "MorpheBuilds")
        if (!morpheDir.exists()) {
            morpheDir.mkdirs()
        }
        return morpheDir
    }

    fun prepareDestination(fileName: String, folderUriString: String?): DownloadDestination {
        if (!folderUriString.isNullOrBlank()) {
            try {
                val treeUri = folderUriString.toUri()
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.canWrite()) {
                    // Check if file with same name already exists, delete it first to overwrite cleanly
                    val existing = docDir.findFile(fileName)
                    existing?.delete()

                    val mimeType = if (fileName.endsWith(".apk", ignoreCase = true)) {
                        "application/vnd.android.package-archive"
                    } else {
                        "application/zip"
                    }

                    val newFile = docDir.createFile(mimeType, fileName)
                        ?: throw IllegalStateException("Could not create file $fileName in selected directory")

                    val stream = context.contentResolver.openOutputStream(newFile.uri, "wt")
                        ?: throw IllegalStateException("Could not open output stream for ${newFile.uri}")

                    return DownloadDestination(
                        outputStream = stream,
                        finalUri = newFile.uri.toString(),
                        displayPath = newFile.name ?: fileName,
                        isDocumentFile = true
                    )
                }
            } catch (e: Exception) {
                // If SAF fails, fall back to default directory
                e.printStackTrace()
            }
        }

        // Fallback to default MorpheBuilds directory
        val targetDir = getDefaultDownloadDir()
        val targetFile = File(targetDir, fileName)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        targetFile.createNewFile()

        return DownloadDestination(
            outputStream = FileOutputStream(targetFile),
            finalUri = Uri.fromFile(targetFile).toString(),
            displayPath = targetFile.absolutePath,
            isDocumentFile = false
        )
    }

    fun scanFiles(folderUriString: String?): List<CleanableFile> {
        val result = mutableListOf<CleanableFile>()

        if (!folderUriString.isNullOrBlank()) {
            try {
                val treeUri = folderUriString.toUri()
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.isDirectory) {
                    docDir.listFiles().forEach { doc ->
                        val name = doc.name ?: return@forEach
                        if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
                            val cleanable = parseToCleanable(
                                name = name,
                                uriString = doc.uri.toString(),
                                size = doc.length(),
                                lastModified = doc.lastModified()
                            )
                            result.add(cleanable)
                        }
                    }
                    return result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Check fallback default directory
        val defaultDir = getDefaultDownloadDir()
        defaultDir.listFiles()?.forEach { file ->
            val name = file.name
            if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
                val cleanable = parseToCleanable(
                    name = name,
                    uriString = Uri.fromFile(file).toString(),
                    size = file.length(),
                    lastModified = file.lastModified()
                )
                result.add(cleanable)
            }
        }

        return result
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
            when (uri.scheme) {
                "content" -> {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    docFile?.delete() ?: (context.contentResolver.delete(uri, null, null) > 0)
                }
                "file" -> {
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

    fun createInstallIntent(uriString: String): Intent {
        val uri = uriString.toUri()
        val contentUri: Uri = if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            uri
        }

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    fun createOpenOrShareIntent(uriString: String, isModule: Boolean): Intent {
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}
