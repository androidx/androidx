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

package androidx.camera.camera2.internal

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageWriter
import android.os.Build
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.internal.Camera2CapturePipeline.ScreenFlashTask
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.quirk.AutoFlashUnderExposedQuirk
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks
import androidx.camera.camera2.internal.compat.quirk.TorchFlashRequiredFor3aUpdateQuirk
import androidx.camera.camera2.internal.compat.quirk.UseTorchAsFlashQuirk
import androidx.camera.camera2.internal.compat.workaround.OverrideAeModeForStillCapture
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureMetaData.AeState
import androidx.camera.core.impl.CameraCaptureMetaData.AfState
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.impl.fakes.FakeImageProxy
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

private const val CAMERA_ID_0 = "0"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
)
class Camera2CapturePipelineTest {

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val executorService = Executors.newSingleThreadScheduledExecutor()

    private val baseRepeatingResult: Map<CaptureResult.Key<*>, Any> =
        mapOf(
            CaptureResult.CONTROL_MODE to CaptureResult.CONTROL_MODE_AUTO,
            CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO,
            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED,
            CaptureResult.CONTROL_AWB_MODE to CaptureResult.CONTROL_AWB_MODE_AUTO,
            CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_ON,
        )

    private val resultConverged: Map<CaptureResult.Key<*>, Any> =
        mapOf(
            CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO,
            CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED,
            CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_CONVERGED,
        )

    private val resultConvergedWith3AModeOff: Map<CaptureResult.Key<*>, Any> =
        mapOf(
            CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_OFF,
            CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_OFF,
            CaptureResult.CONTROL_AWB_MODE to CaptureResult.CONTROL_AWB_MODE_OFF,
            CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_INACTIVE,
            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_INACTIVE,
            CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_INACTIVE,
        )

    private val fakeStillCaptureSurface = ImmediateSurface(Surface(SurfaceTexture(0)))

    private val singleRequest =
        CaptureConfig.Builder()
            .apply {
                templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                addSurface(fakeStillCaptureSurface)
            }
            .build()

    private var runningRepeatingStream: ScheduledFuture<*>? = null
        set(value) {
            runningRepeatingStream?.cancel(false)
            field = value
        }

    private lateinit var testScreenFlash: MockScreenFlash

    @Before
    fun setUp() {
        initCameras()
        testScreenFlash = MockScreenFlash()
    }

    @After
    fun tearDown() {
        runningRepeatingStream = null
        fakeStillCaptureSurface.close()
        executorService.shutdown()
    }

    @Ignore // b/228856476
    @Test
    fun pipelineTest_preCapturePostCaptureShouldCalled() {
        // Arrange.
        val fakeTask =
            object : Camera2CapturePipeline.PipelineTask {
                val preCaptureCountDown = CountDownLatch(1)
                val postCaptureCountDown = CountDownLatch(1)

                override fun preCapture(
                    captureResult: TotalCaptureResult?
                ): ListenableFuture<Boolean> {
                    preCaptureCountDown.countDown()
                    return Futures.immediateFuture(false)
                }

                override fun isCaptureResultNeeded(): Boolean {
                    return false
                }

                override fun postCapture() {
                    postCaptureCountDown.countDown()
                }
            }

        val cameraControl =
            createCameraControl().apply {
                simulateRepeatingResult(resultParameters = resultConverged)
            }

        val pipeline =
            Camera2CapturePipeline.Pipeline(
                    CameraDevice.TEMPLATE_PREVIEW,
                    executorService,
                    executorService,
                    cameraControl,
                    false,
                    OverrideAeModeForStillCapture(Quirks(emptyList())),
                )
                .apply { addTask(fakeTask) }

        // Act.
        pipeline.executeCapture(
            listOf(singleRequest),
            FLASH_MODE_OFF,
        )

        // Assert.
        assertTrue(fakeTask.preCaptureCountDown.await(3, TimeUnit.SECONDS))
        assertTrue(fakeTask.postCaptureCountDown.await(3, TimeUnit.SECONDS))
    }

