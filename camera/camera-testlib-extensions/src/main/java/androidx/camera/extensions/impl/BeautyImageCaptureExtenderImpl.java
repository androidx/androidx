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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
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

/**
 * Implementation for beauty image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class BeautyImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final String TAG = "BeautyICExtender";
    private static final int DEFAULT_STAGE_ID = 0;
    private static final int SESSION_STAGE_ID = 101;
    private static final int EFFECT = CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE;

    private CameraCharacteristics mCameraCharacteristics;

    public BeautyImageCaptureExtenderImpl() {
    }

    @Override
    public void init(String cameraId, CameraCharacteristics cameraCharacteristics) {
        mCameraCharacteristics = cameraCharacteristics;
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        // Requires API 23 for ImageWriter
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        if (cameraCharacteristics == null) {
            return false;
        }

        return CameraCharacteristicAvailability.isEffectAvailable(cameraCharacteristics, EFFECT);
    }

    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        // Placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE, EFFECT);
        List<CaptureStageImpl> captureStages = new ArrayList<>();
        captureStages.add(captureStage);
        return captureStages;
    }

    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new BeautyImageCaptureExtenderCaptureProcessorImpl();
        } else {
            return new NoOpCaptureProcessorImpl();
        }
    }

    @Override
    public void onInit(String cameraId, CameraCharacteristics cameraCharacteristics,
            Context context) {

    }

    @Override
    public void onDeInit() {

    }

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

    @Override
    public CaptureStageImpl onEnableSession() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE, EFFECT);

        return captureStage;
    }

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

    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        List<Pair<Integer, Size[]>> formatResolutionsPairList = new ArrayList<>();

        StreamConfigurationMap map =
                mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map != null) {
            // The sample implementation only retrieves originally supported resolutions from
            // CameraCharacteristics for JPEG and YUV_420_888 formats to return.
            Size[] outputSizes = map.getOutputSizes(ImageFormat.JPEG);

            if (outputSizes != null) {
                formatResolutionsPairList.add(Pair.create(ImageFormat.JPEG, outputSizes));
            }

            outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);

            if (outputSizes != null) {
                formatResolutionsPairList.add(Pair.create(ImageFormat.YUV_420_888, outputSizes));
            }
        }

        return formatResolutionsPairList;
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size captureOutputSize) {
        return new Range<>(300L, 1000L);
    }

    @RequiresApi(23)
    static final class BeautyImageCaptureExtenderCaptureProcessorImpl implements
            CaptureProcessorImpl {
        private ImageWriter mImageWriter;

        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {
            mImageWriter = ImageWriter.newInstance(surface, 1);
        }

        @Override
        public void process(Map<Integer, Pair<Image, TotalCaptureResult>> results) {
            Log.d(TAG, "Started beauty CaptureProcessor");

            Pair<Image, TotalCaptureResult> result = results.get(DEFAULT_STAGE_ID);

            if (result == null) {
                Log.w(TAG,
                        "Unable to process since images does not contain all stages.");
                return;
            } else {
                Image image = mImageWriter.dequeueInputImage();

                // Do processing here
                ByteBuffer yByteBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uByteBuffer = image.getPlanes()[2].getBuffer();
                ByteBuffer vByteBuffer = image.getPlanes()[1].getBuffer();

                // Sample here just simply copy/paste the capture image result
                yByteBuffer.put(result.first.getPlanes()[0].getBuffer());
                uByteBuffer.put(result.first.getPlanes()[2].getBuffer());
                vByteBuffer.put(result.first.getPlanes()[1].getBuffer());

                mImageWriter.queueInputImage(image);
            }

            Log.d(TAG, "Completed beauty CaptureProcessor");
        }

        @Override
        public void onResolutionUpdate(Size size) {

        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {

        }
    }
}
