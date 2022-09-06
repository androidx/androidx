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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

/**
 * A callback for a {@link TakePictureRequest}.
 *
 * <p>This is an internal callback that loosely maps to
 * {@link ImageCapture.OnImageCapturedCallback} or {@link ImageCapture.OnImageSavedCallback}.
 */
public interface TakePictureCallback {

    /**
     * Invoked when the capture is complete..
     *
     * <p>Once invoked, {@link TakePictureManager} can submit the next request to camera.
     *
     * <p>After invoked, the {@link TakePictureCallback} is expected to be invoked again to
     * deliver a final result or an error.
     */
    @MainThread
    void onImageCaptured();

    /**
     * Invoked when the final on-disk result is saved.
     *
     * <p>After invoked, the {@link TakePictureCallback} will never be invoked again.
     */
    @MainThread
    void onFinalResult(@NonNull ImageCapture.OutputFileResults outputFileResults);

    /**
     * Invoked when the final in-memory result is ready.
     *
     * <p>After invoked, the {@link TakePictureCallback} will never be invoked again.
     */
    @MainThread
    void onFinalResult(@NonNull ImageProxy imageProxy);

    /**
     * Invoked when camera fails to return the image.
     *
     * <p>After invoked, the {@link TakePictureCallback} will never be invoked again.
     */
    @MainThread
    void onCaptureFailure(@NonNull ImageCaptureException imageCaptureException);

    /**
     * Invoked when {@link ImagePipeline} fails to post-process the image.
     *
     * <p>After invoked, the {@link TakePictureCallback} will never be invoked again.
     */
    @MainThread
    void onProcessFailure(@NonNull ImageCaptureException imageCaptureException);
}
