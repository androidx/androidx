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
package androidx.camera.extensions.impl;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation for HDR image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@SuppressLint("UnknownNullness")
public final class HdrImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final String TAG = "HdrImageCaptureExtender";
    private static final int UNDER_STAGE_ID = 0;
    private static final int NORMAL_STAGE_ID = 1;
    private static final int OVER_STAGE_ID = 2;
    private static final int SESSION_STAGE_ID = 101;
    private static final long UNDER_EXPOSURE_TIME = TimeUnit.MILLISECONDS.toNanos(8);
    private static final long NORMAL_EXPOSURE_TIME = TimeUnit.MILLISECONDS.toNanos(16);
    private static final long OVER_EXPOSURE_TIME = TimeUnit.MILLISECONDS.toNanos(32);
    private HdrImageCaptureExtenderCaptureProcessorImpl mCaptureProcessor = null;

    public HdrImageCaptureExtenderImpl() {
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        // Return false to skip tests since old devices do not support extensions.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }

        if (cameraCharacteristics == null) {
            return false;
        }

        Range<Long> exposureTimeRange =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);

        if (exposureTimeRange == null || !exposureTimeRange.contains(
                Range.create(UNDER_EXPOSURE_TIME, OVER_EXPOSURE_TIME))) {
            return false;
        }

        // The device needs to support CONTROL_AE_MODE_OFF for the testing CaptureStages
        int[] availableAeModes =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);

        if (availableAeModes != null) {
            for (int mode : availableAeModes) {
                if (mode == CONTROL_AE_MODE_OFF) {
                    return true;
                }
            }
        }

        return false;
    }

    @NonNull
    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        // Under exposed capture stage
        SettableCaptureStage captureStageUnder = new SettableCaptureStage(UNDER_STAGE_ID);
        // Turn off AE so that ISO sensitivity can be controlled
        captureStageUnder.addCaptureRequestParameters(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);
        captureStageUnder.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                UNDER_EXPOSURE_TIME);

        // Normal exposed capture stage
        SettableCaptureStage captureStageNormal = new SettableCaptureStage(NORMAL_STAGE_ID);
        captureStageNormal.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                NORMAL_EXPOSURE_TIME);

        // Over exposed capture stage
        SettableCaptureStage captureStageOver = new SettableCaptureStage(OVER_STAGE_ID);
        captureStageOver.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                OVER_EXPOSURE_TIME);

        List<CaptureStageImpl> captureStages = new ArrayList<>();
        captureStages.add(captureStageUnder);
        captureStages.add(captureStageNormal);
        captureStages.add(captureStageOver);
        return captureStages;
    }


    @Nullable
    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCaptureProcessor = new HdrImageCaptureExtenderCaptureProcessorImpl();
            return mCaptureProcessor;
        } else {
            return new NoOpCaptureProcessorImpl();
        }
    }

    @Override
    public void onInit(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Context context) {
    }

    @Override
    public void onDeInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mCaptureProcessor != null) {
            mCaptureProcessor.release();
        }
    }

    @Nullable
    @Override
    public CaptureStageImpl onPresetSession() {
        // The CaptureRequest parameters will be set via SessionConfiguration#setSessionParameters
        // (CaptureRequest) which only supported from API level 28.
        if (Build.VERSION.SDK_INT < 28) {
            return null;
        }

        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onEnableSession() {
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onDisableSession() {
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Override
    public int getMaxCaptureStage() {
        return 4;
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return null;
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size captureOutputSize) {
        return new Range<>(300L, 1000L);
    }

    @RequiresApi(23)
    static final class HdrImageCaptureExtenderCaptureProcessorImpl implements CaptureProcessorImpl {
        private ImageWriter mImageWriter;
        @Override
        public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
            mImageWriter = ImageWriter.newInstance(surface, 2);
        }

        @Override
        public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results) {
            process(results, null, null);
        }

        @Override
        public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @Nullable ProcessResultImpl resultCallback, @Nullable Executor executor) {
            Log.d(TAG, "Started HDR CaptureProcessor");

            Executor executorForCallback = executor != null ? executor : (cmd) -> cmd.run();

            // Check for availability of all requested images
            if (!results.containsKey(UNDER_STAGE_ID)) {
                Log.w(TAG,
                        "Unable to process since images does not contain "
                                + "underexposed image.");
                return;
            }

            if (!results.containsKey(NORMAL_STAGE_ID)) {
                Log.w(TAG,
                        "Unable to process since images does not contain normal "
                                + "exposed image.");
                return;
            }

            if (!results.containsKey(OVER_STAGE_ID)) {
                Log.w(TAG,
                        "Unable to process since images does not contain "
                                + "overexposed image.");
                return;
            }

            // Do processing of images, our placeholder logic just copies the first
            // Image into the output buffer.
            List<Pair<Image, TotalCaptureResult>> imageDataPairs = new ArrayList<>(
                    results.values());
            Image outputImage = mImageWriter.dequeueInputImage();

            // Do processing here
            // The sample here simply returns the normal image result
            Image normalImage = imageDataPairs.get(NORMAL_STAGE_ID).first;
            if (outputImage.getWidth() != normalImage.getWidth()
                    || outputImage.getHeight() != normalImage.getHeight()) {
                throw new IllegalStateException(String.format("input image "
                                + "resolution [%d, %d] not the same as the "
                                + "output image[%d, %d]", normalImage.getWidth(),
                        normalImage.getHeight(), outputImage.getWidth(),
                        outputImage.getHeight()));
            }

            try {
                // copy y plane
                Image.Plane inYPlane = normalImage.getPlanes()[0];
                Image.Plane outYPlane = outputImage.getPlanes()[0];
                ByteBuffer inYBuffer = inYPlane.getBuffer();
                ByteBuffer outYBuffer = outYPlane.getBuffer();
                int inYPixelStride = inYPlane.getPixelStride();
                int inYRowStride = inYPlane.getRowStride();
                int outYPixelStride = outYPlane.getPixelStride();
                int outYRowStride = outYPlane.getRowStride();
                for (int x = 0; x < outputImage.getHeight(); x++) {
                    for (int y = 0; y < outputImage.getWidth(); y++) {
                        int inIndex = x * inYRowStride + y * inYPixelStride;
                        int outIndex = x * outYRowStride + y * outYPixelStride;
                        outYBuffer.put(outIndex, inYBuffer.get(inIndex));
                    }
                }
                // Copy UV
                for (int i = 1; i < 3; i++) {
                    Image.Plane inPlane = normalImage.getPlanes()[i];
                    Image.Plane outPlane = outputImage.getPlanes()[i];
                    ByteBuffer inBuffer = inPlane.getBuffer();
                    ByteBuffer outBuffer = outPlane.getBuffer();
                    int inPixelStride = inPlane.getPixelStride();
                    int inRowStride = inPlane.getRowStride();
                    int outPixelStride = outPlane.getPixelStride();
                    int outRowStride = outPlane.getRowStride();
                    // UV are half width compared to Y
                    for (int x = 0; x < outputImage.getHeight() / 2; x++) {
                        for (int y = 0; y < outputImage.getWidth() / 2; y++) {
                            int inIndex = x * inRowStride + y * inPixelStride;
                            int outIndex = x * outRowStride + y * outPixelStride;
                            byte b = inBuffer.get(inIndex);
                            outBuffer.put(outIndex, b);
                        }
                    }
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error accessing the Image: " + e);
                // Since something went wrong, don't try to queue up the image.
                // Instead let the Image writing get dropped.
                return;
            }

            mImageWriter.queueInputImage(outputImage);

            TotalCaptureResult captureResult = results.get(NORMAL_STAGE_ID).second;

            if (resultCallback != null) {
                executorForCallback.execute(
                        () -> resultCallback.onCaptureCompleted(
                                captureResult.get(CaptureResult.SENSOR_TIMESTAMP),
                                getFilteredResults(captureResult)));
            }

            Log.d(TAG, "Completed HDR CaptureProcessor");
        }

        @SuppressWarnings("unchecked")
        private List<Pair<CaptureResult.Key, Object>> getFilteredResults(
                TotalCaptureResult captureResult) {
            List<Pair<CaptureResult.Key, Object>> list = new ArrayList<>();
            for (CaptureResult.Key key : captureResult.getKeys()) {
                list.add(new Pair<>(key, captureResult.get(key)));
            }

            return list;
        }


        @Override
        public void onResolutionUpdate(@NonNull Size size) {

        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {

        }

        @Override
        public void onPostviewOutputSurface(@NonNull Surface surface) {
        }

        @Override
        public void onResolutionUpdate(@NonNull Size size, @NonNull Size postviewSize) {
            onResolutionUpdate(size);
        }

        @Override
        public void processWithPostview(
                @NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor) {
            Log.d(TAG, "processWithPostview");
            process(results, resultCallback, executor);
        }

        public void release() {
            if (mImageWriter != null) {
                mImageWriter.close();
            }
        }
    }

    @NonNull
    @Override
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys() {
        return null;
    }

    @Override
    public int onSessionType() {
        return SessionConfiguration.SESSION_REGULAR;
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedPostviewResolutions(@NonNull Size captureSize) {
        return Collections.emptyList();
    }

    @Override
    public boolean isCaptureProcessProgressAvailable() {
        return false;
    }

    @Nullable
    @Override
    public Pair<Long, Long> getRealtimeCaptureLatency() {
        return null;
    }

    @Override
    public boolean isPostviewAvailable() {
        return false;
    }

    @NonNull
    @Override
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
        return null;
    }
}
