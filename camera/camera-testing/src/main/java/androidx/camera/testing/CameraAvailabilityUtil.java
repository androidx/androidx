/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.testing;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.CameraRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class to check whether or not specified cameras are available.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraAvailabilityUtil {
    /**
     * Checks whether this repository supports at least one camera that meets the requirements
     * from the {@link CameraSelector}.
     *
     * <p>If this method returns {@code true}, then the camera selector can be used to bind
     * use cases and retrieve a {@link Camera} instance.
     *
     * @param cameraSelector the {@link CameraSelector} that filters available cameras.
     * @return true if the device has at least one available camera, otherwise false.
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due to
     *                                        insufficient permissions.
     */
    public static boolean hasCamera(@NonNull CameraRepository cameraRepository,
            @NonNull CameraSelector cameraSelector) {
        try {
            cameraSelector.select(cameraRepository.getCameras());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * Gets the default lens facing from the specified
     * {@link CameraRepository}, or throws a {@link IllegalStateException} if there is no
     * available camera.
     *
     * @return The default lens facing.
     * @throws IllegalStateException if unable to find a camera with available lens facing.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @CameraSelector.LensFacing
    public static int getDefaultLensFacing(@NonNull CameraRepository cameraRepository) {
        Integer lensFacingCandidate = null;
        List<Integer> lensFacingList = Arrays.asList(CameraSelector.LENS_FACING_BACK,
                CameraSelector.LENS_FACING_FRONT);
        for (Integer lensFacing : lensFacingList) {
            if (hasCamera(cameraRepository,
                    new CameraSelector.Builder().requireLensFacing(lensFacing).build())) {
                lensFacingCandidate = lensFacing;
                break;
            }
        }
        if (lensFacingCandidate == null) {
            throw new IllegalStateException("Unable to get default lens facing.");
        }
        return lensFacingCandidate;
    }

    // Private to prevent instantiation
    private CameraAvailabilityUtil() {
    }
}
