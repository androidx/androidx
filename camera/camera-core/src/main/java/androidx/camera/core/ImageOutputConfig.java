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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.Config.Option;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Configuration containing options for configuring the output image data of a pipeline.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface ImageOutputConfig {
    /**
     * Invalid integer rotation.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    int INVALID_ROTATION = -1;

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.imageOutput.targetAspectRatioCustom
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Rational> OPTION_TARGET_ASPECT_RATIO_CUSTOM =
            Option.create("camerax.core.imageOutput.targetAspectRatioCustom", Rational.class);

    /**
     * Option: camerax.core.imageOutput.targetAspectRatio
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Integer> OPTION_TARGET_ASPECT_RATIO =
            Option.create("camerax.core.imageOutput.targetAspectRatio", AspectRatio.class);

    /**
     * Option: camerax.core.imageOutput.targetRotation
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Integer> OPTION_TARGET_ROTATION =
            Option.create("camerax.core.imageOutput.targetRotation", int.class);
    /**
     * Option: camerax.core.imageOutput.targetResolution
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Size> OPTION_TARGET_RESOLUTION =
            Option.create("camerax.core.imageOutput.targetResolution", Size.class);
    /**
     * Option: camerax.core.imageOutput.defaultResolution
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Size> OPTION_DEFAULT_RESOLUTION =
            Option.create("camerax.core.imageOutput.defaultResolution", Size.class);
    /**
     * Option: camerax.core.imageOutput.maxResolution
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Size> OPTION_MAX_RESOLUTION =
            Option.create("camerax.core.imageOutput.maxResolution", Size.class);
    /**
     * Option: camerax.core.imageOutput.supportedResolutions
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<List<Pair<Integer, Size[]>>> OPTION_SUPPORTED_RESOLUTIONS =
            Option.create("camerax.core.imageOutput.supportedResolutions", List.class);

    // *********************************************************************************************

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * <p>This is the ratio of the target's width to the image's height, where the numerator of the
     * provided {@link Rational} corresponds to the width, and the denominator corresponds to the
     * height.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    Rational getTargetAspectRatioCustom(@Nullable Rational valueIfMissing);

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * <p>This is the ratio of the target's width to the image's height, where the numerator of the
     * provided {@link Rational} corresponds to the width, and the denominator corresponds to the
     * height.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    Rational getTargetAspectRatioCustom();

    /**
     * Verifies whether the aspect ratio of the target intending to use images from this
     * configuration is set.
     *
     * @return true is the value exists in this configuration, false otherwise.
     */
    boolean hasTargetAspectRatio();

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @AspectRatio.Ratio
    int getTargetAspectRatio();

    /**
     * Retrieves the rotation of the target intending to use images from this configuration.
     *
     * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}. Rotation values are relative to
     * the device's "natural" rotation, {@link Surface#ROTATION_0}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @RotationValue
    int getTargetRotation(int valueIfMissing);

    /**
     * Retrieves the rotation of the target intending to use images from this configuration.
     *
     * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     * {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}. Rotation values are relative to
     * the device's "natural" rotation, {@link Surface#ROTATION_0}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @RotationValue
    int getTargetRotation();

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Size getTargetResolution(@Nullable Size valueIfMissing);

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    Size getTargetResolution();

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Size getDefaultResolution(@Nullable Size valueIfMissing);

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    Size getDefaultResolution();

    /**
     * Retrieves the max resolution limitation of the target intending to use from this
     * configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    Size getMaxResolution(@Nullable Size valueIfMissing);

    /**
     * Retrieves the max resolution limitation of the target intending to use from this
     * configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    Size getMaxResolution();

    /**
     * Retrieves the supported resolutions can be used by the target from this configuration.
     *
     * <p>Pair list is composed with {@link ImageFormat} and {@link Size} array. The returned
     * {@link Size} array should be subset of the complete supported sizes list for the camera
     * device.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    List<Pair<Integer, Size[]>> getSupportedResolutions(
            @Nullable List<Pair<Integer, Size[]>> valueIfMissing);

    /**
     * Retrieves the supported resolutions can be used by the target from this configuration.
     *
     * <p>Pair list is composed with {@link ImageFormat} and {@link Size} array. The returned
     * {@link Size} array should be subset of the complete supported sizes list for the camera
     * device.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    List<Pair<Integer, Size[]>> getSupportedResolutions();

    /**
     * Builder for a {@link ImageOutputConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder<B> {

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>This is the ratio of the target's width to the image's height, where the numerator of
         * the provided {@link Rational} corresponds to the width, and the denominator corresponds
         * to the height.
         *
         * <p>This method can be used to request an aspect ratio that is not from the standard set
         * of aspect ratios defined in the {@link AspectRatio}.
         *
         * <p>This method will remove any value set by setTargetAspectRatio().
         *
         * @param aspectRatio A {@link Rational} representing the ratio of the target's width and
         *                    height.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        B setTargetAspectRatioCustom(@NonNull Rational aspectRatio);

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case.
         *
         * @param aspectRatio A {@link AspectRatio} representing the ratio of the
         *                    target's width and height.
         * @return The current Builder.
         */
        @NonNull
        B setTargetAspectRatio(@AspectRatio.Ratio int aspectRatio);

        /**
         * Sets the rotation of the intended target for images from this configuration.
         *
         * <p>This is one of four valid values: {@link Surface#ROTATION_0}, {@link
         * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
         * Rotation values are relative to the "natural" rotation, {@link Surface#ROTATION_0}.
         *
         * @param rotation The rotation of the intended target.
         * @return The current Builder.
         */
        @NonNull
        B setTargetRotation(@RotationValue int rotation);

        /**
         * Sets the resolution of the intended target from this configuration.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case.
         *
         * <p>The target aspect ratio will also be set the same as the aspect ratio of the provided
         * {@link Size}. Make sure to set the target resolution with the correct orientation.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        B setTargetResolution(@NonNull Size resolution);

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        B setDefaultResolution(@NonNull Size resolution);

        /**
         * Sets the max resolution limitation of the intended target from this configuration.
         *
         * @param resolution The max resolution limitation to choose from supported output sizes
         *                   list.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        B setMaxResolution(@NonNull Size resolution);

        /**
         * Sets the supported resolutions can be used by target from this configuration.
         *
         * <p>Pair list is composed with {@link ImageFormat} and {@link Size} array. The
         * {@link Size} array should be subset of the complete supported sizes list for the camera
         * device.
         *
         * @param resolutionsList The resolutions can be supported for image formats.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        B setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutionsList);
    }

    /**
     * Valid integer rotation values.
     *
     * @hide
     */
    @IntDef({Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270})
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationValue {
    }
}
