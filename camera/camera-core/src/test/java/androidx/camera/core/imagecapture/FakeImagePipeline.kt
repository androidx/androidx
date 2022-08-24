/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.imagecapture

import android.util.Size
import androidx.annotation.MainThread
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.OptionsBundle
import androidx.core.util.Pair

/**
 * Fake [ImagePipeline] class for testing.
 */
class FakeImagePipeline(config: ImageCaptureConfig, cameraSurfaceSize: Size) :
    ImagePipeline(config, cameraSurfaceSize) {

    var receivedProcessingRequest: MutableSet<ProcessingRequest> = mutableSetOf()
    var responseMap: MutableMap<TakePictureRequest, Pair<CameraRequest, ProcessingRequest>> =
        mutableMapOf()
    var captureConfigMap: MutableMap<TakePictureRequest, List<CaptureConfig>> = mutableMapOf()

    constructor() : this(ImageCaptureConfig(OptionsBundle.emptyBundle()), Size(640, 480))

    @MainThread
    override fun createRequests(
        request: TakePictureRequest,
        callback: TakePictureCallback
    ): Pair<CameraRequest, ProcessingRequest> {
        if (responseMap[request] == null) {
            val captureConfig = captureConfigMap[request] ?: listOf()
            responseMap[request] =
                Pair(CameraRequest(captureConfig, callback), ProcessingRequest(callback))
        }
        return responseMap[request]!!
    }

    override fun postProcess(
        request: ProcessingRequest
    ) {
        receivedProcessingRequest.add(request)
    }

    fun getProcessingRequest(takePictureRequest: TakePictureRequest): ProcessingRequest {
        return responseMap[takePictureRequest]!!.second!!
    }
}