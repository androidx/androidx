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

package androidx.graphics.filters

import android.content.Context
import android.opengl.GLES20
import android.util.Pair
import androidx.media3.common.FrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.effect.SingleFrameGlTextureProcessor
import java.io.IOException

/** Applies a {@link Vignette} effect to each frame in the fragment shader. */

/**
 * Creates a new instance.
 *
 * @param context The {@link Context}.
 * @param vignetteEffect The {@link Vignette} to apply to each frame in order.
 * @param useHdr Whether input textures come from an HDR source. If {@code true}, colors will be in
 *   linear RGB BT.2020. If {@code false}, colors will be in linear RGB BT.709.
 * @throws FrameProcessingException If a problem occurs while reading shader files.
 */
internal class VignetteProcessor(context: Context?, vignetteEffect: Vignette, useHdr: Boolean) :
    SingleFrameGlTextureProcessor(useHdr) {
    private var glProgram: GlProgram? = null
    private var aspectRatio: Float = 0.0f

    private val vignetteEffect: Vignette

    init {
        this.vignetteEffect = vignetteEffect

        glProgram =
            try {
                GlProgram(context!!, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH)
            } catch (e: IOException) {
                throw FrameProcessingException(e)
            } catch (e: GlUtil.GlException) {
                throw FrameProcessingException(e)
            }

        // Draw the frame on the entire normalized device coordinate space, from -1 to 1, for x and
        // y.
        glProgram!!.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
        )
        val identityMatrix = GlUtil.create4x4IdentityMatrix()
        glProgram!!.setFloatsUniform("uTransformationMatrix", identityMatrix)
        glProgram!!.setFloatsUniform("uTexTransformationMatrix", identityMatrix)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Pair<Int, Int> {
        this.aspectRatio = inputWidth.toFloat() / inputHeight.toFloat()

        return Pair.create(inputWidth, inputHeight)
    }

    @Throws(FrameProcessingException::class)
    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram!!.use()
            // Set the various uniform right before rendering.  This allows values to
            // be changed or interpolated between frames.

            val shouldVignetteAlpha =
                this.vignetteEffect.vignetteStyle == Vignette.VignetteStyle.ALPHA ||
                    this.vignetteEffect.vignetteStyle == Vignette.VignetteStyle.COLOR_AND_ALPHA
            val shouldVignetteColor =
                this.vignetteEffect.vignetteStyle == Vignette.VignetteStyle.COLOR ||
                    this.vignetteEffect.vignetteStyle == Vignette.VignetteStyle.COLOR_AND_ALPHA
            glProgram!!.setFloatUniform("uAspectRatio", this.aspectRatio)
            glProgram!!.setFloatUniform("uInnerRadius", this.vignetteEffect.innerRadius)
            glProgram!!.setFloatUniform("uOuterRadius", this.vignetteEffect.outerRadius)
            glProgram!!.setIntUniform("uShouldVignetteAlpha", if (shouldVignetteAlpha) 1 else 0)
            glProgram!!.setIntUniform("uShouldVignetteColor", if (shouldVignetteColor) 1 else 0)
            glProgram!!.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex= */ 0)
            glProgram!!.bindAttributesAndUniforms()

            // The four-vertex triangle strip forms a quad.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4)
        } catch (e: GlUtil.GlException) {
            throw FrameProcessingException(e, presentationTimeUs)
        }
    }

    @Throws(FrameProcessingException::class)
    override fun release() {
        super.release()
        try {
            glProgram!!.delete()
        } catch (e: GlUtil.GlException) {
            throw FrameProcessingException(e)
        }
    }

    companion object {
        private const val VERTEX_SHADER_PATH = "shaders/vertex_shader_transformation_es2.glsl"
        private const val FRAGMENT_SHADER_PATH = "shaders/fragment_shader_vignette_es2.glsl"
    }
}
