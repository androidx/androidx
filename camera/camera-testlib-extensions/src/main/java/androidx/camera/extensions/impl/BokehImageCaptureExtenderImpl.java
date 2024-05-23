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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implementation for bokeh image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 *
 * @since 1.0
 */
@SuppressLint("UnknownNullness")
public final class BokehImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final String TAG = "BokehICExtender";
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;
    private static final int EFFECT = CaptureRequest.CONTROL_EFFECT_MODE_SEPIA;
    private BokehImageCaptureExtenderCaptureProcessorImpl mCaptureProcessor;
    public BokehImageCaptureExtenderImpl() {
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

        return CameraCharacteristicAvailability.isEffectAvailable(cameraCharacteristics, EFFECT);
    }

    @NonNull
    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        // Placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE, EFFECT);
        List<CaptureStageImpl> captureStages = new ArrayList<>();
        captureStages.add(captureStage);
        return captureStages;
    }

    @Nullable
    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCaptureProcessor = new BokehImageCaptureExtenderCaptureProcessorImpl();
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

        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE, EFFECT);

        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onEnableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE, EFFECT);

        return captureStage;
    }

    @Nullable
    @Override
    public CaptureStageImpl onDisableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE, EFFECT);

        return captureStage;
    }

    @Override
    public int getMaxCaptureStage() {
        return 3;
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

    @Override
    public int onSessionType() {
        return SessionConfiguration.SESSION_REGULAR;
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedPostviewResolutions(@NonNull Size captureSize) {
        return null;
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

    @RequiresApi(23)
    static final class BokehImageCaptureExtenderCaptureProcessorImpl implements
            CaptureProcessorImpl {
        private ImageWriter mImageWriter;

        @Override
        public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
            mImageWriter = ImageWriter.newInstance(surface, 1);
        }

        @Override
        public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results) {
            Log.d(TAG, "Started bokeh CaptureProcessor");

            Pair<Image, TotalCaptureResult> result = results.get(DEFAULT_STAGE_ID);

            if (result == null) {
                Log.w(TAG,
                        "Unable to process since images does not contain all stages.");
                return;
            } else {
                Image outputImage = mImageWriter.dequeueInputImage();
                Image image = result.first;

                // copy y plane
                Image.Plane inYPlane = image.getPlanes()[0];
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
                    Image.Plane inPlane = image.getPlanes()[i];
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
                outputImage.setTimestamp(image.getTimestamp());
                mImageWriter.queueInputImage(outputImage);
            }

            Log.d(TAG, "Completed bokeh CaptureProcessor");
        }

        @Override
        public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor) {
            process(results);
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

        }

        @Override
        public void processWithPostview(
                @NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results,
                @NonNull ProcessResultImpl resultCallback, @Nullable Executor executor) {
            throw new UnsupportedOperationException("Postview is not supported");
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

    @NonNull
    @Override
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
        return null;
    }
}
