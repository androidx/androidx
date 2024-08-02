/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter.Companion.getStillCaptureTemplate
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.TorchIsClosedAfterImageCapturingQuirk
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * This is a workaround for b/228272227 where the Torch is unexpectedly closed after a single
 * capturing.
 *
 * If the Torch is enabled before performing a single capture, this workaround may turn the Torch
 * OFF then ON after the capturing.
 */
@UseCaseCameraScope
class CapturePipelineTorchCorrection
@Inject
constructor(
    cameraProperties: CameraProperties,
    private val capturePipelineImpl: CapturePipelineImpl,
    private val threads: UseCaseThreads,
    private val torchControl: TorchControl,
) : CapturePipeline {
    private val isLegacyDevice = cameraProperties.metadata.isHardwareLevelLegacy

    override suspend fun submitStillCaptures(
        configs: List<CaptureConfig>,
        requestTemplate: RequestTemplate,
        sessionConfigOptions: Config,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int
    ): List<Deferred<Void?>> {
        val needCorrectTorchState = isCorrectionRequired(configs, requestTemplate)

        // Forward the capture request to capturePipelineImpl
        val deferredResults =
            capturePipelineImpl.submitStillCaptures(
                configs,
                requestTemplate,
                sessionConfigOptions,
                captureMode,
                flashType,
                flashMode
            )

        if (needCorrectTorchState) {
            threads.sequentialScope.launch {
                deferredResults.joinAll()
                Log.debug { "Re-enable Torch to correct the Torch state" }
                torchControl.setTorchAsync(torch = false).join()
                torchControl.setTorchAsync(torch = true).join()
                Log.debug { "Re-enable Torch to correct the Torch state, done" }
            }
        }

        return deferredResults
    }

    override var template: Int = CameraDevice.TEMPLATE_PREVIEW
        set(value) {
            capturePipelineImpl.template = value
            field = value
        }

    /**
     * Return true means the Torch will be unexpectedly closed, and it requires turning on the Torch
     * again after the capturing.
     */
    private fun isCorrectionRequired(
        captureConfigs: List<CaptureConfig>,
        requestTemplate: RequestTemplate,
    ): Boolean {
        return captureConfigs.any {
            it.getStillCaptureTemplate(
                    requestTemplate,
                    isLegacyDevice,
                )
                .value == CameraDevice.TEMPLATE_STILL_CAPTURE
        } && isTorchOn()
    }

    private fun isTorchOn() = torchControl.torchStateLiveData.value == TorchState.ON

    companion object {
        val isEnabled = DeviceQuirks[TorchIsClosedAfterImageCapturingQuirk::class.java] != null
    }
}
