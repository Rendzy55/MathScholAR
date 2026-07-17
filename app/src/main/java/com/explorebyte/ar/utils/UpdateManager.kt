package com.explorebyte.ar.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.explorebyte.ar.BuildConfig
import com.explorebyte.ar.R
import com.explorebyte.ar.data.model.AppVersionResponse
import com.explorebyte.ar.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.sentry.Sentry
import kotlinx.coroutines.*
import java.io.File

class UpdateManager(private val context: Context) {

    private var downloadJob: Job? = null

    private fun getAppUpdateDir(): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "MathScholAR Update")
    }

    private fun getLegacyUpdateDir(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MathScholAR Update")
    }

    private fun resolveApkFile(fileName: String): File {
        val appDirFile = File(getAppUpdateDir(), fileName)
        if (appDirFile.exists()) return appDirFile
        return File(getLegacyUpdateDir(), fileName)
    }

    private fun clearOldUpdateApks() {
        try {
            // Clean internal cache
            context.cacheDir.listFiles()?.forEach { if (it.name.endsWith(".apk")) it.delete() }

            // Clean app-specific update folder
            val appFolder = getAppUpdateDir()
            if (appFolder.exists()) {
                appFolder.listFiles()?.forEach { if (it.name.endsWith(".apk")) it.delete() }
            }

            // Clean legacy public folder (backward compatibility)
            val legacyFolder = getLegacyUpdateDir()
            if (legacyFolder.exists()) {
                legacyFolder.listFiles()?.forEach { if (it.name.endsWith(".apk")) it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Sentry.captureException(e)
        }
    }

    suspend fun checkForUpdates(onProceed: () -> Unit, onError: (String) -> Unit) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val pendingApk = prefs.getString("pending_apk_name", null)
        
        if (pendingApk != null) {
            // Check if file still exists
            val file = resolveApkFile(pendingApk)
            if (file.exists()) {
                // If it exists and user is returning from permission screen, try install again
                withContext(Dispatchers.Main) {
                    installApk(pendingApk)
                }
                // Don't proceed to session until update is handled or canceled
                return
            } else {
                prefs.edit().remove("pending_apk_name").apply()
            }
        }

        clearOldUpdateApks()
        try {
            // Get the latest active update from Supabase
            val latestUpdate = withContext(Dispatchers.IO) {
                SupabaseManager.client.postgrest["app_versions"]
                    .select {
                        order("version_code", Order.DESCENDING)
                        limit(1)
                    }
                    .decodeSingleOrNull<AppVersionResponse>()
            }

            withContext(Dispatchers.Main) {
                if (latestUpdate != null) {
                    if (latestUpdate.version_code > BuildConfig.VERSION_CODE) {
                        showUpdateDialog(latestUpdate, onProceed)
                    } else {
                        onProceed()
                    }
                } else {
                    onError("Database kosong atau akses RLS terblokir")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Sentry.captureException(e)
            withContext(Dispatchers.Main) {
                onError("Gagal mengecek pembaruan: ${e.localizedMessage}")
            }
        }
    }

    private fun showUpdateDialog(update: AppVersionResponse, onProceed: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_update_dialog, null)
        val dialog = AlertDialog.Builder(context, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
            .setView(dialogView)
            .setCancelable(false) // Wajib update
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Shared preference to remember pending install
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        dialogView.findViewById<TextView>(R.id.tvUpdateTitle).text = "Pembaruan Tersedia (Versi ${update.version_code})"
        dialogView.findViewById<TextView>(R.id.tvUpdateMessage).text = (update.message ?: "Versi terbaru telah tersedia!").replace("\\n", "\n")
        
        val btnLater = dialogView.findViewById<Button>(R.id.btnLater)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btnUpdate)
        val layoutProgress = dialogView.findViewById<LinearLayout>(R.id.layoutProgress)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val layoutButtons = dialogView.findViewById<LinearLayout>(R.id.layoutButtons)

        // Asumsikan MathScholAR selalu wajib update (bisa diubah nanti)
        btnLater.visibility = View.GONE

        btnLater.setOnClickListener {
            downloadJob?.cancel()
            dialog.dismiss()
            onProceed()
        }

        btnUpdate.setOnClickListener {
            layoutButtons.visibility = View.GONE
            layoutProgress.visibility = View.VISIBLE
            
            val fileName = "mathscholar.apk"
            val downloadUrl = update.apk_url ?: "https://github.com/explorebyte/mathscholar/releases/latest/download/app-release.apk"
            startDownloadWithProgress(downloadUrl, fileName, progressBar, tvProgress) {
                // Save pending install state
                prefs.edit().putString("pending_apk_name", fileName).apply()
                
                installApk(fileName)
                // Karena wajib update, jangan onProceed di sini.
            }
        }

        dialog.show()
    }

    private fun startDownloadWithProgress(
        url: String, 
        fileName: String, 
        progressBar: ProgressBar, 
        tvProgress: TextView,
        onComplete: () -> Unit
    ) {
        val folder = getAppUpdateDir()
        if (!folder.exists()) folder.mkdirs()
        
        val destinationFile = File(folder, fileName)
        if (destinationFile.exists()) destinationFile.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Mendownload Pembaruan MathScholAR")
            .setDescription("Aplikasi sedang memperbarui diri...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(destinationFile))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        downloadJob = CoroutineScope(Dispatchers.Main).launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                        progressBar.progress = 100
                        tvProgress.text = "100% - Selesai"
                        delay(800)
                        onComplete()
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        Toast.makeText(context, "Gagal mengunduh pembaruan", Toast.LENGTH_SHORT).show()
                    }

                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                        progressBar.progress = progress
                        tvProgress.text = "$progress%"
                    }
                }
                cursor.close()
                delay(500) // Update every 500ms
            }
        }
    }

    private fun installApk(fileName: String) {
        try {
            val file = resolveApkFile(fileName)
            
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                intent.data = uri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Check for unknown source installation permission (Android 8.0+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        Toast.makeText(context, "Berikan izin untuk menginstal dari sumber ini", Toast.LENGTH_LONG).show()
                        val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        settingsIntent.data = Uri.parse("package:${context.packageName}")
                        // Don't use NEW_TASK here if you want to handle result, but since we are in a utility class
                        // we will just let it open. The check in checkForUpdates will handle the rerun.
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(settingsIntent)
                        return
                    }
                }
                
                // Clear pending flag if we reach here
                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .edit().remove("pending_apk_name").apply()
                
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "File APK tidak ditemukan di ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Sentry.captureException(e)
            android.util.Log.e("UpdateManager", "Error installing APK: ${e.message}")
            Toast.makeText(context, "Gagal membuka installer", Toast.LENGTH_LONG).show()
        }
    }
}
