/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.extensions;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageCaptureConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Provides interfaces that OEM need to implement to enable extension function.
 */
public abstract class ImageCaptureExtender {
    private static final String TAG = "ImageCaptureExtender";
    private final ImageCaptureConfig.Builder mBuilder;
    protected ImageCaptureExtender mImpl;

    public ImageCaptureExtender(ImageCaptureConfig.Builder builder) {
        mBuilder = builder;
    }

    boolean loadImplementation(String className) {
        try {
            final Class<?> imageCaptureClass = Class.forName(className);
            Constructor<?> imageCaptureConstructor =
                    imageCaptureClass.getDeclaredConstructor(ImageCaptureConfig.Builder.class);
            mImpl = (ImageCaptureExtender) imageCaptureConstructor.newInstance(mBuilder);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | InvocationTargetException
                | IllegalAccessException e) {
            Log.e(TAG, "Failed to load image capture extension with exception: " + e);
        }

        if (mImpl == null) {
            mImpl = new DefaultImageCaptureExtender(mBuilder);
            return false;
        }

        return true;
    }

    /**
     * Indicates whether extension function can support with {@link ImageCaptureConfig.Builder}
     *
     * @return True if the specific extension function is supported for the camera device.
     */
    public boolean isExtensionAvailable() {
        LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId;
        try {
            cameraId = CameraX.getCameraWithLensFacing(lensFacing);
        } catch (CameraInfoUnavailableException e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }

        Context context = CameraX.getContext();
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics = null;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new IllegalArgumentException(
                    "Unable to retrieve info for camera with id " + cameraId + ".", e);
        }

        return isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    protected boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        return mImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    /** Enable the extension if available. If not available then acts a no-op. */
    public void enableExtension() {
        mImpl.enableExtension();
    }

    /**
     * Sets necessary {@link CaptureStage} lists for the extension effect mode.
     *
     * <p>Sets one or more {@link CaptureStage} objects that depends on the requirement for the
     * feature. If more than one {@link CaptureStage} is set, then the processing step must
     * be set to process the multiple results into one final result.
     *
     * @param captureStages The necessary {@link CaptureStage} lists.
     */
    protected void setCaptureStages(@NonNull List<CaptureStage> captureStages) {
        if (captureStages.isEmpty()) {
            throw new IllegalArgumentException("The CaptureStage list is empty.");
        }

        CaptureBundle captureBundle = new CaptureBundle();

        for (CaptureStage captureStage : captureStages) {
            captureBundle.addCaptureStage(captureStage);
        }

        mBuilder.setCaptureBundle(captureBundle);
    }

    /**
     * Sets the post processing step needed for the extension effect mode.
     *
     * <p>If there is more than one {@link CaptureStage} set by {@link #setCaptureStages(List)},
     * then this must be set. Otherwise, this will be optional that depends on post
     * processing requirement. The post processing will receive YUV_420_888 formatted image data.
     * The post processing should also write out YUV_420_888 image data.
     *
     * @param captureProcessor The post processing implementation
     */
    protected void setCaptureProcessor(@NonNull CaptureProcessor captureProcessor) {
        mBuilder.setCaptureProcessor(captureProcessor);
    }
}
