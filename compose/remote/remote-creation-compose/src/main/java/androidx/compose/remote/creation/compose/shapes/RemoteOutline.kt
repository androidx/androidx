/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.shapes

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.remotePath
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.LayoutDirection

/**
 * Defines a simple shape, used for bounding graphical regions.
 *
 * Can be used for defining a shape of the component background, a shape of shadows cast by the
 * component, or to clip the contents.
 */
public sealed class RemoteOutline {

    /** Rectangular area. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Rectangle(public val topLeft: RemoteOffset, public val size: RemoteSize) :
        RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            drawRect(paint, topLeft, size)
        }
    }

    /**
     * Rectangular area with rounded corners.
     *
     * @param topStart the resolved corner radius of the top start corner
     * @param topEnd the resolved corner radius of the top end corner
     * @param bottomEnd the resolved corner radius of the bottom end corner
     * @param bottomStart the resolved corner radius of the bottom start corner
     * @param offset the top-left offset of the rounded rectangle bounding box (e.g. `(0, 0)` for a
     *   solid background fill, or `(halfStroke, halfStroke)` to center a stroked border within the
     *   component bounds)
     * @param size the dimensions (width and height) of the rounded rectangle (e.g. `(width -
     *   strokeWidth, height - strokeWidth)` for an inset stroked border). If null, defaults to the
     *   full canvas width and height.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Rounded(
        internal val topStart: RemoteFloat,
        internal val topEnd: RemoteFloat,
        internal val bottomEnd: RemoteFloat,
        internal val bottomStart: RemoteFloat,
        internal val offset: RemoteOffset = RemoteOffset.Zero,
        internal val size: RemoteSize? = null,
    ) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            // Compute the bounding rectangle [left, top, right, bottom] from origin `offset`
            // and dimensions `size`. When drawing a centered stroke of width S, `offset` is
            // (S/2, S/2) and `size` is (W - S, H - S), yielding bounds [S/2, S/2, W - S/2, H -
            // S/2].
            val left = offset.x
            val top = offset.y
            val right = left + (this@Rounded.size?.width ?: width)
            val bottom = top + (this@Rounded.size?.height ?: height)

            // Remap corner radii based on layout direction
            val rTopLeft: RemoteFloat
            val rTopRight: RemoteFloat
            val rBottomRight: RemoteFloat
            val rBottomLeft: RemoteFloat

            when (remoteCanvas.layoutDirection) {
                LayoutDirection.Ltr -> {
                    rTopLeft = topStart
                    rTopRight = topEnd
                    rBottomRight = bottomEnd
                    rBottomLeft = bottomStart
                }
                LayoutDirection.Rtl -> {
                    rTopLeft = topEnd
                    rTopRight = topStart
                    rBottomRight = bottomStart
                    rBottomLeft = bottomEnd
                }
            }

            val isUniform =
                areEqual(rTopLeft, rTopRight) &&
                    areEqual(rTopRight, rBottomRight) &&
                    areEqual(rBottomRight, rBottomLeft)

            if (isUniform) {
                remoteCanvas.drawRoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    rx = rTopLeft,
                    ry = rTopLeft,
                    paint = paint,
                )
                return
            }

            val kappa = 0.55228475f.rf
            val cTopLeft = rTopLeft * kappa
            val cTopRight = rTopRight * kappa
            val cBottomRight = rBottomRight * kappa
            val cBottomLeft = rBottomLeft * kappa

            val path = remotePath {
                // 1. Move to top edge
                moveTo(left + rTopLeft, top)

                // 2. Top Line & Top-Right Corner
                lineTo(right - rTopRight, top)
                curveTo(
                    x1 = right - rTopRight + cTopRight,
                    y1 = top,
                    x2 = right,
                    y2 = top + rTopRight - cTopRight,
                    x3 = right,
                    y3 = top + rTopRight,
                )

                // 3. Right Line & Bottom-Right Corner
                lineTo(right, bottom - rBottomRight)
                curveTo(
                    x1 = right,
                    y1 = bottom - rBottomRight + cBottomRight,
                    x2 = right - rBottomRight + cBottomRight,
                    y2 = bottom,
                    x3 = right - rBottomRight,
                    y3 = bottom,
                )

                // 4. Bottom Line & Bottom-Left Corner
                lineTo(left + rBottomLeft, bottom)
                curveTo(
                    x1 = left + rBottomLeft - cBottomLeft,
                    y1 = bottom,
                    x2 = left,
                    y2 = bottom - rBottomLeft + cBottomLeft,
                    x3 = left,
                    y3 = bottom - rBottomLeft,
                )

                // 5. Start Line & Top-Left Corner
                lineTo(left, top + rTopLeft)
                curveTo(
                    x1 = left,
                    y1 = top + rTopLeft - cTopLeft,
                    x2 = left + rTopLeft - cTopLeft,
                    y2 = top,
                    x3 = left + rTopLeft,
                    y3 = top,
                )

                // 6. Close the path
                close()
            }
            drawPath(path, paint)
        }
    }

    /** An area defined as a path. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class Generic(public val path: RemotePath) : RemoteOutline() {
        override fun RemoteDrawScope.drawOutline(paint: RemotePaint) {
            drawPath(path, paint)
        }
    }

    /** Draws the outline to the canvas with paint. */
    public abstract fun RemoteDrawScope.drawOutline(paint: RemotePaint)
}

private fun areEqual(a: RemoteFloat, b: RemoteFloat): Boolean =
    a === b || (a.hasConstantValue && b.hasConstantValue && a.constantValue == b.constantValue)
