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

package androidx.camera.media3.effect

import android.annotation.SuppressLint
import android.opengl.Matrix
import androidx.media3.common.util.Size
import androidx.media3.effect.GlMatrixTransformation

/**
 * A [GlMatrixTransformation] that samples the texture according to the orientation matrix and draws
 * onto the new texture.
 */
@SuppressLint("UnsafeOptInUsageError")
internal class CameraXGlTransformation(
    cameraXTransformation: FloatArray,
    private val outputSize: Size
) : GlMatrixTransformation {
    // Adjusted orientation matrix for vertex shader usage
    private val glMatrix = FloatArray(16)

    // Raw transformation matrix passed from SurfaceOutput
    private val cameraXTransform = cameraXTransformation.clone()

    /**
     * Calculates the transformation needed by the media3 effect.
     *
     * {@inheritDoc}
     *
     * <gl_pos, f(A * B * NDC * gl_pos)> is how the GL shaders map the texture coordinates to NDC
     * coordinates, where:
     * - gl_pos: vertex position.
     * - A: transformation provided by camera framework, representing the camera sensor orientation.
     * - B: transformation provided by CameraX, representing device orientation and crop rect.
     * - NDC: transformation from texture coordinate system to NDC coordinate system.
     * - f() = color function given a position
     *
     * We desire a vertex and fragment pair that is scaled with: <gl_pos, f(A * B * NDC * gl_pos)>
     * But the media3 effect shader does not allow the additional transformation B. Thus, by
     * replacing gl_pos with NDC_inv * B_inv * NDC * gl_pos, we achieve:
     *
     * <NDC_inv * B_inv * NDC * gl_pos, f(A * NDC * gl_pos)>
     *
     * This method computes the glMatrix field to be equal to NDC_inv * B_inv * NDC. The shaders
     * used by media3 effect are located in the following directory:
     * https://github.com/androidx/media/tree/release/libraries/effect/src/main/assets/shaders
     */
    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        // glMatrix = NDC * B
        Matrix.multiplyMM(glMatrix, 0, cameraXTransform, 0, NDC, 0)
        // glMatrix = NDC * B * NDC_inv
        Matrix.multiplyMM(glMatrix, 0, NDC_INV, 0, glMatrix, 0)
        // glMatrix = (NDC * B * NDC_inv) ^ -1 = NDC_inv * B_inv * NDC
        Matrix.invertM(glMatrix, 0, glMatrix, 0)
        return outputSize
    }

    override fun getGlMatrixArray(presentationTimeUs: Long): FloatArray {
        return glMatrix
    }

    private companion object {
        // Matrix that maps texture space [0, 1] to NDC [-1, 1]
        private val NDC =
            floatArrayOf(
                0.5f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.5f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                0.5f,
                0.5f,
                0.0f,
                1.0f
            )

        // Matrix that maps NDC [-1, 1] to texture space [0, 1]
        private val NDC_INV =
            floatArrayOf(
                2.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                2.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                -1.0f,
                -1.0f,
                0.0f,
                1.0f
            )
    }
}
