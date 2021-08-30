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
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks
import androidx.camera.camera2.internal.compat.quirk.UseTorchAsFlashQuirk
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.HandlerUtil
import androidx.core.os.HandlerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

private const val CAMERA_ID_0 = "0"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP
)
class Camera2CameraControlImplTest {

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val controlUpdateCallback =
        mock(CameraControlInternal.ControlUpdateCallback::class.java)
    private lateinit var cameraControl: Camera2CameraControlImpl
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    @Before
    fun setUp() {
        initCameras()

        handlerThread = HandlerThread("ControlThread").apply { start() }
        handler = HandlerCompat.createAsync(handlerThread.looper)

        createCameraControl()
    }

    @After
    fun tearDown() {
        if (::handlerThread.isInitialized) {
            handlerThread.quitSafely()
        }
    }

    @Test
    fun triggerAf_captureRequestSent() {
        // Act.
        cameraControl.triggerAf()
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertAfTrigger()
    }

    @Test
    fun cancelAf_captureRequestSent() {
        // Act.
        cameraControl.triggerAf()
        HandlerUtil.waitForLooperToIdle(handler)

        reset(controlUpdateCallback)

        cameraControl.cancelAfAndFinishFlashSequence(true, false)
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertCancelAfTrigger()
    }

    @Test
    fun startFlashSequence_aePrecaptureSent() {
        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertAePrecaptureTrigger()
    }

    @Test
    fun startFlashSequence_flashModeWasSet() {
        // Act 1
        cameraControl.flashMode = ImageCapture.FLASH_MODE_ON
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert 1: ensures AePrecapture is not invoked.
        verify(controlUpdateCallback, never()).onCameraControlCaptureRequests(any())

        // Act 2: Send the CaptureResult
        val tagBundle = cameraControl.sessionConfig.repeatingCaptureConfig.tagBundle
        val mockCaptureRequest = mock(CaptureRequest::class.java)
        `when`(mockCaptureRequest.tag).thenReturn(tagBundle)
        val mockCaptureResult = mock(TotalCaptureResult::class.java)
        `when`(mockCaptureResult.request).thenReturn(mockCaptureRequest)
        for (cameraCaptureCallback in cameraControl.sessionConfig.repeatingCameraCaptureCallbacks) {
            val callback = CaptureCallbackConverter.toCaptureCallback(cameraCaptureCallback)
            callback.onCaptureCompleted(
                mock(CameraCaptureSession::class.java),
                mockCaptureRequest, mockCaptureResult
            )
        }
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert 2: AePrecapture is triggered.
        assertAePrecaptureTrigger()
    }

    @Config(minSdk = 23)
    @Test
    fun finishFlashSequence_cancelAePrecaptureSent() {
        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        reset(controlUpdateCallback)

        cameraControl.cancelAfAndFinishFlashSequence(false, true)
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertCancelAePrecaptureTrigger()
    }

    @Test
    fun cancelAfAndFinishFlashSequence_cancelAfAndAePrecaptureSent() {
        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        reset(controlUpdateCallback)

        cameraControl.cancelAfAndFinishFlashSequence(true, true)
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertCancelAfTrigger()
        assertCancelAePrecaptureTrigger()
    }

    @Test
    fun startFlashSequence_withTorchAsFlashQuirk_enableTorchSent() {
        // Arrange.
        createCameraControl(quirks = Quirks(listOf(object : UseTorchAsFlashQuirk {})))

        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertTorchEnable()
    }

    @Test
    fun startFlashSequence_withTemplateRecord_enableTorchSent() {
        // Arrange.
        cameraControl.setTemplate(CameraDevice.TEMPLATE_RECORD)

        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertTorchEnable()
    }

    @Test
    fun finishFlashSequence_withUseTorchAsFlashQuirk_disableTorch() {
        // Arrange.
        createCameraControl(quirks = Quirks(listOf(object : UseTorchAsFlashQuirk {})))

        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        reset(controlUpdateCallback)

        cameraControl.cancelAfAndFinishFlashSequence(false, true)
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        assertTorchDisable()
    }

    @Test
    fun startFlashSequence_withUseTorchAsFlashQuirk_torchIsAlreadyOn() {
        // Arrange.
        createCameraControl(quirks = Quirks(listOf(object : UseTorchAsFlashQuirk {})))
        cameraControl.enableTorchInternal(true)
        HandlerUtil.waitForLooperToIdle(handler)
        reset(controlUpdateCallback)

        // Act.
        cameraControl.startFlashSequence()
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        verify(controlUpdateCallback, never()).onCameraControlCaptureRequests(any())
        verify(controlUpdateCallback, never()).onCameraControlUpdateSessionConfig()

        // Arrange.
        reset(controlUpdateCallback)

        // Act.
        cameraControl.cancelAfAndFinishFlashSequence(false, true)
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        verify(controlUpdateCallback, never()).onCameraControlCaptureRequests(any())
        verify(controlUpdateCallback, never()).onCameraControlUpdateSessionConfig()
    }

