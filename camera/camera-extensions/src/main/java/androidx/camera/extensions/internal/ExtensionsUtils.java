/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraInfoInternal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utils for camera-extensions.
 */
public class ExtensionsUtils {
    private ExtensionsUtils() {}

    /**
     * Returns a map consisting of the camera ids and the {@link CameraCharacteristics}s.
     *
     * <p>For every camera, the map contains at least the CameraCharacteristics for the camera id.
     * If the camera is logical camera, it will also contain associated physical camera ids and
     * their CameraCharacteristics.
     *
     */
    @NonNull
    public static Map<String, CameraCharacteristics> getCameraCharacteristicsMap(
            @NonNull CameraInfoInternal cameraInfoInternal) {
        LinkedHashMap<String, CameraCharacteristics> map = new LinkedHashMap<>();
        String cameraId = cameraInfoInternal.getCameraId();
        CameraCharacteristics cameraCharacteristics =
                (CameraCharacteristics) cameraInfoInternal.getCameraCharacteristics();
        map.put(cameraId, cameraCharacteristics);

        if (Build.VERSION.SDK_INT < 28) {
            return map;
        }

        Set<String> physicalCameraIds = Api28Impl.getPhysicalCameraIds(cameraCharacteristics);
        if (physicalCameraIds == null) {
            return map;
        }

        for (String physicalCameraId : physicalCameraIds) {
            if (Objects.equals(physicalCameraId, cameraId)) {
                continue;
            }
            map.put(physicalCameraId, (CameraCharacteristics)
                    cameraInfoInternal.getPhysicalCameraCharacteristics(physicalCameraId));
        }
        return map;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in API 28.
     */
    @RequiresApi(28)
    private static class Api28Impl {

        private Api28Impl() {
        }

        static Set<String> getPhysicalCameraIds(
                @NonNull CameraCharacteristics cameraCharacteristics) {
            try {
                return cameraCharacteristics.getPhysicalCameraIds();
            } catch (Exception e) {
                return Collections.emptySet();
            }
        }
    }
}
