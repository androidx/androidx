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
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.ImageProxyBundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Provides interfaces that OEM need to implement to enable extension function.
 */
public abstract class ImageCaptureUseCaseExtender {
    private static final String TAG = "ImageCaptureExtender";
    private final ImageCaptureUseCaseConfiguration.Builder mBuilder;
    protected ImageCaptureUseCaseExtender mImpl;

    public ImageCaptureUseCaseExtender(ImageCaptureUseCaseConfiguration.Builder builder) {
        mBuilder = builder;
    }

    boolean loadImplementation(String className) {
        try {
            final Class<?> imageCaptureClass = Class.forName(className);
            Constructor<?> imageCaptureConstructor =
                    imageCaptureClass.getDeclaredConstructor(
                            ImageCaptureUseCaseConfiguration.Builder.class);
            mImpl = (ImageCaptureUseCaseExtender) imageCaptureConstructor.newInstance(mBuilder);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | InvocationTargetException
                | IllegalAccessException e) {
            Log.e(TAG, "Failed to load image capture extension with exception: " + e);
        }

        if (mImpl == null) {
            mImpl = new DefaultImageCaptureUseCaseExtender(mBuilder);
            return false;
        }

        return true;
    }

    /**
     * Indicates whether extension function can support with
     * {@link ImageCaptureUseCaseConfiguration.Builder}
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
     * feature. If more than one {@link CaptureStage} is set, then, {@link CaptureProcessor} must
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
     * Sets {@link CaptureProcessor} if post processing is needed for the extension effect mode.
     *
     * <p>If there is more than one {@link CaptureStage} set by {@link #setCaptureStages(List)},
     * {@link CaptureProcessor} must be set. Otherwise, this will be optional that depends on post
     * processing requirement. If {@link CaptureProcessor} is set, YUV format data will be
     * retrieved when {@link CaptureProcessor#process(ImageProxyBundle)} gets called. The
     * processed result should be passed to {@link CaptureProcessor#onOutputSurface(Surface, int)}.
     *
     * @param captureProcessor The implemented {@link CaptureProcessor} object to do post
     *                         processing.
     */
    protected void setCaptureProcessor(@NonNull CaptureProcessor captureProcessor) {
        mBuilder.setCaptureProcessor(captureProcessor);
    }
}
