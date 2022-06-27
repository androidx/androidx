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

package androidx.camera.core.processing;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.Preview;

import com.google.auto.value.AutoValue;

/**
 * Options for how to produce a {@link SurfaceOut}.
 */
@AutoValue
public abstract class SurfaceOption {

    /**
     * The container class of the target.
     *
     * e.g. {@link Preview} or {@link android.media.MediaCodec}.
     */
    @NonNull
    public abstract Class<?> getTarget();

    /**
     * The format of the output {@link Surface}.
     *
     * <p> For GPU processing, it's always {@link ImageFormat#PRIVATE}.
     */
    public abstract int getFormat();

    // Below are transformation need to be performed by the implementer of the node.

    /**
     * Gets the crop rect.
     */
    @NonNull
    public abstract Rect getCropRect();

    /**
     * Gets the clockwise rotation degrees.
     */
    public abstract int getRotationDegrees();

    /**
     * Gets whether the buffer needs to be horizontally mirrored.
     */
    public abstract boolean getMirroring();

    /**
     * The target output size *after* the crop rect and rotation are applied.
     */
    @NonNull
    public abstract Size getSize();

    // Static utility method for creating instance.

    /**
     * Creates an instance of {@link SurfaceOption}.
     */
    @NonNull
    public static SurfaceOption create(@NonNull Class<?> target, int format,
            @NonNull Rect cropRect, int rotationDegrees, boolean mirroring, @NonNull Size size) {
        return new AutoValue_SurfaceOption(target, format, cropRect, rotationDegrees,
                mirroring, size);
    }
}
