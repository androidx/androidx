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

package androidx.camera.testing.impl;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.GuardedBy;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class creates implementations of PreviewSurfaceProvider that provide Surfaces that have been
 * pre-configured for specific work flows.
 */
public final class SurfaceTextureProvider {
    private static final String TAG = "SurfaceTextureProvider";
    private static final Object DEFAULT_DISPLAY_PIN_LOCK = new Object();

    @GuardedBy("DEFAULT_DISPLAY_PIN_LOCK")
    private static boolean sDefaultDisplayPinned = false;

    private SurfaceTextureProvider() {
    }

    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture}.
     *
     * <p>This is a convenience method for creating a {@link Preview.SurfaceProvider}
     * whose {@link Surface} is backed by a {@link SurfaceTexture}. The returned
     * {@link Preview.SurfaceProvider} is responsible for creating the
     * {@link SurfaceTexture}. The {@link SurfaceTexture} may not be safe to use with
     * {@link TextureView}
     * Example:
     *
     * <pre><code>
     * preview.setSurfaceProvider(createSurfaceTextureProvider(
     *         new SurfaceTextureProvider.SurfaceTextureCallback() {
     *             &#64;Override
     *             public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture) {
     *                 // Use the SurfaceTexture
     *             }
     *
     *             &#64;Override
     *             public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
     *                 surfaceTexture.release();
     *             }
     *         }));
     * </code></pre>
     *
     * @param surfaceTextureCallback callback called when the SurfaceTexture is ready to be
     *                               set/released.
     * @return a {@link Preview.SurfaceProvider} to be used with
     * {@link Preview#setSurfaceProvider(Preview.SurfaceProvider)}.
     */
    public static Preview.@NonNull SurfaceProvider createSurfaceTextureProvider(
            @NonNull SurfaceTextureCallback surfaceTextureCallback) {
        return createSurfaceTextureProvider(surfaceTextureCallback, null);
    }

    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture} with
     * a {@link SurfaceRequest.Result} listener.
     *
     * <p>This is a convenience method for creating a {@link Preview.SurfaceProvider}
     * whose {@link Surface} is backed by a {@link SurfaceTexture}. The returned
     * {@link Preview.SurfaceProvider} is responsible for creating the
     * {@link SurfaceTexture}. The {@link SurfaceTexture} may not be safe to use with
     * {@link TextureView}
     * Example:
     *
     * <pre><code>
     * preview.setSurfaceProvider(createSurfaceTextureProvider(
     *         new SurfaceTextureProvider.SurfaceTextureCallback() {
     *             &#64;Override
     *             public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture) {
     *                 // Use the SurfaceTexture
     *             }
     *
     *             &#64;Override
     *             public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
     *                 surfaceTexture.release();
     *             }
     *         }));
     * </code></pre>
     *
     * @param surfaceTextureCallback callback called when the SurfaceTexture is ready to be
     *                               set/released.
     * @param resultListener listener to receive the {@link SurfaceRequest.Result}.
     * @return a {@link Preview.SurfaceProvider} to be used with
     * {@link Preview#setSurfaceProvider(Preview.SurfaceProvider)}.
     */
    public static Preview.@NonNull SurfaceProvider createSurfaceTextureProvider(
            @NonNull SurfaceTextureCallback surfaceTextureCallback,
            @Nullable Consumer<SurfaceRequest.Result> resultListener) {
        return (surfaceRequest) -> {
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight());
            surfaceTexture.detachFromGLContext();
            surfaceTextureCallback.onSurfaceTextureReady(surfaceTexture,
                    surfaceRequest.getResolution());
            Surface surface = new Surface(surfaceTexture);
            surfaceRequest.provideSurface(surface,
                    CameraXExecutors.directExecutor(),
                    (surfaceResponse) ->  {
                        surface.release();
                        surfaceTextureCallback.onSafeToRelease(surfaceTexture);
                        if (resultListener != null) {
                            resultListener.accept(surfaceResponse);
                        }
                    });
        };
    }

    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture} which
     * is suitable to be used in testing that doesn't actually show camera preview but just need
     * a surface for preview.
     *
     * <p> The {@link SurfaceTexture} will be released when it is no longer needed.
     */
    public static Preview.@NonNull SurfaceProvider createSurfaceTextureProvider() {
        return createSurfaceTextureProvider(new SurfaceTextureCallback() {
            @Override
            public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                    @NonNull Size resolution) {
                // no op
            }

            @Override
            public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                surfaceTexture.release();
            }
        });
    }

    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture}.
     *
     * <p>This method also creates a backing OpenGL thread that will automatically drain frames
     * from the SurfaceTexture as they become available.
     */
    public static Preview.@NonNull SurfaceProvider createAutoDrainingSurfaceTextureProvider() {
        return createAutoDrainingSurfaceTextureProvider(null);
    }

    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture} with
     * a given listener to monitor the frame available event.
     *
     * <p>This method also creates a backing OpenGL thread that will automatically drain frames
     * from the SurfaceTexture as they become available.
     */
    public static Preview.@NonNull SurfaceProvider createAutoDrainingSurfaceTextureProvider(
            SurfaceTexture.@Nullable OnFrameAvailableListener frameAvailableListener) {
        return createAutoDrainingSurfaceTextureProvider(frameAvailableListener, null, null);
    }
    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture}.
     *
     * <p>This method also creates a backing OpenGL thread that will automatically drain frames
     * from the SurfaceTexture as they become available.
     *
     * @param frameAvailableListener listener to be invoked when frame is updated.
     * @param onSurfaceRequestAvailableListener listener to be invoked when the
     *                                          surface request is triggered
     * @param resultListener listener to be invoked for the surface provided.
     */
    public static Preview.@NonNull SurfaceProvider createAutoDrainingSurfaceTextureProvider(
            SurfaceTexture.@Nullable OnFrameAvailableListener frameAvailableListener,
            @Nullable Consumer<SurfaceRequest> onSurfaceRequestAvailableListener,
            @Nullable Consumer<SurfaceRequest.Result> resultListener
    ) {
        if (AndroidUtil.isEmulator(24)) {
            // Use ImageReader to drain frames without OpenGL/EGL to avoid SwiftShader native
            // crashes in eglCreateImageKHR on API 24 emulators (b/563154933, b/539514196).
            return createAutoDrainingImageReaderProvider(
                    frameAvailableListener,
                    onSurfaceRequestAvailableListener,
                    resultListener);
        }
        return (surfaceRequest) -> {
            if (onSurfaceRequestAvailableListener != null) {
                onSurfaceRequestAvailableListener.accept(surfaceRequest);
            }
            ListenableFuture<SurfaceTextureHolder> surfaceTextureFuture =
                    createAutoDrainingSurfaceTextureAsync(surfaceRequest.getResolution().getWidth(),
                            surfaceRequest.getResolution().getHeight(), frameAvailableListener);

            surfaceTextureFuture.addListener(() -> {
                SurfaceTextureHolder holder = null;
                try {
                    holder = surfaceTextureFuture.get();
                    SurfaceTextureHolder finalHolder = holder;
                    Surface surface = new Surface(holder.getSurfaceTexture());
                    holder = null;
                    surfaceRequest.provideSurface(surface,
                            CameraXExecutors.directExecutor(),
                            (surfaceResponse) -> {
                                try {
                                    if (resultListener != null) {
                                        resultListener.accept(surfaceResponse);
                                    }
                                } finally {
                                    try {
                                        finalHolder.close();
                                    } catch (Exception e) {
                                        Logger.e(TAG, "SurfaceTextureHolder failed to close", e);
                                    } finally {
                                        surface.release();
                                    }
                                }
                            });
                } catch (ExecutionException | InterruptedException | RuntimeException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    Logger.e(TAG, "Failed to create auto-draining surface texture", e);
                    if (holder != null) {
                        try {
                            holder.close();
                        } catch (Exception closeException) {
                            Logger.e(TAG, "SurfaceTextureHolder failed to close", closeException);
                        }
                    }
                    surfaceRequest.willNotProvideSurface();
                }
            }, CameraXExecutors.directExecutor());
        };
    }

    private static Preview.@NonNull SurfaceProvider createAutoDrainingImageReaderProvider(
            SurfaceTexture.@Nullable OnFrameAvailableListener frameAvailableListener,
            @Nullable Consumer<SurfaceRequest> onSurfaceRequestAvailableListener,
            @Nullable Consumer<SurfaceRequest.Result> resultListener) {
        return (surfaceRequest) -> {
            if (onSurfaceRequestAvailableListener != null) {
                onSurfaceRequestAvailableListener.accept(surfaceRequest);
            }
            HandlerThread handlerThread = new HandlerThread("CameraX-AutoDrainThread");
            handlerThread.start();
            Handler handler = HandlerCompat.createAsync(handlerThread.getLooper());

            ImageReader imageReader = ImageReader.newInstance(
                    surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight(),
                    ImageFormat.PRIVATE,
                    2);

            SurfaceTexture dummySurfaceTexture =
                    frameAvailableListener != null ? new SurfaceTexture(0) : null;

            imageReader.setOnImageAvailableListener(reader -> {
                boolean frameReceived = false;
                try {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        image.close();
                        frameReceived = true;
                    }
                } catch (IllegalStateException e) {
                    // ImageReader may already be closed.
                }
                if (frameReceived && frameAvailableListener != null) {
                    frameAvailableListener.onFrameAvailable(dummySurfaceTexture);
                }
            }, handler);

            Surface surface = imageReader.getSurface();
            surfaceRequest.provideSurface(surface,
                    CameraXExecutors.directExecutor(),
                    (surfaceResponse) -> {
                        if (resultListener != null) {
                            resultListener.accept(surfaceResponse);
                        }
                        surface.release();
                        imageReader.close();
                        if (dummySurfaceTexture != null) {
                            dummySurfaceTexture.release();
                        }
                        handlerThread.quitSafely();
                    });
        };
    }

    /**
     * Creates a {@link SurfaceTextureHolder} asynchronously that contains a {@link SurfaceTexture}
     * which will automatically drain frames as new frames arrive.
     *
     * @param width                  the width of the SurfaceTexture size
     * @param height                 the height of the SurfaceTexture size.
     * @param frameAvailableListener listener to be invoked when there are new frames.
     */
    public static @NonNull ListenableFuture<SurfaceTextureHolder>
            createAutoDrainingSurfaceTextureAsync(
                    int width,
                    int height,
                    SurfaceTexture.@Nullable OnFrameAvailableListener frameAvailableListener) {
        return CallbackToFutureAdapter.getFuture((completer) -> {
            HandlerThread handlerThread = new HandlerThread("CameraX-AutoDrainThread");
            handlerThread.start();
            Handler handler = HandlerCompat.createAsync(handlerThread.getLooper());
            Executor glExecutor = CameraXExecutors.newHandlerExecutor(handler);
            glExecutor.execute(() -> {
                AutoDrainState state = new AutoDrainState(handlerThread);
                SurfaceTexture surfaceTexture;
                try {
                    EGLContextParams contextParams = createDummyEGLContext();
                    state.mContextParams = contextParams;
                    if (!EGL14.eglMakeCurrent(contextParams.display, contextParams.outputSurface,
                            contextParams.outputSurface, contextParams.context)) {
                        throw new UnsupportedOperationException("Unable to make EGL context "
                                + "current: " + eglErrorString(EGL14.eglGetError()));
                    }
                    GLES20.glGenTextures(1, state.mTextureIds, 0);
                    state.mTextureGenerated = true;
                    surfaceTexture = new SurfaceTexture(state.mTextureIds[0]);
                    state.mSurfaceTexture = surfaceTexture;
                    surfaceTexture.setDefaultBufferSize(width, height);
                } catch (RuntimeException e) {
                    // Release what was created and fail the future instead of leaving it
                    // incomplete.
                    state.release();
                    completer.setException(e);
                    return;
                }
                surfaceTexture.setOnFrameAvailableListener(it -> {
                    try {
                        glExecutor.execute(() -> {
                            if (state.mReleased) {
                                return;
                            }
                            try {
                                it.updateTexImage();
                            } catch (RuntimeException e) {
                                if (!state.mUpdateTexImageFailedLogged) {
                                    state.mUpdateTexImageFailedLogged = true;
                                    Logger.w(TAG, "Failed to updateTexImage", e);
                                } else {
                                    Logger.d(TAG, "Failed to updateTexImage: "
                                            + e.getMessage());
                                }
                            }
                            if (frameAvailableListener != null) {
                                frameAvailableListener.onFrameAvailable(surfaceTexture);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        Logger.d(TAG, "The handler of the glExecutor might have been quit.");
                    }
                });

                SurfaceTextureHolder holder = new SurfaceTextureHolder(surfaceTexture, () -> {
                    try {
                        glExecutor.execute(state::release);
                    } catch (RejectedExecutionException e) {
                        Logger.d(TAG, "The handler of the glExecutor might have been quit.");
                    }
                });
                if (!completer.set(holder)) {
                    // The future was cancelled, so no caller can close the holder.
                    state.release();
                }
            });
            return "createAutoDrainingSurfaceTexture";
        });
    }

    /** Auto-drain resources and state that are confined to the auto-drain thread. */
    private static final class AutoDrainState {
        final HandlerThread mHandlerThread;
        final int[] mTextureIds = new int[1];
        @Nullable EGLContextParams mContextParams;
        boolean mTextureGenerated;
        @Nullable SurfaceTexture mSurfaceTexture;
        boolean mReleased;
        boolean mUpdateTexImageFailedLogged;

        AutoDrainState(@NonNull HandlerThread handlerThread) {
            mHandlerThread = handlerThread;
        }

        /**
         * Releases the created resources and quits the auto-drain thread.
         *
         * <p>Idempotent on the auto-drain looper: previously, calling {@code close()} twice queued
         * two release tasks before {@code quitSafely()} drained the queue, invoking
         * {@code eglTerminate()} twice and stealing another holder's display reference.
         */
        void release() {
            if (mReleased) {
                return;
            }
            mReleased = true;
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
            }
            if (mTextureGenerated) {
                GLES20.glDeleteTextures(1, mTextureIds, 0);
            }
            if (mContextParams != null) {
                terminateEGLContext(mContextParams);
            }
            mHandlerThread.quitSafely();
        }
    }

    /**
     * A holder that contains the {@link SurfaceTexture}. Close() must be called to reclaim the
     * resource.
     */
    public static class SurfaceTextureHolder implements AutoCloseable {
        private final AtomicBoolean mClosed = new AtomicBoolean(false);
        private final SurfaceTexture mSurfaceTexture;
        private final Runnable mCloseRunnable;

        public SurfaceTextureHolder(@NonNull SurfaceTexture surfaceTexture,
                @NonNull Runnable closeRunnable) {
            mSurfaceTexture = surfaceTexture;
            mCloseRunnable = closeRunnable;
        }

        public @NonNull SurfaceTexture getSurfaceTexture() {
            return mSurfaceTexture;
        }

        /** Releases the resources. Calling it more than once has no effect. */
        @Override
        public void close() throws Exception {
            if (mClosed.compareAndSet(false, true)) {
                mCloseRunnable.run();
            }
        }
    }

    /**
     * Pins one process-lifetime {@code eglInitialize()} reference on the default display without a
     * matching {@code eglTerminate()}, mirroring Android's {@code RenderThread}.
     *
     * <p>Headless tests lack a {@code RenderThread} keeping {@code EGL_DEFAULT_DISPLAY} at
     * {@code refs >= 1}. When {@code eglTerminate()} drops {@code refs} to 0 in {@code libEGL},
     * {@code refLock} is released while {@code eglIsInitialized} stays {@code true} during driver
     * teardown, allowing multiple concurrent {@code eglInitialize()} callers to return prematurely
     * and fail {@code eglChooseConfig()} (b/583337721). This pin keeps {@code refs >= 1} across
     * sequential/concurrent holders (and absorbs up to one stray unbalanced external
     * {@code eglTerminate()}).
     */
    private static void pinDefaultDisplay(@NonNull EGLDisplay eglDisplay) {
        synchronized (DEFAULT_DISPLAY_PIN_LOCK) {
            if (sDefaultDisplayPinned) {
                return;
            }
            int[] versions = new int[2];
            if (EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) {
                // Intentionally never eglTerminate()'d so EGL_DEFAULT_DISPLAY stays initialized
                // for the process lifetime while per-instance references remain balanced.
                sDefaultDisplayPinned = true;
            } else {
                Logger.w(TAG, "Unable to pin default EGL display: "
                        + eglErrorString(EGL14.eglGetError()));
            }
        }
    }

    private static @NonNull EGLContextParams createDummyEGLContext() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == null || Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new UnsupportedOperationException("Unable to get default EGL display");
        }
        pinDefaultDisplay(eglDisplay);

        int[] versions = new int[2];
        int majorOffset = 0;
        int minorOffset = 1;
        boolean initialized = EGL14.eglInitialize(eglDisplay, versions, majorOffset, versions,
                minorOffset);
        if (!initialized) {
            throw new UnsupportedOperationException(
                    "Unable to initialize EGL: " + eglErrorString(EGL14.eglGetError()));
        }
        Logger.d(TAG, "Initialized EGL version " + versions[0] + "." + versions[1]);

        EGLContextParams params = new EGLContextParams();
        params.display = eglDisplay;
        try {
            int[] eglConfigAttribs = new int[] {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE };
            EGLConfig[] eglConfigs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            boolean foundConfig = EGL14.eglChooseConfig(eglDisplay, eglConfigAttribs, 0,
                    eglConfigs, 0, 1, numConfigs, 0);
            if (!foundConfig || numConfigs[0] <= 0 || eglConfigs[0] == null) {
                throw new UnsupportedOperationException("Unable to choose a valid EGL config: "
                        + eglErrorString(EGL14.eglGetError()));
            }

            int[] contextAttribs = new int[] {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            params.context = EGL14.eglCreateContext(eglDisplay, eglConfigs[0],
                    /* share_context= */EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            if (params.context == null || Objects.equals(params.context, EGL14.EGL_NO_CONTEXT)) {
                throw new UnsupportedOperationException("Unable to create EGL context: "
                        + eglErrorString(EGL14.eglGetError()));
            }

            // Create a placeholder 1x1 pbuffer for the output surface. This is required since
            // some drivers may not support a surfaceless config
            int[] pbufferAttribs = new int[] {
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
            };
            params.outputSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfigs[0],
                    pbufferAttribs, 0);
            if (params.outputSurface == null
                    || Objects.equals(params.outputSurface, EGL14.EGL_NO_SURFACE)) {
                throw new UnsupportedOperationException("Unable to create EGL pbuffer surface: "
                        + eglErrorString(EGL14.eglGetError()));
            }
            return params;
        } catch (RuntimeException e) {
            // Release what was created, including the eglInitialize() reference above.
            terminateEGLContext(params);
            throw e;
        }
    }

    private static void terminateEGLContext(@NonNull EGLContextParams contextParams) {
        EGL14.eglMakeCurrent(contextParams.display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);

        if (contextParams.outputSurface != null
                && !Objects.equals(contextParams.outputSurface, EGL14.EGL_NO_SURFACE)) {
            EGL14.eglDestroySurface(contextParams.display, contextParams.outputSurface);
        }
        if (contextParams.context != null
                && !Objects.equals(contextParams.context, EGL14.EGL_NO_CONTEXT)) {
            EGL14.eglDestroyContext(contextParams.display, contextParams.context);
        }

        EGL14.eglTerminate(contextParams.display);
        EGL14.eglReleaseThread();
    }

    private static @NonNull String eglErrorString(int error) {
        return "0x" + Integer.toHexString(error);
    }

    /**
     * Callback that is called when the {@link SurfaceTexture} is ready to be set/released.
     *
     * <p> Implement this interface to receive the updates on  {@link SurfaceTexture} used in
     * {@link Preview}. See {@link #createSurfaceTextureProvider(SurfaceTextureCallback)} for
     * code example.
     */
    public interface SurfaceTextureCallback {

        /**
         * Called when a {@link Preview} {@link SurfaceTexture} has been created and is ready to
         * be used by the application.
         *
         * <p> This is called when the preview {@link SurfaceTexture} is created and ready. The
         * most common usage is to set it to a {@link TextureView}. Example:
         * <pre><code>textureView.setSurfaceTexture(surfaceTexture)</code></pre>.
         *
         * <p> To display the {@link SurfaceTexture} without a {@link TextureView},
         * {@link SurfaceTexture#getTransformMatrix(float[])} can be used to transform the
         * preview to natural orientation. For {@link TextureView}, it handles the transformation
         * automatically so that no additional work is needed.
         *
         * @param surfaceTexture {@link SurfaceTexture} created for {@link Preview}.
         * @param resolution     the resolution of the created {@link SurfaceTexture}.
         */
        void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                @NonNull Size resolution);

        /**
         * Called when the {@link SurfaceTexture} is safe to be released.
         *
         * <p> This method is called when the {@link SurfaceTexture} previously provided in
         * {@link #onSurfaceTextureReady(SurfaceTexture, Size)} is no longer being used by the
         * camera system, and it's safe to be released during or after this is called. The
         * implementer is responsible to release the {@link SurfaceTexture} when it's also no
         * longer being used by the app.
         *
         * @param surfaceTexture the {@link SurfaceTexture} to be released.
         */
        void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture);
    }

    static final class EGLContextParams {
        public EGLDisplay display;
        public @Nullable EGLContext context;
        public @Nullable EGLSurface outputSurface;
    }
}
