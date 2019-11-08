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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.ImageCapture.CaptureMode;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/** Configuration for an image capture use case. */
public final class ImageCaptureConfig
        implements UseCaseConfig<ImageCapture>,
        ImageOutputConfig,
        CameraDeviceConfig, // TODO(b/142840814): Remove in favor of CameraSelector
        ThreadConfig,
        IoConfig {

    // Option Declarations:
    // *********************************************************************************************

    static final Option<CaptureMode> OPTION_IMAGE_CAPTURE_MODE =
            Option.create(
                    "camerax.core.imageCapture.captureMode", CaptureMode.class);
    static final Option<FlashMode> OPTION_FLASH_MODE =
            Option.create("camerax.core.imageCapture.flashMode", FlashMode.class);
    static final Option<CaptureBundle> OPTION_CAPTURE_BUNDLE =
            Option.create("camerax.core.imageCapture.captureBundle", CaptureBundle.class);
    static final Option<CaptureProcessor> OPTION_CAPTURE_PROCESSOR =
            Option.create("camerax.core.imageCapture.captureProcessor", CaptureProcessor.class);
    static final Option<Integer> OPTION_BUFFER_FORMAT =
            Option.create("camerax.core.imageCapture.bufferFormat", Integer.class);
    static final Option<Integer> OPTION_MAX_CAPTURE_STAGES =
            Option.create("camerax.core.imageCapture.maxCaptureStages", Integer.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    ImageCaptureConfig(OptionsBundle config) {
        mConfig = config;
    }

    /**
     * Returns the {@link CaptureMode}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    public CaptureMode getCaptureMode(
            @Nullable CaptureMode valueIfMissing) {
        return retrieveOption(OPTION_IMAGE_CAPTURE_MODE, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureMode}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    public CaptureMode getCaptureMode() {
        return retrieveOption(OPTION_IMAGE_CAPTURE_MODE);
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
        return retrieveOption(OPTION_FLASH_MODE, valueIfMissing);
    }

    /**
     * Returns the {@link FlashMode}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    public FlashMode getFlashMode() {
        return retrieveOption(OPTION_FLASH_MODE);
    }

    /**
     * Returns the {@link CaptureBundle}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public CaptureBundle getCaptureBundle(@Nullable CaptureBundle valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_BUNDLE, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureBundle}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CaptureBundle getCaptureBundle() {
        return retrieveOption(OPTION_CAPTURE_BUNDLE);
    }

    /**
     * Returns the {@link CaptureProcessor}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public CaptureProcessor getCaptureProcessor(@Nullable CaptureProcessor valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_PROCESSOR, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureProcessor}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CaptureProcessor getCaptureProcessor() {
        return retrieveOption(OPTION_CAPTURE_PROCESSOR);
    }

    /**
     * Returns the {@link ImageFormat} of the capture in memory.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>ValueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Integer getBufferFormat(@Nullable Integer valueIfMissing) {
        return retrieveOption(OPTION_BUFFER_FORMAT, valueIfMissing);
    }

    /**
     * Returns the {@link ImageFormat} of the capture in memory.
     *
     * @return The stored value, if it exists in the configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public Integer getBufferFormat() {
        return retrieveOption(OPTION_BUFFER_FORMAT);
    }

    /**
     * Returns the max number of {@link CaptureStage}.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in
     * this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getMaxCaptureStages(int valueIfMissing) {
        return retrieveOption(OPTION_MAX_CAPTURE_STAGES, valueIfMissing);
    }

    /**
     * Returns the max number of {@link CaptureStage}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getMaxCaptureStages() {
        return retrieveOption(OPTION_MAX_CAPTURE_STAGES);
    }

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(@NonNull Option<?> id) {
        return mConfig.containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id) {
        return mConfig.retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id,
            @Nullable ValueT valueIfMissing) {
        return mConfig.retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(@NonNull String idStem, @NonNull OptionMatcher matcher) {
        mConfig.findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public Set<Option<?>> listOptions() {
        return mConfig.listOptions();
    }

    // Implementations of TargetConfig default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public Class<ImageCapture> getTargetClass(
            @Nullable Class<ImageCapture> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<ImageCapture> storedClass =
                (Class<ImageCapture>) retrieveOption(
                        OPTION_TARGET_CLASS,
                        valueIfMissing);
        return storedClass;
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public Class<ImageCapture> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<ImageCapture> storedClass =
                (Class<ImageCapture>) retrieveOption(
                        OPTION_TARGET_CLASS);
        return storedClass;
    }

    /**
     * Retrieves the name of the target object being configured.
     *
     * <p>The name should be a value that can uniquely identify an instance of the object being
     * configured.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public String getTargetName(@Nullable String valueIfMissing) {
        return retrieveOption(OPTION_TARGET_NAME, valueIfMissing);
    }

    /**
     * Retrieves the name of the target object being configured.
     *
     * <p>The name should be a value that can uniquely identify an instance of the object being
     * configured.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    @NonNull
    public String getTargetName() {
        return retrieveOption(OPTION_TARGET_NAME);
    }

    // Implementations of CameraDeviceConfig default methods

    /**
     * Returns the lens-facing direction of the camera being configured.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public LensFacing getLensFacing(@Nullable LensFacing valueIfMissing) {
        return retrieveOption(OPTION_LENS_FACING, valueIfMissing);
    }

    /**
     * Retrieves the lens facing direction for the primary camera to be configured.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public LensFacing getLensFacing() {
        return retrieveOption(OPTION_LENS_FACING);
    }

    /**
     * Returns the set of {@link CameraIdFilter} that filter out unavailable camera id.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>ValueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public CameraIdFilter getCameraIdFilter(@Nullable CameraIdFilter valueIfMissing) {
        return retrieveOption(OPTION_CAMERA_ID_FILTER, valueIfMissing);
    }

    /**
     * Returns the set of {@link CameraIdFilter} that filter out unavailable camera id.
     *
     * @return The stored value, if it exists in the configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public CameraIdFilter getCameraIdFilter() {
        return retrieveOption(OPTION_CAMERA_ID_FILTER);
    }

    // Implementations of ImageOutputConfig default methods

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * <p>This is the ratio of the target's width to the image's height, where the numerator of the
     * provided {@link Rational} corresponds to the width, and the denominator corresponds to the
     * height.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public Rational getTargetAspectRatioCustom(@Nullable Rational valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM, valueIfMissing);
    }

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * <p>This is the ratio of the target's width to the image's height, where the numerator of the
     * provided {@link Rational} corresponds to the width, and the denominator corresponds to the
     * height.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Rational getTargetAspectRatioCustom() {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM);
    }

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    @Override
    public Integer getTargetAspectRatio(@Nullable Integer valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO, valueIfMissing);
    }

    /**
     * Retrieves the aspect ratio of the target intending to use images from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @AspectRatio
    @Override
    public int getTargetAspectRatio() {
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
    @Override
    @RotationValue
    public int getTargetRotation(int valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ROTATION, valueIfMissing);
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
    @Override
    @RotationValue
    public int getTargetRotation() {
        return retrieveOption(OPTION_TARGET_ROTATION);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public Size getTargetResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    @NonNull
    public Size getTargetResolution() {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION);
    }

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @Nullable
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getDefaultResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the default resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Size getDefaultResolution() {
        return retrieveOption(OPTION_DEFAULT_RESOLUTION);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public Size getMaxResolution(@Nullable Size valueIfMissing) {
        return retrieveOption(OPTION_MAX_RESOLUTION, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public Size getMaxResolution() {
        return retrieveOption(OPTION_MAX_RESOLUTION);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public List<Pair<Integer, Size[]>> getSupportedResolutions(
            @Nullable List<Pair<Integer, Size[]>> valueIfMissing) {
        return retrieveOption(OPTION_SUPPORTED_RESOLUTIONS, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return retrieveOption(OPTION_SUPPORTED_RESOLUTIONS);
    }

    // Implementations of ThreadConfig default methods

    /**
     * Returns the executor that will be used for background tasks.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public Executor getBackgroundExecutor(@Nullable Executor valueIfMissing) {
        return retrieveOption(OPTION_BACKGROUND_EXECUTOR, valueIfMissing);
    }

    /**
     * Returns the executor that will be used for background tasks.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    @NonNull
    public Executor getBackgroundExecutor() {
        return retrieveOption(OPTION_BACKGROUND_EXECUTOR);
    }

    // Implementations of IO default methods

    /**
     * Returns the executor that will be used for IO tasks.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    @Override
    public Executor getIoExecutor(@Nullable Executor valueIfMissing) {
        return retrieveOption(OPTION_IO_EXECUTOR, valueIfMissing);
    }

    /**
     * Returns the executor that will be used for IO tasks.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    @Override
    public Executor getIoExecutor() {
        return retrieveOption(OPTION_IO_EXECUTOR);
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
    @NonNull
    public SessionConfig getDefaultSessionConfig() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public CaptureConfig getDefaultCaptureConfig(@Nullable CaptureConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public CaptureConfig getDefaultCaptureConfig() {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker() {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker(
            @Nullable CaptureConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker() {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public UseCase.EventCallback getUseCaseEventCallback(
            @Nullable UseCase.EventCallback valueIfMissing) {
        return retrieveOption(OPTION_USE_CASE_EVENT_CALLBACK, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @NonNull
    public UseCase.EventCallback getUseCaseEventCallback() {
        return retrieveOption(OPTION_USE_CASE_EVENT_CALLBACK);
    }

    // End of the default implementation of Config
    // *********************************************************************************************

    /** Builder for a {@link ImageCaptureConfig}. */
    public static final class Builder
            implements UseCaseConfig.Builder<
            ImageCapture, ImageCaptureConfig, Builder>,
            ImageOutputConfig.Builder<Builder>,
            CameraDeviceConfig.Builder<Builder>,
            ThreadConfig.Builder<Builder>,
            IoConfig.Builder<Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(ImageCapture.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(ImageCapture.class);
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @NonNull
        public static Builder fromConfig(@NonNull ImageCaptureConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
        }

        /**
         * {@inheritDoc}
         *
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        /**
         * Builds an immutable {@link ImageCaptureConfig} from the current state.
         *
         * @return A {@link ImageCaptureConfig} populated with the current state.
         * @throws IllegalArgumentException if attempting to set both target aspect ratio and
         * target resolution.
         */
        @Override
        @NonNull
        public ImageCaptureConfig build() {
            // Error at runtime for using both setTargetResolution and setTargetAspectRatio on
            // the same config.
            if (getMutableConfig().retrieveOption(OPTION_TARGET_ASPECT_RATIO, null) != null
                    && getMutableConfig().retrieveOption(OPTION_TARGET_RESOLUTION, null) != null) {
                throw new IllegalArgumentException(
                        "Cannot use both setTargetResolution and setTargetAspectRatio on the same "
                                + "config.");
            }
            return new ImageCaptureConfig(OptionsBundle.from(mMutableConfig));
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
        @NonNull
        public Builder setCaptureMode(@NonNull CaptureMode captureMode) {
            getMutableConfig().insertOption(OPTION_IMAGE_CAPTURE_MODE, captureMode);
            return this;
        }

        /**
         * Sets the {@link FlashMode}.
         *
         * <p>When the torch is enabled via {@link CameraControl#enableTorch(boolean)}, the torch
         * will remain enabled during photo capture regardless of {@link FlashMode} setting. When
         * the torch is disabled, flash will function as specified by
         * {@link #setFlashMode(FlashMode)}.
         *
         * @param flashMode The requested flash mode.
         * @return The current Builder.
         */
        @NonNull
        public Builder setFlashMode(@NonNull FlashMode flashMode) {
            getMutableConfig().insertOption(OPTION_FLASH_MODE, flashMode);
            return this;
        }

        /**
         * Sets the {@link CaptureBundle}.
         *
         * @param captureBundle The requested capture bundle for extension.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setCaptureBundle(@NonNull CaptureBundle captureBundle) {
            getMutableConfig().insertOption(OPTION_CAPTURE_BUNDLE, captureBundle);
            return this;
        }

        /**
         * Sets the {@link CaptureProcessor}.
         *
         * @param captureProcessor The requested capture processor for extension.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setCaptureProcessor(@NonNull CaptureProcessor captureProcessor) {
            getMutableConfig().insertOption(OPTION_CAPTURE_PROCESSOR, captureProcessor);
            return this;
        }

        /**
         * Sets the {@link ImageFormat} of the {@link ImageProxy} returned by the
         * {@link ImageCapture.OnImageCapturedCallback}.
         *
         * <p>Warning. This could lead to an invalid configuration as image format support is per
         * device. Also, setting the buffer format in conjuncture with image capture extensions will
         * result in an invalid configuration. In this case {@link
         * ImageCapture#ImageCapture(ImageCaptureConfig)} will throw an
         * {@link IllegalArgumentException}.
         *
         * @param bufferImageFormat The image format for captured images.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setBufferFormat(int bufferImageFormat) {
            getMutableConfig().insertOption(OPTION_BUFFER_FORMAT, bufferImageFormat);
            return this;
        }

        /**
         * Sets the max number of {@link CaptureStage}.
         *
         * @param maxCaptureStages The max CaptureStage number.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setMaxCaptureStages(int maxCaptureStages) {
            getMutableConfig().insertOption(OPTION_MAX_CAPTURE_STAGES, maxCaptureStages);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSupportedResolutions(@NonNull List<Pair<Integer, Size[]>> resolutions) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutions);
            return this;
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setTargetClass(@NonNull Class<ImageCapture> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        /**
         * Sets the name of the target object being configured.
         *
         * <p>The name should be a value that can uniquely identify an instance of the object being
         * configured.
         *
         * @param targetName A unique string identifier for the instance of the class being
         *                   configured.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        // Implementations of CameraDeviceConfig.Builder default methods

        /**
         * Sets the primary camera to be configured based on the direction the lens is facing.
         *
         * <p>If multiple cameras exist with equivalent lens facing direction, the first ("primary")
         * camera for that direction will be chosen.
         *
         * @param lensFacing The direction of the camera's lens.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setLensFacing(@NonNull LensFacing lensFacing) {
            getMutableConfig().insertOption(OPTION_LENS_FACING, lensFacing);
            return this;
        }

        /**
         * Sets a {@link CameraIdFilter} that filter out the unavailable camera id.
         *
         * <p>The camera id filter will be used to filter those cameras with lens facing
         * specified in the config.
         *
         * @param cameraIdFilter The {@link CameraIdFilter}.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCameraIdFilter(@NonNull CameraIdFilter cameraIdFilter) {
            getMutableConfig().insertOption(OPTION_CAMERA_ID_FILTER, cameraIdFilter);
            return this;
        }

        // Implementations of ImageOutputConfig.Builder default methods

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>This is the ratio of the target's width to the image's height, where the numerator of
         * the provided {@link Rational} corresponds to the width, and the denominator corresponds
         * to the height.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints.
         * Application code should check the resulting output's resolution.
         *
         * <p>This method can be used to request an aspect ratio that is not from the standard set
         * of aspect ratios defined in the {@link AspectRatio}.
         *
         * <p>This method will remove any value set by setTargetAspectRatio().
         *
         * <p>For ImageCapture, the outputs are the {@link ImageProxy} or the File passed to image
         * capture listeners.
         *
         * @param aspectRatio A {@link Rational} representing the ratio of the target's width and
         *                    height.
         * @return The current Builder.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @Override
        public Builder setTargetAspectRatioCustom(@NonNull Rational aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM, aspectRatio);
            getMutableConfig().removeOption(OPTION_TARGET_ASPECT_RATIO);
            return this;
        }

        /**
         * Sets the aspect ratio of the intended target for images from this configuration.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case.  Attempting so will throw an IllegalArgumentException when building the
         * Config.
         *
         * <p>The target aspect ratio is used as a hint when determining the resulting output aspect
         * ratio which may differ from the request, possibly due to device constraints.
         * Application code should check the resulting output's resolution.
         *
         * @param aspectRatio A {@link AspectRatio} representing the ratio of the
         *                    target's width and height.
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetAspectRatio(@AspectRatio int aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return this;
        }

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
        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * Sets the intended output target resolution.
         *
         * <p>The target resolution attempts to establish a minimum bound for the image resolution.
         * The actual image resolution will be the closest available resolution in size that is not
         * smaller than the target resolution, as determined by the Camera implementation. However,
         * if no resolution exists that is equal to or larger than the target resolution, the
         * nearest available resolution smaller than the target resolution will be chosen.
         *
         * <p>It is not allowed to set both target aspect ratio and target resolution on the same
         * use case.  Attempting so will throw an IllegalArgumentException when building the
         * Config.
         *
         * <p>The target aspect ratio will also be set the same as the aspect ratio of the provided
         * {@link Size}. Make sure to set the target resolution with the correct orientation.
         *
         * @param resolution The target resolution to choose from supported output sizes list.
         * @return The current Builder.
         */
        @NonNull
        @Override
        public Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_TARGET_RESOLUTION, resolution);
            if (resolution != null) {
                getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO_CUSTOM,
                        new Rational(resolution.getWidth(), resolution.getHeight()));
            }
            return this;
        }

        /**
         * Sets the default resolution of the intended target from this configuration.
         *
         * @param resolution The default resolution to choose from supported output sizes list.
         * @return The current Builder.
         * @hide
         */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(ImageOutputConfig.OPTION_DEFAULT_RESOLUTION,
                    resolution);
            return this;
        }

        /** @hide */
        @NonNull
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        // Implementations of ThreadConfig.Builder default methods

        /**
         * Sets the default executor that will be used for background tasks.
         *
         * @param executor The executor which will be used for background tasks.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setBackgroundExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_BACKGROUND_EXECUTOR, executor);
            return this;
        }

        // Implementations of IoConfig.Builder default methods

        /**
         * Sets the default executor that will be used for IO tasks.
         *
         * @param executor The executor which will be used for IO tasks.
         * @return the current Builder.
         */
        @Override
        @NonNull
        public Builder setIoExecutor(@NonNull Executor executor) {
            getMutableConfig().insertOption(OPTION_IO_EXECUTOR, executor);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        @NonNull
        public Builder setUseCaseEventCallback(
                @NonNull UseCase.EventCallback useCaseEventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, useCaseEventCallback);
            return this;
        }
    }
}
