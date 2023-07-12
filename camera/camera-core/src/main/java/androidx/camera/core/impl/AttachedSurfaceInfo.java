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
import androidx.camera.core.DynamicRange;

import com.google.auto.value.AutoValue;

import java.util.List;

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
            @NonNull DynamicRange dynamicRange,
            @NonNull List<UseCaseConfigFactory.CaptureType> captureTypes,
            @Nullable Config implementationOptions,
            @Nullable Range<Integer> targetFrameRate) {
        return new AutoValue_AttachedSurfaceInfo(surfaceConfig, imageFormat, size,
                dynamicRange, captureTypes, implementationOptions, targetFrameRate);
    }

    /** Returns the SurfaceConfig. */
    @NonNull
    public abstract SurfaceConfig getSurfaceConfig();

    /** Returns the configuration image format. */
    public abstract int getImageFormat();

    /** Returns the configuration size. */
    @NonNull
    public abstract Size getSize();

    /** Returns the dynamic range of this surface. */
    @NonNull
    public abstract DynamicRange getDynamicRange();

    /** Returns the capture types of this surface. Multiple capture types represent a
     *  {@link androidx.camera.core.streamsharing.StreamSharing} and its children.*/
    @SuppressWarnings("AutoValueImmutableFields")
    @NonNull
    public abstract List<UseCaseConfigFactory.CaptureType> getCaptureTypes();

    /** Returns the implementations of this surface. */
    @Nullable
    public abstract Config getImplementationOptions();

    /** Returns the configuration target frame rate. */
    @Nullable
    public abstract Range<Integer> getTargetFrameRate();
}


