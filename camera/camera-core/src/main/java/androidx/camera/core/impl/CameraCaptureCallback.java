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
import androidx.annotation.RequiresApi;

/**
 * A callback object for tracking the progress of a capture request submitted to the camera device.
 * Once one of the methods is called, other methods won't be called again on the same instance.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class CameraCaptureCallback {

    /**
     * This method is called when an image capture has fully completed and all the result metadata
     * is available.
     *
     * @param cameraCaptureResult The output metadata from the capture.
     */
    public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
    }

    /**
     * This method is called instead of {@link #onCaptureCompleted} when the camera device failed to
     * produce a {@link CameraCaptureResult} for the request.
     *
     * @param failure The output failure from the capture, including the failure reason.
     */
    public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
    }


    /**
     * This method is called when the capture request was not submitted to camera device. For
     * Example, requests are cancelled when it is in an inappropriate state (such as closed). After
     * onCaptureCancelled is called, other methods won't be called.
     */
    public void onCaptureCancelled() {
    }
}
