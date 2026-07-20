package com.explorebyte.ar

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.explorebyte.ar.core.utils.MarkerImageHelper
import com.explorebyte.ar.presentation.ar.ArCoachingOverlay
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.sentry.Sentry

class ARActivity : AppCompatActivity() {

    private lateinit var arSceneView: ArSceneView
    private lateinit var coachingOverlay: ArCoachingOverlay
    private lateinit var btnFull: TextView
    private lateinit var btnRusuk: TextView

    // Model paths configuration
    private val modelMap = mapOf(
        "KUBUS" to Pair("models/cube.glb", "models/cubewire.glb"),
        "BALOK" to Pair("models/balok.glb", "models/balokwire.glb"),
        "PRISMA" to Pair("models/prisma.glb", "models/prismawire.glb")
    )

    private var currentShapeType = "KUBUS"
    private var isWireframeMode = false
    private val placedMarkerModels = mutableSetOf<String>()
    private var hasPlaneBeenDetected = false
    private var trackedPlaneCount = 0
    private var lastLightingUpdate = 0L
    private var isObjectRendered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        // Get shape type from intent
        currentShapeType = intent.getStringExtra("SHAPE_TYPE") ?: "KUBUS"

        // Sentry context for ARCore mode
        Sentry.setTag("ar_mode", "arcore")

        arSceneView = findViewById(R.id.arSceneView)
        coachingOverlay = findViewById(R.id.coachingOverlay)
        btnFull = findViewById(R.id.btnFull)
        btnRusuk = findViewById(R.id.btnRusuk)

        coachingOverlay.updateTrackingStatus(ArCoachingOverlay.TrackingStatus.NOT_READY)

