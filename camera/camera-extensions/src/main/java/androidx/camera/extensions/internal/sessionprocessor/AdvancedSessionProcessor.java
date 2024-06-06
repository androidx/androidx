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

import static android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_AUTOMATIC;
import static android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_BOKEH;
import static android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH;
import static android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_HDR;
import static android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_NIGHT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.os.Build;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.OutputSurface;
import androidx.camera.core.impl.OutputSurfaceConfiguration;
import androidx.camera.core.impl.RequestProcessor;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.extensions.ExtensionMode;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link SessionProcessor} based on OEMs' {@link SessionProcessorImpl}.
 */
public class AdvancedSessionProcessor extends SessionProcessorBase {
    private static final String TAG = "AdvancedSessionProcessor";
    @NonNull
    private final SessionProcessorImpl mImpl;
    @NonNull
    private final VendorExtender mVendorExtender;
    @NonNull
    private final Context mContext;
    @ExtensionMode.Mode
    private final int mMode;
    @Nullable
    private final MutableLiveData<Integer> mCurrentExtensionTypeLiveData;
    private boolean mIsPostviewConfigured = false;
    // Caches the working capture config so that the new extension strength can be applied on top
    // of the existing config.
    @GuardedBy("mLock")
    private HashMap<CaptureRequest.Key<?>, Object> mWorkingCaptureConfigMap = new HashMap<>();
    // Caches the capture callback adapter so that repeating can be started again to apply the
    // new extension strength setting.
    @GuardedBy("mLock")
    private SessionProcessorImplCaptureCallbackAdapter mRepeatingCaptureCallbackAdapter = null;
    @Nullable
    private final MutableLiveData<Integer> mExtensionStrengthLiveData;
    @Nullable
    private final ExtensionMetadataMonitor mExtensionMetadataMonitor;
    private final boolean mWillReceiveOnCaptureCompleted;

    public AdvancedSessionProcessor(@NonNull SessionProcessorImpl impl,
            @NonNull List<CaptureRequest.Key> supportedKeys,
            @NonNull VendorExtender vendorExtender,
            @NonNull Context context) {
        this(impl, supportedKeys, vendorExtender, context, ExtensionMode.NONE);
    }

    public AdvancedSessionProcessor(@NonNull SessionProcessorImpl impl,
            @NonNull List<CaptureRequest.Key> supportedKeys,
            @NonNull VendorExtender vendorExtender,
            @NonNull Context context,
            @ExtensionMode.Mode int mode) {
        super(supportedKeys);
        mImpl = impl;
        mVendorExtender = vendorExtender;
        mContext = context;
        mWillReceiveOnCaptureCompleted = vendorExtender.willReceiveOnCaptureCompleted();
        mMode = mode;
        mCurrentExtensionTypeLiveData = isCurrentExtensionModeAvailable() ? new MutableLiveData<>(
                mMode) : null;
        mExtensionStrengthLiveData = isExtensionStrengthAvailable() ? new MutableLiveData<>(100)
                : null;
        if (mCurrentExtensionTypeLiveData != null || mExtensionStrengthLiveData != null) {
            mExtensionMetadataMonitor = new ExtensionMetadataMonitor(mCurrentExtensionTypeLiveData,
                    mExtensionStrengthLiveData);
        } else {
            mExtensionMetadataMonitor = null;
        }
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
        // Resets the extension type and strength result when initializing the session
        if (mCurrentExtensionTypeLiveData != null) {
            mCurrentExtensionTypeLiveData.postValue(mMode);
        }
        if (mExtensionStrengthLiveData != null) {
            mExtensionStrengthLiveData.postValue(100);
        }
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
        synchronized (mLock) {
            // Clears the working config map
            mWorkingCaptureConfigMap = new HashMap<>();
            mRepeatingCaptureCallbackAdapter = null;
        }
        mImpl.deInitSession();
    }

    @Override
    public boolean isCurrentExtensionModeAvailable() {
        return mVendorExtender.isCurrentExtensionModeAvailable();
    }

    @NonNull
    @Override
    public LiveData<Integer> getCurrentExtensionMode() {
        return mCurrentExtensionTypeLiveData;
    }

