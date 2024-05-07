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

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;

/**
 * Use Surface directly to create the OutputConfiguration.
 */
@AutoValue
public abstract class SurfaceOutputConfig implements Camera2OutputConfig {
    /**
     * Creates the {@link SurfaceOutputConfig} instance.
     */
    static SurfaceOutputConfig create(int id, int surfaceGroupId,
            @Nullable String physicalCameraId,
            @NonNull List<Camera2OutputConfig> sharedOutputConfigs,
            @NonNull Surface surface) {
        return new AutoValue_SurfaceOutputConfig(id, surfaceGroupId, physicalCameraId,
                sharedOutputConfigs, surface);
    }

    static SurfaceOutputConfig create(int id, @NonNull Surface surface) {
        return create(id, -1, null, Collections.emptyList(), surface);
    }

    /**
     * Get the {@link Surface}. It'll return a valid surface only when type is TYPE_SURFACE.
     */
    @NonNull
    abstract Surface getSurface();
}
