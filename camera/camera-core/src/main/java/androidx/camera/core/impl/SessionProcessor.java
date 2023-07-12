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

import android.hardware.camera2.CaptureResult;
import android.media.ImageReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A processor for (1) transforming the surfaces used in Preview/ImageCapture/ImageAnalysis
 * into final session configuration where intermediate {@link ImageReader}s could be created for
 * processing the image and writing the result to output surfaces (2) performing repeating request
 * and still image capture by using {@link RequestProcessor}.
 *
 * <p>A {@link RequestProcessor} will be passed to the {@link SessionProcessor} when
 * {@link SessionProcessor#onCaptureSessionStart(RequestProcessor)} is called to execute camera
 * requests. When being requested to execute repeating request or still capture, the
 * SessionProcessor can set any target surfaces and parameters it needs. It can also send
 * multiple requests if necessary.
 *
 * <p>The SessionProcessor is expected to release all intermediate {@link ImageReader}s when
 * {@link #deInitSession()} is called.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface SessionProcessor {
    /**
     * Initializes the session and returns a transformed {@link SessionConfig} which should be
     * used to configure the camera instead of original one.
     *
     * <p>Output surfaces of preview, image capture and imageAnalysis should be passed in. The
     * SessionProcessor is responsible to write the output to this given output surfaces.
     *
     * @param cameraInfo                 cameraInfo for querying the camera info
     * @param previewSurfaceConfig       output surface for preview. This is mandatory.
     * @param imageCaptureSurfaceConfig  output surface for image capture. This is mandatory.
     * @param imageAnalysisSurfaceConfig output surface for image analysis. This is optional.
     *                                   Passing null if image analysis output is not needed.
     * @return a {@link SessionConfig} that contains the surfaces and the session parameters and
     * should be used to configure the camera session.
     */
    @NonNull
    SessionConfig initSession(
            @NonNull CameraInfo cameraInfo,
            @NonNull OutputSurface previewSurfaceConfig,
            @NonNull OutputSurface imageCaptureSurfaceConfig,
            @Nullable OutputSurface imageAnalysisSurfaceConfig);

    /**
     * De-initializes the session. This is called after the camera session is closed.
     */
    void deInitSession();

    /**
     * Sets the camera parameters to be enabled in every single and repeating request.
     */
    void setParameters(@NonNull Config config);

    /**
     * Notifies the SessionProcessor that the camera session is just started. A
     * {@link RequestProcessor} is provided to execute camera requests.
     */
    void onCaptureSessionStart(
            @NonNull RequestProcessor requestProcessor);

    /**
     * Notifies the SessionProcessor that the camera session is going to be closed.
     * {@link RequestProcessor} will no longer accept any requests
     * after onCaptureSessionEnd() returns.
     */
    void onCaptureSessionEnd();

    /**
     * Requests the SessionProcessor to start the repeating request that enables
     * preview and image analysis.
     *
     * @param callback callback to notify the status.
     * @return the id of the capture sequence.
     */
    int startRepeating(@NonNull CaptureCallback callback);


    /**
     * Stop the repeating request.
     */
    void stopRepeating();

    /**
     * Requests the SessionProcessor to start the still image capture. The capture task can only
     * perform one at a time.
     *
     * @param callback callback to notify the status.
     * @return the id of the capture sequence.
     */
    int startCapture(
            @NonNull CaptureCallback callback);

    /**
     * Aborts the pending capture.
     */
    void abortCapture(int captureSequenceId);

    /**
     * Sends trigger-type single request such as AF/AE triggers.
     */
    default int startTrigger(@NonNull Config config, @NonNull CaptureCallback callback) {
        return -1;
    }

    /**
     * Returns the supported camera operations when the SessionProcessor is enabled.
     */
    @NonNull
    default @RestrictedCameraControl.CameraOperation Set<Integer> getSupportedCameraOperations() {
        return Collections.emptySet();
    }

    /**
     * Callback for {@link #startRepeating} and {@link #startCapture}.
     */
    interface CaptureCallback {
        /**
         * This method is called when the camera has started capturing the initial input
         * image.
         *
         * For a multi-frame capture, the method is called when the onCaptureStarted of first
         * frame is called and its timestamp is directly forwarded to timestamp parameter of
         * this method.
         *
         * @param captureSequenceId id of the current capture sequence
         * @param timestamp         the timestamp at start of capture for repeating
         *                          request or the timestamp at start of capture of the
         *                          first frame in a multi-frame capture, in nanoseconds.
         */
        default void onCaptureStarted(int captureSequenceId, long timestamp) {}

        /**
         * This method is called when an image (or images in case of multi-frame
         * capture) is captured and device-specific extension processing is triggered.
         *
         * @param captureSequenceId id of the current capture sequence
         */
        default void onCaptureProcessStarted(int captureSequenceId) {}

        /**
         * This method is called instead of {@link #onCaptureProcessStarted} when the camera
         * device failed to produce the required input image. The cause could be a failed camera
         * capture request, a failed capture result or dropped camera frame.
         *
         * @param captureSequenceId id of the current capture sequence
         */
        default void onCaptureFailed(int captureSequenceId) {}

        /**
         * This method is called independently of the others in the CaptureCallback, when a capture
         * sequence finishes.
         *
         * <p>In total, there will be at least one {@link #onCaptureProcessStarted}/
         * {@link #onCaptureFailed} invocation before this callback is triggered. If the capture
         * sequence is aborted before any requests have begun processing,
         * {@link #onCaptureSequenceAborted} is invoked instead.</p>
         *
         * @param captureSequenceId id of the current capture sequence
         */
        default void onCaptureSequenceCompleted(int captureSequenceId) {}

        /**
         * This method is called when a capture sequence aborts.
         *
         * @param captureSequenceId id of the current capture sequence
         */
        default void onCaptureSequenceAborted(int captureSequenceId) {}

        /**
         * Capture result callback that needs to be called when the process capture results are
         * ready as part of frame post-processing.
         *
         * This callback will fire after {@link #onCaptureStarted}, {@link #onCaptureProcessStarted}
         * and before {@link #onCaptureSequenceCompleted}. The callback is not expected to fire
         * in case of capture failure  {@link #onCaptureFailed} or capture abort
         * {@link #onCaptureSequenceAborted}.
         *
         * @param timestamp            The timestamp at start of capture. The same timestamp value
         *                             passed to {@link #onCaptureStarted}.
         * @param captureSequenceId    the capture id of the request that generated the capture
         *                             results. This is the return value of either
         *                             {@link #startRepeating} or {@link #startCapture}.
         * @param result               Map containing the supported capture results. Do note
         *                             that if results 'android.jpeg.quality' and
         *                             'android.jpeg.orientation' are present in the process
         *                             capture input results, then the values must also be passed
         *                             as part of this callback. Both Camera2 and CameraX guarantee
         *                             that those two settings and results are always supported and
         *                             applied by the corresponding framework.
         */
        default void onCaptureCompleted(long timestamp, int captureSequenceId,
                @NonNull Map<CaptureResult.Key, Object> result) {}
    }
}
