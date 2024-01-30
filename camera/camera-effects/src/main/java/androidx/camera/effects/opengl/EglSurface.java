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

package androidx.camera.effects.opengl;

import android.opengl.EGLSurface;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * A Surface with its corresponding EGLSurface and size.
 */
@AutoValue
abstract class EglSurface {

    @NonNull
    static EglSurface of(@NonNull EGLSurface eglSurface, @Nullable Surface surface, int width,
            int height) {
        return new AutoValue_EglSurface(eglSurface, surface, width, height);
    }

    /**
     * {@link EGLSurface} created based on the {@link #getSurface()}. If {@link #getSurface()} is
     * null, then this value is based on Pbuffer.
     */
    @NonNull
    abstract EGLSurface getEglSurface();

    @Nullable
    abstract Surface getSurface();

    abstract int getWidth();

    abstract int getHeight();
}
