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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Configuration containing options for use cases.
 *
 * @param <T> The use case being configured.
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface UseCaseConfig<T extends UseCase> extends TargetConfig<T>, Config,
        UseCaseEventConfig {
    // Option Declarations:
    // *********************************************************************************************

    /**
     * Option: camerax.core.useCase.defaultSessionConfig
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<SessionConfig> OPTION_DEFAULT_SESSION_CONFIG =
            Option.create("camerax.core.useCase.defaultSessionConfig", SessionConfig.class);
    /**
     * Option: camerax.core.useCase.defaultCaptureConfig
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<CaptureConfig> OPTION_DEFAULT_CAPTURE_CONFIG =
            Option.create("camerax.core.useCase.defaultCaptureConfig", CaptureConfig.class);
    /**
     * Option: camerax.core.useCase.sessionConfigUnpacker
     *
     * <p>TODO(b/120949879): This may be removed when SessionConfig removes all camera2
     * dependencies.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<SessionConfig.OptionUnpacker> OPTION_SESSION_CONFIG_UNPACKER =
            Option.create("camerax.core.useCase.sessionConfigUnpacker",
                    SessionConfig.OptionUnpacker.class);
    /**
     * Option: camerax.core.useCase.captureConfigUnpacker
     *
     * <p>TODO(b/120949879): This may be removed when CaptureConfig removes all camera2
     * dependencies.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<CaptureConfig.OptionUnpacker> OPTION_CAPTURE_CONFIG_UNPACKER =
            Option.create("camerax.core.useCase.captureConfigUnpacker",
                    CaptureConfig.OptionUnpacker.class);
    /**
     * Option: camerax.core.useCase.surfaceOccypyPriority
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    Option<Integer> OPTION_SURFACE_OCCUPANCY_PRIORITY =
            Option.create("camerax.core.useCase.surfaceOccupancyPriority", int.class);

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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    SessionConfig getDefaultSessionConfig(@Nullable SessionConfig valueIfMissing);

    /**
     * Retrieves the default session configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's session configuration with default
     * values.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    SessionConfig getDefaultSessionConfig();

    /**
     * Retrieves the default capture configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's capture configuration with default
     * values.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    CaptureConfig getDefaultCaptureConfig(@Nullable CaptureConfig valueIfMissing);

    /**
     * Retrieves the default capture configuration for this use case.
     *
     * <p>This configuration is used to initialize the use case's capture configuration with default
     * values.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    CaptureConfig getDefaultCaptureConfig();

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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    SessionConfig.OptionUnpacker getSessionOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing);

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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    SessionConfig.OptionUnpacker getSessionOptionUnpacker();

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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    CaptureConfig.OptionUnpacker getCaptureOptionUnpacker(
            @Nullable CaptureConfig.OptionUnpacker valueIfMissing);

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
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    CaptureConfig.OptionUnpacker getCaptureOptionUnpacker();

    /**
     * Retrieves the surface occupancy priority of the target intending to use from this
     * configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    int getSurfaceOccupancyPriority(int valueIfMissing);

    /**
     * Retrieves the surface occupancy priority of the target intending to use from this
     * configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    int getSurfaceOccupancyPriority();

    /**
     * Builder for a {@link UseCaseConfig}.
     *
     * @param <T> The type of the object being configured.
     * @param <C> The top level configuration which will be generated by {@link #build()}.
     * @param <B> The top level builder type for which this builder is composed with.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    interface Builder<T extends UseCase, C extends UseCaseConfig<T>, B> extends
            TargetConfig.Builder<T, B>, ExtendableBuilder, UseCaseEventConfig.Builder<B> {

        /**
         * Sets the default session configuration for this use case.
         *
         * @param sessionConfig The default session configuration to use for this use case.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        B setDefaultSessionConfig(SessionConfig sessionConfig);

        /**
         * Sets the default capture configuration for this use case.
         *
         * @param captureConfig The default capture configuration to use for this use case.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        B setDefaultCaptureConfig(CaptureConfig captureConfig);

        /**
         * Sets the Option Unpacker for translating this configuration into a {@link SessionConfig}
         *
         * <p>TODO(b/120949879): This may be removed when SessionConfig removes all camera2
         * dependencies.
         *
         * @param optionUnpacker The option unpacker for to use for this use case.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        B setSessionOptionUnpacker(SessionConfig.OptionUnpacker optionUnpacker);

        /**
         * Sets the Option Unpacker for translating this configuration into a {@link CaptureConfig}
         *
         * <p>TODO(b/120949879): This may be removed when CaptureConfig removes all camera2
         * dependencies.
         *
         * @param optionUnpacker The option unpacker for to use for this use case.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        B setCaptureOptionUnpacker(CaptureConfig.OptionUnpacker optionUnpacker);

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
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        B setSurfaceOccupancyPriority(int priority);

        /**
         * Builds the configuration for the target use case.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        C build();
    }
}
