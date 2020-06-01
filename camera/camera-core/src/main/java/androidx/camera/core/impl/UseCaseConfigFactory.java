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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.InitializationException;

/**
 * A Repository for generating use case configurations.
 */
public interface UseCaseConfigFactory {

    /**
     * Interface for deferring creation of a UseCaseConfigFactory.
     */
    interface Provider {
        /**
         * Creates a new, initialized instance of a UseCaseConfigFactory.
         *
         * @param context the android context
         * @return the factory instance
         * @throws InitializationException if it fails to create the factory
         */
        @NonNull
        UseCaseConfigFactory newInstance(@NonNull Context context) throws InitializationException;
    }

    /**
     * Returns the configuration for the given type, or <code>null</code> if the configuration
     * cannot be produced.
     *
     * @param cameraInfo The {@link CameraInfo} of the camera that the configuration will target
     *                   to, null if it doesn't target to any camera.
     */
    @Nullable
    <C extends UseCaseConfig<?>> C getConfig(@NonNull Class<C> configType,
            @Nullable CameraInfo cameraInfo);
}
