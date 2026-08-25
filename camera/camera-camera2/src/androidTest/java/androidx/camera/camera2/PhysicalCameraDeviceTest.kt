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

package androidx.camera.camera2

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class PhysicalCameraDeviceTest {
    @get:Rule val useCamera = CameraUtil.grantCameraPermissionAndPreTestAndPostTest()

    private lateinit var context: Context

    private enum class StreamConfig {
        ANALYSIS,
        PREVIEW,
        COMBINED,
    }

    @Before
    fun setUp() {
        assumeFalse(
            "Physical camera streaming is not supported on emulators",
            AndroidUtil.isEmulator() ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.HARDWARE.contains("cutf") ||
                Build.MODEL.contains("Cuttlefish", ignoreCase = true) ||
                Build.MODEL.contains("google_sdk", ignoreCase = true) ||
                Build.MODEL.contains("Emulator", ignoreCase = true) ||
                Build.MODEL.contains("Android SDK built for", ignoreCase = true),
        )

        context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(context, Camera2Config.defaultConfig())
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun canStreamFromPhysicalCamera_imageAnalysis() = runBlocking {
        testPhysicalCameraStreaming(StreamConfig.ANALYSIS)
    }

    @Test
    fun canStreamFromPhysicalCamera_preview() = runBlocking {
        testPhysicalCameraStreaming(StreamConfig.PREVIEW)
    }

    @Test
    fun canStreamFromPhysicalCamera_combined() = runBlocking {
        testPhysicalCameraStreaming(StreamConfig.COMBINED)
    }

    private data class PhysicalCameraFailure(
        val logicalCameraId: String,
        val lensFacing: Int,
        val physicalCameraId: String,
        val errorMessage: String,
    )

    private suspend fun testPhysicalCameraStreaming(streamConfig: StreamConfig) {
        val lensFacings = listOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)
        var testedAnyPhysicalCamera = false
        val failures = mutableListOf<PhysicalCameraFailure>()

        for (lensFacing in lensFacings) {
            if (!CameraUtil.hasCameraWithLensFacing(lensFacing)) {
                continue
            }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Step 1: Verify that the logical camera itself can open and stream frames.
            if (!isLogicalCameraFunctional(cameraSelector)) {
                println(
                    "Skipping lensFacing $lensFacing: logical camera failed to stream frames on " +
                        "${Build.MANUFACTURER} ${Build.MODEL}."
                )
                continue
            }

            val cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
            val cameraInfo = cameraUseCaseAdapter.cameraInfo

            if (!cameraInfo.isLogicalMultiCameraSupported) {
                continue
            }

            val physicalCameraInfos = cameraInfo.physicalCameraInfos
            if (physicalCameraInfos.isEmpty()) {
                continue
            }

            val logicalCameraId = Camera2Interop.getCameraId(cameraInfo)

            // Step 2: Check physical cameras on the logical camera one by one.
            for (physicalCameraInfo in physicalCameraInfos) {
                val physicalCameraId = Camera2Interop.getCameraId(physicalCameraInfo)
                testedAnyPhysicalCamera = true
                try {
                    verifyPhysicalCameraStreaming(cameraSelector, physicalCameraId, streamConfig)
                } catch (t: Throwable) {
                    val errorMsg = t.message ?: t.javaClass.simpleName
                    println(
                        "FAILED Physical Camera $physicalCameraId on Logical Camera $logicalCameraId " +
                            "(${if (lensFacing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT"}): $errorMsg"
                    )
                    failures.add(
                        PhysicalCameraFailure(
                            logicalCameraId = logicalCameraId,
                            lensFacing = lensFacing,
                            physicalCameraId = physicalCameraId,
                            errorMessage = errorMsg,
                        )
                    )
                }
            }
        }

        assumeTrue(
            "Device does not have any functional logical multi-camera with available physical cameras",
            testedAnyPhysicalCamera,
        )

        // Step 3: List failure physical cameras and corresponding logical camera for quirk
        // updating.
        if (failures.isNotEmpty()) {
            val failureReport = buildString {
                appendLine(
                    "Physical camera streaming failed on ${Build.MANUFACTURER} ${Build.MODEL} (${Build.BRAND}):"
                )
                val groupedByLogical = failures.groupBy { it.logicalCameraId to it.lensFacing }
                for ((logicalKey, logicalFailures) in groupedByLogical) {
                    val (logicalId, facing) = logicalKey
                    val facingStr =
                        if (facing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT"
                    appendLine("  Logical Camera $logicalId ($facingStr):")
                    for (f in logicalFailures) {
                        appendLine(
                            "    - Physical Camera ID ${f.physicalCameraId}: ${f.errorMessage}"
                        )
                    }
                }
                appendLine("Suggested ExcludePhysicalCameraIdQuirk mapping entry:")
                val failedIds =
                    failures
                        .map { "\"${it.physicalCameraId}\"" }
                        .distinct()
                        .sorted()
                        .joinToString(", ")
                appendLine("  \"${Build.MODEL}\" to setOf($failedIds)")
            }
            org.junit.Assert.fail(failureReport)
        }
    }

    private suspend fun isLogicalCameraFunctional(cameraSelector: CameraSelector): Boolean {
        val preview = Preview.Builder().build()
        val previewLatch = CountDownLatch(3)
        val listener = SurfaceTexture.OnFrameAvailableListener { previewLatch.countDown() }

        var cameraProvider: androidx.camera.core.internal.CameraUseCaseAdapter? = null
        return try {
            val camera = CameraUtil.createCameraAndAttachUseCase(context, cameraSelector, preview)
            cameraProvider = camera as? androidx.camera.core.internal.CameraUseCaseAdapter
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider(listener)
                )
            }
            previewLatch.await(5, TimeUnit.SECONDS)
        } catch (t: Throwable) {
            println("Logical camera probe failed on ${Build.MODEL}: ${t.message}")
            false
        } finally {
            try {
                cameraProvider?.let {
                    withContext(Dispatchers.Main) { it.removeUseCases(listOf(preview)) }
                }
            } catch (_: Throwable) {}
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun verifyPhysicalCameraStreaming(
        cameraSelector: CameraSelector,
        physicalCameraId: String,
        streamConfig: StreamConfig,
    ) {
        val useCases = mutableListOf<UseCase>()
        var analysis: ImageAnalysis? = null
        var preview: Preview? = null

        if (streamConfig == StreamConfig.ANALYSIS || streamConfig == StreamConfig.COMBINED) {
            analysis =
                ImageAnalysis.Builder()
                    .also { Camera2Interop.Extender(it).setPhysicalCameraId(physicalCameraId) }
                    .build()
            useCases.add(analysis)
        }

        if (streamConfig == StreamConfig.PREVIEW || streamConfig == StreamConfig.COMBINED) {
            preview =
                Preview.Builder()
                    .also { Camera2Interop.Extender(it).setPhysicalCameraId(physicalCameraId) }
                    .build()
            useCases.add(preview)
        }

        val cameraProvider =
            CameraUtil.createCameraAndAttachUseCase(
                context,
                cameraSelector,
                *useCases.toTypedArray(),
            )

        val analysisLatch = CountDownLatch(5)
        val previewLatch = CountDownLatch(5)

        if (analysis != null) {
            analysis.setAnalyzer(Dispatchers.Default.asExecutor()) { image ->
                try {
                    analysisLatch.countDown()
                } finally {
                    image.close()
                }
            }
        }

        if (preview != null) {
            val listener = SurfaceTexture.OnFrameAvailableListener { previewLatch.countDown() }
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider(listener)
                )
            }
        }

        try {
            if (analysis != null) {
                assertTrue(
                    "Timed out waiting for 5 ImageAnalysis frames from physical camera $physicalCameraId with config $streamConfig",
                    analysisLatch.await(10, TimeUnit.SECONDS),
                )
            }
            if (preview != null) {
                assertTrue(
                    "Timed out waiting for 5 Preview frames from physical camera $physicalCameraId with config $streamConfig",
                    previewLatch.await(10, TimeUnit.SECONDS),
                )
            }
        } finally {
            analysis?.clearAnalyzer()
            withContext(Dispatchers.Main) { cameraProvider.removeUseCases(useCases) }
        }
    }
}
