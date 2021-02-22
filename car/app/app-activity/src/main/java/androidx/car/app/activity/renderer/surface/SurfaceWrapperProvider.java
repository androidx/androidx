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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Provides the render config for the given {@link TemplateSurfaceView}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class SurfaceWrapperProvider {
    private final TemplateSurfaceView mSurfaceView;

    public SurfaceWrapperProvider(@NonNull TemplateSurfaceView surfaceView) {
        super();
        mSurfaceView = surfaceView;
    }

    /** Creates a new render config for the current state of the holding surface view. */
    @NonNull
    public SurfaceWrapper createSurfaceWrapper() {
        IBinder hostToken = mSurfaceView.getSurfaceToken();
        int width = mSurfaceView.getWidth();
        int height = mSurfaceView.getHeight();
        int displayId = mSurfaceView.getDisplay().getDisplayId();
        int densityDpi = densityDpi();
        Surface surface = mSurfaceView.getHolder().getSurface();
        return new SurfaceWrapper(hostToken, width, height, displayId, densityDpi, surface);
    }

    private int densityDpi() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mSurfaceView.getDisplay().getRealMetrics(displayMetrics);
        return displayMetrics.densityDpi;
    }
}
