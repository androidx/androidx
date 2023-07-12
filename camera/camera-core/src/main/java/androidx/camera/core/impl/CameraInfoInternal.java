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

package androidx.camera.core.impl;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DynamicRange;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * An interface for retrieving camera information.
 *
 * <p>Contains methods for retrieving characteristics for a specific camera.
 *
 * <p>{@link #getImplementation()} returns a {@link CameraInfoInternal} instance
 * that contains the actual implementation and can be cast to an implementation specific class.
 * If the instance itself is the implementation instance, then it should return <code>this</code>.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraInfoInternal extends CameraInfo {

    /**
     * Returns the camera id of this camera.
     *
     * @return the camera id
     */
    @NonNull
    String getCameraId();

    /**
     * Adds a {@link CameraCaptureCallback} which will be invoked when session capture request is
     * completed, failed or cancelled.
     *
     * <p>The callback will be invoked on the specified {@link Executor}.
     */
    void addSessionCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback callback);

    /**
     * Removes the {@link CameraCaptureCallback} which was added in
     * {@link #addSessionCaptureCallback(Executor, CameraCaptureCallback)}.
     */
    void removeSessionCaptureCallback(@NonNull CameraCaptureCallback callback);

    /** Returns a list of quirks related to the camera. */
    @NonNull
    Quirks getCameraQuirks();

    /** Returns the {@link EncoderProfilesProvider} associated with this camera. */
    @NonNull
    EncoderProfilesProvider getEncoderProfilesProvider();

    /** Returns the {@link Timebase} of frame output by this camera. */
    @NonNull
    Timebase getTimebase();

    /**
     * Returns the supported resolutions of this camera based on the input image format.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}.
     * @return a list of supported resolutions, or an empty list if the format is not supported.
     */
    @NonNull
    List<Size> getSupportedResolutions(int format);

    /**
     * Returns the supported high resolutions of this camera based on the input image format.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}.
     * @return a list of supported resolutions, or an empty list if the format is not supported.
     */
    @NonNull
    List<Size> getSupportedHighResolutions(int format);

    /**
     * Returns the supported dynamic ranges of this camera.
     *
     * @return a set of supported dynamic range, or an empty set if no dynamic range is supported.
     */
    @NonNull
    Set<DynamicRange> getSupportedDynamicRanges();

    /**
     * Gets the underlying implementation instance which could be cast into an implementation
     * specific class for further use in implementation module. Returns <code>this</code> if this
     * instance is the implementation instance.
     */
    @NonNull
    default CameraInfoInternal getImplementation() {
        return this;
    }


    /** {@inheritDoc} */
    @NonNull
    @Override
    default CameraSelector getCameraSelector() {
        return new CameraSelector.Builder()
                .addCameraFilter(cameraInfos -> {
                    final String cameraId = getCameraId();
                    for (CameraInfo cameraInfo : cameraInfos) {
                        Preconditions.checkArgument(cameraInfo instanceof CameraInfoInternal);
                        final CameraInfoInternal cameraInfoInternal =
                                (CameraInfoInternal) cameraInfo;
                        if (cameraInfoInternal.getCameraId().equals(cameraId)) {
                            return Collections.singletonList(cameraInfo);
                        }
                    }
                    throw new IllegalStateException("Unable to find camera with id " + cameraId
                            + " from list of available cameras.");
                })
                .addCameraFilter(new LensFacingCameraFilter(getLensFacing()))
                .build();
    }
}
