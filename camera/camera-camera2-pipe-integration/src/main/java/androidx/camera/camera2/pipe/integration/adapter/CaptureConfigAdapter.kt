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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.toParameters
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import java.util.concurrent.Executor

/**
 * Maps a [CaptureConfig] issued by CameraX (e.g. by the image capture use case) to a [Request]
 * that CameraPipe can submit to the camera.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CaptureConfigAdapter(
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId>,
    private val callbackExecutor: Executor,
) {

    fun mapToRequest(
        captureConfig: CaptureConfig,
        sessionConfigOptions: Config,
    ): Request {
        val surfaces = captureConfig.surfaces
        check(surfaces.isNotEmpty()) {
            "Attempted to issue a capture without surfaces using $captureConfig"
        }

        val streamIdList = surfaces.map {
            checkNotNull(surfaceToStreamMap[it]) {
                "Attempted to issue a capture with an unrecognized surface."
            }
        }

        val callbacks = CameraCallbackMap().apply {
            captureConfig.cameraCaptureCallbacks.forEach { callback ->
                addCaptureCallback(callback, callbackExecutor)
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

        return Request(
            streams = streamIdList,
            listeners = listOf(callbacks),
            parameters = optionBuilder.build().toParameters(),
            extras = mapOf(CAMERAX_TAG_BUNDLE to captureConfig.tagBundle),
            template = RequestTemplate(captureConfig.templateType)
        )
    }
}