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

import static androidx.camera.effects.opengl.Utils.checkEglErrorOrThrow;
import static androidx.core.util.Preconditions.checkState;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.util.Objects;

/**
 * Manages OpenGL configurations.
 *
 * <p>Allows registering and unregistering output Surfaces and manages their corresponding
 * {@link EGLSurface}.
 */
@RequiresApi(21)
public class GlContext {

    private static final String TAG = "GlContext";

    // EGL setup
    @Nullable
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    @Nullable
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    @Nullable
    private EGLConfig mEglConfig = null;

    // Current output Surface being drawn to.
    @Nullable
    @SuppressWarnings("UnusedVariable")
    private EglSurface mCurrentSurface = null;
    // A temporary output Surface. This is used when no Surface has been registered yet.
    @Nullable
    private EglSurface mTempSurface = null;

    void init() {
        // Create EGLDisplay.
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (Objects.equals(eglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new IllegalStateException("Unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new IllegalStateException("Unable to initialize EGL14");
        }

        // Create EGLConfig.
        int rgbBits = 8;
        int alphaBits = 8;
        int renderType = EGL14.EGL_OPENGL_ES2_BIT;
        int recordableAndroid = EGL14.EGL_TRUE;
        int[] attribToChooseConfig = {
                EGL14.EGL_RED_SIZE, rgbBits,
                EGL14.EGL_GREEN_SIZE, rgbBits,
                EGL14.EGL_BLUE_SIZE, rgbBits,
                EGL14.EGL_ALPHA_SIZE, alphaBits,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_RENDERABLE_TYPE, renderType,
                EGLExt.EGL_RECORDABLE_ANDROID, recordableAndroid,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribToChooseConfig, 0, configs, 0, configs.length,
                numConfigs, 0
        )) {
            throw new IllegalStateException("Unable to find a suitable EGLConfig");
        }
        EGLConfig eglConfig = configs[0];
        int[] attribToCreateContext = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        // Create EGLContext.
        EGLContext eglContext = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                attribToCreateContext, 0
        );
        checkEglErrorOrThrow("eglCreateContext");
        int[] values = new int[1];
        EGL14.eglQueryContext(
                eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values,
                0
        );
        Logger.d(TAG, "EGLContext created, client version " + values[0]);

        // All successful. Track the created objects.
        mEglDisplay = eglDisplay;
        mEglConfig = eglConfig;
        mEglContext = eglContext;

        // Create a temporary surface to make it current.
        mTempSurface = create1x1PBufferSurface();
        makeCurrent(mTempSurface);
    }

    /**
     * Registers the given {@link Surface} as an output surface.
     *
     * <p>Once registered, the corresponding {@link EglSurface} can be used in
     * {@link #drawAndSwap}.
     */
    void registerSurface(@NonNull Surface surface) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Unregisters the given {@link Surface} as an output surface.
     *
     * <p>Once unregistered, calling {@link #drawAndSwap} will no longer be effective.
     */
    void unregisterSurface(@NonNull Surface surface) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Draws the current bound texture to the given {@link Surface}.
     *
     * <p>No-ops if the given {@link Surface} is not registered.
     *
     * @param timestampNs The timestamp of the frame in nanoseconds.
     */
    void drawAndSwap(@NonNull Surface surface, long timestampNs) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    boolean release() {
        if (!isInitialized()) {
            return false;
        }
        EGL14.eglMakeCurrent(
                mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
        );

        // TODO: Destroy registered EGL surfaces.

        // Destroy the temporary surface.
        if (mTempSurface != null) {
            EGL14.eglDestroySurface(mEglDisplay, mTempSurface.getEglSurface());
            mTempSurface = null;
        }
        mCurrentSurface = null;

        // Destroy EGLContext and terminate display.
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        EGL14.eglTerminate(mEglDisplay);
        EGL14.eglReleaseThread();

        // Clear the created configurations.
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
        mEglConfig = null;
        return true;
    }

    // --- Private methods ---

    private void makeCurrent(EglSurface eglSurface) {
        checkInitialized();
        if (!EGL14.eglMakeCurrent(mEglDisplay, eglSurface.getEglSurface(),
                eglSurface.getEglSurface(),
                mEglContext)) {
            throw new IllegalStateException("eglMakeCurrent failed");
        }

        mCurrentSurface = eglSurface;
    }

    private void checkInitialized() {
        checkState(isInitialized(), "GlContext is not initialized");
    }

    private boolean isInitialized() {
        return !Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)
                && !Objects.equals(mEglContext, EGL14.EGL_NO_CONTEXT)
                && mEglConfig != null;
    }

    private EglSurface create1x1PBufferSurface() {
        int width = 1;
        int height = 1;
        int[] surfaceAttrib = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig,
                surfaceAttrib, 0
        );
        checkEglErrorOrThrow("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return EglSurface.of(eglSurface, null, width, height);
    }
}
