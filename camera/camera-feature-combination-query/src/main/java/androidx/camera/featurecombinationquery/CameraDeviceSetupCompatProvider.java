/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.featurecombinationquery;

import android.hardware.camera2.CameraAccessException;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface for providing a {@link CameraDeviceSetupCompat} for a camera device.
 *
 * <p> Getting a provider usually needs Binder calls which is costly. This interface allows the
 * to be cached and reused.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraDeviceSetupCompatProvider {

    /**
     * Get a {@link CameraDeviceSetupCompat} for the camera device with the given cameraId.
     *
     * @param cameraId the cameraId of the camera device.
     * @return a {@link CameraDeviceSetupCompat} for the camera device with the given cameraId.
     */
    @NonNull
    CameraDeviceSetupCompat getCameraDeviceSetupCompat(@NonNull String cameraId)
            throws CameraAccessException;
}
