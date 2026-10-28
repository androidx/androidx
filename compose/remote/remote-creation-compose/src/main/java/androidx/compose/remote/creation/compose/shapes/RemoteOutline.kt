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
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.remotePath
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathScope

/**
 * Defines a simple shape, used for bounding graphical regions.
 *
 * Can be used for defining a shape of the component background, a shape of shadows cast by the
 * component, or to clip the contents.
 */
public sealed class RemoteOutline {

    /**
     * Rectangular area.
     *
     * @param topLeft The top-left offset of the rectangle.
     * @param size The dimensions of the rectangle.
     */
    public class Rectangle(public val topLeft: RemoteOffset, public val size: RemoteSize) :
        RemoteOutline()

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
    public class Rounded(
        public val topStart: RemoteFloat,
        public val topEnd: RemoteFloat,
        public val bottomEnd: RemoteFloat,
        public val bottomStart: RemoteFloat,
        public val offset: RemoteOffset = RemoteOffset.Zero,
        public val size: RemoteSize? = null,
    ) : RemoteOutline()

    /** An area defined as a path. */
    public class Generic : RemoteOutline {
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val path: RemotePath?
        internal val block: (RemotePathScope.() -> Unit)?

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public constructor(path: RemotePath) : super() {
            this.path = path
            this.block = null
        }

        /**
         * Creates a generic outline defined by [block].
         *
         * @param block A builder lambda that populates a [RemotePathScope].
         */
        public constructor(block: RemotePathScope.() -> Unit) : super() {
            this.path = null
            this.block = block
        }
    }

    private object NonExhaustive : RemoteOutline()
}

/**
 * Draws the [RemoteOutline] on a [RemoteDrawScope].
 *
 * @param outline the outline to draw.
 * @param paint the paint used for the drawing.
 */
public fun RemoteDrawScope.drawOutline(outline: RemoteOutline, paint: RemotePaint) {
    when (outline) {
        is RemoteOutline.Rectangle -> {
            drawRect(paint, outline.topLeft, outline.size)
        }
        is RemoteOutline.Rounded -> {
            val corners = resolveRoundedCorners(outline)
            if (corners.isUniform) {
                remoteCanvas.drawRoundRect(
                    left = corners.left,
                    top = corners.top,
                    right = corners.right,
                    bottom = corners.bottom,
                    rx = corners.topLeft,
                    ry = corners.topLeft,
                    paint = paint,
                )
                return
            }
            drawPath(roundedOutlinePath(corners), paint)
        }
        is RemoteOutline.Generic -> {
            val p = genericOutlinePath(outline)
            if (p != null) {
                drawPath(p, paint)
            }
        }
        else -> {}
    }
}

/**
 * Clips the drawing area to the [RemoteOutline] and executes [block] within it.
 *
 * This emits a path clip, so callers must check that the current profile supports it, for example
 * with `currentProfileSupportsPathClip()`. If the outline can't be expressed as a path, [block] is
 * executed without clipping.
 *
 * @param outline the outline to clip to.
 * @param block the drawing operations to execute within the clipped area.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteDrawScope.clipOutline(outline: RemoteOutline, block: RemoteDrawScope.() -> Unit) {
    val path =
        when (outline) {
            is RemoteOutline.Rectangle -> {
                val left = outline.topLeft.x
                val top = outline.topLeft.y
                val right = left + outline.size.width
                val bottom = top + outline.size.height
                remotePath {
                    moveTo(left, top)
                    lineTo(right, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                }
            }
            is RemoteOutline.Rounded -> roundedOutlinePath(resolveRoundedCorners(outline))
            is RemoteOutline.Generic -> genericOutlinePath(outline)
            else -> null
        }
    if (path != null) {
        clipPath(path, block = block)
    } else {
        block()
    }
}

/** The resolved bounds and layout direction aware corner radii of a [RemoteOutline.Rounded]. */
private class RoundedCorners(
    val left: RemoteFloat,
    val top: RemoteFloat,
    val right: RemoteFloat,
    val bottom: RemoteFloat,
    val topLeft: RemoteFloat,
    val topRight: RemoteFloat,
    val bottomRight: RemoteFloat,
    val bottomLeft: RemoteFloat,
) {
    val isUniform: Boolean
        get() =
            areEqual(topLeft, topRight) &&
                areEqual(topRight, bottomRight) &&
                areEqual(bottomRight, bottomLeft)
}

private fun RemoteDrawScope.resolveRoundedCorners(outline: RemoteOutline.Rounded): RoundedCorners {
    // Compute the bounding rectangle [left, top, right, bottom] from origin `offset`
    // and dimensions `size`. When drawing a centered stroke of width S, `offset` is
    // (S/2, S/2) and `size` is (W - S, H - S), yielding bounds [S/2, S/2, W - S/2, H -
    // S/2].
    val left = outline.offset.x
    val top = outline.offset.y
    val right = left + (outline.size?.width ?: width)
    val bottom = top + (outline.size?.height ?: height)

    // Remap corner radii based on layout direction
    return when (remoteCanvas.remoteLayoutDirection) {
        RemoteLayoutDirection.Ltr ->
            RoundedCorners(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                topLeft = outline.topStart,
                topRight = outline.topEnd,
                bottomRight = outline.bottomEnd,
                bottomLeft = outline.bottomStart,
            )
        RemoteLayoutDirection.Rtl ->
            RoundedCorners(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                topLeft = outline.topEnd,
                topRight = outline.topStart,
                bottomRight = outline.bottomStart,
                bottomLeft = outline.bottomEnd,
            )
    }
}

private fun RemoteDrawScope.roundedOutlinePath(corners: RoundedCorners): RemotePath {
    val left = corners.left
    val top = corners.top
    val right = corners.right
    val bottom = corners.bottom
    val rTopLeft = corners.topLeft
    val rTopRight = corners.topRight
    val rBottomRight = corners.bottomRight
    val rBottomLeft = corners.bottomLeft

    val kappa = 0.55228475f.rf
    val cTopLeft = rTopLeft * kappa
    val cTopRight = rTopRight * kappa
    val cBottomRight = rBottomRight * kappa
    val cBottomLeft = rBottomLeft * kappa

    return remotePath {
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
}

private fun RemoteDrawScope.genericOutlinePath(outline: RemoteOutline.Generic): RemotePath? =
    outline.path ?: outline.block?.let { remotePath(it) }

private fun areEqual(a: RemoteFloat, b: RemoteFloat): Boolean =
    a === b ||
        a.cacheKey == b.cacheKey ||
        (a.hasConstantValue && b.hasConstantValue && a.constantValue == b.constantValue)