    @Test
    fun submitStillCaptureRequests_withTemplate_templateSent() {
        // Arrange.
        val imageCaptureConfig = CaptureConfig.Builder().let {
            it.templateType = CameraDevice.TEMPLATE_MANUAL
            it.build()
        }

        // Act.
        cameraControl.submitStillCaptureRequests(listOf(imageCaptureConfig))
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        val captureConfig = getIssuedCaptureConfig()
        assertThat(captureConfig.templateType).isEqualTo(CameraDevice.TEMPLATE_MANUAL)
    }

    @Test
    fun submitStillCaptureRequests_withNoTemplate_templateStillCaptureSent() {
        // Arrange.
        val imageCaptureConfig = CaptureConfig.Builder().build()

        // Act.
        cameraControl.submitStillCaptureRequests(listOf(imageCaptureConfig))
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        val captureConfig = getIssuedCaptureConfig()
        assertThat(captureConfig.templateType).isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE)
    }

    @Test
    fun submitStillCaptureRequests_withTemplateRecord_templateVideoSnapshotSent() {
        // Arrange.
        cameraControl.setTemplate(CameraDevice.TEMPLATE_RECORD)
        val imageCaptureConfig = CaptureConfig.Builder().build()

        // Act.
        cameraControl.submitStillCaptureRequests(listOf(imageCaptureConfig))
        HandlerUtil.waitForLooperToIdle(handler)

        // Assert.
        val captureConfig = getIssuedCaptureConfig()
        assertThat(captureConfig.templateType).isEqualTo(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT)
    }

    private fun assertAfTrigger() {
        assertCamera2ConfigValue(
            getIssuedCaptureConfig().toCamera2Config(),
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
    }

    private fun assertCancelAfTrigger() {
        assertCamera2ConfigValue(
            getIssuedCaptureConfig().toCamera2Config(),
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
        )
    }

    private fun assertAePrecaptureTrigger() {
        assertCamera2ConfigValue(
            getIssuedCaptureConfig().toCamera2Config(),
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
        )
    }

    private fun assertCancelAePrecaptureTrigger() {
        if (Build.VERSION.SDK_INT < 23) {
            return
        }

        assertCamera2ConfigValue(
            getIssuedCaptureConfig().toCamera2Config(),
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL
        )
    }

    private fun assertTorchEnable() {
        val camera2Config = getIssuedSessionConfig().toCamera2Config()

        assertCamera2ConfigValue(
            camera2Config,
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON,
        )

        assertCamera2ConfigValue(
            camera2Config,
            CaptureRequest.FLASH_MODE,
            CameraMetadata.FLASH_MODE_TORCH,
        )
    }

    private fun assertTorchDisable() {
        val camera2Config = getIssuedCaptureConfig().toCamera2Config()

        assertCamera2ConfigValue(
            camera2Config,
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON
        )

        assertCamera2ConfigValue(
            camera2Config,
            CaptureRequest.FLASH_MODE,
            CaptureRequest.FLASH_MODE_OFF
        )
    }

    private fun getIssuedCaptureConfig(): CaptureConfig {
        @Suppress("UNCHECKED_CAST")
        val captor =
            ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<CaptureConfig>>
        verify(controlUpdateCallback).onCameraControlCaptureRequests(captor.capture())
        return captor.value[0]
    }

    private fun getIssuedSessionConfig(): SessionConfig {
        verify(controlUpdateCallback).onCameraControlUpdateSessionConfig()
        return cameraControl.sessionConfig
    }

    private fun <T> assertCamera2ConfigValue(
        camera2Config: Camera2ImplConfig,
        key: CaptureRequest.Key<T>,
        expectedValue: T,
        assertMsg: String = ""
    ) {
        assertWithMessage(assertMsg).that(camera2Config.getCaptureRequestOption(key, null))
            .isEqualTo(expectedValue)
    }

    private fun createCameraControl(
        cameraId: String = CAMERA_ID_0,
        quirks: Quirks? = null
    ) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val characteristicsCompat = CameraCharacteristicsCompat
            .toCameraCharacteristicsCompat(characteristics)
        val cameraQuirk = quirks ?: CameraQuirks.get(cameraId, characteristicsCompat)
        val executorService = CameraXExecutors.newHandlerExecutor(handler)

        cameraControl = Camera2CameraControlImpl(
            characteristicsCompat,
            executorService,
            executorService,
            controlUpdateCallback,
            cameraQuirk
        ).apply {
            setActive(true)
            incrementUseCount()
        }
    }

    private fun initCameras() {
        Shadow.extract<ShadowCameraManager>(
            context.getSystemService(Context.CAMERA_SERVICE)
        ).apply {
            addCamera(CAMERA_ID_0, intiCharacteristic0())
        }
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
                set(
                    CameraCharacteristics.LENS_FACING,
                    CameraMetadata.LENS_FACING_BACK
                )
            }
        }
    }
}

private fun CaptureConfig.toCamera2Config() = Camera2ImplConfig(implementationOptions)

private fun SessionConfig.toCamera2Config() = Camera2ImplConfig(implementationOptions)
