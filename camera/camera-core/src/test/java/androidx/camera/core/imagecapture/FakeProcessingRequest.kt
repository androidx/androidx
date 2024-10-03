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
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.createTakePictureRequest
import androidx.camera.core.impl.CaptureBundle
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

/** Fake [ProcessingRequest]. */
internal class FakeProcessingRequest(
    outputFileOptions: ImageCapture.OutputFileOptions?,
    secondaryOutputFileOptions: ImageCapture.OutputFileOptions?,
    captureBundle: CaptureBundle,
    cropRect: Rect,
    rotationDegrees: Int,
    jpegQuality: Int,
    sensorToBufferTransform: Matrix,
    callback: TakePictureCallback,
    captureFuture: ListenableFuture<Void>
) :
    ProcessingRequest(
        captureBundle,
        createTakePictureRequest(
            outputFileOptions,
            secondaryOutputFileOptions,
            cropRect,
            sensorToBufferTransform,
            rotationDegrees,
            jpegQuality
        ),
        callback,
        captureFuture
    ) {
    constructor(
        captureBundle: CaptureBundle,
        callback: TakePictureCallback,
        captureFuture: ListenableFuture<Void> = CallbackToFutureAdapter.getFuture { "test" }
    ) : this(null, null, captureBundle, CROP_RECT, 0, 100, Matrix(), callback, captureFuture)
}
