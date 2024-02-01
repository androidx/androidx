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

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy

/**
 * Fake [TakePictureCallback] that tracks method invocations.
 */
internal class FakeTakePictureCallback : TakePictureCallback {

    var onCaptureStarted = false
    var onImageCapturedCalled = false
    var inMemoryResult: ImageProxy? = null
    var captureFailure: ImageCaptureException? = null
    var processFailure: ImageCaptureException? = null
    var onDiskResult: OutputFileResults? = null
    var captureProcessProgress = -1
    var onPostviewBitmapAvailable: Bitmap? = null

    var aborted = false

    override fun onCaptureStarted() {
        onCaptureStarted = true
    }

    override fun onImageCaptured() {
        onImageCapturedCalled = true
    }

    override fun onFinalResult(outputFileResults: OutputFileResults) {
        onDiskResult = outputFileResults
    }

    override fun onFinalResult(imageProxy: ImageProxy) {
        inMemoryResult = imageProxy
    }

    override fun onCaptureFailure(imageCaptureException: ImageCaptureException) {
        captureFailure = imageCaptureException
    }

    override fun onProcessFailure(imageCaptureException: ImageCaptureException) {
        processFailure = imageCaptureException
    }

    override fun isAborted(): Boolean {
        return aborted
    }

    override fun onCaptureProcessProgressed(progress: Int) {
        captureProcessProgress = progress
    }

    override fun onPostviewBitmapAvailable(bitmap: Bitmap) {
        onPostviewBitmapAvailable = bitmap
    }
}
