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

package androidx.camera.extensions.internal.sessionprocessor;

import static androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR;
import static androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.util.Pair;
import android.util.Size;

import androidx.annotation.GuardedBy;
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
import androidx.camera.extensions.impl.CaptureProcessorImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl;
import androidx.camera.extensions.internal.Camera2CameraCaptureResult;
import androidx.camera.extensions.internal.ClientVersion;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.RequestOptionConfig;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.internal.compat.workaround.OnEnableDisableSessionDurationCheck;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link SessionProcessor} based on OEMs' basic extender implementation.
 */
public class BasicExtenderSessionProcessor extends SessionProcessorBase {
    private static final String TAG = "BasicSessionProcessor";

    private static final int PREVIEW_PROCESS_MAX_IMAGES = 2;
    private static final long INVALID_TIMESTAMP = -1L;
    @NonNull
    private final Context mContext;
    @NonNull
    private final PreviewExtenderImpl mPreviewExtenderImpl;
    @NonNull
    private final ImageCaptureExtenderImpl mImageCaptureExtenderImpl;

    volatile StillCaptureProcessor mStillCaptureProcessor = null;
    volatile PreviewProcessor mPreviewProcessor = null;
    volatile RequestUpdateProcessorImpl mRequestUpdateProcessor = null;
    private volatile Camera2OutputConfig mPreviewOutputConfig;
    private volatile Camera2OutputConfig mCaptureOutputConfig;
    @Nullable
    private volatile Camera2OutputConfig mAnalysisOutputConfig = null;
    private volatile OutputSurface mPreviewOutputSurface;
    private volatile OutputSurface mCaptureOutputSurface;
    private volatile RequestProcessor mRequestProcessor;
    volatile boolean mIsCapturing = false;
    private final AtomicInteger mNextCaptureSequenceId = new AtomicInteger(0);
    static AtomicInteger sLastOutputConfigId = new AtomicInteger(0);
    @GuardedBy("mLock")
    private final Map<CaptureRequest.Key<?>, Object> mParameters = new LinkedHashMap<>();
    @GuardedBy("mLock")
    private final Map<Integer, Long> mRequestCompletedTimestampMap = new HashMap<>();
    private OnEnableDisableSessionDurationCheck mOnEnableDisableSessionDurationCheck =
            new OnEnableDisableSessionDurationCheck();
    @Nullable
    private OutputSurface mPostviewOutputSurface;
    private final VendorExtender mVendorExtender;
    private final boolean mWillReceiveOnCaptureCompleted;

    public BasicExtenderSessionProcessor(@NonNull PreviewExtenderImpl previewExtenderImpl,
            @NonNull ImageCaptureExtenderImpl imageCaptureExtenderImpl,
            @NonNull List<CaptureRequest.Key> supportedRequestKeys,
            @NonNull VendorExtender vendorExtender,
            @NonNull Context context) {
        super(supportedRequestKeys);
        mPreviewExtenderImpl = previewExtenderImpl;
        mImageCaptureExtenderImpl = imageCaptureExtenderImpl;
        mContext = context;
        mVendorExtender = vendorExtender;
        mWillReceiveOnCaptureCompleted = mVendorExtender.willReceiveOnCaptureCompleted();
    }

