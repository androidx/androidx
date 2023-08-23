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

import static androidx.camera.effects.opengl.Utils.checkGlErrorOrThrow;
import static androidx.camera.effects.opengl.Utils.checkLocationOrThrow;

import android.opengl.GLES20;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

/**
 * A GL program that copies the source while overlaying a texture on top of it.
 */
@RequiresApi(21)
class GlProgramOverlay extends GlProgram {

    private static final String TAG = "GlProgramOverlay";

    static final String TEXTURE_MATRIX = "uTexMatrix";
    static final String OVERLAY_SAMPLER = "samplerOverlayTexture";

    private static final String VERTEX_SHADER = "uniform mat4 " + TEXTURE_MATRIX + ";\n"
            + "attribute vec4 " + POSITION_ATTRIBUTE + ";\n"
            + "attribute vec4 " + TEXTURE_ATTRIBUTE + ";\n"
            + "varying vec2 " + TEXTURE_COORDINATES + ";\n"
            + "void main() {\n"
            + "    gl_Position = " + POSITION_ATTRIBUTE + ";\n"
            + "    " + TEXTURE_COORDINATES + " = (" + TEXTURE_MATRIX + " * "
            + TEXTURE_ATTRIBUTE + ").xy;\n"
            + "}";

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 " + TEXTURE_COORDINATES + ";\n"
            + "uniform samplerExternalOES " + INPUT_SAMPLER + ";\n"
            + "uniform sampler2D " + OVERLAY_SAMPLER + ";\n"
            + "void main() {\n"
            + "    vec4 inputColor = texture2D(" + INPUT_SAMPLER + ", "
            + TEXTURE_COORDINATES + ");\n"
            + "    vec4 overlayColor = texture2D(" + OVERLAY_SAMPLER + ", "
            + TEXTURE_COORDINATES + ");\n"
            + "    gl_FragColor = inputColor * (1.0 - overlayColor.a) + overlayColor;\n"
            + "}";

    // Location of the texture matrix used in vertex shader.
    private int mTextureMatrixLoc = -1;

    GlProgramOverlay() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    protected void configure() {
        super.configure();
        // Associate input sampler with texture unit 0 (GL_TEXTURE0).
        int inputSamplerLoc = GLES20.glGetUniformLocation(mProgramHandle, INPUT_SAMPLER);
        checkLocationOrThrow(inputSamplerLoc, INPUT_SAMPLER);
        GLES20.glUniform1i(inputSamplerLoc, 0);

        // Associate overlay sampler with texture unit 1 (GL_TEXTURE1);
        int overlaySamplerLoc = GLES20.glGetUniformLocation(mProgramHandle, OVERLAY_SAMPLER);
        checkLocationOrThrow(overlaySamplerLoc, OVERLAY_SAMPLER);
        GLES20.glUniform1i(overlaySamplerLoc, 1);

        // Setup the location of the texture matrix.
        mTextureMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, TEXTURE_MATRIX);
        checkLocationOrThrow(mTextureMatrixLoc, TEXTURE_MATRIX);
    }

    @Override
    protected void release() {
        super.release();
        mTextureMatrixLoc = -1;
    }

    /**
     * Draws the input texture to the Surface with the overlay texture.
     *
     * @param inputTextureTarget the texture target of the input texture. This could be either
     *                           GLES11Ext.GL_TEXTURE_EXTERNAL_OES or GLES20.GL_TEXTURE_2D,
     *                           depending if copying from an external texture or a 2D texture.
     * @param inputTextureId     the texture id of the input texture. This could be either an
     *                           external texture or a 2D texture.
     * @param overlayTextureId   the texture id of the overlay texture. This must be a 2D texture.
     * @param matrix             the texture transformation matrix.
     * @param glContext          the GL context which has the EGLSurface of the Surface.
     * @param surface            the surface to draw to.
     * @param timestampNs        the timestamp of the frame in nanoseconds.
     */
    void draw(int inputTextureTarget, int inputTextureId, int overlayTextureId,
            @NonNull float[] matrix, @NonNull GlContext glContext, @NonNull Surface surface,
            long timestampNs) {
        use();

        // Uploads the texture transformation matrix.
        GLES20.glUniformMatrix4fv(mTextureMatrixLoc, 1, false, matrix, 0);
        checkGlErrorOrThrow("glUniformMatrix4fv");

        // Bind the input texture to GL_TEXTURE0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(inputTextureTarget, inputTextureId);
        checkGlErrorOrThrow("glBindTexture");

        // Bind the overlay texture to TEXTURE1
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId);
        checkGlErrorOrThrow("glBindTexture");

        try {
            glContext.drawAndSwap(surface, timestampNs);
        } catch (IllegalStateException e) {
            Logger.w(TAG, "Failed to draw the frame", e);
        }
    }
}
