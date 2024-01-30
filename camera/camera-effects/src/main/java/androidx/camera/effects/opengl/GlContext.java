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

import static androidx.camera.effects.opengl.Utils.checkEglErrorOrLog;
import static androidx.camera.effects.opengl.Utils.checkEglErrorOrThrow;
import static androidx.camera.effects.opengl.Utils.drawArrays;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

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

import java.util.HashMap;
import java.util.Map;
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
    private EglSurface mCurrentSurface = null;
    // A temporary output Surface. This is used when no Surface has been registered yet.
    @Nullable
    private EglSurface mTempSurface = null;
    @NonNull
    private final Map<Surface, EglSurface> mRegisteredSurfaces = new HashMap<>();

    void init() {
        checkState(Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY), "Already initialized");

        // Create EGLDisplay.
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new IllegalStateException("Unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
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
                mEglDisplay, attribToChooseConfig, 0, configs, 0, configs.length, numConfigs, 0
        )) {
            throw new IllegalStateException("Unable to find a suitable EGLConfig");
        }
        mEglConfig = configs[0];
        int[] attribToCreateContext = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        // Create EGLContext.
        mEglContext = EGL14.eglCreateContext(
                mEglDisplay, mEglConfig, EGL14.EGL_NO_CONTEXT,
                attribToCreateContext, 0
        );
        checkEglErrorOrThrow("eglCreateContext");
        int[] values = new int[1];
        EGL14.eglQueryContext(
                mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0
        );
        Logger.d(TAG, "EGLContext created, client version " + values[0]);

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
        checkInitialized();
        if (!mRegisteredSurfaces.containsKey(surface)) {
            mRegisteredSurfaces.put(surface, null);
        }
    }

    /**
     * Unregisters the given {@link Surface} as an output surface.
     *
     * <p>Once unregistered, calling {@link #drawAndSwap} will no longer be effective.
     */
    void unregisterSurface(@NonNull Surface surface) {
        checkInitialized();
        if (requireNonNull(mCurrentSurface).getSurface() == surface) {
            // If the current surface is being unregistered, switch to the temporary surface.
            makeCurrent(requireNonNull(mTempSurface));
        }
        // Destroy the EGLSurface.
        EglSurface removedSurface = mRegisteredSurfaces.remove(surface);
        if (removedSurface != null) {
            destroyEglSurface(removedSurface);
        }
    }

    /**
     * Draws the current bound texture to the given {@link Surface}.
     *
     * <p>No-ops if the given {@link Surface} is not registered.
     *
     * @param timestampNs The timestamp of the frame in nanoseconds.
     */
    void drawAndSwap(@NonNull Surface surface, long timestampNs) {
        checkInitialized();
        checkState(mRegisteredSurfaces.containsKey(surface), "The Surface is not registered.");

        // Get or create the EGLSurface.
        EglSurface eglSurface = mRegisteredSurfaces.get(surface);
        // Workaround for when the output Surface is failed to create or needs to be recreated.
        if (eglSurface == null) {
            eglSurface = createEglSurface(surface);
            if (eglSurface == null) {
                Logger.w(TAG, "Failed to create EGLSurface. Skip drawing.");
                return;
            }
            mRegisteredSurfaces.put(surface, eglSurface);
        }

        // Draw.
        makeCurrent(eglSurface);
        drawArrays(eglSurface.getWidth(), eglSurface.getHeight());
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, eglSurface.getEglSurface(), timestampNs);

        // Swap buffer
        if (!EGL14.eglSwapBuffers(mEglDisplay, eglSurface.getEglSurface())) {
            // If swap buffer failed, destroy the invalid EGL Surface.
            Logger.w(TAG, "Failed to swap buffers with EGL error: 0x" + Integer.toHexString(
                    EGL14.eglGetError()));
            unregisterSurface(surface);
            // Add the surface back since it's still registered.
            mRegisteredSurfaces.put(surface, null);
        }
    }

    void release() {
        if (!Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            EGL14.eglMakeCurrent(
                    mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
            );
        }

        // Destroy EGLSurfaces
        for (EglSurface eglSurface : mRegisteredSurfaces.values()) {
            if (eglSurface != null) {
                destroyEglSurface(eglSurface);
            }
        }
        mRegisteredSurfaces.clear();

        // Destroy the temporary surface.
        if (mTempSurface != null) {
            destroyEglSurface(mTempSurface);
            mTempSurface = null;
        }
        mCurrentSurface = null;

        // Destroy EGLContext and terminate display.
        if (!Objects.equals(mEglContext, EGL14.EGL_NO_CONTEXT)) {
            EGL14.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = EGL14.EGL_NO_CONTEXT;
        }
        if (!Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            EGL14.eglTerminate(mEglDisplay);
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
        }

        EGL14.eglReleaseThread();
        mEglConfig = null;
    }

    // --- Private methods ---

    private void destroyEglSurface(@NonNull EglSurface eglSurface) {
        if (!EGL14.eglDestroySurface(mEglDisplay, eglSurface.getEglSurface())) {
            checkEglErrorOrLog("eglDestroySurface");
        }
    }

    @Nullable
    private EglSurface createEglSurface(@NonNull Surface surface) {
        EGLSurface eglSurface;
        try {
            int[] surfaceAttrib = {
                    EGL14.EGL_NONE
            };
            eglSurface = EGL14.eglCreateWindowSurface(
                    mEglDisplay, mEglConfig, surface, surfaceAttrib, 0);
            checkEglErrorOrThrow("eglCreateWindowSurface");
        } catch (IllegalStateException | IllegalArgumentException e) {
            Logger.w(TAG, "Failed to create EGL surface: " + e.getMessage(), e);
            return null;
        }
        int width = querySurface(eglSurface, EGL14.EGL_WIDTH);
        int height = querySurface(eglSurface, EGL14.EGL_HEIGHT);
        return EglSurface.of(eglSurface, surface, width, height);
    }

    private int querySurface(@NonNull EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    private void makeCurrent(@NonNull EglSurface eglSurface) {
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