    @Test
    fun maxQuality_afInactive_shouldTriggerAf(): Unit = runBlocking {
        val cameraControl =
            createCameraControl().apply {

                // Arrange. Simulate the scenario that we need to triggerAF.
                simulateRepeatingResult(
                    initialDelay = 100,
                    resultParameters =
                        mapOf(
                            CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO,
                            CaptureResult.CONTROL_AF_STATE to
                                CaptureResult.CONTROL_AF_STATE_INACTIVE,
                        )
                )

                // Act.
                submitStillCaptureRequests(
                    listOf(singleRequest),
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                    ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
            }

        // Assert 1, verify the CONTROL_AF_TRIGGER is triggered
        immediateCompleteCapture.verifyRequestResult {
            it.requestContains(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START
            )
        }

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert 2, that CONTROL_AF_TRIGGER should be cancelled finally.
        immediateCompleteCapture.verifyRequestResult {
            it.requestContains(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
            )
        }
    }

    @Test
    fun miniLatency_flashOn_shouldTriggerAe() {
        flashOn_shouldTriggerAe(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_flashOn_shouldTriggerAe() {
        flashOn_shouldTriggerAe(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private fun flashOn_shouldTriggerAe(imageCaptureMode: Int) {
        val cameraControl =
            createCameraControl().apply {
                // Arrange.
                flashMode = FLASH_MODE_ON

                // Act.
                submitStillCaptureRequests(
                    listOf(singleRequest),
                    imageCaptureMode,
                    ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
                simulateRepeatingResult(initialDelay = 100)
            }

        // Assert 1, verify the CONTROL_AE_PRECAPTURE_TRIGGER is triggered
        immediateCompleteCapture.verifyRequestResult {
            it.requestContains(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
        }

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert 2 that CONTROL_AE_PRECAPTURE_TRIGGER should be cancelled finally.
        if (Build.VERSION.SDK_INT >= 23) {
            immediateCompleteCapture.verifyRequestResult {
                it.requestContains(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                )
            }
        }
    }

    @Test
    fun miniLatency_flashAutoFlashRequired_shouldTriggerAe() {
        flashAutoFlashRequired_shouldTriggerAe(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_flashAutoFlashRequired_shouldTriggerAe(): Unit = runBlocking {
        flashAutoFlashRequired_shouldTriggerAe(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private fun flashAutoFlashRequired_shouldTriggerAe(imageCaptureMode: Int) {
        val cameraControl =
            createCameraControl().apply {
                // Arrange.
                flashMode = FLASH_MODE_AUTO
                simulateRepeatingResult(
                    initialDelay = 100,
                    resultParameters =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                        )
                )

                // Act.
                submitStillCaptureRequests(
                    listOf(singleRequest),
                    imageCaptureMode,
                    ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
            }

        // Assert 1, verify the CONTROL_AE_PRECAPTURE_TRIGGER is triggered
        immediateCompleteCapture.verifyRequestResult {
            it.requestContains(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
        }

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert 2 that CONTROL_AE_PRECAPTURE_TRIGGER should be cancelled finally.
        if (Build.VERSION.SDK_INT >= 23) {
            immediateCompleteCapture.verifyRequestResult {
                it.requestContains(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
                )
            }
        }
    }

    @Test
    fun createPipeline_screenFlashTaskAdded() {
        val camera2CapturePipeline =
            Camera2CapturePipeline(
                createCameraControl(),
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                    ShadowCameraCharacteristics.newCameraCharacteristics(),
                    CAMERA_ID_0,
                ),
                Quirks(emptyList()),
                CameraXExecutors.directExecutor(),
                CameraXExecutors.myLooperExecutor(),
            )

        val pipeline =
            camera2CapturePipeline.createPipeline(
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_MODE_SCREEN,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH
            )

        var hasScreenFlashTask = false
        pipeline.mTasks.forEach { task ->
            if (task is ScreenFlashTask) {
                hasScreenFlashTask = true
            }
        }

        assertThat(hasScreenFlashTask).isTrue()
    }

    @Test
    fun miniLatency_withTorchAsFlashQuirk_shouldOpenTorch() {
        withTorchAsFlashQuirk_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withTorchAsFlashQuirk_shouldOpenTorch() {
        withTorchAsFlashQuirk_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private fun withTorchAsFlashQuirk_shouldOpenTorch(imageCaptureMode: Int) {
        val cameraControl =
            createCameraControl(
                    // Arrange.
                    quirks = Quirks(listOf(object : UseTorchAsFlashQuirk {}))
                )
                .apply {
                    flashMode = FLASH_MODE_ON
                    simulateRepeatingResult(initialDelay = 100)

                    // Act.
                    submitStillCaptureRequests(
                        listOf(singleRequest),
                        imageCaptureMode,
                        ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                    )
                }

        // Assert 1 torch should be turned on
        cameraControl.waitForSessionConfig { it.isTorchParameterEnabled() }

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert 2 torch should be turned off
        immediateCompleteCapture.verifyRequestResult { it.isTorchParameterDisabled() }
    }

    @Test
    fun miniLatency_withTemplateRecord_shouldOpenTorch() {
        withTemplateRecord_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withTemplateRecord_shouldOpenTorch() {
        withTemplateRecord_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private fun withTemplateRecord_shouldOpenTorch(imageCaptureMode: Int) {

        val cameraControl =
            createCameraControl().apply {
                // Arrange.
                setTemplate(CameraDevice.TEMPLATE_RECORD)
                flashMode = FLASH_MODE_ON
                simulateRepeatingResult(initialDelay = 100)
                submitStillCaptureRequests(
                    listOf(singleRequest),
                    imageCaptureMode,
                    ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
            }

        // Assert 1 torch should be turned on
        cameraControl.waitForSessionConfig { it.isTorchParameterEnabled() }

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert 2 torch should be turned off
        immediateCompleteCapture.verifyRequestResult { it.isTorchParameterDisabled() }
    }

    @Test
    fun miniLatency_withFlashTypeTorch_shouldOpenTorch() {
        withFlashTypeTorch_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    }

    @Test
    fun maxQuality_withFlashTypeTorch_shouldOpenTorch() {
        withFlashTypeTorch_shouldOpenTorch(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    }

    private fun withFlashTypeTorch_shouldOpenTorch(imageCaptureMode: Int) {
        val cameraControl =
            createCameraControl().apply {
                flashMode = FLASH_MODE_ON
                simulateRepeatingResult(initialDelay = 100)
                submitStillCaptureRequests(
                    listOf(singleRequest),
                    imageCaptureMode,
                    ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH,
                )
            }

        // Assert 1 torch should be turned on
        cameraControl.waitForSessionConfig { it.isTorchParameterEnabled() }

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert 2 torch should be turned off
        immediateCompleteCapture.verifyRequestResult { it.isTorchParameterDisabled() }
    }

    @Test
    fun miniLatency_shouldNoPreCapture(): Unit = runBlocking {
        // Arrange.
        val cameraControl =
            createCameraControl().apply { simulateRepeatingResult(initialDelay = 100) }

        // Act.
        cameraControl
            .submitStillCaptureRequests(
                listOf(singleRequest),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .await()

        // Assert, there is only 1 single capture request.
        assertThat(immediateCompleteCapture.getAllResults().size).isEqualTo(1)
    }

    @Test
    fun submitStillCaptureRequests_withTemplate_templateSent(): Unit = runBlocking {
        // Arrange.
        val imageCaptureConfig =
            CaptureConfig.Builder().let {
                it.addSurface(fakeStillCaptureSurface)
                it.templateType = CameraDevice.TEMPLATE_MANUAL
                it.build()
            }
        val cameraControl =
            createCameraControl().apply { simulateRepeatingResult(initialDelay = 100) }

        // Act.
        cameraControl
            .submitStillCaptureRequests(
                listOf(imageCaptureConfig),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .await()

        // Assert.
        immediateCompleteCapture.verifyRequestResult { captureConfigList ->
            captureConfigList
                .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                .map { captureConfig -> captureConfig.templateType }
                .contains(CameraDevice.TEMPLATE_MANUAL)
        }
    }

    @Test
    fun submitStillCaptureRequests_withNoTemplate_templateStillCaptureSent(): Unit = runBlocking {
        // Arrange.
        val imageCaptureConfig =
            CaptureConfig.Builder().apply { addSurface(fakeStillCaptureSurface) }.build()
        val cameraControl =
            createCameraControl().apply { simulateRepeatingResult(initialDelay = 100) }

        // Act.
        cameraControl
            .submitStillCaptureRequests(
                listOf(imageCaptureConfig),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
            .await()

        // Assert.
        immediateCompleteCapture.verifyRequestResult { captureConfigList ->
            captureConfigList
                .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                .map { captureConfig -> captureConfig.templateType }
                .contains(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }
    }

    @Test
    fun submitStillCaptureRequests_withTemplateRecord_templateVideoSnapshotSent(): Unit =
        runBlocking {
            createCameraControl().apply {
                // Arrange.
                setTemplate(CameraDevice.TEMPLATE_RECORD)
                simulateRepeatingResult(initialDelay = 100)

                // Act.
                submitStillCaptureRequests(
                        listOf(singleRequest),
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                        ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                    )
                    .await()
            }

            // Assert.
            immediateCompleteCapture.verifyRequestResult { captureConfigList ->
                captureConfigList
                    .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                    .map { captureConfig -> captureConfig.templateType }
                    .contains(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT)
            }
        }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withZslTemplate_templateZeroShutterLagSent(): Unit = runBlocking {
        // Arrange.
        val imageCaptureConfig =
            CaptureConfig.Builder().let {
                it.addSurface(fakeStillCaptureSurface)
                it.templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                it.build()
            }

        val cameraControl =
            initCameraControlWithZsl(
                isZslDisabledByFlashMode = false,
                isZslDisabledByUserCaseConfig = false
            )

        // Act.
        cameraControl
            .submitStillCaptureRequests(
                listOf(imageCaptureConfig),
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                FLASH_MODE_OFF,
            )
            .await()

        // Assert.
        val templateTypeToVerify =
            if (Build.VERSION.SDK_INT >= 23) CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
            else CameraDevice.TEMPLATE_STILL_CAPTURE

        immediateCompleteCapture.verifyRequestResult { captureConfigList ->
            captureConfigList
                .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                .map { captureConfig -> captureConfig.templateType }
                .contains(templateTypeToVerify)
        }
    }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withZslDisabledByFlashMode_templateStillPictureSent(): Unit =
        runBlocking {
            // Arrange.
            val imageCaptureConfig =
                CaptureConfig.Builder().let {
                    it.addSurface(fakeStillCaptureSurface)
                    it.templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                    it.build()
                }

            val cameraControl =
                initCameraControlWithZsl(
                    isZslDisabledByFlashMode = true,
                    isZslDisabledByUserCaseConfig = false
                )

            // Act.
            cameraControl
                .submitStillCaptureRequests(
                    listOf(imageCaptureConfig),
                    ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                    FLASH_MODE_OFF,
                )
                .await()

            // Assert.
            immediateCompleteCapture.verifyRequestResult { captureConfigList ->
                captureConfigList
                    .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                    .map { captureConfig -> captureConfig.templateType }
                    .contains(CameraDevice.TEMPLATE_STILL_CAPTURE)
            }
        }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withZslDisabledByUseCaseConfig_templateStillPictureSent(): Unit =
        runBlocking {
            // Arrange.
            val imageCaptureConfig =
                CaptureConfig.Builder().let {
                    it.addSurface(fakeStillCaptureSurface)
                    it.templateType = CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                    it.build()
                }

            val cameraControl =
                initCameraControlWithZsl(
                    isZslDisabledByFlashMode = false,
                    isZslDisabledByUserCaseConfig = true
                )

            // Act.
            cameraControl
                .submitStillCaptureRequests(
                    listOf(imageCaptureConfig),
                    ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                    FLASH_MODE_OFF,
                )
                .await()

            // Assert.
            immediateCompleteCapture.verifyRequestResult { captureConfigList ->
                captureConfigList
                    .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                    .map { captureConfig -> captureConfig.templateType }
                    .contains(CameraDevice.TEMPLATE_STILL_CAPTURE)
            }
        }

    @Config(minSdk = 23)
    @Test
    fun submitZslCaptureRequests_withNoTemplate_templateStillPictureSent(): Unit = runBlocking {
        // Arrange.
        val imageCaptureConfig =
            CaptureConfig.Builder().let {
                it.addSurface(fakeStillCaptureSurface)
                it.build()
            }
        val cameraControl =
            initCameraControlWithZsl(
                isZslDisabledByFlashMode = false,
                isZslDisabledByUserCaseConfig = false
            )

        // Act.
        cameraControl
            .submitStillCaptureRequests(
                listOf(imageCaptureConfig),
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG,
                FLASH_MODE_OFF,
            )
            .await()

        // Assert.
        immediateCompleteCapture.verifyRequestResult { captureConfigList ->
            captureConfigList
                .filter { it.surfaces.contains(fakeStillCaptureSurface) }
                .map { captureConfig -> captureConfig.templateType }
                .contains(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }
    }

    @Test
    fun captureFailure_taskShouldFailure() {
        // Arrange.
        val immediateFailureCapture =
            object : CameraControlInternal.ControlUpdateCallback {

                override fun onCameraControlUpdateSessionConfig() {}

                override fun onCameraControlCaptureRequests(
                    captureConfigs: MutableList<CaptureConfig>
                ) {
                    captureConfigs.forEach { captureConfig ->
                        captureConfig.cameraCaptureCallbacks.forEach {
                            it.onCaptureFailed(
                                CaptureConfig.DEFAULT_ID,
                                CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR)
                            )
                        }
                    }
                }
            }
        val cameraControl = createCameraControl(updateCallback = immediateFailureCapture)

        // Act.
        val future =
            cameraControl.submitStillCaptureRequests(
                listOf(CaptureConfig.Builder().build()),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )

        // Assert.
        val exception =
            assertThrows(ExecutionException::class.java) { future.get(1, TimeUnit.SECONDS) }
        assertTrue(exception.cause is ImageCaptureException)
        assertThat((exception.cause as ImageCaptureException).imageCaptureError)
            .isEqualTo(ImageCapture.ERROR_CAPTURE_FAILED)
    }

    @Test
    fun captureCancel_taskShouldFailureWithCAMERA_CLOSED() {
        // Arrange.
        val immediateCancelCapture =
            object : CameraControlInternal.ControlUpdateCallback {

                override fun onCameraControlUpdateSessionConfig() {}

                override fun onCameraControlCaptureRequests(
                    captureConfigs: MutableList<CaptureConfig>
                ) {
                    captureConfigs.forEach { captureConfig ->
                        captureConfig.cameraCaptureCallbacks.forEach {
                            it.onCaptureCancelled(CaptureConfig.DEFAULT_ID)
                        }
                    }
                }
            }
        val cameraControl = createCameraControl(updateCallback = immediateCancelCapture)

        // Act.
        val future =
            cameraControl.submitStillCaptureRequests(
                listOf(CaptureConfig.Builder().build()),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )

        // Assert.
        val exception =
            assertThrows(ExecutionException::class.java) { future.get(1, TimeUnit.SECONDS) }
        assertTrue(exception.cause is ImageCaptureException)
        assertThat((exception.cause as ImageCaptureException).imageCaptureError)
            .isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
    }

    @Test
    fun overrideAeModeForStillCapture_quirkAbsent_notOverride(): Unit = runBlocking {
        // Arrange. Not have the quirk.
        val cameraControl =
            createCameraControl(quirks = Quirks(emptyList())).apply {
                flashMode = FLASH_MODE_ON // Set flash ON to enable aePreCapture
                simulateRepeatingResult(initialDelay = 100) // Make sures flashMode is updated.
            }

        // Act.
        val deferred =
            cameraControl.submitStillCaptureRequests(
                listOf(singleRequest),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        deferred.await()

        // Assert.
        // AE mode should not be overridden
        immediateCompleteCapture
            .getAllResults()
            .flatten()
            .filter { it.surfaces.contains(fakeStillCaptureSurface) }
            .let { stillCaptureRequests ->
                assertThat(stillCaptureRequests).isNotEmpty()
                stillCaptureRequests.forEach { config ->
                    assertThat(
                            config
                                .toCamera2Config()
                                .getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
                        )
                        .isNull()
                }
            }
    }

    @Test
    @Ignore("AutoFlashUnderExposedQuirk was disabled, ignoring the test.")
    fun overrideAeModeForStillCapture_aePrecaptureStarted_override(): Unit = runBlocking {
        // Arrange.
        val cameraControl =
            createCameraControl(quirks = Quirks(listOf(AutoFlashUnderExposedQuirk()))).apply {
                flashMode = FLASH_MODE_AUTO // Set flash auto to enable aePreCapture
                simulateRepeatingResult(
                    initialDelay = 100,
                    resultParameters =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                        )
                ) // Make sures flashMode is updated and the flash is required.
            }

        // Act.
        cameraControl.submitStillCaptureRequests(
            listOf(singleRequest),
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        // Assert.
        // AE mode should be overridden to CONTROL_AE_MODE_ON_ALWAYS_FLASH
        immediateCompleteCapture.verifyRequestResult { configList ->
            configList.requestContains(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
            ) && configList.surfaceContains(fakeStillCaptureSurface)
        }
    }

    @Test
    fun overrideAeModeForStillCapture_aePrecaptureFinish_notOverride(): Unit = runBlocking {
        // Arrange.
        val cameraControl =
            createCameraControl(quirks = Quirks(listOf(AutoFlashUnderExposedQuirk()))).apply {
                flashMode = FLASH_MODE_AUTO // Set flash auto to enable aePreCapture
                simulateRepeatingResult(
                    initialDelay = 100,
                    resultParameters =
                        mapOf(
                            CaptureResult.CONTROL_AE_STATE to
                                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                        )
                ) // Make sures flashMode is updated and the flash is required.
            }
        val firstCapture =
            cameraControl.submitStillCaptureRequests(
                listOf(singleRequest),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )
        firstCapture.await()
        immediateCompleteCapture.clearAllResults() // Clear the result of the firstCapture

        // Act.
        // Set flash OFF to disable aePreCapture for testing
        cameraControl.flashMode = FLASH_MODE_OFF
        val result =
            cameraControl
                .submitStillCaptureRequests(
                    listOf(singleRequest),
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                    ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
                )
                .await()

        // Assert. The second capturing should not override the AE mode.
        assertThat(result.size).isEqualTo(1)
        immediateCompleteCapture
            .getAllResults()
            .flatten()
            .filter { it.surfaces.contains(fakeStillCaptureSurface) }
            .let { stillCaptureRequests ->
                assertThat(stillCaptureRequests).isNotEmpty()
                stillCaptureRequests.forEach { config ->
                    assertThat(
                            config
                                .toCamera2Config()
                                .getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
                        )
                        .isNull()
                }
            }
    }

    @Test
    fun overrideAeModeForStillCapture_noAePrecaptureTriggered_notOverride(): Unit = runBlocking {
        // Arrange.
        val cameraControl =
            createCameraControl(quirks = Quirks(listOf(AutoFlashUnderExposedQuirk()))).apply {
                flashMode = FLASH_MODE_AUTO // Set flash auto to enable aePreCapture

                // Make sures flashMode is updated but the flash is not required.
                simulateRepeatingResult(initialDelay = 100)
            }

        // Act.
        val deferred =
            cameraControl.submitStillCaptureRequests(
                listOf(singleRequest),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )

        // Switch the repeating result to 3A converged state.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConverged
        )

        deferred.await()

        // Assert.
        // AE mode should not be overridden
        immediateCompleteCapture
            .getAllResults()
            .flatten()
            .filter { it.surfaces.contains(fakeStillCaptureSurface) }
            .let { stillCaptureRequests ->
                assertThat(stillCaptureRequests).isNotEmpty()
                stillCaptureRequests.forEach { config ->
                    assertThat(
                            config
                                .toCamera2Config()
                                .getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
                        )
                        .isNull()
                }
            }
    }

    @Test
    fun skip3AConvergenceInFlashOn_when3AModeOff(): Unit = runBlocking {
        // Arrange. Not have the quirk.
        val cameraControl =
            createCameraControl(quirks = Quirks(emptyList())).apply {
                flashMode = FLASH_MODE_ON // Set flash ON
                simulateRepeatingResult(initialDelay = 100) // Make sures flashMode is updated.
            }

        // Act.
        val deferred =
            cameraControl.submitStillCaptureRequests(
                listOf(singleRequest),
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
            )
        // Switch the repeating result to 3A converged state with 3A modes being set to OFF.
        cameraControl.simulateRepeatingResult(
            initialDelay = 500,
            resultParameters = resultConvergedWith3AModeOff
        )

        // Ensure 3A is converged (skips 3A check) and capture request is sent.
        withTimeout(2000) { assertThat(deferred.await()) }
    }

    @Test
    fun waitForResultCompletes_whenCaptureResultProvided_noTimeout_noCheckingCondition() {
        val cameraControl =
            createCameraControl().apply { simulateRepeatingResult(initialDelay = 1) }

        val future = Camera2CapturePipeline.waitForResult(cameraControl, null)

        future.get(500, TimeUnit.MILLISECONDS)
    }

    @Test
    fun waitForResultCompletes_whenCaptureResultProvided_noTimeout_specificCheckingCondition() {
        val cameraControl =
            createCameraControl().apply { simulateRepeatingResult(initialDelay = 1) }

        cameraControl.simulateRepeatingResult(initialDelay = 50, resultParameters = resultConverged)

        val future =
            Camera2CapturePipeline.waitForResult(cameraControl) { result ->
                Camera2CapturePipeline.is3AConverged(result, false)
            }

        future.get(500, TimeUnit.MILLISECONDS).verifyResultFields(resultConverged)
    }

    @Test
    fun waitForResultDoesNotComplete_whenNoResult_noCheckingCondition() {
        // tested for 500ms
        Camera2CapturePipeline.waitForResult(createCameraControl(), null)
            .awaitException(500, TimeoutException::class.java)
    }

    @Test
    fun waitForResultDoesNotComplete_whenNoMatchingResult() {
        // tested for 500ms
        Camera2CapturePipeline.waitForResult(
                createCameraControl().apply { simulateRepeatingResult(initialDelay = 1) }
            ) { result ->
                Camera2CapturePipeline.is3AConverged(result, false)
            }
            .awaitException(500, TimeoutException::class.java)
    }

    @Test
    fun waitForResultCompletesWithNullResult_whenNoResultWithinTimeout_noCheckingCondition() {
        val result =
            Camera2CapturePipeline.waitForResult(
                    TimeUnit.MILLISECONDS.toNanos(500),
                    executorService,
                    createCameraControl(),
                    null
                )
                .get(
                    1,
                    TimeUnit.SECONDS
                ) // timeout exception will be thrown if not completed within 1s

        assertThat(result).isNull()
    }

    @Test
    fun waitForResultCompletesWithNullResult_whenNoMatchingResultWithinTimeout() {
        val result =
            Camera2CapturePipeline.waitForResult(
                    TimeUnit.MILLISECONDS.toNanos(500),
                    executorService,
                    createCameraControl().apply { simulateRepeatingResult(initialDelay = 1) }
                ) { result ->
                    Camera2CapturePipeline.is3AConverged(result, false)
                }
                .get(
                    1,
                    TimeUnit.SECONDS
                ) // timeout exception will be thrown if not completed within 1s

        assertThat(result).isNull()
    }

    private fun TotalCaptureResult.verifyResultFields(
        expectedFields: Map<CaptureResult.Key<*>, Any>
    ) {
        assertThat(this).isNotNull()
        expectedFields.forEach { entry -> assertThat(this[entry.key]).isEqualTo(entry.value) }
    }

    private fun ListenableFuture<*>.awaitException(timeoutMillis: Long, exceptionType: Class<*>) {
        try {
            get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: ExecutionException) {
            if (exceptionType != ExecutionException::class.java) {
                assertThat(e.cause).isInstanceOf(exceptionType)
            }
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(exceptionType)
        }
    }

    private fun Camera2CameraControlImpl.waitForSessionConfig(
        checkResult: (sessionConfig: SessionConfig) -> Boolean = { true }
    ) {
        var verifyCount = 0
        while (true) {
            immediateCompleteCapture.waitForSessionConfigUpdate()
            if (checkResult(sessionConfig)) {
                return
            }
            Truth.assertWithMessage("Verify over 5 times").that(++verifyCount).isLessThan(5)
        }
    }

    private fun SessionConfig.isTorchParameterEnabled(): Boolean {
        val config = toCamera2Config()

        return config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, null) ==
            CaptureRequest.CONTROL_AE_MODE_ON &&
            config.getCaptureRequestOption(CaptureRequest.FLASH_MODE, null) ==
                CameraMetadata.FLASH_MODE_TORCH
    }

    private fun List<CaptureConfig>.isTorchParameterDisabled() =
        requestContains(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON,
        ) &&
            requestContains(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF,
            )

    private fun List<CaptureConfig>.requestContains(
        key: CaptureRequest.Key<*>,
        value: Any?
    ): Boolean {
        forEach { config ->
            if (value == config.toCamera2Config().getCaptureRequestOption(key, null)) {
                return true
            }
        }
        return false
    }

    private fun List<CaptureConfig>.surfaceContains(surface: DeferrableSurface): Boolean {
        forEach { config ->
            if (config.surfaces.contains(surface)) {
                return true
            }
        }
        return false
    }

    private fun Camera2CameraControlImpl.simulateRepeatingResult(
        initialDelay: Long = 100,
        period: Long = 100, // in milliseconds
        resultParameters: Map<CaptureResult.Key<*>, Any> = mutableMapOf(),
        requestCountLatch: CountDownLatch? = null,
        scheduledRunnableExecutor: Executor = executorService
    ) {
        runningRepeatingStream =
            executorService.scheduleAtFixedRate(
                {
                    scheduledRunnableExecutor.execute {
                        val tagBundle = sessionConfig.repeatingCaptureConfig.tagBundle
                        val requestOptions =
                            sessionConfig.repeatingCaptureConfig.implementationOptions
                        val resultOptions =
                            baseRepeatingResult.toMutableMap().apply { putAll(resultParameters) }
                        sendRepeatingResult(tagBundle, requestOptions.toParameters(), resultOptions)
                        requestCountLatch?.countDown()
                    }
                },
                initialDelay,
                period,
                TimeUnit.MILLISECONDS
            )
    }

    private fun Camera2CameraControlImpl.sendRepeatingResult(
        requestTag: Any? = null,
        requestParameters: Map<CaptureRequest.Key<*>, Any>,
        resultParameters: Map<CaptureResult.Key<*>, Any>,
    ) {
        val request = mock(CaptureRequest::class.java)
        Mockito.`when`(request.tag).thenReturn(requestTag)
        requestParameters.forEach { (key, any) -> Mockito.`when`(request.get(key)).thenReturn(any) }

        val result = mock(TotalCaptureResult::class.java)
        Mockito.`when`(result.request).thenReturn(request)
        resultParameters.forEach { (key, any) -> Mockito.`when`(result.get(key)).thenReturn(any) }

        sessionConfig.repeatingCameraCaptureCallbacks.toList().forEach {
            CaptureCallbackConverter.toCaptureCallback(it)
                .onCaptureCompleted(mock(CameraCaptureSession::class.java), request, result)
        }
    }

    private fun CaptureConfig.toCamera2Config() = Camera2ImplConfig(implementationOptions)

    private fun SessionConfig.toCamera2Config() = Camera2ImplConfig(implementationOptions)

    private fun createCameraControl(
        cameraId: String = CAMERA_ID_0,
        quirks: Quirks? = null,
        updateCallback: CameraControlInternal.ControlUpdateCallback = immediateCompleteCapture,
        addTorchFlashRequiredFor3aUpdateQuirk: Boolean = false,
        executor: Executor = executorService,
    ): Camera2CameraControlImpl {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val characteristicsCompat =
            CameraCharacteristicsCompat.toCameraCharacteristicsCompat(characteristics, cameraId)
        var cameraQuirk = quirks ?: CameraQuirks.get(cameraId, characteristicsCompat)

        if (addTorchFlashRequiredFor3aUpdateQuirk) {
            cameraQuirk =
                Quirks(
                    cameraQuirk.getAll(Quirk::class.java).apply {
                        add(TorchFlashRequiredFor3aUpdateQuirk(characteristicsCompat))
                    }
                )
        }

        return Camera2CameraControlImpl(
                characteristicsCompat,
                executorService,
                executor,
                updateCallback,
                cameraQuirk
            )
            .apply {
                setActive(true)
                incrementUseCount()
                this.screenFlash = testScreenFlash
            }
    }

    private fun initCameras() {
        Shadow.extract<ShadowCameraManager>(context.getSystemService(Context.CAMERA_SERVICE))
            .apply { addCamera(CAMERA_ID_0, intiCharacteristic0()) }
    }

    private fun intiCharacteristic0(): CameraCharacteristics {
        return ShadowCameraCharacteristics.newCameraCharacteristics().also {
            Shadow.extract<ShadowCameraCharacteristics>(it).apply {
                set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true)
                set(
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
                    intArrayOf(
                        CaptureRequest.CONTROL_AE_MODE_OFF,
                        CaptureRequest.CONTROL_AE_MODE_ON,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE,
                        CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH
                    )
                )
                set(CameraCharacteristics.LENS_FACING, CameraMetadata.LENS_FACING_BACK)
            }
        }
    }

    private val immediateCompleteCapture =
        object : CameraControlInternal.ControlUpdateCallback {
            private val lock = Any()
            private val allResults: MutableList<List<CaptureConfig>> = mutableListOf()
            val waitingList =
                mutableListOf<
                    Pair<CountDownLatch, (captureRequests: List<CaptureConfig>) -> Boolean>
                >()
            var updateSessionCountDown = CountDownLatch(1)

            fun verifyRequestResult(
                timeout: Long = TimeUnit.SECONDS.toMillis(5),
                verifyResults: (captureRequests: List<CaptureConfig>) -> Boolean = { true }
            ) {
                val resultPair = Pair(CountDownLatch(1), verifyResults)
                synchronized(lock) {
                    allResults.forEach {
                        if (verifyResults(it)) {
                            return
                        }
                    }
                    waitingList.add(resultPair)
                }
                assertTrue(resultPair.first.await(timeout, TimeUnit.MILLISECONDS))
                waitingList.remove(resultPair)
            }

            fun waitForSessionConfigUpdate(timeout: Long = TimeUnit.SECONDS.toMillis(5)) {
                // No matter onCameraControlUpdateSessionConfig is called before or after
                // the waitForSessionConfigUpdate call, the count down operation should be
                // executed correctly on the updateSessionCountDown object
                updateSessionCountDown.await(timeout, TimeUnit.MILLISECONDS)

                // Reset count down latch here for next call of waitForSessionConfigUpdate
                updateSessionCountDown = CountDownLatch(1)
            }

            override fun onCameraControlUpdateSessionConfig() {
                // Only count down when count is still larger than 1
                if (updateSessionCountDown.count > 0) {
                    updateSessionCountDown.countDown()
                }
            }

            override fun onCameraControlCaptureRequests(
                captureConfigs: MutableList<CaptureConfig>
            ) {
                synchronized(lock) { allResults.add(captureConfigs) }
                waitingList.toList().forEach {
                    if (it.second(captureConfigs)) {
                        it.first.countDown()
                    }
                }

                // Complete the single capture with an empty result.
                captureConfigs.forEach { captureConfig ->
                    captureConfig.cameraCaptureCallbacks.forEach {
                        it.onCaptureCompleted(
                            CaptureConfig.DEFAULT_ID,
                            CameraCaptureResult.EmptyCameraCaptureResult()
                        )
                    }
                }
            }

            fun clearAllResults() = synchronized(lock) { allResults.clear() }

            fun getAllResults() = synchronized(lock) { allResults.toList() }
        }

    /** Convert the Config to the CaptureRequest key-value map. */
    private fun androidx.camera.core.impl.Config.toParameters(): Map<CaptureRequest.Key<*>, Any> {
        val parameters = mutableMapOf<CaptureRequest.Key<*>, Any>()
        for (configOption in listOptions()) {
            val requestKey = configOption.token as? CaptureRequest.Key<*> ?: continue
            val value = retrieveOption(configOption) ?: continue
            parameters[requestKey] = value
        }

        return parameters
    }

    private fun createCameraCharacteristicsCompat(
        hasCapabilities: Boolean,
        isYuvReprocessingSupported: Boolean,
        isPrivateReprocessingSupported: Boolean
    ): CameraCharacteristicsCompat {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)

        val capabilities = arrayListOf<Int>()
        if (isYuvReprocessingSupported) {
            capabilities.add(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING)
        }
        if (isPrivateReprocessingSupported) {
            capabilities.add(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING
            )
        }

        if (hasCapabilities) {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                capabilities.toIntArray()
            )
        }

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            characteristics,
            CAMERA_ID_0
        )
    }

    @RequiresApi(23)
    private fun initCameraControlWithZsl(
        isZslDisabledByFlashMode: Boolean,
        isZslDisabledByUserCaseConfig: Boolean
    ): Camera2CameraControlImpl {
        val cameraControl =
            createCameraControl().apply { simulateRepeatingResult(initialDelay = 100) }

        val zslControl =
            ZslControlImpl(
                createCameraCharacteristicsCompat(
                    hasCapabilities = true,
                    isYuvReprocessingSupported = true,
                    isPrivateReprocessingSupported = true
                )
            )

        // Only need to initialize when not disabled
        if (!isZslDisabledByFlashMode && !isZslDisabledByUserCaseConfig) {
            val captureResult = FakeCameraCaptureResult()
            captureResult.afState = AfState.LOCKED_FOCUSED
            captureResult.aeState = AeState.CONVERGED
            captureResult.awbState = AwbState.CONVERGED
            val imageProxy = FakeImageProxy(CameraCaptureResultImageInfo(captureResult))
            imageProxy.image = mock(Image::class.java)
            zslControl.mImageRingBuffer.enqueue(imageProxy)
            zslControl.mReprocessingImageWriter = mock(ImageWriter::class.java)
        }

        zslControl.isZslDisabledByFlashMode = isZslDisabledByFlashMode
        zslControl.isZslDisabledByUserCaseConfig = isZslDisabledByUserCaseConfig

        cameraControl.mZslControl = zslControl

        return cameraControl
    }

    private fun Looper.advanceUntilIdle() {
        val shadowLooper = Shadows.shadowOf(this)
        while (!shadowLooper.isIdle) {
            shadowLooper.idle()
        }
    }
}
