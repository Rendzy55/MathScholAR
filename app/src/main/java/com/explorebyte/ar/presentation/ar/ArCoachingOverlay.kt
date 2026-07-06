package com.explorebyte.ar.presentation.ar

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.explorebyte.ar.R

/**
 * ArCoachingOverlay — Custom View overlay yang memberikan panduan visual
 * kepada pengguna untuk membantu tracking AR bekerja optimal.
 *
 * Fitur:
 * - Animasi ikon scanning (ponsel bergerak kiri-kanan)
 * - Status text yang berubah sesuai kondisi tracking
 * - Indikator pencahayaan (warning jika terlalu gelap)
 * - Auto-hide ketika plane berhasil terdeteksi
 * - Dua mode: PLANE_DETECTION dan MARKER_SCANNING
 */
class ArCoachingOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // UI Elements
    private val overlayBackground: View
    private val coachingContent: View
    private val ivCoachingIcon: ImageView
    private val tvCoachingTitle: TextView
    private val tvCoachingSubtitle: TextView
    private val statusBarContainer: View
    private val tvTrackingStatus: TextView
    private val tvLightingStatus: TextView

    // Animators
    private var scanAnimator: AnimatorSet? = null
    private var pulseAnimator: ObjectAnimator? = null

    // State
    private var currentMode: CoachingMode = CoachingMode.PLANE_DETECTION
    private var isShowing = true

    enum class CoachingMode {
        PLANE_DETECTION,
        MARKER_SCANNING
    }

    enum class TrackingStatus {
        NOT_READY,        // Belum ada tracking
        SEARCHING,        // Sedang mencari plane/marker
        FOUND,            // Plane/marker ditemukan
        LOST              // Tracking hilang
    }

    enum class LightingCondition {
        UNKNOWN,
        TOO_DARK,
        ADEQUATE,
        GOOD
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_coaching_overlay, this, true)

        overlayBackground = findViewById(R.id.overlayBackground)
        coachingContent = findViewById(R.id.coachingContent)
        ivCoachingIcon = findViewById(R.id.ivCoachingIcon)
        tvCoachingTitle = findViewById(R.id.tvCoachingTitle)
        tvCoachingSubtitle = findViewById(R.id.tvCoachingSubtitle)
        statusBarContainer = findViewById(R.id.statusBarContainer)
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus)
        tvLightingStatus = findViewById(R.id.tvLightingStatus)

        // Start default animation
        startScanAnimation()
    }

    /**
     * Set mode coaching: PLANE_DETECTION atau MARKER_SCANNING
     */
    fun setMode(mode: CoachingMode) {
        currentMode = mode
        when (mode) {
            CoachingMode.PLANE_DETECTION -> {
                ivCoachingIcon.setImageResource(R.drawable.ic_hand_scan)
                tvCoachingTitle.text = "Arahkan kamera ke bidang datar"
                tvCoachingSubtitle.text = "Gerakkan ponsel secara perlahan ke kiri dan kanan"
            }
            CoachingMode.MARKER_SCANNING -> {
                ivCoachingIcon.setImageResource(R.drawable.ic_marker_scan)
                tvCoachingTitle.text = "Arahkan kamera ke marker"
                tvCoachingSubtitle.text = "Pastikan marker terlihat jelas dan tidak terlipat"
            }
        }
        startScanAnimation()
    }

    /**
     * Update status tracking — mengubah teks dan warna status indicator
     */
    fun updateTrackingStatus(status: TrackingStatus) {
        when (status) {
            TrackingStatus.NOT_READY -> {
                tvTrackingStatus.text = "⏳ Mempersiapkan kamera..."
                tvTrackingStatus.setBackgroundResource(R.drawable.bg_status_pill)
                showOverlay()
            }
            TrackingStatus.SEARCHING -> {
                val searchText = if (currentMode == CoachingMode.MARKER_SCANNING) {
                    "🔍 Mencari marker..."
                } else {
                    "🔍 Mencari bidang datar..."
                }
                tvTrackingStatus.text = searchText
                showOverlay()
            }
            TrackingStatus.FOUND -> {
                val foundText = if (currentMode == CoachingMode.MARKER_SCANNING) {
                    "✅ Marker terdeteksi!"
                } else {
                    "✅ Bidang datar terdeteksi! Tap untuk menaruh objek"
                }
                tvTrackingStatus.text = foundText
                hideOverlay()
            }
            TrackingStatus.LOST -> {
                val lostText = if (currentMode == CoachingMode.MARKER_SCANNING) {
                    "⚠️ Marker hilang — arahkan ulang ke marker"
                } else {
                    "⚠️ Tracking hilang — gerakkan ponsel perlahan"
                }
                tvTrackingStatus.text = lostText
                showOverlay()
            }
        }
    }

    /**
     * Update kondisi pencahayaan
     */
    fun updateLightingCondition(condition: LightingCondition) {
        when (condition) {
            LightingCondition.UNKNOWN -> {
                tvLightingStatus.visibility = View.GONE
            }
            LightingCondition.TOO_DARK -> {
                tvLightingStatus.visibility = View.VISIBLE
                tvLightingStatus.text = "🌑 Terlalu gelap!"
            }
            LightingCondition.ADEQUATE -> {
                tvLightingStatus.visibility = View.VISIBLE
                tvLightingStatus.text = "🌤️ Pencahayaan cukup"
            }
            LightingCondition.GOOD -> {
                tvLightingStatus.visibility = View.VISIBLE
                tvLightingStatus.text = "☀️ Pencahayaan baik"
                // Auto-hide after 3 seconds
                tvLightingStatus.postDelayed({
                    tvLightingStatus.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction { tvLightingStatus.visibility = View.GONE }
                        .start()
                }, 3000)
            }
        }
    }

    /**
     * Tampilkan custom message di status bar (untuk feedback marker dll)
     */
    fun showStatusMessage(message: String) {
        tvTrackingStatus.text = message
    }

    /**
     * Tampilkan coaching overlay dengan animasi fade-in
     */
    fun showOverlay() {
        if (isShowing) return
        isShowing = true

        overlayBackground.visibility = View.VISIBLE
        coachingContent.visibility = View.VISIBLE

        overlayBackground.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
        coachingContent.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        startScanAnimation()
    }

    /**
     * Sembunyikan coaching overlay dengan animasi fade-out.
     * Status bar tetap terlihat.
     */
    fun hideOverlay() {
        if (!isShowing) return
        isShowing = false

        overlayBackground.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction { overlayBackground.visibility = View.GONE }
            .start()
        coachingContent.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction { coachingContent.visibility = View.GONE }
            .start()

        stopScanAnimation()
    }

    /**
     * Sembunyikan seluruh overlay termasuk status bar
     */
    fun hideCompletely() {
        hideOverlay()
        statusBarContainer.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction { statusBarContainer.visibility = View.GONE }
            .start()
    }

    /**
     * Tampilkan kembali status bar (misalnya setelah model ditempatkan)
     */
    fun showStatusBar() {
        statusBarContainer.visibility = View.VISIBLE
        statusBarContainer.alpha = 0f
        statusBarContainer.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * Mulai animasi scan (ikon bergerak kiri-kanan + pulse)
     */
    private fun startScanAnimation() {
        stopScanAnimation()

        // Horizontal translate animation (kiri-kanan)
        val translateX = ObjectAnimator.ofFloat(ivCoachingIcon, "translationX", -30f, 30f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Subtle scale pulse
        val scaleX = ObjectAnimator.ofFloat(ivCoachingIcon, "scaleX", 0.9f, 1.1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(ivCoachingIcon, "scaleY", 0.9f, 1.1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        scanAnimator = AnimatorSet().apply {
            playTogether(translateX, scaleX, scaleY)
            start()
        }
    }

    /**
     * Hentikan semua animasi
     */
    private fun stopScanAnimation() {
        scanAnimator?.cancel()
        scanAnimator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopScanAnimation()
    }
}
