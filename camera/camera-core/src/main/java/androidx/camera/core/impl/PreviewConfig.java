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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.camera.core.internal.ThreadConfig;

/**
 * Configuration for a {@link Preview} use case.
 */
public final class PreviewConfig
        implements UseCaseConfig<Preview>,
        ImageOutputConfig,
        ThreadConfig {

    // Options declarations
    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    public PreviewConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should be YUV_420_888, when processing is run on the image. Otherwise it is PRIVATE.
     */
    @Override
    public int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT);
    }
}
