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

package androidx.camera.extensions.internal.sessionprocessor;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.OutputSurface;
import androidx.camera.core.impl.OutputSurfaceConfiguration;
import androidx.camera.core.impl.RequestProcessor;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl;
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl;
import androidx.camera.extensions.impl.advanced.ImageProcessorImpl;
import androidx.camera.extensions.impl.advanced.ImageReferenceImpl;
import androidx.camera.extensions.impl.advanced.OutputSurfaceConfigurationImpl;
import androidx.camera.extensions.impl.advanced.OutputSurfaceImpl;
import androidx.camera.extensions.impl.advanced.RequestProcessorImpl;
import androidx.camera.extensions.impl.advanced.SessionProcessorImpl;
import androidx.camera.extensions.internal.ClientVersion;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.RequestOptionConfig;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.extensions.internal.Version;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link SessionProcessor} based on OEMs' {@link SessionProcessorImpl}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AdvancedSessionProcessor extends SessionProcessorBase {
    private static final String TAG = "AdvancedSessionProcessor";
    @NonNull
    private final SessionProcessorImpl mImpl;
    @NonNull
    private final VendorExtender mVendorExtender;
    @NonNull
    private final Context mContext;
    private boolean mIsPostviewConfigured = false;

    public AdvancedSessionProcessor(@NonNull SessionProcessorImpl impl,
            @NonNull List<CaptureRequest.Key> supportedKeys,
            @NonNull VendorExtender vendorExtender,
            @NonNull Context context) {
        super(supportedKeys);
        mImpl = impl;
        mVendorExtender = vendorExtender;
        mContext = context;
    }

    @NonNull
    @Override
    protected Camera2SessionConfig initSessionInternal(
            @NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull OutputSurfaceConfiguration outputSurfaceConfig) {
        Camera2SessionConfigImpl sessionConfigImpl = null;
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            sessionConfigImpl =
                    mImpl.initSession(
                            cameraId,
                            cameraCharacteristicsMap,
                            mContext,
                            new OutputSurfaceConfigurationImplAdapter(outputSurfaceConfig));

        }

        // In case of OEM doesn't implement the v1.4 version of initSession, we fallback to invoke
        // prior version.
        if (sessionConfigImpl == null) {
            sessionConfigImpl =
                    mImpl.initSession(
                            cameraId,
                            cameraCharacteristicsMap,
                            mContext,
                            new OutputSurfaceImplAdapter(
                                    outputSurfaceConfig.getPreviewOutputSurface()),
                            new OutputSurfaceImplAdapter(
                                    outputSurfaceConfig.getImageCaptureOutputSurface()),
                            outputSurfaceConfig.getImageAnalysisOutputSurface() == null
                                    ? null : new OutputSurfaceImplAdapter(
                                    outputSurfaceConfig.getImageAnalysisOutputSurface()));
        }

        mIsPostviewConfigured = outputSurfaceConfig.getPostviewOutputSurface() != null;
        // Convert Camera2SessionConfigImpl(implemented in OEM) into Camera2SessionConfig
        return convertToCamera2SessionConfig(sessionConfigImpl);
    }

    private Camera2SessionConfig convertToCamera2SessionConfig(
            @NonNull Camera2SessionConfigImpl sessionConfigImpl) {
        Camera2SessionConfigBuilder camera2SessionConfigBuilder = new Camera2SessionConfigBuilder();
        for (Camera2OutputConfigImpl outputConfigImpl : sessionConfigImpl.getOutputConfigs()) {
            Camera2OutputConfig outputConfig =
                    Camera2OutputConfigConverter.fromImpl(outputConfigImpl);
            camera2SessionConfigBuilder.addOutputConfig(outputConfig);
        }

        for (CaptureRequest.Key<?> key : sessionConfigImpl.getSessionParameters().keySet()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> objKey = (CaptureRequest.Key<Object>) key;
            camera2SessionConfigBuilder.addSessionParameter(objKey,
                    sessionConfigImpl.getSessionParameters().get(objKey));
        }
        camera2SessionConfigBuilder
                .setSessionTemplateId(sessionConfigImpl.getSessionTemplateId());
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            try {
                int sessionType = sessionConfigImpl.getSessionType();
                if (sessionType == -1) { // -1 means using default value
                    sessionType = SessionConfiguration.SESSION_REGULAR;
                }
                camera2SessionConfigBuilder.setSessionType(sessionType);
            } catch (NoSuchMethodError e) {
                camera2SessionConfigBuilder.setSessionType(SessionConfiguration.SESSION_REGULAR);
            }
        }
        return camera2SessionConfigBuilder.build();
    }

    @Override
    protected void deInitSessionInternal() {
        mImpl.deInitSession();
    }

    @Override
    public void setParameters(
            @NonNull Config parameters) {
        HashMap<CaptureRequest.Key<?>, Object> map = convertConfigToMap(parameters);
        mImpl.setParameters(map);
    }

    @NonNull
    private static HashMap<CaptureRequest.Key<?>, Object> convertConfigToMap(
            @NonNull Config parameters) {
        HashMap<CaptureRequest.Key<?>, Object> map = new HashMap<>();

        RequestOptionConfig options =
                RequestOptionConfig.Builder.from(parameters).build();

        for (Config.Option<?> option : options.listOptions()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();
            map.put(key, options.retrieveOption(option));
        }
        return map;
    }

    @Override
    public void onCaptureSessionStart(
            @NonNull RequestProcessor requestProcessor) {
        mImpl.onCaptureSessionStart(new RequestProcessorImplAdapter(requestProcessor));
    }

    @Override
    public void onCaptureSessionEnd() {
        mImpl.onCaptureSessionEnd();
    }

    @Override
    public int startCapture(
            boolean postviewEnabled,
            @NonNull SessionProcessor.CaptureCallback callback) {
        SessionProcessorImplCaptureCallbackAdapter stillCaptureCallback =
                new SessionProcessorImplCaptureCallbackAdapter(callback);

        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && mIsPostviewConfigured && postviewEnabled
                && mVendorExtender.isPostviewAvailable()) {
            Logger.d(TAG, "startCaptureWithPostview");
            return mImpl.startCaptureWithPostview(stillCaptureCallback);
        } else {
            Logger.d(TAG, "startCapture");
            return mImpl.startCapture(stillCaptureCallback);
        }
    }

    @Override
    public int startRepeating(@NonNull SessionProcessor.CaptureCallback callback) {
        return mImpl.startRepeating(new SessionProcessorImplCaptureCallbackAdapter(callback));
    }

    @Override
    public int startTrigger(@NonNull Config config, @NonNull CaptureCallback callback) {
        HashMap<CaptureRequest.Key<?>, Object> map = convertConfigToMap(config);
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)) {
            return mImpl.startTrigger(map,
                    new SessionProcessorImplCaptureCallbackAdapter(callback));
        }
        return -1;
    }

    @Override
    public void stopRepeating() {
        mImpl.stopRepeating();
    }

    @Override
    public void abortCapture(int captureSequenceId) {
        mImpl.abortCapture(captureSequenceId);
    }

    @Nullable
    @Override
    public Pair<Long, Long> getRealtimeCaptureLatency() {
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            return mImpl.getRealtimeCaptureLatency();
        }
        return null;
    }

    @NonNull
    @Override
    public Map<Integer, List<Size>> getSupportedPostviewSize(@NonNull Size captureSize) {
        return mVendorExtender.getSupportedPostviewResolutions(captureSize);
    }

    /**
     * Adapter to transform a {@link OutputSurface} to a {@link OutputSurfaceImpl}.
     */
    private static class OutputSurfaceImplAdapter implements OutputSurfaceImpl {
        private final OutputSurface mOutputSurface;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        OutputSurfaceImplAdapter(OutputSurface outputSurface) {
            mOutputSurface = outputSurface;
        }

        @NonNull
        @Override
        public Surface getSurface() {
            return mOutputSurface.getSurface();
        }

        @NonNull
        @Override
        public Size getSize() {
            return mOutputSurface.getSize();
        }

        @Override
        public int getImageFormat() {
            return mOutputSurface.getImageFormat();
        }
    }

    private static class OutputSurfaceConfigurationImplAdapter implements
            OutputSurfaceConfigurationImpl {
        private final OutputSurfaceImpl mPreviewOutputSurface;
        private final OutputSurfaceImpl mCaptureOutputSurface;
        private final OutputSurfaceImpl mAnalysisOutputSurface;
        private final OutputSurfaceImpl mPostviewOutputSurface;

        OutputSurfaceConfigurationImplAdapter(
                @NonNull OutputSurfaceConfiguration outputSurfaceConfig) {
            mPreviewOutputSurface = new OutputSurfaceImplAdapter(
                    outputSurfaceConfig.getPreviewOutputSurface());
            mCaptureOutputSurface = new OutputSurfaceImplAdapter(
                    outputSurfaceConfig.getImageCaptureOutputSurface());
            mAnalysisOutputSurface =
                    outputSurfaceConfig.getImageAnalysisOutputSurface() != null
                            ? new OutputSurfaceImplAdapter(
                                    outputSurfaceConfig.getImageAnalysisOutputSurface()) : null;
            mPostviewOutputSurface =
                    outputSurfaceConfig.getPostviewOutputSurface() != null
                            ? new OutputSurfaceImplAdapter(
                                    outputSurfaceConfig.getPostviewOutputSurface()) : null;
        }

        @NonNull
        @Override
        public OutputSurfaceImpl getPreviewOutputSurface() {
            return mPreviewOutputSurface;
        }

        @NonNull
        @Override
        public OutputSurfaceImpl getImageCaptureOutputSurface() {
            return mCaptureOutputSurface;
        }

        @Nullable
        @Override
        public OutputSurfaceImpl getImageAnalysisOutputSurface() {
            return mAnalysisOutputSurface;
        }

        @Nullable
        @Override
        public OutputSurfaceImpl getPostviewOutputSurface() {
            return mPostviewOutputSurface;
        }
    }

    /**
     * Adapter to transform a {@link RequestProcessor} to {@link RequestProcessorImpl}.
     */
    private class RequestProcessorImplAdapter implements RequestProcessorImpl {
        private final RequestProcessor mRequestProcessor;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        RequestProcessorImplAdapter(@NonNull RequestProcessor requestProcessor) {
            mRequestProcessor = requestProcessor;
        }

        @Override
        public void setImageProcessor(int outputConfigId,
                @NonNull ImageProcessorImpl imageProcessorImpl) {
            AdvancedSessionProcessor.this.setImageProcessor(outputConfigId,
                    new ImageProcessorAdapter(imageProcessorImpl));
        }

        @Override
        public int submit(
                @NonNull RequestProcessorImpl.Request request, @NonNull Callback callback) {
            return mRequestProcessor.submit(new RequestAdapter(request),
                    new CallbackAdapter(callback));
        }

        @Override
        public int submit(
                @NonNull List<RequestProcessorImpl.Request> requests, @NonNull Callback callback) {
            ArrayList<RequestProcessor.Request> outRequests = new ArrayList<>();
            for (RequestProcessorImpl.Request request : requests) {
                outRequests.add(new RequestAdapter(request));
            }
            return mRequestProcessor.submit(outRequests, new CallbackAdapter(callback));
        }

        @Override
        public int setRepeating(
                @NonNull RequestProcessorImpl.Request request, @NonNull Callback callback) {
            return mRequestProcessor.setRepeating(new RequestAdapter(request),
                    new CallbackAdapter(callback));
        }

        @Override
        public void abortCaptures() {
            mRequestProcessor.abortCaptures();
        }

        @Override
        public void stopRepeating() {
            mRequestProcessor.stopRepeating();
        }
    }

    /**
     * Adapter to transform a {@link RequestProcessorImpl.Request} to a
     * {@link RequestProcessor.Request}.
     */
    private static class RequestAdapter implements RequestProcessor.Request {
        private final RequestProcessorImpl.Request mImplRequest;
        private final List<Integer> mTargetOutputConfigIds;
        private final Config mParameters;
        private final int mTemplateId;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        RequestAdapter(@NonNull RequestProcessorImpl.Request implRequest) {
            mImplRequest = implRequest;

            List<Integer> targetOutputConfigIds = new ArrayList<>();
            for (Integer outputConfigId : implRequest.getTargetOutputConfigIds()) {
                targetOutputConfigIds.add(outputConfigId);
            }
            mTargetOutputConfigIds = targetOutputConfigIds;

            RequestOptionConfig.Builder optionBuilder = new RequestOptionConfig.Builder();
            for (CaptureRequest.Key<?> key : implRequest.getParameters().keySet()) {
                @SuppressWarnings("unchecked")
                CaptureRequest.Key<Object> objKey = (CaptureRequest.Key<Object>) key;
                optionBuilder.setCaptureRequestOption(objKey,
                        implRequest.getParameters().get(objKey));
            }
            mParameters = optionBuilder.build();
            mTemplateId = implRequest.getTemplateId();
        }

        @NonNull
        @Override
        public List<Integer> getTargetOutputConfigIds() {
            return mTargetOutputConfigIds;
        }

        @NonNull
        @Override
        public Config getParameters() {
            return mParameters;
        }

        @Override
        public int getTemplateId() {
            return mTemplateId;
        }

        @Nullable
        public RequestProcessorImpl.Request getImplRequest() {
            return mImplRequest;
        }
    }

    /**
     * Adapter to transform a {@link ImageProcessorImpl} to {@link ImageProcessor}.
     */
    private static class ImageProcessorAdapter implements ImageProcessor {
        private final ImageProcessorImpl mImpl;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        ImageProcessorAdapter(ImageProcessorImpl impl) {
            mImpl = impl;
        }

        @Override
        public void onNextImageAvailable(int outputStreamId, long timestampNs,
                @NonNull ImageReference imageReference, @Nullable String physicalCameraId) {
            mImpl.onNextImageAvailable(outputStreamId, timestampNs,
                    new ImageReferenceImplAdapter(imageReference), physicalCameraId);
        }
    }

    /**
     * Adapter to transform a {@link ImageReference} to a {@link ImageReferenceImpl}.
     */
    private static class ImageReferenceImplAdapter implements ImageReferenceImpl {
        private final ImageReference mImageReference;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        ImageReferenceImplAdapter(ImageReference imageReference) {
            mImageReference = imageReference;
        }

        @Override
        public boolean increment() {
            return mImageReference.increment();
        }

        @Override
        public boolean decrement() {
            return mImageReference.decrement();
        }

        @Nullable
        @Override
        public Image get() {
            return mImageReference.get();
        }
    }

    /**
     * Adapter to transform a {@link RequestProcessorImpl.Callback} to a
     * {@link RequestProcessor.Callback}.
     */
    private static class CallbackAdapter implements RequestProcessor.Callback {
        private final RequestProcessorImpl.Callback mCallback;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        CallbackAdapter(RequestProcessorImpl.Callback callback) {
            mCallback = callback;
        }

        @Override
        public void onCaptureStarted(
                @NonNull RequestProcessor.Request request,
                long frameNumber, long timestamp) {
            mCallback.onCaptureStarted(getImplRequest(request), frameNumber,
                    timestamp);
        }

        @Override
        public void onCaptureProgressed(
                @NonNull RequestProcessor.Request request,
                @NonNull CameraCaptureResult cameraCaptureResult) {
            CaptureResult captureResult = cameraCaptureResult.getCaptureResult();
            Preconditions.checkArgument(captureResult != null,
                    "Cannot get CaptureResult from the cameraCaptureResult ");
            mCallback.onCaptureProgressed(getImplRequest(request), captureResult);
        }

        @Override
        public void onCaptureCompleted(
                @NonNull RequestProcessor.Request request,
                @Nullable CameraCaptureResult cameraCaptureResult) {
            CaptureResult captureResult = cameraCaptureResult.getCaptureResult();
            Preconditions.checkArgument(captureResult instanceof TotalCaptureResult,
                    "CaptureResult in cameraCaptureResult is not a TotalCaptureResult");
            mCallback.onCaptureCompleted(getImplRequest(request),
                    (TotalCaptureResult) captureResult);
        }

        @Override
        public void onCaptureFailed(
                @NonNull RequestProcessor.Request request,
                @Nullable CameraCaptureFailure cameraCaptureFailure) {
            Object captureFailure = cameraCaptureFailure.getCaptureFailure();
            Preconditions.checkArgument(captureFailure instanceof CaptureFailure,
                    "CameraCaptureFailure does not contain CaptureFailure.");
            mCallback.onCaptureFailed(getImplRequest(request), (CaptureFailure) captureFailure);
        }

        @Override
        public void onCaptureBufferLost(
                @NonNull RequestProcessor.Request request,
                long frameNumber, int outputStreamId) {
            mCallback.onCaptureBufferLost(getImplRequest(request), frameNumber, outputStreamId);
        }

        @Override
        public void onCaptureSequenceCompleted(
                int sequenceId, long frameNumber) {
            mCallback.onCaptureSequenceCompleted(sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(int sequenceId) {
            mCallback.onCaptureSequenceAborted(sequenceId);
        }

        private RequestProcessorImpl.Request getImplRequest(
                RequestProcessor.Request request) {
            Preconditions.checkArgument(request instanceof RequestAdapter);

            RequestAdapter requestProcessorRequest = (RequestAdapter) request;
            return requestProcessorRequest.getImplRequest();
        }
    }

    /**
     * Adapter to transform a {@link SessionProcessor.CaptureCallback} to a
     * {@link SessionProcessorImpl.CaptureCallback}.
     */
    private static class SessionProcessorImplCaptureCallbackAdapter implements
            SessionProcessorImpl.CaptureCallback {
        private final SessionProcessor.CaptureCallback mCaptureCallback;

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        SessionProcessorImplCaptureCallbackAdapter(
                @NonNull SessionProcessor.CaptureCallback callback) {
            mCaptureCallback = callback;
        }

        @Override
        public void onCaptureStarted(
                int captureSequenceId,
                long timestamp) {
            mCaptureCallback.onCaptureStarted(captureSequenceId, timestamp);
        }

        @Override
        public void onCaptureProcessStarted(
                int captureSequenceId) {
            mCaptureCallback.onCaptureProcessStarted(captureSequenceId);
        }

        @Override
        public void onCaptureFailed(int captureSequenceId) {
            mCaptureCallback.onCaptureFailed(captureSequenceId);
        }

        @Override
        public void onCaptureSequenceCompleted(int captureSequenceId) {
            mCaptureCallback.onCaptureSequenceCompleted(captureSequenceId);
        }

        @Override
        public void onCaptureSequenceAborted(int captureSequenceId) {
            mCaptureCallback.onCaptureSequenceAborted(captureSequenceId);
        }

        @Override
        public void onCaptureCompleted(long timestamp, int captureSequenceId,
                Map<CaptureResult.Key, Object> result) {
            mCaptureCallback.onCaptureCompleted(timestamp, captureSequenceId, result);
        }

        @Override
        public void onCaptureProcessProgressed(int progress) {
            mCaptureCallback.onCaptureProcessProgressed(progress);
        }
    }
}
