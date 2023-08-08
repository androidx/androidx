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

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

import static androidx.camera.core.ImageProcessingUtil.copyByteBufferToBitmap;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceOutput;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenGLRenderer renders texture image to the output surface.
 *
 * <p>OpenGLRenderer's methods must run on the same thread, so called GL thread. The GL thread is
 * locked as the thread running the {@link #init(DynamicRange, ShaderProvider)} method, otherwise an
 * {@link IllegalStateException} will be thrown when other methods are called.
 */
@WorkerThread
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class OpenGlRenderer {
    private static final String TAG = "OpenGlRenderer";

    private static final String VAR_TEXTURE_COORD = "vTextureCoord";
    private static final String VAR_TEXTURE = "sTexture";
    private static final int PIXEL_STRIDE = 4;

    private static final String DEFAULT_VERTEX_SHADER = String.format(Locale.US,
            "uniform mat4 uTexMatrix;\n"
                    + "attribute vec4 aPosition;\n"
                    + "attribute vec4 aTextureCoord;\n"
                    + "varying vec2 %s;\n"
                    + "void main() {\n"
                    + "    gl_Position = aPosition;\n"
                    + "    %s = (uTexMatrix * aTextureCoord).xy;\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE_COORD);

    private static final String HDR_VERTEX_SHADER = String.format(Locale.US,
            "#version 300 es\n"
                    + "in vec4 aPosition;\n"
                    + "in vec4 aTextureCoord;\n"
                    + "uniform mat4 uTexMatrix;\n"
                    + "out vec2 %s;\n"
                    + "void main() {\n"
                    + "  gl_Position = aPosition;\n"
                    + "  %s = (uTexMatrix * aTextureCoord).xy;\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE_COORD);

    private static final String DEFAULT_FRAGMENT_SHADER = String.format(Locale.US,
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 %s;\n"
                    + "uniform samplerExternalOES %s;\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(%s, %s);\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE, VAR_TEXTURE, VAR_TEXTURE_COORD);

    private static final String HDR_FRAGMENT_SHADER = String.format(Locale.US,
            "#version 300 es\n"
                    + "#extension GL_OES_EGL_image_external : require\n"
                    + "#extension GL_EXT_YUV_target : require\n"
                    + "precision mediump float;\n"
                    + "uniform __samplerExternal2DY2YEXT %s;\n"
                    + "in vec2 %s;\n"
                    + "out vec4 outColor;\n"
                    + "\n"
                    + "vec3 yuvToRgb(vec3 yuv) {\n"
                    + "  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);\n"
                    + "  const mat3 yuvToRgbColorTransform = mat3(\n"
                    + "    1.1689f, 1.1689f, 1.1689f,\n"
                    + "    0.0000f, -0.1881f, 2.1502f,\n"
                    + "    1.6853f, -0.6530f, 0.0000f\n"
                    + "  );\n"
                    + "  return clamp(yuvToRgbColorTransform * (yuv - yuvOffset), 0.0, 1.0);\n"
                    + "}\n"
                    + "\n"
                    + "void main() {\n"
                    + "  vec3 srcYuv = texture(%s, %s).xyz;\n"
                    + "  outColor = vec4(yuvToRgb(srcYuv), 1.0);\n"
                    + "}", VAR_TEXTURE, VAR_TEXTURE_COORD, VAR_TEXTURE, VAR_TEXTURE_COORD
    );

    private static final float[] VERTEX_COORDS = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,    // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,    // 3 top right
    };
    private static final FloatBuffer VERTEX_BUF = createFloatBuffer(VERTEX_COORDS);

    private static final float[] TEX_COORDS = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer TEX_BUF = createFloatBuffer(TEX_COORDS);

    private static final int SIZEOF_FLOAT = 4;
    private static final OutputSurface NO_OUTPUT_SURFACE =
            OutputSurface.of(EGL14.EGL_NO_SURFACE, 0, 0);

    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    @VisibleForTesting
    final Map<Surface, OutputSurface> mOutputSurfaceMap = new HashMap<>();
    @Nullable
    private Thread mGlThread;
    @NonNull
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    @NonNull
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    @Nullable
    private EGLConfig mEglConfig;
    @NonNull
    private EGLSurface mTempSurface = EGL14.EGL_NO_SURFACE;
    @Nullable
    private Surface mCurrentSurface;
    private int mExternalTextureId = -1;
    private int mProgramHandle = -1;
    private int mTexMatrixLoc = -1;
    private int mPositionLoc = -1;
    private int mTexCoordLoc = -1;

    /**
     * Initializes the OpenGLRenderer
     *
     * <p>Initialization must be done before calling other methods, otherwise an
     * {@link IllegalStateException} will be thrown. Following methods must run on the same
     * thread as this method, so called GL thread, otherwise an {@link IllegalStateException}
     * will be thrown.
     *
     * @throws IllegalStateException    if the renderer is already initialized or failed to be
     *                                  initialized.
     * @throws IllegalArgumentException if the ShaderProvider fails to create shader or provides
     *                                  invalid shader string.
     */
    public void init(@NonNull DynamicRange dynamicRange, @NonNull ShaderProvider shaderProvider) {
        checkInitializedOrThrow(false);
        try {
            if (dynamicRange.is10BitHdr()) {
                String glExtensions = getGlExtensionsBeforeInitialized(dynamicRange);
                if (!glExtensions.contains("GL_EXT_YUV_target")) {
                    Log.w(TAG, "Device does not support GL_EXT_YUV_target. Fallback to SDR.");
                    dynamicRange = DynamicRange.SDR;
                }
            }
            createEglContext(dynamicRange);
            createTempSurface();
            makeCurrent(mTempSurface);
            createProgram(dynamicRange, shaderProvider);
            loadLocations();
            createTexture();
            useAndConfigureProgram();
        } catch (IllegalStateException | IllegalArgumentException e) {
            releaseInternal();
            throw e;
        }
        mGlThread = Thread.currentThread();
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
        checkGlThreadOrThrow();
        releaseInternal();
    }

    /**
     * Register the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     *                               on the GL thread.
     */
    public void registerOutputSurface(@NonNull Surface surface) {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        if (!mOutputSurfaceMap.containsKey(surface)) {
            mOutputSurfaceMap.put(surface, NO_OUTPUT_SURFACE);
        }
    }

    /**
     * Unregister the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     *                               on the GL thread.
     */
    public void unregisterOutputSurface(@NonNull Surface surface) {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        removeOutputSurfaceInternal(surface, true);
    }

    /**
     * Gets the texture name.
     *
     * @return the texture name
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     *                               on the GL thread.
     */
    public int getTextureName() {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        return mExternalTextureId;
    }

    /**
     * Renders the texture image to the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized, the caller doesn't run
     *                               on the GL thread or the surface is not registered by
     *                               {@link #registerOutputSurface(Surface)}.
     */
    public void render(long timestampNs, @NonNull float[] textureTransform,
            @NonNull Surface surface) {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        OutputSurface outputSurface = getOutSurfaceOrThrow(surface);

        // Workaround situations that out surface is failed to create or needs to be recreated.
        if (outputSurface == NO_OUTPUT_SURFACE) {
            outputSurface = createOutputSurfaceInternal(surface);
            if (outputSurface == null) {
                return;
            }

            mOutputSurfaceMap.put(surface, outputSurface);
        }

        // Set output surface.
        if (surface != mCurrentSurface) {
            makeCurrent(outputSurface.getEglSurface());
            mCurrentSurface = surface;
            GLES20.glViewport(0, 0, outputSurface.getWidth(), outputSurface.getHeight());
            GLES20.glScissor(0, 0, outputSurface.getWidth(), outputSurface.getHeight());
        }

        // TODO(b/245855601): Upload the matrix to GPU when textureTransform is changed.
        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(mTexMatrixLoc, /*count=*/1, /*transpose=*/false, textureTransform,
                /*offset=*/0);
        checkGlErrorOrThrow("glUniformMatrix4fv");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /*firstVertex=*/0, /*vertexCount=*/4);
        checkGlErrorOrThrow("glDrawArrays");

        // Set timestamp
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, outputSurface.getEglSurface(), timestampNs);

        // Swap buffer
        if (!EGL14.eglSwapBuffers(mEglDisplay, outputSurface.getEglSurface())) {
            Logger.w(TAG, "Failed to swap buffers with EGL error: 0x" + Integer.toHexString(
                    EGL14.eglGetError()));
            removeOutputSurfaceInternal(surface, false);
        }
    }

    /**
     * Takes a snapshot of the current external texture and returns a Bitmap.
     *
     * @param size             the size of the output {@link Bitmap}.
     * @param textureTransform the transformation matrix.
     *                         See: {@link SurfaceOutput#updateTransformMatrix(float[], float[])}
     */
    @NonNull
    public Bitmap snapshot(@NonNull Size size, @NonNull float[] textureTransform) {
        // Allocate buffer.
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                size.getWidth() * size.getHeight() * PIXEL_STRIDE);
        // Take a snapshot.
        snapshot(byteBuffer, size, textureTransform);
        // Create a Bitmap and copy the bytes over.
        Bitmap bitmap = Bitmap.createBitmap(
                size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
        byteBuffer.rewind();
        copyByteBufferToBitmap(bitmap, byteBuffer, size.getWidth() * PIXEL_STRIDE);
        return bitmap;
    }

    /**
     * Takes a snapshot of the current external texture and stores it in the given byte buffer.
     *
     * <p> The image is stored as RGBA with pixel stride of 4 bytes and row stride of width * 4
     * bytes.
     *
     * @param byteBuffer       the byte buffer to store the snapshot.
     * @param size             the size of the output image.
     * @param textureTransform the transformation matrix.
     *                         See: {@link SurfaceOutput#updateTransformMatrix(float[], float[])}
     */
    private void snapshot(@NonNull ByteBuffer byteBuffer, @NonNull Size size,
            @NonNull float[] textureTransform) {
        checkArgument(byteBuffer.capacity() == size.getWidth() * size.getHeight() * 4,
                "ByteBuffer capacity is not equal to width * height * 4.");
        checkArgument(byteBuffer.isDirect(), "ByteBuffer is not direct.");

        // Create and initialize intermediate texture.
        int texture = generateTexture();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        checkGlErrorOrThrow("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        checkGlErrorOrThrow("glBindTexture");
        // Configure the texture.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, size.getWidth(),
                size.getHeight(), 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
        checkGlErrorOrThrow("glTexImage2D");
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        // Create FBO.
        int fbo = generateFbo();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        checkGlErrorOrThrow("glBindFramebuffer");

        // Attach the intermediate texture to the FBO
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texture, 0);
        checkGlErrorOrThrow("glFramebufferTexture2D");

        // Bind external texture (camera output).
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlErrorOrThrow("glActiveTexture");
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);
        checkGlErrorOrThrow("glBindTexture");

        // Set scissor and viewport.
        mCurrentSurface = null;
        GLES20.glViewport(0, 0, size.getWidth(), size.getHeight());
        GLES20.glScissor(0, 0, size.getWidth(), size.getHeight());

        // Upload transform matrix.
        GLES20.glUniformMatrix4fv(mTexMatrixLoc, /*count=*/1, /*transpose=*/false, textureTransform,
                /*offset=*/0);
        checkGlErrorOrThrow("glUniformMatrix4fv");

        // Draw the external texture to the intermediate texture.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /*firstVertex=*/0, /*vertexCount=*/4);
        checkGlErrorOrThrow("glDrawArrays");

        // Read the pixels from the framebuffer
        GLES20.glReadPixels(0, 0, size.getWidth(), size.getHeight(), GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                byteBuffer);
        checkGlErrorOrThrow("glReadPixels");

        // Clean up
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        deleteTexture(texture);
        deleteFbo(fbo);
        // Set the external texture to be active.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);
    }

    @NonNull
    private String getGlExtensionsBeforeInitialized(
            @NonNull DynamicRange dynamicRangeToInitialize) {
        checkInitializedOrThrow(false);
        try {
            createEglContext(dynamicRangeToInitialize);
            createTempSurface();
            makeCurrent(mTempSurface);
            // eglMakeCurrent() has to be called before checking GL_EXTENSIONS.
            String glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
            return glExtensions != null ? glExtensions : "";
        } catch (IllegalStateException e) {
            Logger.w(TAG, "Failed to get GL extensions: " + e.getMessage(), e);
            return "";
        } finally {
            releaseInternal();
        }
    }

    private static int generateFbo() {
        int[] fbos = new int[1];
        GLES20.glGenFramebuffers(1, fbos, 0);
        checkGlErrorOrThrow("glGenFramebuffers");
        return fbos[0];
    }

    private static int generateTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlErrorOrThrow("glGenTextures");
        return textures[0];
    }

    private static void deleteTexture(int texture) {
        int[] textures = {texture};
        GLES20.glDeleteTextures(1, textures, 0);
        checkGlErrorOrThrow("glDeleteTextures");
    }

    private static void deleteFbo(int fbo) {
        int[] fbos = {fbo};
        GLES20.glDeleteFramebuffers(1, fbos, 0);
        checkGlErrorOrThrow("glDeleteFramebuffers");
    }

    private void createEglContext(@NonNull DynamicRange dynamicRange) {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new IllegalStateException("Unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
            throw new IllegalStateException("Unable to initialize EGL14");
        }
        int rgbBits = dynamicRange.is10BitHdr() ? 10 : 8;
        int alphaBits = dynamicRange.is10BitHdr() ? 2 : 8;
        int renderType = dynamicRange.is10BitHdr() ? EGLExt.EGL_OPENGL_ES3_BIT_KHR
                : EGL14.EGL_OPENGL_ES2_BIT;
        // recordableAndroid with EGL14.EGL_TRUE causes eglError for 10BitHdr.
        int recordableAndroid = dynamicRange.is10BitHdr() ? EGL14.EGL_FALSE : EGL14.EGL_TRUE;
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
        if (!EGL14.eglChooseConfig(mEglDisplay, attribToChooseConfig, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new IllegalStateException("Unable to find a suitable EGLConfig");
        }
        EGLConfig config = configs[0];
        int[] attribToCreateContext = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, dynamicRange.is10BitHdr() ? 3 : 2,
                EGL14.EGL_NONE
        };
        EGLContext context = EGL14.eglCreateContext(mEglDisplay, config, EGL14.EGL_NO_CONTEXT,
                attribToCreateContext, 0);
        checkEglErrorOrThrow("eglCreateContext");
        mEglConfig = config;
        mEglContext = context;

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values,
                0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    private void createTempSurface() {
        mTempSurface = createPBufferSurface(mEglDisplay, requireNonNull(mEglConfig), /*width=*/1,
                /*height=*/1);
    }

    private void makeCurrent(@NonNull EGLSurface eglSurface) {
        Preconditions.checkNotNull(mEglDisplay);
        Preconditions.checkNotNull(mEglContext);
        if (!EGL14.eglMakeCurrent(mEglDisplay, eglSurface, eglSurface, mEglContext)) {
            throw new IllegalStateException("eglMakeCurrent failed");
        }
    }

    private void createProgram(@NonNull DynamicRange dynamicRange,
            @NonNull ShaderProvider shaderProvider) {
        int vertexShader = -1;
        int fragmentShader = -1;
        int program = -1;
        try {
            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER,
                    dynamicRange.is10BitHdr() ? HDR_VERTEX_SHADER : DEFAULT_VERTEX_SHADER);
            fragmentShader = loadFragmentShader(dynamicRange, shaderProvider);
            program = GLES20.glCreateProgram();
            checkGlErrorOrThrow("glCreateProgram");
            GLES20.glAttachShader(program, vertexShader);
            checkGlErrorOrThrow("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGlErrorOrThrow("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, /*offset=*/0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                throw new IllegalStateException(
                        "Could not link program: " + GLES20.glGetProgramInfoLog(program));
            }
            mProgramHandle = program;
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (vertexShader != -1) {
                GLES20.glDeleteShader(vertexShader);
            }
            if (fragmentShader != -1) {
                GLES20.glDeleteShader(fragmentShader);
            }
            if (program != -1) {
                GLES20.glDeleteProgram(program);
            }
            throw e;
        }
    }

    private void useAndConfigureProgram() {
        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        checkGlErrorOrThrow("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(mPositionLoc);
        checkGlErrorOrThrow("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        int coordsPerVertex = 2;
        int vertexStride = 0;
        GLES20.glVertexAttribPointer(mPositionLoc, coordsPerVertex, GLES20.GL_FLOAT,
                /*normalized=*/false, vertexStride, VERTEX_BUF);
        checkGlErrorOrThrow("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        checkGlErrorOrThrow("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        int coordsPerTex = 2;
        int texStride = 0;
        GLES20.glVertexAttribPointer(mTexCoordLoc, coordsPerTex, GLES20.GL_FLOAT,
                /*normalized=*/false, texStride, TEX_BUF);
        checkGlErrorOrThrow("glVertexAttribPointer");
    }

    private void loadLocations() {
        mPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        checkLocationOrThrow(mPositionLoc, "aPosition");
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        checkLocationOrThrow(mTexCoordLoc, "aTextureCoord");
        mTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        checkLocationOrThrow(mTexMatrixLoc, "uTexMatrix");
    }

    private void createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlErrorOrThrow("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId);
        checkGlErrorOrThrow("glBindTexture " + texId);

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlErrorOrThrow("glTexParameter");

        mExternalTextureId = texId;
    }

    private int loadFragmentShader(@NonNull DynamicRange dynamicRange,
            @NonNull ShaderProvider shaderProvider) {
        if (shaderProvider == ShaderProvider.DEFAULT) {
            return loadShader(GLES20.GL_FRAGMENT_SHADER,
                    dynamicRange.is10BitHdr() ? HDR_FRAGMENT_SHADER : DEFAULT_FRAGMENT_SHADER);
        } else {
            // Throw IllegalArgumentException if the shader provider can not provide a valid
            // fragment shader.
            String source;
            try {
                source = shaderProvider.createFragmentShader(VAR_TEXTURE, VAR_TEXTURE_COORD);
                // A simple check to workaround custom shader doesn't contain required variable.
                // See b/241193761.
                if (source == null || !source.contains(VAR_TEXTURE_COORD) || !source.contains(
                        VAR_TEXTURE)) {
                    throw new IllegalArgumentException("Invalid fragment shader");
                }
                return loadShader(GLES20.GL_FRAGMENT_SHADER, source);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException) {
                    throw t;
                }
                throw new IllegalArgumentException("Unable to compile fragment shader", t);
            }
        }
    }

    @NonNull
    private Size getSurfaceSize(@NonNull EGLSurface eglSurface) {
        int width = querySurface(mEglDisplay, eglSurface, EGL14.EGL_WIDTH);
        int height = querySurface(mEglDisplay, eglSurface, EGL14.EGL_HEIGHT);
        return new Size(width, height);
    }

    private void releaseInternal() {
        // Delete program
        if (mProgramHandle != -1) {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = -1;
        }

        if (!Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);

            // Destroy EGLSurfaces
            for (OutputSurface outputSurface : mOutputSurfaceMap.values()) {
                if (!Objects.equals(outputSurface.getEglSurface(), EGL14.EGL_NO_SURFACE)) {
                    if (!EGL14.eglDestroySurface(mEglDisplay, outputSurface.getEglSurface())) {
                        checkEglErrorOrLog("eglDestroySurface");
                    }
                }
            }
            mOutputSurfaceMap.clear();

            // Destroy temp surface
            if (!Objects.equals(mTempSurface, EGL14.EGL_NO_SURFACE)) {
                EGL14.eglDestroySurface(mEglDisplay, mTempSurface);
                mTempSurface = EGL14.EGL_NO_SURFACE;
            }

            // Destroy EGLContext and terminate display
            if (!Objects.equals(mEglContext, EGL14.EGL_NO_CONTEXT)) {
                EGL14.eglDestroyContext(mEglDisplay, mEglContext);
                mEglContext = EGL14.EGL_NO_CONTEXT;
            }
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEglDisplay);
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
        }

        // Reset other members
        mEglConfig = null;
        mProgramHandle = -1;
        mTexMatrixLoc = -1;
        mPositionLoc = -1;
        mTexCoordLoc = -1;
        mExternalTextureId = -1;
        mCurrentSurface = null;
        mGlThread = null;
    }

    private void checkInitializedOrThrow(boolean shouldInitialized) {
        boolean result = shouldInitialized == mInitialized.get();
        String message = shouldInitialized ? "OpenGlRenderer is not initialized"
                : "OpenGlRenderer is already initialized";
        Preconditions.checkState(result, message);
    }

    private void checkGlThreadOrThrow() {
        Preconditions.checkState(mGlThread == Thread.currentThread(),
                "Method call must be called on the GL thread.");
    }

    @NonNull
    private OutputSurface getOutSurfaceOrThrow(@NonNull Surface surface) {
        Preconditions.checkState(mOutputSurfaceMap.containsKey(surface),
                "The surface is not registered.");

        return requireNonNull(mOutputSurfaceMap.get(surface));
    }

    @SuppressWarnings("SameParameterValue") // currently hard code width/height with 1/1
    @NonNull
    private static EGLSurface createPBufferSurface(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLConfig eglConfig, int width, int height) {
        int[] surfaceAttrib = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttrib,
                /*offset=*/0);
        checkEglErrorOrThrow("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return eglSurface;
    }

    @Nullable
    private OutputSurface createOutputSurfaceInternal(@NonNull Surface surface) {
        EGLSurface eglSurface;
        try {
            eglSurface = createWindowSurface(mEglDisplay, requireNonNull(mEglConfig), surface);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Logger.w(TAG, "Failed to create EGL surface: " + e.getMessage(), e);
            return null;
        }

        Size size = getSurfaceSize(eglSurface);
        return OutputSurface.of(eglSurface, size.getWidth(), size.getHeight());
    }

    private void removeOutputSurfaceInternal(@NonNull Surface surface, boolean unregister) {
        // Unmake current surface.
        if (mCurrentSurface == surface) {
            mCurrentSurface = null;
            makeCurrent(mTempSurface);
        }

        // Remove cached EGL surface.
        OutputSurface removedOutputSurface;
        if (unregister) {
            removedOutputSurface = mOutputSurfaceMap.remove(surface);
        } else {
            removedOutputSurface = mOutputSurfaceMap.put(surface, NO_OUTPUT_SURFACE);
        }

        // Destroy EGL surface.
        if (removedOutputSurface != null && removedOutputSurface != NO_OUTPUT_SURFACE) {
            try {
                EGL14.eglDestroySurface(mEglDisplay, removedOutputSurface.getEglSurface());
            } catch (RuntimeException e) {
                Logger.w(TAG, "Failed to destroy EGL surface: " + e.getMessage(), e);
            }
        }
    }

    @NonNull
    private static EGLSurface createWindowSurface(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLConfig eglConfig, @NonNull Surface surface) {
        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttrib = {
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface,
                surfaceAttrib, /*offset=*/0);
        checkEglErrorOrThrow("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return eglSurface;
    }

    private static int loadShader(int shaderType, @NonNull String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlErrorOrThrow("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, /*offset=*/0);
        if (compiled[0] == 0) {
            Logger.w(TAG, "Could not compile shader: " + source);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException(
                    "Could not compile shader type " + shaderType + ":" + GLES20.glGetShaderInfoLog(
                            shader));
        }
        return shader;
    }

    private static int querySurface(@NonNull EGLDisplay eglDisplay, @NonNull EGLSurface eglSurface,
            int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, /*offset=*/0);
        return value[0];
    }

    @NonNull
    public static FloatBuffer createFloatBuffer(@NonNull float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    private static void checkLocationOrThrow(int location, @NonNull String label) {
        if (location < 0) {
            throw new IllegalStateException("Unable to locate '" + label + "' in program");
        }
    }

    private static void checkEglErrorOrThrow(@NonNull String op) {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            throw new IllegalStateException(op + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private static void checkEglErrorOrLog(@NonNull String op) {
        try {
            checkEglErrorOrThrow(op);
        } catch (IllegalStateException e) {
            Logger.e(TAG, e.toString(), e);
        }
    }

    private static void checkGlErrorOrThrow(@NonNull String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new IllegalStateException(op + ": GL error 0x" + Integer.toHexString(error));
        }
    }

    @AutoValue
    abstract static class OutputSurface {

        @NonNull
        static OutputSurface of(@NonNull EGLSurface eglSurface, int width, int height) {
            return new AutoValue_OpenGlRenderer_OutputSurface(eglSurface, width, height);
        }

        @NonNull
        abstract EGLSurface getEglSurface();

        abstract int getWidth();

        abstract int getHeight();
    }
}