    @Override
    public boolean isExtensionStrengthAvailable() {
        return mVendorExtender.isExtensionStrengthAvailable();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setExtensionStrength(@IntRange(from = 0, to = 100) int strength) {
        if (!isExtensionStrengthAvailable()
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return;
        }

        SessionProcessorImplCaptureCallbackAdapter captureCallbackAdapter;
        HashMap<CaptureRequest.Key<?>, Object> captureConfigMap;

        synchronized (mLock) {
            mExtensionStrength = strength;
            mWorkingCaptureConfigMap.put(CaptureRequest.EXTENSION_STRENGTH, mExtensionStrength);
            captureCallbackAdapter = mRepeatingCaptureCallbackAdapter;
            captureConfigMap =
                    (HashMap<CaptureRequest.Key<?>, Object>) mWorkingCaptureConfigMap.clone();
        }

        mImpl.setParameters(captureConfigMap);

        // Starts the repeating again to apply the new strength setting if it has been started.
        // Otherwise, the new strength setting will be applied when the capture session is
        // configured and repeating is started.
        if (captureCallbackAdapter != null) {
            mImpl.startRepeating(captureCallbackAdapter);
        }
    }

    @SuppressLint("KotlinPropertyAccess")
    @NonNull
    @Override
    public LiveData<Integer> getExtensionStrength() {
        return mExtensionStrengthLiveData;
    }

    @Override
    public void setParameters(
            @NonNull Config parameters) {
        HashMap<CaptureRequest.Key<?>, Object> captureConfigMap;

        synchronized (mLock) {
            captureConfigMap = convertConfigToMap(parameters);
            // Applies extension strength setting if it is set via
            // CameraExtensionsControl#setExtensionStrength() API.
            if (mExtensionStrength != EXTENSION_STRENGTH_UNKNOWN
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                captureConfigMap.put(CaptureRequest.EXTENSION_STRENGTH, mExtensionStrength);
            }
            mWorkingCaptureConfigMap = captureConfigMap;
        }

        mImpl.setParameters(captureConfigMap);
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
            @NonNull TagBundle tagBundle,
            @NonNull SessionProcessor.CaptureCallback callback) {
        Logger.d(TAG, "startCapture postviewEnabled = " + postviewEnabled
                + " mWillReceiveOnCaptureCompleted = " + mWillReceiveOnCaptureCompleted);
        SessionProcessorImplCaptureCallbackAdapter stillCaptureCallback =
                new SessionProcessorImplCaptureCallbackAdapter(
                        callback, tagBundle, mWillReceiveOnCaptureCompleted);

        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && mIsPostviewConfigured && postviewEnabled
                && mVendorExtender.isPostviewAvailable()) {
            return mImpl.startCaptureWithPostview(stillCaptureCallback);
        } else {
            return mImpl.startCapture(stillCaptureCallback);
        }
    }

    @Override
    public int startRepeating(@NonNull TagBundle tagBundle,
            @NonNull SessionProcessor.CaptureCallback callback) {
        SessionProcessorImplCaptureCallbackAdapter captureCallbackAdapter;
        synchronized (mLock) {
            captureCallbackAdapter = new SessionProcessorImplCaptureCallbackAdapter(callback,
                    tagBundle, mExtensionMetadataMonitor, mWillReceiveOnCaptureCompleted);
            mRepeatingCaptureCallbackAdapter = captureCallbackAdapter;
        }
        return mImpl.startRepeating(captureCallbackAdapter);
    }

