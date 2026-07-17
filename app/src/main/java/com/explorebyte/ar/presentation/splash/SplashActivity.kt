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
import com.explorebyte.ar.utils.UpdateManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
        tvSystemStatus.text = "Memeriksa pembaruan..."
        val updateManager = UpdateManager(this)
        lifecycleScope.launch {
            updateManager.checkForUpdates(
                onProceed = {
                    tvSystemStatus.text = "Sistem Siap"
                    btnInitialize.animate()
                        .alpha(1f)
                        .scaleX(1.1f).scaleY(1.1f)
                        .setDuration(500)
                        .withEndAction {
                            btnInitialize.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                        }
                        .start()
                },
                onError = { errorMessage ->
                    tvSystemStatus.text = "Sistem Siap ($errorMessage)"
                    // Fallback to start
                    btnInitialize.animate().alpha(1f).start()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
