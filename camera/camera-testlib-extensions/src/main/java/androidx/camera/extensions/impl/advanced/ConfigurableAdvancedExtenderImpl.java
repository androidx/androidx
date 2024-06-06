/*
 * Copyright 2023 The Android Open Source Project
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
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProcessingUtil;
import androidx.camera.core.impl.utils.AspectRatioUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link AdvancedExtenderImpl} implementation that can be configured to have long processing
 * time for capture and is capable of outputting the postview and the process progress event.
 */
public class ConfigurableAdvancedExtenderImpl implements AdvancedExtenderImpl {
    private static final int EV_INDEX = 10;
    protected static final int POSTVIEW_NOT_SUPPORTED = -1;
    private CameraCharacteristics mCameraCharacteristics;
    private final int mPostviewFormat;
    private final boolean mLongDurationCapture;
    private final boolean mInvokeOnCaptureComplete;

    public ConfigurableAdvancedExtenderImpl(
            boolean longDurationCapture, int postviewFormat, boolean invokeOnCaptureComplete) {
        mLongDurationCapture = longDurationCapture;
        mPostviewFormat = postviewFormat;
        mInvokeOnCaptureComplete = invokeOnCaptureComplete;
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        return true;
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        mCameraCharacteristics = characteristicsMap.get(cameraId);
    }

    @Override
    @Nullable
    public Range<Long> getEstimatedCaptureLatencyRange(
            @NonNull String cameraId, @Nullable Size size, int imageFormat) {
        return mLongDurationCapture ? new Range<>(2000L, 3000L) : new Range<>(500L, 800L);
    }

    @Override
    @NonNull
    public Map<Integer, List<Size>> getSupportedPreviewOutputResolutions(
            @NonNull String cameraId) {
        HashMap<Integer, List<Size>> map = new HashMap<>();
        map.put(ImageFormat.PRIVATE, getOutputSizes(ImageFormat.PRIVATE));
        return map;
    }

    private List<Size> getOutputSizes(int imageFormat) {
        StreamConfigurationMap map = mCameraCharacteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        return Arrays.asList(map.getOutputSizes(imageFormat));
    }

    @Override
    @NonNull
    public Map<Integer, List<Size>> getSupportedCaptureOutputResolutions(
            @NonNull String cameraId) {
        HashMap<Integer, List<Size>> map = new HashMap<>();
        map.put(ImageFormat.JPEG, getOutputSizes(ImageFormat.JPEG));
        return map;
    }

    @Override
    @NonNull
    public Map<Integer, List<Size>> getSupportedPostviewResolutions(
            @NonNull Size captureSize) {
        HashMap<Integer, List<Size>> map = new HashMap<>();
        // Here it intentionally contains JPEG or YUV instead of both so that we can test
        // the different path.
        if (mPostviewFormat == ImageFormat.YUV_420_888) {
            map.put(ImageFormat.YUV_420_888, getCompatibleYuvSizes(captureSize));
        } else if (mPostviewFormat == ImageFormat.JPEG) {
            // it will configure YUV stream and convert it to JPEG.
            map.put(ImageFormat.JPEG, getCompatibleYuvSizes(captureSize));
        }
        return map;
    }

    private List<Size> getCompatibleYuvSizes(Size captureSize) {
        List<Size> yuvSizes = getOutputSizes(ImageFormat.YUV_420_888);
        List<Size> results = new ArrayList<>();

        for (Size yuvSize : yuvSizes) {
            int area = yuvSize.getWidth() * yuvSize.getHeight();
            if (area <= captureSize.getWidth() * captureSize.getHeight()
                    && area <= 1920 * 1080 /* 1080P */ && AspectRatioUtil.hasMatchingAspectRatio(
                    captureSize, new Rational(yuvSize.getWidth(), yuvSize.getHeight()))) {
                results.add(yuvSize);
            }
        }
        return results;
    }

    @Override
    @Nullable
    public List<Size> getSupportedYuvAnalysisResolutions(
            @NonNull String cameraId) {
        return Collections.emptyList();
    }