        setupArSession()
        setupFrameListener()
        setupControls()
    }

    private fun setupArSession() {
        arSceneView.onArSessionCreated = { session ->
            try {
                val config = Config(session)
                config.focusMode = Config.FocusMode.AUTO
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                // Set up Augmented Image Database
                val markers = listOf(
                    MarkerImageHelper.MarkerInfo(
                        name = "maker_kubus",
                        assetPath = "images/maker_kubus.jpg",
                        physicalWidthMeters = 0.20f
                    ),
                    MarkerImageHelper.MarkerInfo(
                        name = "kubus_maker",
                        assetPath = "images/kubus_maker.png",
                        physicalWidthMeters = 0.20f
                    )
                )
                val markerResult = MarkerImageHelper.buildImageDatabase(this, session, markers)
                if (markerResult.success && markerResult.imageDatabase != null) {
                    config.augmentedImageDatabase = markerResult.imageDatabase
                    Log.d("ARActivity", "Marker database loaded successfully: ${markerResult.message}")
                } else {
                    Log.e("ARActivity", "Failed to load marker database: ${markerResult.message}")
                }

                try {
                    session.configure(config)
                } catch (e: Exception) {
                    Log.w("ARActivity", "Session config failed, falling back to AMBIENT_INTENSITY", e)
                    config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                    session.configure(config)
                }
                
                runOnUiThread {
                    try {
                        arSceneView.lightEstimationMode = config.lightEstimationMode
                    } catch (e: Exception) {
                        Log.e("ARActivity", "Error setting arSceneView lightEstimationMode", e)
                    }
                    coachingOverlay.setMode(ArCoachingOverlay.CoachingMode.PLANE_DETECTION)
                    coachingOverlay.updateTrackingStatus(ArCoachingOverlay.TrackingStatus.SEARCHING)
                }
            } catch (e: Exception) {
                Sentry.captureException(e)
                runOnUiThread {
                    Toast.makeText(this@ARActivity, "Gagal memuat ARCore", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupControls() {
        updateToggleUI()

        btnFull.setOnClickListener {
            if (isWireframeMode) {
                isWireframeMode = false
                updateToggleUI()
                refreshModels()
            }
        }

        btnRusuk.setOnClickListener {
            if (!isWireframeMode) {
                isWireframeMode = true
                updateToggleUI()
                refreshModels()
            }
        }

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.widget.ImageButton>(R.id.btnReset).setOnClickListener {
            resetAR()
        }

        arSceneView.onTapAr = { hitResult, _ ->
            placeModelAt(hitResult.createAnchor())
        }
    }

    private fun updateToggleUI() {
        val activeColor = getColor(R.color.math_blue_main)
        val inactiveColor = getColor(R.color.math_text_primary)
        val activeBg = ColorStateList.valueOf(activeColor)
        val inactiveBg = null // uses drawable default

        btnFull.isSelected = !isWireframeMode
        btnRusuk.isSelected = isWireframeMode

        btnFull.setTextColor(if (!isWireframeMode) getColor(R.color.white) else inactiveColor)
        btnRusuk.setTextColor(if (isWireframeMode) getColor(R.color.white) else inactiveColor)
    }

    private fun refreshModels() {
        // Replace all existing models with the new mode version
        val currentNodes = arSceneView.children.filterIsInstance<ArModelNode>()
        currentNodes.forEach { node ->
            val anchor = node.anchor
            arSceneView.removeChild(node)
            if (anchor != null) {
                placeModelAt(anchor)
            }
        }
    }

    private fun resetAR() {
        clearAllObjects()
        Toast.makeText(this, "AR Reset", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllObjects() {
        arSceneView.children.toList().forEach { 
            if (it is ArModelNode) arSceneView.removeChild(it)
        }
        placedMarkerModels.clear()
        isObjectRendered = false
    }

    override fun onResume() {
        super.onResume()
        Log.d("RenderDebug", "onResume called in ARActivity, isObjectRendered = $isObjectRendered")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RenderDebug", "onDestroy called in ARActivity")
        clearAllObjects()
        Log.d("RenderDebug", "Objects cleared at ${System.currentTimeMillis()}, isObjectRendered = false")
    }

    private fun placeModelAt(anchor: com.google.ar.core.Anchor) {
        val paths = modelMap[currentShapeType] ?: return
        val modelPath = if (isWireframeMode) paths.second else paths.first

        val modelNode = ArModelNode(arSceneView.engine).apply {
            this.anchor = anchor
            loadModelGlbAsync(
                glbFileLocation = modelPath,
                autoAnimate = true,
                scaleToUnits = 0.2f
            )
        }
        arSceneView.addChild(modelNode)
        isObjectRendered = true
        Log.d("RenderDebug", "Object created at ${System.currentTimeMillis()}, isObjectRendered = true")
    }

    private fun setupFrameListener() {
        arSceneView.onArFrame = { arFrame ->
            processPlaneDetection(arFrame.frame)
            processLightEstimation(arFrame.frame)
            processAugmentedImages(arFrame.frame)
        }
    }

    private fun processAugmentedImages(frame: com.google.ar.core.Frame) {
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (image in updatedAugmentedImages) {
            if (image.trackingState == TrackingState.TRACKING) {
                val markerName = image.name
                if (!placedMarkerModels.contains(markerName)) {
                    Log.d("ARActivity", "Marker terdeteksi: $markerName")
                    // Buat anchor tepat di tengah gambar marker
                    val anchor = image.createAnchor(image.centerPose)
                    placeModelAt(anchor)
                    placedMarkerModels.add(markerName)
                    
                    runOnUiThread {
                        Toast.makeText(this, "Marker terdeteksi!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processPlaneDetection(frame: com.google.ar.core.Frame) {
        val updatedPlanes = frame.getUpdatedTrackables(Plane::class.java)
        for (plane in updatedPlanes) {
            when (plane.trackingState) {
                TrackingState.TRACKING -> trackedPlaneCount++
                TrackingState.STOPPED -> trackedPlaneCount = maxOf(0, trackedPlaneCount - 1)
                else -> {}
            }
        }

        val hasActivePlane = trackedPlaneCount > 0
        if (hasActivePlane && !hasPlaneBeenDetected) {
            hasPlaneBeenDetected = true
            runOnUiThread { coachingOverlay.updateTrackingStatus(ArCoachingOverlay.TrackingStatus.FOUND) }
        } else if (!hasActivePlane && hasPlaneBeenDetected) {
            hasPlaneBeenDetected = false
            runOnUiThread { coachingOverlay.updateTrackingStatus(ArCoachingOverlay.TrackingStatus.LOST) }
        }
    }

    private fun processLightEstimation(frame: com.google.ar.core.Frame) {
        val now = System.currentTimeMillis()
        if (now - lastLightingUpdate < 2000) return
        lastLightingUpdate = now

        val lightEstimate = frame.lightEstimate
        if (lightEstimate.state == com.google.ar.core.LightEstimate.State.VALID) {
            val pixelIntensity = lightEstimate.pixelIntensity
            val condition = when {
                pixelIntensity < 0.15f -> ArCoachingOverlay.LightingCondition.TOO_DARK
                pixelIntensity < 0.4f -> ArCoachingOverlay.LightingCondition.ADEQUATE
                else -> ArCoachingOverlay.LightingCondition.GOOD
            }
            runOnUiThread { coachingOverlay.updateLightingCondition(condition) }
        }
    }
}