    @Override
    public int startTrigger(@NonNull Config config, @NonNull TagBundle tagBundle,
            @NonNull CaptureCallback callback) {
        HashMap<CaptureRequest.Key<?>, Object> map = convertConfigToMap(config);
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)) {
            return mImpl.startTrigger(map,
                    new SessionProcessorImplCaptureCallbackAdapter(
                            callback, tagBundle, mWillReceiveOnCaptureCompleted));
        }
        return -1;
    }

    @Override
    public void stopRepeating() {
        mImpl.stopRepeating();
        synchronized (mLock) {
            mRepeatingCaptureCallbackAdapter = null;
        }
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
        CallbackAdapter(@NonNull RequestProcessorImpl.Callback callback) {
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
        @Nullable
        private final ExtensionMetadataMonitor mExtensionMetadataMonitor;
        @NonNull
        private final TagBundle mTagBundle;
        private long mOnCaptureStartedTimestamp = -1;
        private boolean mWillReceiveOnCaptureCompleted;

        SessionProcessorImplCaptureCallbackAdapter(
                @NonNull SessionProcessor.CaptureCallback callback,
                @NonNull TagBundle tagBundle,
                boolean willReceiveOnCaptureCompleted) {
            this(callback, tagBundle, null, willReceiveOnCaptureCompleted);
        }

        @SuppressWarnings("WeakerAccess") /* synthetic accessor */
        SessionProcessorImplCaptureCallbackAdapter(
                @NonNull SessionProcessor.CaptureCallback callback,
                @NonNull TagBundle tagBundle,
                @Nullable ExtensionMetadataMonitor extensionMetadataMonitor,
                boolean willReceiveOnCaptureCompleted) {
            mCaptureCallback = callback;
            mTagBundle = tagBundle;
            mExtensionMetadataMonitor = extensionMetadataMonitor;
            mWillReceiveOnCaptureCompleted = willReceiveOnCaptureCompleted;
        }

        @Override
        public void onCaptureStarted(
                int captureSequenceId,
                long timestamp) {
            mOnCaptureStartedTimestamp = timestamp;
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
            if (!mWillReceiveOnCaptureCompleted) {
                // If SessionProcessorImpl.CaptureCallback.onCaptureCompleted won't be invoked,
                // We finish the capture sequence using the timestamp retrieved at onCaptureStarted
                // when onCaptureSequenceCompleted is invoked.
                mCaptureCallback.onCaptureCompleted(mOnCaptureStartedTimestamp,
                        captureSequenceId,
                        new KeyValueMapCameraCaptureResult(
                                mOnCaptureStartedTimestamp, mTagBundle, Collections.emptyMap()));
                mCaptureCallback.onCaptureSequenceCompleted(captureSequenceId);
            }
        }

        @Override
        public void onCaptureSequenceAborted(int captureSequenceId) {
            mCaptureCallback.onCaptureSequenceAborted(captureSequenceId);
        }

        @Override
        public void onCaptureCompleted(long timestamp, int captureSequenceId,
                Map<CaptureResult.Key, Object> result) {
            if (mExtensionMetadataMonitor != null) {
                mExtensionMetadataMonitor.checkExtensionMetadata(result);
            }
            if (mWillReceiveOnCaptureCompleted) {
                mCaptureCallback.onCaptureCompleted(timestamp, captureSequenceId,
                        new KeyValueMapCameraCaptureResult(timestamp, mTagBundle, result));
                mCaptureCallback.onCaptureSequenceCompleted(captureSequenceId);
            }
        }

        @Override
        public void onCaptureProcessProgressed(int progress) {
            mCaptureCallback.onCaptureProcessProgressed(progress);
        }
    }

    /**
     * Monitors the extension metadata (extension strength, type) changes from the capture results.
     */
    private static class ExtensionMetadataMonitor {
        @Nullable
        private final MutableLiveData<Integer> mCurrentExtensionTypeLiveData;
        @Nullable
        private final MutableLiveData<Integer> mExtensionStrengthLiveData;

        ExtensionMetadataMonitor(
                @Nullable MutableLiveData<Integer> currentExtensionTypeLiveData,
                @Nullable MutableLiveData<Integer> extensionStrengthLiveData) {
            mCurrentExtensionTypeLiveData = currentExtensionTypeLiveData;
            mExtensionStrengthLiveData = extensionStrengthLiveData;
        }

        void checkExtensionMetadata(Map<CaptureResult.Key, Object> captureResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

                if (mCurrentExtensionTypeLiveData != null) {
                    // Monitors and update current extension type
                    Object extensionType = captureResult.get(CaptureResult.EXTENSION_CURRENT_TYPE);
                    // The returned type should be the value defined by the Camera2 API.
                    // Needs to
                    // convert it to the value defined by CameraX.
                    if (extensionType != null && !Objects.equals(
                            mCurrentExtensionTypeLiveData.getValue(),
                            convertExtensionMode((int) extensionType))) {
                        mCurrentExtensionTypeLiveData.postValue(
                                convertExtensionMode((int) extensionType));
                    }
                }

                if (mExtensionStrengthLiveData != null) {
                    // Monitors and update current extension strength
                    Object extensionStrength = captureResult.get(CaptureResult.EXTENSION_STRENGTH);
                    if (extensionStrength != null && !Objects.equals(
                            mExtensionStrengthLiveData.getValue(), extensionStrength)) {
                        mExtensionStrengthLiveData.postValue((Integer) extensionStrength);
                    }
                }
            }
        }

        @ExtensionMode.Mode
        private int convertExtensionMode(int camera2ExtensionMode) {
            switch (camera2ExtensionMode) {
                case EXTENSION_AUTOMATIC:
                    return ExtensionMode.AUTO;
                case EXTENSION_FACE_RETOUCH:
                    return ExtensionMode.FACE_RETOUCH;
                case EXTENSION_BOKEH:
                    return ExtensionMode.BOKEH;
                case EXTENSION_HDR:
                    return ExtensionMode.HDR;
                case EXTENSION_NIGHT:
                    return ExtensionMode.NIGHT;
                default:
                    return ExtensionMode.NONE;
            }
        }
    }
}
