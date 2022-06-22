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

package androidx.camera.testing.fakes;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A CameraDeviceSurfaceManager which has no supported SurfaceConfigs. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FakeCameraDeviceSurfaceManager implements CameraDeviceSurfaceManager {

    public static final Size MAX_OUTPUT_SIZE = new Size(4032, 3024); // 12.2 MP

    private final Map<String, Map<Class<? extends UseCaseConfig<?>>, Size>> mDefinedResolutions =
            new HashMap<>();

    /**
     * Sets the given suggested resolutions for the specified camera Id and use case type.
     */
    public void setSuggestedResolution(@NonNull String cameraId,
            @NonNull Class<? extends UseCaseConfig<?>> type,
            @NonNull Size size) {
        Map<Class<? extends UseCaseConfig<?>>, Size> useCaseConfigTypeToSizeMap =
                mDefinedResolutions.get(cameraId);
        if (useCaseConfigTypeToSizeMap == null) {
            useCaseConfigTypeToSizeMap = new HashMap<>();
            mDefinedResolutions.put(cameraId, useCaseConfigTypeToSizeMap);
        }

        useCaseConfigTypeToSizeMap.put(type, size);
    }

    @Override
    public boolean checkSupported(@NonNull String cameraId,
            @NonNull List<SurfaceConfig> surfaceConfigList) {
        return false;
    }

    @NonNull
    @Override
    public SurfaceConfig transformSurfaceConfig(@NonNull String cameraId, int imageFormat,
            @NonNull Size size) {

        //returns a placeholder SurfaceConfig
        return SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW);
    }

    @Override
    @NonNull
    public Map<UseCaseConfig<?>, Size> getSuggestedResolutions(
            @NonNull String cameraId,
            @NonNull List<AttachedSurfaceInfo> existingSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs) {
        Map<UseCaseConfig<?>, Size> suggestedSizes = new HashMap<>();
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            Size resolution = MAX_OUTPUT_SIZE;
            Map<Class<? extends UseCaseConfig<?>>, Size> definedResolutions =
                    mDefinedResolutions.get(cameraId);
            if (definedResolutions != null) {
                Size definedResolution = definedResolutions.get(useCaseConfig.getClass());
                if (definedResolution != null) {
                    resolution = definedResolution;
                }
            }

            suggestedSizes.put(useCaseConfig, resolution);
        }

        return suggestedSizes;
    }
}
