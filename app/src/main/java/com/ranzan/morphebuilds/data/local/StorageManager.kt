package com.ranzan.morphebuilds.data.local

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.ranzan.morphebuilds.domain.model.AppType
import com.ranzan.morphebuilds.domain.model.CleanableFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class DownloadDestination(
    val outputStream: OutputStream,
    val finalUri: String,
    val displayPath: String,
    val isDocumentFile: Boolean,
    val isMediaStore: Boolean = false
)

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

    /**
     * Prepares a write stream for the incoming build file.
     * 1. If [folderUriString] is provided and valid, uses Storage Access Framework (SAF) DocumentFile.
     * 2. On Android 10+ (API 29+), uses MediaStore.Downloads for direct access to Downloads/MorpheBuilds.
     * 3. Fallback to app-specific external storage which never fails with EACCES permission denied.
     */
    fun prepareDestination(fileName: String, folderUriString: String?): DownloadDestination {
        val mimeType = if (fileName.endsWith(".apk", ignoreCase = true)) {
            "application/vnd.android.package-archive"
        } else {
            "application/zip"
        }

        // Strategy 1: User-selected custom folder via SAF
        if (!folderUriString.isNullOrBlank()) {
            try {
                val treeUri = folderUriString.toUri()
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.canWrite()) {
                    val existing = docDir.findFile(fileName)
                    val targetDoc = if (existing != null && existing.canWrite()) {
                        existing
                    } else {
                        try {
                            existing?.delete()
                        } catch (_: Exception) {}
                        docDir.createFile(mimeType, fileName)
                    }

                    if (targetDoc != null) {
                        val stream = context.contentResolver.openOutputStream(targetDoc.uri, "w")
                        if (stream != null) {
                            return DownloadDestination(
                                outputStream = stream,
                                finalUri = targetDoc.uri.toString(),
                                displayPath = targetDoc.name ?: fileName,
                                isDocumentFile = true,
                                isMediaStore = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Strategy 2: Android 10+ (API 29+) MediaStore.Downloads
        // Writes directly to public Download/MorpheBuilds without needing any runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Delete existing entry if present
                try {
                    val projection = arrayOf(MediaStore.MediaColumns._ID)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                    val selectionArgs = arrayOf(fileName, "${Environment.DIRECTORY_DOWNLOADS}/MorpheBuilds%")
                    context.contentResolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val delUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                            try {
                                context.contentResolver.delete(delUri, null, null)
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MorpheBuilds")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val mediaUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (mediaUri != null) {
                    val stream = context.contentResolver.openOutputStream(mediaUri, "w")
                    if (stream != null) {
                        return DownloadDestination(
                            outputStream = stream,
                            finalUri = mediaUri.toString(),
                            displayPath = "Downloads/MorpheBuilds/$fileName",
                            isDocumentFile = false,
                            isMediaStore = true
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Strategy 3: Guaranteed safe app-specific external storage
        val targetDir = getDefaultDownloadDir()
        val targetFile = File(targetDir, fileName)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        targetFile.createNewFile()
        val fileProviderUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        return DownloadDestination(
            outputStream = FileOutputStream(targetFile),
            finalUri = fileProviderUri.toString(),
            displayPath = targetFile.name,
            isDocumentFile = false,
            isMediaStore = false
        )
    }

    /**
     * Marks the download as completed (e.g. clears IS_PENDING flag for MediaStore).
     */
    fun onDownloadComplete(destination: DownloadDestination) {
        if (destination.isMediaStore && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val uri = destination.finalUri.toUri()
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Cleans up incomplete download if failed or cancelled.
     */
    fun onDownloadFailed(destination: DownloadDestination?) {
        if (destination == null) return
        try {
            if (destination.isMediaStore) {
                context.contentResolver.delete(destination.finalUri.toUri(), null, null)
            } else if (destination.isDocumentFile) {
                val doc = DocumentFile.fromSingleUri(context, destination.finalUri.toUri())
                doc?.delete()
            }
        } catch (_: Exception) {}
    }

    fun scanFiles(folderUriString: String?): List<CleanableFile> {
        val result = mutableListOf<CleanableFile>()

        // 1. Scan SAF folder if configured
        if (!folderUriString.isNullOrBlank()) {
            try {
                val treeUri = folderUriString.toUri()
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir != null && docDir.isDirectory) {
                    docDir.listFiles().forEach { doc ->
                        val name = doc.name ?: return@forEach
                        if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
                            result.add(
                                parseToCleanable(
                                    name = name,
                                    uriString = doc.uri.toString(),
                                    size = doc.length(),
                                    lastModified = doc.lastModified()
                                )
                            )
                        }
                    }
                    return result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Scan MediaStore Downloads on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )
                val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("${Environment.DIRECTORY_DOWNLOADS}/MorpheBuilds%")
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val size = cursor.getLong(sizeCol)
                        val dateSec = cursor.getLong(dateCol)

                        if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
                            val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                            result.add(
                                parseToCleanable(
                                    name = name,
                                    uriString = uri.toString(),
                                    size = size,
                                    lastModified = dateSec * 1000
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Scan app-specific downloads
        val defaultDir = getDefaultDownloadDir()
        defaultDir.listFiles()?.forEach { file ->
            val name = file.name
            if (name.endsWith(".apk", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)) {
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
            when {
                uri.scheme == "content" -> {
                    // Try MediaStore / ContentResolver delete first
                    val deletedFromResolver = try {
                        context.contentResolver.delete(uri, null, null) > 0
                    } catch (_: Exception) {
                        false
                    }
                    if (deletedFromResolver) {
                        true
                    } else {
                        val docFile = DocumentFile.fromSingleUri(context, uri)
                        docFile?.delete() ?: false
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

    fun createInstallIntent(uriString: String): Intent {
        val uri = uriString.toUri()
        val contentUri: Uri = if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else if (uri.scheme == "content" && uri.authority?.contains("documents") == true) {
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
        } else {
            uri
        }

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
