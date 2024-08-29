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
import androidx.arch.core.util.Function
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FlashControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.StillCaptureRequestControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureChain
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Adapt the [CameraControlInternal] interface to [CameraPipe].
 *
 * This controller class maintains state as use-cases are attached / detached from the camera as
 * well as providing access to other utility methods. The primary purpose of this class it to
 * forward these interactions to the currently configured [UseCaseCamera].
 */
@SuppressLint("UnsafeOptInUsageError")
@CameraScope
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalCamera2Interop::class)
public class CameraControlAdapter
@Inject
constructor(
    private val cameraProperties: CameraProperties,
    private val evCompControl: EvCompControl,
    private val flashControl: FlashControl,
    private val focusMeteringControl: FocusMeteringControl,
    private val stillCaptureRequestControl: StillCaptureRequestControl,
    private val torchControl: TorchControl,
    private val zoomControl: ZoomControl,
    private val zslControl: ZslControl,
    public val camera2cameraControl: Camera2CameraControl,
) : CameraControlInternal {
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

    override fun enableTorch(torch: Boolean): ListenableFuture<Void> =
        Futures.nonCancellationPropagating(
            FutureChain.from(torchControl.setTorchAsync(torch).asListenableFuture())
                .transform(
                    Function {
                        return@Function null
                    },
                    CameraXExecutors.directExecutor()
                )
        )

    override fun startFocusAndMetering(
        action: FocusMeteringAction
    ): ListenableFuture<FocusMeteringResult> =
        Futures.nonCancellationPropagating(focusMeteringControl.startFocusAndMetering(action))

    override fun cancelFocusAndMetering(): ListenableFuture<Void> {
        return Futures.nonCancellationPropagating(
            CompletableDeferred<Void?>()
                .also {
                    // Convert to null once the task is done, ignore the results.
                    focusMeteringControl.cancelFocusAndMeteringAsync().propagateTo(it) { null }
                }
                .asListenableFuture()
        )
    }

    override fun setZoomRatio(ratio: Float): ListenableFuture<Void> =
        zoomControl.setZoomRatio(ratio)

    override fun setLinearZoom(linearZoom: Float): ListenableFuture<Void> =
        zoomControl.setLinearZoom(linearZoom)

    override fun getFlashMode(): Int {
        return flashControl.flashMode
    }

    override fun setFlashMode(@ImageCapture.FlashMode flashMode: Int) {
        flashControl.setFlashAsync(flashMode)
        zslControl.setZslDisabledByFlashMode(
            flashMode == FLASH_MODE_ON || flashMode == FLASH_MODE_AUTO
        )
    }

    override fun setScreenFlash(screenFlash: ImageCapture.ScreenFlash?) {
        flashControl.setScreenFlash(screenFlash)
    }

    override fun setExposureCompensationIndex(exposure: Int): ListenableFuture<Int> =
        Futures.nonCancellationPropagating(evCompControl.updateAsync(exposure).asListenableFuture())

    override fun setZslDisabledByUserCaseConfig(disabled: Boolean) {
        zslControl.setZslDisabledByUserCaseConfig(disabled)
    }

    override fun isZslDisabledByByUserCaseConfig(): Boolean {
        return zslControl.isZslDisabledByUserCaseConfig()
    }

    override fun addZslConfig(sessionConfigBuilder: SessionConfig.Builder) {
        zslControl.addZslConfig(sessionConfigBuilder)
    }

    override fun submitStillCaptureRequests(
        captureConfigs: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): ListenableFuture<List<Void?>> =
        stillCaptureRequestControl.issueCaptureRequests(captureConfigs, captureMode, flashType)

    override fun getSessionConfig(): SessionConfig {
        warn { "TODO: getSessionConfig is not yet supported" }
        return SessionConfig.defaultEmptySessionConfig()
    }
}
