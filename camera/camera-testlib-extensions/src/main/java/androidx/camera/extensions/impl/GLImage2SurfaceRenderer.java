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

package androidx.camera.extensions.impl;

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
import static android.opengl.EGL14.EGL_TRUE;
import static android.opengl.EGL14.EGL_WIDTH;
import static android.opengl.EGL14.EGL_WINDOW_BIT;
import static android.opengl.EGLExt.EGL_RECORDABLE_ANDROID;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_LUMINANCE;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;

import android.media.Image;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Objects;

/**
 * A renderer that takes an {@link Image} and renders it to a {@link Surface} that is backed by a
 * {@link android.graphics.SurfaceTexture}.
 *
 * <p> The renderer only takes the Y channel of the input YUV image and copies the same values to
 * the RGB channels. This is meant only as a demonstration that the preview processing pipeline can
 * take as input a {@link Image} and write to a {@link android.graphics.SurfaceTexture}. It has only
 * been tested on a Pixel 2XL.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class GLImage2SurfaceRenderer {
    private static final String TAG = "GLImage2SurfaceRenderer";

    private EGLDisplay mEGLDisplay;
    private EGLConfig[] mEGLConfigs = new EGLConfig[1];
    private EGLContext mEGLContext;
    private EGLSurface mEGLPbufferSurface;
    private EGLSurface mWindowSurface;

    private int mProgram;
    private int mPositionHandle;

    private int mTextureYHandle;

    private FloatBuffer mVerticesFloatBuffer;
    private ByteBuffer mVerticesByteBuffer;

    private Size mInputSize;

    private static final String sVertexShaderSrc =
            "attribute vec4 position;"
                    + "varying vec2 texCoord;"
                    + "void main() {"
                    + "    vec2 texCoordUnrotated = (position.xy + vec2(1.0, 1.0)) * 0.5;"
                    + "    texCoord = vec2(1.0 - texCoordUnrotated.y, 1.0 -texCoordUnrotated.x);"
                    + "    gl_Position = position;"
                    + "}";

    private static final String sFragmentShaderSrc =
            "precision mediump float;"
                    + "varying vec2 texCoord;"
                    + "uniform sampler2D texY;"
                    + "void main() {"
                    + "  float y = texture2D(texY, texCoord).r;"
                    + "  gl_FragColor = vec4(y,y,y, 1.0);"
                    + "}";

    GLImage2SurfaceRenderer() {
        // Initialize
        mEGLDisplay = EGL14.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (Objects.equals(mEGLDisplay, EGL_NO_DISPLAY)) {
            throw new RuntimeException("Unable to get GL display");
        }

        int[] version = new int[2];

        boolean initSuccess = EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1);

        if (!initSuccess) {
            throw new RuntimeException("Unable to initialize EGL");
        }

        int[] configAttribs = {EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL_SURFACE_TYPE,
                EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
                EGL_RECORDABLE_ANDROID,
                EGL_TRUE,
                EGL_NONE};

        int[] numConfigs = new int[1];

        boolean eglChooseConfigSuccess = EGL14.eglChooseConfig(
                mEGLDisplay,
                configAttribs,
                0,
                mEGLConfigs,
                0,
                mEGLConfigs.length,
                numConfigs,
                0);

        if (!eglChooseConfigSuccess) {
            throw new RuntimeException("Unable to successfully config egl");
        }

        if (numConfigs[0] <= 0) {
            throw new RuntimeException("Number of configs not greater than 0");
        }

        int[] contextAttribs = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
        mEGLContext = EGL14.eglCreateContext(
                mEGLDisplay, mEGLConfigs[0], EGL_NO_CONTEXT, contextAttribs, 0);

        if (Objects.equals(mEGLContext, EGL_NO_CONTEXT)) {
            throw new RuntimeException("EGL has no context");
        }

        // Create 1x1 pixmap to use as a surface until one is set.
        int[] pbufferAttribs = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};

        mEGLPbufferSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfigs[0],
                pbufferAttribs, 0);

        if (Objects.equals(mEGLPbufferSurface, EGL_NO_SURFACE)) {
            throw new RuntimeException("No EGL surface");
        }

        EGL14.eglMakeCurrent(mEGLDisplay, mEGLPbufferSurface, mEGLPbufferSurface, mEGLContext);

        mProgram = createGlProgram();

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");

        if (mPositionHandle == -1) {
            throw new RuntimeException("Unable to position handle");
        }

        mTextureYHandle = GLES20.glGetUniformLocation(mProgram, "texY");

        if (mTextureYHandle == -1) {
            throw new RuntimeException("Unable to get texture y handle");
        }

        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);

        GLES20.glUniform1i(mTextureYHandle, 0);

        initVertexBuffer();
    }

    void setInput(Size size) {
        mInputSize = size;
    }

    void setWindowSurface(Surface surface, int width, int height) {
        // Destroy previously connected surface
        destroySurface();

        // Null surface may have just been passed in to destroy previous surface.
        if (surface == null) {
            return;
        }

        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        mWindowSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfigs[0], surface,
                surfaceAttribs,
                0);

        if (Objects.equals(mWindowSurface, EGL_NO_SURFACE)) {
            throw new RuntimeException("Unable to create window surface");
        }

        EGL14.eglMakeCurrent(mEGLDisplay, mWindowSurface, mWindowSurface, mEGLContext);

        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);
    }

    void initVertexBuffer() {
        float[] vertices = {-1.0f, -1.0f, 3.0f, -1.0f, -1.0f, 3.0f};
        mVerticesByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(
                ByteOrder.nativeOrder());
        mVerticesFloatBuffer = mVerticesByteBuffer.asFloatBuffer();
        mVerticesFloatBuffer.put(vertices).position(0);

        int vertexComponents = 2;
        int vertexType = GL_FLOAT;
        boolean normalized = false;
        int vertexStride = 0;

        GLES20.glVertexAttribPointer(mPositionHandle, vertexComponents, vertexType, normalized,
                vertexStride, mVerticesFloatBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
    }

    void renderTexture(Image image) {
        GLES20.glUseProgram(mProgram);
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        // Bind Y texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, mInputSize.getWidth(),
                mInputSize.getHeight(), 0, GL_LUMINANCE, GL_UNSIGNED_BYTE,
                image.getPlanes()[0].getBuffer());
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glDrawArrays(GL_TRIANGLES, 0, 3);

        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mWindowSurface, image.getTimestamp());

        EGL14.eglSwapBuffers(mEGLDisplay, mWindowSurface);
    }

    void close() {
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }

        destroySurface();
        EGL14.eglDestroySurface(mEGLDisplay, mEGLPbufferSurface);
        EGL14.eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
    }

    private String getShaderTypeString(int shaderType) {
        switch (shaderType) {
            case GL_VERTEX_SHADER:
                return "GL_VERTEX_SHADER";
            case GL_FRAGMENT_SHADER:
                return "GL_FRAGMENT_SHADER";
            default:
                return "<Unknown shader type>";
        }
    }

    // Returns a handle to the shader
    private int compileShader(int shaderType, String shaderSource) {
        int shader = GLES20.glCreateShader(shaderType);

        if (shader == 0) {
            throw new RuntimeException("Unable to create shader");
        }

        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            String logBuffer = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG,
                    String.format("Unable to compile %s shader: %s",
                            getShaderTypeString(shaderType),
                            logBuffer));

            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        return shader;
    }

    // Returns a handle to the output program
    private int createGlProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, sVertexShaderSrc);
        if (vertexShader == 0) {
            throw new RuntimeException("Unable to compile vertex shader");
        }

        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, sFragmentShaderSrc);
        if (fragmentShader == 0) {
            throw new RuntimeException("Unable to compile fragment shader");
        }
        int program = GLES20.glCreateProgram();
        if (program == 0) {
            throw new RuntimeException("Unable to create GL program");
        }
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String logBuffer = GLES20.glGetProgramInfoLog(program);
            Log.e(TAG, String.format("Unable to link program: %s", logBuffer));

            GLES20.glDeleteProgram(program);
            program = 0;
        }
        if (program == 0) {
            throw new RuntimeException("Unable to create GL program");
        }
        return program;
    }

    private void destroySurface() {
        if (mWindowSurface == null) {
            return;
        }
        EGL14.eglMakeCurrent(mEGLDisplay, mEGLPbufferSurface, mEGLPbufferSurface, mEGLContext);
        EGL14.eglDestroySurface(mEGLDisplay, mWindowSurface);
        mWindowSurface = null;
    }
}
