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
import androidx.camera.core.ImageCaptureUseCase.CaptureMode;

/** Configuration for an image capture use case. */
public final class ImageCaptureUseCaseConfiguration
        implements UseCaseConfiguration<ImageCaptureUseCase>,
        ImageOutputConfiguration,
        CameraDeviceConfiguration,
        ThreadConfiguration {

    // Option Declarations:
    // *********************************************************************************************
    static final Option<ImageCaptureUseCase.CaptureMode> OPTION_IMAGE_CAPTURE_MODE =
            Option.create(
                    "camerax.core.imageCapture.captureMode", ImageCaptureUseCase.CaptureMode.class);
    static final Option<FlashMode> OPTION_FLASH_MODE =
            Option.create("camerax.core.imageCapture.flashMode", FlashMode.class);
    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    ImageCaptureUseCaseConfiguration(OptionsBundle config) {
        mConfig = config;
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

    /**
     * Returns the {@link ImageCaptureUseCase.CaptureMode}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public ImageCaptureUseCase.CaptureMode getCaptureMode(
            @Nullable ImageCaptureUseCase.CaptureMode valueIfMissing) {
        return getConfiguration().retrieveOption(OPTION_IMAGE_CAPTURE_MODE, valueIfMissing);
    }

    /**
     * Returns the {@link ImageCaptureUseCase.CaptureMode}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public ImageCaptureUseCase.CaptureMode getCaptureMode() {
        return getConfiguration().retrieveOption(OPTION_IMAGE_CAPTURE_MODE);
    }

    /**
     * Returns the {@link FlashMode}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public FlashMode getFlashMode(@Nullable FlashMode valueIfMissing) {
        return getConfiguration().retrieveOption(OPTION_FLASH_MODE, valueIfMissing);
    }

    /**
     * Returns the {@link FlashMode}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public FlashMode getFlashMode() {
        return getConfiguration().retrieveOption(OPTION_FLASH_MODE);
    }

    /** Builder for a {@link ImageCaptureUseCaseConfiguration}. */
    public static final class Builder
            implements UseCaseConfiguration.Builder<
            ImageCaptureUseCase, ImageCaptureUseCaseConfiguration, Builder>,
            ImageOutputConfiguration.Builder<ImageCaptureUseCaseConfiguration, Builder>,
            CameraDeviceConfiguration.Builder<ImageCaptureUseCaseConfiguration, Builder>,
            ThreadConfiguration.Builder<ImageCaptureUseCaseConfiguration, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfiguration.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ImageCaptureUseCase.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ImageCaptureUseCase.class);
        }

        /**
         * Generates a Builder from another Configuration object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(ImageCaptureUseCaseConfiguration configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
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
        public ImageCaptureUseCaseConfiguration build() {
            return new ImageCaptureUseCaseConfiguration(OptionsBundle.from(mMutableConfig));
        }

        /**
         * Sets the image capture mode.
         *
         * <p>Valid capture modes are {@link CaptureMode#MIN_LATENCY}, which prioritizes latency
         * over image quality, or {@link CaptureMode#MAX_QUALITY}, which prioritizes image quality
         * over latency.
         *
         * @param captureMode The requested image capture mode.
         * @return The current Builder.
         */
        public Builder setCaptureMode(ImageCaptureUseCase.CaptureMode captureMode) {
            getMutableConfiguration().insertOption(OPTION_IMAGE_CAPTURE_MODE, captureMode);
            return builder();
        }

        /**
         * Sets the {@link FlashMode}.
         *
         * @param flashMode The requested flash mode.
         * @return The current Builder.
         */
        public Builder setFlashMode(FlashMode flashMode) {
            getMutableConfiguration().insertOption(OPTION_FLASH_MODE, flashMode);
            return builder();
        }
    }
}
