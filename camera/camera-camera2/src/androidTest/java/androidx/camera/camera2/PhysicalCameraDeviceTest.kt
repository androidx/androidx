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

    private suspend fun testPhysicalCameraStreaming(streamConfig: StreamConfig) {
        val lensFacings = listOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)
        var testedAnyPhysicalCamera = false

        for (lensFacing in lensFacings) {
            if (!CameraUtil.hasCameraWithLensFacing(lensFacing)) {
                continue
            }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
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

            for (physicalCameraInfo in physicalCameraInfos) {
                val physicalCameraId = Camera2Interop.getCameraId(physicalCameraInfo)
                verifyPhysicalCameraStreaming(cameraSelector, physicalCameraId, streamConfig)
                testedAnyPhysicalCamera = true
            }
        }

        assumeTrue(
            "Device does not have any logical multi-camera with available physical cameras",
            testedAnyPhysicalCamera,
        )
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
