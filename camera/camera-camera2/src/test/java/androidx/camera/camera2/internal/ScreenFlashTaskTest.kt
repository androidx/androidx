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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Looper.getMainLooper
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.quirk.TorchFlashRequiredFor3aUpdateQuirk
import androidx.camera.camera2.internal.compat.workaround.UseFlashModeTorchFor3aUpdate
import androidx.camera.core.ImageCapture.ScreenFlash
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.Quirks
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowSystem
import org.robolectric.shadows.ShadowTotalCaptureResult

@Config(minSdk = 21)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class ScreenFlashTaskTest {
    private val executorService = Executors.newSingleThreadScheduledExecutor()

    private var cameraCharacteristics = createCameraCharacteristicsCompat()
    private val screenFlash = MockScreenFlash()

    private lateinit var cameraControl: FakeCamera2CameraControlImpl

    @After
    fun tearDown() {
        executorService.shutdown()
    }

    @Test
    fun screenFlashApplyInvokedWithAtLeast3sTimeout_whenPreCaptureCalled() {
        val screenFlashTask = createScreenFlashTask()
        val initialTime = ShadowSystem.currentTimeMillis()

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle()

        assertThat(screenFlash.lastApplyExpirationTimeMillis).isAtLeast(
            initialTime + TimeUnit.SECONDS.toMillis(
                3
            )
        )
    }

    @Test
    fun screenFlashApplyInvokedInMainThread_whenPreCaptureCalled() {
        val screenFlashTask = createScreenFlashTask()

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle()

        assertThat(screenFlash.lastApplyThreadLooper).isEqualTo(getMainLooper())
    }

    @Config(minSdk = 28)
    @Test
    fun externalFlashAeModeEnabled_whenPreCaptureCalled() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = true)
        val screenFlashTask = createScreenFlashTask()

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.focusMeteringControl.externalFlashAeModeEnabled).isEqualTo(true)
    }

    @Config(minSdk = 28)
    @Test
    fun externalFlashAeModeEnabled_whenScreenFlashApplyNotCompleted() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = true)
        val screenFlashTask = createScreenFlashTask()
        screenFlash.setApplyCompletedInstantly(false)

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.focusMeteringControl.externalFlashAeModeEnabled).isEqualTo(true)
    }

    @Test
    fun torchNotEnabled_whenPreCaptureCalledWithoutQuirk() {
        val screenFlashTask = createScreenFlashTask(addTorchFlashRequiredQuirk = false)

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.isTorchEnabled).isFalse()
    }

    @Config(minSdk = 28)
    @Test
    fun torchNotEnabled_whenPreCaptureCalledWithQuirk_butExternalFlashAeModeSupported() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = true)
        val screenFlashTask = createScreenFlashTask(addTorchFlashRequiredQuirk = true)

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.isTorchEnabled).isFalse()
    }

    @Test
    fun torchEnabled_whenPreCaptureCalledWithQuirk_andNoExternalFlashAeMode() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = false)
        val screenFlashTask = createScreenFlashTask(addTorchFlashRequiredQuirk = true)

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.isTorchEnabled).isTrue()
    }

    @Test
    fun torchEnabled_whenScreenFlashApplyNotCompleted() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = false)
        val screenFlashTask = createScreenFlashTask(addTorchFlashRequiredQuirk = true)
        screenFlash.setApplyCompletedInstantly(false)

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.isTorchEnabled).isTrue()
    }

    @Test
    fun aePrecaptureTriggered_whenPreCaptureCalled() {
        val screenFlashTask = createScreenFlashTask()

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.focusMeteringControl.triggerAePrecaptureCount).isEqualTo(1)
    }

    @Test
    fun aePrecaptureNotTriggered_whenScreenFlashApplyNotCompleted() {
        val screenFlashTask = createScreenFlashTask()
        screenFlash.setApplyCompletedInstantly(false)

        screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        assertThat(cameraControl.focusMeteringControl.triggerAePrecaptureCount).isEqualTo(0)
    }

    @Test
    fun preCaptureIncompleteUntilTimeout_without3aConverge() {
        val screenFlashTask = createScreenFlashTask()

        val future = screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        future.awaitException(1000, TimeoutException::class.java)
    }

    @Test
    fun preCaptureCompletes_when3aConverges() {
        val screenFlashTask = createScreenFlashTask()

        val future = screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        cameraControl.notifyCaptureResultListeners(RESULT_CONVERGED)

        future.await(1000)
    }

    @Test
    fun preCaptureCompletesByTimeout_without3aConverge() {
        val screenFlashTask = createScreenFlashTask()

        val future = screenFlashTask.preCapture(null)
        shadowOf(getMainLooper()).idle() // ScreenFlash#apply is invoked in main thread

        future.await(3000)
    }

    @Test
    fun screenFlashClearInvokedInMainThread_whenPostCaptureCalled() {
        val screenFlashTask = createScreenFlashTask()

        screenFlashTask.postCapture()
        shadowOf(getMainLooper()).idle()

        assertThat(screenFlash.lastClearThreadLooper).isEqualTo(getMainLooper())
    }

    @Test
    fun torchDisabled_whenPostCaptureCalledWithQuirk_andNoExternalFlashAeMode() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = false)
        val screenFlashTask = createScreenFlashTask(addTorchFlashRequiredQuirk = true)
        screenFlashTask.preCapture(null)

        screenFlashTask.postCapture()

        assertThat(cameraControl.isTorchEnabled).isFalse()
    }

    @Config(minSdk = 28)
    @Test
    fun externalFlashAeModeDisabled_whenPostCaptureCalled() {
        cameraCharacteristics = createCameraCharacteristicsCompat(addExternalFlashAeMode = true)
        val screenFlashTask = createScreenFlashTask()
        screenFlashTask.preCapture(null)

        screenFlashTask.postCapture()

        assertThat(cameraControl.focusMeteringControl.externalFlashAeModeEnabled).isEqualTo(false)
    }

    @Test
    fun afAeTriggerCancelled_whenPostCaptureCalled() {
        val screenFlashTask = createScreenFlashTask()

        screenFlashTask.postCapture()
        shadowOf(getMainLooper()).idle()

        assertThat(cameraControl.focusMeteringControl.cancelAfAeTriggerCount).isEqualTo(1)
    }

    private fun createScreenFlashTask(
        addTorchFlashRequiredQuirk: Boolean = false
    ): Camera2CapturePipeline.ScreenFlashTask {
        val quirks = Quirks(mutableListOf<Quirk>().apply {
            if (addTorchFlashRequiredQuirk) {
                add(TorchFlashRequiredFor3aUpdateQuirk(cameraCharacteristics))
            }
        })

        cameraControl = FakeCamera2CameraControlImpl(cameraCharacteristics, quirks, screenFlash)
        return Camera2CapturePipeline.ScreenFlashTask(
            cameraControl,
            MoreExecutors.directExecutor(),
            executorService,
            UseFlashModeTorchFor3aUpdate(quirks),
        )
    }

    private fun createCameraCharacteristicsCompat(
        addExternalFlashAeMode: Boolean = false,
    ) = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            ShadowCameraCharacteristics.newCameraCharacteristics().also {
                Shadow.extract<ShadowCameraCharacteristics>(it).apply {
                    if (addExternalFlashAeMode) {
                        set(
                            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,
                            intArrayOf(
                                CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH
                            )
                        )
                    }
                }
            }, CAMERA_ID
        )

    internal inner class FakeCamera2CameraControlImpl(
        cameraCharacteristics: CameraCharacteristicsCompat = createCameraCharacteristicsCompat(),
        quirks: Quirks,
        private val screenFlash: ScreenFlash,
    ) : Camera2CameraControlImpl(
        cameraCharacteristics,
        executorService,
        MoreExecutors.directExecutor(),
        object : CameraControlInternal.ControlUpdateCallback {
            override fun onCameraControlUpdateSessionConfig() {}

            override fun onCameraControlCaptureRequests(
                captureConfigs: MutableList<CaptureConfig>
            ) {}
        }
    ) {
        private lateinit var captureResultListeners: MutableList<CaptureResultListener>
        var isTorchEnabled = false

        private val focusMeteringControl = FakeFocusMeteringControl(this, quirks)

        init {
            // can be called from super class constructor
            if (!::captureResultListeners.isInitialized) {
                captureResultListeners = mutableListOf()
            }
        }

        override fun getFocusMeteringControl(): FakeFocusMeteringControl {
            return focusMeteringControl
        }

        override fun getScreenFlash(): ScreenFlash {
            return screenFlash
        }

        override fun enableTorchInternal(torch: Boolean) {
            isTorchEnabled = torch
        }

        override fun addCaptureResultListener(listener: CaptureResultListener) {
            // can be called from super class constructor
            if (!::captureResultListeners.isInitialized) {
                captureResultListeners = mutableListOf()
            }
            captureResultListeners.add(listener)
        }

        @Suppress("UNCHECKED_CAST")
        fun notifyCaptureResultListeners(resultParameters: Map<CaptureResult.Key<*>, *>) {
            captureResultListeners.forEach { listener ->
                val shadowCaptureResult = ShadowTotalCaptureResult()

                resultParameters.forEach { (k, v) ->
                    shadowCaptureResult.set(k as CaptureResult.Key<Any>, v as Any)
                }

                listener.onCaptureResult(ShadowTotalCaptureResult.newTotalCaptureResult())
            }
        }
    }

    private fun Future<*>.await(timeoutMillis: Long) = get(timeoutMillis, TimeUnit.MILLISECONDS)

    private fun <T : Throwable?> Future<*>.awaitException(
        timeoutMillis: Long,
        exceptionType: Class<T>
    ) {
        assertThrows(exceptionType) {
            get(timeoutMillis, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        private const val CAMERA_ID = "0"

        private val RESULT_CONVERGED: Map<CaptureResult.Key<*>, *> = mapOf(
            CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO,
            CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED,
            CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_CONVERGED,
        )
    }
}
