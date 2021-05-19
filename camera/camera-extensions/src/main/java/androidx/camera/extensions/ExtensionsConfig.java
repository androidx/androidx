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

import androidx.annotation.NonNull;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.ReadableConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;

/**
 * Implementation of CameraConfig which provides the extensions capability.
 */
class ExtensionsConfig implements ReadableConfig, CameraConfig {
    // Option Declarations:
    // *********************************************************************************************

    public static final Option<Integer> OPTION_EXTENSION_MODE =
            Option.create(
                    "camerax.extensions.extensionMode", int.class);

    private final Config mConfig;

    ExtensionsConfig(Config config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    @ExtensionMode.Mode
    public int getExtensionMode() {
        return retrieveOption(OPTION_EXTENSION_MODE);
    }

    @Override
    @NonNull
    public UseCaseConfigFactory getUseCaseConfigFactory() {
        return retrieveOption(OPTION_USECASE_CONFIG_FACTORY);
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

        @NonNull
        @Override
        public Builder setCameraFilter(@NonNull CameraFilter cameraFilter) {
            mConfig.insertOption(OPTION_CAMERA_FILTER, cameraFilter);
            return this;
        }

        @NonNull
        @Override
        public Builder setUseCaseConfigFactory(@NonNull UseCaseConfigFactory factory) {
            mConfig.insertOption(OPTION_USECASE_CONFIG_FACTORY, factory);
            return this;
        }
    }
}
