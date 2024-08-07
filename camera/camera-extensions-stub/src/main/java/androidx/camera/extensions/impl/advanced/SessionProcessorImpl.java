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

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 *
 * @since 1.2
 */
public interface SessionProcessorImpl {
    /**
     * Initializes the session for the extension. This is where the OEMs allocate resources for
     * preparing a CameraCaptureSession. After initSession() is called, the camera ID,
     * cameraCharacteristics and context will not change until deInitSession() has been called.
     *
     * <p>CameraX / Camera2 specifies the output surface configurations for preview using
     * {@link OutputSurfaceConfigurationImpl#getPreviewOutputSurface}, image capture using
     * {@link OutputSurfaceConfigurationImpl#getImageCaptureOutputSurface}, and image analysis
     * [optional] using {@link OutputSurfaceConfigurationImpl#getImageAnalysisOutputSurface}.
     * And OEM returns a {@link Camera2SessionConfigImpl} which consists of a list of
     * {@link Camera2OutputConfigImpl} and session parameters. The {@link Camera2SessionConfigImpl}
     * will be used to configure the CameraCaptureSession.
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
     * @param surfaceConfigs contains output surfaces for preview, image capture, and an
     *                       optional output config for image analysis (YUV_420_888).
     * @return a {@link Camera2SessionConfigImpl} consisting of a list of
     * {@link Camera2OutputConfigImpl} and session parameters which will decide the
     * {@link android.hardware.camera2.params.SessionConfiguration} for configuring the
     * CameraCaptureSession. Please note that the OutputConfiguration list may not be part of any
     * supported or mandatory stream combination BUT OEM must ensure this list will always
     * produce a valid camera capture session.
     *
     * @since 1.4
     */
    @NonNull
    Camera2SessionConfigImpl initSession(
            @NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull Context context,
            @NonNull OutputSurfaceConfigurationImpl surfaceConfigs);

