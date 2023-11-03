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
import static androidx.camera.effects.opengl.Utils.createFbo;
import static androidx.camera.effects.opengl.Utils.drawArrays;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import androidx.annotation.RequiresApi;

/**
 * A GL program that copies the input texture to the given 2D texture.
 *
 * <p>It assumes that the output texture has the same size as the input, so no transformation
 * needed.
 */
@RequiresApi(21)
class GlProgramCopy extends GlProgram {

    private static final String VERTEX_SHADER = "attribute vec4 " + POSITION_ATTRIBUTE + ";\n"
            + "attribute vec4 " + TEXTURE_ATTRIBUTE + ";\n"
            + "varying vec2 " + TEXTURE_COORDINATES + ";\n"
            + "void main() {\n"
            + "    gl_Position = " + POSITION_ATTRIBUTE + ";\n"
            + "    " + TEXTURE_COORDINATES + "= " + TEXTURE_ATTRIBUTE + ".xy;\n"
            + "}";

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 " + TEXTURE_COORDINATES + ";\n"
            + "uniform samplerExternalOES " + INPUT_SAMPLER + ";\n"
            + "void main() {\n"
            + "    gl_FragColor = texture2D(" + INPUT_SAMPLER + ", "
            + TEXTURE_COORDINATES + ");\n"
            + "}";

    // A FBO object for attaching the output texture.
    private int mFbo = -1;

    GlProgramCopy() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    protected void configure() {
        super.configure();
        // Create a FBO for attaching the output texture.
        mFbo = createFbo();
    }

    @Override
    protected void release() {
        super.release();
        // Delete the FBO.
        if (mFbo != -1) {
            GLES20.glDeleteFramebuffers(1, new int[]{mFbo}, 0);
            checkGlErrorOrThrow("glDeleteFramebuffers");
            mFbo = -1;
        }
    }

    /**
     * Copies the input texture to the output texture.
     *
     * @param inputTextureId  the input texture ID. Usually this is an external texture.
     * @param outputTextureId the output texture ID. This must be a 2D texture.
     * @param outputWidth     the width of the output textures.
     * @param outputHeight    the height of the output textures.
     */
    void draw(int inputTextureId, int outputTextureId, int outputWidth, int outputHeight) {
        use();

        // Bind external texture to TEXTURE0 as input texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlErrorOrThrow("glActiveTexture");
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputTextureId);
        checkGlErrorOrThrow("glBindTexture");

        // Bind FBO and attach the output texture.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFbo);
        checkGlErrorOrThrow("glBindFramebuffer");
        GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, outputTextureId, 0
        );
        checkGlErrorOrThrow("glFramebufferTexture2D");

        // Copy the input texture to the output texture
        drawArrays(outputWidth, outputHeight);

        // Unbind FBO.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        checkGlErrorOrThrow("glBindFramebuffer");
    }
}
