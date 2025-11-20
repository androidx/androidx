/*
 * Copyright 2022 The Android Open Source Project
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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
import android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_START
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.VerifyResultListener
import androidx.camera.camera2.pipe.integration.testing.toCameraControlAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Assume.assumeThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TapToFocusDeviceTest {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(CameraPipeConfig.defaultConfig())
        )

    private lateinit var cameraSelector: CameraSelector
    private lateinit var context: Context
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var cameraControl: CameraControlAdapter
    private lateinit var comboListener: ComboRequestListener

    @Before
    fun setUp() {
        assumeTrue("Device has no camera", CameraUtil.deviceHasCamera())

        assumeTrue(
            "Flash unit not available with a valid camera and lens facing back",
            CameraUtil.hasFlashUnitWithLensFacing(CameraSelector.LENS_FACING_BACK),
        )

        assumeTrue(
            "CONTROL_AE_MODE_ON not available on device",
            CameraUtil.getCameraCharacteristics(CameraSelector.LENS_FACING_BACK)
                ?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                ?.contains(CONTROL_AE_MODE_ON) ?: false,
        )

        context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(context, CameraPipeConfig.defaultConfig())

        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        withContext(Dispatchers.Main) {
            if (::camera.isInitialized) {
                camera.removeUseCases(camera.useCases)
            }
        }

        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    private fun bindUseCase(@ImageCapture.FlashMode flashMode: Int) {
        val imageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()

        camera = CameraUtil.createCameraAndAttachUseCase(context, cameraSelector, imageCapture)

        cameraControl = camera.cameraControl.toCameraControlAdapter()

        @OptIn(ExperimentalCamera2Interop::class)
        comboListener = cameraControl.camera2cameraControl.requestListener
    }

    private suspend fun verifyAeModeForAfTriggerStartByTapToFocus() {
        val firstCaptureListener = VerifyResultListener(Int.MAX_VALUE)
        val verifyResultListener = VerifyResultListener(Int.MAX_VALUE)

        comboListener.addListener(firstCaptureListener, Dispatchers.Default.asExecutor())
        comboListener.addListener(verifyResultListener, Dispatchers.Default.asExecutor())

        // waits for first capture callback to make sure camera is ready for startFocusAndMetering
        firstCaptureListener.verify({ _, _ -> true }, 5000)

        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val meteringPoint = factory.createPoint(0f, 0f)
        cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(meteringPoint).build())

        assumeTrue(
            "Device can not start AF trigger",
            !TimeoutCancellationException::class
                .java
                .isInstance(
                    runCatching {
                            verifyResultListener.verify(
                                { captureRequest, _ ->
                                    if (
                                        captureRequest[CONTROL_AF_TRIGGER] ==
                                            CONTROL_AF_TRIGGER_START
                                    ) {
                                        assumeThat(
                                            "No AE mode in the CONTROL_AF_TRIGGER_START request",
                                            captureRequest[CONTROL_AE_MODE],
                                            CoreMatchers.notNullValue(),
                                        )

                                        Truth.assertWithMessage(
                                                "AE mode in CONTROL_AF_TRIGGER_START request is not as expected"
                                            )
                                            .that(captureRequest[CONTROL_AE_MODE])
                                            .isEqualTo(CONTROL_AE_MODE_ON)

                                        return@verify true
                                    }
                                    return@verify false
                                },
                                5000,
                            )
                        }
                        .exceptionOrNull()
                ),
        )
    }

    @Test
    fun tapToFocusAfTriggerStart_aeModeIsControlAeModeOn_whenFlashModeOff() = runBlocking {
        bindUseCase(ImageCapture.FLASH_MODE_OFF)
        verifyAeModeForAfTriggerStartByTapToFocus()
    }

    @Test
    fun tapToFocusAfTriggerStart_aeModeIsControlAeModeOn_whenFlashModeOn() = runBlocking {
        bindUseCase(ImageCapture.FLASH_MODE_ON)
        verifyAeModeForAfTriggerStartByTapToFocus()
    }

    @Test
    fun tapToFocusAfTriggerStart_aeModeIsControlAeModeOn_whenFlashModeAuto() = runBlocking {
        bindUseCase(ImageCapture.FLASH_MODE_AUTO)
        verifyAeModeForAfTriggerStartByTapToFocus()
    }
}
