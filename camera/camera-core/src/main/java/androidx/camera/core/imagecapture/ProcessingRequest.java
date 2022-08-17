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

package androidx.camera.core.imagecapture;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

/**
 * A post-processing request and its callback.
 */
public class ProcessingRequest {

    private final TakePictureCallback mCallback;

    ProcessingRequest(@NonNull TakePictureCallback callback) {
        mCallback = callback;
    }

    /**
     * @see TakePictureCallback#onImageCaptured()
     */
    @MainThread
    void onImageCaptured() {
        mCallback.onImageCaptured();
    }

    /**
     * @see TakePictureCallback#onFinalResult
     */
    @MainThread
    void onFinalResult(@Nullable ImageCapture.OutputFileResults outputFileResults) {
        mCallback.onFinalResult(outputFileResults);
    }

    /**
     * @see TakePictureCallback#onFinalResult
     */
    @MainThread
    void onFinalResult(@Nullable ImageProxy imageProxy) {
        mCallback.onFinalResult(imageProxy);
    }

    /**
     * @see TakePictureCallback#onProcessFailure
     */
    @MainThread
    void onProcessFailure(@NonNull ImageCaptureException imageCaptureException) {
        mCallback.onProcessFailure(imageCaptureException);
    }

}
