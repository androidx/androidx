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

package androidx.camera.camera2.impl;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CameraXThreads;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The factory class that creates {@link Camera} instances.
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class Camera2CameraFactory implements CameraFactory {
    private static final String TAG = "Camera2CameraFactory";

    private static final HandlerThread sHandlerThread = new HandlerThread(CameraXThreads.TAG);
    private static final Handler sHandler;

    static {
        sHandlerThread.start();
        sHandler = new Handler(sHandlerThread.getLooper());
    }

    private final CameraManager mCameraManager;

    public Camera2CameraFactory(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public BaseCamera getCamera(String cameraId) {
        return new Camera(mCameraManager, cameraId, sHandler);
    }

    @Override
    public Set<String> getAvailableCameraIds() throws CameraInfoUnavailableException {
        List<String> camerasList = null;
        try {
            camerasList = Arrays.asList(mCameraManager.getCameraIdList());
        } catch (CameraAccessException e) {
            throw new CameraInfoUnavailableException(
                    "Unable to retrieve list of cameras on device.", e);
        }
        // Use a LinkedHashSet to preserve order
        return new LinkedHashSet<>(camerasList);
    }

    @Nullable
    @Override
    public String cameraIdForLensFacing(LensFacing lensFacing)
            throws CameraInfoUnavailableException {
        Set<String> cameraIds = getAvailableCameraIds();

        // Convert to from CameraX enum to Camera2 CameraMetadata
        Integer lensFacingInteger = -1;
        switch (lensFacing) {
            case BACK:
                lensFacingInteger = CameraMetadata.LENS_FACING_BACK;
                break;
            case FRONT:
                lensFacingInteger = CameraMetadata.LENS_FACING_FRONT;
                break;
        }

        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = null;
            try {
                characteristics = mCameraManager.getCameraCharacteristics(cameraId);
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
