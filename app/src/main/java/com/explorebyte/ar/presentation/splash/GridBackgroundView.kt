package com.explorebyte.ar.presentation.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.explorebyte.ar.R

/**
 * Custom View yang menggambar pola dot grid background untuk splash screen.
 */
class GridBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dotPaint = Paint().apply {
        color = context.getColor(R.color.math_dot_grid)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val gridSpacing = 32f // dp-ish pixel spacing

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density
        val spacing = gridSpacing * density

        // Draw small dots in a grid pattern
        var x = (w % spacing) / 2f
        while (x < w) {
            var y = (h % spacing) / 2f
            while (y < h) {
                canvas.drawCircle(x, y, 1.2f * density, dotPaint)
                y += spacing
            }
            x += spacing
        }
    }
}
