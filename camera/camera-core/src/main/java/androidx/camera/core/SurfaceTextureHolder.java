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

package androidx.camera.core;

import android.graphics.SurfaceTexture;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * A class that holds a {@link SurfaceTexture}.
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
interface SurfaceTextureHolder {
    /** Returns the held {@link SurfaceTexture}. */
    @NonNull
    SurfaceTexture getSurfaceTexture();

    /**
     * Release the object, including the {@link SurfaceTexture}.
     *
     * <p>Once this has been called the {@link SurfaceTexture} obtained via {@link
     * #getSurfaceTexture()} should no longer be used.
     */
    void release();
}
