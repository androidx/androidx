/*
 * Copyright 2022 The Android Open Source Project
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

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.SessionConfig;

/**
 * Interface for Zero-Shutter Lag image capture control.
 */
interface ZslControl {

    /**
     * Add zero-shutter lag config to {@link SessionConfig}.
     * @param resolution surface resolution.
     * @param sessionConfigBuilder session config builder.
     */
    void addZslConfig(
            @NonNull Size resolution,
            @NonNull SessionConfig.Builder sessionConfigBuilder);
}
