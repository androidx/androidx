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

import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.ImageOutputConfig;
import androidx.camera.core.SurfaceConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A CameraDeviceSurfaceManager which has no supported SurfaceConfigs. */
public final class FakeCameraDeviceSurfaceManager implements CameraDeviceSurfaceManager {

    private static final Size MAX_OUTPUT_SIZE = new Size(4032, 3024); // 12.2 MP
    private static final Size PREVIEW_SIZE = new Size(1920, 1080);

    private Map<String, Map<Class<? extends UseCase>, Size>> mDefinedResolutions = new HashMap<>();

    /**
     * Sets the given suggested resolutions for the specified camera Id and use case type.
     */
    public void setSuggestedResolution(String cameraId, Class<? extends UseCase> type, Size size) {
        Map<Class<? extends UseCase>, Size> useCaseTypeToSizeMap =
                mDefinedResolutions.get(cameraId);
        if (useCaseTypeToSizeMap == null) {
            useCaseTypeToSizeMap = new HashMap<>();
            mDefinedResolutions.put(cameraId, useCaseTypeToSizeMap);
        }

        useCaseTypeToSizeMap.put(type, size);
    }

    @Override
    public boolean checkSupported(String cameraId, List<SurfaceConfig> surfaceConfigList) {
        return false;
    }

    @Override
    public SurfaceConfig transformSurfaceConfig(String cameraId, int imageFormat, Size size) {
        return null;
    }

    @Nullable
    @Override
    public Size getMaxOutputSize(String cameraId, int imageFormat) {
        return MAX_OUTPUT_SIZE;
    }

    @Override
    public Map<UseCase, Size> getSuggestedResolutions(
            String cameraId, List<UseCase> originalUseCases, List<UseCase> newUseCases) {
        Map<UseCase, Size> suggestedSizes = new HashMap<>();
        for (UseCase useCase : newUseCases) {
            Size resolution = MAX_OUTPUT_SIZE;
            Map<Class<? extends UseCase>, Size> definedResolutions =
                    mDefinedResolutions.get(cameraId);
            if (definedResolutions != null) {
                Size definedResolution = definedResolutions.get(useCase.getClass());
                if (definedResolution != null) {
                    resolution = definedResolution;
                }
            }

            suggestedSizes.put(useCase, resolution);
        }

        return suggestedSizes;
    }

    @Override
    public Size getPreviewSize() {
        return PREVIEW_SIZE;
    }

    @Override
    public boolean requiresCorrectedAspectRatio(@NonNull UseCaseConfig<?> useCaseConfig) {
        return true;
    }

    @Nullable
    @Override
    public Rational getCorrectedAspectRatio(@NonNull UseCaseConfig<?> useCaseConfig) {
        ImageOutputConfig config = (ImageOutputConfig) useCaseConfig;
        Rational aspectRatio = config.getTargetAspectRatio(null);
        return aspectRatio;
    }
}
