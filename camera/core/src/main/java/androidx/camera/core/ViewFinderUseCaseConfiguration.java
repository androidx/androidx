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

import android.util.Size;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/** Configuration for an image capture use case. */
public final class ViewFinderUseCaseConfiguration
        implements UseCaseConfiguration<ViewFinderUseCase>,
        ImageOutputConfiguration,
        CameraDeviceConfiguration,
        ThreadConfiguration {

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    ViewFinderUseCaseConfiguration(OptionsBundle config) {
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

    /** Builder for a {@link ViewFinderUseCaseConfiguration}. */
    public static final class Builder
            implements UseCaseConfiguration.Builder<
            ViewFinderUseCase, ViewFinderUseCaseConfiguration, Builder>,
            ImageOutputConfiguration.Builder<ViewFinderUseCaseConfiguration, Builder>,
            CameraDeviceConfiguration.Builder<ViewFinderUseCaseConfiguration, Builder>,
            ThreadConfiguration.Builder<ViewFinderUseCaseConfiguration, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfiguration.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ViewFinderUseCase.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ViewFinderUseCase.class);
        }

        /**
         * Generates a Builder from another Configuration object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(ViewFinderUseCaseConfiguration configuration) {
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
        public ViewFinderUseCaseConfiguration build() {
            return new ViewFinderUseCaseConfiguration(OptionsBundle.from(mMutableConfig));
        }
    }
}
