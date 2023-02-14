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

package androidx.camera.extensions.impl.advanced;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.view.Surface;

import java.util.Map;

/**
 * Interface for creating Camera2 CameraCaptureSessions with extension enabled based on
 * advanced vendor implementation.
 *
 * <p><pre>
 * The flow of a extension session is shown below:
 * (1) {@link #initSession}: CameraX prepares streams configuration for creating
 *     CameraCaptureSession. Output surfaces for Preview, ImageCapture and ImageAnalysis are passed
 *     in and vendor is responsible for outputting the results to these surfaces.
 *
 * (2) {@link #onCaptureSessionStart}: It is called after CameraCaptureSession is configured.
 *     A {@link RequestProcessorImpl} is passed for vendor to send repeating requests and
 *     single requests.
 *
 * (3) {@link #startRepeating}:  CameraX will call this method to start the repeating request
 *     after CameraCaptureSession is called. Vendor should start the repeating request by
 *     {@link RequestProcessorImpl}. Vendor can also update the repeating request if needed later.
 *
 * (4) {@link #setParameters(Map)}: The passed parameters will be attached to the repeating request
 *     and single requests but vendor can choose to apply some of them only.
 *
 * (5) {@link #startCapture(CaptureCallback)}: It is called when apps want to
 *     start a multi-frame image capture.  {@link CaptureCallback} will be called
 *     to report the status and the output image will be written to the capture output surface
 *     specified in {@link #initSession}.
 *
 * (5) {@link #onCaptureSessionEnd}: It is called right BEFORE CameraCaptureSession.close() is
 *     called.
 *
 * (6) {@link #deInitSession}: called when CameraCaptureSession is closed.
 * </pre>
 */
@SuppressLint("UnknownNullness")
public interface SessionProcessorImpl {
    /**
     * Initializes the session for the extension. This is where the OEMs allocate resources for
     * preparing a CameraCaptureSession. After initSession() is called, the camera ID,
     * cameraCharacteristics and context will not change until deInitSession() has been called.
     *
     * <p>CameraX specifies the output surface configurations for preview, image capture and image
     * analysis[optional]. And OEM returns a {@link Camera2SessionConfigImpl} which consists of a
     * list of {@link Camera2OutputConfigImpl} and session parameters. The
     * {@link Camera2SessionConfigImpl} will be used to configure the CameraCaptureSession.
     *
     * <p>OEM is responsible for outputting correct camera images output to these output surfaces.
     * OEM can have the following options to enable the output:
     * <pre>
     * (1) Add these output surfaces in CameraCaptureSession directly using
     * {@link Camera2OutputConfigImplBuilder#newSurfaceConfig(Surface)} }. Processing is done in
     * HAL.
     *
     * (2) Use surface sharing with other surface by calling
     * {@link Camera2OutputConfigImplBuilder#addSurfaceSharingOutputConfig(Camera2OutputConfigImpl)}
     * to add the output surface to the other {@link Camera2OutputConfigImpl}.
     *
     * (3) Process output from other surfaces (RAW, YUV..) and write the result to the output
     * surface. The output surface won't be contained in the returned
     * {@link Camera2SessionConfigImpl}.
     * </pre>
     *
     * <p>{@link Camera2OutputConfigImplBuilder} and {@link Camera2SessionConfigImplBuilder}
     * implementations are provided in the stub for OEM to construct the
     * {@link Camera2OutputConfigImpl} and {@link Camera2SessionConfigImpl} instances.
     *
     * @param previewSurfaceConfig       output surface for preview
     * @param imageCaptureSurfaceConfig  output surface for image capture.
     * @param imageAnalysisSurfaceConfig an optional output config for image analysis
     *                                   (YUV_420_888).
     * @return a {@link Camera2SessionConfigImpl} consisting of a list of
     * {@link Camera2OutputConfigImpl} and session parameters which will decide the
     * {@link android.hardware.camera2.params.SessionConfiguration} for configuring the
     * CameraCaptureSession. Please note that the OutputConfiguration list may not be part of any
     * supported or mandatory stream combination BUT OEM must ensure this list will always
     * produce a valid camera capture session.
     */
    Camera2SessionConfigImpl initSession(
            String cameraId,
            Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            Context context,
            OutputSurfaceImpl previewSurfaceConfig,
            OutputSurfaceImpl imageCaptureSurfaceConfig,
            OutputSurfaceImpl imageAnalysisSurfaceConfig);

    /**
     * Notify to de-initialize the extension. This callback will be invoked after
     * CameraCaptureSession is closed. After onDeInit() was called, it is expected that the
     * camera ID, cameraCharacteristics will no longer hold and tear down any resources allocated
     * for this extension. Aborts all pending captures.
     */
    void deInitSession();

