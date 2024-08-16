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

import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExtendableBuilder;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.imagecapture.ImageCaptureControl;
import androidx.camera.core.imagecapture.TakePictureManager;
import androidx.camera.core.imagecapture.TakePictureManagerImpl;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.internal.TargetConfig;

import java.util.Objects;

/**
 * Configuration containing options for use cases.
 *
 * @param <T> The use case being configured.
 */
public interface UseCaseConfig<T extends UseCase> extends TargetConfig<T>, ImageInputConfig {
    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.useCase.defaultSessionConfig
     */
    Option<SessionConfig> OPTION_DEFAULT_SESSION_CONFIG =
            Option.create("camerax.core.useCase.defaultSessionConfig", SessionConfig.class);

    /**
     * Option: camerax.core.useCase.defaultCaptureConfig
     */
    Option<CaptureConfig> OPTION_DEFAULT_CAPTURE_CONFIG =
            Option.create("camerax.core.useCase.defaultCaptureConfig", CaptureConfig.class);

    /**
     * Option: camerax.core.useCase.sessionConfigUnpacker
     *
     * <p>TODO(b/120949879): This may be removed when SessionConfig removes all camera2
     * dependencies.
     */
    Option<SessionConfig.OptionUnpacker> OPTION_SESSION_CONFIG_UNPACKER =
            Option.create("camerax.core.useCase.sessionConfigUnpacker",
                    SessionConfig.OptionUnpacker.class);

    /**
     * Option: camerax.core.useCase.captureConfigUnpacker
     *
     * <p>TODO(b/120949879): This may be removed when CaptureConfig removes all camera2
     * dependencies.
     */
    Option<CaptureConfig.OptionUnpacker> OPTION_CAPTURE_CONFIG_UNPACKER =
            Option.create("camerax.core.useCase.captureConfigUnpacker",
                    CaptureConfig.OptionUnpacker.class);

    /**
     * Option: camerax.core.useCase.surfaceOccypyPriority
     */
    Option<Integer> OPTION_SURFACE_OCCUPANCY_PRIORITY =
            Option.create("camerax.core.useCase.surfaceOccupancyPriority", int.class);

    /**
     * Option: camerax.core.useCase.targetFrameRate
     */
    Option<Range<Integer>> OPTION_TARGET_FRAME_RATE =
            Config.Option.create("camerax.core.useCase.targetFrameRate", Range.class);

    /**
     * Option: camerax.core.useCase.zslDisabled
     */
    Option<Boolean> OPTION_ZSL_DISABLED =
            Option.create("camerax.core.useCase.zslDisabled", boolean.class);

    /**
     * Option: camerax.core.useCase.highResolutionDisabled
     */
    Option<Boolean> OPTION_HIGH_RESOLUTION_DISABLED =
            Option.create("camerax.core.useCase.highResolutionDisabled", boolean.class);

    /**
     * Option: camerax.core.useCase.highResolutionDisabled
     */
    Option<UseCaseConfigFactory.CaptureType> OPTION_CAPTURE_TYPE = Option.create(
            "camerax.core.useCase.captureType", UseCaseConfigFactory.CaptureType.class);

    /**
     * Option: camerax.core.useCase.previewStabilizationMode
     */
    Option<Integer> OPTION_PREVIEW_STABILIZATION_MODE =
            Option.create("camerax.core.useCase.previewStabilizationMode", int.class);

    /**
     * Option: camerax.core.useCase.videoStabilizationMode
     */
    Option<Integer> OPTION_VIDEO_STABILIZATION_MODE =
            Option.create("camerax.core.useCase.videoStabilizationMode", int.class);

    Option<TakePictureManager.Provider> OPTION_TAKE_PICTURE_MANAGER_PROVIDER =
            Option.create("camerax.core.useCase.takePictureManagerProvider",
                    TakePictureManager.Provider.class);

    // *********************************************************************************************

