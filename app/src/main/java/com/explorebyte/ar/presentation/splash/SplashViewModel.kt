package com.explorebyte.ar.presentation.splash

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.explorebyte.ar.BuildConfig
import com.explorebyte.ar.data.remote.AppVersionResponse
import com.explorebyte.ar.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val _updateState = MutableLiveData<UpdateState>()
    val updateState: LiveData<UpdateState> = _updateState

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> = _downloadProgress

    private var downloadedApkUri: Uri? = null

    sealed class UpdateState {
        object Checking : UpdateState()
        object NoUpdate : UpdateState()
        data class UpdateAvailable(val version: AppVersionResponse) : UpdateState()
        object Downloading : UpdateState()
        data class DownloadReady(val file: File) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    fun checkForUpdate() {
        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            try {
                val versions = SupabaseClient.apiService.getLatestVersion()
                if (versions.isNotEmpty()) {
                    val latest = versions[0]
                    if (latest.version_code > BuildConfig.VERSION_CODE) {
                        _updateState.value = UpdateState.UpdateAvailable(latest)
                    } else {
                        _updateState.value = UpdateState.NoUpdate
                    }
                } else {
                    _updateState.postValue(UpdateState.Error("Database kosong atau akses RLS terblokir"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("SplashViewModel", "Error checking update: ${e.message}", e)
                _updateState.postValue(UpdateState.Error("Err: ${e.message}"))
            }
        }
    }

    fun downloadApk(url: String) {
        _updateState.value = UpdateState.Downloading
        _downloadProgress.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    _updateState.postValue(UpdateState.Error("Gagal mengunduh pembaruan"))
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _updateState.postValue(UpdateState.Error("File tidak ditemukan"))
                    return@launch
                }

                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                
                val dir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (dir != null && !dir.exists()) {
                    dir.mkdirs()
                }
                
                val file = File(dir, "update_v${System.currentTimeMillis()}.apk")
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        _downloadProgress.postValue(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                _updateState.postValue(UpdateState.DownloadReady(file))

            } catch (e: Exception) {
                _updateState.postValue(UpdateState.Error(e.message ?: "Terjadi kesalahan"))
            }
        }
    }
}
