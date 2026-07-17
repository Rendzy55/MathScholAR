package com.explorebyte.ar.presentation.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.explorebyte.ar.ARActivity
import com.explorebyte.ar.R
import com.explorebyte.ar.presentation.main.MainActivity

/**
 * SplashActivity — Layar pembuka aplikasi MathScholAR.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var flCubeContainer: FrameLayout
    private lateinit var ivTriangle: ImageView
    private lateinit var ivCircle: ImageView
    private lateinit var brandingSection: View
    private lateinit var btnInitialize: Button
    private lateinit var pbSplash: ProgressBar
    private lateinit var tvSystemStatus: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window.statusBarColor = getColor(R.color.math_bg_light)
        window.navigationBarColor = getColor(R.color.math_bg_light)

        setContentView(R.layout.activity_splash)

        // Bind views
        flCubeContainer = findViewById(R.id.flCubeContainer)
        ivTriangle = findViewById(R.id.ivTriangle)
        ivCircle = findViewById(R.id.ivCircle)
        brandingSection = findViewById(R.id.brandingSection)
        btnInitialize = findViewById(R.id.btnInitialize)
        pbSplash = findViewById(R.id.pbSplash)
        tvSystemStatus = findViewById(R.id.tvSystemStatus)

        // Initial state
        flCubeContainer.alpha = 0f
        ivTriangle.alpha = 0f
        ivCircle.alpha = 0f
        brandingSection.alpha = 0f
        btnInitialize.alpha = 0f
        pbSplash.progress = 0

        // Start animation sequence
        startAnimationSequence()

        // Button click → navigate to AR
        btnInitialize.setOnClickListener {
            navigateToAR()
        }
    }

    private fun startAnimationSequence() {
        // Step 1: Assets entry
        handler.postDelayed({
            animateAssetsEntry()
        }, 500)

        // Step 2: Branding entry
        handler.postDelayed({
            brandingSection.animate()
                .alpha(1f)
                .translationYBy(-20f)
                .setDuration(800)
                .start()
        }, 1200)

        // Step 3: Progress simulation
        handler.postDelayed({
            simulateProgress()
        }, 2000)

        // Step 4: Button entry
        // Step 4: Button entry / Update Check
        handler.postDelayed({
            checkUpdate()
        }, 3500)
    }

    private fun animateAssetsEntry() {
        // Cube
        flCubeContainer.scaleX = 0.5f
        flCubeContainer.scaleY = 0.5f
        flCubeContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(OvershootInterpolator())
            .setDuration(1000)
            .start()

        // Triangle
        ivTriangle.translationY = -100f
        ivTriangle.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(800)
            .start()

        // Circle
        ivCircle.translationY = 100f
        ivCircle.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(800)
            .start()
    }

    private fun simulateProgress() {
        val progressAnimator = ObjectAnimator.ofInt(pbSplash, "progress", 0, 100)
        progressAnimator.duration = 1500
        progressAnimator.interpolator = AccelerateDecelerateInterpolator()
        progressAnimator.start()
    }

    private fun navigateToAR() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun checkUpdate() {
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[SplashViewModel::class.java]
        
        viewModel.updateState.observe(this) { state ->
            when (state) {
                is SplashViewModel.UpdateState.Checking -> {
                    tvSystemStatus.text = "Memeriksa pembaruan..."
                }
                is SplashViewModel.UpdateState.NoUpdate -> {
                    tvSystemStatus.text = "Sistem Siap"
                    btnInitialize.animate()
                        .alpha(1f)
                        .scaleX(1.1f).scaleY(1.1f)
                        .setDuration(500)
                        .withEndAction {
                            btnInitialize.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                        }
                        .start()
                }
                is SplashViewModel.UpdateState.UpdateAvailable -> {
                    tvSystemStatus.text = "Pembaruan Tersedia"
                    showUpdateDialog(viewModel, state.version)
                }
                is SplashViewModel.UpdateState.Downloading -> {
                    tvSystemStatus.text = "Mengunduh pembaruan..."
                }
                is SplashViewModel.UpdateState.DownloadReady -> {
                    tvSystemStatus.text = "Menginstal pembaruan..."
                    installApk(state.file)
                }
                is SplashViewModel.UpdateState.Error -> {
                    tvSystemStatus.text = "Sistem Siap (Offline)"
                    // Fallback to start
                    btnInitialize.animate().alpha(1f).start()
                }
            }
        }

        viewModel.downloadProgress.observe(this) { progress ->
            pbSplash.progress = progress
            tvSystemStatus.text = "Mengunduh pembaruan... $progress%"
        }

        viewModel.checkForUpdate()
    }

    private fun showUpdateDialog(viewModel: SplashViewModel, version: com.explorebyte.ar.data.remote.AppVersionResponse) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Pembaruan Wajib Tersedia")
        builder.setMessage("Versi terbaru (${version.version_name ?: version.version_code}) telah tersedia.\n\nCatatan:\n${version.message ?: "-"}")
        builder.setCancelable(false)
        builder.setPositiveButton("Download & Update") { _, _ ->
            version.apk_url?.let {
                viewModel.downloadApk(it)
            }
        }
        builder.show()
    }

    private fun installApk(file: java.io.File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            tvSystemStatus.text = "Gagal membuka installer"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