    /**
     * Retrieves the default session configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's session configuration with default
     * values.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default SessionConfig getDefaultSessionConfig(@Nullable SessionConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    /**
     * Retrieves the default session configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's session configuration with default
     * values.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default SessionConfig getDefaultSessionConfig() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    /**
     * Retrieves the default capture configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's capture configuration with default
     * values.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default CaptureConfig getDefaultCaptureConfig(@Nullable CaptureConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG, valueIfMissing);
    }

    /**
     * Retrieves the default capture configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's capture configuration with default
     * values.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default CaptureConfig getDefaultCaptureConfig() {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG);
    }

    /**
     * Retrieves the {@link SessionConfig.OptionUnpacker} for this use case.
     *
     * <p>This unpacker is used to initialize the use case's session configuration.
     *
     * <p>TODO(b/120949879): This may be removed when SessionConfig removes all camera2
     * dependencies.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default SessionConfig.OptionUnpacker getSessionOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER, valueIfMissing);
    }

    /**
     * Retrieves the {@link SessionConfig.OptionUnpacker} for this use case.
     *
     * <p>This unpacker is used to initialize the use case's session configuration.
     *
     * <p>TODO(b/120949879): This may be removed when SessionConfig removes all camera2
     * dependencies.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default SessionConfig.OptionUnpacker getSessionOptionUnpacker() {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER);
    }

    /**
     * Retrieves the {@link CaptureConfig.OptionUnpacker} for this use case.
     *
     * <p>This unpacker is used to initialize the use case's capture configuration.
     *
     * <p>TODO(b/120949879): This may be removed when CaptureConfig removes all camera2
     * dependencies.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default CaptureConfig.OptionUnpacker getCaptureOptionUnpacker(
            @Nullable CaptureConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER, valueIfMissing);
    }

    /**
     * Retrieves the {@link CaptureConfig.OptionUnpacker} for this use case.
     *
     * <p>This unpacker is used to initialize the use case's capture configuration.
     *
     * <p>TODO(b/120949879): This may be removed when CaptureConfig removes all camera2
     * dependencies.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default CaptureConfig.OptionUnpacker getCaptureOptionUnpacker() {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER);
    }

    /**
     * Retrieves the surface occupancy priority of the target intending to use from this
     * configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    default int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    /**
     * Retrieves the surface occupancy priority of the target intending to use from this
     * configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    default int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    /**
     * Retrieves target frame rate
     * @param valueIfMissing
     * @return the stored value or <code>valueIfMissing</code> if the value does not exist in
     * this configuration
     */
    @Nullable
    default Range<Integer> getTargetFrameRate(@Nullable Range<Integer> valueIfMissing) {
        return retrieveOption(OPTION_TARGET_FRAME_RATE, valueIfMissing);
    }

    /**
     * Retrieves the target frame rate
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default Range<Integer> getTargetFrameRate() {
        return retrieveOption(OPTION_TARGET_FRAME_RATE);
    }

    /**
     * Retrieves the flag whether zero-shutter lag is disabled.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in
     * this configuration
     */
    default boolean isZslDisabled(boolean valueIfMissing) {
        return retrieveOption(OPTION_ZSL_DISABLED, valueIfMissing);
    }

    /**
     * Retrieves the flag whether high resolution is disabled.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in
     * this configuration
     */
    default boolean isHighResolutionDisabled(boolean valueIfMissing) {
        return retrieveOption(OPTION_HIGH_RESOLUTION_DISABLED, valueIfMissing);
    }

    /**
     * @return The {@link UseCaseConfigFactory.CaptureType} of this UseCaseConfig.
     */
    @NonNull
    default UseCaseConfigFactory.CaptureType getCaptureType() {
        return retrieveOption(OPTION_CAPTURE_TYPE);
    }

    /**
     * @return The preview stabilization mode of this UseCaseConfig.
     */
    @StabilizationMode.Mode
    default int getPreviewStabilizationMode() {
        return retrieveOption(OPTION_PREVIEW_STABILIZATION_MODE,
                StabilizationMode.UNSPECIFIED);
    }

    /**
     * @return The video stabilization mode of this UseCaseConfig.
     */
    @StabilizationMode.Mode
    default int getVideoStabilizationMode() {
        return retrieveOption(OPTION_VIDEO_STABILIZATION_MODE, StabilizationMode.UNSPECIFIED);
    }

