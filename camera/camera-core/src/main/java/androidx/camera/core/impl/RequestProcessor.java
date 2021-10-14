/*
 * Copyright 2021 The Android Open Source Project
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

import java.util.List;

/**
 * A wrapper provided to {@link SessionProcessor} to execute camera requests.
 *
 * <p>When the {@link SessionProcessor#startRepeating(SessionProcessor.CaptureCallback)} or
 * {@link SessionProcessor#startCapture(SessionProcessor.CaptureCallback)} is called, it can
 * invoke the APIs in this class to submit camera requests to achieve the required functionality.
 *
 * <p>The images to be fetched is managed inside {@link SessionProcessor}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface RequestProcessor {
    /**
     * Submit a request.
     *
     * @return the id of the capture sequence or -1 in case the processor encounters a fatal error
     *         or receives an invalid argument.
     */
    int submit(@NonNull Request request,
            @NonNull Callback callback);

    /**
     * Submit a list of requests.
     *
     * @return the id of the capture sequence or -1 in case the processor encounters a fatal error
     *         or receives an invalid argument.
     */
    int submit(@NonNull List<Request> requests,
            @NonNull Callback callback);

    /**
     * Set a repeating request.
     *
     * @return the id of the capture sequence or -1 in case the processor encounters a fatal error
     *         or receives an invalid argument.
     */
    int setRepeating(@NonNull Request request,
            @NonNull Callback callback);

    /**
     * Abort captures
     */
    void abortCaptures();

    /**
     * Stop repeating requests.
     */
    void stopRepeating();

    /**
     * A interface representing a capture request configuration used for submitting requests in
     * {@link RequestProcessor}.
     */
    interface Request {
        /**
         * Gets the target ids of the outputConfig which identifies corresponding
         * Surface to be the targeted for the request.
         */
        @NonNull
        List<Integer> getTargetOutputConfigIds();

        /**
         * Gets all the parameters.
         */
        @NonNull
        Config getParameters();

        /**
         * Gets the template id.
         */
        int getTemplateId();
    }

    /**
     * Callback to be invoked during the capture.
     */
    interface Callback {
        void onCaptureStarted(
                @NonNull Request request,
                long frameNumber,
                long timestamp);

        void onCaptureProgressed(
                @NonNull Request request,
                @NonNull CameraCaptureResult captureResult);

        void onCaptureCompleted(
                @NonNull Request request,
                @NonNull CameraCaptureResult captureResult);

        void onCaptureFailed(
                @NonNull Request request,
                @NonNull CameraCaptureFailure captureFailure);

        void onCaptureBufferLost(
                @NonNull Request request,
                long frameNumber,
                int outputConfigId);

        void onCaptureSequenceCompleted(int sequenceId, long frameNumber);

        void onCaptureSequenceAborted(int sequenceId);
    }
}
