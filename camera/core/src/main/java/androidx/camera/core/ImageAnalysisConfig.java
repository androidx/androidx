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
import android.os.Handler;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.ImageAnalysis.ImageReaderMode;

import java.util.Set;
import java.util.UUID;

/** Configuration for an image analysis use case. */
public final class ImageAnalysisConfig
        implements UseCaseConfig<ImageAnalysis>,
        ImageOutputConfig,
        CameraDeviceConfig,
        ThreadConfig {

    // Option Declarations:
    // *********************************************************************************************
    static final Option<ImageReaderMode> OPTION_IMAGE_READER_MODE =
            Option.create("camerax.core.imageAnalysis.imageReaderMode", ImageReaderMode.class);
    static final Option<Integer> OPTION_IMAGE_QUEUE_DEPTH =
            Option.create("camerax.core.imageAnalysis.imageQueueDepth", int.class);
    private final OptionsBundle mConfig;

    ImageAnalysisConfig(OptionsBundle config) {
        mConfig = config;
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
        return getConfig().retrieveOption(OPTION_IMAGE_READER_MODE, valueIfMissing);
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
        return getConfig().retrieveOption(OPTION_IMAGE_READER_MODE);
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
        return getConfig().retrieveOption(OPTION_IMAGE_QUEUE_DEPTH, valueIfMissing);
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
        return getConfig().retrieveOption(OPTION_IMAGE_QUEUE_DEPTH);
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
        return getConfig()
                .retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    public Size getTargetResolution() {
        return getConfig().retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /** Builder for a {@link ImageAnalysisConfig}. */
    public static final class Builder implements
            CameraDeviceConfig.Builder<ImageAnalysisConfig, Builder>,
            ImageOutputConfig.Builder<ImageAnalysisConfig, Builder>,
            ThreadConfig.Builder<ImageAnalysisConfig, Builder>,
            UseCaseConfig.Builder<ImageAnalysis, ImageAnalysisConfig, Builder> {
        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ImageAnalysis.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ImageAnalysis.class);
        }

        /**
         * Generates a Builder from another Config object.
         *
         * @param config An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(ImageAnalysisConfig config) {
            return new Builder(MutableOptionsBundle.from(config));
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
            getMutableConfig().insertOption(OPTION_IMAGE_READER_MODE, mode);
            return builder();
        }

        /**
         * Sets the number of images available to the camera pipeline.
         *
         * <p>The image queue depth is the number of images available to the camera to fill with
         * data. This includes the image currently being analyzed by {@link
         * ImageAnalysis.Analyzer#analyze(ImageProxy, int)}. Increasing the image queue depth
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
            getMutableConfig().insertOption(OPTION_IMAGE_QUEUE_DEPTH, depth);
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
            getMutableConfig()
                    .insertOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, resolution);
            return builder();
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public MutableConfig getMutableConfig() {
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
        public ImageAnalysisConfig build() {
            return new ImageAnalysisConfig(OptionsBundle.from(mMutableConfig));
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
        public Builder setTargetClass(Class<ImageAnalysis> targetClass) {
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

        // Implementations of CameraDeviceConfig.Builder default methods

        @Override
        public Builder setLensFacing(CameraX.LensFacing lensFacing) {
            getMutableConfig().insertOption(OPTION_LENS_FACING, lensFacing);
            return builder();
        }

        // Implementations of ImageOutputConfig.Builder default methods

        @Override
        public Builder setTargetAspectRatio(Rational aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return builder();
        }

        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return builder();
        }

        // Implementations of ThreadConfig.Builder default methods

        @Override
        public Builder setCallbackHandler(Handler handler) {
            getMutableConfig().insertOption(OPTION_CALLBACK_HANDLER, handler);
            return builder();
        }

        // Implementations of UseCaseConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultSessionConfig(SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setOptionUnpacker(SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CONFIG_UNPACKER, optionUnpacker);
            return builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
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
    public Class<ImageAnalysis> getTargetClass(@Nullable Class<ImageAnalysis> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<ImageAnalysis> storedClass =
                (Class<ImageAnalysis>) retrieveOption(
                        OPTION_TARGET_CLASS,
                        valueIfMissing);
        return storedClass;
    }

    @Override
    public Class<ImageAnalysis> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<ImageAnalysis> storedClass =
                (Class<ImageAnalysis>) retrieveOption(
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

    // Implementations of CameraDeviceConfig default methods

    @Override
    @Nullable
    public CameraX.LensFacing getLensFacing(@Nullable CameraX.LensFacing valueIfMissing) {
        return retrieveOption(OPTION_LENS_FACING, valueIfMissing);
    }

    @Override
    public CameraX.LensFacing getLensFacing() {
        return retrieveOption(OPTION_LENS_FACING);
    }

    // Implementations of ImageOutputConfig default methods

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

    // Implementations of ThreadConfig default methods

    @Override
    @Nullable
    public Handler getCallbackHandler(@Nullable Handler valueIfMissing) {
        return retrieveOption(OPTION_CALLBACK_HANDLER, valueIfMissing);
    }

    @Override
    public Handler getCallbackHandler() {
        return retrieveOption(OPTION_CALLBACK_HANDLER);
    }

    // Implementations of UseCaseConfig default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfig getDefaultSessionConfig(@Nullable SessionConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfig getDefaultSessionConfig() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfig.OptionUnpacker getOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CONFIG_UNPACKER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfig.OptionUnpacker getOptionUnpacker() {
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

    // End of the default implementation of Config
    // *********************************************************************************************
}
