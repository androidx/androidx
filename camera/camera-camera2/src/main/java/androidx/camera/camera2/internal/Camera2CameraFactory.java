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
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXThreads;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.LensFacingCameraIdFilter;
import androidx.core.os.HandlerCompat;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The factory class that creates {@link Camera2CameraImpl} instances.
 */
public final class Camera2CameraFactory implements CameraFactory {
    private static final int DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS = 1;

    private static final HandlerThread sHandlerThread = new HandlerThread(CameraXThreads.TAG);
    private static final Handler sHandler;
    private final CameraStateRegistry mCameraStateRegistry;
    private final CameraManagerCompat mCameraManager;

    static {
        sHandlerThread.start();
        sHandler = HandlerCompat.createAsync(sHandlerThread.getLooper());
    }

    /** Creates a Camera2 implementation of CameraFactory */
    public Camera2CameraFactory(@NonNull Context context) {
        mCameraStateRegistry = new CameraStateRegistry(DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS);
        mCameraManager = CameraManagerCompat.from(context);
    }

    @Override
    @NonNull
    public CameraInternal getCamera(@NonNull String cameraId)
            throws CameraInfoUnavailableException {
        if (!getAvailableCameraIds().contains(cameraId)) {
            throw new IllegalArgumentException(
                    "The given camera id is not on the available camera id list.");
        }
        Camera2CameraImpl camera2CameraImpl = new Camera2CameraImpl(mCameraManager, cameraId,
                mCameraStateRegistry, sHandler, sHandler);
        return camera2CameraImpl;
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
        Set<String> availableCameraIds = getLensFacingCameraIdFilter(
                lensFacing).filter(getAvailableCameraIds());

        if (!availableCameraIds.isEmpty()) {
            return availableCameraIds.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    @NonNull
    public LensFacingCameraIdFilter getLensFacingCameraIdFilter(
            @CameraSelector.LensFacing int lensFacing) {
        return new Camera2LensFacingCameraIdFilter(lensFacing, mCameraManager.unwrap());
    }
}
