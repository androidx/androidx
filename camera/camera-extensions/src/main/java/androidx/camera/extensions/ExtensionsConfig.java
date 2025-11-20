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

package androidx.camera.extensions;

import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Identifier;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.UseCaseConfigFactory;

import org.jspecify.annotations.NonNull;

/**
 * Implementation of CameraConfig which provides the extensions capability.
 */
class ExtensionsConfig implements CameraConfig {
    // Option Declarations:
    // *********************************************************************************************
    public static final Option<Integer> OPTION_EXTENSION_MODE =
            Option.create(
                    "camerax.extensions.extensionMode", int.class);

    private final Config mConfig;

    ExtensionsConfig(Config config) {
        mConfig = config;
    }

    @Override
    public @NonNull Config getConfig() {
        return mConfig;
    }

    @ExtensionMode.Mode
    public int getExtensionMode() {
        return retrieveOption(OPTION_EXTENSION_MODE);
    }

    @Override
    public @NonNull Identifier getCompatibilityId() {
        return retrieveOption(OPTION_COMPATIBILITY_ID);
    }

    static final class Builder implements CameraConfig.Builder<Builder> {
        private final MutableOptionsBundle mConfig = MutableOptionsBundle.create();

        ExtensionsConfig build() {
            return new ExtensionsConfig(mConfig);
        }

        public Builder setExtensionMode(@ExtensionMode.Mode int mode) {
            mConfig.insertOption(OPTION_EXTENSION_MODE, mode);
            return this;
        }

        @Override
        public @NonNull Builder setUseCaseConfigFactory(@NonNull UseCaseConfigFactory factory) {
            mConfig.insertOption(OPTION_USECASE_CONFIG_FACTORY, factory);
            return this;
        }

        @Override
        public @NonNull Builder setCompatibilityId(@NonNull Identifier identifier) {
            mConfig.insertOption(OPTION_COMPATIBILITY_ID, identifier);
            return this;
        }

        @Override
        public @NonNull Builder setUseCaseCombinationRequiredRule(
                int useCaseCombinationRequiredRule) {
            mConfig.insertOption(OPTION_USE_CASE_COMBINATION_REQUIRED_RULE,
                    useCaseCombinationRequiredRule);
            return this;
        }

        @Override
        public @NonNull Builder setSessionProcessor(@NonNull SessionProcessor sessionProcessor) {
            mConfig.insertOption(OPTION_SESSION_PROCESSOR, sessionProcessor);
            return this;
        }

        @Override
        public @NonNull Builder setZslDisabled(boolean disabled) {
            mConfig.insertOption(OPTION_ZSL_DISABLED, disabled);
            return this;
        }

        @Override
        public @NonNull Builder setPostviewSupported(boolean supported) {
            mConfig.insertOption(OPTION_POSTVIEW_SUPPORTED, supported);
            return this;
        }

        @Override
        public Builder setPostviewFormatSelector(PostviewFormatSelector postviewFormatSelector) {
            mConfig.insertOption(OPTION_POSTVIEW_FORMAT_SELECTOR, postviewFormatSelector);
            return this;
        }

        @Override
        public @NonNull Builder setCaptureProcessProgressSupported(boolean supported) {
            mConfig.insertOption(OPTION_CAPTURE_PROCESS_PROGRESS_SUPPORTED, supported);
            return this;
        }
    }
}
