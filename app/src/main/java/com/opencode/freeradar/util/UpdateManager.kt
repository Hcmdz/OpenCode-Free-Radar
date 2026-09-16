/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface UpdateCheckStore {
    suspend fun lastCheck(): Long
    suspend fun record(timestamp: Long)
}

class UpdateManager(
    private val client: HttpClient,
    private val checkStore: UpdateCheckStore,
) {

    data class UpdateInfo(
        val versionTag: String,
        val versionName: String,
        val downloadUrl: String,
        val fileName: String,
        val fileSize: Long,
        val releaseNotes: String,
        val sha256: String? = null,
    )

    sealed class UpdateResult {
        data class Found(val info: UpdateInfo) : UpdateResult()
        data object UpToDate : UpdateResult()
        data object Error : UpdateResult()
    }

    @Volatile
    var downloadJob: Job? = null

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    @Serializable
    private data class ReleaseAsset(
        val name: String = "",
        @SerialName("browser_download_url") val downloadUrl: String = "",
        val size: Long = 0L,
    )

    @Serializable
    private data class ReleaseDto(
        @SerialName("tag_name") val tagName: String = "",
        val body: String = "",
        val assets: List<ReleaseAsset> = emptyList(),
    )

    suspend fun shouldAutoCheck(): Boolean {
        val lastCheck = checkStore.lastCheck()
        return System.currentTimeMillis() - lastCheck > CHECK_INTERVAL_MS
    }

    suspend fun recordCheck() {
        checkStore.record(System.currentTimeMillis())
    }

    suspend fun checkForUpdate(currentVersionName: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val release: ReleaseDto = client.get(API_URL) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.body()
            val remoteVersion = release.tagName.removePrefix("v")
            if (!isVersionNewer(currentVersionName, remoteVersion)) return@withContext UpdateResult.UpToDate
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext UpdateResult.UpToDate
            UpdateResult.Found(
                UpdateInfo(
                    versionTag = release.tagName,
                    versionName = remoteVersion,
                    downloadUrl = apk.downloadUrl,
                    fileName = apk.name,
                    fileSize = apk.size,
                    releaseNotes = release.body,
                    sha256 = SHA_PATTERN.find(release.body)?.groupValues?.get(1),
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            UpdateResult.Error
        }
    }

    suspend fun downloadApk(
        appContext: Context,
        url: String,
        fileName: String,
        expectedSha256: String? = null,
        onProgress: (Float) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        if (!isAllowedDownloadUrl(url)) return@withContext null
        try {
            val updateDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
            updateDir.listFiles()?.forEach { it.delete() }
            val apkFile = File(updateDir, fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))
            val digest = MessageDigest.getInstance("SHA-256")
            var totalSize = -1L
            client.prepareGet(url).execute { response ->
                totalSize = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                val channel: ByteReadChannel = response.body()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead == -1) break
                    apkFile.appendBytes(buffer.copyOf(bytesRead))
                    digest.update(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalSize > 0) {
                        val progress = (downloaded.toFloat() / totalSize).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                }
            }
            if (totalSize > 0 && apkFile.length() != totalSize) {
                apkFile.delete()
                return@withContext null
            }
            if (expectedSha256 != null) {
                val computed = digest.digest().joinToString("") { "%02x".format(it) }
                if (!computed.equals(expectedSha256, ignoreCase = true)) {
                    apkFile.delete()
                    return@withContext null
                }
            }
            withContext(Dispatchers.Main) { onProgress(1f) }
            apkFile
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri()
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            )
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    internal fun isAllowedDownloadUrl(url: String): Boolean {
        if (!url.lowercase().startsWith("https://")) return false
        val host = runCatching { java.net.URI(url).host }.getOrNull()?.lowercase() ?: return false
        if (host in ALLOWED_DOWNLOAD_HOSTS) return true
        return ALLOWED_DOWNLOAD_SUFFIXES.any { host.endsWith(it) }
    }

    fun isVersionNewer(current: String, remote: String): Boolean {
        val curParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val remParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(curParts.size, remParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val r = remParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private companion object {
        const val API_URL = "https://api.github.com/repos/Hcmdz/OpenCode-Free-Radar/releases/latest"
        const val USER_AGENT = "OFR-Android"
        const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
        val SHA_PATTERN = Regex("SHA-256:\\s*([a-fA-F0-9]{64})")
        val ALLOWED_DOWNLOAD_HOSTS = setOf(
            "github.com",
            "api.github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )
        val ALLOWED_DOWNLOAD_SUFFIXES = listOf(".githubusercontent.com")
    }
}
