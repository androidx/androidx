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

import android.content.Context;
import android.graphics.ImageFormat;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implementation for night image capture use case.
 *
 * <p>This implementation enable the Extensions-Interface v1.4 features such as postview,
 * onCaptureProcessProgressed callback and realtime capture latency.
 *
 * <p>This is only for testing camera-extensions and should not be used as a sample OEM
 * implementation.
 *
 * @since 1.0
 */
public final class NightImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final String TAG = "NightICExtender";
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;
    private static final int EV_INDEX = 2;

    private static final int CAPTURE_STAGET_COUNT = 6;

    public NightImageCaptureExtenderImpl() {
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // ImageWriter needs API 23.
            return false;
        }
        Range<Integer> compensationRange =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);

        return compensationRange != null && compensationRange.contains(EV_INDEX);
    }

    @NonNull
    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        // Placeholder set of CaptureRequest.Key values
        List<CaptureStageImpl> captureStages = new ArrayList<>();
        for (int i = 0; i < CAPTURE_STAGET_COUNT; i++) {
            SettableCaptureStage captureStage = new SettableCaptureStage(DEFAULT_STAGE_ID + i);
            captureStage.addCaptureRequestParameters(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, EV_INDEX);
            captureStages.add(captureStage);
        }

        return captureStages;
    }

    private NightCaptureProcessorImpl mCaptureProcessor = null;

    @Nullable
    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        if (mCaptureProcessor == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Needs ImageWriter
                mCaptureProcessor = new NightCaptureProcessorImpl();
            }
        }
        return mCaptureProcessor;
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
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onEnableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onDisableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Override
    public int getMaxCaptureStage() {
        return CAPTURE_STAGET_COUNT + 1;
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return null;
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedPostviewResolutions(@Nullable Size captureSize) {
        Pair<Integer, Size[]> pair = new Pair<>(ImageFormat.YUV_420_888, new Size[]{captureSize});
        return Arrays.asList(pair);
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size captureOutputSize) {
        return new Range<>(2600L, 3000L);
    }

    @NonNull
    @Override
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys() {
        List<CaptureRequest.Key> keys = new ArrayList<>(Arrays.asList(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_REGIONS,
                CaptureRequest.CONTROL_AE_REGIONS));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keys.add(CaptureRequest.CONTROL_ZOOM_RATIO);
        } else {
            keys.add(CaptureRequest.SCALER_CROP_REGION);
        }
        return Collections.unmodifiableList(keys);
    }

    @NonNull
    @Override
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
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
    public int onSessionType() {
        return SessionConfiguration.SESSION_REGULAR;
    }

    @Override
    public boolean isCaptureProcessProgressAvailable() {
        return true;
    }

    @Nullable
    @Override
    public Pair<Long, Long> getRealtimeCaptureLatency() {
        return new Pair<>(500L, 2500L);
    }

    @Override
    public boolean isPostviewAvailable() {
        return true;
    }

    @RequiresApi(23)
    final class NightCaptureProcessorImpl implements CaptureProcessorImpl {
        private ImageWriter mImageWriter;
        private ImageWriter mImageWriterPostview;

        @Override
        public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
            mImageWriter = ImageWriter.newInstance(surface, 2);
        }

        @Override
        public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results) {
            processInternal(results, null, null, false);
        }

        @Override
        public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor) {
            processInternal(results, resultCallback, executor, false);
        }

        @SuppressWarnings("BanThreadSleep")
        public void processInternal(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @Nullable ProcessResultImpl resultCallback, @Nullable Executor executor,
                boolean hasPostview) {
            Executor executorForCallback = executor != null ? executor : (cmd) -> cmd.run();

            // Check for availability of all requested images
            for (int i = 0; i < getMaxCaptureStage() - 1; i++) {
                if (!results.containsKey(DEFAULT_STAGE_ID + i)) {
                    Log.w(TAG,
                            "Unable to process since images does not contain all images.");
                    return;
                }
            }

            // Do processing of images, our placeholder logic just copies the first
            // Image into the output buffer.
            List<Pair<Image, TotalCaptureResult>> imageDataPairs = new ArrayList<>(
                    results.values());
            Image outputImage = mImageWriter.dequeueInputImage();
            outputImage.setTimestamp(imageDataPairs.get(0).first.getTimestamp());
            // Do processing here
            // The sample here simply returns the normal image result
            int stageId = DEFAULT_STAGE_ID;
            Image normalImage = imageDataPairs.get(stageId).first;
            TotalCaptureResult captureResult = imageDataPairs.get(stageId).second;
            if (resultCallback != null) {
                executorForCallback.execute(() -> {
                    resultCallback.onCaptureProcessProgressed(10);
                });
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
                if (resultCallback != null) {
                    executorForCallback.execute(
                            () -> resultCallback.onCaptureProcessProgressed(50));
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

            if (resultCallback != null) {
                executorForCallback.execute(
                        () -> resultCallback.onCaptureProcessProgressed(75));
            }

            if (hasPostview) {
                mImageWriterPostview.queueInputImage(normalImage);
            }


            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            mImageWriter.queueInputImage(outputImage);
            if (resultCallback != null) {
                executorForCallback.execute(
                        () -> resultCallback.onCaptureProcessProgressed(100));
            }

            if (resultCallback != null) {
                executorForCallback.execute(
                        () -> resultCallback.onCaptureCompleted(
                                captureResult.get(CaptureResult.SENSOR_TIMESTAMP),
                                getFilteredResults(captureResult)));
            }
        }

        @SuppressWarnings("unchecked")
        private List<Pair<CaptureResult.Key, Object>> getFilteredResults(
                TotalCaptureResult captureResult) {
            List<Pair<CaptureResult.Key, Object>> list = new ArrayList<>();

            for (CaptureResult.Key availableCaptureResultKey : getAvailableCaptureResultKeys()) {
                if (captureResult.get(availableCaptureResultKey) != null) {
                    list.add(new Pair<>(availableCaptureResultKey,
                            captureResult.get(availableCaptureResultKey)));
                }
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
            mImageWriterPostview = ImageWriter.newInstance(surface, ImageFormat.YUV_420_888);
        }

        @Override
        public void onResolutionUpdate(@NonNull Size size, @NonNull Size postviewSize) {
            onResolutionUpdate(size);
        }

        @Override
        public void processWithPostview(
                @NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor) {
            processInternal(results, resultCallback, executor, true);
        }

        void release() {
            if (mImageWriter != null) {
                mImageWriter.close();
            }
            if (mImageWriterPostview != null) {
                mImageWriterPostview.close();
            }
        }
    }

    /**
     * This method is used to check if test lib is running. If OEM implementation exists, invoking
     * this method will throw {@link NoSuchMethodError}. This can be used to determine if OEM
     * implementation is used or not.
     */
    public static void checkTestlibRunning() {}
}