    @NonNull
    @Override
    protected Camera2SessionConfig initSessionInternal(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
            @NonNull OutputSurfaceConfiguration outputSurfaceConfiguration) {
        Logger.d(TAG, "PreviewExtenderImpl.onInit");
        mPreviewExtenderImpl.onInit(cameraId, cameraCharacteristicsMap.get(cameraId),
                mContext);
        Logger.d(TAG, "ImageCaptureExtenderImpl.onInit");
        mImageCaptureExtenderImpl.onInit(cameraId, cameraCharacteristicsMap.get(cameraId),
                mContext);

        mPreviewOutputSurface = outputSurfaceConfiguration.getPreviewOutputSurface();
        mCaptureOutputSurface = outputSurfaceConfiguration.getImageCaptureOutputSurface();
        mPostviewOutputSurface = outputSurfaceConfiguration.getPostviewOutputSurface();

        // Preview
        PreviewExtenderImpl.ProcessorType processorType =
                mPreviewExtenderImpl.getProcessorType();
        Logger.d(TAG, "preview processorType=" + processorType);
        if (processorType == PROCESSOR_TYPE_IMAGE_PROCESSOR) {
            mPreviewOutputConfig = ImageReaderOutputConfig.create(
                    sLastOutputConfigId.getAndIncrement(),
                    mPreviewOutputSurface.getSize(),
                    ImageFormat.YUV_420_888,
                    PREVIEW_PROCESS_MAX_IMAGES);
            PreviewImageProcessorImpl previewImageProcessor =
                    (PreviewImageProcessorImpl) mPreviewExtenderImpl.getProcessor();
            mPreviewProcessor = new PreviewProcessor(
                    previewImageProcessor, mPreviewOutputSurface.getSurface(),
                    mPreviewOutputSurface.getSize());
        } else if (processorType == PROCESSOR_TYPE_REQUEST_UPDATE_ONLY) {
            mPreviewOutputConfig = SurfaceOutputConfig.create(
                    sLastOutputConfigId.getAndIncrement(),
                    mPreviewOutputSurface.getSurface());
            mRequestUpdateProcessor =
                    (RequestUpdateProcessorImpl) mPreviewExtenderImpl.getProcessor();
        } else {
            mPreviewOutputConfig = SurfaceOutputConfig.create(
                    sLastOutputConfigId.getAndIncrement(),
                    mPreviewOutputSurface.getSurface());
        }

        // Image Capture
        CaptureProcessorImpl captureProcessor = mImageCaptureExtenderImpl.getCaptureProcessor();
        Logger.d(TAG, "CaptureProcessor=" + captureProcessor);

        if (captureProcessor != null) {
            mCaptureOutputConfig = ImageReaderOutputConfig.create(
                    sLastOutputConfigId.getAndIncrement(),
                    mCaptureOutputSurface.getSize(),
                    ImageFormat.YUV_420_888,
                    mImageCaptureExtenderImpl.getMaxCaptureStage());
            mStillCaptureProcessor = new StillCaptureProcessor(
                    captureProcessor, mCaptureOutputSurface.getSurface(),
                    mCaptureOutputSurface.getSize(),
                    mPostviewOutputSurface,
                    /* needOverrideTimestamp */ !mWillReceiveOnCaptureCompleted);
        } else {
            mCaptureOutputConfig = SurfaceOutputConfig.create(
                    sLastOutputConfigId.getAndIncrement(),
                    mCaptureOutputSurface.getSurface());
        }

        // Image Analysis
        if (outputSurfaceConfiguration.getImageAnalysisOutputSurface() != null) {
            mAnalysisOutputConfig = SurfaceOutputConfig.create(
                    sLastOutputConfigId.getAndIncrement(),
                    outputSurfaceConfiguration.getImageAnalysisOutputSurface()
                            .getSurface());
        }

        Camera2SessionConfigBuilder builder =
                new Camera2SessionConfigBuilder()
                        .addOutputConfig(mPreviewOutputConfig)
                        .addOutputConfig(mCaptureOutputConfig)
                        .setSessionTemplateId(CameraDevice.TEMPLATE_PREVIEW);

        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            int previewSessionType = mPreviewExtenderImpl.onSessionType();
            int captureSessionType = mImageCaptureExtenderImpl.onSessionType();
            Preconditions.checkArgument(previewSessionType == captureSessionType,
                    "Needs same session type in both PreviewExtenderImpl and "
                            + "ImageCaptureExtenderImpl");
            if (previewSessionType == -1) { // -1 means using default value
                previewSessionType = SessionConfiguration.SESSION_REGULAR;
            }
            builder.setSessionType(previewSessionType);
        }

        if (mAnalysisOutputConfig != null) {
            builder.addOutputConfig(mAnalysisOutputConfig);
        }

