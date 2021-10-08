/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Adapt the [CameraControlInternal] interface to [CameraPipe].
 *
 * This controller class maintains state as use-cases are attached / detached from the camera as
 * well as providing access to other utility methods. The primary purpose of this class it to
 * forward these interactions to the currently configured [UseCaseCamera].
 */
@SuppressLint("UnsafeOptInUsageError")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalCamera2Interop::class)
class CameraControlAdapter @Inject constructor(
    private val cameraProperties: CameraProperties,
    private val threads: UseCaseThreads,
    private val useCaseManager: UseCaseManager,
    private val cameraStateAdapter: CameraStateAdapter,
    private val zoomControl: ZoomControl,
    private val evCompControl: EvCompControl,
    val camera2cameraControl: Camera2CameraControl,
) : CameraControlInternal {
    private var imageCaptureFlashMode: Int = ImageCapture.FLASH_MODE_OFF

    private val focusMeteringControl = FocusMeteringControl(
        cameraProperties,
        useCaseManager,
        threads
    )

    override fun getSensorRect(): Rect {
        return cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
    }

    override fun addInteropConfig(config: Config) {
        camera2cameraControl.addCaptureRequestOptions(
            CaptureRequestOptions.Builder.from(config).build()
        )
    }

    override fun clearInteropConfig() {
        camera2cameraControl.clearCaptureRequestOptions()
    }

    override fun getInteropConfig(): Config {
        return camera2cameraControl.getCaptureRequestOptions()
    }

    override fun enableTorch(torch: Boolean): ListenableFuture<Void> {
        // Launch UNDISPATCHED to preserve interaction order with the camera.
        return threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            useCaseManager.camera?.let {
                // Tell the camera to turn the torch on / off.
                val result = it.setTorchAsync(torch)

                // Update the torch state
                cameraStateAdapter.setTorchState(
                    when (torch) {
                        true -> TorchState.ON
                        false -> TorchState.OFF
                    }
                )

                // Wait until the command is received by the camera.
                result.await()
            }
        }.asListenableFuture()
    }

    override fun startFocusAndMetering(
        action: FocusMeteringAction
    ): ListenableFuture<FocusMeteringResult> {
        // TODO(sushilnath@): use preview aspect ratio instead of sensor active array aspect ratio.
        val sensorAspectRatio = Rational(sensorRect.width(), sensorRect.height())
        return focusMeteringControl.startFocusAndMetering(action, sensorAspectRatio)
    }

    override fun cancelFocusAndMetering(): ListenableFuture<Void> {
        warn { "TODO: cancelFocusAndMetering is not yet supported" }
        return Futures.immediateFuture(null)
    }

    override fun setZoomRatio(ratio: Float): ListenableFuture<Void> {
        return threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            useCaseManager.camera?.let {
                zoomControl.zoomRatio = ratio
                val zoomValue = ZoomValue(
                    ratio,
                    zoomControl.minZoom,
                    zoomControl.maxZoom
                )
                cameraStateAdapter.setZoomState(zoomValue)
            }
        }.asListenableFuture()
    }

    override fun setLinearZoom(linearZoom: Float): ListenableFuture<Void> {
        val ratio = zoomControl.toZoomRatio(linearZoom)
        return setZoomRatio(ratio)
    }

    override fun getFlashMode(): Int {
        return imageCaptureFlashMode
    }

    override fun setFlashMode(flashMode: Int) {
        warn { "TODO: setFlashMode is not yet supported" }
        this.imageCaptureFlashMode = flashMode
    }

    override fun triggerAf(): ListenableFuture<CameraCaptureResult> {
        warn { "TODO: triggerAf is not yet supported" }
        return Futures.immediateFuture(CameraCaptureResult.EmptyCameraCaptureResult.create())
    }

    override fun startFlashSequence(
        @ImageCapture.FlashType flashType: Int
    ): ListenableFuture<Void> {
        warn { "TODO: startFlashSequence is not yet supported" }
        return Futures.immediateFuture(null)
    }

    override fun cancelAfAndFinishFlashSequence(
        cancelAfTrigger: Boolean,
        finishFlashSequence: Boolean
    ) {
        warn { "TODO: cancelAfAndFinishFlashSequence is not yet supported" }
    }

    override fun setExposureCompensationIndex(exposure: Int): ListenableFuture<Int> =
        Futures.nonCancellationPropagating(
            evCompControl.updateAsync(exposure).asListenableFuture()
        )

    override fun submitStillCaptureRequests(captureConfigs: List<CaptureConfig>) {
        val camera = useCaseManager.camera
        checkNotNull(camera) { "Attempted to issue capture requests while the camera isn't ready." }
        camera.capture(captureConfigs)
    }

    override fun getSessionConfig(): SessionConfig {
        warn { "TODO: getSessionConfig is not yet supported" }
        return SessionConfig.defaultEmptySessionConfig()
    }
}