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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation for bokeh image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 */
public class BokehImageCaptureExtender extends ImageCaptureExtender {
    private static final String TAG = "BokehICExtender";
    private static final int DEFAULT_STAGE_ID = 0;

    public BokehImageCaptureExtender(ImageCaptureConfig.Builder builder) {
        super(builder);
    }

    @Override
    public void enableExtension() {
        // 1. Sets necessary CaptureStage settings
        CaptureStage captureStage = new CaptureStage(DEFAULT_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);

        // 2. Sets CaptureProcess if necessary...
        CaptureProcessor captureProcessor =
                new CaptureProcessor() {
                    @Override
                    public void onOutputSurface(Surface surface, int imageFormat) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mImageWriter = ImageWriter.newInstance(surface, 1);
                        }
                    }

                    @Override
                    public void process(ImageProxyBundle bundle) {
                        Log.d(TAG, "Started bokeh CaptureProcessor");

                        ListenableFuture<ImageProxy> resultFuture = bundle.getImageProxy(
                                DEFAULT_STAGE_ID);

                        ImageProxy result = null;

                        try {
                            result = resultFuture.get(5, TimeUnit.SECONDS);

                            Image image = null;
                            if (android.os.Build.VERSION.SDK_INT
                                    >= android.os.Build.VERSION_CODES.M) {
                                image = mImageWriter.dequeueInputImage();

                                // Do processing here
                                ByteBuffer yByteBuffer = image.getPlanes()[0].getBuffer();
                                ByteBuffer uByteBuffer = image.getPlanes()[2].getBuffer();
                                ByteBuffer vByteBuffer = image.getPlanes()[1].getBuffer();

                                // Sample here just simply copy/paste the capture image result
                                yByteBuffer.put(result.getPlanes()[0].getBuffer());
                                uByteBuffer.put(result.getPlanes()[2].getBuffer());
                                vByteBuffer.put(result.getPlanes()[1].getBuffer());

                                mImageWriter.queueInputImage(image);
                            }
                        } catch (ExecutionException | InterruptedException | TimeoutException e) {
                            Log.e(TAG, "Failed to obtain result: " + e);
                        } finally {
                            result.close();
                        }

                        Log.d(TAG, "Completed bokeh CaptureProcessor");
                    }

                    ImageWriter mImageWriter;
                };

        setCaptureStages(Arrays.asList(captureStage));
        setCaptureProcessor(captureProcessor);
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        // Implement the logic to check whether the extension function is supported or not.
        return true;
    }
}