    private LongCaptureSessionProcessor mNightSessionProcessor = new LongCaptureSessionProcessor();
    @Override
    @NonNull
    public SessionProcessorImpl createSessionProcessor() {
        return mNightSessionProcessor;
    }

    @Override
    @NonNull
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys() {
        List<CaptureRequest.Key> keys = new ArrayList<>(Arrays.asList(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_REGIONS,
                CaptureRequest.CONTROL_AE_REGIONS)
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keys.add(CaptureRequest.CONTROL_ZOOM_RATIO);
        } else {
            keys.add(CaptureRequest.SCALER_CROP_REGION);
        }
        return Collections.unmodifiableList(keys);
    }

    @Override
    @NonNull
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
        if (!mInvokeOnCaptureComplete) {
            return Collections.emptyList();
        }

        List<CaptureResult.Key> keys = new ArrayList<>(Arrays.asList(
                CaptureResult.CONTROL_AF_MODE,
                CaptureResult.CONTROL_AE_REGIONS,
                CaptureResult.CONTROL_AF_REGIONS,
                CaptureResult.CONTROL_AE_STATE,
                CaptureResult.CONTROL_AF_STATE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keys.add(CaptureResult.CONTROL_ZOOM_RATIO);
        } else {
            keys.add(CaptureResult.SCALER_CROP_REGION);
        }
        return Collections.unmodifiableList(keys);
    }

    @Override
    public boolean isCaptureProcessProgressAvailable() {
        return true;
    }

    @Override
    public boolean isPostviewAvailable() {
        return mPostviewFormat != POSTVIEW_NOT_SUPPORTED;
    }

    private class LongCaptureSessionProcessor implements SessionProcessorImpl {
        private HandlerThread mHandlerThread;
        private Handler mBackgroundHandler;

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private Map<CaptureRequest.Key<?>, Object> mParameters = new LinkedHashMap<>();

        private Camera2OutputConfigImpl mPreviewOutputConfig;
        private Camera2OutputConfigImpl mCaptureOutputConfig;
        private Camera2OutputConfigImpl mPostviewOutputConfig;

        private Surface mPostviewJpegOutputSurface;

        private RequestProcessorImpl mRequestProcessor;
        private AtomicInteger mNextCaptureSequenceId = new AtomicInteger(1);

        @NonNull
        @Override
        public Camera2SessionConfigImpl initSession(@NonNull String cameraId,
                @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
                @NonNull Context context, @NonNull OutputSurfaceConfigurationImpl surfaceConfigs) {
            return initSessionInternal(
                    surfaceConfigs.getPreviewOutputSurface(),
                    surfaceConfigs.getImageCaptureOutputSurface(),
                    surfaceConfigs.getPostviewOutputSurface());
        }

        @NonNull
        @Override
        public Camera2SessionConfigImpl initSession(@NonNull String cameraId,
                @NonNull Map<String, CameraCharacteristics> cameraCharacteristicsMap,
                @NonNull Context context, @NonNull OutputSurfaceImpl previewSurfaceConfig,
                @NonNull OutputSurfaceImpl imageCaptureSurfaceConfig,
                @Nullable OutputSurfaceImpl imageAnalysisSurfaceConfig) {
            return initSessionInternal(previewSurfaceConfig,
                    imageCaptureSurfaceConfig,
                    null);
        }

