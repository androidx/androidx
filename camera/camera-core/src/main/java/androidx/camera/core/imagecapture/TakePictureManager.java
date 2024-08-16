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
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;

import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * Manages {@link ImageCapture#takePicture} calls.
 *
 * <p>In coming requests are added to a queue and later sent to camera one at a time. Only one
 * in-flight request is allowed at a time. The next request cannot be sent until the current one
 * is completed by camera. However, it allows multiple concurrent requests for post-processing,
 * as {@link ImagePipeline} supports parallel processing.
 *
 * <p>This class selectively propagates callbacks from camera and {@link ImagePipeline} to the
 * app. e.g. it may choose to retry the request before sending the {@link ImageCaptureException}
 * to the app.
 *
 * <p>The thread safety is guaranteed by using the main thread.
 */
public interface TakePictureManager {
    /**
     * Sets the {@link ImagePipeline} for building capture requests and post-processing camera
     * output.
     */
    @MainThread
    void setImagePipeline(@NonNull ImagePipeline imagePipeline);

    /**
     * Adds requests to the queue.
     *
     * <p>The requests in the queue will be executed based on the order being added.
     */
    @MainThread
    void offerRequest(@NonNull TakePictureRequest takePictureRequest);

    /**
     * Pauses sending request to camera.
     */
    @MainThread
    void pause();

    /**
     * Resumes sending request to camera.
     */
    @MainThread
    void resume();

    /**
     * Clears the requests queue.
     */
    @MainThread
    void abortRequests();

    /**
     * Returns whether any capture request is being processed currently.
     */
    @VisibleForTesting
    boolean hasCapturingRequest();

    /**
     * Returns the capture request being processed currently.
     */
    @VisibleForTesting
    @Nullable
    RequestWithCallback getCapturingRequest();

    /**
     * Returns the requests that have not received a result or an error yet.
     */
    @NonNull
    @VisibleForTesting
    List<RequestWithCallback> getIncompleteRequests();

    /**
     * Returns the {@link ImagePipeline} instance used under the hood.
     */
    @VisibleForTesting
    @NonNull
    ImagePipeline getImagePipeline();

    @AutoValue
    abstract static class CaptureError {
        abstract int getRequestId();

        @NonNull
        abstract ImageCaptureException getImageCaptureException();

        static CaptureError of(int requestId,
                @NonNull ImageCaptureException imageCaptureException) {
            return new AutoValue_TakePictureManager_CaptureError(requestId, imageCaptureException);
        }
    }

    /**
     * Interface for deferring creation of a {@link TakePictureManager}.
     */
    interface Provider {
        /**
         * Creates a new, initialized instance of a {@link TakePictureManager}.
         *
         * @param imageCaptureControl       Used by TakePictureManager to control an
         *                                  {@link ImageCapture} instance.
         * @return                          The {@code TakePictureManager} instance.
         */
        @NonNull
        TakePictureManager newInstance(@NonNull ImageCaptureControl imageCaptureControl);
    }
}
