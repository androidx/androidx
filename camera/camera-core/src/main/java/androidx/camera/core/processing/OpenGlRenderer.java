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
import static androidx.camera.core.processing.util.GLUtils.EMPTY_ATTRIBS;
import static androidx.camera.core.processing.util.GLUtils.NO_OUTPUT_SURFACE;
import static androidx.camera.core.processing.util.GLUtils.PIXEL_STRIDE;
import static androidx.camera.core.processing.util.GLUtils.TEX_BUF;
import static androidx.camera.core.processing.util.GLUtils.VAR_TEXTURE;
import static androidx.camera.core.processing.util.GLUtils.VAR_TEXTURE_YUV;
import static androidx.camera.core.processing.util.GLUtils.VERTEX_BUF;
import static androidx.camera.core.processing.util.GLUtils.checkEglErrorOrLog;
import static androidx.camera.core.processing.util.GLUtils.checkEglErrorOrThrow;
import static androidx.camera.core.processing.util.GLUtils.checkGlErrorOrThrow;
import static androidx.camera.core.processing.util.GLUtils.checkGlThreadOrThrow;
import static androidx.camera.core.processing.util.GLUtils.checkInitializedOrThrow;
import static androidx.camera.core.processing.util.GLUtils.checkLocationOrThrow;
import static androidx.camera.core.processing.util.GLUtils.chooseSurfaceAttrib;
import static androidx.camera.core.processing.util.GLUtils.createPBufferSurface;
import static androidx.camera.core.processing.util.GLUtils.createProgram;
import static androidx.camera.core.processing.util.GLUtils.createTexture;
import static androidx.camera.core.processing.util.GLUtils.createWindowSurface;
import static androidx.camera.core.processing.util.GLUtils.deleteFbo;
import static androidx.camera.core.processing.util.GLUtils.deleteTexture;
import static androidx.camera.core.processing.util.GLUtils.generateFbo;
import static androidx.camera.core.processing.util.GLUtils.generateTexture;
import static androidx.camera.core.processing.util.GLUtils.getGlVersionNumber;
import static androidx.camera.core.processing.util.GLUtils.getSurfaceSize;
import static androidx.camera.core.processing.util.GLUtils.getTexNumUnits;
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
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.processing.util.GLUtils.InputFormat;
import androidx.camera.core.processing.util.GraphicDeviceInfo;
import androidx.camera.core.processing.util.OutputSurface;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;

/**
 * OpenGLRenderer renders texture image to the output surface.
 *
 * <p>OpenGLRenderer's methods must run on the same thread, so called GL thread. The GL thread is
 * locked as the thread running the {@link #init(DynamicRange, ShaderProvider)} method, otherwise an
 * {@link IllegalStateException} will be thrown when other methods are called.
 */
@WorkerThread
public final class OpenGlRenderer {

    private static final String TAG = "OpenGlRenderer";

    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    @VisibleForTesting
    final Map<Surface, OutputSurface> mOutputSurfaceMap = new HashMap<>();
    @Nullable
    private Thread mGlThread;
    @NonNull
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    @NonNull
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    @NonNull
    private int[] mSurfaceAttrib = EMPTY_ATTRIBS;
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
    private int mSamplerDefaultLoc = -1;
    private int mSamplerYuvLoc = -1;
    private int mSamplerSelectorLoc = -1;
    private int mExternalTexNumUnits = -1;
    private boolean mIsDefaultHdrShader = false;
    private InputFormat mCurrentInputformat = InputFormat.UNKNOWN;

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
     * @return Info about the initialized graphics device.
     */
    @NonNull
    public GraphicDeviceInfo init(@NonNull DynamicRange dynamicRange,
            @NonNull ShaderProvider shaderProvider) {
        checkInitializedOrThrow(mInitialized, false);
        GraphicDeviceInfo.Builder infoBuilder = GraphicDeviceInfo.builder();
        try {
            if (dynamicRange.is10BitHdr()) {
                Pair<String, String> extensions = getExtensionsBeforeInitialized(dynamicRange);
                String glExtensions = Preconditions.checkNotNull(extensions.first);
                String eglExtensions = Preconditions.checkNotNull(extensions.second);
                if (!glExtensions.contains("GL_EXT_YUV_target")) {
                    Logger.w(TAG, "Device does not support GL_EXT_YUV_target. Fallback to SDR.");
                    dynamicRange = DynamicRange.SDR;
                }
                mSurfaceAttrib = chooseSurfaceAttrib(eglExtensions, dynamicRange);
                infoBuilder.setGlExtensions(glExtensions);
                infoBuilder.setEglExtensions(eglExtensions);
            }
            createEglContext(dynamicRange, infoBuilder);
            createTempSurface();
            makeCurrent(mTempSurface);
            infoBuilder.setGlVersion(getGlVersionNumber());
            mProgramHandle = createProgram(dynamicRange, shaderProvider);
            mIsDefaultHdrShader = dynamicRange.is10BitHdr();
            loadLocations();
            mExternalTextureId = createTexture();
            mExternalTexNumUnits = getTexNumUnits();
            useAndConfigureProgram();
        } catch (IllegalStateException | IllegalArgumentException e) {
            releaseInternal();
            throw e;
        }
        mGlThread = Thread.currentThread();
        mInitialized.set(true);
        return infoBuilder.build();
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
        checkGlThreadOrThrow(mGlThread);
        releaseInternal();
    }

    /**
     * Register the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     *                               on the GL thread.
     */
    public void registerOutputSurface(@NonNull Surface surface) {
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

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
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

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
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

        return mExternalTextureId;
    }

