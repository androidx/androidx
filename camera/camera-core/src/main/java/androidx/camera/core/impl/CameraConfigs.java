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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utility methods for operating on {@link CameraConfig} instances.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraConfigs {
    private static final CameraConfig EMPTY_CONFIG = new EmptyCameraConfig();

    /**
     * Gets the empty config instance.
     */
    @NonNull
    public static CameraConfig emptyConfig() {
        return EMPTY_CONFIG;
    }

    static final class EmptyCameraConfig implements CameraConfig {
        private final Identifier mIdentifier = Identifier.create(new Object());

        @NonNull
        @Override
        public Identifier getCompatibilityId() {
            return mIdentifier;
        }
        @NonNull
        @Override
        public Config getConfig() {
            return OptionsBundle.emptyBundle();
        }
    }

    private CameraConfigs() {
    }
}
