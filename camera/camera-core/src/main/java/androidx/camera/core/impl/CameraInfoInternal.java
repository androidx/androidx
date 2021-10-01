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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * An interface for retrieving camera information.
 *
 * <p>Contains methods for retrieving characteristics for a specific camera.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraInfoInternal extends CameraInfo {
    /**
     * Returns the LensFacing of this camera.
     *
     * @return One of {@link androidx.camera.core.CameraSelector#LENS_FACING_FRONT},
     * {@link androidx.camera.core.CameraSelector#LENS_FACING_BACK}, or <code>null</code> if the
     * LensFacing does not fall into one of these two categories.
     */
    // TODO(b/122975195): Remove @Nullable and null return type once we have a LensFacing type which
    // can be used to represent non-BACK or FRONT facing lenses.
    @Nullable
    Integer getLensFacing();

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

    /** Returns the {@link CamcorderProfileProvider} associated with this camera. */
    @NonNull
    CamcorderProfileProvider getCamcorderProfileProvider();

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
                .build();
    }
}
