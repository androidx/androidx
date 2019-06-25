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
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation for HDR image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 */
public final class HdrImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final String TAG = "HdrImageCaptureExtender";
    private static final int UNDER_STAGE_ID = 0;
    private static final int NORMAL_STAGE_ID = 1;
    private static final int OVER_STAGE_ID = 2;
    private static final int SESSION_STAGE_ID = 101;

    public HdrImageCaptureExtenderImpl() {
    }

    @Override
    public void init(String cameraId, CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        // Requires API 23 for ImageWriter
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
    }

    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        // Under exposed capture stage
        SettableCaptureStage captureStageUnder = new SettableCaptureStage(UNDER_STAGE_ID);
        // Turn off AE so that ISO sensitivity can be controlled
        captureStageUnder.addCaptureRequestParameters(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);
        captureStageUnder.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                TimeUnit.MILLISECONDS.toNanos(8));

        // Normal exposed capture stage
        SettableCaptureStage captureStageNormal = new SettableCaptureStage(NORMAL_STAGE_ID);
        captureStageNormal.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                TimeUnit.MILLISECONDS.toNanos(16));

        // Over exposed capture stage
        SettableCaptureStage captureStageOver = new SettableCaptureStage(OVER_STAGE_ID);
        captureStageOver.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                TimeUnit.MILLISECONDS.toNanos(32));

        List<CaptureStageImpl> captureStages = new ArrayList<>();
        captureStages.add(captureStageUnder);
        captureStages.add(captureStageNormal);
        captureStages.add(captureStageOver);
        return captureStages;
    }

    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        CaptureProcessorImpl captureProcessor =
                new CaptureProcessorImpl() {
                    private ImageWriter mImageWriter;

                    @Override
                    public void onOutputSurface(Surface surface, int imageFormat) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mImageWriter = ImageWriter.newInstance(surface, 1);
                        }
                    }

                    @Override
                    public void process(Map<Integer, Pair<Image, TotalCaptureResult>> results) {
                        Log.d(TAG, "Started HDR CaptureProcessor");

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
                        Image image = null;
                        if (android.os.Build.VERSION.SDK_INT
                                >= android.os.Build.VERSION_CODES.M) {
                            image = mImageWriter.dequeueInputImage();

                            // Do processing here
                            ByteBuffer yByteBuffer = image.getPlanes()[0].getBuffer();
                            ByteBuffer uByteBuffer = image.getPlanes()[2].getBuffer();
                            ByteBuffer vByteBuffer = image.getPlanes()[1].getBuffer();

                            // Sample here just simply return the normal image result
                            yByteBuffer.put(imageDataPairs.get(1).first.getPlanes()[0].getBuffer());
                            uByteBuffer.put(imageDataPairs.get(1).first.getPlanes()[2].getBuffer());
                            vByteBuffer.put(imageDataPairs.get(1).first.getPlanes()[1].getBuffer());

                            mImageWriter.queueInputImage(image);
                        }

                        Log.d(TAG, "Completed HDR CaptureProcessor");
                    }

                    @Override
                    public void onResolutionUpdate(Size size) {

                    }

                    @Override
                    public void onImageFormatUpdate(int imageFormat) {

                    }
                };
        return captureProcessor;
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
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Override
    public CaptureStageImpl onEnableSession() {
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Override
    public CaptureStageImpl onDisableSession() {
        SettableCaptureStage captureStage = new SettableCaptureStage(SESSION_STAGE_ID);
        return captureStage;
    }

    @Override
    public int getMaxCaptureStage() {
        return 4;
    }

}
