package com.ranzan.morphebuilds.data.local

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.DownloadItem
import com.ranzan.morphebuilds.domain.model.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Downloads build assets using Android's system DownloadManager service.
 * Handles system notification tray updates, network reconnection, and robust background downloading.
 */
class AndroidDownloadManagerDownloader(
    private val context: Context,
    private val storageManager: StorageManager
) : BuildDownloader {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val activeDownloads = ConcurrentHashMap<String, Long>()

    override fun cancel(assetId: String) {
        val downloadId = activeDownloads.remove(assetId)
        if (downloadId != null) {
            try {
                downloadManager.remove(downloadId)
            } catch (_: Exception) {}
        }
    }

    override fun download(build: AppBuild, folderUri: String?): Flow<DownloadItem> = flow {
        var currentItem = DownloadItem(
            assetId = build.id,
            fileName = build.fileName,
            appType = build.appType,
            version = build.version,
            isModule = build.isModule,
            state = DownloadState.CONNECTING,
            totalBytes = build.sizeBytes
        )
        emit(currentItem)

        val targetDir = storageManager.getDefaultDownloadDir()
        val targetFile = File(targetDir, build.fileName)
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val mimeType = if (build.isModule) {
            "application/zip"
        } else {
            "application/vnd.android.package-archive"
        }

        var downloadId: Long? = null

        try {
            val request = DownloadManager.Request(build.downloadUrl.toUri()).apply {
                setTitle(build.displayName)
                setDescription("Downloading ${build.fileName}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType(mimeType)
                addRequestHeader("User-Agent", "MorpheBuilds-Android-App")
                setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    "MorpheBuilds/${build.fileName}"
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val id = downloadManager.enqueue(request)
            downloadId = id
            activeDownloads[build.id] = id

            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            currentItem = currentItem.copy(
                destinationUri = fileProviderUri.toString(),
                destinationPath = targetFile.name,
                state = DownloadState.DOWNLOADING
            )
            emit(currentItem)

            var lastDownloadedBytes = 0L
            var lastProgressTimeMs = System.currentTimeMillis()
            var smoothedSpeed = 0L
            val speedHistory = ArrayDeque<Pair<Long, Long>>() // (timestampMs, bytesDownloaded)
            var isFinished = false

            while (!isFinished && currentCoroutineContext().isActive) {
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)

                if (cursor == null || !cursor.moveToFirst()) {
                    cursor?.close()
                    throw CancellationException("Download removed or cancelled")
                }

                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val reasonCol = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                val status = if (statusCol != -1) cursor.getInt(statusCol) else DownloadManager.STATUS_FAILED
                val downloadedBytes = if (bytesCol != -1) cursor.getLong(bytesCol) else 0L
                val totalBytes = if (totalCol != -1 && cursor.getLong(totalCol) > 0) cursor.getLong(totalCol) else build.sizeBytes
                val reason = if (reasonCol != -1) cursor.getInt(reasonCol) else 0

                cursor.close()

                val now = System.currentTimeMillis()

                if (downloadedBytes > lastDownloadedBytes) {
                    lastDownloadedBytes = downloadedBytes
                    lastProgressTimeMs = now
                }

                // Append sample and maintain sliding window of 2500ms
                speedHistory.addLast(now to downloadedBytes)
                while (speedHistory.isNotEmpty() && (now - speedHistory.first().first > 2500L)) {
                    speedHistory.removeFirst()
                }

                val oldestSample = speedHistory.firstOrNull()
                if (oldestSample != null) {
                    val windowTimeSpan = now - oldestSample.first
                    val windowBytesSpan = downloadedBytes - oldestSample.second

                    if (windowTimeSpan >= 500L && windowBytesSpan > 0L) {
                        val instantSpeed = (windowBytesSpan * 1000L) / windowTimeSpan
                        smoothedSpeed = if (smoothedSpeed == 0L) {
                            instantSpeed
                        } else {
                            (smoothedSpeed * 7 + instantSpeed * 3) / 10
                        }
                    }
                }

                // If no progress for > 1.5s, decay speed; after 3.5s zero out
                val idleTime = now - lastProgressTimeMs
                if (idleTime > 3500L) {
                    smoothedSpeed = 0L
                } else if (idleTime > 1500L && smoothedSpeed > 0L) {
                    smoothedSpeed = (smoothedSpeed * 7) / 10
                }

                val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

                when (status) {
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> {
                        smoothedSpeed = 0L
                        speedHistory.clear()
                        currentItem = currentItem.copy(
                            state = DownloadState.CONNECTING,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speedBytesPerSec = 0L
                        )
                        emit(currentItem)
                    }

                    DownloadManager.STATUS_RUNNING -> {
                        currentItem = currentItem.copy(
                            state = DownloadState.DOWNLOADING,
                            progressPercent = percent.coerceIn(0, 100),
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speedBytesPerSec = smoothedSpeed
                        )
                        emit(currentItem)
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        isFinished = true
                        activeDownloads.remove(build.id)

                        // Verify that the file physically exists and has data
                        val finalSize = if (targetFile.exists()) targetFile.length() else downloadedBytes
                        if (finalSize <= 0L) {
                            throw IOException("Download completed but downloaded file is empty (0 bytes)")
                        }

                        // If user configured a custom SAF folder, copy the completed file to that folder
                        if (!folderUri.isNullOrBlank()) {
                            copyToSafFolder(targetFile, build.fileName, mimeType, folderUri)
                        }

                        emit(
                            currentItem.copy(
                                state = DownloadState.COMPLETED,
                                progressPercent = 100,
                                downloadedBytes = finalSize,
                                totalBytes = finalSize,
                                speedBytesPerSec = 0
                            )
                        )
                    }

                    DownloadManager.STATUS_FAILED -> {
                        isFinished = true
                        activeDownloads.remove(build.id)
                        val errorMsg = mapReasonToMessage(reason)
                        throw IOException(errorMsg)
                    }
                }

                if (!isFinished) {
                    delay(400.milliseconds)
                }
            }
        } catch (e: Exception) {
            activeDownloads.remove(build.id)
            if (downloadId != null) {
                try {
                    downloadManager.remove(downloadId)
                } catch (_: Exception) {}
            }

            if (targetFile.exists() && targetFile.length() <= 0L) {
                targetFile.delete()
            }

            if (e is CancellationException) {
                emit(
                    currentItem.copy(
                        state = DownloadState.CANCELLED,
                        errorMessage = "Download cancelled by user"
                    )
                )
            } else {
                emit(
                    currentItem.copy(
                        state = DownloadState.FAILED,
                        errorMessage = e.localizedMessage ?: "Download failed"
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun copyToSafFolder(sourceFile: File, fileName: String, mimeType: String, folderUriString: String) {
        try {
            val treeUri = folderUriString.toUri()
            val docDir = DocumentFile.fromTreeUri(context, treeUri) ?: return
            if (!docDir.canWrite()) return

            val existing = docDir.findFile(fileName)
            if (existing != null) {
                try {
                    existing.delete()
                } catch (_: Exception) {}
            }

            val targetDoc = docDir.createFile(mimeType, fileName) ?: return
            context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapReasonToMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Download cannot be resumed"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage device not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Destination file already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage file system error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data transfer error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space on device"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects from server"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server returned an unhandled HTTP code"
            else -> "Download failed (code: $reason)"
        }
    }
}
