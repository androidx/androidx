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
import android.media.Image;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.OutputSurface;
import androidx.camera.core.impl.RequestProcessor;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl;
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl;
import androidx.camera.extensions.impl.advanced.ImageProcessorImpl;
import androidx.camera.extensions.impl.advanced.ImageReferenceImpl;
import androidx.camera.extensions.impl.advanced.OutputSurfaceImpl;
import androidx.camera.extensions.impl.advanced.RequestProcessorImpl;
import androidx.camera.extensions.impl.advanced.SessionProcessorImpl;
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
    private final SessionProcessorImpl mImpl;
    private final Context mContext;

    public AdvancedSessionProcessor(@NonNull SessionProcessorImpl impl,
            @NonNull List<CaptureRequest.Key> supportedKeys,
            @NonNull Context context) {
        super(supportedKeys);
        mImpl = impl;
        mContext = context;
    }

    @NonNull
    @Override
    protected Camera2SessionConfig initSessionInternal(
            @NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull OutputSurface previewSurfaceConfig,
            @NonNull OutputSurface imageCaptureSurfaceConfig,
            @Nullable OutputSurface imageAnalysisSurfaceConfig) {
        Camera2SessionConfigImpl sessionConfigImpl =
                mImpl.initSession(
                        cameraId,
                        cameraCharacteristicsMap,
                        mContext,
                        new OutputSurfaceImplAdapter(previewSurfaceConfig),
                        new OutputSurfaceImplAdapter(imageCaptureSurfaceConfig),
                        imageAnalysisSurfaceConfig == null
                                ? null : new OutputSurfaceImplAdapter(imageAnalysisSurfaceConfig));

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

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @NonNull
    private static HashMap<CaptureRequest.Key<?>, Object> convertConfigToMap(
            @NonNull Config parameters) {
        HashMap<CaptureRequest.Key<?>, Object> map = new HashMap<>();

        CaptureRequestOptions options =
                CaptureRequestOptions.Builder.from(parameters).build();

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
            @NonNull SessionProcessor.CaptureCallback callback) {
        return mImpl.startCapture(new SessionProcessorImplCaptureCallbackAdapter(callback));
    }

    @Override
    public int startRepeating(@NonNull SessionProcessor.CaptureCallback callback) {
        return mImpl.startRepeating(new SessionProcessorImplCaptureCallbackAdapter(callback));
    }

    @Override
    public int startTrigger(@NonNull Config config, @NonNull CaptureCallback callback) {
        HashMap<CaptureRequest.Key<?>, Object> map = convertConfigToMap(config);
        return mImpl.startTrigger(map, new SessionProcessorImplCaptureCallbackAdapter(callback));
    }

    @Override
    public void stopRepeating() {
        mImpl.stopRepeating();
    }

    @Override
    public void abortCapture(int captureSequenceId) {
        mImpl.abortCapture(captureSequenceId);
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
        @OptIn(markerClass = ExperimentalCamera2Interop.class)
        RequestAdapter(@NonNull RequestProcessorImpl.Request implRequest) {
            mImplRequest = implRequest;

            List<Integer> targetOutputConfigIds = new ArrayList<>();
            for (Integer outputConfigId : implRequest.getTargetOutputConfigIds()) {
                targetOutputConfigIds.add(outputConfigId);
            }
            mTargetOutputConfigIds = targetOutputConfigIds;

            Camera2ImplConfig.Builder camera2ConfigBuilder = new Camera2ImplConfig.Builder();
            for (CaptureRequest.Key<?> key : implRequest.getParameters().keySet()) {
                @SuppressWarnings("unchecked")
                CaptureRequest.Key<Object> objKey = (CaptureRequest.Key<Object>) key;
                camera2ConfigBuilder.setCaptureRequestOption(objKey,
                        implRequest.getParameters().get(objKey));
            }
            mParameters = camera2ConfigBuilder.build();

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
            CaptureResult captureResult =
                    Camera2CameraCaptureResultConverter.getCaptureResult(cameraCaptureResult);
            Preconditions.checkArgument(captureResult != null,
                    "Cannot get CaptureResult from the cameraCaptureResult ");
            mCallback.onCaptureProgressed(getImplRequest(request), captureResult);
        }

        @Override
        public void onCaptureCompleted(
                @NonNull RequestProcessor.Request request,
                @Nullable CameraCaptureResult cameraCaptureResult) {
            CaptureResult captureResult =
                    Camera2CameraCaptureResultConverter.getCaptureResult(cameraCaptureResult);
            Preconditions.checkArgument(captureResult instanceof TotalCaptureResult,
                    "CaptureResult in cameraCaptureResult is not a TotalCaptureResult");
            mCallback.onCaptureCompleted(getImplRequest(request),
                    (TotalCaptureResult) captureResult);
        }

        @Override
        public void onCaptureFailed(
                @NonNull RequestProcessor.Request request,
                @Nullable CameraCaptureFailure cameraCaptureFailure) {
            CaptureFailure captureFailure =
                    Camera2CameraCaptureResultConverter.getCaptureFailure(cameraCaptureFailure);
            Preconditions.checkArgument(captureFailure != null,
                    "CameraCaptureFailure does not contain CaptureFailure.");
            mCallback.onCaptureFailed(getImplRequest(request), captureFailure);
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
        }
    }
}
