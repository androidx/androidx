/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.processing.concurrent;

import static androidx.camera.core.processing.util.GLUtils.NO_OUTPUT_SURFACE;
import static androidx.camera.core.processing.util.GLUtils.checkGlErrorOrThrow;
import static androidx.camera.core.processing.util.GLUtils.checkGlThreadOrThrow;
import static androidx.camera.core.processing.util.GLUtils.checkInitializedOrThrow;
import static androidx.camera.core.processing.util.GLUtils.create4x4IdentityMatrix;
import static androidx.camera.core.processing.util.GLUtils.createTexture;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.processing.OpenGlRenderer;
import androidx.camera.core.processing.ShaderProvider;
import androidx.camera.core.processing.util.GLUtils;
import androidx.camera.core.processing.util.GLUtils.InputFormat;
import androidx.camera.core.processing.util.GLUtils.SamplerShaderProgram;
import androidx.camera.core.processing.util.GraphicDeviceInfo;
import androidx.camera.core.processing.util.OutputSurface;
import androidx.core.util.Preconditions;

import java.util.Map;

/**
 * An internal augmented {@link OpenGlRenderer} for dual concurrent cameras.
 */
@WorkerThread
public final class DualOpenGlRenderer extends OpenGlRenderer {

    private static final String TAG = "DualOpenGlRenderer";

    private int mPrimaryExternalTextureId = -1;
    private int mSecondaryExternalTextureId = -1;

    @NonNull
    private final CompositionSettings mPrimaryCompositionSettings;
    @NonNull
    private final CompositionSettings mSecondaryCompositionSettings;

    public DualOpenGlRenderer(
            @NonNull CompositionSettings primaryCompositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings) {
        mPrimaryCompositionSettings = primaryCompositionSettings;
        mSecondaryCompositionSettings = secondaryCompositionSettings;
    }

    @NonNull
    @Override
    public GraphicDeviceInfo init(@NonNull DynamicRange dynamicRange,
            @NonNull Map<InputFormat, ShaderProvider> shaderProviderOverrides) {
        GraphicDeviceInfo graphicDeviceInfo = super.init(dynamicRange, shaderProviderOverrides);
        mPrimaryExternalTextureId = createTexture();
        mSecondaryExternalTextureId = createTexture();
        return graphicDeviceInfo;
    }

    @Override
    public void release() {
        super.release();
        mPrimaryExternalTextureId = -1;
        mSecondaryExternalTextureId = -1;
    }

    /**
     * Gets the texture name.
     *
     * @return the texture name
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     *                               on the GL thread.
     */
    public int getTextureName(boolean isPrimary) {
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

        return isPrimary ? mPrimaryExternalTextureId : mSecondaryExternalTextureId;
    }

    /**
     * Renders the texture image to the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized, the caller doesn't run
     *                               on the GL thread or the surface is not registered by
     *                               {@link #registerOutputSurface(Surface)}.
     */
    public void render(long timestampNs,
            @NonNull Surface surface,
            @NonNull SurfaceOutput surfaceOutput,
            @NonNull SurfaceTexture primarySurfaceTexture,
            @NonNull SurfaceTexture secondarySurfaceTexture) {
        checkInitializedOrThrow(mInitialized, true);
        checkGlThreadOrThrow(mGlThread);

        OutputSurface outputSurface = getOutSurfaceOrThrow(surface);

        if (outputSurface == NO_OUTPUT_SURFACE) {
            outputSurface = createOutputSurfaceInternal(surface);
            if (outputSurface == null) {
                return;
            }

            mOutputSurfaceMap.put(surface, outputSurface);
        }

        if (surface != mCurrentSurface) {
            makeCurrent(outputSurface.getEglSurface());
            mCurrentSurface = surface;
        }

        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        // Primary Camera
        renderInternal(outputSurface, surfaceOutput, primarySurfaceTexture,
                mPrimaryCompositionSettings, mPrimaryExternalTextureId, true);
        // Secondary Camera
        // Only use primary camera info for output surface
        renderInternal(outputSurface, surfaceOutput, secondarySurfaceTexture,
                mSecondaryCompositionSettings, mSecondaryExternalTextureId, true);

        EGLExt.eglPresentationTimeANDROID(mEglDisplay, outputSurface.getEglSurface(), timestampNs);

        if (!EGL14.eglSwapBuffers(mEglDisplay, outputSurface.getEglSurface())) {
            Logger.w(TAG, "Failed to swap buffers with EGL error: 0x" + Integer.toHexString(
                    EGL14.eglGetError()));
            removeOutputSurfaceInternal(surface, false);
        }
    }

