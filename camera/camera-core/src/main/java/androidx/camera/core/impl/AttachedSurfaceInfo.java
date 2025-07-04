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

import androidx.camera.core.DynamicRange;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Container object for holding {@link SurfaceConfig} and its attributed ImageFormat,
 * {@link Size}, and target Frame Rate {@link Range}
 *
 */
@AutoValue
public abstract class AttachedSurfaceInfo {
    /** Prevent subclassing */
    AttachedSurfaceInfo() {
    }

    /**
     * Creates a new instance of SurfaceConfig with the given parameters.
     */
    public static @NonNull AttachedSurfaceInfo create(@NonNull SurfaceConfig surfaceConfig,
            int imageFormat,
            @NonNull Size size,
            @NonNull DynamicRange dynamicRange,
            @NonNull List<UseCaseConfigFactory.CaptureType> captureTypes,
            @Nullable Config implementationOptions,
            int sessionType,
            @NonNull Range<Integer> targetFrameRate,
            boolean isStrictFrameRateRequired) {
        return new AutoValue_AttachedSurfaceInfo(surfaceConfig, imageFormat, size,
                dynamicRange, captureTypes, implementationOptions, sessionType, targetFrameRate,
                isStrictFrameRateRequired);
    }

    /**
     * Obtains the StreamSpec from the given AttachedSurfaceInfo with the given
     * implementationOptions.
     */
    public @NonNull StreamSpec toStreamSpec(
            @NonNull Config implementationOptions) {
        StreamSpec.Builder streamSpecBuilder =
                StreamSpec.builder(getSize())
                        .setSessionType(getSessionType())
                        .setExpectedFrameRateRange(getTargetFrameRate())
                        .setDynamicRange(getDynamicRange())
                        .setImplementationOptions(implementationOptions);
        return streamSpecBuilder.build();
    }

    /** Returns the SurfaceConfig. */
    public abstract @NonNull SurfaceConfig getSurfaceConfig();

    /** Returns the configuration image format. */
    public abstract int getImageFormat();

    /** Returns the configuration size. */
    public abstract @NonNull Size getSize();

    /** Returns the dynamic range of this surface. */
    public abstract @NonNull DynamicRange getDynamicRange();

    /** Returns the capture types of this surface. Multiple capture types represent a
     *  {@link androidx.camera.core.streamsharing.StreamSharing} and its children.*/
    @SuppressWarnings("AutoValueImmutableFields")
    public abstract @NonNull List<UseCaseConfigFactory.CaptureType> getCaptureTypes();

    /** Returns the implementations of this surface. */
    public abstract @Nullable Config getImplementationOptions();

    /** Returns the session type. */
    public abstract int getSessionType();

    /** Returns the configuration target frame rate. */
    public abstract @NonNull Range<Integer> getTargetFrameRate();

    /** Returns whether strict frame rate is required. */
    public abstract boolean isStrictFrameRateRequired();
}


