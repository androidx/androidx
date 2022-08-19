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
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewStub;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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
        AtomicReference<Pair<Surface, SurfaceControl>> surfaceInfoReference =
                new AtomicReference<>(null);

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
                Surface surface = holder.getSurface();

                // Tries to use SurfaceControl to avoid the surface-destroyed problem on devices
                // which have buggy EGL implementation (for example, Cuttlefish).
                if (nonBlocking) {
                    Pair<Surface, SurfaceControl> surfaceInfoPair =
                            createSurfaceControlSurfaceAndReparent(surfaceView, format, width,
                                    height);

                    if (surfaceInfoPair != null) {
                        surface = surfaceInfoPair.first;
                        surfaceInfoReference.set(surfaceInfoPair);
                    }
                }

                renderer.attachOutputSurface(surface, new Size(width, height),
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

                Pair<Surface, SurfaceControl> surfaceInfoPair = surfaceInfoReference.get();
                if (Build.VERSION.SDK_INT >= 29 && surfaceInfoPair != null) {
                    Api29Impl.reparent(surfaceInfoPair.second, null);
                    detachFuture.addListener(surfaceInfoPair.first::release,
                            command -> command.run());
                }
            }
        });

        return surfaceView;
    }

    private static void warnOnKnownBuggyNonBlockingDevice() {
        // Cuttlefish currently uses swiftshader for its OpenGL and EGL implementations.
        // Swiftshader is not thread-safe, and sometimes will crash in OpenGL or EGL calls if the
        // consumer has already been detached. See b/74108717 for more info. This issue is
        // avoided by SurfaceControl if the device API level is 29 or above.
        if (Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT < 29) {
            Log.w(TAG, "Running SurfaceView in non-blocking mode on a device with known buggy EGL "
                    + "implementation: Cuttlefish");
        }
    }

    private SurfaceViewRenderSurface() {
    }

    /**
     * Returns a surface with {@link SurfaceControl} or null if {@link SurfaceControl} can't be
     * supported.
     */
    @Nullable
    static Pair<Surface, SurfaceControl> createSurfaceControlSurfaceAndReparent(
            @NonNull SurfaceView surfaceView, int format, int width, int height) {
        if (Build.VERSION.SDK_INT < 29) {
            return null;
        }

        // Gets the parent surface control from the SurfaceView
        SurfaceControl parentSurfaceControl = Api29Impl.getSurfaceControl(surfaceView);
        // Creates a child surface control with the desired resolution
        SurfaceControl childSurfaceControl = Api29Impl.createSurfaceControl(format, width, height);
        // Immediately creates a surface with the child surface control
        Surface surface = Api29Impl.createSurface(childSurfaceControl);
        // Re-parents the created surface control to the SurfaceView's surface control
        Api29Impl.reparent(childSurfaceControl, parentSurfaceControl);

        return Pair.create(surface, childSurfaceControl);
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static SurfaceControl createSurfaceControl(int format, int width, int height) {
            return new SurfaceControl.Builder()
                    .setName("ChildSurfaceControl")
                    .setFormat(format)
                    .setBufferSize(width, height)
                    .build();
        }

        @DoNotInline
        static Surface createSurface(SurfaceControl from) {
            return new Surface(from);
        }

        @DoNotInline
        static SurfaceControl getSurfaceControl(SurfaceView surfaceView) {
            return surfaceView.getSurfaceControl();
        }

        @DoNotInline
        static void reparent(SurfaceControl sc, SurfaceControl newParent) {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            transaction.reparent(sc, newParent).setVisibility(sc, true).apply();
        }
    }
}
