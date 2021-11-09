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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExtendableBuilder;
import androidx.camera.core.UseCase;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.UseCaseEventConfig;

/**
 * Configuration containing options for use cases.
 *
 * @param <T> The use case being configured.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface UseCaseConfig<T extends UseCase> extends TargetConfig<T>, UseCaseEventConfig,
        ImageInputConfig {
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
     * Option: camerax.core.useCase.cameraSelector
     */
    Option<CameraSelector> OPTION_CAMERA_SELECTOR =
            Config.Option.create("camerax.core.useCase.cameraSelector", CameraSelector.class);

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
     * Retrieves the camera selector that this use case requires.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default CameraSelector getCameraSelector(@Nullable CameraSelector valueIfMissing) {
        return retrieveOption(OPTION_CAMERA_SELECTOR, valueIfMissing);
    }

    /**
     * Retrieves the camera selector that this use case requires.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default CameraSelector getCameraSelector() {
        return retrieveOption(OPTION_CAMERA_SELECTOR);
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
            TargetConfig.Builder<T, B>, ExtendableBuilder<T>, UseCaseEventConfig.Builder<B> {

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
         * Sets the camera selector that this use case requires.
         *
         * @param cameraSelector The camera filter appended internally.
         * @return The current Builder.
         */
        @NonNull
        B setCameraSelector(@NonNull CameraSelector cameraSelector);

        /**
         * Retrieves the configuration used by this builder.
         *
         * @return the configuration used by this builder.
         */
        @NonNull
        C getUseCaseConfig();
    }
}
