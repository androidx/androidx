/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.testing.VerifyResultListener
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

private val TIMEOUT = TimeUnit.SECONDS.toMillis(10)

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCamera2Interop::class)
@SdkSuppress(minSdkVersion = 21)
class CameraControlAdapterDeviceTest {
    private lateinit var cameraSelector: CameraSelector
    private lateinit var context: Context
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var cameraControl: CameraControlAdapter
    private lateinit var comboListener: ComboRequestListener

    private val imageCapture = ImageCapture.Builder().build()
    private val imageAnalysis = ImageAnalysis.Builder().build().apply {
        // set analyzer to make it active.
        setAnalyzer(Dispatchers.Default.asExecutor()) {
            // Fake analyzer, do nothing.
        }
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        context = ApplicationProvider.getApplicationContext()
        CameraX.initialize(context, CameraPipeConfig.defaultConfig())
        cameraSelector = CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK
        ).build()
        camera = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraControl = camera.cameraControl as CameraControlAdapter
        comboListener = cameraControl.camera2cameraControl.requestListener
    }

    @After
    fun tearDown() {
        if (::camera.isInitialized) {
            camera.detachUseCases()
        }

        CameraX.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    // TODO: test all public API of the CameraControl to ensure the RequestOptions still exist
    //  after adding/removing the UseCase.
    @Test
    fun addUseCase_requestOptionsShouldSetToCamera(): Unit = runBlocking {
        // Arrange.
        bindUseCase(imageAnalysis)
        arrangeRequestOptions()

        // Act.
        withContext(Dispatchers.Main) {
            camera.addUseCases(listOf(imageCapture))
        }

        // Assert. Attaching a new UseCase should not change the RequestOptions that we set the
        // UseCaseCamera, The CaptureRequest after the new UseCase is attached should have the
        // same RequestOptions as before. The verify block will verify the CaptureRequest has the
        // same RequestOptions as we arranged.
        verifyRequestOptions()
    }

    // TODO: test all public API of the CameraControl to ensure the RequestOptions still exist
    //  after adding/removing the UseCase.
    @Test
    fun removeUseCase_requestOptionsShouldSetToCamera(): Unit = runBlocking {
        // Arrange.
        bindUseCase(imageAnalysis, imageCapture)
        arrangeRequestOptions()

        // Act.
        withContext(Dispatchers.Main) {
            camera.removeUseCases(listOf(imageCapture))
        }

        // Assert. Removing one of the UseCases (not all) should not change the
        // RequestOptions that we set the UseCaseCamera, the CaptureRequest after the UseCase
        // removal should have the same RequestOptions as before. The verify block will verify
        // the CaptureRequest has the same RequestOptions as we arranged.
        verifyRequestOptions()
    }

    private suspend fun arrangeRequestOptions() {
        cameraControl.setExposureCompensationIndex(1)
        cameraControl.setZoomRatio(1.0f)
        cameraControl.camera2cameraControl.setCaptureRequestOptions(
            CaptureRequestOptions.Builder().setCaptureRequestOption(
                CaptureRequest.CONTROL_CAPTURE_INTENT,
                CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM
            ).build()
        ).await()

        // Ensure the requests are already set to the CaptureRequest.
        waitForResult(captureCount = 10).verify(
            { captureRequests: List<RequestMetadata>, _ ->
                run {
                    captureRequests.last().let { lastRequest ->
                        // Ensure the EV working before testing
                        assumeThat(
                            "EV Request doesn't set to CaptureRequest, ignore the test",
                            lastRequest.request[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION],
                            CoreMatchers.equalTo(1)
                        )

                        // Ensure the Camera2Interop working before testing
                        assumeThat(
                            "Camera2Interop Request doesn't set to CaptureRequest, ignore the test",
                            lastRequest.request[CaptureRequest.CONTROL_CAPTURE_INTENT],
                            CoreMatchers.equalTo(CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM)
                        )

                        // Ensure the Zoom working before testing
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            assumeThat(
                                "Zoom Request doesn't set to CaptureRequest, ignore the test",
                                lastRequest.request[CaptureRequest.CONTROL_ZOOM_RATIO],
                                CoreMatchers.notNullValue()
                            )
                        } else {
                            assumeThat(
                                "Zoom Request doesn't set to CaptureRequest, ignore the test",
                                lastRequest.request[CaptureRequest.SCALER_CROP_REGION],
                                CoreMatchers.notNullValue()
                            )
                        }
                    }
                }
            },
            TIMEOUT
        )
    }

    private suspend fun verifyRequestOptions() {
        waitForResult(captureCount = 30).verify(
            { captureRequests: List<RequestMetadata>, _ ->
                run {
                    captureRequests.last().let { lastRequest ->
                        Truth.assertThat(
                            lastRequest.request[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION]
                        ).isEqualTo(1)
                        Truth.assertThat(
                            lastRequest.request[CaptureRequest.CONTROL_CAPTURE_INTENT]
                        ).isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Truth.assertThat(
                                lastRequest.request[CaptureRequest.CONTROL_ZOOM_RATIO]
                            ).isNotNull()
                        } else {
                            Truth.assertThat(
                                lastRequest.request[CaptureRequest.SCALER_CROP_REGION]
                            ).isNotNull()
                        }
                    }
                }
            },
            TIMEOUT
        )
    }

    private fun waitForResult(captureCount: Int = 1): VerifyResultListener =
        VerifyResultListener(captureCount).also {
            comboListener.addListener(it, Dispatchers.Default.asExecutor())
        }

    private fun bindUseCase(vararg useCases: UseCase) {
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            cameraSelector,
            *useCases,
        )
        cameraControl = camera.cameraControl as CameraControlAdapter
    }
}