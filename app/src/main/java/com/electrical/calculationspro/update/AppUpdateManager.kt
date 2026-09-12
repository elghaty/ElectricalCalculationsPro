package com.electrical.calculationspro.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

class AppUpdateManager(
    private val context: Context
) {

    companion object {

        private const val API_URL =
            "https://api.github.com/repos/elghaty/ElectricalCalculationsPro/releases/latest"

        private const val APK_NAME =
            "ElectricalCalculationsPro-release.apk"
    }

    suspend fun checkForUpdate(): AppReleaseInfo? =
        withContext(Dispatchers.IO) {

            try {

                val connection =
                    URL(API_URL)
                        .openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    "ElectricalCalculationsPro"
                )

                if (
                    connection.responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {
                    connection.disconnect()
                    return@withContext null
                }

                val json =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                connection.disconnect()

                val release =
                    JSONObject(json)

                val tag =
                    release.optString("tag_name")

                val versionCode =
                    tag.substringAfterLast(".")
                        .toIntOrNull()
                        ?: return@withContext null

                val versionName =
                    tag.removePrefix("v")

                val assets =
                    release.optJSONArray("assets")
                        ?: return@withContext null

                var downloadUrl: String? = null

                for (index in 0 until assets.length()) {

                    val asset =
                        assets.getJSONObject(index)

                    val name =
                        asset.optString("name")

                    if (name == APK_NAME) {

                        downloadUrl =
                            asset.optString(
                                "browser_download_url"
                            )

                        break
                    }
                }

                if (downloadUrl.isNullOrBlank()) {
                    return@withContext null
                }

                val currentVersionCode =
                    getCurrentVersionCode()

                if (versionCode <= currentVersionCode) {
                    return@withContext null
                }

                AppReleaseInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    downloadUrl = downloadUrl,
                    releaseNotes =
                        release.optString(
                            "body",
                            ""
                        )
                )

            } catch (_: Exception) {

                null
            }
        }

    private fun getCurrentVersionCode(): Int {

        return try {

            val packageInfo =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

        } catch (_: Exception) {

            1
        }
    }

    fun downloadAndInstall(
        release: AppReleaseInfo
    ) {

        val request =
            DownloadManager.Request(
                Uri.parse(release.downloadUrl)
            )

        request.setTitle(
            "Electrical Calculations Pro"
        )

        request.setDescription(
            "Downloading update ${release.versionName}"
        )

        request.setMimeType(
            "application/vnd.android.package-archive"
        )

        request.setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )

        request.setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            APK_NAME
        )

        val downloadManager =
            context.getSystemService(
                Context.DOWNLOAD_SERVICE
            ) as DownloadManager

        val downloadId =
            downloadManager.enqueue(request)

        Thread {

            var finished = false

            while (!finished) {

                Thread.sleep(1000)

                val query =
                    DownloadManager.Query()
                        .setFilterById(downloadId)

                val cursor =
                    downloadManager.query(query)

                cursor.use {

                    if (!it.moveToFirst()) {
                        return@Thread
                    }

                    val status =
                        it.getInt(
                            it.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_STATUS
                            )
                        )

                    when (status) {

                        DownloadManager.STATUS_SUCCESSFUL -> {

                            finished = true

                            installDownloadedApk(
                                downloadManager,
                                downloadId
                            )
                        }

                        DownloadManager.STATUS_FAILED -> {

                            finished = true
                        }
                    }
                }
            }

        }.start()
    }

    private fun installDownloadedApk(
        downloadManager: DownloadManager,
        downloadId: Long
    ) {

        try {

            val downloadedUri =
                downloadManager.getUriForDownloadedFile(
                    downloadId
                )
                    ?: return

            val installIntent =
                Intent(
                    Intent.ACTION_VIEW
                )

            installIntent.setDataAndType(
                downloadedUri,
                "application/vnd.android.package-archive"
            )

            installIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            installIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            context.startActivity(
                installIntent
            )

        } catch (_: Exception) {

            try {

                val file =
                    context.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS
                    )?.resolve(APK_NAME)
                        ?: return

                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                val intent =
                    Intent(
                        Intent.ACTION_VIEW
                    )

                intent.setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
                )

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                context.startActivity(intent)

            } catch (_: Exception) {
            }
        }
    }

    fun canInstallPackages(): Boolean {

        return if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O
        ) {

            context.packageManager.canRequestPackageInstalls()

        } else {

            true
        }
    }

    fun openInstallPermissionSettings() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O
        ) {

            val intent =
                Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(intent)
        }
    }
}
