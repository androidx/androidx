/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfo;

import java.util.Map;

/**
 * A unified vendor extensions interface which interacts with both basic and advanced extender
 * vendor implementation.
 */
public interface VendorExtender {
    /**
     * Indicates whether the extension is supported on the device.
     *
     * @param cameraId           The camera2 id string of the camera.
     * @param characteristicsMap A map consisting of the camera ids and the
     *                           {@link CameraCharacteristics}s. For every camera, the map
     *                           contains at least the CameraCharacteristics for the camera id.
     *                           If the camera is logical camera, it will also contain associated
     *                           physical camera ids and their CameraCharacteristics.
     * @return true if the extension is supported, otherwise false
     */
    boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap);


    /**
     * Initializes the extender to be used with the specified camera.
     */
    void init(@NonNull CameraInfo cameraInfo);
}
