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

import android.os.Handler;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.Set;
import java.util.UUID;

/** Configuration for an image capture use case. */
public final class PreviewConfiguration
        implements UseCaseConfiguration<Preview>,
        ImageOutputConfiguration,
        CameraDeviceConfiguration,
        ThreadConfiguration {

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    PreviewConfiguration(OptionsBundle config) {
        mConfig = config;
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or {@code valueIfMissing} if the value does not exist in this
     * configuration.
     */
    @Override
    public Size getTargetResolution(Size valueIfMissing) {
        return getConfiguration()
                .retrieveOption(ImageOutputConfiguration.OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    public Size getTargetResolution() {
        return getConfiguration().retrieveOption(ImageOutputConfiguration.OPTION_TARGET_RESOLUTION);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Configuration getConfiguration() {
        return mConfig;
    }

    /** Builder for a {@link PreviewConfiguration}. */
    public static final class Builder implements
            UseCaseConfiguration.Builder<Preview, PreviewConfiguration, Builder>,
            ImageOutputConfiguration.Builder<PreviewConfiguration, Builder>,
            CameraDeviceConfiguration.Builder<PreviewConfiguration, Builder>,
            ThreadConfiguration.Builder<PreviewConfiguration, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfiguration.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(Preview.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(Preview.class);
        }

        /**
         * Generates a Builder from another Configuration object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(PreviewConfiguration configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Sets the resolution of the intended target from this configuration.
         *
         * <p>The target resolution attempts to establish a minimum bound for the view finder
         * resolution. The actual view finder resolution will be the closest available resolution in
         * size that is not smaller than the target resolution, as determined by the Camera
         * implementation. However, if no resolution exists that is equal to or larger than the
         * target resolution, the nearest available resolution smaller than the target resolution
         * will be chosen.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @Override
        public Builder setTargetResolution(Size resolution) {
            getMutableConfiguration()
                    .insertOption(ImageOutputConfiguration.OPTION_TARGET_RESOLUTION, resolution);
            return builder();
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public MutableConfiguration getMutableConfiguration() {
            return mMutableConfig;
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder builder() {
            return this;
        }

        @Override
        public PreviewConfiguration build() {
            return new PreviewConfiguration(OptionsBundle.from(mMutableConfig));
        }

        // Start of the default implementation of Configuration.Builder
        // *****************************************************************************************

        // Implementations of Configuration.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public <ValueT> Builder insertOption(Option<ValueT> opt, ValueT value) {
            getMutableConfiguration().insertOption(opt, value);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @Nullable
        public <ValueT> Builder removeOption(Option<ValueT> opt) {
            getMutableConfiguration().removeOption(opt);
            return builder();
        }

        // Implementations of TargetConfiguration.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setTargetClass(Class<Preview> targetClass) {
            getMutableConfiguration().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfiguration().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return builder();
        }

        @Override
        public Builder setTargetName(String targetName) {
            getMutableConfiguration().insertOption(OPTION_TARGET_NAME, targetName);
            return builder();
        }

        // Implementations of CameraDeviceConfiguration.Builder default methods

        @Override
        public Builder setLensFacing(CameraX.LensFacing lensFacing) {
            getMutableConfiguration().insertOption(OPTION_LENS_FACING, lensFacing);
            return builder();
        }

        // Implementations of ImageOutputConfiguration.Builder default methods

        @Override
        public Builder setTargetAspectRatio(Rational aspectRatio) {
            getMutableConfiguration().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return builder();
        }

        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfiguration().insertOption(OPTION_TARGET_ROTATION, rotation);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(Size resolution) {
            getMutableConfiguration().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return builder();
        }

        // Implementations of ThreadConfiguration.Builder default methods

        @Override
        public Builder setCallbackHandler(Handler handler) {
            getMutableConfiguration().insertOption(OPTION_CALLBACK_HANDLER, handler);
            return builder();
        }

        // Implementations of UseCaseConfiguration.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultSessionConfiguration(SessionConfiguration sessionConfig) {
            getMutableConfiguration().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setOptionUnpacker(SessionConfiguration.OptionUnpacker optionUnpacker) {
            getMutableConfiguration().insertOption(OPTION_CONFIG_UNPACKER, optionUnpacker);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfiguration().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return builder();
        }

        // End of the default implementation of Configuration.Builder
        // *****************************************************************************************

    }

    // Start of the default implementation of Configuration
    // *********************************************************************************************

    // Implementations of Configuration.Reader default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(Option<?> id) {
        return getConfiguration().containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return getConfiguration().retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return getConfiguration().retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        getConfiguration().findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Set<Option<?>> listOptions() {
        return getConfiguration().listOptions();
    }

    // Implementations of TargetConfiguration default methods

    @Override
    @Nullable
    public Class<Preview> getTargetClass(@Nullable Class<Preview> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<Preview> storedClass = (Class<Preview>) retrieveOption(
                OPTION_TARGET_CLASS,
                valueIfMissing);
        return storedClass;
    }

    @Override
    public Class<Preview> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<Preview> storedClass = (Class<Preview>) retrieveOption(
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

    // Implementations of CameraDeviceConfiguration default methods

    @Override
    @Nullable
    public CameraX.LensFacing getLensFacing(@Nullable CameraX.LensFacing valueIfMissing) {
        return retrieveOption(OPTION_LENS_FACING, valueIfMissing);
    }

    @Override
    public CameraX.LensFacing getLensFacing() {
        return retrieveOption(OPTION_LENS_FACING);
    }

    // Implementations of ImageOutputConfiguration default methods

    @Override
    @Nullable
    public Rational getTargetAspectRatio(@Nullable Rational valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO, valueIfMissing);
    }

    @Override
    public Rational getTargetAspectRatio() {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO);
    }

    @Override
    @RotationValue
    public int getTargetRotation(int valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ROTATION, valueIfMissing);
    }

    @Override
    @RotationValue
    public int getTargetRotation() {
        return retrieveOption(OPTION_TARGET_ROTATION);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getMaxResolution(Size valueIfMissing) {
        return retrieveOption(OPTION_MAX_RESOLUTION, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getMaxResolution() {
        return retrieveOption(OPTION_MAX_RESOLUTION);
    }

    // Implementations of ThreadConfiguration default methods

    @Override
    @Nullable
    public Handler getCallbackHandler(@Nullable Handler valueIfMissing) {
        return retrieveOption(OPTION_CALLBACK_HANDLER, valueIfMissing);
    }

    @Override
    public Handler getCallbackHandler() {
        return retrieveOption(OPTION_CALLBACK_HANDLER);
    }

    // Implementations of UseCaseConfiguration default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfiguration getDefaultSessionConfiguration(
            @Nullable SessionConfiguration valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfiguration getDefaultSessionConfiguration() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfiguration.OptionUnpacker getOptionUnpacker(
            @Nullable SessionConfiguration.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CONFIG_UNPACKER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfiguration.OptionUnpacker getOptionUnpacker() {
        return retrieveOption(OPTION_CONFIG_UNPACKER);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    // End of the default implementation of Configuration
    // *********************************************************************************************
}
