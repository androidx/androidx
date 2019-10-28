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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link UseCaseConfigFactory} that uses {@link ConfigProvider}s to provide configurations.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class ExtendableUseCaseConfigFactory implements UseCaseConfigFactory {
    private final Map<Class<?>, ConfigProvider<?>> mDefaultProviders = new HashMap<>();

    /** Inserts or overrides the {@link ConfigProvider} for the given config type. */
    public <C extends Config> void installDefaultProvider(
            Class<C> configType, ConfigProvider<C> defaultProvider) {
        mDefaultProviders.put(configType, defaultProvider);
    }

    @Nullable
    @Override
    public <C extends UseCaseConfig<?>> C getConfig(Class<C> configType,
            CameraX.LensFacing lensFacing) {
        @SuppressWarnings("unchecked") // Providers only could have been inserted with
                // installDefaultProvider(), so the class should return the correct type.
                ConfigProvider<C> provider = (ConfigProvider<C>) mDefaultProviders.get(configType);
        if (provider != null) {
            return provider.getConfig(lensFacing);
        }
        return null;
    }
}
