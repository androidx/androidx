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

package androidx.camera.camera2.internal;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.CameraThreadConfig;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The factory class that creates {@link Camera2CameraImpl} instances.
 */
public final class Camera2CameraFactory implements CameraFactory {
    private static final int DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS = 1;
    private final CameraThreadConfig mThreadConfig;

    private final CameraStateRegistry mCameraStateRegistry;
    private final CameraManagerCompat mCameraManager;

    /** Creates a Camera2 implementation of CameraFactory */
    public Camera2CameraFactory(@NonNull Context context,
            @NonNull CameraThreadConfig threadConfig) {
        mThreadConfig = threadConfig;
        mCameraStateRegistry = new CameraStateRegistry(DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS);
        mCameraManager = CameraManagerCompat.from(context, mThreadConfig.getSchedulerHandler());
    }

    @Override
    @NonNull
    public CameraInternal getCamera(@NonNull String cameraId)
            throws CameraInfoUnavailableException {
        if (!getAvailableCameraIds().contains(cameraId)) {
            throw new IllegalArgumentException(
                    "The given camera id is not on the available camera id list.");
        }
        return new Camera2CameraImpl(mCameraManager, cameraId, mCameraStateRegistry,
                mThreadConfig.getCameraExecutor(), mThreadConfig.getSchedulerHandler());
    }

    @Override
    @NonNull
    public Set<String> getAvailableCameraIds() throws CameraInfoUnavailableException {
        List<String> camerasList;
        try {
            camerasList = Arrays.asList(mCameraManager.unwrap().getCameraIdList());
        } catch (CameraAccessException e) {
            throw new CameraInfoUnavailableException(
                    "Unable to retrieve list of cameras on device.", e);
        }
        // Use a LinkedHashSet to preserve order
        return new LinkedHashSet<>(camerasList);
    }

    @Override
    @Nullable
    public String cameraIdForLensFacing(@CameraSelector.LensFacing int lensFacing)
            throws CameraInfoUnavailableException {
        Set<String> cameraIds = getAvailableCameraIds();

        Integer lensFacingInteger = -1;
        switch (lensFacing) {
            case CameraSelector.LENS_FACING_BACK:
                lensFacingInteger = CameraMetadata.LENS_FACING_BACK;
                break;
            case CameraSelector.LENS_FACING_FRONT:
                lensFacingInteger = CameraMetadata.LENS_FACING_FRONT;
                break;
        }

        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = null;
            try {
                characteristics = mCameraManager.unwrap().getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
                throw new CameraInfoUnavailableException(
                        "Unable to retrieve info for camera with id " + cameraId + ".", e);
            }
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null) {
                continue;
            }
            if (cameraLensFacing.equals(lensFacingInteger)) {
                return cameraId;
            }
        }

        return null;
    }
}
