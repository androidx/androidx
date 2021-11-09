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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.InitializationException;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.CameraThreadConfig;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The factory class that creates {@link Camera2CameraImpl} instances.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2CameraFactory implements CameraFactory {
    private static final int DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS = 1;
    private final CameraThreadConfig mThreadConfig;
    private final CameraStateRegistry mCameraStateRegistry;
    private final CameraManagerCompat mCameraManager;
    private final List<String> mAvailableCameraIds;
    private final Map<String, Camera2CameraInfoImpl> mCameraInfos = new HashMap<>();

    /** Creates a Camera2 implementation of CameraFactory */
    public Camera2CameraFactory(@NonNull Context context,
            @NonNull CameraThreadConfig threadConfig,
            @Nullable CameraSelector availableCamerasSelector) throws InitializationException {
        mThreadConfig = threadConfig;
        mCameraStateRegistry = new CameraStateRegistry(DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS);
        mCameraManager = CameraManagerCompat.from(context, mThreadConfig.getSchedulerHandler());

        mAvailableCameraIds = CameraSelectionOptimizer
                .getSelectedAvailableCameraIds(this, availableCamerasSelector);
    }

    @Override
    @NonNull
    public CameraInternal getCamera(@NonNull String cameraId) throws CameraUnavailableException {
        if (!mAvailableCameraIds.contains(cameraId)) {
            throw new IllegalArgumentException(
                    "The given camera id is not on the available camera id list.");
        }
        return new Camera2CameraImpl(mCameraManager,
                cameraId,
                getCameraInfo(cameraId),
                mCameraStateRegistry,
                mThreadConfig.getCameraExecutor(),
                mThreadConfig.getSchedulerHandler());
    }

    Camera2CameraInfoImpl getCameraInfo(@NonNull String cameraId)
            throws CameraUnavailableException {
        try {
            Camera2CameraInfoImpl camera2CameraInfoImpl = mCameraInfos.get(cameraId);
            if (camera2CameraInfoImpl == null) {
                camera2CameraInfoImpl = new Camera2CameraInfoImpl(
                        cameraId, mCameraManager);
                mCameraInfos.put(cameraId, camera2CameraInfoImpl);
            }
            return camera2CameraInfoImpl;
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
        }
    }
    @Override
    @NonNull
    public Set<String> getAvailableCameraIds() {
        // Use a LinkedHashSet to preserve order
        return new LinkedHashSet<>(mAvailableCameraIds);
    }

    @NonNull
    @Override
    public CameraManagerCompat getCameraManager() {
        return mCameraManager;
    }
}
