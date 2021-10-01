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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.InitializationException;

import java.util.Set;

/**
 * The factory class that creates {@link CameraInternal} instances.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraFactory {

    /**
     * Interface for deferring creation of a CameraFactory.
     */
    interface Provider {
        /**
         * Creates a new, initialized instance of a CameraFactory.
         *
         * @param context the android context
         * @param threadConfig the thread config to run the camera operations
         * @param availableCamerasLimiter a CameraSelector used to specify which cameras will be
         *                                 loaded and available to CameraX.
         * @return the factory instance
         * @throws InitializationException if it fails to create the factory.
         */
        @NonNull CameraFactory newInstance(@NonNull Context context,
                @NonNull CameraThreadConfig threadConfig,
                @Nullable CameraSelector availableCamerasLimiter) throws InitializationException;
    }

    /**
     * Gets the camera with the associated id.
     *
     * @param cameraId the camera id to get camera with
     * @return the camera object with given camera id
     * @throws CameraUnavailableException if unable to access cameras, perhaps due
     *                                    to insufficient permissions.
     * @throws IllegalArgumentException   if the given camera id is not on the available
     *                                    camera id list.
     */
    @NonNull
    CameraInternal getCamera(@NonNull String cameraId) throws CameraUnavailableException;

    /**
     * Gets the ids of all available cameras.
     *
     * @return the list of available cameras
     */
    @NonNull
    Set<String> getAvailableCameraIds();

    /**
     * Gets the camera manager instance that is used to access the camera API.
     *
     * <p>Notes that actual type of this camera manager depends on the implementation. While it
     * is CameraManagerCompat in camera2 implementation, it could be some other type in
     * other implementation.
     */
    @Nullable
    Object getCameraManager();
}
