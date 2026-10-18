package com.ranzan.morphebuilds.data.local

import com.ranzan.morphebuilds.domain.model.AppBuild
import com.ranzan.morphebuilds.domain.model.DownloadItem
import com.ranzan.morphebuilds.domain.model.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FileDownloader(
    private val storageManager: StorageManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val runningCalls = ConcurrentHashMap<String, okhttp3.Call>()

    fun cancel(assetId: String) {
        runningCalls[assetId]?.cancel()
        runningCalls.remove(assetId)
    }

    fun download(build: AppBuild, folderUri: String?): Flow<DownloadItem> = flow {
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

        var destination: DownloadDestination? = null

        try {
            destination = storageManager.prepareDestination(build.fileName, folderUri)

            val request = Request.Builder()
                .url(build.downloadUrl)
                .header("User-Agent", "MorpheBuilds-Android-App")
                .build()

            val call = client.newCall(request)
            runningCalls[build.id] = call

            val response = call.execute()
            if (!response.isSuccessful) {
                response.close()
                throw IOException("Server returned HTTP ${response.code}: ${response.message}")
            }

            val responseBody = response.body ?: throw IOException("Empty response body from download server")
            val totalBytes = if (responseBody.contentLength() > 0) responseBody.contentLength() else build.sizeBytes

            currentItem = currentItem.copy(
                state = DownloadState.DOWNLOADING,
                totalBytes = totalBytes,
                destinationUri = destination.finalUri,
                destinationPath = destination.displayPath
            )
            emit(currentItem)

            val inputStream: InputStream = responseBody.byteStream()
            val outputStream = destination.outputStream
            val buffer = ByteArray(8 * 1024)
            var bytesCopied = 0L
            var lastUpdateMs = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            var speed = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        if (!currentCoroutineContext().isActive) {
                            throw CancellationException("Download cancelled")
                        }

                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        bytesSinceLastUpdate += bytes

                        val now = System.currentTimeMillis()
                        val diffMs = now - lastUpdateMs
                        if (diffMs >= 500) {
                            speed = (bytesSinceLastUpdate * 1000) / diffMs
                            lastUpdateMs = now
                            bytesSinceLastUpdate = 0L

                            val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                            currentItem = currentItem.copy(
                                progressPercent = percent.coerceIn(0, 100),
                                downloadedBytes = bytesCopied,
                                speedBytesPerSec = speed
                            )
                            emit(currentItem)
                        }

                        bytes = input.read(buffer)
                    }
                    output.flush()
                }
            }

            response.close()
            runningCalls.remove(build.id)

            emit(
                currentItem.copy(
                    state = DownloadState.COMPLETED,
                    progressPercent = 100,
                    downloadedBytes = bytesCopied,
                    speedBytesPerSec = 0
                )
            )
        } catch (e: Exception) {
            runningCalls.remove(build.id)
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
}
