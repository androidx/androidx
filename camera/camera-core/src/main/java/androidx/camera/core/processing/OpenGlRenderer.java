/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenGLRenderer renders texture image to the output surface.
 *
 * <p>OpenGLRenderer's methods must run on the same thread, so called GL thread. The GL thread is
 * locked as the thread running the {@link #init()} method, otherwise an
 * {@link IllegalStateException} will be thrown when other methods are called.
 */
@WorkerThread
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class OpenGlRenderer {
    static {
        System.loadLibrary("camerax_core_opengl_renderer_jni");
    }

    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    private final ThreadLocal<Long> mNativeContext = new ThreadLocal<>();

    /**
     * Initializes the OpenGLRenderer
     *
     * <p>Initialization must be done before calling other methods, otherwise an
     * {@link IllegalStateException} will be thrown. Following methods must run on the same
     * thread as this method, so called GL thread, otherwise an {@link IllegalStateException}
     * will be thrown.
     *
     * @throws IllegalStateException if the renderer is already initialized.
     */
    public void init() {
        checkInitializedOrThrow(false);
        mNativeContext.set(initContext());
        mInitialized.set(true);
    }

    /**
     * Releases the OpenGLRenderer
     *
     * @throws IllegalStateException if the caller doesn't run on the GL thread.
     */
    public void release() {
        if (!mInitialized.getAndSet(false)) {
            return;
        }
        long nativeContext = getNativeContextOrThrow();
        closeContext(nativeContext);
        mNativeContext.remove();
    }

    /**
     * Set the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    public void setOutputSurface(@NonNull Surface surface) {
        checkInitializedOrThrow(true);
        long nativeContext = getNativeContextOrThrow();

        setWindowSurface(nativeContext, surface);
    }

    /**
     * Gets the texture name.
     *
     * @return the texture name
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    public int getTextureName() {
        checkInitializedOrThrow(true);
        long nativeContext = getNativeContextOrThrow();

        return getTexName(nativeContext);
    }

    /**
     * Renders the texture image to the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    public void render(long timestampNs, @NonNull float[] textureTransform) {
        checkInitializedOrThrow(true);
        long nativeContext = getNativeContextOrThrow();

        renderTexture(nativeContext, timestampNs, textureTransform);
    }

    private void checkInitializedOrThrow(boolean shouldInitialized) {
        boolean result = shouldInitialized == mInitialized.get();
        String message = shouldInitialized ? "OpenGlRenderer is not initialized"
                : "OpenGlRenderer is already initialized";
        Preconditions.checkState(result, message);
    }

    private long getNativeContextOrThrow() {
        Long nativeContext = mNativeContext.get();
        Preconditions.checkState(nativeContext != null,
                "Method call must be called on the GL thread.");
        return nativeContext;
    }

    private static native long initContext();

    private static native boolean setWindowSurface(long nativeContext, @Nullable Surface surface);

    private static native int getTexName(long nativeContext);

    private static native boolean renderTexture(
            long nativeContext,
            long timestampNs,
            @NonNull float[] textureTransform);

    private static native void closeContext(long nativeContext);
}
