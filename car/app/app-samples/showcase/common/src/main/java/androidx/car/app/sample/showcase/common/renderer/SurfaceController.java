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

package androidx.car.app.sample.showcase.common.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;

import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A very simple implementation of a renderer for the app's background surface. */
public final class SurfaceController implements DefaultLifecycleObserver {
    private static final String TAG = "showcase";

    private final DefaultRenderer mDefaultRenderer;
    private @Nullable Renderer mOverrideRenderer;

    private final CarContext mCarContext;
    @Nullable Surface mSurface;
    @Nullable Rect mVisibleArea;
    @Nullable Rect mStableArea;
    private final SurfaceCallback mSurfaceCallback =
            new SurfaceCallback() {
                @Override
                public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
                    synchronized (SurfaceController.this) {
                        Log.i(TAG, "Surface available " + surfaceContainer);
                        mSurface = surfaceContainer.getSurface();
                        renderFrame();
                    }
                }

                @Override
                public void onVisibleAreaChanged(@NonNull Rect visibleArea) {
                    synchronized (SurfaceController.this) {
                        Log.i(TAG, "Visible area changed " + mSurface + ". stableArea: "
                                + mStableArea + " visibleArea:" + visibleArea);
                        mVisibleArea = visibleArea;
                        renderFrame();
                    }
                }

                @Override
                public void onStableAreaChanged(@NonNull Rect stableArea) {
                    synchronized (SurfaceController.this) {
                        Log.i(TAG, "Stable area changed " + mSurface + ". stableArea: "
                                + mStableArea + " visibleArea:" + mVisibleArea);
                        mStableArea = stableArea;
                        renderFrame();
                    }
                }

                @Override
                public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
                    synchronized (SurfaceController.this) {
                        mSurface = null;
                    }
                }
            };

    public SurfaceController(@NonNull CarContext carContext, @NonNull Lifecycle lifecycle) {
        mCarContext = carContext;
        mDefaultRenderer = new DefaultRenderer();
        lifecycle.addObserver(this);
    }

    /** Callback called when the car configuration changes. */
    public void onCarConfigurationChanged() {
        renderFrame();
    }

    /** Tells the controller whether to override the default renderer. */
    public void overrideRenderer(@Nullable Renderer renderer) {

        if (mOverrideRenderer == renderer) {
            return;
        }

        if (mOverrideRenderer != null) {
            mOverrideRenderer.disable();
        } else {
            mDefaultRenderer.disable();
        }

        mOverrideRenderer = renderer;

        if (mOverrideRenderer != null) {
            mOverrideRenderer.enable(this::renderFrame);
        } else {
            mDefaultRenderer.enable(this::renderFrame);
        }
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Log.i(TAG, "SurfaceController created");
        mCarContext.getCarService(AppManager.class).setSurfaceCallback(mSurfaceCallback);
    }

    void renderFrame() {
        if (mSurface == null || !mSurface.isValid()) {
            // Surface is not available, or has been destroyed, skip this frame.
            return;
        }
        Canvas canvas = mSurface.lockCanvas(null);

        // Clear the background.
        canvas.drawColor(mCarContext.isDarkMode() ? Color.DKGRAY : Color.LTGRAY);

        if (mOverrideRenderer != null) {
            mOverrideRenderer.renderFrame(canvas, mVisibleArea, mStableArea);
        } else {
            mDefaultRenderer.renderFrame(canvas, mVisibleArea, mStableArea);
        }
        mSurface.unlockCanvasAndPost(canvas);

    }
}
