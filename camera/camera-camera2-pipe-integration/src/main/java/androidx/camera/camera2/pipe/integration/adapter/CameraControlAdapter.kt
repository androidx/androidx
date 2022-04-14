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
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.arch.core.util.Function
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.FlashControl
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.FutureChain
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.guava.asListenableFuture
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
    private val cameraStateAdapter: CameraStateAdapter,
    private val evCompControl: EvCompControl,
    private val flashControl: FlashControl,
    private val torchControl: TorchControl,
    private val threads: UseCaseThreads,
    private val useCaseManager: UseCaseManager,
    private val zoomControl: ZoomControl,
    val camera2cameraControl: Camera2CameraControl,
) : CameraControlInternal {
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

    override fun enableTorch(torch: Boolean): ListenableFuture<Void> =
        Futures.nonCancellationPropagating(
            FutureChain.from(
                torchControl.setTorchAsync(torch).asListenableFuture()
            ).transform(
                Function { return@Function null }, CameraXExecutors.directExecutor()
            )
        )

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
        return flashControl.flashMode
    }

    override fun setFlashMode(flashMode: Int) {
        flashControl.setFlashAsync(flashMode)
    }

    override fun setExposureCompensationIndex(exposure: Int): ListenableFuture<Int> =
        Futures.nonCancellationPropagating(
            evCompControl.updateAsync(exposure).asListenableFuture()
        )

    override fun addZslConfig(resolution: Size, sessionConfigBuilder: SessionConfig.Builder) {
        // Override if Zero-Shutter Lag needs to add config to session config.
    }

    override fun submitStillCaptureRequests(
        captureConfigs: List<CaptureConfig>,
        captureMode: Int,
        flashType: Int,
    ): ListenableFuture<List<Void?>> {
        val camera = useCaseManager.camera
        checkNotNull(camera) { "Attempted to issue capture requests while the camera isn't ready." }

        val flashMode = flashMode
        // Prior to submitStillCaptures, wait until the pending flash mode session change is
        // completed. On some devices, AE preCapture triggered in submitStillCaptures may not
        // work properly if the repeating request to change the flash mode is not completed.
        return Futures.nonCancellationPropagating(
            threads.sequentialScope.async {
                flashControl.updateSignal.join()
                camera.requestControl.issueSingleCaptureAsync(
                    captureConfigs,
                    captureMode,
                    flashType,
                    flashMode,
                ).awaitAll()
            }.asListenableFuture()
        )
    }

    override fun getSessionConfig(): SessionConfig {
        warn { "TODO: getSessionConfig is not yet supported" }
        return SessionConfig.defaultEmptySessionConfig()
    }
}