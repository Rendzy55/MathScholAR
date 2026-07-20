package com.explorebyte.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.sentry.Sentry

/**
 * PseudoARActivity — Mode fallback AR untuk device yang tidak mendukung ARCore.
 *
 * Menampilkan preview kamera (CameraX) sebagai background, dengan objek 3D
 * interaktif di atasnya (SceneView non-AR). Menyediakan UX yang konsisten
 * dengan ARActivity: tab Full/Rusuk, tap-to-place, gesture rotate/zoom.
 *
 * Perbedaan utama dengan ARActivity:
 * - Tidak ada plane detection (objek langsung muncul di tengah layar saat tap)
 * - Tidak ada marker/augmented image detection
 * - Tidak membutuhkan ARCore session
 */
class PseudoARActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PseudoARActivity"
    }

    // UI References
    private lateinit var cameraPreview: PreviewView
    private lateinit var sceneView: SceneView
    private lateinit var tvInstruction: TextView
    private lateinit var btnFull: TextView
    private lateinit var btnRusuk: TextView
    private lateinit var permissionDeniedLayout: View
    private lateinit var btnOpenSettings: Button

    // Model paths configuration (identical to ARActivity)
    private val modelMap = mapOf(
        "KUBUS" to Pair("models/cube.glb", "models/cubewire.glb"),
        "BALOK" to Pair("models/balok.glb", "models/balokwire.glb"),
        "PRISMA" to Pair("models/prisma.glb", "models/prismawire.glb")
    )

    // State
    private var currentShapeType = "KUBUS"
    private var isWireframeMode = false
    private var sceneModelNode: ModelNode? = null
    private var isObjectRendered = false
    
    // Gesture Detectors
    private lateinit var scaleGestureDetector: android.view.ScaleGestureDetector
    private lateinit var gestureDetector: android.view.GestureDetector
    private var scaleFactor = 1.0f
    private var currentScale = 0.5f

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Sentry.addBreadcrumb("PseudoAR: Camera permission granted")
            permissionDeniedLayout.visibility = View.GONE
            setupCamera()
        } else {
            Sentry.addBreadcrumb("PseudoAR: Camera permission denied")
            showPermissionDeniedUI()
        }
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pseudo_ar)

        // Sentry context for fallback mode
        Sentry.setTag("ar_mode", "pseudo_ar")

        // Get shape type from intent
        currentShapeType = intent.getStringExtra("SHAPE_TYPE") ?: "KUBUS"
        Sentry.addBreadcrumb("PseudoAR: Activity created, shape=$currentShapeType")
        Log.d(TAG, "PseudoARActivity started with shape: $currentShapeType")

        // Bind views
        cameraPreview = findViewById(R.id.cameraPreview)
        sceneView = findViewById(R.id.sceneView)
        tvInstruction = findViewById(R.id.tvInstruction)
        btnFull = findViewById(R.id.btnFull)
        btnRusuk = findViewById(R.id.btnRusuk)
        permissionDeniedLayout = findViewById(R.id.permissionDeniedLayout)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        setupSceneView()
        setupControls()
        setupGestures()
        requestCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        if (!isObjectRendered) {
            loadModel()
            isObjectRendered = true
            Log.d("RenderDebug", "Object created at ${System.currentTimeMillis()}, isObjectRendered = true")
            sceneView.invalidate()
        } else {
            Log.d("RenderDebug", "onResume called, isObjectRendered = true. Applying FALLBACK: recreate()")
            recreate()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("RenderDebug", "onPause called")
        try {
            @Suppress("DEPRECATION")
            sceneView.destroyDrawingCache()
        } catch (e: Exception) {
            Log.w(TAG, "Error in destroyDrawingCache", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RenderDebug", "onDestroy called")
        clearAllObjects()
        isObjectRendered = false
        Log.d("RenderDebug", "Object cleared at ${System.currentTimeMillis()}, isObjectRendered = false")
        
        try {
            sceneView.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying SceneView", e)
        }
    }

    private fun clearAllObjects() {
        sceneModelNode?.let { 
            sceneView.removeChild(it)
            try {
                it.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying node", e)
            }
        }
        sceneModelNode = null
        System.gc()
    }

    // ─── Camera Permission ──────────────────────────────────────────────

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionDeniedLayout.visibility = View.GONE
                setupCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale, then request
                Snackbar.make(
                    cameraPreview,
                    "Fitur ini membutuhkan akses kamera untuk menampilkan objek 3D",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("Izinkan") {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionDeniedUI() {
        permissionDeniedLayout.visibility = View.VISIBLE
        btnOpenSettings.setOnClickListener {
            // Open app settings so user can manually enable camera permission
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    // ─── Camera Setup (CameraX) ─────────────────────────────────────────

    private fun setupCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(cameraPreview.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Unbind any previous use cases before rebinding
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                    Sentry.addBreadcrumb("PseudoAR: CameraX started successfully")
                    Log.d(TAG, "CameraX preview started")
                } catch (e: Exception) {
                    Log.e(TAG, "CameraX bind failed", e)
                    Sentry.captureException(e)
                    Toast.makeText(this, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "CameraX initialization failed", e)
            Sentry.captureException(e)
            Toast.makeText(this, "Gagal menginisialisasi kamera", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── SceneView Setup ────────────────────────────────────────────────

    private fun setupSceneView() {
        try {
            // 1. SurfaceView layer: put SceneView above camera (media overlay) but below UI window
            // This prevents the compositor bug (tiling) caused by setZOrderOnTop(true)
            sceneView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            sceneView.setZOrderMediaOverlay(true)
            sceneView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
            
            // HACK: Force GPU composition to bypass Mediatek/Mali SurfaceFlinger tiling bug
            sceneView.alpha = 0.99f

            // 2. Filament renderer: set blend mode to TRANSLUCENT so alpha channel is preserved
            //    Without this, Filament clears every frame with opaque black regardless of SurfaceView format
            sceneView.view.blendMode = com.google.android.filament.View.BlendMode.TRANSLUCENT

            // 3. Remove skybox (skybox renders an opaque environment background)
            sceneView.scene.skybox = null

            Sentry.addBreadcrumb("PseudoAR: SceneView initialized with Filament transparency")
            Log.d(TAG, "SceneView initialized with transparent Filament rendering")
        } catch (e: Exception) {
            Log.e(TAG, "SceneView transparency setup failed", e)
            Sentry.captureException(e)
            Toast.makeText(this, "Gagal memuat mesin 3D (Render Error)", Toast.LENGTH_LONG).show()
        }
    }

    // ─── Controls Setup ─────────────────────────────────────────────────

    private fun setupControls() {
        updateToggleUI()

        // Tab Full
        btnFull.setOnClickListener {
            if (isWireframeMode) {
                isWireframeMode = false
                updateToggleUI()
                loadModel()
                Sentry.addBreadcrumb("PseudoAR: Switched to Full mode")
            }
        }

        // Tab Rusuk
        btnRusuk.setOnClickListener {
            if (!isWireframeMode) {
                isWireframeMode = true
                updateToggleUI()
                loadModel()
                Sentry.addBreadcrumb("PseudoAR: Switched to Wireframe/Rusuk mode")
            }
        }

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Reset button
        findViewById<ImageButton>(R.id.btnReset).setOnClickListener {
            resetScene()
        }
        
        // Hide tap instruction, change to drag instruction
        tvInstruction.text = "Geser untuk memutar, cubit untuk zoom"
        
        // Pass touch events to gesture detectors
        sceneView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupGestures() {
        scaleGestureDetector = android.view.ScaleGestureDetector(this, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.2f, 3.0f) // limit scale
                val newScale = currentScale * scaleFactor
                sceneModelNode?.scale = io.github.sceneview.math.Scale(newScale, newScale, newScale)
                return true
            }
        })

        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                sceneModelNode?.let { node ->
                    // Rotate model: dragging horizontally rotates around Y, vertically around X
                    val yaw = -distanceX * 0.5f
                    val pitch = -distanceY * 0.5f
                    val currentRotation = node.rotation
                    node.rotation = io.github.sceneview.math.Rotation(
                        x = currentRotation.x + pitch,
                        y = currentRotation.y + yaw,
                        z = currentRotation.z
                    )
                }
                return true
            }
        })
    }

    /**
     * Update toggle UI — identical logic to ARActivity.updateToggleUI()
     */
    private fun updateToggleUI() {
        val inactiveColor = getColor(R.color.math_text_primary)

        btnFull.isSelected = !isWireframeMode
        btnRusuk.isSelected = isWireframeMode

        btnFull.setTextColor(if (!isWireframeMode) getColor(R.color.white) else inactiveColor)
        btnRusuk.setTextColor(if (isWireframeMode) getColor(R.color.white) else inactiveColor)
    }

    // ─── Model Loading & Placement ──────────────────────────────────────

    /**
     * Load model immediately and center it perfectly.
     */
    private fun loadModel() {
        try {
            val paths = modelMap[currentShapeType] ?: return
            val modelPath = if (isWireframeMode) paths.second else paths.first

            // Clear old node
            sceneModelNode?.let { sceneView.removeChild(it) }
            
            // Reset scale factor
            scaleFactor = 1.0f

            sceneModelNode = ModelNode(sceneView.engine).apply {
                loadModelGlbAsync(
                    glbFileLocation = modelPath,
                    autoAnimate = true,
                    scaleToUnits = currentScale,
                    centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f) // Force center origin!
                ) { 
                    Log.d(TAG, "Model loaded: $modelPath")
                }
                position = Position(0.0f, 0.0f, -2.0f) // Place in front of camera
            }

            sceneView.addChild(sceneModelNode!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            Sentry.captureException(e)
            Toast.makeText(this, "Gagal memuat model 3D", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Reset scene — reload model to original position and scale.
     */
    private fun resetScene() {
        try {
            loadModel()
            Toast.makeText(this, "AR Reset", Toast.LENGTH_SHORT).show()
            Sentry.addBreadcrumb("PseudoAR: Scene reset")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset scene", e)
            Sentry.captureException(e)
        }
    }
}
