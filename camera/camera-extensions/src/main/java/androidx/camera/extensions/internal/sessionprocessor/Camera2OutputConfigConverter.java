/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor;

import androidx.annotation.NonNull;
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl;
import androidx.camera.extensions.impl.advanced.ImageReaderOutputConfigImpl;
import androidx.camera.extensions.impl.advanced.MultiResolutionImageReaderOutputConfigImpl;
import androidx.camera.extensions.impl.advanced.SurfaceOutputConfigImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * For converting a {@link Camera2OutputConfigImpl} to a {@link Camera2OutputConfig}.
 */
class Camera2OutputConfigConverter {
    private Camera2OutputConfigConverter() {
    }

    /**
     * Create a {@link Camera2OutputConfig} from the {@link Camera2OutputConfigImpl}.
     */
    @NonNull
    static Camera2OutputConfig fromImpl(@NonNull Camera2OutputConfigImpl impl) {
        List<Camera2OutputConfig> sharedOutputConfigs = new ArrayList<>();
        if (impl.getSurfaceSharingOutputConfigs() != null) {
            for (Camera2OutputConfigImpl surfaceSharingOutputConfig :
                    impl.getSurfaceSharingOutputConfigs()) {
                Camera2OutputConfig sharedConfig = fromImpl(surfaceSharingOutputConfig);
                sharedOutputConfigs.add(sharedConfig);
            }
        }

        // Workaround to avoid the R8 issue (b/350689668)
        // Force cast the input Camera2OutputConfigImpl instance to all possible types in sequence
        // to know what is its real type.
        try {
            SurfaceOutputConfigImpl surfaceImpl = (SurfaceOutputConfigImpl) impl;
            return SurfaceOutputConfig.create(
                    surfaceImpl.getId(),
                    surfaceImpl.getSurfaceGroupId(),
                    surfaceImpl.getPhysicalCameraId(),
                    sharedOutputConfigs,
                    surfaceImpl.getSurface());
        } catch (ClassCastException e) {
            // Not type of SurfaceOutputConfigImpl! Go to try the next type.
        }

        try {
            ImageReaderOutputConfigImpl imageReaderImpl = (ImageReaderOutputConfigImpl) impl;
            return ImageReaderOutputConfig.create(
                    imageReaderImpl.getId(),
                    imageReaderImpl.getSurfaceGroupId(),
                    imageReaderImpl.getPhysicalCameraId(),
                    sharedOutputConfigs,
                    imageReaderImpl.getSize(),
                    imageReaderImpl.getImageFormat(),
                    imageReaderImpl.getMaxImages());
        } catch (ClassCastException e) {
            // Not type of ImageReaderOutputConfigImpl! Go to try the next type.
        }

        try {
            MultiResolutionImageReaderOutputConfigImpl multiResolutionImageReaderImpl =
                    (MultiResolutionImageReaderOutputConfigImpl) impl;
            return MultiResolutionImageReaderOutputConfig.create(
                    multiResolutionImageReaderImpl.getId(),
                    multiResolutionImageReaderImpl.getSurfaceGroupId(),
                    multiResolutionImageReaderImpl.getPhysicalCameraId(),
                    sharedOutputConfigs,
                    multiResolutionImageReaderImpl.getImageFormat(),
                    multiResolutionImageReaderImpl.getMaxImages());
        } catch (ClassCastException e) {
            // Not type of MultiResolutionImageReaderOutputConfigImpl!
        }

        throw new IllegalArgumentException(
                "Not supported Camera2OutputConfigImpl: " + impl.getClass());
    }
}
