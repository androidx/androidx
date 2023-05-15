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

package androidx.camera.testing;

import android.graphics.SurfaceTexture;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.os.HandlerCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * This class creates implementations of PreviewSurfaceProvider that provide Surfaces that have been
 * pre-configured for specific work flows.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class SurfaceTextureProvider {
    private static final String TAG = "SurfaceTextureProvider";

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
    @NonNull
    public static Preview.SurfaceProvider createSurfaceTextureProvider(
            @NonNull SurfaceTextureCallback surfaceTextureCallback) {
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
                    (surfaceResponse) -> {
                        surface.release();
                        surfaceTextureCallback.onSafeToRelease(surfaceTexture);
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
    @NonNull
    public static Preview.SurfaceProvider createSurfaceTextureProvider() {
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
    @NonNull
    public static Preview.SurfaceProvider createAutoDrainingSurfaceTextureProvider() {
        return createAutoDrainingSurfaceTextureProvider(null);
    }

    /**
     * Creates a {@link Preview.SurfaceProvider} that is backed by a {@link SurfaceTexture}.
     *
     * <p>This method also creates a backing OpenGL thread that will automatically drain frames
     * from the SurfaceTexture as they become available.
     *
     * @param frameAvailableListener listener to be invoked when frame is updated.
     */
    @NonNull
    public static Preview.SurfaceProvider createAutoDrainingSurfaceTextureProvider(
            @Nullable SurfaceTexture.OnFrameAvailableListener frameAvailableListener
    ) {
        return (surfaceRequest) -> {
            HandlerThread handlerThread = new HandlerThread(String.format("CameraX"
                    + "-AutoDrainThread-%x", surfaceRequest.hashCode()));
            handlerThread.start();
            Handler handler = HandlerCompat.createAsync(handlerThread.getLooper());
            Executor glContextExecutor = CameraXExecutors.newHandlerExecutor(handler);
            ListenableFuture<SurfaceTextureHolder> surfaceTextureFuture =
                    createAutoDrainingSurfaceTextureAsync(glContextExecutor,
                            surfaceRequest.getResolution().getWidth(),
                            surfaceRequest.getResolution().getHeight(), frameAvailableListener,
                            handlerThread::quitSafely);

            surfaceTextureFuture.addListener(() -> {
                try {
                    SurfaceTextureHolder holder = surfaceTextureFuture.get();
                    surfaceRequest.provideSurface(new Surface(holder.getSurfaceTexture()),
                            glContextExecutor,
                            (surfaceResponse) -> {
                                try {
                                    holder.close();
                                } catch (Exception e) {
                                    throw new AssertionError("SurfaceTextureHolder failed"
                                            + " to close", e);
                                }
                            });
                } catch (Exception e) {
                    // Should never happen
                    throw new AssertionError("Failed to create auto-draining surface "
                            + "texture",
                            e);
                }
            }, glContextExecutor);
        };
    }

    /**
     * Creates a {@link SurfaceTextureHolder} asynchronously that contains a {@link SurfaceTexture}
     * which will automatically drain frames as new frames arrive.
     *
     * @param glExecutor             the executor where the GL codes will run.
     * @param width                  the width of the SurfaceTexture size
     * @param height                 the height of the SurfaceTexture size.
     * @param frameAvailableListener listener to be invoked when there are new frames.
     * @param onClosed               runnable which will be called after all resources managed by
     *                               the SurfaceTextureHolder have been released.
     */
    @NonNull
    public static ListenableFuture<SurfaceTextureHolder> createAutoDrainingSurfaceTextureAsync(
            @NonNull Executor glExecutor,
            int width,
            int height,
            @Nullable SurfaceTexture.OnFrameAvailableListener frameAvailableListener,
            @Nullable Runnable onClosed) {
        return CallbackToFutureAdapter.getFuture((completer) -> {
            glExecutor.execute(() -> {
                EGLContextParams contextParams = createDummyEGLContext();
                EGL14.eglMakeCurrent(contextParams.display, contextParams.outputSurface,
                        contextParams.outputSurface, contextParams.context);
                int[] textureIds = new int[1];
                GLES20.glGenTextures(1, textureIds, 0);
                SurfaceTexture surfaceTexture = new SurfaceTexture(textureIds[0]);
                surfaceTexture.setDefaultBufferSize(width, height);
                surfaceTexture.setOnFrameAvailableListener(it ->
                        glExecutor.execute(() -> {
                            it.updateTexImage();
                            if (frameAvailableListener != null) {
                                frameAvailableListener.onFrameAvailable(surfaceTexture);
                            }
                        }));

                completer.set(
                        new SurfaceTextureHolder(surfaceTexture, () -> glExecutor.execute(() -> {
                            surfaceTexture.release();
                            GLES20.glDeleteTextures(1, textureIds, 0);
                            terminateEGLContext(contextParams);
                            if (onClosed != null) {
                                onClosed.run();
                            }
                        })));
            });
            return "createAutoDrainingSurfaceTexture";
        });
    }

    /**
     * A holder that contains the {@link SurfaceTexture}. Close() must be called to reclaim the
     * resource.
     */
    public static class SurfaceTextureHolder implements AutoCloseable {
        private final SurfaceTexture mSurfaceTexture;
        private final Runnable mCloseRunnable;

        public SurfaceTextureHolder(@NonNull SurfaceTexture surfaceTexture,
                @NonNull Runnable closeRunnable) {
            mSurfaceTexture = surfaceTexture;
            mCloseRunnable = closeRunnable;
        }

        @NonNull
        public SurfaceTexture getSurfaceTexture() {
            return mSurfaceTexture;
        }

        @Override
        public void close() throws Exception {
            mCloseRunnable.run();
        }
    }

    @NonNull
    private static EGLContextParams createDummyEGLContext() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new UnsupportedOperationException("Unable to get default EGL display");
        }

        int[] versions = new int[2];
        int majorOffset = 0;
        int minorOffset = 1;
        boolean initialized = EGL14.eglInitialize(eglDisplay, versions, majorOffset, versions,
                minorOffset);
        if (!initialized) {
            throw new UnsupportedOperationException("Unable to initialize EGL");
        }
        Logger.d(TAG, "Initialized EGL version " + versions[0] + "." + versions[1]);

        int[] eglConfigAttribs = new int[] {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE };
        EGLConfig[] eglConfigs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        boolean foundConfig = EGL14.eglChooseConfig(eglDisplay, eglConfigAttribs, 0, eglConfigs,
                0, 1, numConfigs, 0);
        if (!foundConfig) {
            throw new UnsupportedOperationException("Unable to choose a valid EGL config");
        }

        int[] contextAttribs = new int[] {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, eglConfigs[0],
                /* share_context= */EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
        if (Objects.equals(eglContext, EGL14.EGL_NO_CONTEXT)) {
            throw new UnsupportedOperationException("Unable to create EGL context");
        }

        // Create a placeholder 1x1 pbuffer for the output surface. This is required since some
        // drivers may not support a surfaceless config
        int[] pbufferAttribs = new int[] {
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
        };
        EGLSurface eglPbuffer = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfigs[0],
                pbufferAttribs, 0);

        EGLContextParams returnParams = new EGLContextParams();
        returnParams.display = eglDisplay;
        returnParams.context = eglContext;
        returnParams.outputSurface = eglPbuffer;

        return returnParams;
    }

    private static void terminateEGLContext(@NonNull EGLContextParams contextParams) {
        EGL14.eglMakeCurrent(contextParams.display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);

        EGL14.eglDestroySurface(contextParams.display, contextParams.outputSurface);
        EGL14.eglDestroyContext(contextParams.display, contextParams.context);

        EGL14.eglTerminate(contextParams.display);
    }

    /**
     * Callback that is called when the {@link SurfaceTexture} is ready to be set/released.
     *
     * <p> Implement this interface to receive the updates on  {@link SurfaceTexture} used in
     * {@link Preview}. See {@link #createSurfaceTextureProvider(SurfaceTextureCallback)} for
     * code example.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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
        public EGLContext context;
        public EGLSurface outputSurface;
    }
}
