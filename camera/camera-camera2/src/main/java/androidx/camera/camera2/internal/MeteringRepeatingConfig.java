/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.internal.TargetConfig;

import java.util.Set;
import java.util.UUID;

/**
 * Configuration for a {@link MeteringRepeating} use case.
 */
public class MeteringRepeatingConfig implements UseCaseConfig<MeteringRepeating> {

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    public MeteringRepeatingConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    // Start of the default implementation of Config
    // *********************************************************************************************

    // Implementations of Config default methods

    @Override
    public boolean containsOption(@NonNull Option<?> id) {
        return mConfig.containsOption(id);
    }

    @Nullable
    @Override
    public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id) {
        return mConfig.retrieveOption(id);
    }

    @Nullable
    @Override
    public <ValueT> ValueT retrieveOption(@NonNull Option<ValueT> id,
            @Nullable ValueT valueIfMissing) {
        return mConfig.retrieveOption(id, valueIfMissing);
    }

    @Override
    public void findOptions(@NonNull String idSearchString, @NonNull OptionMatcher matcher) {
        mConfig.findOptions(idSearchString, matcher);
    }

    @NonNull
    @Override
    public Set<Option<?>> listOptions() {
        return mConfig.listOptions();
    }

    // Implementations of TargetConfig default methods

    @Nullable
    @Override
    public Class<MeteringRepeating> getTargetClass(
            @Nullable Class<MeteringRepeating> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<MeteringRepeating> storedClass =
                (Class<MeteringRepeating>) retrieveOption(
                        OPTION_TARGET_CLASS,
                        valueIfMissing);
        return storedClass;
    }

    @NonNull
    @Override
    public Class<MeteringRepeating> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<MeteringRepeating> storedClass =
                (Class<MeteringRepeating>) retrieveOption(
                        OPTION_TARGET_CLASS);
        return storedClass;
    }

    @Nullable
    @Override
    public String getTargetName(@Nullable String valueIfMissing) {
        return retrieveOption(OPTION_TARGET_NAME, valueIfMissing);
    }

    @NonNull
    @Override
    public String getTargetName() {
        return retrieveOption(OPTION_TARGET_NAME);
    }



    // Implementations of UseCaseConfig default methods

    @Nullable
    @Override
    public SessionConfig getDefaultSessionConfig(@Nullable SessionConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    @NonNull
    @Override
    public SessionConfig getDefaultSessionConfig() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    @Nullable
    @Override
    public CaptureConfig getDefaultCaptureConfig(@Nullable CaptureConfig valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG, valueIfMissing);
    }

    @NonNull
    @Override
    public CaptureConfig getDefaultCaptureConfig() {
        return retrieveOption(OPTION_DEFAULT_CAPTURE_CONFIG);
    }

    @Nullable
    @Override
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker(
            @Nullable SessionConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER, valueIfMissing);
    }

    @NonNull
    @Override
    public SessionConfig.OptionUnpacker getSessionOptionUnpacker() {
        return retrieveOption(OPTION_SESSION_CONFIG_UNPACKER);
    }

    @Nullable
    @Override
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker(
            @Nullable CaptureConfig.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER, valueIfMissing);
    }

    @NonNull
    @Override
    public CaptureConfig.OptionUnpacker getCaptureOptionUnpacker() {
        return retrieveOption(OPTION_CAPTURE_CONFIG_UNPACKER);
    }

    @Override
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    @Override
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    @Nullable
    @Override
    public CameraSelector getCameraSelector(@Nullable CameraSelector valueIfMissing) {
        return retrieveOption(OPTION_CAMERA_SELECTOR, valueIfMissing);
    }

    @NonNull
    @Override
    public CameraSelector getCameraSelector() {
        return retrieveOption(OPTION_CAMERA_SELECTOR);
    }

    @Nullable
    @Override
    public UseCase.EventCallback getUseCaseEventCallback(
            @Nullable UseCase.EventCallback valueIfMissing) {
        return retrieveOption(OPTION_USE_CASE_EVENT_CALLBACK, valueIfMissing);
    }

    @NonNull
    @Override
    public UseCase.EventCallback getUseCaseEventCallback() {
        return retrieveOption(OPTION_USE_CASE_EVENT_CALLBACK);
    }

    @Override
    public int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT);
    }

    /** Builder for an empty Config */
    public static final class Builder implements
            UseCaseConfig.Builder<MeteringRepeating, MeteringRepeatingConfig, Builder> {

        private final MutableOptionsBundle mMutableConfig;

        /** Creates a new Builder object. */
        public Builder() {
            this(MutableOptionsBundle.create());
        }

        private Builder(MutableOptionsBundle mutableConfig) {
            mMutableConfig = mutableConfig;

            Class<?> oldConfigClass =
                    mutableConfig.retrieveOption(TargetConfig.OPTION_TARGET_CLASS, null);
            if (oldConfigClass != null && !oldConfigClass.equals(MeteringRepeating.class)) {
                throw new IllegalArgumentException(
                        "Invalid target class configuration for "
                                + MeteringRepeatingConfig.Builder.this
                                + ": "
                                + oldConfigClass);
            }

            setTargetClass(MeteringRepeating.class);
        }

        /**
         * Generates a Builder from another Config object
         *
         * @param configuration An immutable configuration to pre-populate this builder.
         * @return The new Builder.
         */
        @NonNull
        public static MeteringRepeatingConfig.Builder fromConfig(
                @NonNull MeteringRepeatingConfig configuration) {
            return new MeteringRepeatingConfig.Builder(MutableOptionsBundle.from(configuration));
        }


        @NonNull
        @Override
        public MutableConfig getMutableConfig() {
            return mMutableConfig;
        }

        @NonNull
        @Override
        public MeteringRepeatingConfig getUseCaseConfig() {
            return new MeteringRepeatingConfig(OptionsBundle.from(mMutableConfig));
        }


        @NonNull
        @Override
        public MeteringRepeating build() {
            return new MeteringRepeating(getUseCaseConfig());
        }

        // Implementations of TargetConfig.Builder default methods

        @NonNull
        @Override
        public Builder setTargetClass(@NonNull Class<MeteringRepeating> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;

        }

        @NonNull
        @Override
        public Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }


        // Implementations of UseCaseConfig.Builder default methods
        @NonNull
        @Override
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @NonNull
        @Override
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @NonNull
        @Override
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @NonNull
        @Override
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @NonNull
        @Override
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @NonNull
        @Override
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
            return this;
        }

        @NonNull
        @Override
        public Builder setUseCaseEventCallback(@NonNull UseCase.EventCallback eventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, eventCallback);
            return this;
        }

    }

}

