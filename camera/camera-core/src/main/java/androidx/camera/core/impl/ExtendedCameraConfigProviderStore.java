/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores {@link CameraConfigProvider} instances which allow building {@link CameraConfig} using
 * keys (extensions modes for example). The provided {@link CameraConfig}s are unique to the device.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ExtendedCameraConfigProviderStore {

    private static final Object LOCK = new Object();

    @GuardedBy("LOCK")
    private static final Map<Object, CameraConfigProvider> CAMERA_CONFIG_PROVIDERS =
            new HashMap<>();

    private ExtendedCameraConfigProviderStore() {
    }

    /**
     * Associates the specified {@link CameraConfigProvider} with the specified key and stores them.
     */
    public static void addConfig(@NonNull Object key, @NonNull CameraConfigProvider provider) {
        synchronized (LOCK) {
            CAMERA_CONFIG_PROVIDERS.put(key, provider);
        }
    }

    /**
     * Retrieves the {@link CameraConfigProvider} associated with the specified key.
     *
     * <p>A default {@link CameraConfigProvider#EMPTY} will be returned if there isn't a
     * {@link CameraConfigProvider} associated with the key.
     */
    @NonNull
    public static CameraConfigProvider getConfigProvider(@NonNull Object key) {
        final CameraConfigProvider provider;
        synchronized (LOCK) {
            provider = CAMERA_CONFIG_PROVIDERS.get(key);
        }

        if (provider == null) {
            return CameraConfigProvider.EMPTY;
        }
        return provider;
    }
}
