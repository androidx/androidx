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

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.InputRequest
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.toParameters
import androidx.camera.camera2.pipe.media.AndroidImage
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.impl.CameraCaptureResults
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import javax.inject.Inject

/**
 * Maps a [CaptureConfig] issued by CameraX (e.g. by the image capture use case) to a [Request] that
 * CameraPipe can submit to the camera.
 */
@UseCaseCameraScope
class CaptureConfigAdapter
@Inject
constructor(
    cameraProperties: CameraProperties,
    private val useCaseGraphConfig: UseCaseGraphConfig,
    private val zslControl: ZslControl,
    private val threads: UseCaseThreads,
    private val templateParamsOverride: TemplateParamsOverride,
) {
    private val isLegacyDevice = cameraProperties.metadata.isHardwareLevelLegacy

    /**
     * Maps [CaptureConfig] to [Request].
     *
     * @throws IllegalStateException When CaptureConfig does not have any surface or a CaptureConfig
     *   surface is not recognized in [UseCaseGraphConfig.surfaceToStreamMap]
     */
    @OptIn(ExperimentalGetImage::class)
    fun mapToRequest(
        captureConfig: CaptureConfig,
        requestTemplate: RequestTemplate,
        sessionConfigOptions: Config,
        additionalListeners: List<Request.Listener> = emptyList(),
    ): Request {
        val surfaces = captureConfig.surfaces
        check(surfaces.isNotEmpty()) {
            "Attempted to issue a capture without surfaces using $captureConfig"
        }

        val streamIdList =
            surfaces.map {
                checkNotNull(useCaseGraphConfig.surfaceToStreamMap[it]) {
                    "Attempted to issue a capture with an unrecognized surface: $it"
                }
            }

        val callbacks =
            CameraCallbackMap().apply {
                captureConfig.cameraCaptureCallbacks.forEach { callback ->
                    addCaptureCallback(callback, threads.sequentialExecutor)
                }
            }

        val configOptions = captureConfig.implementationOptions
        val optionBuilder = Camera2ImplConfig.Builder()

        // The override priority for implementation options
        // P1 Single capture options
        // P2 SessionConfig options
        optionBuilder.insertAllOptions(sessionConfigOptions)
        optionBuilder.insertAllOptions(configOptions)

        // Add capture options defined in CaptureConfig
        if (configOptions.containsOption(CaptureConfig.OPTION_ROTATION)) {
            optionBuilder.setCaptureRequestOption(
                CaptureRequest.JPEG_ORIENTATION,
                configOptions.retrieveOption(CaptureConfig.OPTION_ROTATION)!!
            )
        }
        if (configOptions.containsOption(CaptureConfig.OPTION_JPEG_QUALITY)) {
            optionBuilder.setCaptureRequestOption(
                CaptureRequest.JPEG_QUALITY,
                configOptions.retrieveOption(CaptureConfig.OPTION_JPEG_QUALITY)!!.toByte()
            )
        }

        var inputRequest: InputRequest? = null
        var requestTemplateToSubmit = RequestTemplate(captureConfig.templateType)
        if (
            captureConfig.templateType == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG &&
                !zslControl.isZslDisabledByUserCaseConfig() &&
                !zslControl.isZslDisabledByFlashMode()
        ) {
            zslControl.dequeueImageFromBuffer()?.let { imageProxy ->
                CameraCaptureResults.retrieveCameraCaptureResult(imageProxy.imageInfo)?.let {
                    cameraCaptureResult ->
                    check(cameraCaptureResult is CaptureResultAdapter) {
                        "Unexpected capture result type: ${cameraCaptureResult.javaClass}"
                    }
                    val imageWrapper = AndroidImage(checkNotNull(imageProxy.image))
                    val frameInfo = checkNotNull(cameraCaptureResult.unwrapAs(FrameInfo::class))
                    inputRequest = InputRequest(imageWrapper, frameInfo)
                }
            }
        }

        // Apply still capture template type for regular still capture case
        if (inputRequest == null) {
            requestTemplateToSubmit =
                captureConfig.getStillCaptureTemplate(requestTemplate, isLegacyDevice)
        }

        val parameters =
            templateParamsOverride.getOverrideParams(requestTemplateToSubmit) +
                optionBuilder.build().toParameters()

        return Request(
            streams = streamIdList,
            listeners = listOf(callbacks) + additionalListeners,
            parameters = parameters,
            extras = mapOf(CAMERAX_TAG_BUNDLE to captureConfig.tagBundle),
            template = requestTemplateToSubmit,
            inputRequest = inputRequest,
        )
    }

    companion object {
        internal fun CaptureConfig.getStillCaptureTemplate(
            sessionTemplate: RequestTemplate,
            isLegacyDevice: Boolean,
        ): RequestTemplate {
            var templateToModify = CaptureConfig.TEMPLATE_TYPE_NONE
            if (
                sessionTemplate == RequestTemplate(CameraDevice.TEMPLATE_RECORD) && !isLegacyDevice
            ) {
                // Always override template by TEMPLATE_VIDEO_SNAPSHOT when
                // repeating template is TEMPLATE_RECORD. Note:
                // TEMPLATE_VIDEO_SNAPSHOT is not supported on legacy device.
                templateToModify = CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
            } else if (
                templateType == CaptureConfig.TEMPLATE_TYPE_NONE ||
                    templateType == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
            ) {
                templateToModify = CameraDevice.TEMPLATE_STILL_CAPTURE
            }

            return if (templateToModify != CaptureConfig.TEMPLATE_TYPE_NONE) {
                RequestTemplate(templateToModify)
            } else {
                RequestTemplate(templateType)
            }
        }
    }
}