        private Camera2SessionConfigImpl initSessionInternal(
                @NonNull OutputSurfaceImpl previewSurfaceConfig,
                @NonNull OutputSurfaceImpl imageCaptureSurfaceConfig,
                @Nullable OutputSurfaceImpl postviewSurfaceConfig) {

            mHandlerThread = new HandlerThread("LongCapture advanced extender impl");
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            Camera2SessionConfigImplBuilder builder =
                    new Camera2SessionConfigImplBuilder()
                            .setSessionTemplateId(CameraDevice.TEMPLATE_PREVIEW);

            // Preview
            if (previewSurfaceConfig.getSurface() != null) {
                mPreviewOutputConfig = Camera2OutputConfigImplBuilder
                        .newSurfaceConfig(previewSurfaceConfig.getSurface()).build();

                builder.addOutputConfig(mPreviewOutputConfig);
            }

            // Image Capture
            if (imageCaptureSurfaceConfig.getSurface() != null) {
                mCaptureOutputConfig = Camera2OutputConfigImplBuilder.newSurfaceConfig(
                        imageCaptureSurfaceConfig.getSurface()).build();
                builder.addOutputConfig(mCaptureOutputConfig);
            }

            // postview
            if (postviewSurfaceConfig != null
                    && postviewSurfaceConfig.getImageFormat() != POSTVIEW_NOT_SUPPORTED) {
                if (postviewSurfaceConfig.getImageFormat() == ImageFormat.YUV_420_888) {
                    // For YUV format postview, it just configures the YUV surface into the camera.
                    mPostviewOutputConfig = Camera2OutputConfigImplBuilder.newSurfaceConfig(
                            postviewSurfaceConfig.getSurface()).build();
                } else if (postviewSurfaceConfig.getImageFormat() == ImageFormat.JPEG) {
                    // For JPEG format postview, because we can't configure two JPEG streams on
                    // most devices, alternatively, we configure a YUV stream and convert it to
                    // JPEG to the postview output surface.
                    mPostviewOutputConfig = Camera2OutputConfigImplBuilder.newImageReaderConfig(
                            postviewSurfaceConfig.getSize(), ImageFormat.YUV_420_888, 2
                    ).build();
                    mPostviewJpegOutputSurface = postviewSurfaceConfig.getSurface();
                }

                builder.addOutputConfig(mPostviewOutputConfig);
            }

            return builder.build();
        }

        @Override
        public void deInitSession() {
            mHandlerThread.quitSafely();
        }

        @Override
        public void setParameters(@NonNull Map<CaptureRequest.Key<?>, Object> parameters) {
            synchronized (mLock) {
                for (CaptureRequest.Key<?> key : parameters.keySet()) {
                    Object value = parameters.get(key);
                    if (value != null) {
                        mParameters.put(key, value);
                    }
                }
            }
        }

        private void applyParameters(@NonNull RequestBuilder requestBuilder) {
            synchronized (mLock) {
                for (CaptureRequest.Key<?> key : mParameters.keySet()) {
                    requestBuilder.setParameters(key, mParameters.get(key));
                }
            }
        }

        @Override
        public int startTrigger(@NonNull Map<CaptureRequest.Key<?>, Object> triggers,
                @NonNull CaptureCallback captureCallback) {
            RequestBuilder builder = new RequestBuilder(mPreviewOutputConfig.getId(),
                    CameraDevice.TEMPLATE_PREVIEW);
            applyParameters(builder);
            builder.setParameters(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, EV_INDEX);

            List<CaptureRequest.Key> availableKeys = getAvailableCaptureRequestKeys();
            for (CaptureRequest.Key<?> key : triggers.keySet()) {
                if (availableKeys.contains(key)) {
                    builder.setParameters(key, triggers.get(key));
                }
            }

            int seqId = mNextCaptureSequenceId.getAndIncrement();

            RequestProcessorImpl.Callback callback = new RequestProcessorImpl.Callback() {
                @Override
                public void onCaptureStarted(@NonNull RequestProcessorImpl.Request request,
                        long frameNumber,
                        long timestamp) {
                    captureCallback.onCaptureStarted(seqId, timestamp);
                }

                @Override
                public void onCaptureProgressed(@NonNull RequestProcessorImpl.Request request,
                        @NonNull CaptureResult partialResult) {

                }

                @Override
                public void onCaptureCompleted(@NonNull RequestProcessorImpl.Request request,
                        @NonNull TotalCaptureResult totalCaptureResult) {
                    captureCallback.onCaptureProcessStarted(seqId);
                }

                @Override
                public void onCaptureFailed(@NonNull RequestProcessorImpl.Request request,
                        @NonNull CaptureFailure captureFailure) {
                    captureCallback.onCaptureFailed(seqId);
                }

                @Override
                public void onCaptureBufferLost(@NonNull RequestProcessorImpl.Request request,
                        long frameNumber, int outputStreamId) {
                    captureCallback.onCaptureFailed(seqId);
                }

                @Override
                public void onCaptureSequenceCompleted(int sequenceId, long frameNumber) {
                    captureCallback.onCaptureSequenceCompleted(seqId);
                }

                @Override
                public void onCaptureSequenceAborted(int sequenceId) {
                    captureCallback.onCaptureSequenceAborted(seqId);
                }
            };

            mRequestProcessor.submit(builder.build(), callback);
            return seqId;
        }

