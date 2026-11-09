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

package androidx.camera.core.streamsharing;

import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.internal.ThreadConfig;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for a {@link StreamSharing} use case.
 *
 * <p> This class is a thin wrapper of the underlying {@link OptionsBundle}. Instead of adding
 * getters and setters to this class, one should modify the config using
 * {@link MutableConfig#insertOption} and {@link MutableConfig#retrieveOption} directly.
 */
public class StreamSharingConfig implements UseCaseConfig<StreamSharing>,
        ImageOutputConfig,
        ThreadConfig {
    static final Config.Option<List<UseCaseConfigFactory.CaptureType>> OPTION_CAPTURE_TYPES =
            Config.Option.create("camerax.core.streamSharing.captureTypes",
                    List.class);

    @SuppressWarnings("unchecked")
    static final Config.Option<List<UseCaseConfig<?>>> OPTION_CHILDREN_CONFIGS =
            (Config.Option<List<UseCaseConfig<?>>>) (Config.Option<?>) Config.Option.create(
                    "camerax.core.streamSharing.childrenConfigs",
                    List.class);

    private final OptionsBundle mConfig;

    /** Creates a new configuration instance. */
    StreamSharingConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    @Override
    public @NonNull Config getConfig() {
        return mConfig;
    }

    public @NonNull List<UseCaseConfigFactory.CaptureType> getCaptureTypes() {
        return retrieveOption(OPTION_CAPTURE_TYPES);
    }

    /**
     * Returns the merged {@link UseCaseConfig}s of the children wrapped by {@link StreamSharing}.
     */
    public @NonNull List<UseCaseConfig<?>> getChildrenConfigs() {
        return retrieveOption(OPTION_CHILDREN_CONFIGS, Collections.emptyList());
    }
}
