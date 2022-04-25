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

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_DEFAULT_DISPLAY;
import static android.opengl.EGL14.EGL_HEIGHT;
import static android.opengl.EGL14.EGL_NONE;
import static android.opengl.EGL14.EGL_NO_CONTEXT;
import static android.opengl.EGL14.EGL_NO_DISPLAY;
import static android.opengl.EGL14.EGL_NO_SURFACE;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static android.opengl.EGL14.EGL_PBUFFER_BIT;
import static android.opengl.EGL14.EGL_RENDERABLE_TYPE;
import static android.opengl.EGL14.EGL_SURFACE_TYPE;
import static android.opengl.EGL14.EGL_WIDTH;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * Utility functions interacting with OpenGL.
 *
 * <p> These utility methods are meant only for testing purposes.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class GLUtil {
    private GLUtil() {
    }

    /** Set up a GL context so that GL calls requiring a context can be made. */
    private static void setupGLContext() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (Objects.equals(eglDisplay, EGL_NO_DISPLAY)) {
            throw new RuntimeException("Unable to get EGL display.");
        }
        int[] majorVer = new int[1];
        int majorOffset = 0;
        int[] minorVer = new int[1];
        int minorOffset = 0;
        if (!EGL14.eglInitialize(eglDisplay, majorVer, majorOffset, minorVer, minorOffset)) {
            throw new RuntimeException("Unable to initialize EGL.");
        }

        int[] configAttribs =
                new int[]{
                        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                        EGL_NONE
                };
        int configAttribsOffset = 0;
        EGLConfig[] configs = new EGLConfig[1];
        int configsOffset = 0;
        int[] numConfigs = new int[1];
        int numConfigsOffset = 0;
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                configAttribs,
                configAttribsOffset,
                configs,
                configsOffset,
                configs.length,
                numConfigs,
                numConfigsOffset)) {
            throw new RuntimeException("No appropriate EGL config exists on device.");
        }
        EGLConfig eglConfig = configs[0];

        // Use a 1x1 pbuffer as our surface
        int[] pbufferAttribs =
                new int[]{
                        EGL_WIDTH, 1,
                        EGL_HEIGHT, 1,
                        EGL_NONE
                };
        int pbufferAttribsOffset = 0;
        EGLSurface eglPbuffer =
                EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttribs,
                        pbufferAttribsOffset);
        if (Objects.equals(eglPbuffer, EGL_NO_SURFACE)) {
            throw new RuntimeException("Unable to create pbuffer surface.");
        }

        int[] contextAttribs = new int[]{EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
        int contextAttribsOffset = 0;
        EGLContext eglContext =
                EGL14.eglCreateContext(
                        eglDisplay, eglConfig, EGL_NO_CONTEXT, contextAttribs,
                        contextAttribsOffset);
        if (Objects.equals(eglContext, EGL_NO_CONTEXT)) {
            throw new RuntimeException("Unable to create EGL context.");
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglPbuffer, eglPbuffer, eglContext)) {
            throw new RuntimeException("Failed to make EGL context current.");
        }
    }

    /** Get a texture id for GL. */
    public static int getTexIdFromGLContext() {
        setupGLContext();
        int[] texIds = new int[1];
        GLES20.glGenTextures(1, texIds, 0);
        return texIds[0];
    }
}