    /**
     * CameraX / Camera2 would call these API’s to pass parameters from the app to the OEM. It’s
     * expected that the OEM would (eventually) update the repeating request if the keys are
     * supported. Setting a value to null explicitly un-sets the value.
     */
    void setParameters(Map<CaptureRequest.Key<?>, Object> parameters);

    /**
     * CameraX / Camera2 will call this interface in response to client requests involving
     * the output preview surface. Typical examples include requests that include AF/AE triggers.
     * Extensions can disregard any capture request keys that were not advertised in
     * {@link AdvancedExtenderImpl#getAvailableCaptureRequestKeys}.
     *
     * @param triggers Capture request key value map.
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     *
     * @throws IllegalArgumentException If there are no valid settings that can be applied
     *
     * @since 1.3
     */
    int startTrigger(Map<CaptureRequest.Key<?>, Object> triggers, CaptureCallback callback);

    /**
     * This will be invoked once after the {@link android.hardware.camera2.CameraCaptureSession}
     * has been created. {@link RequestProcessorImpl} is passed for OEM to submit single
     * requests or set repeating requests. This ExtensionRequestProcessor will be valid to use
     * until onCaptureSessionEnd is called.
     */
    void onCaptureSessionStart(RequestProcessorImpl requestProcessor);

    /**
     * This will be invoked before the {@link android.hardware.camera2.CameraCaptureSession} is
     * closed. {@link RequestProcessorImpl} passed in onCaptureSessionStart will no longer
     * accept any requests after onCaptureSessionEnd() returns.
     */
    void onCaptureSessionEnd();

    /**
     * Starts the repeating request after CameraCaptureSession is called. Vendor should start the
     * repeating request by {@link RequestProcessorImpl}. Vendor can also update the
     * repeating request when needed later.
     *
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     */
    int startRepeating(CaptureCallback callback);

    /**
     * Stop the repeating request. To prevent OEM from not calling stopRepeating, CameraX will
     * first stop the repeating request of current CameraCaptureSession and call this API to signal
     * OEM that the repeating request was stopped and going forward calling
     * {@link RequestProcessorImpl#setRepeating} will simply do nothing.
     */
    void stopRepeating();

    /**
     * Start a multi-frame capture.
     *
     * When the capture is completed, {@link CaptureCallback#onCaptureSequenceCompleted}
     * is called and {@code OnImageAvailableListener#onImageAvailable}
     * will also be called on the ImageReader that creates the image capture output surface.
     *
     * <p>Only one capture can perform at a time. Starting a capture when another capture is running
     * will cause onCaptureFailed to be called immediately.
     *
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     */
    int startCapture(CaptureCallback callback);

    /**
     * Abort all capture tasks.
     */
    void abortCapture(int captureSequenceId);

    /**
     * Callback for notifying the status of {@link #startCapture(CaptureCallback)} and
     * {@link #startRepeating(CaptureCallback)}.
     */
    interface CaptureCallback {
        /**
         * This method is called when the camera device has started capturing the initial input
         * image.
         *
         * For a multi-frame capture, the method is called when the
         * CameraCaptureSession.CaptureCallback onCaptureStarted of first frame is called and its
         * timestamp is directly forwarded to timestamp parameter of
         * this method.
         *
         * @param captureSequenceId id of the current capture sequence
         * @param timestamp         the timestamp at start of capture for repeating
         *                          request or the timestamp at start of capture of the
         *                          first frame in a multi-frame capture, in nanoseconds.
         */
        void onCaptureStarted(int captureSequenceId, long timestamp);

        /**
         * This method is called when an image (or images in case of multi-frame
         * capture) is captured and device-specific extension processing is triggered.
         *
         * @param captureSequenceId id of the current capture sequence
         */
        void onCaptureProcessStarted(int captureSequenceId);

        /**
         * This method is called instead of
         * {@link #onCaptureProcessStarted} when the camera device failed
         * to produce the required input for the device-specific extension. The
         * cause could be a failed camera capture request, a failed
         * capture result or dropped camera frame.
         *
         * @param captureSequenceId id of the current capture sequence
         */
        void onCaptureFailed(int captureSequenceId);

        /**
         * This method is called independently of the others in the CaptureCallback, when a capture
         * sequence finishes.
         *
         * <p>In total, there will be at least one
         * {@link #onCaptureProcessStarted}/{@link #onCaptureFailed}
         * invocation before this callback is triggered. If the capture
         * sequence is aborted before any requests have begun processing,
         * {@link #onCaptureSequenceAborted} is invoked instead.</p>
         *
         * @param captureSequenceId id of the current capture sequence
         */
        void onCaptureSequenceCompleted(int captureSequenceId);

        /**
         * This method is called when a capture sequence aborts.
         *
         * @param captureSequenceId id of the current capture sequence
         */
        void onCaptureSequenceAborted(int captureSequenceId);

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
        void onCaptureCompleted(long timestamp, int captureSequenceId,
                Map<CaptureResult.Key, Object> result);
    }
}
