/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.processing.util;

import static androidx.camera.core.impl.utils.TransformUtils.getRotatedSize;

import static java.util.UUID.randomUUID;

import android.graphics.Rect;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.UseCase;
import androidx.camera.core.processing.SurfaceEdge;

import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.UUID;

/**
 * Configuration of how to create an output stream from an input stream.
 *
 * <p>The value in this class will override the corresponding value in the
 * {@link SurfaceEdge} class. The override is necessary when a single stream is shared
 * to multiple output streams with different transformations. For example, if a single 4:3
 * preview stream is shared to a 16:9 video stream, the video stream must override the crop
 * rect.
 */
@AutoValue
public abstract class OutConfig {

    /**
     * Unique ID of the config.
     *
     * <p> This is for making sure two {@link OutConfig} with the same value can be stored as
     * different keys in a {@link HashMap}.
     */
    @NonNull
    abstract UUID getUuid();

    /**
     * The target {@link UseCase} of the output stream.
     */
    @CameraEffect.Targets
    public abstract int getTargets();

    /**
     * The format of the output stream.
     */
    @CameraEffect.Formats
    public abstract int getFormat();

    /**
     * How the input should be cropped.
     */
    @NonNull
    public abstract Rect getCropRect();

    /**
     * The stream should scale to this size after cropping and rotating.
     *
     * <p>The input stream should be scaled to match this size after cropping and rotating
     */
    @NonNull
    public abstract Size getSize();

    /**
     * How the input should be rotated clockwise.
     */
    public abstract int getRotationDegrees();

    /**
     * Whether the stream should be mirrored.
     */
    public abstract boolean isMirroring();

    /**
     * Whether the node should respect the input's crop rect.
     *
     * <p>If true, the output's crop rect will be calculated based
     * {@link OutConfig#getCropRect()} AND the input's crop rect. In this case, the
     * {@link OutConfig#getCropRect()} must contain the input's crop rect. This applies to
     * the scenario where the input crop rect is valid but the current node cannot apply crop
     * rect. For example, when
     * {@link CameraEffect#TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION} option is used.
     *
     * <p>If false, then the node will override input's crop rect with
     * {@link OutConfig#getCropRect()}. This mostly applies to the sharing node. For example,
     * the children want to crop the input stream to different sizes, in which case, the
     * input crop rect is invalid.
     */
    public abstract boolean shouldRespectInputCropRect();

    /**
     * Creates an {@link OutConfig} instance from the input edge.
     *
     * <p>The result is an output edge with the input's transformation applied.
     */
    @NonNull
    public static OutConfig of(@NonNull SurfaceEdge inputEdge) {
        return of(inputEdge.getTargets(),
                inputEdge.getFormat(),
                inputEdge.getCropRect(),
                getRotatedSize(inputEdge.getCropRect(), inputEdge.getRotationDegrees()),
                inputEdge.getRotationDegrees(),
                inputEdge.isMirroring());
    }

    /**
     * Creates an {@link OutConfig} instance with custom transformations.
     *
     * // TODO: remove this method and make the shouldRespectInputCropRect bit explicit.
     */
    @NonNull
    public static OutConfig of(@CameraEffect.Targets int targets,
            @CameraEffect.Formats int format,
            @NonNull Rect cropRect,
            @NonNull Size size,
            int rotationDegrees,
            boolean mirroring) {
        return of(targets, format, cropRect, size, rotationDegrees, mirroring,
                /*shouldRespectInputCropRect=*/false);
    }

    /**
     * Creates an {@link OutConfig} instance with custom transformations.
     */
    @NonNull
    public static OutConfig of(@CameraEffect.Targets int targets,
            @CameraEffect.Formats int format,
            @NonNull Rect cropRect,
            @NonNull Size size,
            int rotationDegrees,
            boolean mirroring,
            boolean shouldRespectInputCropRect) {
        return new AutoValue_OutConfig(randomUUID(), targets, format,
                cropRect, size, rotationDegrees, mirroring, shouldRespectInputCropRect);
    }
}
