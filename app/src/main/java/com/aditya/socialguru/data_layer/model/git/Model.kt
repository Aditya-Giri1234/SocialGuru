package com.aditya.socialguru.data_layer.model.git

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.aditya.socialguru.BuildConfig
import com.aditya.socialguru.R
import com.aditya.socialguru.domain_layer.helper.Constants.ApkFolderName
import com.aditya.socialguru.domain_layer.helper.toFileSize
import com.aditya.socialguru.domain_layer.service.SharePref
import com.google.gson.annotations.SerializedName
import com.vanniktech.ui.Parcelize
import io.github.g00fy2.versioncompare.Version
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Locale


class GitHubRelease(
    @SerializedName("name")
    val name: String,
    @SerializedName("tag_name")
    val tag: String,
    @SerializedName("html_url")
    val url: String,
    @SerializedName("published_at")
    val date: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("prerelease")
    val isPrerelease: Boolean,
    @SerializedName("assets")
    val downloads: List<ReleaseAsset>
) : Serializable {

    companion object {
        private const val IGNORED_RELEASE = "ignored_release"
    }

    suspend fun isDownloadable(context: Context): Boolean {
        val hasApk = downloads.any { it.isApk }
        if (hasApk) {
//            return isNewer(context) && isUpdateDialogCanShow(context).first()
            return isNewer(context)
        }
        return false
    }
    private suspend fun isUpdateDialogCanShow(context: Context): Flow<Boolean> = flow {
        // Get the last time the dialog was shown
        val lastTimeShown = SharePref(context).getPrefLong(SharePref.PreferencesKeys.LAST_TIME_UPDATE_SHOW_DIALOG).first()

        // Get the current time in milliseconds
        val currentTimeMillis = System.currentTimeMillis()

        // Calculate the time difference
        val timeDiff = currentTimeMillis - lastTimeShown

        // Check if one hour (3600000 milliseconds) has passed
        emit(timeDiff > 3600000) // Emit true if more than an hour has passed
    }



    fun isNewer(context: Context): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installedVersionName = packageInfo.versionName ?: return true
            var updateVersionName = this.tag
            if (updateVersionName.startsWith("v", ignoreCase = true)) {
                updateVersionName = updateVersionName.substring(1)
            }
            return Version(updateVersionName) > Version(installedVersionName)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return true // assume true
    }

    fun getDownloadSize(): String? {
        val assetSize = downloads.firstOrNull { it.isApk }
        return assetSize?.size?.toFileSize()
    }

/*    fun getDownloadRequest(context: Context): DownloadManager.Request? {
        val apkAsset = downloads.firstOrNull { it.isApk }
        if (apkAsset != null) {
            val apkFileName = apkAsset.name
            val downloadUri = apkAsset.downloadUrl.toUri()
            val downloadUriFile = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkFileName).toUri()
            }else{
                val folderName = File(context.filesDir,ApkFolderName)
                if(!folderName.exists()){
                    folderName.mkdirs()
                }
               FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(folderName, apkAsset.name)).toFile().toUri()
            }
            // Get the app name from resources
            val appName = context.getString(R.string.app_name)
            return DownloadManager.Request(downloadUri)
                .setTitle(apkAsset.name)
                .setDescription(context.getString(R.string.downloading_update))
                .setDestinationUri(downloadUriFile)
                .setMimeType(ReleaseAsset.APK_MIME_TYPE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .addRequestHeader("User-Agent", appName)
        }
        return null
    }*/

    fun getDownloadRequest(context: Context, onUpdate: (progress: Int?) -> Unit): File? {
        val apkAsset = downloads.firstOrNull { it.isApk }
        if (apkAsset != null) {
            val apkFileName = apkAsset.name
            val downloadUri = apkAsset.downloadUrl
            val folderName = File(context.filesDir, ApkFolderName)
            if (!folderName.exists()) {
                folderName.mkdirs()
            }
            val apkFile = File(folderName, apkFileName)
            val client = OkHttpClient()

            // Prepare the request
            val request = Request.Builder()
                .url(downloadUri)
                .build()

            // Perform the request
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val fos = FileOutputStream(apkFile)
                val inputStream: InputStream? = response.body?.byteStream()
                val totalBytes = response.body?.contentLength() ?: -1L
                // Handle progress updates
                val buffer = ByteArray(4096)
                var downloadedBytes = 0L
                var read: Int

                inputStream?.use { input ->
                    fos.use { output ->
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            // Calculate and update progress
                            val progress = if (totalBytes > 0) {
                                (downloadedBytes * 100 / totalBytes).toInt()
                            } else {
                                0
                            }
                            onUpdate(progress)
                        }
                    }
                }
                return apkFile
            }
            return null
        }
        return null
    }


    fun getFormattedDate(): String? {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = format.parse(date)
        if (date != null) {
            return SimpleDateFormat.getDateInstance().format(date)
        }
        return null
    }
}

class ReleaseAsset(
    @SerializedName("name")
    val name: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("size")
    val size: Long,
    @SerializedName("browser_download_url")
    val downloadUrl: String
) : Serializable {

    val isApk: Boolean
        get() = contentType == APK_MIME_TYPE

    companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}