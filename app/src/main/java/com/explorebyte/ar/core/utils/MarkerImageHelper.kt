package com.explorebyte.ar.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session
import com.google.ar.core.exceptions.ImageInsufficientQualityException

/**
 * Helper class untuk memuat, memvalidasi, dan mendaftarkan marker image
 * ke ARCore AugmentedImageDatabase.
 *
 * ARCore membutuhkan gambar marker yang:
 * - Memiliki resolusi minimal 300x300 pixel
 * - Memiliki banyak fitur visual (edge, kontras)
 * - Skor kualitas >= 75 dari 100
 */
object MarkerImageHelper {

    private const val TAG = "MarkerImageHelper"
    private const val MIN_RECOMMENDED_SIZE = 300
    private const val DEFAULT_PHYSICAL_WIDTH_METERS = 0.20f // 20cm default

    /**
     * Data class untuk hasil operasi marker.
     */
    data class MarkerResult(
        val success: Boolean,
        val message: String,
        val imageDatabase: AugmentedImageDatabase? = null
    )

    /**
     * Informasi tentang satu marker yang akan didaftarkan.
     * @param name Nama unik marker (digunakan untuk identifikasi saat tracking)
     * @param assetPath Path ke gambar di folder assets
     * @param physicalWidthMeters Ukuran fisik lebar marker saat dicetak (meter)
     * @param modelPath Path ke model 3D yang akan ditampilkan saat marker terdeteksi
     */
    data class MarkerInfo(
        val name: String,
        val assetPath: String,
        val physicalWidthMeters: Float = DEFAULT_PHYSICAL_WIDTH_METERS,
        val modelPath: String = ""
    )

    /**
     * Memuat bitmap dari assets dan scale ke resolusi yang optimal untuk ARCore.
     */
    fun loadAndScaleBitmap(context: Context, assetPath: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(assetPath)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e(TAG, "Gagal decode bitmap dari: $assetPath")
                return null
            }

            // Scale up jika terlalu kecil
            val scaledBitmap = ensureMinimumSize(originalBitmap)
            Log.d(TAG, "Bitmap loaded: ${scaledBitmap.width}x${scaledBitmap.height} dari $assetPath")
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat gambar marker: $assetPath", e)
            null
        }
    }

    /**
     * Memastikan bitmap memiliki ukuran minimal yang disarankan ARCore.
     * Jika lebih kecil, akan di-scale up dengan filter bilinear.
     */
    private fun ensureMinimumSize(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width >= MIN_RECOMMENDED_SIZE && height >= MIN_RECOMMENDED_SIZE) {
            return bitmap
        }

        val scaleFactor = MIN_RECOMMENDED_SIZE.toFloat() / minOf(width, height)
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        Log.d(TAG, "Scaling bitmap dari ${width}x${height} ke ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Pre-check kualitas gambar sebelum mengirim ke ARCore.
     * Menghitung kontras dan edge density.
     * @return Pair(isGoodQuality, qualityMessage)
     */
    fun preCheckImageQuality(bitmap: Bitmap): Pair<Boolean, String> {
        val width = bitmap.width
        val height = bitmap.height

        // Check resolusi
        if (width < 100 || height < 100) {
            return Pair(false, "Resolusi marker terlalu rendah (${width}x${height}). Gunakan gambar minimal 300x300 pixel.")
        }

        // Hitung kontras — sampling pixel secara grid
        var minLuminance = 255
        var maxLuminance = 0
        val stepX = maxOf(1, width / 10)
        val stepY = maxOf(1, height / 10)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val luminance = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
                minLuminance = minOf(minLuminance, luminance)
                maxLuminance = maxOf(maxLuminance, luminance)
            }
        }

        val contrast = maxLuminance - minLuminance
        if (contrast < 50) {
            return Pair(false, "Marker memiliki kontras rendah ($contrast). Gunakan gambar dengan pola warna yang bervariasi.")
        }

        // Hitung edge density sederhana (Sobel-like horizontal)
        var edgeCount = 0
        var totalSampled = 0
        for (y in 1 until height - 1 step stepY) {
            for (x in 1 until width - 1 step stepX) {
                val left = luminance(bitmap.getPixel(x - 1, y))
                val right = luminance(bitmap.getPixel(x + 1, y))
                val top = luminance(bitmap.getPixel(x, y - 1))
                val bottom = luminance(bitmap.getPixel(x, y + 1))
                val gradient = Math.abs(right - left) + Math.abs(bottom - top)
                if (gradient > 30) edgeCount++
                totalSampled++
            }
        }

        val edgeDensity = if (totalSampled > 0) edgeCount.toFloat() / totalSampled else 0f
        if (edgeDensity < 0.05f) {
            return Pair(false, "Marker memiliki terlalu sedikit fitur visual. Gunakan gambar dengan pola yang lebih kompleks.")
        }

        return Pair(true, "Kualitas marker baik (kontras: $contrast, edge density: ${(edgeDensity * 100).toInt()}%)")
    }

    private fun luminance(pixel: Int): Int {
        return (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
    }

    /**
     * Membuat AugmentedImageDatabase dan mendaftarkan semua marker.
     * Menangani ImageInsufficientQualityException secara eksplisit.
     */
    fun buildImageDatabase(
        context: Context,
        session: Session,
        markers: List<MarkerInfo>
    ): MarkerResult {
        if (markers.isEmpty()) {
            return MarkerResult(false, "Tidak ada marker yang didaftarkan.")
        }

        val imageDatabase = AugmentedImageDatabase(session)
        val successNames = mutableListOf<String>()
        val failedNames = mutableListOf<String>()
        val messages = mutableListOf<String>()

        for (marker in markers) {
            try {
                val bitmap = loadAndScaleBitmap(context, marker.assetPath)
                if (bitmap == null) {
                    failedNames.add(marker.name)
                    messages.add("❌ ${marker.name}: Gagal memuat gambar dari ${marker.assetPath}")
                    continue
                }

                // Pre-check kualitas
                val (isGood, qualityMsg) = preCheckImageQuality(bitmap)
                Log.d(TAG, "${marker.name} quality check: $qualityMsg")

                if (!isGood) {
                    messages.add("⚠️ ${marker.name}: $qualityMsg (tetap mencoba mendaftarkan...)")
                }

                // Daftarkan ke ARCore dengan physical width
                imageDatabase.addImage(marker.name, bitmap, marker.physicalWidthMeters)
                successNames.add(marker.name)
                messages.add("✅ ${marker.name}: Berhasil didaftarkan")

            } catch (e: ImageInsufficientQualityException) {
                failedNames.add(marker.name)
                messages.add("❌ ${marker.name}: Kualitas gambar terlalu rendah untuk ARCore. Gunakan gambar dengan lebih banyak detail visual.")
                Log.e(TAG, "ImageInsufficientQualityException untuk ${marker.name}", e)
            } catch (e: Exception) {
                failedNames.add(marker.name)
                messages.add("❌ ${marker.name}: Error - ${e.message}")
                Log.e(TAG, "Error mendaftarkan marker ${marker.name}", e)
            }
        }

        return if (successNames.isNotEmpty()) {
            MarkerResult(
                success = true,
                message = "Marker siap: ${successNames.joinToString(", ")}. " +
                        if (failedNames.isNotEmpty()) "Gagal: ${failedNames.joinToString(", ")}." else "",
                imageDatabase = imageDatabase
            )
        } else {
            MarkerResult(
                success = false,
                message = "Semua marker gagal dimuat. ${messages.joinToString(" | ")}"
            )
        }
    }
}
