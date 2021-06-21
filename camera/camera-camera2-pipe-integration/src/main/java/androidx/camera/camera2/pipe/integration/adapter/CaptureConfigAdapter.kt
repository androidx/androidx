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

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import java.util.concurrent.Executor

/**
 * Maps a [CaptureConfig] issued by CameraX (e.g. by the image capture use case) to a [Request]
 * that CameraPipe can submit to the camera.
 */
class CaptureConfigAdapter(
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    private val callbackExecutor: Executor,
) {

    fun mapToRequest(captureConfig: CaptureConfig): Request {
        val surfaces = captureConfig.surfaces
        check(surfaces.isNotEmpty()) {
            "Attempted to issue a capture without surfaces using $captureConfig"
        }

        // TODO: It's assumed a single surface is used per use case, even though capture requests
        //  can support multiple surfaces. Look into potentially bridging the gap between the two
        //  in this layer.
        val streamId = surfaceToStreamMap[surfaces.single()]
        checkNotNull(streamId) { "Attempted to issue a capture with an unrecognized surface." }

        val callbacks = CameraCallbackMap().apply {
            captureConfig.cameraCaptureCallbacks.forEach { callback ->
                addCaptureCallback(callback, callbackExecutor)
            }
        }

        val parameters = mutableMapOf<CaptureRequest.Key<*>, Any>()
        val configOptions = captureConfig.implementationOptions

        // Add potential capture options set through Camera2 interop
        // TODO: When adding support for Camera2 interop, ensure interop options are correctly
        //  being added to the capture request
        for (configOption in configOptions.listOptions()) {
            val requestKey = configOption.token as? CaptureRequest.Key<*> ?: continue
            val value = configOptions.retrieveOption(configOption) ?: continue
            parameters[requestKey] = value
        }

        // Add capture options defined in CaptureConfig
        if (configOptions.containsOption(CaptureConfig.OPTION_ROTATION)) {
            parameters[CaptureRequest.JPEG_ORIENTATION] =
                configOptions.retrieveOption(CaptureConfig.OPTION_ROTATION)!!
        }
        if (configOptions.containsOption(CaptureConfig.OPTION_JPEG_QUALITY)) {
            parameters[CaptureRequest.JPEG_QUALITY] =
                configOptions.retrieveOption(CaptureConfig.OPTION_JPEG_QUALITY)!!.toByte()
        }

        // TODO: When adding support for extensions, also add support for passing capture request
        //  tags with each request, since extensions may rely on these tags.
        return Request(
            streams = listOf(streamId),
            listeners = listOf(callbacks),
            parameters = parameters,
            template = RequestTemplate(captureConfig.templateType)
        )
    }
}