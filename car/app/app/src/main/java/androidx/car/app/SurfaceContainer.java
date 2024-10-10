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

package androidx.car.app;

import android.view.Surface;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.Nullable;

/** A container for the {@link Surface} created by the host and its associated properties. */
@CarProtocol
@KeepFields
public final class SurfaceContainer {
    private final @Nullable Surface mSurface;
    private final int mWidth;
    private final int mHeight;
    private final int mDpi;

    public SurfaceContainer(@Nullable Surface surface, int width, int height, int dpi) {
        mSurface = surface;
        mWidth = width;
        mHeight = height;
        mDpi = dpi;
    }

    // No argument constructor needs for serialization.
    private SurfaceContainer() {
        mSurface = null;
        mWidth = 0;
        mHeight = 0;
        mDpi = 0;
    }

    /** Returns the {@link Surface} held by the host or {@code null} if the surface is not ready. */
    public @Nullable Surface getSurface() {
        return mSurface;
    }

    /** Returns the width of the surface or 0 if the surface is not ready. */
    public int getWidth() {
        return mWidth;
    }

    /** Returns the height of the surface or 0 if the surface is not ready. */
    public int getHeight() {
        return mHeight;
    }

    /** Returns the pixel density of the surface or 0 if the surface is not ready. */
    public int getDpi() {
        return mDpi;
    }

    @Override
    public String toString() {
        return "[" + mSurface + ", " + mWidth + "x" + mHeight + ", dpi: " + mDpi + "]";
    }
}