    /**
     * @return The {@link TakePictureManager} implementation for {@link ImageCapture} use case.
     */
    @NonNull
    default TakePictureManager.Provider getTakePictureManagerProvider() {
        return Objects.requireNonNull(retrieveOption(OPTION_TAKE_PICTURE_MANAGER_PROVIDER,
                new TakePictureManager.Provider() {
                    @NonNull
                    @Override
                    public TakePictureManager newInstance(
                            @NonNull ImageCaptureControl imageCaptureControl) {
                        return new TakePictureManagerImpl(imageCaptureControl);
                    }
                }));
    }

    /**
     * Builder for a {@link UseCase}.
     *
     * @param <T> The type of the object which will be built by {@link #build()}.
     * @param <C> The top level configuration which will be generated by
     *            {@link #getUseCaseConfig()}.
     * @param <B> The top level builder type for which this builder is composed with.
     */
    interface Builder<T extends UseCase, C extends UseCaseConfig<T>, B> extends
            TargetConfig.Builder<T, B>, ExtendableBuilder<T> {

        /**
         * Sets the default session configuration for this use case.
         *
         * @param sessionConfig The default session configuration to use for this use case.
         * @return the current Builder.
         */
        @NonNull
        B setDefaultSessionConfig(@NonNull SessionConfig sessionConfig);

        /**
         * Sets the default capture configuration for this use case.
         *
         * @param captureConfig The default capture configuration to use for this use case.
         * @return the current Builder.
         */
        @NonNull
        B setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig);

        /**
         * Sets the Option Unpacker for translating this configuration into a {@link SessionConfig}
         *
         * <p>TODO(b/120949879): This may be removed when SessionConfig removes all camera2
         * dependencies.
         *
         * @param optionUnpacker The option unpacker for to use for this use case.
         * @return the current Builder.
         */
        @NonNull
        B setSessionOptionUnpacker(@NonNull SessionConfig.OptionUnpacker optionUnpacker);

        /**
         * Sets the Option Unpacker for translating this configuration into a {@link CaptureConfig}
         *
         * <p>TODO(b/120949879): This may be removed when CaptureConfig removes all camera2
         * dependencies.
         *
         * @param optionUnpacker The option unpacker for to use for this use case.
         * @return the current Builder.
         */
        @NonNull
        B setCaptureOptionUnpacker(@NonNull CaptureConfig.OptionUnpacker optionUnpacker);

        /**
         * Sets the surface occupancy priority of the intended target from this configuration.
         *
         * <p>The stream resource of {@link android.hardware.camera2.CameraDevice} is limited. When
         * one use case occupies a larger stream resource, it will impact the other use cases to get
         * smaller stream resource. Use this to determine which use case can have higher priority to
         * occupancy stream resource first.
         *
         * @param priority The priority to occupancy the available stream resource. Higher value
         *                 will have higher priority.
         * @return The current Builder.
         */
        @NonNull
        B setSurfaceOccupancyPriority(int priority);

        /**
         * Sets zsl disabled or not.
         *
         * <p> Zsl will be disabled when any of the following conditions:
         * <ul>
         *     <li> Extension is ON
         *     <li> Flash mode is ON or AUTO
         *     <li> VideoCapture is ON
         * </ul>
         *
         * @param disabled True if zero-shutter lag should be disabled. Otherwise, should not be
         *                 disabled. However, enabling zero-shutter lag needs other conditions e.g.
         *                 flash mode OFF, so setting to false doesn't guarantee zero-shutter lag to
         *                 be always ON.
         */
        @NonNull
        B setZslDisabled(boolean disabled);

        /**
         * Sets high resolution disabled or not.
         *
         * <p> High resolution will be disabled when Extension is ON.
         *
         * @param disabled True if high resolution should be disabled. Otherwise, should not be
         *                 disabled.
         */
        @NonNull
        B setHighResolutionDisabled(boolean disabled);

        /**
         * Sets the capture type for this configuration.
         *
         * @param captureType The capture type for this use case.
         */
        @NonNull
        B setCaptureType(@NonNull UseCaseConfigFactory.CaptureType captureType);

        /**
         * Retrieves the configuration used by this builder.
         *
         * @return the configuration used by this builder.
         */
        @NonNull
        C getUseCaseConfig();
    }
}
