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
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsLowLightBoost
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FlashControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.LowLightBoostControl
import androidx.camera.camera2.pipe.integration.impl.StillCaptureRequestControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.VideoUsageControl
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraControl.OperationCanceledException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.LowLightBoostState
import androidx.camera.core.TorchState
import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
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
    private val lowLightBoostControl: LowLightBoostControl,
    private val zoomControl: ZoomControl,
    private val zslControl: ZslControl,
    public val camera2cameraControl: Camera2CameraControl,
    private val useCaseManager: UseCaseManager,
    private val threads: UseCaseThreads,
    private val videoUsageControl: VideoUsageControl,
) : CameraControlInternal {
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
        if (
            cameraProperties.metadata.supportsLowLightBoost &&
                lowLightBoostControl.lowLightBoostStateLiveData.value != LowLightBoostState.OFF
        ) {
            debug { "Unable to enable/disable torch when low-light boost is on." }
            return Futures.immediateFailedFuture<Void>(
                IllegalStateException(
                    "Torch can not be enabled/disable when low-light boost is on!"
                )
            )
        }

        return Futures.nonCancellationPropagating(
            torchControl.setTorchAsync(torch).asVoidListenableFuture()
        )
    }

    override fun setTorchStrengthLevel(torchStrengthLevel: Int): ListenableFuture<Void> =
        Futures.nonCancellationPropagating(
            torchControl.setTorchStrengthLevelAsync(torchStrengthLevel).asVoidListenableFuture()
        )

    override fun enableLowLightBoostAsync(lowLightBoost: Boolean): ListenableFuture<Void> {
        if (!cameraProperties.metadata.supportsLowLightBoost) {
            debug { "Unable to enable/disable low-light boost due to it is not supported." }
            return Futures.immediateFailedFuture<Void>(
                IllegalStateException("Low-light boost is not supported!")
            )
        }

        return Futures.nonCancellationPropagating(
            Futures.transformAsync(
                if (torchControl.torchStateLiveData.value == TorchState.ON) {
                    torchControl.setTorchAsync(false).asVoidListenableFuture()
                } else {
                    CompletableDeferred(Unit).apply { complete(Unit) }.asVoidListenableFuture()
                },
                {
                    lowLightBoostControl
                        .setLowLightBoostAsync(lowLightBoost)
                        .asVoidListenableFuture()
                },
                CameraXExecutors.directExecutor(),
            )
        )
    }

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

    override fun setLowLightBoostDisabledByUseCaseSessionConfig(disabled: Boolean) {
        lowLightBoostControl.setLowLightBoostDisabledByUseCaseSessionConfig(disabled)
    }

    override fun clearZslConfig() {
        zslControl.clearZslConfig()
    }

    override fun submitStillCaptureRequests(
        captureConfigs: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): ListenableFuture<List<Void?>> =
        stillCaptureRequestControl.issueCaptureRequests(captureConfigs, captureMode, flashType)

    override fun getCameraCapturePipelineAsync(
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): ListenableFuture<CameraCapturePipeline> {
        val camera =
            useCaseManager.camera
                ?: return Futures.immediateFailedFuture(
                    OperationCanceledException("Camera is not active.")
                )
        return threads.sequentialScope.future {
            camera.getCameraCapturePipeline(
                captureMode,
                flashControl.awaitFlashModeUpdate(),
                flashType,
            )
        }
    }

    override fun getSessionConfig(): SessionConfig {
        warn { "TODO: getSessionConfig is not yet supported" }
        return SessionConfig.defaultEmptySessionConfig()
    }

    override fun incrementVideoUsage() {
        videoUsageControl.incrementUsage()
    }

    override fun decrementVideoUsage() {
        videoUsageControl.decrementUsage()
    }

    override fun isInVideoUsage(): Boolean = videoUsageControl.isInVideoUsage()
}