        private int getJpegOrientation() {
            synchronized (mLock) {
                if (mParameters.get(CaptureRequest.JPEG_ORIENTATION) != null) {
                    return (int) mParameters.get(CaptureRequest.JPEG_ORIENTATION);
                }
            }
            return 0;
        }

        @Override
        public void onCaptureSessionStart(@NonNull RequestProcessorImpl requestProcessor) {
            mRequestProcessor = requestProcessor;

            if (mPostviewFormat == ImageFormat.JPEG && mPostviewJpegOutputSurface != null) {
                requestProcessor.setImageProcessor(mPostviewOutputConfig.getId(),
                        new ImageProcessorImpl() {
                            @Override
                            public void onNextImageAvailable(int outputConfigId, long timestampNs,
                                    ImageReferenceImpl imageReference, String physicalCameraId) {
                                ImageProcessingUtil.convertYuvToJpegBytesIntoSurface(
                                        imageReference.get(),
                                        90,
                                        getJpegOrientation(),
                                        mPostviewJpegOutputSurface
                                );

                                imageReference.decrement();
                            }
                        }
                );
            }
        }

        @Override
        public void onCaptureSessionEnd() {

        }

        @Override
        public int startRepeating(@NonNull CaptureCallback captureCallback) {
            RequestBuilder builder = new RequestBuilder(mPreviewOutputConfig.getId(),
                    CameraDevice.TEMPLATE_PREVIEW);
            applyParameters(builder);
            builder.setParameters(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, EV_INDEX);
            final int seqId = mNextCaptureSequenceId.getAndIncrement();

            RequestProcessorImpl.Callback callback = new RequestProcessorImpl.Callback() {
                @Override
                public void onCaptureStarted(@NonNull RequestProcessorImpl.Request request,
                        long frameNumber,
                        long timestamp) {
                    captureCallback.onCaptureStarted(seqId, timestamp);
                }

                @Override
                public void onCaptureProgressed(@NonNull RequestProcessorImpl.Request request,
                        @NonNull CaptureResult partialResult) {

                }

                @Override
                public void onCaptureCompleted(@NonNull RequestProcessorImpl.Request request,
                        @NonNull TotalCaptureResult totalCaptureResult) {
                    captureCallback.onCaptureProcessStarted(seqId);
                }

                @Override
                public void onCaptureFailed(@NonNull RequestProcessorImpl.Request request,
                        @NonNull CaptureFailure captureFailure) {
                    captureCallback.onCaptureFailed(seqId);
                }

                @Override
                public void onCaptureBufferLost(@NonNull RequestProcessorImpl.Request request,
                        long frameNumber, int outputStreamId) {
                    captureCallback.onCaptureFailed(seqId);
                }

                @Override
                public void onCaptureSequenceCompleted(int sequenceId, long frameNumber) {
                    captureCallback.onCaptureSequenceCompleted(seqId);
                }

                @Override
                public void onCaptureSequenceAborted(int sequenceId) {
                    captureCallback.onCaptureSequenceAborted(seqId);
                }
            };

            mRequestProcessor.setRepeating(builder.build(), callback);

            return seqId;
        }

        @Override
        public void stopRepeating() {
            mRequestProcessor.stopRepeating();
        }

        @Override
        public int startCapture(@NonNull CaptureCallback callback) {
            return startCaptureInternal(false, callback);
        }

        @Override
        public int startCaptureWithPostview(@NonNull CaptureCallback callback) {
            return startCaptureInternal(true, callback);
        }