    private void renderInternal(
            @NonNull OutputSurface outputSurface,
            @NonNull SurfaceOutput surfaceOutput,
            @NonNull SurfaceTexture surfaceTexture,
            @NonNull CompositionSettings compositionSettings,
            int externalTextureId,
            boolean isPrimary) {
        useAndConfigureProgramWithTexture(externalTextureId);
        GLES20.glViewport(0, 0, outputSurface.getWidth(),
                outputSurface.getHeight());
        GLES20.glScissor(0, 0, outputSurface.getWidth(),
                outputSurface.getHeight());

        float[] textureTransform = new float[16];
        surfaceTexture.getTransformMatrix(textureTransform);

        float[] surfaceOutputMatrix = new float[16];
        surfaceOutput.updateTransformMatrix(
                surfaceOutputMatrix, textureTransform, isPrimary);

        GLUtils.Program2D currentProgram = Preconditions.checkNotNull(mCurrentProgram);
        if (currentProgram instanceof SamplerShaderProgram) {
            ((SamplerShaderProgram) currentProgram).updateTextureMatrix(surfaceOutputMatrix);
        }

        float[] transTransform = getTransformMatrix(
                new Size((int) (outputSurface.getWidth() * compositionSettings.getScale().first),
                        (int) (outputSurface.getHeight() * compositionSettings.getScale().second)),
                new Size(outputSurface.getWidth(), outputSurface.getHeight()),
                compositionSettings);
        currentProgram.updateTransformMatrix(transTransform);

        currentProgram.updateAlpha(compositionSettings.getAlpha());

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(
                /* srcRGB= */ GLES20.GL_SRC_ALPHA,
                /* dstRGB= */ GLES20.GL_ONE_MINUS_SRC_ALPHA,
                /* srcAlpha= */ GLES20.GL_ONE,
                /* dstAlpha= */ GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /*firstVertex=*/0, /*vertexCount=*/4);
        checkGlErrorOrThrow("glDrawArrays");

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @NonNull
    private static float[] getTransformMatrix(
            @NonNull Size overlaySize,
            @NonNull Size backgroundSize,
            @NonNull CompositionSettings compositionSettings) {
        float[] aspectRatioMatrix = create4x4IdentityMatrix();
        float[] overlayFrameAnchorMatrix = create4x4IdentityMatrix();
        float[] transformationMatrix = create4x4IdentityMatrix();

        Matrix.scaleM(
                aspectRatioMatrix,
                /* mOffset= */ 0,
                (float) overlaySize.getWidth() / backgroundSize.getWidth(),
                (float) overlaySize.getHeight() / backgroundSize.getHeight(),
                /* z= */ 1.0f);

        // Translate the image.
        if (compositionSettings.getScale().first != 0.0f
                || compositionSettings.getScale().second != 0.0f) {
            Matrix.translateM(
                    overlayFrameAnchorMatrix,
                    /* mOffset= */ 0,
                    compositionSettings.getOffset().first / compositionSettings.getScale().first,
                    compositionSettings.getOffset().second / compositionSettings.getScale().second,
                    /* z= */ 0.0f);
        }

        // Correct for aspect ratio of image in output frame.
        Matrix.multiplyMM(
                transformationMatrix,
                /* resultOffset= */ 0,
                aspectRatioMatrix,
                /* lhsOffset= */ 0,
                overlayFrameAnchorMatrix,
                /* rhsOffset= */ 0);

        return transformationMatrix;
    }
}