    /**
     * Sets the input format.
     *
     * <p>This will ensure the correct sampler is used for the input.
     *
     * @param inputFormat The input format for the input texture.
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     *                               on the GL thread.
     */
    public void setInputFormat(@NonNull InputFormat inputFormat) {
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

        if (mCurrentInputformat != inputFormat) {
            mCurrentInputformat = inputFormat;
            activateExternalTexture();
        }
    }

    private void activateExternalTexture() {
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

        int texUnit = GLES20.GL_TEXTURE0;
        if (mIsDefaultHdrShader) {
            GLES20.glUniform1i(mSamplerSelectorLoc, mCurrentInputformat.getSamplerSelector());
            checkGlErrorOrThrow("glUniform1i " + mCurrentInputformat);

            if (mCurrentInputformat == InputFormat.YUV) {
                texUnit = GLES20.GL_TEXTURE0 + mExternalTexNumUnits;
            }
        }
        GLES20.glActiveTexture(texUnit);
        checkGlErrorOrThrow("glActiveTexture");

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);
        checkGlErrorOrThrow("glBindTexture");
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
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

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
        activateExternalTexture();
    }

    // Returns a pair of GL extension (first) and EGL extension (second) strings.
    @NonNull
    private Pair<String, String> getExtensionsBeforeInitialized(
            @NonNull DynamicRange dynamicRangeToInitialize) {
        checkInitializedOrThrow(mInitialized, false);
        try {
            createEglContext(dynamicRangeToInitialize, /*infoBuilder=*/null);
            createTempSurface();
            makeCurrent(mTempSurface);
            // eglMakeCurrent() has to be called before checking GL_EXTENSIONS.
            String glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
            String eglExtensions = EGL14.eglQueryString(mEglDisplay, EGL14.EGL_EXTENSIONS);
            return new Pair<>(glExtensions != null ? glExtensions : "", eglExtensions != null
                    ? eglExtensions : "");
        } catch (IllegalStateException e) {
            Logger.w(TAG, "Failed to get GL or EGL extensions: " + e.getMessage(), e);
            return new Pair<>("", "");
        } finally {
            releaseInternal();
        }
    }

    private void createEglContext(@NonNull DynamicRange dynamicRange,
            @Nullable GraphicDeviceInfo.Builder infoBuilder) {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new IllegalStateException("Unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
            throw new IllegalStateException("Unable to initialize EGL14");
        }

        if (infoBuilder != null) {
            infoBuilder.setEglVersion(version[0] + "." + version[1]);
        }

        int rgbBits = dynamicRange.is10BitHdr() ? 10 : 8;
        int alphaBits = dynamicRange.is10BitHdr() ? 2 : 8;
        int renderType = dynamicRange.is10BitHdr() ? EGLExt.EGL_OPENGL_ES3_BIT_KHR
                : EGL14.EGL_OPENGL_ES2_BIT;
        // TODO(b/319277249): It will crash on older Samsung devices for HDR video 10-bit
        //  because EGLExt.EGL_RECORDABLE_ANDROID is only supported from OneUI 6.1. We need to
        //  check by GPU Driver version when new OS is release.
        int recordableAndroid = dynamicRange.is10BitHdr() ? EGL10.EGL_DONT_CARE : EGL14.EGL_TRUE;
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

    private void useAndConfigureProgram() {
        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        checkGlErrorOrThrow("glUseProgram");

        // Initialize the samplers to the correct texture unit offsets
        GLES20.glUniform1i(mSamplerDefaultLoc, 0);
        if (mIsDefaultHdrShader) {
            GLES20.glUniform1i(mSamplerYuvLoc, mExternalTexNumUnits);
        }

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

        // Activate the texture
        activateExternalTexture();
    }

    private void loadLocations() {
        mPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        checkLocationOrThrow(mPositionLoc, "aPosition");
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        checkLocationOrThrow(mTexCoordLoc, "aTextureCoord");
        mTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        checkLocationOrThrow(mTexMatrixLoc, "uTexMatrix");
        mSamplerDefaultLoc = GLES20.glGetUniformLocation(mProgramHandle, VAR_TEXTURE);
        checkLocationOrThrow(mSamplerDefaultLoc, VAR_TEXTURE);
        if (mIsDefaultHdrShader) {
            mSamplerYuvLoc = GLES20.glGetUniformLocation(mProgramHandle, VAR_TEXTURE_YUV);
            checkLocationOrThrow(mSamplerYuvLoc, VAR_TEXTURE_YUV);
            mSamplerSelectorLoc = GLES20.glGetUniformLocation(mProgramHandle, "uSamplerSelector");
            checkLocationOrThrow(mSamplerSelectorLoc, "uSamplerSelector");
        }
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
        mSamplerDefaultLoc = -1;
        mSamplerYuvLoc = -1;
        mSamplerSelectorLoc = -1;
        mExternalTextureId = -1;
        mExternalTexNumUnits = -1;
        mCurrentInputformat = InputFormat.UNKNOWN;
        mCurrentSurface = null;
        mGlThread = null;
        mIsDefaultHdrShader = false;
    }

    @NonNull
    private OutputSurface getOutSurfaceOrThrow(@NonNull Surface surface) {
        Preconditions.checkState(mOutputSurfaceMap.containsKey(surface),
                "The surface is not registered.");

        return requireNonNull(mOutputSurfaceMap.get(surface));
    }

    @Nullable
    private OutputSurface createOutputSurfaceInternal(@NonNull Surface surface) {
        EGLSurface eglSurface;
        try {
            eglSurface = createWindowSurface(mEglDisplay, requireNonNull(mEglConfig), surface,
                    mSurfaceAttrib);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Logger.w(TAG, "Failed to create EGL surface: " + e.getMessage(), e);
            return null;
        }

        Size size = getSurfaceSize(mEglDisplay, eglSurface);
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
}
