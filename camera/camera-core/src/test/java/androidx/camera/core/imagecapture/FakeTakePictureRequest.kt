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

import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import java.util.concurrent.Executor

/**
 * Fake [TakePictureRequest].
 */
class FakeTakePictureRequest() : TakePictureRequest() {

    var imageCapturedCallback: OnImageCapturedCallback? = null
    var onImageSavedCallback: ImageCapture.OnImageSavedCallback? = null
    var fileOptions: ImageCapture.OutputFileOptions? = null
    var exceptionReceived: ImageCaptureException? = null
    var imageReceived: ImageProxy? = null
    var fileReceived: ImageCapture.OutputFileResults? = null

    constructor(type: Type) : this() {
        when (type) {
            Type.IN_MEMORY -> {
                imageCapturedCallback = object : OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        imageReceived = image
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exceptionReceived = exception
                    }
                }
            }
            Type.ON_DISK -> {
                onImageSavedCallback = object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        fileReceived = outputFileResults
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exceptionReceived = exception
                    }
                }
            }
        }
    }

    override fun getAppExecutor(): Executor {
        return mainThreadExecutor()
    }

    override fun getInMemoryCallback(): OnImageCapturedCallback? {
        return imageCapturedCallback
    }

    override fun getOnDiskCallback(): ImageCapture.OnImageSavedCallback? {
        return onImageSavedCallback
    }

    override fun getOutputFileOptions(): ImageCapture.OutputFileOptions? {
        return fileOptions
    }

    override fun getCropRect(): Rect {
        return Rect(0, 0, 640, 480)
    }

    override fun sensorToBufferTransform(): Matrix {
        return Matrix()
    }

    override fun getRotationDegrees(): Int {
        return 0
    }

    override fun getJpegQuality(): Int {
        return 100
    }

    override fun getSessionConfigCameraCaptureCallbacks(): MutableList<CameraCaptureCallback> {
        return arrayListOf()
    }

    enum class Type {
        IN_MEMORY, ON_DISK
    }
}