/*
 * Copyright 2019 The Android Open Source Project
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

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.resolutionselector.ResolutionSelector;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration containing options for configuring the output image data of a pipeline.
 */
public interface ImageOutputConfig extends ReadableConfig {
    /**
     * Invalid integer rotation.
     */
    int INVALID_ROTATION = -1;

    /**
     * Rotation not specified.
     */
    int ROTATION_NOT_SPECIFIED = -1;

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.imageOutput.targetAspectRatio
     */
    Option<Integer> OPTION_TARGET_ASPECT_RATIO =
            Option.create("camerax.core.imageOutput.targetAspectRatio", AspectRatio.class);

    /**
     * Option: camerax.core.imageOutput.targetRotation
     */
    Option<Integer> OPTION_TARGET_ROTATION =
            Option.create("camerax.core.imageOutput.targetRotation", int.class);

    /**
     * Option: camerax.core.imageOutput.appTargetRotation
     */
    Option<Integer> OPTION_APP_TARGET_ROTATION =
            Option.create("camerax.core.imageOutput.appTargetRotation", int.class);

    /**
     * Option: camerax.core.imageOutput.mirrorMode
     */
    Option<Integer> OPTION_MIRROR_MODE =
            Option.create("camerax.core.imageOutput.mirrorMode", int.class);

    /**
     * Option: camerax.core.imageOutput.targetResolution
     */
    Option<Size> OPTION_TARGET_RESOLUTION =
            Option.create("camerax.core.imageOutput.targetResolution", Size.class);
    /**
     * Option: camerax.core.imageOutput.defaultResolution
     */
    Option<Size> OPTION_DEFAULT_RESOLUTION =
            Option.create("camerax.core.imageOutput.defaultResolution", Size.class);
    /**
     * Option: camerax.core.imageOutput.maxResolution
     */
    Option<Size> OPTION_MAX_RESOLUTION =
            Option.create("camerax.core.imageOutput.maxResolution", Size.class);
    /**
     * Option: camerax.core.imageOutput.supportedResolutions
     */
    Option<List<Pair<Integer, Size[]>>> OPTION_SUPPORTED_RESOLUTIONS =
            Option.create("camerax.core.imageOutput.supportedResolutions", List.class);

    /**
     * Option: camerax.core.imageOutput.resolutionSelector
     */
    Option<ResolutionSelector> OPTION_RESOLUTION_SELECTOR =
            Option.create("camerax.core.imageOutput.resolutionSelector", ResolutionSelector.class);

    /**
     * Option: camerax.core.imageOutput.customOrderedResolutions
     */
    Option<List<Size>> OPTION_CUSTOM_ORDERED_RESOLUTIONS =
            Option.create("camerax.core.imageOutput.customOrderedResolutions", List.class);

    // *********************************************************************************************

