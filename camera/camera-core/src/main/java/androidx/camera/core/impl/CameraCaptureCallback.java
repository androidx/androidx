/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;

/**
 * A callback object for tracking the progress of a capture request submitted to the camera device.
 * Once one of the methods is called, other methods won't be called again on the same instance.
 *
 */
public abstract class CameraCaptureCallback {

    /**
     * This method is called when a capture request starts to be processed.
     *
     * @param captureConfigId  the ID of {@link CaptureConfig} that triggers the callback.
     */
    public void onCaptureStarted(int captureConfigId) {
    }

    /**
     * This method is called when an image capture has fully completed and all the result metadata
     * is available.
     *
     * @param captureConfigId  the ID of {@link CaptureConfig} that triggers the callback.
     * @param cameraCaptureResult The output metadata from the capture.
     */
    public void onCaptureCompleted(int captureConfigId,
            @NonNull CameraCaptureResult cameraCaptureResult) {
    }

    /**
     * This method is called instead of {@link #onCaptureCompleted} when the camera device failed to
     * produce a {@link CameraCaptureResult} for the request.
     *
     * @param captureConfigId  the ID of {@link CaptureConfig} that triggers the callback.
     * @param failure The output failure from the capture, including the failure reason.
     */
    public void onCaptureFailed(int captureConfigId, @NonNull CameraCaptureFailure failure) {
    }


    /**
     * This method is called when the capture request was not submitted to camera device. For
     * Example, requests are cancelled when it is in an inappropriate state (such as closed). After
     * onCaptureCancelled is called, other methods won't be called.
     *
     * @param captureConfigId  the ID of {@link CaptureConfig} that triggers the callback.
     */
    public void onCaptureCancelled(int captureConfigId) {
    }

    /**
     * This method is called to notify the client of the progress in the processing stage.
     *
     * @param captureConfigId  the ID of {@link CaptureConfig} that triggers the callback.
     */
    public void onCaptureProcessProgressed(int captureConfigId, int progress) {

    }
}
