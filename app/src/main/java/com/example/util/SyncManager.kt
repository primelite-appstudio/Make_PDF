package com.example.util

import android.content.Context
import com.example.data.model.LocalFolderSyncConfig
import com.example.data.model.WebDavConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object SyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val PREFS_NAME = "pdf_converter_sync_prefs"
    private const val KEY_WEBDAV_URL = "webdav_url"
    private const val KEY_WEBDAV_USER = "webdav_user"
    private const val KEY_WEBDAV_AUTH = "webdav_auth"
    private const val KEY_WEBDAV_PATH = "webdav_path"
    private const val KEY_WEBDAV_ENABLED = "webdav_enabled"

    private const val KEY_LOCAL_FOLDER = "local_folder_path"
    private const val KEY_LOCAL_ENABLED = "local_enabled"

    fun getWebDavConfig(context: Context): WebDavConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return WebDavConfig(
            serverUrl = prefs.getString(KEY_WEBDAV_URL, "") ?: "",
            username = prefs.getString(KEY_WEBDAV_USER, "") ?: "",
            authKey = prefs.getString(KEY_WEBDAV_AUTH, "") ?: "",
            remotePath = prefs.getString(KEY_WEBDAV_PATH, "/PDFConverter/Backup/") ?: "/PDFConverter/Backup/",
            isEnabled = prefs.getBoolean(KEY_WEBDAV_ENABLED, false)
        )
    }

    fun saveWebDavConfig(context: Context, config: WebDavConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_WEBDAV_URL, config.serverUrl)
            .putString(KEY_WEBDAV_USER, config.username)
            .putString(KEY_WEBDAV_AUTH, config.authKey)
            .putString(KEY_WEBDAV_PATH, config.remotePath)
            .putBoolean(KEY_WEBDAV_ENABLED, config.isEnabled)
            .apply()
    }

    fun getLocalSyncConfig(context: Context): LocalFolderSyncConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultDir = File(context.getExternalFilesDir(null), "AutoExportSync").absolutePath
        return LocalFolderSyncConfig(
            customFolderPath = prefs.getString(KEY_LOCAL_FOLDER, defaultDir) ?: defaultDir,
            isAutoExportEnabled = prefs.getBoolean(KEY_LOCAL_ENABLED, true)
        )
    }

    fun saveLocalSyncConfig(context: Context, config: LocalFolderSyncConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LOCAL_FOLDER, config.customFolderPath)
            .putBoolean(KEY_LOCAL_ENABLED, config.isAutoExportEnabled)
            .apply()
    }

    suspend fun syncDocument(context: Context, file: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext Pair(false, "File does not exist")

        var statusLog = ""
        var successCount = 0

        // 1. Local Auto Export Sync
        val localConfig = getLocalSyncConfig(context)
        if (localConfig.isAutoExportEnabled) {
            try {
                val targetDir = File(localConfig.customFolderPath)
                if (!targetDir.exists()) targetDir.mkdirs()
                val destFile = File(targetDir, file.name)
                file.copyTo(destFile, overwrite = true)
                statusLog += "✓ Saved local backup to ${destFile.name}\n"
                successCount++
            } catch (e: Exception) {
                statusLog += "✗ Local export failed: ${e.message}\n"
            }
        }

        // 2. WebDAV Cloud Sync
        val webDav = getWebDavConfig(context)
        if (webDav.isEnabled && webDav.serverUrl.isNotBlank()) {
            val webDavResult = uploadToWebDav(file, webDav)
            if (webDavResult.first) {
                statusLog += "✓ WebDAV synced: ${file.name}\n"
                successCount++
            } else {
                statusLog += "✗ WebDAV failed: ${webDavResult.second}\n"
            }
        }

        val overallSuccess = successCount > 0 || (!localConfig.isAutoExportEnabled && !webDav.isEnabled)
        Pair(overallSuccess, statusLog.ifBlank { "Sync skipped (no active sync destinations configured)" })
    }

    suspend fun testWebDavConnection(config: WebDavConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.serverUrl.isBlank()) {
            return@withContext Pair(false, "WebDAV Server URL cannot be empty")
        }

        try {
            val url = config.serverUrl.trimEnd('/') + "/"
            val requestBuilder = Request.Builder()
                .url(url)
                .method("PROPFIND", null)

            if (config.username.isNotBlank() || config.authKey.isNotBlank()) {
                val credential = Credentials.basic(config.username, config.authKey)
                requestBuilder.header("Authorization", credential)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful || response.code == 207 || response.code == 405) {
                Pair(true, "WebDAV connection successful! (HTTP ${response.code})")
            } else {
                Pair(false, "Server responded with HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message ?: "Failed to reach WebDAV server"}")
        }
    }

    private fun uploadToWebDav(file: File, config: WebDavConfig): Pair<Boolean, String> {
        return try {
            val baseUrl = config.serverUrl.trimEnd('/')
            val subPath = config.remotePath.trim('/')
            val fullUrl = "$baseUrl/$subPath/${file.name}"

            val mediaType = when {
                file.extension.equals("pdf", true) -> "application/pdf"
                file.extension.equals("txt", true) -> "text/plain"
                else -> "application/octet-stream"
            }.toMediaTypeOrNull()

            val requestBody = file.asRequestBody(mediaType)
            val requestBuilder = Request.Builder()
                .url(fullUrl)
                .put(requestBody)

            if (config.username.isNotBlank() || config.authKey.isNotBlank()) {
                val credential = Credentials.basic(config.username, config.authKey)
                requestBuilder.header("Authorization", credential)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful || response.code == 201 || response.code == 204) {
                Pair(true, "Uploaded")
            } else {
                Pair(false, "HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Upload exception")
        }
    }
}