        private int startCaptureInternal(boolean enablePostivew,
                @NonNull CaptureCallback captureCallback) {

            // Send postview request
            if (enablePostivew && mPostviewFormat != POSTVIEW_NOT_SUPPORTED) {
                RequestBuilder builderPostview = new RequestBuilder(mPostviewOutputConfig.getId(),
                        CameraDevice.TEMPLATE_PREVIEW);
                applyParameters(builderPostview);
                RequestProcessorImpl.Request postviewRequest = builderPostview.build();

                mRequestProcessor.submit(postviewRequest, new DefaultRequestProcessorImpl());
            }

            captureCallback.onCaptureProcessProgressed(10);

            // send still capture request
            final int seqId = mNextCaptureSequenceId.getAndIncrement();
            if (mLongDurationCapture) {
                updateProcessProgressDelayed(captureCallback, 40, 1000);
                updateProcessProgressDelayed(captureCallback, 70, 1600);
                mBackgroundHandler.postDelayed(() -> {
                    captureCallback.onCaptureProcessProgressed(100);
                    submitStillCapture(seqId, captureCallback);
                }, 2000);
            } else {
                mBackgroundHandler.postDelayed(() -> {
                    captureCallback.onCaptureProcessProgressed(100);
                    submitStillCapture(seqId, captureCallback);
                }, 600);
            }
            return seqId;
        }

        private void updateProcessProgressDelayed(CaptureCallback callback,
                int progress, long delayInMs) {
            mBackgroundHandler.postDelayed(() -> {
                callback.onCaptureProcessProgressed(progress);
            }, delayInMs);
        }

        private int submitStillCapture(int seqId, @NonNull CaptureCallback captureCallback) {
            if (mRequestProcessor == null) {
                return -1;
            }
            RequestBuilder builderStillCapture = new RequestBuilder(mCaptureOutputConfig.getId(),
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            applyParameters(builderStillCapture);
            RequestProcessorImpl.Request stillcaptureRequest = builderStillCapture.build();

            RequestProcessorImpl.Callback callback = new RequestProcessorImpl.Callback() {
                private boolean mOnCaptureStartedInvokded = false;

                @Override
                public void onCaptureStarted(@NonNull RequestProcessorImpl.Request request,
                        long frameNumber, long timestamp) {
                    if (!mOnCaptureStartedInvokded) {
                        mOnCaptureStartedInvokded = true;
                        captureCallback.onCaptureStarted(seqId, timestamp);
                    }
                }

                @Override
                public void onCaptureProgressed(@NonNull RequestProcessorImpl.Request request,
                        @NonNull CaptureResult partialResult) {

                }

                @Override
                public void onCaptureCompleted(@NonNull RequestProcessorImpl.Request request,
                        @NonNull TotalCaptureResult totalCaptureResult) {
                    if (mInvokeOnCaptureComplete) {
                        captureCallback.onCaptureCompleted(
                                totalCaptureResult.get(CaptureResult.SENSOR_TIMESTAMP),
                                seqId, Collections.emptyMap());
                    }

                    captureCallback.onCaptureSequenceCompleted(seqId);
                }

                @Override
                public void onCaptureFailed(@NonNull RequestProcessorImpl.Request request,
                        @NonNull CaptureFailure captureFailure) {
                    captureCallback.onCaptureFailed(seqId);
                }

                @Override
                public void onCaptureBufferLost(@NonNull RequestProcessorImpl.Request request,
                        long frameNumber, int outputStreamId) {
                    captureCallback.onCaptureFailed(seqId);
                }

                @Override
                public void onCaptureSequenceCompleted(int sequenceId, long frameNumber) {
                }

                @Override
                public void onCaptureSequenceAborted(int sequenceId) {
                    captureCallback.onCaptureSequenceAborted(seqId);
                }
            };

            mRequestProcessor.submit(stillcaptureRequest, callback);

            return seqId;
        }

        @Override
        public void abortCapture(int captureSequenceId) {

        }

        @Nullable
        @Override
        public Pair<Long, Long> getRealtimeCaptureLatency() {
            return mLongDurationCapture ? new Pair<>(500L, 2000L) : new Pair<>(500L, 0L);
        }
    }
}

