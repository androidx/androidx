/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.viewfinder.core.impl

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.SurfaceControl

/**
 * The arguments to [SurfaceControl.Transaction.setGeometry] that rotate the content of a layer
 * clockwise by a multiple of 90 degrees, scaled to fit its original `width x height` bounds.
 *
 * For quarter turns, [sourceCrop] is a square that contains the content, with sides that are a
 * multiple of the reduced aspect ratio, so that [destinationFrame] expresses the exact non-uniform
 * scale with integer coordinates. [transform] is passed as the `orientation` of `setGeometry()`,
 * which forwards it to the native transaction as a buffer transform; this is not part of the
 * documented contract, so the viewfinder's instrumented tests check the rendered output.
 */
public class ContentRotationGeometry
private constructor(
    public val sourceCrop: Rect,
    public val destinationFrame: Rect,
    public val transform: Int,
) {
    // SurfaceControl.BUFFER_TRANSFORM_* were only added to the SDK in API 32, but are compile-time
    // constants matching the native buffer transforms supported since API 29.
    @SuppressLint("InlinedApi")
    public companion object {
        public const val BUFFER_TRANSFORM_IDENTITY: Int = SurfaceControl.BUFFER_TRANSFORM_IDENTITY
        public const val BUFFER_TRANSFORM_ROTATE_90: Int = SurfaceControl.BUFFER_TRANSFORM_ROTATE_90
        public const val BUFFER_TRANSFORM_ROTATE_180: Int =
            SurfaceControl.BUFFER_TRANSFORM_ROTATE_180
        public const val BUFFER_TRANSFORM_ROTATE_270: Int =
            SurfaceControl.BUFFER_TRANSFORM_ROTATE_270

        /**
         * Creates the geometry that rotates `width x height` content clockwise by
         * [rotationDegrees], scaled to fit the original bounds.
         *
         * @throws IllegalArgumentException if [rotationDegrees] is not a multiple of 90, if the
         *   dimensions are not positive, or if they are too large for integer [Rect] coordinates.
         */
        @JvmStatic
        public fun create(rotationDegrees: Int, width: Int, height: Int): ContentRotationGeometry {
            require(width > 0 && height > 0) { "Invalid content size: ${width}x$height" }
            val bounds = Rect(0, 0, width, height)
            return when (rotationDegrees.mod(360)) {
                0 -> ContentRotationGeometry(bounds, Rect(bounds), BUFFER_TRANSFORM_IDENTITY)
                180 -> ContentRotationGeometry(bounds, Rect(bounds), BUFFER_TRANSFORM_ROTATE_180)
                90 -> createQuarterTurnGeometry(width, height, clockwise90 = true)
                270 -> createQuarterTurnGeometry(width, height, clockwise90 = false)
                else ->
                    throw IllegalArgumentException(
                        "Rotation degrees must be a multiple of 90: $rotationDegrees"
                    )
            }
        }

        private fun createQuarterTurnGeometry(
            width: Int,
            height: Int,
            clockwise90: Boolean,
        ): ContentRotationGeometry {
            val divisor = gcd(width, height)
            val reducedWidth = (width / divisor).toLong()
            val reducedHeight = (height / divisor).toLong()
            // Long math can't overflow for Int dimensions. Reduced dimensions above
            // floor(sqrt(Int.MAX_VALUE)) make destWidth or destHeight exceed Int.MAX_VALUE.
            val unitSide = reducedWidth * reducedHeight
            val scale = (maxOf(width, height).toLong() - 1L) / unitSide + 1L
            val side = scale * unitSide
            val destWidth = scale * reducedWidth * reducedWidth
            val destHeight = scale * reducedHeight * reducedHeight
            require(
                side <= Int.MAX_VALUE &&
                    width.toLong() + destWidth <= Int.MAX_VALUE &&
                    height.toLong() + destHeight <= Int.MAX_VALUE
            ) {
                "Content size too large to rotate without overflow: ${width}x$height"
            }
            val sideInt = side.toInt()
            val destinationFrame =
                if (clockwise90) {
                    // Clockwise: (x, y) -> (width - y * width / height, x * height / width)
                    Rect(width, -sideInt, width + destWidth.toInt(), -sideInt + destHeight.toInt())
                } else {
                    // Counter-clockwise: (x, y) -> (y * width / height, height - x * height /
                    // width)
                    Rect(
                        -sideInt,
                        height,
                        -sideInt + destWidth.toInt(),
                        height + destHeight.toInt(),
                    )
                }
            return ContentRotationGeometry(
                sourceCrop = Rect(0, 0, sideInt, sideInt),
                destinationFrame = destinationFrame,
                transform =
                    if (clockwise90) BUFFER_TRANSFORM_ROTATE_270 else BUFFER_TRANSFORM_ROTATE_90,
            )
        }

        private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    }
}
