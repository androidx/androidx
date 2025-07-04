/*
 * Copyright 2024 The Android Open Source Project
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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult.CONTROL_LOW_LIGHT_BOOST_STATE
import android.hardware.camera2.TotalCaptureResult
import android.os.Looper.getMainLooper
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.core.CameraControl.OperationCanceledException
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLog

private const val CAMERA_ID_0 = "0"

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 35)
class LowLightBoostControlTest {

    private val context = ApplicationProvider.getApplicationContext() as Context

    @Test
    fun enableLowLightBoostThrowException_stateActive_whenIsSupported() {
        ShadowLog.stream = System.out
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            val future = enableLowLightBoost(true)

            // Issue CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY AE mode capture result
            // to check whether the future can be completed or not.
            issueControlAeModeCaptureResult(mCaptureResultListener)
            shadowOf(getMainLooper()).idle()

            future.get(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun enableLowLightBoost_stateInactive_whenIsSupported() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(false)
            val future = enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()
            assertFutureCompleteWithException(future, OperationCanceledException::class.java)
        }
    }

    @Test
    fun enableLowLightBoostThrowException_stateActive_whenIsNotSupported() {
        initCamera(supportedLlb = false)
        createLowLightBoostControl().apply {
            setActive(true)
            val future = enableLowLightBoost(true)
            assertFutureCompleteWithException(future, IllegalStateException::class.java)
        }
    }

    @Test
    fun getLowLightBoostState_stateActive_whenIsNotSupported() {
        initCamera(supportedLlb = false)
        createLowLightBoostControl().apply {
            setActive(true)
            enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.OFF)
        }
    }

    @Test
    fun getLowLightBoostState_stateActive_whenIsSupported() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            // State is OFF before low-light boost is enabled
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.OFF)

            enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()
            // State is INACTIVE after low-light boost is enabled but no ACTIVE state is received
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.INACTIVE)

            issueControlAeModeCaptureResult(
                mCaptureResultListener,
                llbState = LowLightBoostState.ACTIVE,
            )
            // When low-light boost state is updated, postValue method is invoked. In robolectric
            // test, it is hard to know when will the task is added to the main thread. Adding
            // delay 100ms here to make sure that the value update task has been posted to the main
            // thread.
            runBlocking { delay(100) }
            shadowOf(getMainLooper()).idle()
            // State is ACTIVE after ACTIVE state is received
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.ACTIVE)

            issueControlAeModeCaptureResult(
                mCaptureResultListener,
                llbState = LowLightBoostState.INACTIVE,
            )
            runBlocking { delay(100) }
            shadowOf(getMainLooper()).idle()
            // State is INACTIVE after INACTIVE state is received again
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.INACTIVE)
        }
    }

    @Test
    fun getLowLightBoostState_stateInactive_whenIsSupported() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(false)
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.OFF)
        }
    }

    @Test
    fun enableTorchTwice_cancelPreviousFuture() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            val future1 = enableLowLightBoost(true)
            val future2 = enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()

            // Verifies that the first future has been canceled.
            assertFutureCompleteWithException(future1, OperationCanceledException::class.java)

            // Verifies that the second future can be completed after issue the AE mode capture
            // result
            issueControlAeModeCaptureResult(mCaptureResultListener)
            shadowOf(getMainLooper()).idle()
            future2.get(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun setInActive_cancelPreviousFuture() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            val future = enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()

            setActive(false)
            assertFutureCompleteWithException(future, OperationCanceledException::class.java)
        }
    }

    @Test
    fun setInActive_changeToStateOff() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            enableLowLightBoost(true)
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.INACTIVE)

            setActive(false)
            shadowOf(getMainLooper()).idle()
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.OFF)
        }
    }

    @Test
    fun enableLowLightBoostThrowException_stateActive_whenDisabledByUseCaseConfig() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            setLowLightBoostDisabledByUseCaseSessionConfig(true)
            val future = enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()
            assertFutureCompleteWithException(future, IllegalStateException::class.java)
        }
    }

    @Test
    fun lowLightBoostCanBeDisabledByUseCaseConfig() {
        initCamera(supportedLlb = true)
        createLowLightBoostControl().apply {
            setActive(true)
            enableLowLightBoost(true)
            shadowOf(getMainLooper()).idle()
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.INACTIVE)

            // Sets low-light boost disabled flag
            setLowLightBoostDisabledByUseCaseSessionConfig(true)
            runBlocking { delay(100) }
            shadowOf(getMainLooper()).idle()

            // Verifies that low-light boost is turned off
            assertThat(mTargetLlbEnabled).isFalse()
            assertThat(lowLightBoostState.value).isEqualTo(LowLightBoostState.OFF)
        }
    }

    private fun assertFutureCompleteWithException(future: ListenableFuture<Void>, clazz: Class<*>) {
        assertThat(future.isDone).isTrue()
        try {
            future.get()
        } catch (exception: ExecutionException) {
            assertThat(exception.cause).isInstanceOf(clazz)
        }
    }

    private fun createLowLightBoostControl(): LowLightBoostControl {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(CAMERA_ID_0)
        val characteristicsCompat =
            CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                cameraCharacteristics,
                CAMERA_ID_0,
            )

        val cameraControlImpl =
            Camera2CameraControlImpl(
                characteristicsCompat,
                CameraXExecutors.mainThreadExecutor(),
                CameraXExecutors.mainThreadExecutor(),
                mock(CameraControlInternal.ControlUpdateCallback::class.java),
            )

        return LowLightBoostControl(
            cameraControlImpl,
            characteristicsCompat,
            CameraXExecutors.mainThreadExecutor(),
        )
    }

    private fun issueControlAeModeCaptureResult(
        captureResultListener: Camera2CameraControlImpl.CaptureResultListener,
        resultAeMode: Int = CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY,
        llbState: Int? = null,
    ) {
        Executors.newSingleThreadScheduledExecutor()
            .schedule(
                {
                    val captureRequest = mock(CaptureRequest::class.java)
                    Mockito.`when`(captureRequest.get(CaptureRequest.CONTROL_AE_MODE))
                        .thenReturn(resultAeMode)

                    val totalCaptureResult = mock(TotalCaptureResult::class.java)
                    Mockito.`when`(totalCaptureResult.request).thenReturn(captureRequest)

                    llbState?.let {
                        Mockito.`when`(totalCaptureResult.get(CONTROL_LOW_LIGHT_BOOST_STATE))
                            .thenReturn(llbState)
                    }
                    captureResultListener.onCaptureResult(totalCaptureResult)
                },
                20,
                TimeUnit.MILLISECONDS,
            )
    }

    private fun initCamera(supportedLlb: Boolean) {
        val cameraCharacteristics =
            ShadowCameraCharacteristics.newCameraCharacteristics().also {
                Shadow.extract<ShadowCameraCharacteristics>(it).apply {
                    set(
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
                        mutableListOf<Int>()
                            .apply {
                                add(CaptureRequest.CONTROL_AE_MODE_OFF)
                                add(CaptureRequest.CONTROL_AE_MODE_ON)
                                add(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                                add(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                                add(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE)
                                add(CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH)

                                if (supportedLlb) {
                                    add(CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY)
                                }
                            }
                            .toIntArray(),
                    )
                    set(CameraCharacteristics.LENS_FACING, CameraMetadata.LENS_FACING_BACK)
                }
            }

        Shadow.extract<ShadowCameraManager>(context.getSystemService(Context.CAMERA_SERVICE))
            .apply { addCamera(CAMERA_ID_0, cameraCharacteristics) }
    }
}
