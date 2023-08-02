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
import androidx.camera.core.impl.CaptureBundle

/**
 * Fake [ProcessingRequest].
 */
internal class FakeProcessingRequest(
    outputFileOptions: ImageCapture.OutputFileOptions?,
    captureBundle: CaptureBundle,
    cropRect: Rect,
    rotationDegrees: Int,
    jpegQuality: Int,
    sensorToBufferTransform: Matrix,
    callback: TakePictureCallback
) : ProcessingRequest(
    captureBundle,
    outputFileOptions,
    cropRect,
    rotationDegrees,
    jpegQuality,
    sensorToBufferTransform,
    callback
) {
    constructor(captureBundle: CaptureBundle, callback: TakePictureCallback) : this(
        null,
        captureBundle,
        Rect(0, 0, 0, 0),
        0,
        100,
        Matrix(), callback
    )
}