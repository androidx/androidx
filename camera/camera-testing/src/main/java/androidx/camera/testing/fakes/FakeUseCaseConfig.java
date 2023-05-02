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

package androidx.camera.testing.fakes;

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType;
import androidx.camera.core.resolutionselector.ResolutionSelector;

import java.util.List;
import java.util.UUID;

/** A fake configuration for {@link FakeUseCase}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FakeUseCaseConfig implements UseCaseConfig<FakeUseCase>, ImageOutputConfig {

    private final Config mConfig;

    FakeUseCaseConfig(Config config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    @Override
    public int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
    }

    /** Builder for an empty Config */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder implements
            UseCaseConfig.Builder<FakeUseCase, FakeUseCaseConfig, FakeUseCaseConfig.Builder>,
            ImageOutputConfig.Builder<FakeUseCaseConfig.Builder> {

        private final MutableOptionsBundle mOptionsBundle;
        private final CaptureType mCaptureType;

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
            this(MutableOptionsBundle.create(), captureType);
            mOptionsBundle.insertOption(OPTION_INPUT_FORMAT, inputFormat);
        }

        public Builder(@NonNull Config config, @NonNull CaptureType captureType) {
            mOptionsBundle = MutableOptionsBundle.from(config);
            setTargetClass(FakeUseCase.class);
            mCaptureType = captureType;
        }

        @Override
        @NonNull
        public MutableConfig getMutableConfig() {
            return mOptionsBundle;
        }

        @NonNull
        @Override
        public FakeUseCaseConfig getUseCaseConfig() {
            return new FakeUseCaseConfig(OptionsBundle.from(mOptionsBundle));
        }

        @Override
        @NonNull
        public FakeUseCase build() {
            return new FakeUseCase(getUseCaseConfig(), mCaptureType);
        }

        // Implementations of TargetConfig.Builder default methods

        @Override
        @NonNull
        public Builder setTargetClass(@NonNull Class<FakeUseCase> targetClass) {
            getMutableConfig().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfig().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return this;
        }

        @Override
        @NonNull
        public Builder setTargetName(@NonNull String targetName) {
            getMutableConfig().insertOption(OPTION_TARGET_NAME, targetName);
            return this;
        }

        // Implementations of UseCaseConfig.Builder default methods

        @Override
        @NonNull
        public Builder setDefaultSessionConfig(@NonNull SessionConfig sessionConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return this;
        }

        @Override
        @NonNull
        public Builder setDefaultCaptureConfig(@NonNull CaptureConfig captureConfig) {
            getMutableConfig().insertOption(OPTION_DEFAULT_CAPTURE_CONFIG, captureConfig);
            return this;
        }

        @Override
        @NonNull
        public Builder setSessionOptionUnpacker(
                @NonNull SessionConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_SESSION_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @Override
        @NonNull
        public Builder setCaptureOptionUnpacker(
                @NonNull CaptureConfig.OptionUnpacker optionUnpacker) {
            getMutableConfig().insertOption(OPTION_CAPTURE_CONFIG_UNPACKER, optionUnpacker);
            return this;
        }

        @Override
        @NonNull
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfig().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return this;
        }

        @Override
        @NonNull
        public Builder setCameraSelector(@NonNull CameraSelector cameraSelector) {
            getMutableConfig().insertOption(OPTION_CAMERA_SELECTOR, cameraSelector);
            return this;
        }

        @Override
        @NonNull
        public Builder setUseCaseEventCallback(@NonNull UseCase.EventCallback eventCallback) {
            getMutableConfig().insertOption(OPTION_USE_CASE_EVENT_CALLBACK, eventCallback);
            return this;
        }

        @NonNull
        @Override
        public Builder setTargetAspectRatio(int aspectRatio) {
            getMutableConfig().insertOption(OPTION_TARGET_ASPECT_RATIO, aspectRatio);
            return this;
        }

        @NonNull
        @Override
        public Builder setTargetRotation(int rotation) {
            getMutableConfig().insertOption(OPTION_TARGET_ROTATION, rotation);
            return this;
        }

        @NonNull
        @Override
        public Builder setMirrorMode(@MirrorMode.Mirror int mirrorMode) {
            getMutableConfig().insertOption(OPTION_MIRROR_MODE, mirrorMode);
            return this;
        }

        @NonNull
        @Override
        public Builder setTargetResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION, resolution);
            return this;
        }

        @NonNull
        @Override
        public Builder setDefaultResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_DEFAULT_RESOLUTION, resolution);
            return this;
        }

        @NonNull
        @Override
        public Builder setMaxResolution(@NonNull Size resolution) {
            getMutableConfig().insertOption(OPTION_MAX_RESOLUTION, resolution);
            return this;
        }

        @NonNull
        @Override
        public Builder setSupportedResolutions(
                @NonNull List<Pair<Integer, Size[]>> resolutionsList) {
            getMutableConfig().insertOption(OPTION_SUPPORTED_RESOLUTIONS, resolutionsList);
            return this;
        }

        @NonNull
        @Override
        public Builder setCustomOrderedResolutions(@NonNull List<Size> resolutionsList) {
            getMutableConfig().insertOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, resolutionsList);
            return this;
        }

        @NonNull
        @Override
        public Builder setResolutionSelector(@NonNull ResolutionSelector resolutionSelector) {
            getMutableConfig().insertOption(OPTION_RESOLUTION_SELECTOR, resolutionSelector);
            return this;
        }

        /**
         * Sets specific image format to the fake use case.
         */
        @NonNull
        public Builder setBufferFormat(int imageFormat) {
            getMutableConfig().insertOption(OPTION_INPUT_FORMAT, imageFormat);
            return this;
        }

        @NonNull
        @Override
        public Builder setZslDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @NonNull
        @Override
        public Builder setHighResolutionDisabled(boolean disabled) {
            getMutableConfig().insertOption(OPTION_HIGH_RESOLUTION_DISABLED, disabled);
            return this;
        }

        @NonNull
        @Override
        public Builder setCaptureType(@NonNull CaptureType captureType) {
            getMutableConfig().insertOption(OPTION_CAPTURE_TYPE, captureType);
            return this;
        }
    }
}
