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

package androidx.camera.integration.core;

import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewStub;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Utilities for instantiating a {@link SurfaceView} and attaching to an {@link OpenGLRenderer}.
 */
public final class SurfaceViewRenderSurface {
    private static final String TAG = "SurfaceViewRndrSrfc";

    /**
     * Inflates a non-blocking {@link SurfaceView} into the provided {@link ViewStub} and attaches
     * it to the provided {@link OpenGLRenderer}.
     *
     * <p>WARNING: This type of render surface should only be used on specific devices. A
     * non-blocking {@link SurfaceView} will not block the main thread when destroying its
     * internal {@link Surface}, which is known to cause race conditions between the main thread
     * and the rendering thread. Some OpenGL/EGL drivers do not support this usage and may crash
     * on the rendering thread.
     *
     * @param viewStub Stub which will be replaced by SurfaceView.
     * @param renderer Renderer which will be used to update the SurfaceView.
     * @return The inflated SurfaceView.
     */
    @NonNull
    public static SurfaceView inflateNonBlockingWith(@NonNull ViewStub viewStub,
            @NonNull OpenGLRenderer renderer) {
        return inflateWith(viewStub, renderer, /*nonBlocking=*/true);
    }

    /**
     * Inflates a {@link SurfaceView} into the provided {@link ViewStub} and attaches it to the
     * provided {@link OpenGLRenderer}.
     *
     * @param viewStub Stub which will be replaced by SurfaceView.
     * @param renderer Renderer which will be used to update the SurfaceView.
     * @return The inflated SurfaceView.
     */
    @NonNull
    public static SurfaceView inflateWith(@NonNull ViewStub viewStub,
            @NonNull OpenGLRenderer renderer) {
        return inflateWith(viewStub, renderer, /*nonBlocking=*/false);
    }

    @NonNull
    private static SurfaceView inflateWith(@NonNull ViewStub viewStub,
            @NonNull OpenGLRenderer renderer, boolean nonBlocking) {
        Log.d(TAG, "Inflating SurfaceView into view stub (non-blocking = " + nonBlocking + ").");
        if (nonBlocking) {
            warnOnKnownBuggyNonBlockingDevice();
        }
        viewStub.setLayoutResource(R.layout.surface_view_render_surface);

        SurfaceView surfaceView = (SurfaceView) viewStub.inflate();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder holder) {
                Display surfaceViewDisplay = surfaceView.getDisplay();
                if (surfaceViewDisplay != null) {
                    renderer.invalidateSurface(
                            Surfaces.toSurfaceRotationDegrees(surfaceViewDisplay.getRotation()));
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                renderer.attachOutputSurface(holder.getSurface(), new Size(width, height),
                        Surfaces.toSurfaceRotationDegrees(surfaceView.getDisplay().getRotation()));
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // SurfaceView's documentation states that the Surface should only be touched
                // between surfaceCreated() and surfaceDestroyed(). However, many EGL
                // implementations will allow it to be touched but may return errors during
                // drawing operations. Other implementations may crash when those drawing
                // operations are called. In normal operation, we block the main thread until
                // the surface has been detached from the renderer. This is safe, but can cause
                // jank and/or ANRs. In non-blocking mode, we signal to the renderer to detach
                // but do not wait for a signal that the surface has been detached. This will
                // work on some devices with more robust EGL implementations. For devices with
                // crashing EGL implementations TextureView is an alternative which provides
                // stable non-blocking behavior between the main thread and render thread.
                ListenableFuture<Void> detachFuture = renderer.detachOutputSurface();
                if (!nonBlocking) {
                    try {
                        detachFuture.get();
                    } catch (ExecutionException e) {
                        Log.e(TAG, "An error occurred while waiting for surface to detach from "
                                + "the renderer", e.getCause());
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for surface to detach from the "
                                + "renderer.");
                        Thread.currentThread().interrupt(); // Restore the interrupted status
                    }
                }
            }


        });

        return surfaceView;
    }

    private static void warnOnKnownBuggyNonBlockingDevice() {
        // Cuttlefish currently uses swiftshader for its OpenGL and EGL implementations.
        // Swiftshader is not thread-safe, and sometimes will crash in OpenGL or EGL calls if the
        // consumer has already been detached. See b/74108717 for more info.
        if (Build.MODEL.contains("Cuttlefish")) {
            Log.w(TAG, "Running SurfaceView in non-blocking mode on a device with known buggy EGL "
                    + "implementation: Cuttlefish");
        }
    }

    private SurfaceViewRenderSurface() {
    }
}
