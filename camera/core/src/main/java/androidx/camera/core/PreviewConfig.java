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

import android.os.Handler;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.Set;
import java.util.UUID;

/** Configuration for a {@link Preview} use case. */
public final class PreviewConfig
        implements UseCaseConfig<Preview>,
        ImageOutputConfig,
        CameraDeviceConfig,
        ThreadConfig {

    // Options declarations

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    static final Option<ImageInfoProcessor> IMAGE_INFO_PROCESSOR = Option.create(
            "camerax.core.preview.imageInfoProcessor", ImageInfoProcessor.class);
    static final Option<CaptureProcessor> OPTION_PREVIEW_CAPTURE_PROCESSOR =
            Option.create("camerax.core.preview.captureProcessor", CaptureProcessor.class);
    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    PreviewConfig(OptionsBundle config) {
        mConfig = config;
    }

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public boolean containsOption(Option<?> id) {
        return mConfig.containsOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return mConfig.retrieveOption(id);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return mConfig.retrieveOption(id, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        mConfig.findOptions(idStem, matcher);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Set<Option<?>> listOptions() {
        return mConfig.listOptions();
    }

    // Implementations of TargetConfig default methods

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    @Nullable
    public Class<Preview> getTargetClass(
            @Nullable Class<Preview> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<Preview> storedClass =
                (Class<Preview>) retrieveOption(
                        OPTION_TARGET_CLASS,
                        valueIfMissing);
        return storedClass;
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public Class<Preview> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<Preview> storedClass =
                (Class<Preview>) retrieveOption(
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
     */
    @Override
    @Nullable
    public CameraX.LensFacing getLensFacing(@Nullable CameraX.LensFacing valueIfMissing) {
        return retrieveOption(OPTION_LENS_FACING, valueIfMissing);
    }

    /**
     * Retrieves the lens facing direction for the primary camera to be configured.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    public CameraX.LensFacing getLensFacing() {
        return retrieveOption(OPTION_LENS_FACING);
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
     */
    @Override
    @Nullable
    public Rational getTargetAspectRatio(@Nullable Rational valueIfMissing) {
        return retrieveOption(OPTION_TARGET_ASPECT_RATIO, valueIfMissing);
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
     */
    @Override
    public Rational getTargetAspectRatio() {
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
    public Size getTargetResolution(Size valueIfMissing) {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, valueIfMissing);
    }

    /**
     * Retrieves the resolution of the target intending to use from this configuration.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @Override
    public Size getTargetResolution() {
        return retrieveOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION);
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

    /**
     * Returns the default handler that will be used for callbacks.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Override
    @Nullable
    public Handler getCallbackHandler(@Nullable Handler valueIfMissing) {
        return retrieveOption(OPTION_CALLBACK_HANDLER, valueIfMissing);
    }

    /**
     * Returns the default handler that will be used for callbacks.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
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
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Override
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker() {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER);
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
    public CaptureConfig getDefaultCaptureConfig() {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG);
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
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker() {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER);
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

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public UseCase.EventListener getUseCaseEventListener(
            @Nullable UseCase.EventListener valueIfMissing) {
        return retrieveOption(OPTION_USE_CASE_EVENT_LISTENER, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    @Override
    public UseCase.EventListener getUseCaseEventListener() {
        return retrieveOption(OPTION_USE_CASE_EVENT_LISTENER);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    ImageInfoProcessor getImageInfoProcessor(ImageInfoProcessor valueIfMissing) {
        return retrieveOption(IMAGE_INFO_PROCESSOR, valueIfMissing);
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    ImageInfoProcessor getImageInfoProcessor() {
        return retrieveOption(IMAGE_INFO_PROCESSOR);
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
        return retrieveOption(OPTION_PREVIEW_CAPTURE_PROCESSOR, valueIfMissing);
    }

    /**
     * Returns the {@link CaptureProcessor}.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public CaptureProcessor getCaptureProcessor() {
        return retrieveOption(OPTION_PREVIEW_CAPTURE_PROCESSOR);
    }

    // End of the default implementation of Config
    // *********************************************************************************************

    /** Builder for a {@link PreviewConfig}. */
    public static final class Builder
            implements UseCaseConfig.Builder<Preview, PreviewConfig, Builder>,
            ImageOutputConfig.Builder<Builder>,
            CameraDeviceConfig.Builder<Builder>,
            ThreadConfig.Builder<Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
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
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        public static Builder fromConfig(PreviewConfig configuration) {
            return new Builder(MutableOptionsBundle.from(configuration));
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
         * Builds an immutable {@link PreviewConfig} from the current state.
         *
         * @return A {@link PreviewConfig} populated with the current state.
         */
        public PreviewConfig build() {
            return new PreviewConfig(OptionsBundle.from(mMutableConfig));
        }

        // Implementations of TargetConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setTargetClass(Class<Preview> targetClass) {
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
        public Builder setTargetName(String targetName) {
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
         */
        @Override
        public Builder setLensFacing(CameraX.LensFacing lensFacing) {
            getMutableConfig().insertOption(OPTION_LENS_FACING, lensFacing);
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
         * <p>For Preview, the output is the SurfaceTexture of the
         * {@link androidx.camera.core.Preview.PreviewOutput}.
         *
         * @param aspectRatio A {@link Rational} representing the ratio of the target's width and
         *                    height.
         * @return The current Builder.
         */
        @Override
        public Builder setTargetAspectRatio(Rational aspectRatio) {
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
        @Override
        public Builder setTargetRotation(@RotationValue int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        /**
         * Sets the resolution of the intended target from this configuration.
         *
         * <p>The target resolution attempts to establish a minimum bound for the preview
         * resolution. The actual preview resolution will be the closest available resolution in
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
            getMutableConfig()
                    .insertOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, resolution);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setMaxResolution(Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        // Implementations of ThreadConfig.Builder default methods

        /**
         * Sets the default handler that will be used for callbacks.
         *
         * @param handler The handler which will be used to post callbacks.
         * @return the current Builder.
         */
        @Override
        public Builder setCallbackHandler(Handler handler) {
            getMutableConfig().insertOption(OPTION_CALLBACK_HANDLER, handler);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultSessionConfig(SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setDefaultCaptureConfig(CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setSessionOptionUnpacker(SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setCaptureOptionUnpacker(CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Override
        public Builder setUseCaseEventListener(UseCase.EventListener useCaseEventListener) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_LISTENER, useCaseEventListener);
            return this;
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public Builder setImageInfoProcessor(@Nullable ImageInfoProcessor processor) {
            getMutableConfig().insertOption(IMAGE_INFO_PROCESSOR, processor);
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
        public Builder setCaptureProcessor(@Nullable CaptureProcessor captureProcessor) {
            getMutableConfig().insertOption(OPTION_PREVIEW_CAPTURE_PROCESSOR, captureProcessor);
            return this;
        }
    }
}
