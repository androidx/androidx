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

import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamUseCase;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

/** A fake configuration for {@link FakeOtherUseCase}. */
public class FakeOtherUseCaseConfig implements UseCaseConfig<FakeOtherUseCase> {

    private final Config mConfig;

    private FakeOtherUseCaseConfig(Config config) {
        mConfig = config;
    }

    @Override
    public @NonNull Config getConfig() {
        return mConfig;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    @Override
    public UseCaseConfigFactory.@NonNull CaptureType getCaptureType() {
        return UseCaseConfigFactory.CaptureType.PREVIEW;
    }

    @Override
    public int getInputFormat() {
        return ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
    }

    /** Builder for an empty Config */
    public static final class Builder implements
            UseCaseConfig.Builder<FakeOtherUseCase, FakeOtherUseCaseConfig,
                    FakeOtherUseCaseConfig.Builder> {

        private final MutableOptionsBundle mOptionsBundle;

        public Builder() {
            mOptionsBundle = MutableOptionsBundle.create();
            setTargetClass(FakeOtherUseCase.class);
        }

        @Override
        public @NonNull MutableConfig getMutableConfig() {
            return mOptionsBundle;
        }

        @Override
        public @NonNull FakeOtherUseCaseConfig getUseCaseConfig() {
            return new FakeOtherUseCaseConfig(OptionsBundle.from(mOptionsBundle));
        }

        @Override
        public @NonNull FakeOtherUseCase build() {
            return new FakeOtherUseCase(getUseCaseConfig());
        }

        // Implementations of TargetConfig.Builder default methods

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setTargetClass(@NonNull Class<FakeOtherUseCase> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        @Override
        public @NonNull Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setSessionOptionUnpacker(
                SessionConfig.@NonNull OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setCaptureOptionUnpacker(
                CaptureConfig.@NonNull OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public @NonNull Builder setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }

        @Override
        public @NonNull Builder setCaptureType(
                UseCaseConfigFactory.@NonNull CaptureType captureType) {
            getMutableConfig().insertOption(OPTION_CAPTURE_TYPE, captureType);
            return this;
        }

        @Override
        public @NonNull Builder setStreamUseCase(@NonNull StreamUseCase streamUseCase) {
            getMutableConfig().insertOption(OPTION_STREAM_USE_CASE, streamUseCase);
            return this;
        }
    }
}
