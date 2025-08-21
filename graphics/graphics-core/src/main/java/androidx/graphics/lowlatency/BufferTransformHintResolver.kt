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

import android.graphics.Matrix
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.annotation.RequiresApi
import androidx.graphics.surface.JniBindings
import androidx.graphics.surface.SurfaceControlCompat

/**
 * Helper class to determine the corresponding transformation hint based on various Android API
 * levels
 */
internal class BufferTransformHintResolver {

    /** Equivalent to [android.view.AttachedSurfaceControl.getBufferTransformHint]. */
    fun getBufferTransformHint(view: View): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            return TransformHintHelper.resolveBufferTransformHint(view)
        } else {
            val orientation: String?
            return try {
                orientation = JniBindings.nGetDisplayOrientation()
                val rotation = view.display?.rotation
                if (rotation != null) {
                    val transform =
                        getBufferTransformHintFromInstallOrientation(orientation, rotation)
                    Log.v(TAG, "Obtained transform: $transform for orientation: $orientation")
                    transform
                } else {
                    Log.w(TAG, "Unable to obtain current display rotation")
                    UNKNOWN_TRANSFORM
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Unable to obtain current display orientation")
                UNKNOWN_TRANSFORM
            }
        }
    }

    internal fun getBufferTransformHintFromInstallOrientation(
        orientation: String,
        rotation: Int,
    ): Int =
        when (orientation) {
            ORIENTATION_90 -> {
                when (rotation) {
                    Surface.ROTATION_0 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
                    Surface.ROTATION_90 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
                    Surface.ROTATION_180 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
                    Surface.ROTATION_270 -> SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
                    else -> UNKNOWN_TRANSFORM
                }
            }
            ORIENTATION_180 -> {
                when (rotation) {
                    Surface.ROTATION_0 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
                    Surface.ROTATION_90 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
                    Surface.ROTATION_180 -> SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
                    Surface.ROTATION_270 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
                    else -> UNKNOWN_TRANSFORM
                }
            }
            ORIENTATION_270 -> {
                when (rotation) {
                    Surface.ROTATION_0 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
                    Surface.ROTATION_90 -> SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
                    Surface.ROTATION_180 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
                    Surface.ROTATION_270 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
                    else -> UNKNOWN_TRANSFORM
                }
            }
            ORIENTATION_0 -> {
                when (rotation) {
                    Surface.ROTATION_0 -> SurfaceControlCompat.BUFFER_TRANSFORM_IDENTITY
                    Surface.ROTATION_90 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90
                    Surface.ROTATION_180 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180
                    Surface.ROTATION_270 -> SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270
                    else -> UNKNOWN_TRANSFORM
                }
            }
            else -> {
                Log.w(TAG, "Unknown orientation \"$orientation\"")
                UNKNOWN_TRANSFORM
            }
        }

    internal companion object {

        const val TAG = "TRANSFORM_HINT_RESOLVER"

        const val UNKNOWN_TRANSFORM = -1

        const val ORIENTATION_0 = "ORIENTATION_0"
        const val ORIENTATION_90 = "ORIENTATION_90"
        const val ORIENTATION_180 = "ORIENTATION_180"
        const val ORIENTATION_270 = "ORIENTATION_270"

        @RequiresApi(Build.VERSION_CODES.Q)
        internal fun configureTransformMatrix(
            matrix: Matrix,
            width: Float,
            height: Float,
            @SurfaceControlCompat.Companion.BufferTransform transform: Int,
        ): Matrix =
            matrix.apply {
                when (transform) {
                    SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 -> {
                        reset()
                        setRotate(90f)
                        postTranslate(width, 0f)
                    }
                    SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 -> {
                        reset()
                        setRotate(180f)
                        postTranslate(width, height)
                    }
                    SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 -> {
                        reset()
                        setRotate(270f)
                        postTranslate(0f, height)
                    }
                    else -> {
                        reset()
                    }
                }
            }
    }
}

/** Helper class to avoid class verification errors */
@RequiresApi(Build.VERSION_CODES.S_V2)
internal class TransformHintHelper private constructor() {

    companion object {
        @RequiresApi(Build.VERSION_CODES.S_V2)
        fun resolveBufferTransformHint(view: View): Int =
            view.rootSurfaceControl?.bufferTransformHint ?: 0
    }
}
