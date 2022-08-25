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

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy

/**
 * Fake [TakePictureCallback] that tracks method invocations.
 */
internal class FakeTakePictureCallback : TakePictureCallback {

    var onImageCapturedCalled = false

    override fun onImageCaptured() {
        onImageCapturedCalled = true
    }

    override fun onFinalResult(outputFileResults: ImageCapture.OutputFileResults) {
    }

    override fun onFinalResult(imageProxy: ImageProxy) {
    }

    override fun onCaptureFailure(imageCaptureException: ImageCaptureException) {
    }

    override fun onProcessFailure(imageCaptureException: ImageCaptureException) {
    }
}