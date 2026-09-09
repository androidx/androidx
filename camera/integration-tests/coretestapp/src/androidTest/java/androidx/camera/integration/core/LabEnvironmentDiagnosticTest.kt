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
import android.util.Log
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.LabTestUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
 * orientation mismatch, device placement errors, or defective fusion box light switches).
 *
 * Usage & Execution Guidelines:
 * 1. Manual / Agent Trigger Only: This test is annotated with [@LabTestRule.LabTestOnly] and is
 *    intended to be triggered manually or by AI triage agents when troubleshooting suspected lab
 *    infra defects across lab devices. It is excluded from daily automated test suites.
 * 2. Multi-Camera Diagnostic Capture: Attempts to capture test frames from BOTH
 *    [CameraSelector.LENS_FACING_BACK] and [CameraSelector.LENS_FACING_FRONT] cameras on the
 *    device.
 * 3. Non-Failing Image Dumps: Does NOT throw hard assertion failures if one camera produces a dark
 *    or pitch-black frame (since lab fixtures typically align only one camera to the test chart).
 *    Instead, the test logs structured `[LAB_DIAGNOSTIC]` summaries and saves test bitmaps via
 *    [LabTestUtil.saveTestBitmap] for automated test runners to extract
 *    (`lab_diagnostic_rear_camera.png` and `lab_diagnostic_front_camera.png`).
 * 4. Post-Run AI / Human Verification: Triage agents or developers should inspect the exported
 *    photos and luminance logs in test artifacts to determine:
 *     - Whether the device camera orientation configuration matches physical box alignment.
 *     - Whether the fusion box light switch is functioning, OFF, or stuck in low-light mode.
 *
 * Execution Examples:
 * - On-Demand Lab Run: See `AGENTS_INTERNAL.md` for lab test runner commands.
 * - Local / ADB: `./gradlew :camera:integration-tests:coretestapp:connectedDebugAndroidTest \`
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
        private val TARGET_RESOLUTION = Size(1280, 720)
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setup(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
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
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
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

            val rearSummary =
                "Rear Camera Frame Captured: ${rearResult.captured} | " +
                    "Luminance: %.2f / 255.0 | Light PASS: ${rearResult.isLightON}"
                        .format(rearResult.luminance)
            val frontSummary =
                "Front Camera Frame Captured: ${frontResult.captured} | " +
                    "Luminance: %.2f / 255.0 | Light PASS: ${frontResult.isLightON}"
                        .format(frontResult.luminance)

            Log.i(
                TAG,
                "[LAB_DIAGNOSTIC_SUMMARY] Diagnostic run complete.\n" +
                    "  $rearSummary\n" +
                    "  $frontSummary",
            )
        }
    }

    private data class DiagnosticResult(
        val captured: Boolean,
        val luminance: Double,
        val isLightON: Boolean,
    )

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

        val resolutionSelector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        TARGET_RESOLUTION,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .build()

        val imageAnalysis =
            ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        val frameCapturedLatch = CountDownLatch(1)
        val frameSaved = AtomicBoolean(false)
        var luminanceScore = 0.0
        var isLightON = false

        imageAnalysis.setAnalyzer(CameraXExecutors.ioExecutor()) { imageProxy ->
            imageProxy.use { proxy ->
                if (frameSaved.compareAndSet(false, true)) {
                    try {
                        val bitmap = proxy.toBitmap()
                        try {
                            luminanceScore = LabTestUtil.calculateBitmapLuminance(bitmap)
                            isLightON =
                                LabTestUtil.checkLabEnvironmentLighting(
                                    bitmap,
                                    label = "$label ($testName)",
                                )
                            val savedFile = LabTestUtil.saveTestBitmap(bitmap, testName)
                            Log.i(
                                TAG,
                                ("[LAB_DIAGNOSTIC] $label Frame Saved: " +
                                        "${savedFile?.absolutePath ?: "skipped"} | " +
                                        "Luminance: %.2f | Lighting PASS: $isLightON")
                                    .format(luminanceScore),
                            )
                        } finally {
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[LAB_DIAGNOSTIC] Failed to analyze $label frame", e)
                    } finally {
                        frameCapturedLatch.countDown()
                    }
                }
            }
        }

        try {
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageAnalysis)
            }

            val success = frameCapturedLatch.await(FRAME_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!success) {
                Log.w(TAG, "[LAB_DIAGNOSTIC] Timed out waiting for $label frame capture.")
            }

            return DiagnosticResult(
                captured = success,
                luminance = luminanceScore,
                isLightON = isLightON,
            )
        } finally {
            imageAnalysis.clearAnalyzer()
            withContext(Dispatchers.Main) { cameraProvider.unbind(imageAnalysis) }
        }
    }
}
