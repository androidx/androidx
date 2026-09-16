/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.arcore.testapp.helloar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.SpatialAnnotation
import androidx.xr.arcore.SpatialAnnotationId
import androidx.xr.arcore.SpatialAnnotationImageFormat
import androidx.xr.arcore.SpatialAnnotationTrackingOptions
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.helloar.rendering.SpatialAnnotationRenderer
import androidx.xr.arcore.testapp.helloar.ui.CameraFrameAnalyzer
import androidx.xr.arcore.testapp.helloar.ui.CameraPreviewScreen
import androidx.xr.arcore.testapp.helloar.ui.FrameSnapshot
import androidx.xr.arcore.testapp.helloar.ui.ViewfinderTrackingConfig
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SpatialAnnotationTrackingMode
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Quad
import androidx.xr.runtime.math.Vector2
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalSpatialAnnotationsApi::class)
class HelloArSpatialAnnotationActivity : ComponentActivity() {
    private var session: Session? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var cameraFrameAnalyzer: CameraFrameAnalyzer

    private var cameraProvider: ProcessCameraProvider? = null
    private val resolutionSelector =
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(
                        CameraFrameAnalyzer.DEFAULT_FRAME_WIDTH,
                        CameraFrameAnalyzer.DEFAULT_FRAME_HEIGHT,
                    ),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
            )
            .build()
    private val cameraPreviewUseCase =
        Preview.Builder().setResolutionSelector(resolutionSelector).build()
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var spatialAnnotationRenderer: SpatialAnnotationRenderer

    private var lastSnapshot: FrameSnapshot? = null
    var isTrackingStarted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraFrameAnalyzer = CameraFrameAnalyzer { width, height ->
            Log.d("HelloAr", "Camera resolution updated: ${width}x${height}")
        }

        spatialAnnotationRenderer =
            SpatialAnnotationRenderer(this, onTrackingStopped = ::onTrackingStopped)

        sessionHelper =
            SessionLifecycleHelper(
                activity = this,
                config =
                    Config.Builder()
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .setSpatialAnnotationTracking(SpatialAnnotationTrackingMode.QUAD)
                        .build(),
                onSessionAvailable = { s ->
                    session = s
                    spatialAnnotationRenderer.startRendering(s, lifecycleScope)
                    setContent {
                        Subspace {
                            if (!isTrackingStarted) {
                                SpatialPanel(
                                    modifier =
                                        SubspaceModifier.size(DpVolumeSize(1280.dp, 720.dp, 0.dp))
                                            .movable()
                                            .resizable()
                                ) {
                                    CameraPreviewScreen(
                                        cameraPreviewUseCase = cameraPreviewUseCase,
                                        // TODO(b/561608504): Support dynamic camera resolution and
                                        // back
                                        // activeFrameWidth/Height with Compose state.
                                        activeFrameWidth =
                                            CameraFrameAnalyzer.DEFAULT_FRAME_WIDTH.toFloat(),
                                        activeFrameHeight =
                                            CameraFrameAnalyzer.DEFAULT_FRAME_HEIGHT.toFloat(),
                                        onStartTrackingClick = ::onStartTrackingClicked,
                                    )
                                }
                            }
                        }
                    }
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { bindCameraUseCases() }
    }

    override fun onDestroy() {
        super.onDestroy()
        val s = session
        if (isFinishing && s != null) {
            try {
                SpatialAnnotation.stopTrackingAllAnnotations(s)
            } catch (e: Exception) {}
        }
        spatialAnnotationRenderer.stopRendering()
        cameraExecutor.shutdown()
        lastSnapshot = null
        if (::cameraFrameAnalyzer.isInitialized) {
            cameraFrameAnalyzer.clearBuffers()
        }
    }

    private suspend fun bindCameraUseCases() {
        try {
            val provider = ProcessCameraProvider.getInstance(this).await()
            cameraProvider = provider

            val imageAnalysis =
                ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

            imageAnalysis.setAnalyzer(cameraExecutor, cameraFrameAnalyzer)

            // TODO(b/561608250): Unbind the camera use cases when tracking starts.
            provider.unbindAll()
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                cameraPreviewUseCase,
                imageAnalysis,
            )
        } catch (e: Exception) {
            Log.e("HelloAr", "Failed to bind camera", e)
        }
    }

    private fun onStartTrackingClicked(config: ViewfinderTrackingConfig) {
        val currentSession = session ?: return

        spatialAnnotationRenderer.isDotCenter = config.isDotCenter
        spatialAnnotationRenderer.isDotTop = config.isDotTop

        val snapshot = cameraFrameAnalyzer.copyLatestFrame()
        if (snapshot == null) {
            Toast.makeText(this, "Waiting for camera frame...", Toast.LENGTH_SHORT).show()
            return
        }

        lastSnapshot = snapshot // Prevent GC of native memory while C++ processes asynchronously!
        val effectiveWidth =
            if (snapshot.isRotated) config.boxHeight.toFloat() else config.boxWidth.toFloat()
        val effectiveHeight =
            if (snapshot.isRotated) config.boxWidth.toFloat() else config.boxHeight.toFloat()
        val coercedWidth = effectiveWidth.coerceIn(50f, snapshot.width.toFloat())
        val coercedHeight = effectiveHeight.coerceIn(50f, snapshot.height.toFloat())
        val w2 = coercedWidth / 2f
        val h2 = coercedHeight / 2f
        val centerX = snapshot.width.toFloat() / 2f
        val centerY = snapshot.height.toFloat() / 2f

        val quad =
            // TODO(b/561608415): Investigate possible misalignment on rotation.
            Quad.fromCorners(
                upperLeft = Vector2(centerX - w2, centerY - h2),
                upperRight = Vector2(centerX + w2, centerY - h2),
                lowerRight = Vector2(centerX + w2, centerY + h2),
                lowerLeft = Vector2(centerX - w2, centerY + h2),
            )

        if (isSnapshotSaveEnabled()) {
            lifecycleScope.launch(Dispatchers.IO) { saveSnapshotWithQuad(snapshot, quad) }
        }

        lifecycleScope.launch {
            try {
                if (isTrackingStarted) {
                    try {
                        // TODO(b/561608917): Stop previous tracking before capturing new snapshot.
                        SpatialAnnotation.stopTrackingAllAnnotations(currentSession)
                    } catch (e: Exception) {
                        Log.w("HelloAr", "Error stopping previous tracking before starting new", e)
                    }
                    isTrackingStarted = false
                }
                Log.d(
                    "XR_TESTAPP",
                    "TimestampNs: ${snapshot.timestampNs}\nNanoTime: ${System.nanoTime()}\nElapsed: ${SystemClock.elapsedRealtimeNanos()}",
                )
                val trackingOptions =
                    SpatialAnnotationTrackingOptions.Builder(
                            snapshot.buffer,
                            IntSize2d(snapshot.width, snapshot.height),
                            snapshot.timestampNs,
                        )
                        .setFormat(SpatialAnnotationImageFormat.GRAYSCALE)
                        .setAlignment(config.alignment)
                        .setQuads(mapOf(SpatialAnnotationId.fromString("target-1") to quad))
                        .build()

                SpatialAnnotation.startTracking(currentSession, trackingOptions)
                isTrackingStarted = true
                Toast.makeText(
                        this@HelloArSpatialAnnotationActivity,
                        "Tracking started!",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onTrackingStopped("Failed to start tracking: ${e.message}")
                Log.e("XR_TESTAPP", "Exception: ", e)
            }
        }
    }

    fun onTrackingStopped(message: String? = null) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!isTrackingStarted && lastSnapshot == null) return@launch
            if (message != null) {
                Toast.makeText(this@HelloArSpatialAnnotationActivity, message, Toast.LENGTH_SHORT)
                    .show()
            }
            val s = session
            if (s != null && isTrackingStarted) {
                try {
                    SpatialAnnotation.stopTrackingAllAnnotations(s)
                } catch (e: Exception) {
                    Log.w("HelloAr", "Error stopping tracking", e)
                }
            }
            isTrackingStarted = false
            lastSnapshot = null
            if (::cameraFrameAnalyzer.isInitialized) {
                cameraFrameAnalyzer.clearBuffers()
            }
        }
    }

    private fun isSnapshotSaveEnabled(): Boolean {
        return intent.getBooleanExtra("save_snapshot", false)
    }

    private fun saveSnapshotWithQuad(snapshot: FrameSnapshot, quad: Quad) {
        try {
            val bitmap =
                Bitmap.createBitmap(
                    snapshot.width,
                    snapshot.height,
                    Bitmap.Config.ARGB_8888,
                )
            val pixels = IntArray(snapshot.width * snapshot.height)
            val buf = snapshot.buffer.asReadOnlyBuffer()
            buf.rewind()
            val total = minOf(pixels.size, buf.remaining())
            for (i in 0 until total) {
                val y = buf.get().toInt() and 0xFF
                pixels[i] = (0xFF shl 24) or (y shl 16) or (y shl 8) or y
            }
            bitmap.setPixels(pixels, 0, snapshot.width, 0, 0, snapshot.width, snapshot.height)
            val canvas = Canvas(bitmap)
            val paint =
                Paint().apply {
                    color = Color.GREEN
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
            canvas.drawLine(
                quad.upperLeft.x,
                quad.upperLeft.y,
                quad.upperRight.x,
                quad.upperRight.y,
                paint,
            )
            canvas.drawLine(
                quad.upperRight.x,
                quad.upperRight.y,
                quad.lowerRight.x,
                quad.lowerRight.y,
                paint,
            )
            canvas.drawLine(
                quad.lowerRight.x,
                quad.lowerRight.y,
                quad.lowerLeft.x,
                quad.lowerLeft.y,
                paint,
            )
            canvas.drawLine(
                quad.lowerLeft.x,
                quad.lowerLeft.y,
                quad.upperLeft.x,
                quad.upperLeft.y,
                paint,
            )

            val dir = getExternalFilesDir(null) ?: filesDir
            val file = File(dir, "captured_target.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.i("HelloAr", "Saved captured target to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("HelloAr", "Failed to save snapshot with quad", e)
        }
    }
}
