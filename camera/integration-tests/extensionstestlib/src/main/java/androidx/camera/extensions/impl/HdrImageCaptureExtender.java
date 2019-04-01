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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageProxyBundle;
import androidx.camera.extensions.CaptureStage;
import androidx.camera.extensions.ImageCaptureExtender;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation for HDR image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 */
public final class HdrImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "HdrImageCaptureExtender";
    private static final int UNDER_STAGE_ID = 0;
    private static final int NORMAL_STAGE_ID = 1;
    private static final int OVER_STAGE_ID = 2;

    public HdrImageCaptureExtender(ImageCaptureConfig.Builder builder) {
        super(builder);
    }

    @Override
    public void enableExtension() {
        Log.d(TAG, "Enabling HDR extension");
        // 1. Sets necessary CaptureStage settings
        // Under exposed capture stage
        CaptureStage captureStageUnder = new CaptureStage(UNDER_STAGE_ID);
        // Turn off AE so that ISO sensitivity can be controlled
        captureStageUnder.addCaptureRequestParameters(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF);
        captureStageUnder.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                TimeUnit.MILLISECONDS.toNanos(8));

        // Normal exposed capture stage
        CaptureStage captureStageNormal = new CaptureStage(NORMAL_STAGE_ID);
        captureStageNormal.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                TimeUnit.MILLISECONDS.toNanos(16));

        // Over exposed capture stage
        CaptureStage captureStageOver = new CaptureStage(OVER_STAGE_ID);
        captureStageOver.addCaptureRequestParameters(CaptureRequest.SENSOR_EXPOSURE_TIME,
                TimeUnit.MILLISECONDS.toNanos(32));

        // 2. Sets CaptureProcess if necessary...
        CaptureProcessor captureProcessor =
                new CaptureProcessor() {
                    ImageWriter mImageWriter;

                    @Override
                    public void onOutputSurface(Surface surface, int imageFormat) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mImageWriter = ImageWriter.newInstance(surface, 1);
                        }
                    }

                    @Override
                    public void process(ImageProxyBundle bundle) {
                        Log.d(TAG, "Started HDR CaptureProcessor");

                        ListenableFuture<ImageProxy> underResultFuture = bundle.getImageProxy(
                                UNDER_STAGE_ID);
                        ListenableFuture<ImageProxy> normalResultFuture = bundle.getImageProxy(
                                NORMAL_STAGE_ID);
                        ListenableFuture<ImageProxy> overResultFuture = bundle.getImageProxy(
                                OVER_STAGE_ID);

                        List<ImageProxy> results = new ArrayList<>();

                        try {
                            results.add(underResultFuture.get(5, TimeUnit.SECONDS));
                            results.add(normalResultFuture.get(5, TimeUnit.SECONDS));
                            results.add(overResultFuture.get(5, TimeUnit.SECONDS));

                            Image image = null;
                            if (android.os.Build.VERSION.SDK_INT
                                    >= android.os.Build.VERSION_CODES.M) {
                                image = mImageWriter.dequeueInputImage();

                                // Do processing here
                                ByteBuffer yByteBuffer = image.getPlanes()[0].getBuffer();
                                ByteBuffer uByteBuffer = image.getPlanes()[2].getBuffer();
                                ByteBuffer vByteBuffer = image.getPlanes()[1].getBuffer();

                                // Sample here just simply return the normal image result
                                yByteBuffer.put(results.get(1).getPlanes()[0].getBuffer());
                                uByteBuffer.put(results.get(1).getPlanes()[2].getBuffer());
                                vByteBuffer.put(results.get(1).getPlanes()[1].getBuffer());

                                mImageWriter.queueInputImage(image);
                            }
                        } catch (ExecutionException | InterruptedException | TimeoutException e) {
                            Log.e(TAG, "Failed to obtain result: " + e);
                        } finally {

                            for (ImageProxy imageProxy : results) {
                                imageProxy.close();
                            }
                        }

                        Log.d(TAG, "Completed HDR CaptureProcessor");
                    }
                };

        setCaptureStages(Arrays.asList(captureStageUnder, captureStageNormal, captureStageOver));
        setCaptureProcessor(captureProcessor);
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        // Implement the logic to check whether the extension function is supported or not.
        return true;
    }
}
