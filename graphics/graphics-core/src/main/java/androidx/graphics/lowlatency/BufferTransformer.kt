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

package androidx.graphics.lowlatency

import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.surface.SurfaceControlCompat

/**
 * Class responsible for computing the corresponding transformations necessary to support
 * pre-rotation. Consumers are expected to use the corresponding [bufferWidth] and [bufferHeight]
 * parameters to configure with [GLES20.glViewport] as well as [transform] that should be consumed
 * in any vertex shader computations
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class BufferTransformer {

    private val mViewTransform = FloatArray(16)

    val transform: FloatArray
        get() = mViewTransform

    var logicalWidth = 0
        private set

    var logicalHeight = 0
        private set

    var bufferWidth = 0
        private set

    var bufferHeight = 0
        private set

    var computedTransform: Int = BufferTransformHintResolver.UNKNOWN_TRANSFORM
        private set

    fun invertBufferTransform(transform: Int): Int =
        when (transform) {
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 ->
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 ->
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 ->
                SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
            SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY ->
                SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
            else -> BufferTransformHintResolver.UNKNOWN_TRANSFORM // Return unknown transform
        }

    /** Update the [transform] matrix to reflect the given buffer [transformHint]. */
    fun computeTransform(width: Int, height: Int, transformHint: Int) {
        val fWidth = width.toFloat()
        val fHeight = height.toFloat()
        logicalWidth = width
        logicalHeight = height
        bufferWidth = width
        bufferHeight = height
        computedTransform = transformHint
        when (transformHint) {
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 -> {
                Matrix.setRotateM(mViewTransform, 0, -90f, 0f, 0f, 1f)
                Matrix.translateM(mViewTransform, 0, -fWidth, 0f, 0f)
                bufferWidth = height
                bufferHeight = width
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 -> {
                Matrix.setRotateM(mViewTransform, 0, 180f, 0f, 0f, 1f)
                Matrix.translateM(mViewTransform, 0, -fWidth, -fHeight, 0f)
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 -> {
                Matrix.setRotateM(mViewTransform, 0, 90f, 0f, 0f, 1f)
                Matrix.translateM(mViewTransform, 0, 0f, -fHeight, 0f)
                bufferWidth = height
                bufferHeight = width
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY -> {
                Matrix.setIdentityM(mViewTransform, 0)
            }
            // Identity or unknown case, just set the identity matrix
            else -> {
                computedTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
                Matrix.setIdentityM(mViewTransform, 0)
            }
        }
    }
}
