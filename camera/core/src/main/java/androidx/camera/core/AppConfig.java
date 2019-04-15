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

import java.util.Set;
import java.util.UUID;

/**
 * Configuration for adding implementation and user-specific behavior to CameraX.
 *
 * <p>The AppConfig
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class AppConfig implements TargetConfig<CameraX> {

    static final Option<CameraFactory> OPTION_CAMERA_FACTORY =
            Option.create("camerax.core.appConfig.cameraFactory", CameraFactory.class);
    static final Option<CameraDeviceSurfaceManager> OPTION_DEVICE_SURFACE_MANAGER =
            Option.create(
                    "camerax.core.appConfig.deviceSurfaceManager",
                    CameraDeviceSurfaceManager.class);
    static final Option<UseCaseConfigFactory> OPTION_USECASE_CONFIG_FACTORY =
            Option.create(
                    "camerax.core.appConfig.useCaseConfigFactory",
                    UseCaseConfigFactory.class);
    private final OptionsBundle mConfig;

    AppConfig(OptionsBundle options) {
        mConfig = options;
    }

    /**
     * Returns the {@link CameraFactory} implementation for the application.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraFactory getCameraFactory(@Nullable CameraFactory valueIfMissing) {
        return getConfig().retrieveOption(OPTION_CAMERA_FACTORY, valueIfMissing);
    }

    /**
     * Returns the {@link CameraDeviceSurfaceManager} implementation for the application.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CameraDeviceSurfaceManager getDeviceSurfaceManager(
            @Nullable CameraDeviceSurfaceManager valueIfMissing) {
        return getConfig().retrieveOption(OPTION_DEVICE_SURFACE_MANAGER, valueIfMissing);
    }

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Returns the {@link UseCaseConfigFactory} implementation for the application.
     *
     * <p>This factory should produce all default configurations for the application's use cases.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public UseCaseConfigFactory getUseCaseConfigRepository(
            @Nullable UseCaseConfigFactory valueIfMissing) {
        return getConfig().retrieveOption(OPTION_USECASE_CONFIG_FACTORY, valueIfMissing);
    }

    @Override
    public Config getConfig() {
        return mConfig;
    }

    /** A builder for generating {@link AppConfig} objects. */
    public static final class Builder
            implements TargetConfig.Builder<CameraX, AppConfig, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(CameraX.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + AppConfig.Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(CameraX.class);
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param config An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(Config config) {
            return new Builder(MutableOptionsBundle.from(config));
        }

        /**
         * Sets the {@link CameraFactory} implementation for the application.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setCameraFactory(CameraFactory cameraFactory) {
            getMutableConfig().insertOption(OPTION_CAMERA_FACTORY, cameraFactory);
            return builder();
        }

        /**
         * Sets the {@link CameraDeviceSurfaceManager} implementation for the application.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setDeviceSurfaceManager(CameraDeviceSurfaceManager repository) {
            getMutableConfig().insertOption(OPTION_DEVICE_SURFACE_MANAGER, repository);
            return builder();
        }

        /**
         * Sets the {@link UseCaseConfigFactory} implementation for the application.
         *
         * <p>This factory should produce all default configurations for the application's use
         * cases.
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public Builder setUseCaseConfigFactory(UseCaseConfigFactory repository) {
            getMutableConfig().insertOption(OPTION_USECASE_CONFIG_FACTORY, repository);
            return builder();
        }

        @Override
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /** The solution for the unchecked cast warning. */
        @Override
        public Builder builder() {
            return this;
        }

        @Override
        public AppConfig build() {
            return new AppConfig(OptionsBundle.from(mMutableConfig));
        }


        // Start of the default implementation of Config.Builder
        // *****************************************************************************************

        // Implementations of Config.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public <ValueT> Builder insertOption(Option<ValueT> opt, ValueT value) {
            getMutableConfig().insertOption(opt, value);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @Nullable
        public <ValueT> Builder removeOption(Option<ValueT> opt) {
            getMutableConfig().removeOption(opt);
            return builder();
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setTargetClass(Class<CameraX> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return builder();
        }

        @Override
        public Builder setTargetName(String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return builder();
        }

        // End of the default implementation of Config.Builder
        // *****************************************************************************************
    }


    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config.Reader default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(Option<?> id) {
        return getConfig().containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return getConfig().retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return getConfig().retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        getConfig().findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Set<Option<?>> listOptions() {
        return getConfig().listOptions();
    }

    // Implementations of TargetConfig default methods

    @Override
    @Nullable
    public Class<CameraX> getTargetClass(
            @Nullable Class<CameraX> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<CameraX> storedClass = (Class<CameraX>) retrieveOption(
                OPTION_TARGET_CLASS,
                valueIfMissing);
        return storedClass;
    }

    @Override
    public Class<CameraX> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<CameraX> storedClass = (Class<CameraX>) retrieveOption(
                OPTION_TARGET_CLASS);
        return storedClass;
    }

    @Override
    @Nullable
    public String getTargetName(@Nullable String valueIfMissing) {
        return retrieveOption(OPTION_TARGET_NAME, valueIfMissing);
    }

    @Override
    public String getTargetName() {
        return retrieveOption(OPTION_TARGET_NAME);
    }

    // End of the default implementation of Config
    // *********************************************************************************************
}
