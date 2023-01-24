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

package androidx.camera.core.impl;

import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;
/**
 * Container object for holding {@link SurfaceConfig} and its attributed ImageFormat,
 * {@link Size}, and target Frame Rate {@link Range}
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class AttachedSurfaceInfo {
    /** Prevent subclassing */
    AttachedSurfaceInfo() {
    }

    /**
     * Creates a new instance of SurfaceConfig with the given parameters.
     */
    @NonNull
    public static AttachedSurfaceInfo create(@NonNull SurfaceConfig surfaceConfig,
            int imageFormat,
            @NonNull Size size,
            @Nullable Range<Integer> targetFrameRate) {
        return new AutoValue_AttachedSurfaceInfo(surfaceConfig, imageFormat, size, targetFrameRate);
    }

    /** Returns the SurfaceConfig. */
    @NonNull
    public abstract SurfaceConfig getSurfaceConfig();

    /** Returns the configuration image format. */
    public abstract int getImageFormat();

    /** Returns the configuration size. */
    @NonNull
    public abstract Size getSize();

    /** Returns the configuration target frame rate. */
    @Nullable
    public abstract Range<Integer> getTargetFrameRate();
}


