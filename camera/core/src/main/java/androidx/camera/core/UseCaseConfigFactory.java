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

/**
 * A Repository for generating use case configurations.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface UseCaseConfigFactory {

    /**
     * Returns the configuration for the given type, or <code>null</code> if the configuration
     * cannot be produced.
     *
     * @param lensFacing The {@link CameraX.LensFacing} that the configuration will target to.
     */
    @Nullable
    <C extends UseCaseConfig<?>> C getConfig(Class<C> configType, CameraX.LensFacing lensFacing);
}
