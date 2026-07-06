package com.explorebyte.ar.core.utils

import android.content.Context
import android.content.Intent
import com.explorebyte.ar.ARActivity
import com.explorebyte.ar.PseudoARActivity
import com.google.ar.core.ArCoreApk
import io.sentry.Sentry

/**
 * Helper utility untuk mengecek ketersediaan ARCore di device
 * dan merutekan user ke activity yang sesuai.
 *
 * - Device support ARCore → buka ARActivity (marker + tap-to-place)
 * - Device tidak support ARCore → buka PseudoARActivity (CameraX + SceneView)
 */
object ArCoreCheckHelper {

    /**
     * Membuka fitur AR dengan otomatis mendeteksi kemampuan device.
     *
     * @param context Context pemanggil (Activity)
     * @param shapeType Tipe bangun ruang: "KUBUS", "BALOK", atau "PRISMA"
     */
    fun openARFeature(context: Context, shapeType: String = "KUBUS") {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        val intent = if (availability.isSupported) {
            Sentry.addBreadcrumb("ARCheck: Device supports ARCore, opening ARActivity")
            Intent(context, ARActivity::class.java)
        } else {
            Sentry.addBreadcrumb("ARCheck: Device does NOT support ARCore, opening PseudoARActivity")
            Intent(context, PseudoARActivity::class.java)
        }
        intent.putExtra("SHAPE_TYPE", shapeType)
        context.startActivity(intent)
    }
}
