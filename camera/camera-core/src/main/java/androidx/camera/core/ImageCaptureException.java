/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * An exception thrown to indicate an error has occurred during image capture or while saving the
 * captured image. See {@link ImageCapture.OnImageCapturedCallback} and
 * {@link ImageCapture.OnImageSavedCallback}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ImageCaptureException extends Exception {

    @ImageCapture.ImageCaptureError
    private final int mImageCaptureError;

    public ImageCaptureException(@ImageCapture.ImageCaptureError final int imageCaptureError,
            @NonNull final String message, @Nullable final Throwable cause) {
        super(message, cause);
        mImageCaptureError = imageCaptureError;
    }

    /**
     * Returns the type of the image capture error.
     *
     * @return The image capture error type, can have one of the following values:
     * {@link ImageCapture#ERROR_UNKNOWN}, {@link ImageCapture#ERROR_FILE_IO},
     * {@link ImageCapture#ERROR_CAPTURE_FAILED}, {@link ImageCapture#ERROR_CAMERA_CLOSED},
     * {@link ImageCapture#ERROR_INVALID_CAMERA}.
     */
    @ImageCapture.ImageCaptureError
    public int getImageCaptureError() {
        return mImageCaptureError;
    }
}
