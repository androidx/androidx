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

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import androidx.annotation.MainThread
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.imagecapture.CaptureNode.MAX_IMAGES
import androidx.camera.core.imagecapture.Utils.createEmptyImageCaptureConfig
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.core.util.Pair
import com.google.common.util.concurrent.ListenableFuture
import org.mockito.Mockito.mock

/** Fake [ImagePipeline] class for testing. */
class FakeImagePipeline(
    config: ImageCaptureConfig,
    cameraSurfaceSize: Size,
    cameraCharacteristics: CameraCharacteristics
) : ImagePipeline(config, cameraSurfaceSize, cameraCharacteristics) {

    private var currentProcessingRequest: ProcessingRequest? = null
    private var receivedProcessingRequest: MutableSet<ProcessingRequest> = mutableSetOf()
    private var responseMap:
        MutableMap<TakePictureRequest, Pair<CameraRequest, ProcessingRequest>> =
        mutableMapOf()
    var captureConfigMap: MutableMap<TakePictureRequest, List<CaptureConfig>> = mutableMapOf()
    var queueCapacity: Int = MAX_IMAGES
    var captureErrorReceived: ImageCaptureException? = null

    companion object {
        var sNextRequestId = 0
    }

    constructor() :
        this(
            createEmptyImageCaptureConfig(),
            Size(640, 480),
            mock(CameraCharacteristics::class.java)
        )

    @MainThread
    internal override fun createRequests(
        request: TakePictureRequest,
        callback: TakePictureCallback,
        captureFuture: ListenableFuture<Void>
    ): Pair<CameraRequest, ProcessingRequest> {
        if (responseMap[request] == null) {
            val captureConfig =
                captureConfigMap[request]
                    ?: listOf(CaptureConfig.Builder().also { it.setId(sNextRequestId++) }.build())
            responseMap[request] =
                Pair(
                    CameraRequest(captureConfig, callback),
                    FakeProcessingRequest({ mutableListOf() }, callback, captureFuture)
                )
        }
        return responseMap[request]!!
    }

    internal override fun submitProcessingRequest(request: ProcessingRequest) {
        receivedProcessingRequest.add(request)
        currentProcessingRequest = request
    }

    internal override fun notifyCaptureError(error: TakePictureManager.CaptureError) {
        captureErrorReceived = error.imageCaptureException
        currentProcessingRequest!!.onCaptureFailure(error.imageCaptureException)
    }

    internal fun getProcessingRequest(takePictureRequest: TakePictureRequest): ProcessingRequest {
        return responseMap[takePictureRequest]!!.second!!
    }

    @MainThread
    override fun getCapacity(): Int {
        return queueCapacity
    }
}
