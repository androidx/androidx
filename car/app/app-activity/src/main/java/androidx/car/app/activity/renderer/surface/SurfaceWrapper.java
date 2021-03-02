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

package androidx.car.app.activity.renderer.surface;

import static java.util.Objects.requireNonNull;

import android.os.IBinder;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class holding the information needed to render the content on a surface.
 */
public final class SurfaceWrapper {
    @Keep
    @Nullable
    private IBinder mHostToken;
    @Keep
    private int mWidth;
    @Keep
    private int mHeight;
    @Keep
    private int mDisplayId;
    @Keep
    private int mDensityDpi;
    @Keep
    @Nullable
    private Surface mSurface;

    /**
     * Creates a {@link SurfaceWrapper}.
     *
     * @param hostToken  a token used for constructing SurfaceControlViewHost. see
     *                   {@link SurfaceView} for more details
     * @param width      the width of the surface view
     * @param height     the height of the surface view
     * @param displayId  the ID of the display showing the surface
     * @param densityDpi the density of the display showing the surface
     * @param surface    the surface for which the wrapper is created
     */
    public SurfaceWrapper(@Nullable IBinder hostToken, int width, int height, int displayId,
            int densityDpi, @NonNull Surface surface) {
        mHostToken = hostToken;
        mWidth = width;
        mHeight = height;
        mDisplayId = displayId;
        mDensityDpi = densityDpi;
        mSurface = surface;
    }

    /** Empty constructor needed for serializations. **/
    private SurfaceWrapper() {
    }

    /**
     * Returns the host token corresponding to the {@link SurfaceView} contained in this class.
     */
    @Nullable
    public IBinder getHostToken() {
        return mHostToken;
    }

    /**
     * Returns the width of the {@link SurfaceView} contained in this class.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the width of the {@link SurfaceView} contained in this class.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Returns the display id of the {@link android.view.Display} for the {@link SurfaceView}
     * contained in this class.
     */
    public int getDisplayId() {
        return mDisplayId;
    }

    /**
     * Returns the display id of the {@link SurfaceView} contained in this class.
     */
    public int getDensityDpi() {
        return mDensityDpi;
    }

    /**
     * Returns the screen density of the {@link android.view.Display} for the {@link SurfaceView}
     * contained in this class.
     */
    @NonNull
    public Surface getSurface() {
        return requireNonNull(mSurface);
    }
}
