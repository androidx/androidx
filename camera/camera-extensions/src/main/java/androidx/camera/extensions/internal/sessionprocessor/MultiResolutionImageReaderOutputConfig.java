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

package androidx.camera.extensions.internal.sessionprocessor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.List;

/**
 * Surface will be created by constructing a MultiResolutionImageReader.
 */
@AutoValue
public abstract class MultiResolutionImageReaderOutputConfig implements Camera2OutputConfig {
    /**
     * Creates the {@link MultiResolutionImageReaderOutputConfig} instance.
     */
    static MultiResolutionImageReaderOutputConfig create(
            int id, int surfaceGroupId, @Nullable String physicalCameraId,
            @NonNull List<Camera2OutputConfig> sharedOutputConfigs,
            int imageFormat, int maxImages) {
        return new AutoValue_MultiResolutionImageReaderOutputConfig(id, surfaceGroupId,
                physicalCameraId,
                sharedOutputConfigs, imageFormat, maxImages);
    }

    /**
     * Gets the image format of the surface.
     */
    abstract int getImageFormat();

    /**
     * Gets the capacity for the image reader.
     */
    abstract int getMaxImages();
}