    /**
     * Verifies whether the aspect ratio of the target intending to use images from this
     * configuration is set.
     *
     * @return true is the value exists in this configuration, false otherwise.
     */
    default boolean hasTargetAspectRatio() {
        return containsOption(OPTION_TARGET_ASPECT_RATIO);
    }

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @AspectRatio.Ratio
    default int getTargetAspectRatio() {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO);
    }

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
    default int getTargetRotation(int valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ROTATION, valueIfMissing);
    }

    /**
     * Retrieves the target rotation set by app explicitly.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @RotationValue
    default int getAppTargetRotation(int valueIfMissing) {
        return retrieveOption(OPTION_APP_TARGET_ROTATION, valueIfMissing);
    }

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
    default int getTargetRotation() {
        return retrieveOption(OPTION_TARGET_ROTATION);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default Size getTargetResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the mirror mode of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in
     * this configuration.
     */
    @MirrorMode.Mirror
    default int getMirrorMode(int valueIfMissing) {
        return retrieveOption(ImageOutputConfig.OPTION_MIRROR_MODE, valueIfMissing);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default Size getTargetResolution() {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION);
    }

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default Size getDefaultResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default Size getDefaultResolution() {
        return retrieveOption(OPTION_DEFAULT_RESOLUTION);
    }

    /**
     * Retrieves the max resolution limitation of the target intending to use from this
     * configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default Size getMaxResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(OPTION_MAX_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the max resolution limitation of the target intending to use from this
     * configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default Size getMaxResolution() {
        return retrieveOption(OPTION_MAX_RESOLUTION);
    }

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
     */
    @Nullable
    default List<Pair<Integer, Size[]>> getSupportedResolutions(
            @Nullable List<Pair<Integer, Size[]>> valueIfMissing) {
        return retrieveOption(OPTION_SUPPORTED_RESOLUTIONS, valueIfMissing);
    }

    /**
     * Retrieves the resolution selector can be used by the target from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default ResolutionSelector getResolutionSelector(@Nullable ResolutionSelector valueIfMissing) {
        return retrieveOption(OPTION_RESOLUTION_SELECTOR, valueIfMissing);
    }

    /**
     * Retrieves the resolution selector can be used by the target from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default ResolutionSelector getResolutionSelector() {
        return retrieveOption(OPTION_RESOLUTION_SELECTOR);
    }

    /**
     * Retrieves the supported resolutions can be used by the target from this configuration.
     *
     * <p>Pair list is composed with {@link ImageFormat} and {@link Size} array. The returned
     * {@link Size} array should be subset of the complete supported sizes list for the camera
     * device.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return retrieveOption(OPTION_SUPPORTED_RESOLUTIONS);
    }

    /**
     * Retrieves the custom ordered resolutions can be used by the target from this configuration.
     *
     * <p>The returned {@link Size} list contains preferred priority from high to low and should
     * be the subset of the supported sizes for the camera device.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Nullable
    default List<Size> getCustomOrderedResolutions(@Nullable List<Size> valueIfMissing) {
        List<Size> list = retrieveOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, valueIfMissing);
        return list != null ? new ArrayList<>(list) : null;
    }

    /**
     * Retrieves the custom resolutions can be used by the target from this configuration.
     *
     * <p>The returned {@link Size} list contains preferred priority from high to low and should
     * be the subset of the supported sizes for the camera device.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default List<Size> getCustomOrderedResolutions() {
        return new ArrayList<>(requireNonNull(retrieveOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS)));
    }

    /**
     * Checks whether the input config contains any conflicted settings.
     *
     * @param config to be validated.
     * @throws IllegalArgumentException if both the target aspect ratio and the target resolution
     * settings are contained in the config, or if either the target aspect ratio or the target
     * resolution is contained when a resolution selector has been set in the config.
     */
    static void validateConfig(@NonNull ImageOutputConfig config) {
        boolean hasTargetAspectRatio = config.hasTargetAspectRatio();
        boolean hasTargetResolution = config.getTargetResolution(null) != null;

        // Case 1. Error at runtime for using both setTargetResolution and setTargetAspectRatio on
        // the same config.
        if (hasTargetAspectRatio && hasTargetResolution) {
            throw new IllegalArgumentException(
                    "Cannot use both setTargetResolution and setTargetAspectRatio on the same "
                            + "config.");
        }

        ResolutionSelector resolutionSelector = config.getResolutionSelector(null);

        if (resolutionSelector != null) {
            // Case 2. Error at runtime for using setTargetResolution or setTargetAspectRatio
            // with setResolutionSelector on the same config.
            if (hasTargetAspectRatio || hasTargetResolution) {
                throw new IllegalArgumentException(
                        "Cannot use setTargetResolution or setTargetAspectRatio with "
                                + "setResolutionSelector on the same config.");
            }
        }
    }

    /**
     * Builder for a {@link ImageOutputConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     */
    interface Builder<B> {

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
         * Sets the mirror mode of the intended target for images from this configuration.
         *
         * <p>Valid values include: {@link MirrorMode#MIRROR_MODE_OFF},
         * {@link MirrorMode#MIRROR_MODE_ON} and {@link MirrorMode#MIRROR_MODE_ON_FRONT_ONLY}.
         *
         * @param mirrorMode The mirror mode of the intended target.
         * @return The current Builder.
         */
        @NonNull
        B setMirrorMode(@MirrorMode.Mirror int mirrorMode);

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
         */
        @NonNull
        B setTargetResolution(@NonNull Size resolution);

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @NonNull
        B setDefaultResolution(@NonNull Size resolution);

        /**
         * Sets the max resolution limitation of the intended target from this configuration.
         *
         * @param resolution The max resolution limitation to choose from supported output sizes
         *                   list.
         * @return The current Builder.
         */
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
         */
        @NonNull
        B setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutionsList);

        /**
         * Sets the custom resolutions can be used by target from this configuration.
         *
         * <p>The {@link Size} list should contain preferred priority from high to low and should
         * be the subset of the supported sizes for the camera device. The list will be fully
         * respected, meaning it will not be sorted or filtered by other configurations such as
         * max/default/target/supported resolutions.
         *
         * @param resolutionsList The resolutions can be supported for this image config.
         * @return The current Builder.
         */
        @NonNull
        B setCustomOrderedResolutions(@NonNull List<Size> resolutionsList);

        /**
         * Sets the resolution selector can be used by target from this configuration.
         *
         * @param resolutionSelector The resolution selector to select a preferred resolution.
         * @return The current Builder.
         */
        @NonNull
        B setResolutionSelector(@NonNull ResolutionSelector resolutionSelector);
    }

    /**
     * Valid integer rotation values.
     */
    @IntDef({Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270})
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationValue {
    }

    /**
     * Valid integer rotation values including option indicating value not specified.
     */
    @IntDef({ROTATION_NOT_SPECIFIED, Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180,
            Surface.ROTATION_270})
    @Retention(RetentionPolicy.SOURCE)
    @interface OptionalRotationValue {
    }

    /**
     * Valid integer rotation degrees values.
     */
    @IntDef({0, 90, 180, 270})
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationDegreesValue {
    }
}
