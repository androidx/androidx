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

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LensCategoryDeviceTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
    }

    @Before
    fun setUp() = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        withTimeout(10000) {
            ProcessCameraProvider.configureInstance(cameraConfig)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        }
        fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.unbindAll() }
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Suppress("DEPRECATION")
    @Test
    fun supportedLensCategories_bindToLifecycle_streamsFramesAndCaptureResults() = runBlocking {
        for (lensFacing in
            listOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)) {
            if (!CameraUtil.hasCameraWithLensFacing(lensFacing)) continue

            val supportedCategories = cameraProvider.getSupportedLensCategories(lensFacing)
            if (supportedCategories.isEmpty()) continue

            // Track tested camera targets (cameraId, physicalCameraId) to verify every distinct
            // standalone camera and physical sub-camera while avoiding redundant session thrashing.
            val testedCameras = mutableSetOf<Pair<String, String?>>()

            for (category in supportedCategories) {
                val selector =
                    CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .setLensCategory(category)
                        .build()
                val cameraInfo = cameraProvider.getCameraInfo(selector)
                val targetPhysicalId = cameraInfo.cameraSelector.physicalCameraId
                val expectedCameraId = (cameraInfo as CameraInfoInternal).cameraId
                val expectedIntrinsicZoomRatio = cameraInfo.intrinsicZoomRatio

                val cameraKey = Pair(expectedCameraId, targetPhysicalId)
                if (!testedCameras.add(cameraKey)) {
                    continue
                }

                // Emulators (Cuttlefish/Goldfish) expose synthetic physical sub-cameras but omit
                // SCALER_AVAILABLE_STREAM_USE_CASES in physical camera metadata. When CameraX
                // assigns a stream use case derived from the logical camera, cameraserver rejects
                // physical stream creation.
                if (targetPhysicalId != null && AndroidUtil.isEmulator()) {
                    continue
                }

                val captureResultLatch = CountDownLatch(1)
                val physicalResultLatch = CountDownLatch(1)
                val frameLatch = CountDownLatch(1)
                var capturedCameraDeviceId: String? = null

                val captureCallback =
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            capturedCameraDeviceId = session.device.id
                            captureResultLatch.countDown()

                            if (targetPhysicalId != null) {
                                val physicalIds =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        result.physicalCameraTotalResults?.keys ?: emptySet()
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        @Suppress("DEPRECATION")
                                        result.physicalCameraResults?.keys ?: emptySet()
                                    } else {
                                        emptySet()
                                    }
                                if (physicalIds.contains(targetPhysicalId)) {
                                    physicalResultLatch.countDown()
                                }
                            }
                        }
                    }

                val previewBuilder = Preview.Builder()
                Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(captureCallback)
                val preview = previewBuilder.build()

                try {
                    withContext(Dispatchers.Main) {
                        preview.setSurfaceProvider(
                            SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                                frameLatch.countDown()
                            }
                        )
                        val camera =
                            cameraProvider.bindToLifecycle(fakeLifecycleOwner, selector, preview)
                        assertThat(camera).isNotNull()
                        assertThat(cameraProvider.isBound(preview)).isTrue()
                        assertThat((camera.cameraInfo as CameraInfoInternal).cameraId)
                            .isEqualTo(expectedCameraId)
                        assertThat(camera.cameraInfo.intrinsicZoomRatio)
                            .isEqualTo(expectedIntrinsicZoomRatio)
                        assertThat(camera.cameraInfo.cameraSelector.physicalCameraId)
                            .isEqualTo(targetPhysicalId)
                        assertThat(preview.physicalCameraId).isEqualTo(targetPhysicalId)
                    }

                    // 1. Verify frames are received by the SurfaceTexture
                    assertThat(frameLatch.await(10, TimeUnit.SECONDS)).isTrue()

                    // 2. Verify TotalCaptureResult is received and matches the expected camera
                    // device
                    assertThat(captureResultLatch.await(10, TimeUnit.SECONDS)).isTrue()
                    assertThat(capturedCameraDeviceId).isEqualTo(expectedCameraId)

                    // 3. For physical sub-cameras, verify TotalCaptureResult contains results from
                    // target physical camera
                    if (targetPhysicalId != null) {
                        assertThat(physicalResultLatch.await(10, TimeUnit.SECONDS)).isTrue()
                    }
                } finally {
                    withContext(Dispatchers.Main) { cameraProvider.unbindAll() }
                }
            }
        }
    }
}