    /**
     * Initializes the session for the extension. This is where the OEMs allocate resources for
     * preparing a CameraCaptureSession. After initSession() is called, the camera ID,
     * cameraCharacteristics and context will not change until deInitSession() has been called.
     *
     * <p>CameraX / Camera 2 specifies the output surface configurations for preview, image capture
     * and image analysis[optional]. And OEM returns a {@link Camera2SessionConfigImpl} which
     * consists of a list of {@link Camera2OutputConfigImpl} and session parameters. The
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
     * @param previewSurfaceConfig       output surface for preview, which may contain a
     *                                   <code>null</code> surface if the app doesn't specify the
     *                                   preview surface.
     * @param imageCaptureSurfaceConfig  output surface for still capture, which may contain a
     *                                   <code>null</code> surface if the app doesn't specify the
     *                                   still capture surface.
     * @param imageAnalysisSurfaceConfig an optional output config for image analysis
     *                                   (YUV_420_888).
     * @return a {@link Camera2SessionConfigImpl} consisting of a list of
     * {@link Camera2OutputConfigImpl} and session parameters which will decide the
     * {@link android.hardware.camera2.params.SessionConfiguration} for configuring the
     * CameraCaptureSession. Please note that the OutputConfiguration list may not be part of any
     * supported or mandatory stream combination BUT OEM must ensure this list will always
     * produce a valid camera capture session.
     */
    @NonNull
    Camera2SessionConfigImpl initSession(
            @NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull Context context,
            @NonNull OutputSurfaceImpl previewSurfaceConfig,
            @NonNull OutputSurfaceImpl imageCaptureSurfaceConfig,
            @Nullable OutputSurfaceImpl imageAnalysisSurfaceConfig);

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
     * supported. These parameters should be set by the OEM on all capture requests sent during
     * {@link #startRepeating(CaptureCallback)},
     * {@link #startCapture(CaptureCallback)} and {@link #startTrigger(Map, CaptureCallback)}.
     */
    void setParameters(@NonNull Map<CaptureRequest.Key<?>, Object> parameters);

    /**
     * CameraX / Camera2 will call this interface in response to client requests involving
     * the output preview surface. Typical examples include requests that include AF/AE triggers.
     * Extensions can disregard any capture request keys that were not advertised in
     * {@link AdvancedExtenderImpl#getAvailableCaptureRequestKeys}. In addition to the
     * Key/value map in the {@code trigger} parameter, the capture request must also
     * include the parameters set in {@link #setParameters(Map)}.
     *
     * @param triggers Capture request key value map.
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     *
     * @throws IllegalArgumentException If there are no valid settings that can be applied
     *
     * @since 1.3
     */
    int startTrigger(@NonNull Map<CaptureRequest.Key<?>, Object> triggers,
            @NonNull CaptureCallback callback);

    /**
     * This will be invoked once after the {@link android.hardware.camera2.CameraCaptureSession}
     * has been created. {@link RequestProcessorImpl} is passed for OEM to submit single
     * requests or set repeating requests. This ExtensionRequestProcessor will be valid to use
     * until onCaptureSessionEnd is called.
     */
    void onCaptureSessionStart(@NonNull RequestProcessorImpl requestProcessor);

    /**
     * This will be invoked before the {@link android.hardware.camera2.CameraCaptureSession} is
     * closed. {@link RequestProcessorImpl} passed in onCaptureSessionStart will no longer
     * accept any requests after onCaptureSessionEnd() returns.
     */
    void onCaptureSessionEnd();

    /**
     * Starts the repeating request after CameraCaptureSession is called. Vendor should start the
     * repeating request by {@link RequestProcessorImpl}. Vendor can also update the
     * repeating request when needed later. The repeating request is expected to contain the
     * parameters set in {@link #setParameters(Map)}.
     *
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     */
    int startRepeating(@NonNull CaptureCallback callback);

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
     * will also be called on the ImageReader that creates the image capture output surface. All
     * the capture requests are expected to contain the parameters set in
     * {@link #setParameters(Map)}.
     *
     * <p>Only one capture can perform at a time. Starting a capture when another capture is running
     * will cause onCaptureFailed to be called immediately.
     *
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     */
    int startCapture(@NonNull CaptureCallback callback);

    /**
     * Start a multi-frame capture with a postview. {@link #startCapture(CaptureCallback)}
     * will be used for captures without a postview request.
     *
     * Postview will be available before the capture. Upon postview completion,
     * {@code OnImageAvailableListener#onImageAvailable} will be called on the ImageReader
     * that creates the postview output surface. When the capture is completed,
     * {@link CaptureCallback#onCaptureSequenceCompleted} is called and
     * {@code OnImageAvailableListener#onImageAvailable} will also be called on the ImageReader
     * that creates the image capture output surface.
     *
     * <p>Only one capture can perform at a time. Starting a capture when another capture is
     * running will cause onCaptureFailed to be called immediately.
     *
     * @param callback a callback to report the status.
     * @return the id of the capture sequence.
     * @since 1.4
     */
    int startCaptureWithPostview(@NonNull CaptureCallback callback);

    /**
     * Abort all capture tasks.
     */
    void abortCapture(int captureSequenceId);

    /**
     * Returns the dynamically calculated capture latency pair in milliseconds.
     *
     * <p>In contrast to {@link AdvancedExtenderImpl#getEstimatedCaptureLatencyRange} this method is
     * guaranteed to be called after {@link #onCaptureSessionStart}.
     * The measurement is expected to take in to account dynamic parameters such as the current
     * scene, the state of 3A algorithms, the state of internal HW modules and return a more
     * accurate assessment of the still capture latency.</p>
     *
     * @return pair that includes the estimated input frame/frames camera capture latency as the
     * first field. This is the time between {@link #onCaptureStarted} and
     * {@link #onCaptureProcessStarted}. The second field value includes the estimated
     * post-processing latency. This is the time between {@link #onCaptureProcessStarted} until
     * the processed frame returns back to the client registered surface.
     * Both first and second values will be in milliseconds. The total still capture latency will be
     * the sum of both the first and second values of the pair.
     * The pair is expected to be null if the dynamic latency estimation is not supported.
     * If clients have not configured a still capture output, then this method can also return a
     * null pair.
     * @since 1.4
     */
    @Nullable
    Pair<Long, Long> getRealtimeCaptureLatency();

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
         * @since 1.3
         */
        default void onCaptureCompleted(long timestamp, int captureSequenceId,
                @NonNull Map<CaptureResult.Key, Object> result) {}

        /**
         * Capture progress callback that needs to be called when the process capture is
         * ongoing and includes the estimated progress of the processing.
         *
         * <p>Extensions must ensure that they always call this callback with monotonically
         * increasing values.</p>
         *
         * <p>Extensions are allowed to trigger this callback multiple times but at the minimum the
         * callback is expected to be called once when processing is done with value 100.</p>
         *
         * @param progress             Value between 0 and 100.
         * @since 1.4
         */
        default void onCaptureProcessProgressed(int progress) {}

        /**
         * This method is called instead of
         * {@link #onCaptureProcessStarted} when the camera device failed
         * to produce the required input for the device-specific extension. The
         * cause could be a failed camera capture request, a failed
         * capture result or dropped camera frame.
         * The callback allows clients to be notified
         * about failure reason.
         *
         * @param captureSequenceId id of the current capture sequence
         * @param reason            The capture failure reason @see CaptureFailure#FailureReason
         * @since 1.5
         */
        default void onCaptureFailed(int captureSequenceId, int reason) {}
    }
}