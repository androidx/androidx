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
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.JPEG_QUALITY
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import java.util.concurrent.Executor

/** Fake [TakePictureRequest]. */
class FakeTakePictureRequest() : TakePictureRequest() {

    private var imageCapturedCallback: OnImageCapturedCallback? = null
    private var imageSavedCallback: OnImageSavedCallback? = null
    private var fileOptions: ImageCapture.OutputFileOptions? = null
    private var secondaryFileOptions: ImageCapture.OutputFileOptions? = null
    var exceptionReceived: ImageCaptureException? = null
    var imageReceived: ImageProxy? = null
    var fileReceived: ImageCapture.OutputFileResults? = null
    var captureStarted = false
    var captureProcessProgress = -1

    constructor(type: Type) : this() {
        when (type) {
            Type.IN_MEMORY -> {
                imageCapturedCallback =
                    object : OnImageCapturedCallback() {
                        override fun onCaptureStarted() {
                            captureStarted = true
                        }

                        override fun onCaptureSuccess(image: ImageProxy) {
                            imageReceived = image
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exceptionReceived = exception
                        }

                        override fun onCaptureProcessProgressed(progress: Int) {
                            captureProcessProgress = progress
                        }
                    }
            }
            Type.ON_DISK -> {
                imageSavedCallback =
                    object : OnImageSavedCallback {
                        override fun onCaptureStarted() {
                            captureStarted = true
                        }

                        override fun onImageSaved(
                            outputFileResults: ImageCapture.OutputFileResults
                        ) {
                            fileReceived = outputFileResults
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exceptionReceived = exception
                        }

                        override fun onCaptureProcessProgressed(progress: Int) {
                            captureProcessProgress = progress
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

    fun setInMemoryCallback(inMemoryCallback: OnImageCapturedCallback) {
        imageCapturedCallback = inMemoryCallback
    }

    override fun getOnDiskCallback(): OnImageSavedCallback? {
        return imageSavedCallback
    }

    fun setOnDiskCallback(onDiskCallback: OnImageSavedCallback) {
        imageSavedCallback = onDiskCallback
    }

    override fun getOutputFileOptions(): List<ImageCapture.OutputFileOptions> {
        return listOfNotNull(fileOptions, secondaryFileOptions)
    }

    override fun getCropRect(): Rect {
        return Rect(0, 0, 640, 480)
    }

    internal override fun getSensorToBufferTransform(): Matrix {
        return Matrix()
    }

    override fun getRotationDegrees(): Int {
        return ROTATION_DEGREES
    }

    override fun getJpegQuality(): Int {
        return JPEG_QUALITY
    }

    internal override fun getCaptureMode(): Int {
        return ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    }

    override fun getSessionConfigCameraCaptureCallbacks(): MutableList<CameraCaptureCallback> {
        return arrayListOf()
    }

    enum class Type {
        IN_MEMORY,
        ON_DISK
    }
}
