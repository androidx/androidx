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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.PhysicalCameraInfo;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

/**
 * Camera2 implementation of {@link PhysicalCameraInfo} which wraps physical camera id and
 * camera characteristics.
 */
@RequiresApi(21)
@AutoValue
abstract class Camera2PhysicalCameraInfo implements PhysicalCameraInfo {

    @NonNull
    @Override
    public abstract String getPhysicalCameraId();

    @NonNull
    public abstract CameraCharacteristicsCompat getCameraCharacteristicsCompat();

    @RequiresApi(28)
    @NonNull
    @Override
    public Integer getLensPoseReference() {
        Integer lensPoseRef =
                getCameraCharacteristicsCompat().get(CameraCharacteristics.LENS_POSE_REFERENCE);
        Preconditions.checkNotNull(lensPoseRef);
        return lensPoseRef;
    }

    /**
     * Creates {@link Camera2PhysicalCameraInfo} instance.
     *
     * @param physicalCameraId physical camera id.
     * @param cameraCharacteristicsCompat {@link CameraCharacteristicsCompat}.
     * @return
     */
    @NonNull
    public static Camera2PhysicalCameraInfo of(
            @NonNull String physicalCameraId,
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        return new AutoValue_Camera2PhysicalCameraInfo(
                physicalCameraId, cameraCharacteristicsCompat);
    }
}