        CaptureStageImpl captureStagePreview = mPreviewExtenderImpl.onPresetSession();
        Logger.d(TAG, "preview onPresetSession:" + captureStagePreview);

        CaptureStageImpl captureStageCapture = mImageCaptureExtenderImpl.onPresetSession();
        Logger.d(TAG, "capture onPresetSession:" + captureStageCapture);

        if (captureStagePreview != null && captureStagePreview.getParameters() != null) {
            for (Pair<CaptureRequest.Key, Object> parameter :
                    captureStagePreview.getParameters()) {
                builder.addSessionParameter(parameter.first, parameter.second);
            }
        }

        if (captureStageCapture != null && captureStageCapture.getParameters() != null) {
            for (Pair<CaptureRequest.Key, Object> parameter :
                    captureStageCapture.getParameters()) {
                builder.addSessionParameter(parameter.first, parameter.second);
            }
        }
        return builder.build();
    }

    @Override
    protected void deInitSessionInternal() {
        if (mPreviewProcessor != null) {
            mPreviewProcessor.close();
            mPreviewProcessor = null;
        }
        if (mStillCaptureProcessor != null) {
            mStillCaptureProcessor.close();
            mStillCaptureProcessor = null;
        }

        // Close the processor prior to OEMs's onDeinit in case OEMs block the thread for too
        // long and the processor is closed too late.
        Logger.d(TAG, "preview onDeInit");
        mPreviewExtenderImpl.onDeInit();
        Logger.d(TAG, "capture onDeInit");
        mImageCaptureExtenderImpl.onDeInit();
    }

    @Override
    public void setParameters(@NonNull Config config) {
        synchronized (mLock) {
            HashMap<CaptureRequest.Key<?>, Object> map = new HashMap<>();

            RequestOptionConfig options =
                    RequestOptionConfig.Builder.from(config).build();

            for (Config.Option<?> option : options.listOptions()) {
                @SuppressWarnings("unchecked")
                CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();
                map.put(key, options.retrieveOption(option));
            }
            mParameters.clear();
            mParameters.putAll(map);
        }
    }

    @Override
    public void onCaptureSessionStart(@NonNull RequestProcessor requestProcessor) {
        mRequestProcessor = requestProcessor;

        List<CaptureStageImpl> captureStages = new ArrayList<>();
        CaptureStageImpl captureStage1 = mPreviewExtenderImpl.onEnableSession();
        Logger.d(TAG, "preview onEnableSession: " + captureStage1);
        if (captureStage1 != null) {
            captureStages.add(captureStage1);
        }
        CaptureStageImpl captureStage2 = mImageCaptureExtenderImpl.onEnableSession();
        Logger.d(TAG, "capture onEnableSession:" + captureStage2);
        if (captureStage2 != null) {
            captureStages.add(captureStage2);
        }
        mOnEnableDisableSessionDurationCheck.onEnableSessionInvoked();

        if (!captureStages.isEmpty()) {
            submitRequestByCaptureStages(requestProcessor, captureStages);
        }

        if (mPreviewProcessor != null) {
            mPreviewProcessor.resume();
            setImageProcessor(mPreviewOutputConfig.getId(),
                    new ImageProcessor() {
                        @Override
                        public void onNextImageAvailable(int outputStreamId, long timestampNs,
                                @NonNull ImageReference imageReference,
                                @Nullable String physicalCameraId) {
                            if (mPreviewProcessor != null) {
                                mPreviewProcessor.notifyImage(imageReference);
                            } else {
                                imageReference.decrement();
                            }
                        }
                    });
        }
    }

    private void applyParameters(RequestBuilder builder) {
        synchronized (mLock) {
            for (CaptureRequest.Key<?> key : mParameters.keySet()) {
                Object value = mParameters.get(key);
                if (value != null) {
                    builder.setParameters(key, value);
                }
            }
        }
    }

    private void submitRequestByCaptureStages(RequestProcessor requestProcessor,
            List<CaptureStageImpl> captureStageList) {
        List<RequestProcessor.Request> requestList = new ArrayList<>();
        for (CaptureStageImpl captureStage : captureStageList) {
            RequestBuilder builder = new RequestBuilder();
            builder.addTargetOutputConfigIds(mPreviewOutputConfig.getId());
            if (mAnalysisOutputConfig != null) {
                builder.addTargetOutputConfigIds(mAnalysisOutputConfig.getId());
            }
            for (Pair<CaptureRequest.Key, Object> keyObjectPair : captureStage.getParameters()) {
                builder.setParameters(keyObjectPair.first, keyObjectPair.second);
            }
            builder.setTemplateId(CameraDevice.TEMPLATE_PREVIEW);
            requestList.add(builder.build());
        }
        requestProcessor.submit(requestList, new RequestProcessor.Callback() {
        });
    }

    @Override
    public void onCaptureSessionEnd() {
        mOnEnableDisableSessionDurationCheck.onDisableSessionInvoked();
        if (mPreviewProcessor != null) {
            mPreviewProcessor.pause();
        }
        List<CaptureStageImpl> captureStages = new ArrayList<>();
        CaptureStageImpl captureStage1 = mPreviewExtenderImpl.onDisableSession();
        Logger.d(TAG, "preview onDisableSession: " + captureStage1);
        if (captureStage1 != null) {
            captureStages.add(captureStage1);
        }
        CaptureStageImpl captureStage2 = mImageCaptureExtenderImpl.onDisableSession();
        Logger.d(TAG, "capture onDisableSession:" + captureStage2);
        if (captureStage2 != null) {
            captureStages.add(captureStage2);
        }

        if (!captureStages.isEmpty()) {
            submitRequestByCaptureStages(mRequestProcessor, captureStages);
        }
        mRequestProcessor = null;
        mIsCapturing = false;
    }

    Map<CaptureResult.Key, Object> getCaptureResultKeyMapFromList(
            List<Pair<CaptureResult.Key, Object>> list) {
        Map<CaptureResult.Key, Object> map = new HashMap<>();
        for (Pair<CaptureResult.Key, Object> pair : list) {
            map.put(pair.first, pair.second);
        }
        return map;
    }

    @Override
    public int startRepeating(@NonNull TagBundle tagBundle,
            @NonNull CaptureCallback captureCallback) {
        int repeatingCaptureSequenceId = mNextCaptureSequenceId.getAndIncrement();
        if (mRequestProcessor == null) {
            captureCallback.onCaptureFailed(repeatingCaptureSequenceId);
            captureCallback.onCaptureSequenceAborted(repeatingCaptureSequenceId);
        } else {
            if (mPreviewProcessor != null) {
                mPreviewProcessor.start((shutterTimestamp, result) -> {
                    captureCallback.onCaptureCompleted(shutterTimestamp,
                            repeatingCaptureSequenceId,
                            new KeyValueMapCameraCaptureResult(
                                    shutterTimestamp,
                                    tagBundle,
                                    getCaptureResultKeyMapFromList(result))
                    );
                });
            }
            updateRepeating(repeatingCaptureSequenceId, captureCallback);
        }

        return repeatingCaptureSequenceId;
    }

    void updateRepeating(int repeatingCaptureSequenceId, @NonNull CaptureCallback captureCallback) {
        if (mRequestProcessor == null) {
            Logger.d(TAG, "mRequestProcessor is null, ignore repeating request");
            return;
        }
        RequestBuilder builder = new RequestBuilder();
        builder.addTargetOutputConfigIds(mPreviewOutputConfig.getId());
        if (mAnalysisOutputConfig != null) {
            builder.addTargetOutputConfigIds(mAnalysisOutputConfig.getId());
        }
        builder.setTemplateId(CameraDevice.TEMPLATE_PREVIEW);
        applyParameters(builder);
        applyPreviewStagesParameters(builder);

        RequestProcessor.Callback callback = new RequestProcessor.Callback() {
            @Override
            public void onCaptureCompleted(@NonNull RequestProcessor.Request request,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                CaptureResult captureResult = cameraCaptureResult.getCaptureResult();
                Preconditions.checkArgument(captureResult instanceof TotalCaptureResult,
                        "Cannot get TotalCaptureResult from the cameraCaptureResult ");
                TotalCaptureResult totalCaptureResult = (TotalCaptureResult) captureResult;

                if (mPreviewProcessor != null) {
                    mPreviewProcessor.notifyCaptureResult(totalCaptureResult);
                } else {
                    if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_3)
                            && ExtensionVersion
                            .isMinimumCompatibleVersion(Version.VERSION_1_3)) {
                        Long timestamp = totalCaptureResult.get(CaptureResult.SENSOR_TIMESTAMP);
                        if (timestamp != null) {
                            captureCallback.onCaptureCompleted(timestamp,
                                    repeatingCaptureSequenceId,
                                    new Camera2CameraCaptureResult(totalCaptureResult));
                        }
                    }
                }

                if (mRequestUpdateProcessor != null) {
                    CaptureStageImpl captureStage =
                            mRequestUpdateProcessor.process(totalCaptureResult);

                    if (captureStage != null) {
                        updateRepeating(repeatingCaptureSequenceId, captureCallback);
                    }
                }

                captureCallback.onCaptureSequenceCompleted(repeatingCaptureSequenceId);
            }
        };

        Logger.d(TAG, "requestProcessor setRepeating");
        mRequestProcessor.setRepeating(builder.build(), callback);
    }

    private void applyPreviewStagesParameters(RequestBuilder builder) {
        CaptureStageImpl captureStage = mPreviewExtenderImpl.getCaptureStage();
        if (captureStage != null) {
            for (Pair<CaptureRequest.Key, Object> keyObjectPair :
                    captureStage.getParameters()) {
                builder.setParameters(keyObjectPair.first, keyObjectPair.second);
            }
        }
    }

    @Override
    public void stopRepeating() {
        mRequestProcessor.stopRepeating();
    }

    private long getRequestCompletedTimestamp(int captureSequenceId) {
        synchronized (mLock) {
            Long timestamp = mRequestCompletedTimestampMap.get(captureSequenceId);
            if (timestamp == null) {
                return INVALID_TIMESTAMP;
            }
            mRequestCompletedTimestampMap.remove(captureSequenceId);
            return timestamp;
        }
    }

    @Override
    public int startCapture(boolean postviewEnabled, @NonNull TagBundle tagBundle,
            @NonNull CaptureCallback captureCallback) {
        Logger.d(TAG, "startCapture postviewEnabled = " + postviewEnabled
                + " mWillReceiveOnCaptureCompleted = " + mWillReceiveOnCaptureCompleted);
        int captureSequenceId = mNextCaptureSequenceId.getAndIncrement();

        if (mRequestProcessor == null || mIsCapturing) {
            Logger.d(TAG, "startCapture failed");
            captureCallback.onCaptureFailed(captureSequenceId);
            captureCallback.onCaptureSequenceAborted(captureSequenceId);
            return captureSequenceId;
        }
        mIsCapturing = true;

        List<RequestProcessor.Request> requestList = new ArrayList<>();
        List<CaptureStageImpl> captureStages = mImageCaptureExtenderImpl.getCaptureStages();
        List<Integer> captureIdList = new ArrayList<>();

        for (CaptureStageImpl captureStage : captureStages) {
            RequestBuilder builder = new RequestBuilder();
            builder.addTargetOutputConfigIds(mCaptureOutputConfig.getId());
            builder.setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.setCaptureStageId(captureStage.getId());

            captureIdList.add(captureStage.getId());

            applyParameters(builder);
            applyPreviewStagesParameters(builder);

            for (Pair<CaptureRequest.Key, Object> keyObjectPair :
                    captureStage.getParameters()) {
                builder.setParameters(keyObjectPair.first, keyObjectPair.second);
            }
            requestList.add(builder.build());
        }

        Logger.d(TAG, "Wait for capture stage id: " + captureIdList);

        RequestProcessor.Callback callback = new RequestProcessor.Callback() {
            boolean mIsCaptureFailed = false;
            boolean mIsCaptureStarted = false;

            @Override
            public void onCaptureStarted(@NonNull RequestProcessor.Request request,
                    long frameNumber, long timestamp) {
                if (!mIsCaptureStarted) {
                    mIsCaptureStarted = true;
                    captureCallback.onCaptureStarted(captureSequenceId, timestamp);
                }
            }

            @Override
            public void onCaptureCompleted(@NonNull RequestProcessor.Request request,
                    @NonNull CameraCaptureResult cameraCaptureResult) {
                CaptureResult captureResult = cameraCaptureResult.getCaptureResult();
                Preconditions.checkArgument(captureResult instanceof TotalCaptureResult,
                        "Cannot get capture TotalCaptureResult from the cameraCaptureResult ");
                TotalCaptureResult totalCaptureResult = (TotalCaptureResult) captureResult;

                RequestBuilder.RequestProcessorRequest requestProcessorRequest =
                        (RequestBuilder.RequestProcessorRequest) request;

                if (mStillCaptureProcessor != null) {
                    synchronized (mLock) {
                        if (!mRequestCompletedTimestampMap.containsKey(captureSequenceId)) {
                            mRequestCompletedTimestampMap.put(
                                    captureSequenceId, cameraCaptureResult.getTimestamp());
                        }
                    }

                    mStillCaptureProcessor.notifyCaptureResult(
                            totalCaptureResult,
                            requestProcessorRequest.getCaptureStageId());
                } else {
                    // No CaptureProcessorImpl
                    mIsCapturing = false;
                    if (mRequestProcessor == null) {
                        // notify the onCaptureSequenceAborted callback if onCaptureCompleted
                        // happens but session is closed.
                        captureCallback.onCaptureSequenceAborted(captureSequenceId);
                        return;
                    }
                    captureCallback.onCaptureProcessStarted(captureSequenceId);
                    captureCallback.onCaptureCompleted(cameraCaptureResult.getTimestamp(),
                            captureSequenceId, new Camera2CameraCaptureResult(
                                    tagBundle, cameraCaptureResult.getCaptureResult()));
                    captureCallback.onCaptureSequenceCompleted(captureSequenceId);
                }
            }

            @Override
            public void onCaptureFailed(@NonNull RequestProcessor.Request request,
                    @NonNull CameraCaptureFailure captureFailure) {
                if (!mIsCaptureFailed) {
                    mIsCaptureFailed = true;
                    captureCallback.onCaptureFailed(captureSequenceId);
                    captureCallback.onCaptureSequenceAborted(captureSequenceId);
                    mIsCapturing = false;
                }
            }

            @Override
            public void onCaptureSequenceAborted(int sequenceId) {
                captureCallback.onCaptureSequenceAborted(captureSequenceId);
                mIsCapturing = false;
            }
        };

        Logger.d(TAG, "startCapture");
        if (mStillCaptureProcessor != null) {
            setImageProcessor(mCaptureOutputConfig.getId(),
                    new ImageProcessor() {
                        boolean mIsFirstFrame = true;

                        @Override
                        public void onNextImageAvailable(int outputStreamId, long timestampNs,
                                @NonNull ImageReference imageReference,
                                @Nullable String physicalCameraId) {
                            Logger.d(TAG, "onNextImageAvailable  outputStreamId=" + outputStreamId);
                            if (mStillCaptureProcessor != null) {
                                mStillCaptureProcessor.notifyImage(imageReference);
                            } else {
                                imageReference.decrement();
                            }

                            if (mIsFirstFrame) {
                                captureCallback.onCaptureProcessStarted(captureSequenceId);
                                mIsFirstFrame = false;
                            }
                        }
                    });
            mStillCaptureProcessor.startCapture(postviewEnabled, captureIdList,
                    new StillCaptureProcessor.OnCaptureResultCallback() {
                        @Override
                        public void onProcessCompleted() {
                            if (!mWillReceiveOnCaptureCompleted) {
                                // If ProcessResultImpl.onCaptureCompleted won't be invoked,
                                // We finish the capture sequence using the timestamp retrieved at
                                // onCaptureStarted when the process() completed.
                                long timestamp = getRequestCompletedTimestamp(captureSequenceId);
                                if (timestamp == INVALID_TIMESTAMP) {
                                    Logger.e(TAG, "Cannot get timestamp for the capture result");
                                    captureCallback.onCaptureFailed(captureSequenceId);
                                    captureCallback.onCaptureSequenceAborted(captureSequenceId);
                                    mIsCapturing = false;
                                    return;
                                }
                                captureCallback.onCaptureCompleted(timestamp,
                                        captureSequenceId,
                                        new KeyValueMapCameraCaptureResult(
                                                timestamp,
                                                tagBundle,
                                                Collections.emptyMap()));
                                captureCallback.onCaptureSequenceCompleted(captureSequenceId);
                            }
                            mIsCapturing = false;
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            captureCallback.onCaptureFailed(captureSequenceId);
                            mIsCapturing = false;
                        }

                        @Override
                        public void onCaptureCompleted(long shutterTimestamp,
                                @NonNull List<Pair<CaptureResult.Key, Object>> result) {
                            if (mWillReceiveOnCaptureCompleted) {
                                captureCallback.onCaptureCompleted(shutterTimestamp,
                                        captureSequenceId,
                                        new KeyValueMapCameraCaptureResult(
                                                shutterTimestamp, tagBundle,
                                                getCaptureResultKeyMapFromList(result)));
                                captureCallback.onCaptureSequenceCompleted(
                                        captureSequenceId);
                            }
                        }

                        @Override
                        public void onCaptureProcessProgressed(int progress) {
                            captureCallback.onCaptureProcessProgressed(progress);
                        }
                    });
        }

        mRequestProcessor.submit(requestList, callback);
        return captureSequenceId;
    }

    @Override
    public void abortCapture(int captureSequenceId) {
        mRequestProcessor.abortCaptures();
    }

    @Override
    public int startTrigger(@NonNull Config config, @NonNull TagBundle tagBundle,
            @NonNull CaptureCallback callback) {
        Logger.d(TAG, "startTrigger");
        int captureSequenceId = mNextCaptureSequenceId.getAndIncrement();
        RequestBuilder builder = new RequestBuilder();
        builder.addTargetOutputConfigIds(mPreviewOutputConfig.getId());
        if (mAnalysisOutputConfig != null) {
            builder.addTargetOutputConfigIds(mAnalysisOutputConfig.getId());
        }
        builder.setTemplateId(CameraDevice.TEMPLATE_PREVIEW);
        applyParameters(builder);
        applyPreviewStagesParameters(builder);

        RequestOptionConfig options =
                RequestOptionConfig.Builder.from(config).build();
        for (Config.Option<?> option : options.listOptions()) {
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();
            builder.setParameters(key, options.retrieveOption(option));
        }

        mRequestProcessor.submit(builder.build(), new RequestProcessor.Callback() {
            @Override
            public void onCaptureCompleted(@NonNull RequestProcessor.Request request,
                    @NonNull CameraCaptureResult captureResult) {
                callback.onCaptureCompleted(captureResult.getTimestamp(), captureSequenceId,
                        new Camera2CameraCaptureResult(tagBundle,
                                captureResult.getCaptureResult()));
                callback.onCaptureSequenceCompleted(captureSequenceId);
            }

            @Override
            public void onCaptureFailed(@NonNull RequestProcessor.Request request,
                    @NonNull CameraCaptureFailure captureFailure) {
                callback.onCaptureFailed(captureSequenceId);
            }
        });

        return captureSequenceId;
    }

    @Nullable
    @Override
    public Pair<Long, Long> getRealtimeCaptureLatency() {
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            return mImageCaptureExtenderImpl.getRealtimeCaptureLatency();
        }
        return null;
    }

    @NonNull
    @Override
    public Map<Integer, List<Size>> getSupportedPostviewSize(@NonNull Size captureSize) {
        return mVendorExtender.getSupportedPostviewResolutions(captureSize);
    }
}
