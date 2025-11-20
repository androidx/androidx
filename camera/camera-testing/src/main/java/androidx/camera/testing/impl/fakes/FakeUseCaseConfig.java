/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

import android.graphics.ImageFormat;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.camera.core.DynamicRange;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageInputConfig;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamUseCase;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType;
import androidx.camera.core.resolutionselector.ResolutionSelector;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

/** A fake configuration for {@link FakeUseCase}. */
public class FakeUseCaseConfig implements UseCaseConfig<FakeUseCase>, ImageOutputConfig {

    private final Config mConfig;

    FakeUseCaseConfig(Config config) {
        mConfig = config;
    }

    @Override
    public @NonNull Config getConfig() {
        return mConfig;
    }

    @Override
    public int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
    }

    @Override
    public @NonNull CaptureType getCaptureType() {
        return retrieveOption(OPTION_CAPTURE_TYPE);
    }

    /** Builder for an empty Config */
    public static final class Builder implements
            UseCaseConfig.Builder<FakeUseCase, FakeUseCaseConfig, FakeUseCaseConfig.Builder>,
            ImageOutputConfig.Builder<FakeUseCaseConfig.Builder>,
            ImageInputConfig.Builder<FakeUseCaseConfig.Builder> {

        private final MutableOptionsBundle mOptionsBundle;

        public Builder() {
            this(MutableOptionsBundle.create(), CaptureType.PREVIEW);
        }

        public Builder(@NonNull Config config) {
            this(config, CaptureType.PREVIEW);
        }

        public Builder(@NonNull CaptureType captureType) {
            this(MutableOptionsBundle.create(), captureType);
        }

        public Builder(@NonNull CaptureType captureType, int inputFormat) {
            this(MutableOptionsBundle.create(), captureType, inputFormat);
        }

        public Builder(@NonNull Config config, @NonNull CaptureType captureType) {
            this(config, captureType, ImageFormat.UNKNOWN);
        }

        public Builder(@NonNull Config config, @NonNull CaptureType captureType, int inputFormat) {
            mOptionsBundle = MutableOptionsBundle.from(config);
            setTargetClass(FakeUseCase.class);
            mOptionsBundle.insertOption(OPTION_CAPTURE_TYPE, captureType);
            if (inputFormat != ImageFormat.UNKNOWN) {
                mOptionsBundle.insertOption(OPTION_INPUT_FORMAT, inputFormat);
            }
        }

        @Override
        public @NonNull MutableConfig getMutableConfig() {
            return mOptionsBundle;
        }

        @Override
        public @NonNull FakeUseCaseConfig getUseCaseConfig() {
            return new FakeUseCaseConfig(OptionsBundle.from(mOptionsBundle));
        }

        @Override
        public @NonNull FakeUseCase build() {
            return new FakeUseCase(getUseCaseConfig());
        }

        // Implementations of TargetConfig.Builder default methods

        @Override
        public @NonNull Builder setTargetClass(@NonNull Class<FakeUseCase> targetClass) {
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

        @Override
        public @NonNull Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @Override
        public @NonNull Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @Override
        public @NonNull Builder setSessionOptionUnpacker(
                SessionConfig.@NonNull OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @Override
        public @NonNull Builder setDynamicRange(
                @NonNull DynamicRange dynamicRange) {
            getMutableConfig().insertOption(OPTION_INPUT_DYNAMIC_RANGE, dynamicRange);
            return this;
        }

        @Override
        public @NonNull Builder setCaptureOptionUnpacker(
                CaptureConfig.@NonNull OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @Override
        public @NonNull Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @Override
        public @NonNull Builder setTargetAspectRatio(int aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return this;
        }

        @Override
        public @NonNull Builder setTargetRotation(int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        @Override
        public @NonNull Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            getMutableConfig().insertOption(OPTION_MIRROR_MODE, mirrorMode);
            return this;
        }

        @Override
        public @NonNull Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, resolution);
            return this;
        }

        @Override
        public @NonNull Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_DEFAULT_RESOLUTION, resolution);
            return this;
        }

        @Override
        public @NonNull Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        @Override
        public @NonNull Builder setSupportedResolutions(
                @NonNull List<Pair<Integer, Size[]>> resolutionsList) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutionsList);
            return this;
        }

        @Override
        public @NonNull Builder setCustomOrderedResolutions(@NonNull List<Size> resolutionsList) {
            getMutableConfig().insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, resolutionsList);
            return this;
        }

        @Override
        public @NonNull Builder setResolutionSelector(
                @NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR, resolutionSelector);
            return this;
        }

        /**
         * Sets specific image format to the fake use case.
         */
        public @NonNull Builder setBufferFormat(int imageFormat) {
            getMutableConfig().insertOption(OPTION_INPUT_FORMAT, imageFormat);
            return this;
        }

        @Override
        public @NonNull Builder setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @Override
        public @NonNull Builder setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }

        @Override
        public @NonNull Builder setCaptureType(@NonNull CaptureType captureType) {
            getMutableConfig().insertOption(OPTION_CAPTURE_TYPE, captureType);
            return this;
        }

        @Override
        public @NonNull Builder setStreamUseCase(
                @NonNull StreamUseCase streamUseCase) {
            getMutableConfig().insertOption(OPTION_STREAM_USE_CASE, streamUseCase);
            return this;
        }

        /** Sets the session type to the fake use case. */
        public @NonNull Builder setSessionType(int sessionType) {
            getMutableConfig().insertOption(OPTION_SESSION_TYPE, sessionType);
            return this;
        }

        /**
         * Sets specific target frame rate to the fake use case.
         */
        public @NonNull Builder setTargetFrameRate(@NonNull Range<Integer> targetFrameRate) {
            getMutableConfig().insertOption(OPTION_TARGET_FRAME_RATE, targetFrameRate);
            return this;
        }
    }
}
