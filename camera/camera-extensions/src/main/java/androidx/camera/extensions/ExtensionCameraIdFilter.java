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

package androidx.camera.extensions;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraIdFilter;
import androidx.camera.extensions.impl.ExtensionAvailability;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter camera id by extension availability.
 */
public final class ExtensionCameraIdFilter implements CameraIdFilter {
    private ExtensionAvailability mExtensionAvailability;

    ExtensionCameraIdFilter(ExtensionAvailability extensionAvailability) {
        mExtensionAvailability = extensionAvailability;
    }

    @Override
    @NonNull
    public Set<String> filter(@NonNull Set<String> cameraIdSet) {
        Set<String> resultCameraIdSet = new LinkedHashSet<>();
        for (String cameraId : cameraIdSet) {
            if (mExtensionAvailability.isExtensionAvailable(cameraId,
                    CameraUtil.getCameraCharacteristics(cameraId))) {
                resultCameraIdSet.add(cameraId);
            }
        }

        return resultCameraIdSet;
    }
}
