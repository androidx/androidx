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

import androidx.annotation.Nullable;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.SurfaceConfig;
import androidx.camera.core.UseCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A CameraDeviceSurfaceManager which has no supported SurfaceConfigs. */
public class FakeCameraDeviceSurfaceManager implements CameraDeviceSurfaceManager {

    private static final Size MAX_OUTPUT_SIZE = new Size(0, 0);
    private static final Size PREVIEW_SIZE = new Size(1920, 1080);

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
            suggestedSizes.put(useCase, MAX_OUTPUT_SIZE);
        }

        return suggestedSizes;
    }

    @Override
    public Size getPreviewSize() {
        return PREVIEW_SIZE;
    }
}
