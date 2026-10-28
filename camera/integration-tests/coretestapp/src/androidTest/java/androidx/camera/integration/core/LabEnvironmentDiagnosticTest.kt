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

package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.applyCamera2InteropAsync
import androidx.camera.camera2.interop.cameraCharacteristics
import androidx.camera.camera2.interop.cameraId
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.LabTestUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-Demand Diagnostic Test Suite to troubleshoot CameraX lab environment issues (camera
 * orientation mismatch, device placement errors, defective fusion box light switches / relay
 * controllers, or blurry / out-of-focus camera hardware).
 *
 * Usage & Execution Guidelines:
 * 1. Manual / Agent Trigger Only: Tests in this class are annotated with [LabTestRule.LabTestOnly]
 *    and are intended to be triggered manually or by AI triage agents when troubleshooting
 *    suspected lab infra or hardware defects. They are excluded from daily automated suites.
 * 2. Multi-Camera Lighting & Torch Self-Illumination
 *    ([captureLabEnvironmentPhotosAndCheckLighting]): Captures frames from BOTH
 *    [CameraSelector.LENS_FACING_BACK] and [CameraSelector.LENS_FACING_FRONT] cameras without
 *    throwing hard assertion failures on dark frames. When a camera with a flash unit
 *    (`hasFlashUnit()`) sees a dark frame (`!isLightON`), it automatically enables the camera torch
 *    (`enableTorch(true)`) and saves a follow-up `${testName}_torch_on.png` frame to help
 *    distinguish whether the camera faces the test chart inside an unlit box (dead LED strip or USB
 *    relay controller) versus being obstructed or misaligned.
 * 3. Optical Focus & ImageCapture Max-Quality vs. Manual Diopter Sweep
 *    ([diagnoseFocusAndImageCaptureQuality]): When a box is lit and the camera faces the chart but
 *    vision/barcode tests fail due to blurriness, captures [ImageCapture]
 *    ([ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY]) frames with Auto AF and explicit center
 *    [FocusMeteringAction], followed by an [ImageAnalysis] Auto-AF baseline (`ia_auto_af`) and
 *    manual [CaptureRequest.LENS_FOCUS_DISTANCE] diopter sweep (`0.0f..minFocusDist`) using the
 *    same target resolution and aspect ratio. Logs `[LAB_FOCUS_DIAGNOSTIC]` luminance and gradient
 *    sharpness metrics and saves diagnostic bitmaps to isolate CameraX/HAL autofocus issues from
 *    physical lens obstruction, mount distance, or VCM actuator defects.
 *
 * Execution Examples:
 * - On-Demand Lab Run: See `AGENTS_INTERNAL.md` for lab test runner commands.
 * - Local / ADB (requires `adb shell setprop log.tag.MH DEBUG` to satisfy [LabTestRule]):
 *   `./gradlew :camera:integration-tests:camera-testapp-core:connectedDebugAndroidTest \`
 *   `-Pandroid.testInstrumentationRunnerArguments.class=\`
 *   `androidx.camera.integration.core.LabEnvironmentDiagnosticTest`
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LabEnvironmentDiagnosticTest {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule
    val storageRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule val labTest: LabTestRule = LabTestRule()

    companion object {
        private const val TAG = "LabDiagnosticTest"
        private const val FRAME_TIMEOUT_MS = 10_000L
        private const val IMAGE_CAPTURE_TIMEOUT_MS = 15_000L
        private const val PREVIEW_WARMUP_FRAMES = 15
        private const val SETTLE_FRAMES_AFTER_CONTROL_CHANGE = 8
        private const val MAX_SAVED_BITMAP_WIDTH = 1920
        private val TARGET_RESOLUTION = Size(1280, 720)
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setup(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::fakeLifecycleOwner.isInitialized) {
            withContext(Dispatchers.Main) {
                fakeLifecycleOwner.pauseAndStop()
                fakeLifecycleOwner.destroy()
            }
        }
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync().get(10, TimeUnit.SECONDS)
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun captureLabEnvironmentPhotosAndCheckLighting() {
        runBlocking {
            val rearResult =
                runCameraDiagnostic(
                    lensFacing = CameraSelector.LENS_FACING_BACK,
                    testName = "lab_diagnostic_rear_camera",
                    label = "Rear Camera",
                )
            val frontResult =
                runCameraDiagnostic(
                    lensFacing = CameraSelector.LENS_FACING_FRONT,
                    testName = "lab_diagnostic_front_camera",
                    label = "Front Camera",
                )

            val rearSummary = rearResult.toSummary("Rear Camera")
            val frontSummary = frontResult.toSummary("Front Camera")

            Log.i(
                TAG,
                "[LAB_DIAGNOSTIC_SUMMARY] Diagnostic run complete.\n" +
                    "  $rearSummary\n" +
                    "  $frontSummary",
            )
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun diagnoseFocusAndImageCaptureQuality() {
        runBlocking {
            val summaries = mutableListOf<String>()
            summaries.add(
                runFocusDiagnosticForLens(
                    lensFacing = CameraSelector.LENS_FACING_BACK,
                    prefix = "rear",
                    label = "Rear Camera",
                )
            )
            summaries.add(
                runFocusDiagnosticForLens(
                    lensFacing = CameraSelector.LENS_FACING_FRONT,
                    prefix = "front",
                    label = "Front Camera",
                )
            )
            Log.i(
                TAG,
                "[LAB_FOCUS_DIAGNOSTIC_SUMMARY] Focus & ImageCapture quality run complete.\n" +
                    summaries.joinToString("\n"),
            )
        }
    }

    private data class FrameLightingMetrics(
        val luminance: Double = 0.0,
        val isLightON: Boolean = false,
    )

    private data class DiagnosticResult(
        val captured: Boolean,
        val luminance: Double,
        val isLightON: Boolean,
        val torchCaptured: Boolean = false,
        val torchLuminance: Double = 0.0,
        val torchLightON: Boolean = false,
    ) {
        fun toSummary(label: String): String {
            val basePart =
                String.format(
                    Locale.US,
                    "%s Frame Captured: %s | Luminance: %.2f / 255.0 | Light PASS: %s",
                    label,
                    captured,
                    luminance,
                    isLightON,
                )
            if (!torchCaptured) return basePart
            val torchPart =
                String.format(
                    Locale.US,
                    " | Torch-ON Luminance: %.2f / 255.0 (Light PASS: %s)",
                    torchLuminance,
                    torchLightON,
                )
            return basePart + torchPart
        }
    }

    private fun buildTargetResolutionSelector(): ResolutionSelector =
        ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    TARGET_RESOLUTION,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
            )
            .build()

    private suspend fun runCameraDiagnostic(
        @CameraSelector.LensFacing lensFacing: Int,
        testName: String,
        label: String,
    ): DiagnosticResult {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        if (!cameraProvider.hasCamera(cameraSelector)) {
            Log.w(TAG, "Device does not have $label, skipping $label diagnostic.")
            return DiagnosticResult(captured = false, luminance = 0.0, isLightON = false)
        }

        val imageAnalysis =
            ImageAnalysis.Builder()
                .setResolutionSelector(buildTargetResolutionSelector())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        val initialFrameLatch = CountDownLatch(1)
        val torchFrameLatch = CountDownLatch(1)
        val initialSettleRemaining = AtomicInteger(SETTLE_FRAMES_AFTER_CONTROL_CHANGE)
        val initialFrameSaved = AtomicBoolean(false)
        val torchCaptureEnabled = AtomicBoolean(false)
        val torchSettleRemaining = AtomicInteger(SETTLE_FRAMES_AFTER_CONTROL_CHANGE)
        val torchFrameSaved = AtomicBoolean(false)
        val initialMetricsRef = AtomicReference(FrameLightingMetrics())
        val torchMetricsRef = AtomicReference(FrameLightingMetrics())

        imageAnalysis.setAnalyzer(CameraXExecutors.ioExecutor()) { imageProxy ->
            imageProxy.use { proxy ->
                if (
                    !torchCaptureEnabled.get() &&
                        initialSettleRemaining.decrementAndGet() <= 0 &&
                        initialFrameSaved.compareAndSet(false, true)
                ) {
                    try {
                        val bitmap = decodeBoundedBitmap(proxy)
                        try {
                            val lum = LabTestUtil.calculateBitmapLuminance(bitmap)
                            val lightOn =
                                LabTestUtil.checkLabEnvironmentLighting(
                                    bitmap,
                                    label = "$label ($testName)",
                                )
                            initialMetricsRef.set(FrameLightingMetrics(lum, lightOn))
                            val savedFile = LabTestUtil.saveTestBitmap(bitmap, testName)
                            val savedPath = savedFile?.absolutePath ?: "skipped"
                            Log.i(
                                TAG,
                                String.format(
                                    Locale.US,
                                    "[LAB_DIAGNOSTIC] %s Frame Saved: %s | " +
                                        "Luminance: %.2f | Lighting PASS: %s",
                                    label,
                                    savedPath,
                                    lum,
                                    lightOn,
                                ),
                            )
                        } finally {
                            bitmap.recycle()
                        }
                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "[LAB_DIAGNOSTIC] OOM analyzing $label frame", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "[LAB_DIAGNOSTIC] Failed to analyze $label frame", e)
                    } finally {
                        initialFrameLatch.countDown()
                    }
                } else if (
                    torchCaptureEnabled.get() &&
                        torchSettleRemaining.decrementAndGet() <= 0 &&
                        torchFrameSaved.compareAndSet(false, true)
                ) {
                    try {
                        val bitmap = decodeBoundedBitmap(proxy)
                        try {
                            val torchTestName = "${testName}_torch_on"
                            val lum = LabTestUtil.calculateBitmapLuminance(bitmap)
                            val lightOn =
                                LabTestUtil.checkLabEnvironmentLighting(
                                    bitmap,
                                    label = "$label Torch-ON ($torchTestName)",
                                )
                            torchMetricsRef.set(FrameLightingMetrics(lum, lightOn))
                            val savedFile = LabTestUtil.saveTestBitmap(bitmap, torchTestName)
                            val savedPath = savedFile?.absolutePath ?: "skipped"
                            Log.i(
                                TAG,
                                String.format(
                                    Locale.US,
                                    "[LAB_DIAGNOSTIC] %s Torch-ON Frame Saved: %s | " +
                                        "Luminance: %.2f | Lighting PASS: %s",
                                    label,
                                    savedPath,
                                    lum,
                                    lightOn,
                                ),
                            )
                        } finally {
                            bitmap.recycle()
                        }
                    } catch (e: OutOfMemoryError) {
                        Log.e(TAG, "[LAB_DIAGNOSTIC] OOM analyzing $label torch-on frame", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "[LAB_DIAGNOSTIC] Failed to analyze $label torch-on frame", e)
                    } finally {
                        torchFrameLatch.countDown()
                    }
                }
            }
        }

        var torchEnabled = false
        var camera: Camera? = null
        try {
            camera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        fakeLifecycleOwner,
                        cameraSelector,
                        imageAnalysis,
                    )
                }

            val success = initialFrameLatch.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!success) {
                Log.w(TAG, "[LAB_DIAGNOSTIC] Timed out waiting for $label frame capture.")
            }

            val initialMetrics = initialMetricsRef.get()
            var torchCaptured = false
            if (success && !initialMetrics.isLightON && camera.cameraInfo.hasFlashUnit()) {
                try {
                    Log.i(
                        TAG,
                        "[LAB_DIAGNOSTIC] $label frame is dark and flash unit is available; " +
                            "enabling torch to test self-illumination.",
                    )
                    torchEnabled = true
                    camera.cameraControl.enableTorch(true).get(5, TimeUnit.SECONDS)
                    torchCaptureEnabled.set(true)
                    torchCaptured = torchFrameLatch.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    if (!torchCaptured) {
                        Log.w(
                            TAG,
                            "[LAB_DIAGNOSTIC] Timed out waiting for $label torch-on frame capture.",
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[LAB_DIAGNOSTIC] Failed to capture $label torch-on frame", e)
                }
            }

            val torchMetrics = torchMetricsRef.get()
            return DiagnosticResult(
                captured = success,
                luminance = initialMetrics.luminance,
                isLightON = initialMetrics.isLightON,
                torchCaptured = torchCaptured,
                torchLuminance = torchMetrics.luminance,
                torchLightON = torchMetrics.isLightON,
            )
        } finally {
            imageAnalysis.clearAnalyzer()
            if (torchEnabled && camera != null) {
                runCatching { camera.cameraControl.enableTorch(false).get(5, TimeUnit.SECONDS) }
            }
            withContext(Dispatchers.Main) { cameraProvider.unbind(imageAnalysis) }
        }
    }

    private data class SharpnessMetrics(
        val captured: Boolean,
        val width: Int = 0,
        val height: Int = 0,
        val luminance: Double = 0.0,
        val centerSharpness: Double = 0.0,
        val failureReason: String = "timeout",
    ) {
        fun toSummaryToken(
            name: String,
            includeResolution: Boolean = false,
            extraPrefix: String? = null,
        ): String {
            val prefixPart = if (extraPrefix != null) "$extraPrefix " else ""
            if (!captured) {
                return "$name($prefixPart$failureReason)"
            }
            val resPart = if (includeResolution) "${width}x${height} " else ""
            return String.format(
                Locale.US,
                "%s(%s%slum=%.1f sharp=%.3f)",
                name,
                prefixPart,
                resPart,
                luminance,
                centerSharpness,
            )
        }
    }

    private class ManualFocusStepCapture(
        val testName: String,
        val stepLabel: String,
        val settleRemaining: AtomicInteger = AtomicInteger(SETTLE_FRAMES_AFTER_CONTROL_CHANGE),
        val claimed: AtomicBoolean = AtomicBoolean(false),
        val latch: CountDownLatch = CountDownLatch(1),
        val metricsRef: AtomicReference<SharpnessMetrics> =
            AtomicReference(SharpnessMetrics(captured = false)),
    )

    private suspend fun runFocusDiagnosticForLens(
        @CameraSelector.LensFacing lensFacing: Int,
        prefix: String,
        label: String,
    ): String {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        if (!cameraProvider.hasCamera(cameraSelector)) {
            return "  $label: not available on device"
        }

        val previewWarmupLatch = CountDownLatch(PREVIEW_WARMUP_FRAMES)
        val preview =
            Preview.Builder().build().apply {
                withContext(Dispatchers.Main) {
                    surfaceProvider =
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                            previewWarmupLatch.countDown()
                        }
                }
            }
        val imageCapture =
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(buildTargetResolutionSelector())
                .build()

        val minFocusDist: Float
        val hyperfocalDist: Float
        val supportsManualFocusOff: Boolean
        val cameraId: String
        val results = mutableListOf<String>()
        var camera: Camera? = null

        try {
            camera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        fakeLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                    )
                }
            cameraId = camera.cameraInfo.cameraId
            val characteristics = camera.cameraInfo.cameraCharacteristics
            minFocusDist =
                characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            hyperfocalDist =
                characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE) ?: 0f
            val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            supportsManualFocusOff = afModes?.contains(CaptureRequest.CONTROL_AF_MODE_OFF) ?: false

            Log.i(
                TAG,
                "[LAB_FOCUS_DIAGNOSTIC] $label (camId=$cameraId): " +
                    "minFocusDist=$minFocusDist, hyperfocal=$hyperfocalDist, " +
                    "afModes=${afModes?.contentToString()}",
            )

            previewWarmupLatch.await(5, TimeUnit.SECONDS)

            val autoAfMetrics =
                takePictureAndMeasureSharpness(
                    imageCapture = imageCapture,
                    testName = "lab_focus_diag_${prefix}_ic_auto_af",
                    label = "$label IC MaxQuality AutoAF",
                )
            results.add(autoAfMetrics.toSummaryToken("ic_auto_af", includeResolution = true))

            if (minFocusDist > 0f) {
                val point = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
                val action =
                    FocusMeteringAction.Builder(
                            point,
                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                        )
                        .disableAutoCancel()
                        .build()
                val afSuccess =
                    try {
                        camera.cameraControl
                            .startFocusAndMetering(action)
                            .get(5, TimeUnit.SECONDS)
                            .isFocusSuccessful
                    } catch (e: Exception) {
                        Log.w(TAG, "[LAB_FOCUS_DIAGNOSTIC] $label center AF failed: ${e.message}")
                        false
                    }
                val centerAfMetrics =
                    takePictureAndMeasureSharpness(
                        imageCapture = imageCapture,
                        testName = "lab_focus_diag_${prefix}_ic_center_af",
                        label = "$label IC MaxQuality CenterAF(locked=$afSuccess)",
                    )
                results.add(
                    centerAfMetrics.toSummaryToken(
                        "ic_center_af",
                        extraPrefix = "locked=$afSuccess",
                    )
                )
            }
        } finally {
            if (camera != null) {
                runCatching {
                    camera.cameraControl.cancelFocusAndMetering().get(5, TimeUnit.SECONDS)
                }
            }
            withContext(Dispatchers.Main) { cameraProvider.unbind(preview, imageCapture) }
        }

        if (minFocusDist > 0f && supportsManualFocusOff) {
            val diopters =
                listOf(0.0f, 2.0f, 4.0f, 7.0f, minFocusDist)
                    .filter { it <= minFocusDist + 0.01f }
                    .distinctBy { (it * 10).toInt() }
            val sweepResults =
                runManualFocusDiopterSweep(
                    cameraSelector = cameraSelector,
                    prefix = prefix,
                    label = label,
                    diopters = diopters,
                )
            results.addAll(sweepResults)
        } else if (minFocusDist <= 0f) {
            results.add("manual_sweep(skipped:fixed_focus)")
        } else {
            results.add("manual_sweep(skipped:no_AF_MODE_OFF)")
        }

        return "  $label (camId=$cameraId, minFocusDist=$minFocusDist, " +
            "hyperfocal=$hyperfocalDist): ${results.joinToString(" | ")}"
    }

    private fun takePictureAndMeasureSharpness(
        imageCapture: ImageCapture,
        testName: String,
        label: String,
    ): SharpnessMetrics {
        val latch = CountDownLatch(1)
        val metricsRef = AtomicReference(SharpnessMetrics(captured = false))

        imageCapture.takePicture(
            CameraXExecutors.ioExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.use { proxy ->
                        try {
                            val bitmap = decodeBoundedBitmap(proxy)
                            try {
                                metricsRef.set(
                                    analyzeAndSaveDiagnosticBitmap(bitmap, testName, label)
                                )
                            } finally {
                                bitmap.recycle()
                            }
                        } catch (e: OutOfMemoryError) {
                            metricsRef.set(
                                SharpnessMetrics(captured = false, failureReason = "oom")
                            )
                            Log.e(TAG, "[LAB_FOCUS_DIAGNOSTIC] OOM processing $label", e)
                        } catch (e: Exception) {
                            metricsRef.set(
                                SharpnessMetrics(captured = false, failureReason = "error")
                            )
                            Log.e(TAG, "[LAB_FOCUS_DIAGNOSTIC] Failed to process $label", e)
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    metricsRef.set(SharpnessMetrics(captured = false, failureReason = "error"))
                    Log.e(
                        TAG,
                        "[LAB_FOCUS_DIAGNOSTIC] takePicture failed for $label: " +
                            exception.message,
                        exception,
                    )
                    latch.countDown()
                }
            },
        )

        if (!latch.await(IMAGE_CAPTURE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Log.w(TAG, "[LAB_FOCUS_DIAGNOSTIC] Timed out waiting for $label takePicture.")
        }
        return metricsRef.get()
    }

    /**
     * Captures an initial [ImageAnalysis] Auto-AF baseline frame (`ia_auto_af`) followed by manual
     * [CaptureRequest.LENS_FOCUS_DISTANCE] diopter steps on the same [ImageAnalysis] stream so that
     * Auto-AF and manual-focus sharpness scores share identical resolution, field of view, and YUV
     * processing pipeline.
     */
    private suspend fun runManualFocusDiopterSweep(
        cameraSelector: CameraSelector,
        prefix: String,
        label: String,
        diopters: List<Float>,
    ): List<String> {
        val imageAnalysis =
            ImageAnalysis.Builder()
                .setResolutionSelector(buildTargetResolutionSelector())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        val activeStepRef = AtomicReference<ManualFocusStepCapture?>(null)

        imageAnalysis.setAnalyzer(CameraXExecutors.ioExecutor()) { imageProxy ->
            imageProxy.use { proxy ->
                val step = activeStepRef.get() ?: return@use
                if (
                    step.settleRemaining.decrementAndGet() <= 0 &&
                        step.claimed.compareAndSet(false, true)
                ) {
                    activeStepRef.compareAndSet(step, null)
                    try {
                        val bitmap = decodeBoundedBitmap(proxy)
                        try {
                            step.metricsRef.set(
                                analyzeAndSaveDiagnosticBitmap(
                                    bitmap,
                                    step.testName,
                                    step.stepLabel,
                                )
                            )
                        } finally {
                            bitmap.recycle()
                        }
                    } catch (e: OutOfMemoryError) {
                        step.metricsRef.set(
                            SharpnessMetrics(captured = false, failureReason = "oom")
                        )
                        Log.e(
                            TAG,
                            "[LAB_FOCUS_DIAGNOSTIC] OOM in focus capture ${step.stepLabel}",
                            e,
                        )
                    } catch (e: Exception) {
                        step.metricsRef.set(
                            SharpnessMetrics(captured = false, failureReason = "error")
                        )
                        Log.e(
                            TAG,
                            "[LAB_FOCUS_DIAGNOSTIC] Failed manual focus capture ${step.stepLabel}",
                            e,
                        )
                    } finally {
                        step.latch.countDown()
                    }
                }
            }
        }

        val results = mutableListOf<String>()
        var camera: Camera? = null
        try {
            camera =
                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        fakeLifecycleOwner,
                        cameraSelector,
                        imageAnalysis,
                    )
                }

            val autoAfStep =
                ManualFocusStepCapture(
                    testName = "lab_focus_diag_${prefix}_ia_auto_af",
                    stepLabel = "$label IA AutoAF",
                    settleRemaining = AtomicInteger(PREVIEW_WARMUP_FRAMES),
                )
            activeStepRef.set(autoAfStep)
            val autoCaptured = autoAfStep.latch.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            activeStepRef.compareAndSet(autoAfStep, null)
            if (!autoCaptured) {
                Log.w(
                    TAG,
                    "[LAB_FOCUS_DIAGNOSTIC] Timed out waiting for ${autoAfStep.stepLabel} frame.",
                )
            }
            results.add(
                autoAfStep.metricsRef.get().toSummaryToken("ia_auto_af", includeResolution = true)
            )

            for (diopter in diopters) {
                val diopterTag = String.format(Locale.US, "%.1fD", diopter).replace('.', '_')
                val stepLabel = String.format(Locale.US, "%s ManualFocus %.1fD", label, diopter)
                val step =
                    ManualFocusStepCapture(
                        testName = "lab_focus_diag_${prefix}_manual_${diopterTag}",
                        stepLabel = stepLabel,
                    )

                val applied = runCatching {
                    camera.cameraControl
                        .applyCamera2InteropAsync {
                            setCaptureRequestOption(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_OFF,
                            )
                            setCaptureRequestOption(
                                CaptureRequest.LENS_FOCUS_DISTANCE,
                                diopter,
                            )
                        }
                        .get(5, TimeUnit.SECONDS)
                }
                    .onFailure { e ->
                        Log.w(
                            TAG,
                            "[LAB_FOCUS_DIAGNOSTIC] Failed to set manual focus $stepLabel",
                            e,
                        )
                    }
                    .isSuccess

                if (!applied) {
                    results.add(String.format(Locale.US, "manual_%.1fD(apply_failed)", diopter))
                    continue
                }

                activeStepRef.set(step)
                val captured = step.latch.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                activeStepRef.compareAndSet(step, null)
                if (!captured) {
                    Log.w(
                        TAG,
                        "[LAB_FOCUS_DIAGNOSTIC] Timed out waiting for $stepLabel frame.",
                    )
                }
                results.add(
                    step.metricsRef
                        .get()
                        .toSummaryToken(String.format(Locale.US, "manual_%.1fD", diopter))
                )
            }
        } finally {
            imageAnalysis.clearAnalyzer()
            if (camera != null) {
                runCatching {
                    camera.cameraControl
                        .applyCamera2InteropAsync { clearAllCaptureRequestOptions() }
                        .get(5, TimeUnit.SECONDS)
                }
            }
            withContext(Dispatchers.Main) { cameraProvider.unbind(imageAnalysis) }
        }
        return results
    }

    /**
     * Decodes [proxy] into a [Bitmap] while downsampling oversized JPEG frames via
     * [BitmapFactory.Options.inSampleSize] before full heap allocation so low-memory lab devices do
     * not hit [OutOfMemoryError].
     */
    private fun decodeBoundedBitmap(proxy: ImageProxy): Bitmap {
        if (
            proxy.format == ImageFormat.JPEG &&
                max(proxy.width, proxy.height) > MAX_SAVED_BITMAP_WIDTH
        ) {
            val buffer = proxy.planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            val srcWidth = if (boundsOptions.outWidth > 0) boundsOptions.outWidth else proxy.width
            val srcHeight =
                if (boundsOptions.outHeight > 0) boundsOptions.outHeight else proxy.height
            var sampleSize = 1
            while (
                max(srcWidth / sampleSize, srcHeight / sampleSize) > MAX_SAVED_BITMAP_WIDTH &&
                    max(srcWidth / (sampleSize * 2), srcHeight / (sampleSize * 2)) >=
                        max(TARGET_RESOLUTION.width, TARGET_RESOLUTION.height)
            ) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: throw IllegalStateException(
                    "Failed to decode JPEG (${proxy.width}x${proxy.height}, sampleSize=$sampleSize)"
                )
        }
        return proxy.toBitmap()
    }

    private fun analyzeAndSaveDiagnosticBitmap(
        bitmap: Bitmap,
        testName: String,
        label: String,
    ): SharpnessMetrics {
        val luminance = LabTestUtil.calculateBitmapLuminance(bitmap)
        val sharpness = calculateCenterGradientSharpness(bitmap)
        val maxDim = max(bitmap.width, bitmap.height)
        val bitmapToSave =
            if (maxDim > MAX_SAVED_BITMAP_WIDTH) {
                val scaledWidth =
                    max(1, (bitmap.width.toLong() * MAX_SAVED_BITMAP_WIDTH / maxDim).toInt())
                val scaledHeight =
                    max(1, (bitmap.height.toLong() * MAX_SAVED_BITMAP_WIDTH / maxDim).toInt())
                Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            } else {
                bitmap
            }
        try {
            val savedFile = LabTestUtil.saveTestBitmap(bitmapToSave, testName)
            val savedPath = savedFile?.absolutePath ?: "skipped"
            Log.i(
                TAG,
                String.format(
                    Locale.US,
                    "[LAB_FOCUS_DIAGNOSTIC] %s (%dx%d) -> " +
                        "Luminance: %.2f | CenterSharpness: %.3f | Saved: %s",
                    label,
                    bitmap.width,
                    bitmap.height,
                    luminance,
                    sharpness,
                    savedPath,
                ),
            )
        } finally {
            if (bitmapToSave !== bitmap) {
                bitmapToSave.recycle()
            }
        }
        return SharpnessMetrics(
            captured = true,
            width = bitmap.width,
            height = bitmap.height,
            luminance = luminance,
            centerSharpness = sharpness,
        )
    }

    /**
     * Computes mean Tenengrad-style horizontal + vertical luma gradient magnitude across the center
     * 50% ROI of [bitmap] to quantify optical focus sharpness independent of sensor resolution.
     */
    private fun calculateCenterGradientSharpness(bitmap: Bitmap): Double {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 8 || h < 8) return 0.0
        val startX = w / 4
        val endX = (w * 3) / 4 - 1
        val startY = h / 4
        val endY = (h * 3) / 4 - 1
        val step = max(1, min(w, h) / 300)
        var sumGrad = 0.0
        var count = 0L
        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val lumC = pixelLuma(bitmap.getPixel(x, y))
                val lumR = pixelLuma(bitmap.getPixel(min(w - 1, x + step), y))
                val lumD = pixelLuma(bitmap.getPixel(x, min(h - 1, y + step)))
                sumGrad += abs(lumR - lumC) + abs(lumD - lumC)
                count++
                x += step
            }
            y += step
        }
        return if (count > 0L) sumGrad / count else 0.0
    }

    private fun pixelLuma(pixel: Int): Double {
        return 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
    }
}
