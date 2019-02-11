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

import android.media.ImageReader;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.ImageAnalysisUseCase.ImageReaderMode;

/** Configuration for an image analysis use case. */
public final class ImageAnalysisUseCaseConfiguration
        implements UseCaseConfiguration<ImageAnalysisUseCase>,
        ImageOutputConfiguration,
        CameraDeviceConfiguration,
        ThreadConfiguration {

    // Option Declarations:
    // ***********************************************************************************************
    static final Option<ImageReaderMode> OPTION_IMAGE_READER_MODE =
            Option.create("camerax.core.imageAnalysis.imageReaderMode", ImageReaderMode.class);
    static final Option<Integer> OPTION_IMAGE_QUEUE_DEPTH =
            Option.create("camerax.core.imageAnalysis.imageQueueDepth", int.class);
    private final OptionsBundle config;

    private ImageAnalysisUseCaseConfiguration(OptionsBundle config) {
        this.config = config;
    }

    /**
     * Returns the mode that the image is acquired from {@link ImageReader}.
     *
     * <p>The available values are {@link ImageReaderMode#ACQUIRE_NEXT_IMAGE} and {@link
     * ImageReaderMode#ACQUIRE_LATEST_IMAGE}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public ImageReaderMode getImageReaderMode(@Nullable ImageReaderMode valueIfMissing) {
        return getConfiguration().retrieveOption(OPTION_IMAGE_READER_MODE, valueIfMissing);
    }

    /**
     * Returns the mode that the image is acquired from {@link ImageReader}.
     *
     * <p>The available values are {@link ImageReaderMode#ACQUIRE_NEXT_IMAGE} and {@link
     * ImageReaderMode#ACQUIRE_LATEST_IMAGE}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public ImageReaderMode getImageReaderMode() {
        return getConfiguration().retrieveOption(OPTION_IMAGE_READER_MODE);
    }

    /**
     * Returns the number of images available to the camera pipeline.
     *
     * <p>The image queue depth is the total number of images, including the image being analyzed,
     * available to the camera pipeline. If analysis takes long enough, the image queue may become
     * full and stall the camera pipeline.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getImageQueueDepth(int valueIfMissing) {
        return getConfiguration().retrieveOption(OPTION_IMAGE_QUEUE_DEPTH, valueIfMissing);
    }

    /**
     * Returns the number of images available to the camera pipeline.
     *
     * <p>The image queue depth is the total number of images, including the image being analyzed,
     * available to the camera pipeline. If analysis takes long enough, the image queue may become
     * full and stall the camera pipeline.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getImageQueueDepth() {
        return getConfiguration().retrieveOption(OPTION_IMAGE_QUEUE_DEPTH);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
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
        return config;
    }

    /** Builder for a {@link ImageAnalysisUseCaseConfiguration}. */
    public static final class Builder
            implements CameraDeviceConfiguration.Builder<
            ImageAnalysisUseCaseConfiguration, Builder>,
            ImageOutputConfiguration.Builder<ImageAnalysisUseCaseConfiguration, Builder>,
            ThreadConfiguration.Builder<ImageAnalysisUseCaseConfiguration, Builder>,
            UseCaseConfiguration.Builder<
                    ImageAnalysisUseCase, ImageAnalysisUseCaseConfiguration, Builder> {
        private final MutableOptionsBundle mutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            this.mutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfiguration.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ImageAnalysisUseCase.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ImageAnalysisUseCase.class);
        }

        /**
         * Generates a Builder from another Configuration object.
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(ImageAnalysisUseCaseConfiguration configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * Sets the mode that the image is acquired from {@link ImageReader}.
         *
         * <p>The available values are {@link ImageReaderMode#ACQUIRE_NEXT_IMAGE} and {@link
         * ImageReaderMode#ACQUIRE_LATEST_IMAGE}.
         *
         * @param mode The mode to set.
         * @return The current Builder.
         */
        public Builder setImageReaderMode(ImageReaderMode mode) {
            getMutableConfiguration().insertOption(OPTION_IMAGE_READER_MODE, mode);
            return builder();
        }

        /**
         * Sets the number of images available to the camera pipeline.
         *
         * <p>The image queue depth is the number of images available to the camera to fill with
         * data. This includes the image currently being analyzed by {@link
         * ImageAnalysisUseCase.Analyzer#analyze(ImageProxy, int)}. Increasing the image queue depth
         * may make camera operation smoother, depending on the {@link ImageReaderMode}, at the cost
         * of increased memory usage.
         *
         * <p>When the {@link ImageReaderMode} is set to {@link
         * ImageReaderMode#ACQUIRE_LATEST_IMAGE}, increasing the image queue depth will increase the
         * amount of time available to analyze an image before stalling the capture pipeline.
         *
         * <p>When the {@link ImageReaderMode} is set to {@link ImageReaderMode#ACQUIRE_NEXT_IMAGE},
         * increasing the image queue depth may make the camera pipeline run smoother on systems
         * under high load. However, the time spent analyzing an image should still be kept under a
         * single frame period for the current frame rate, on average, to avoid stalling the camera
         * pipeline.
         *
         * @param depth The total number of images available to the camera.
         * @return The current Builder.
         */
        public Builder setImageQueueDepth(int depth) {
            getMutableConfiguration().insertOption(OPTION_IMAGE_QUEUE_DEPTH, depth);
            return builder();
        }

        /**
         * Sets the resolution of the intended target from this configuration.
         *
         * <p>The target resolution attempts to establish a minimum bound for the image resolution.
         * The actual image resolution will be the closest available resolution in size that is not
         * smaller than the target resolution, as determined by the Camera implementation. However,
         * if no resolution exists that is equal to or larger than the target resolution, the
         * nearest available resolution smaller than the target resolution will be chosen.
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
            return mutableConfig;
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
        public ImageAnalysisUseCaseConfiguration build() {
            return new ImageAnalysisUseCaseConfiguration(OptionsBundle.from(mutableConfig));
        }
    }
}
