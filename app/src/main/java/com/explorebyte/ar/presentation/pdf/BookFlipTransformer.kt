package com.explorebyte.ar.presentation.pdf

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class BookFlipTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.cameraDistance = 20000f

        when {
            position < -1f -> { // Off-screen to the left
                page.alpha = 0f
            }
            position <= 0f -> { // Current page flipping to the left (Forward)
                page.alpha = 1f
                page.pivotX = 0f // Spine is on the left
                page.rotationY = 180f * position // 0 to -180 degrees
                
                // Keep the page visible while it's flipping
                page.translationX = 0f
                
                // Depth effect: move it "above" the next page
                page.translationZ = if (position > -0.5f) 1f else -1f
            }
            position <= 1f -> { // Next page appearing from underneath
                page.alpha = 1f
                page.pivotX = 0f
                page.rotationY = 0f
                
                // Static underneath the flipping page
                page.translationX = -page.width * position
                page.translationZ = -1f
            }
            else -> { // Off-screen to the right
                page.alpha = 0f
            }
        }
    }
}
