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

package androidx.camera.core;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * An interface for retrieving physical camera information.
 *
 * <p>Applications can retrieve physical camera information via
 * {@link CameraInfo#getPhysicalCameraInfos()}. As a comparison, {@link CameraInfo} represents
 * logical camera information. A logical camera is a grouping of two or more of those physical
 * cameras.
 *
 * <p>See <a href="https://developer.android.com/media/camera/camera2/multi-camera">Multi-camera API</a>
 * for more information.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21)
public interface PhysicalCameraInfo {

    /**
     * Returns physical camera id.
     *
     * @return physical camera id.
     */
    @NonNull
    String getPhysicalCameraId();

    /**
     * Returns {@link android.hardware.camera2.CameraCharacteristics#LENS_POSE_REFERENCE}.
     *
     * @return lens pose reference.
     */
    @RequiresApi(28)
    @NonNull
    Integer getLensPoseReference();
}
