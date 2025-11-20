/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.core.camera2

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.FlashState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Test if low-light boost functionality can run well in real devices. */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35)
class LowLightBoostDeviceTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val labTestRule = LabTestRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private val lifecycleOwner = FakeLifecycleOwner().also { it.startAndResume() }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.awaitInstance(context)
        assumeTrue(cameraProvider.hasCamera(cameraSelector))
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
        }
        assumeTrue(camera.cameraInfo.isLowLightBoostSupported)
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun canReceiveCorrectLowLightBoostStates() {
        // Binds a Preview
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Enables low-light boost
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]

        // Verifies that all received states are either INACTIVE or ACTIVE
        verifyLowLightBoostOnStatesReceived()

        // Enables low-light boost
        camera.cameraControl.enableLowLightBoostAsync(false)[1, TimeUnit.SECONDS]

        // Verifies that low-light boost state is OFF
        verifyLowLightBoostOffStateReceived()
    }

    @Test
    @LabTestRule.LabTestRearCamera
    fun turnsOnLowLightBoost_willTurnsOffTorch() {
        assumeTrue(
            cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK &&
                camera.cameraInfo.hasFlashUnit()
        )

        // Binds a Preview
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Enables torch
        camera.cameraControl.enableTorch(true)[1, TimeUnit.SECONDS]
        assertThat(camera.cameraInfo.torchState.value).isEqualTo(TorchState.ON)

        // Checks that torch will be turned off after turning low-light boost on
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        verifyLowLightBoostOnStatesReceived()
        assertThat(camera.cameraInfo.torchState.value).isEqualTo(TorchState.OFF)
    }

    @Test
    fun turnsOnTorchThrowsException_whenLowLightBoostIsOn() {
        assumeTrue(
            cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK &&
                camera.cameraInfo.hasFlashUnit()
        )

        // Binds a Preview
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Enables low-light boost
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        verifyLowLightBoostOnStatesReceived()

        // Checks that ExecutionException will be thrown after turning torch on
        assertThrows<ExecutionException> {
            camera.cameraControl.enableTorch(true)[1, TimeUnit.SECONDS]
        }
    }

    @Test
    fun turnsOnLowLightBoostThrowsException_whenFpsExceeds30() {
        // Finds a 30+ supported frame rate range
        var fpsRange30Plus: Range<Int>? = null
        camera.cameraInfo.supportedFrameRateRanges.forEach {
            if (it.upper > 30) {
                fpsRange30Plus = it
                return@forEach
            }
        }
        assumeTrue(fpsRange30Plus != null)

        val map =
            CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)!![
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!

        // Binds a Preview with a CameraFilter to select a resolution supporting 30+ FPS
        val preview =
            Preview.Builder()
                .setTargetFrameRate(fpsRange30Plus!!)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionFilter { outputSizes, _ ->
                            outputSizes.forEach {
                                val minFrameDuration =
                                    map.getOutputMinFrameDuration(SurfaceTexture::class.java, it)
                                if (floor(1_000_000_000.0 / minFrameDuration + 0.05).toInt() > 30) {
                                    return@setResolutionFilter listOf(it)
                                }
                            }
                            return@setResolutionFilter outputSizes
                        }
                        .build()
                )
                .build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // Ensures that the expected frame rate range in stream spec is larger than 30
            assumeTrue(preview.attachedStreamSpec!!.expectedFrameRateRange.upper > 30)
        }

        // Checks that ExecutionException will be thrown after turning low-light boost on
        assertThrows<ExecutionException> {
            camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        }
    }

    @Test
    fun canTurnsOnLowLightBoost_when10BitDynamicRangeIsOn() {
        // Finds 10-bit supported dynamic ranges
        val candidate10BitDynamicRanges =
            setOf(
                DynamicRange.HLG_10_BIT,
                DynamicRange.HDR10_10_BIT,
                DynamicRange.DOLBY_VISION_10_BIT,
            )
        val supported10BitDynamicRange =
            camera.cameraInfo.querySupportedDynamicRanges(candidate10BitDynamicRanges).firstOrNull()
        assumeTrue(supported10BitDynamicRange != null)

        // Binds a Preview
        val preview = Preview.Builder().setDynamicRange(supported10BitDynamicRange!!).build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // Ensures that the dynamic range in stream spec is 10-bit
            assumeTrue(preview.attachedStreamSpec!!.dynamicRange == supported10BitDynamicRange)
        }

        // Enables low-light boost
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        verifyLowLightBoostOnStatesReceived()
    }

    @Test
    fun turnsOnLowLightBoost_willDisableFlash() {
        assumeTrue(camera.cameraInfo.hasFlashUnit())

        // Binds Preview and ImageCapture
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_ON).build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        }

        // Checks that low-light boost is on
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        verifyLowLightBoostOnStatesReceived()

        val capturedCountDownLatch = CountDownLatch(1)
        var error: Exception? = null
        var flashState: Int = FlashState.UNKNOWN

        imageCapture.takePicture(
            Dispatchers.Main.asExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    flashState = image.imageInfo.flashState
                    image.close()
                    capturedCountDownLatch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {
                    error = exception
                    capturedCountDownLatch.countDown()
                }
            },
        )

        // Checks the image is captured successfully with flash state NOT_FIRED
        assertThat(capturedCountDownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(error).isNull()
        assertThat(flashState).isEqualTo(FlashState.NOT_FIRED)
    }

    @Test
    fun keepLowLightBoostOn_afterBindUnbindAdditionalUseCase() {
        // Binds a Preview
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Enables low-light boost
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        verifyLowLightBoostOnStatesReceived()

        // Binds a ImageCapture
        val imageCapture = ImageCapture.Builder().build()
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
        }
        verifyLowLightBoostOnStatesReceived()

        // Unbinds the ImageCapture
        instrumentation.runOnMainSync { cameraProvider.unbind(imageCapture) }
        verifyLowLightBoostOnStatesReceived()
    }

    @Test
    fun turnOffLowLightBoost_afterUnbindAll() {
        // Binds a Preview
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Enables low-light boost
        camera.cameraControl.enableLowLightBoostAsync(true)[1, TimeUnit.SECONDS]
        verifyLowLightBoostOnStatesReceived()

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }

        // Verifies that low-light boost state is OFF
        verifyLowLightBoostOffStateReceived()
    }

    private fun verifyLowLightBoostOnStatesReceived() =
        verifyLowLightBoostStateReceived(
            intArrayOf(LowLightBoostState.INACTIVE, LowLightBoostState.ACTIVE)
        )

    private fun verifyLowLightBoostOffStateReceived() =
        verifyLowLightBoostStateReceived(intArrayOf(LowLightBoostState.OFF))

    private fun verifyLowLightBoostStateReceived(acceptedStates: IntArray) {
        val stateReceivedCountDownLatch = CountDownLatch(1)

        instrumentation.runOnMainSync {
            camera.cameraInfo.lowLightBoostState.observe(lifecycleOwner) { state ->
                if (stateReceivedCountDownLatch.count > 0 && acceptedStates.contains(state)) {
                    stateReceivedCountDownLatch.countDown()
                }
            }
        }

        // Verifies that the expected states can be received
        assertThat(stateReceivedCountDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    private fun waitForCameraClosed() {
        val cameraClosedCountDownLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            camera.cameraInfo.cameraState.observeForever { state ->
                if (state.type == CameraState.Type.CLOSED) {
                    cameraClosedCountDownLatch.countDown()
                }
            }
        }

        // Waits for the CameraState to be CLOSED
        cameraClosedCountDownLatch.await(1, TimeUnit.SECONDS)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }
    }
}
