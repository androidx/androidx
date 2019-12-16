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

package androidx.camera.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.LensFacingCameraIdFilter;

import java.util.Set;

/**
 * The factory class that creates {@link CameraInternal} instances.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraFactory {

    /**
     * Interface for deferring creation of a CameraFactory.
     */
    interface Provider {
        /** Creates a new, initialized instance of a CameraFactory. */
        @NonNull CameraFactory newInstance(@NonNull Context context);
    }

    /**
     * Gets the camera with the associated id.
     *
     * @param cameraId the camera id to get camera with
     * @return the camera object with given camera id
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due
     *                                        to insufficient permissions.
     * @throws IllegalArgumentException       if the given camera id is not on the available
     *                                        camera id list.
     */
    @NonNull
    CameraInternal getCamera(@NonNull String cameraId) throws CameraInfoUnavailableException;

    /**
     * Gets the ids of all available cameras.
     *
     * @return the list of available cameras
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due
     *                                        to insufficient permissions.
     */
    @NonNull
    Set<String> getAvailableCameraIds() throws CameraInfoUnavailableException;

    /**
     * Gets the first id of the camera with the given lens facing. Returns null if there's no
     * camera with given lens facing.
     *
     * @param lensFacing the lens facing to query camera id with
     * @return the first id of the camera with the given lens facing
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due
     *                                        to insufficient permissions.
     */
    @Nullable
    String cameraIdForLensFacing(@CameraSelector.LensFacing int lensFacing)
            throws CameraInfoUnavailableException;

    /**
     * Gets a {@link LensFacingCameraIdFilter} with given lens facing.
     *
     * @param lensFacing the lens facing to create camera id filter with
     * @return the camera id filter that filters cameras with given lens facing
     */
    @NonNull
    LensFacingCameraIdFilter getLensFacingCameraIdFilter(@CameraSelector.LensFacing int lensFacing);
}
