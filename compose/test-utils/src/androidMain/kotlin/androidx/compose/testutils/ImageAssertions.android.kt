/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.testutils

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A helper function to run asserts on [Bitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position. The
 *   returned color is then asserted as the expected one on the given bitmap.
 * @throws AssertionError if size or colors don't match.
 */
fun ImageBitmap.assertPixels(
    expectedSize: IntSize? = null,
    expectedColorProvider: (pos: IntOffset) -> Color?
) {
    if (expectedSize != null) {
        if (width != expectedSize.width || height != expectedSize.height) {
            throw AssertionError(
                "Bitmap size is wrong! Expected '$expectedSize' but got " + "'$width x $height'"
            )
        }
    }

    val pixel = toPixelMap()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pxPos = IntOffset(x, y)
            val expectedClr = expectedColorProvider(pxPos)
            if (expectedClr != null) {
                pixel.assertPixelColor(expectedClr, x, y)
            }
        }
    }
}

/** Asserts that the color at a specific pixel in the bitmap at ([x], [y]) is [expected]. */
fun PixelMap.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) {
    val actual = this[x, y]
    assert(abs(expected.red - actual.red) < 0.02f) { error(actual) }
    assert(abs(expected.green - actual.green) < 0.02f) { error(actual) }
    assert(abs(expected.blue - actual.blue) < 0.02f) { error(actual) }
    assert(abs(expected.alpha - actual.alpha) < 0.02f) { error(actual) }
}

/**
 * Asserts that the expected color is present in this bitmap.
 *
 * @throws AssertionError if the expected color is not present.
 */
fun ImageBitmap.assertContainsColor(expectedColor: Color): ImageBitmap {
    if (!containsColor(expectedColor)) {
        throw AssertionError("The given color $expectedColor was not found in the bitmap.")
    }
    return this
}

fun ImageBitmap.assertDoesNotContainColor(unexpectedColor: Color): ImageBitmap {
    if (containsColor(unexpectedColor)) {
        throw AssertionError("The given color $unexpectedColor was found in the bitmap.")
    }
    return this
}

private fun ImageBitmap.containsColor(expectedColor: Color): Boolean {
    val pixels = this.toPixelMap()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixels[x, y]
            if (color == expectedColor) {
                return true
            }
        }
    }
    return false
}

/**
 * Asserts that the given [shape] is drawn in the bitmap in the color [shapeColor] on a background
 * of [backgroundColor].
 *
 * The whole background of the bitmap should be filled with the [backgroundColor]. The [shape]'s
 * size is that of the bitmap, minus the [horizontalPadding] and [verticalPadding] on all sides. The
 * shape must be aligned in the center.
 *
 * To avoid the pixels on the edge of a shape, where anti-aliasing means the pixel is neither the
 * shape color nor the background color, a gap size can be given with [antiAliasingGap]. Pixels that
 * are close to the border of the shape are not checked. A larger [antiAliasingGap] means more
 * pixels are left unchecked, and a gap of 0 pixels means all pixels are tested.
 *
 * @param density current [Density] or the screen
 * @param shape the [Shape] of the foreground
 * @param shapeColor the color of the foreground shape
 * @param backgroundColor the color of the background shape
 * @param antiAliasingGap The size of the border area from the shape outline to leave it untested as
 *   it is likely anti-aliased. Only works for convex shapes. The default is 1 pixel
 */
fun ImageBitmap.assertShape(
    density: Density,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backgroundColor: Color,
    shapeColor: Color,
    shape: Shape = RectangleShape,
    antiAliasingGap: Float = with(density) { 1.dp.toPx() }
) =
    assertShape(
        density = density,
        shape = shape,
        shapeColor = shapeColor,
        backgroundColor = backgroundColor,
        backgroundShape = RectangleShape,
        shapeSize =
            Size(
                width - with(density) { horizontalPadding.toPx() * 2 },
                height - with(density) { verticalPadding.toPx() * 2 }
            ),
        antiAliasingGap = antiAliasingGap
    )

/**
 * Asserts that the given [shape] and [backgroundShape] are drawn in the bitmap according to the
 * given parameters.
 *
 * The [shape]'s bounding box should have size [shapeSize] and be centered at [shapeCenter]. The
 * [backgroundShape]'s bounding box should have size [backgroundSize] and be centered at
 * [backgroundCenter].
 *
 * Pixels that are outside the background area are not checked. Pixels that are outside the [shape]
 * and inside the [backgroundShape] must be [backgroundColor]. Pixels that are inside the [shape]
 * must be [shapeColor].
 *
 * If [backgroundColor] is `null`, only pixels inside the [shape] are checked.
 *
 * Because pixels on the edge of a shape are anti-aliased, pixels that are close the shape's edges
 * are not checked. Use [antiAliasingGap] to ignore more (or less) pixels around the shape's edges.
 * A larger [antiAliasingGap] means more pixels are left unchecked, and a gap of 0 pixels means all
 * pixels are tested.
 */
// TODO (mount, malkov) : to investigate why it flakes when shape is not rect
fun ImageBitmap.assertShape(
    density: Density,
    shape: Shape,
    shapeColor: Color,
    shapeSize: Size = Size(width.toFloat(), height.toFloat()),
    shapeCenter: Offset = Offset(width / 2f, height / 2f),
    backgroundShape: Shape = RectangleShape,
    backgroundColor: Color?,
    backgroundSize: Size = Size(width.toFloat(), height.toFloat()),
    backgroundCenter: Offset = Offset(width / 2f, height / 2f),
    antiAliasingGap: Float = with(density) { 1.dp.toPx() }
) {
    val pixels = toPixelMap()

    // the bounding box of the foreground shape in the bitmap
    val shapeBounds =
        Rect(
            left = shapeCenter.x - shapeSize.width / 2f,
            top = shapeCenter.y - shapeSize.height / 2f,
            right = shapeCenter.x + shapeSize.width / 2f,
            bottom = shapeCenter.y + shapeSize.height / 2f,
        )
    // the bounding box of the background shape in the bitmap
    val backgroundBounds =
        Rect(
            left = backgroundCenter.x - backgroundSize.width / 2f,
            top = backgroundCenter.y - backgroundSize.height / 2f,
            right = backgroundCenter.x + backgroundSize.width / 2f,
            bottom = backgroundCenter.y + backgroundSize.height / 2f,
        )

    // Convert the shapes into a paths
    val foregroundPath = shape.asPath(shapeBounds, density)
    val backgroundPath = backgroundShape.asPath(backgroundBounds, density)

    forEachPixelIn(shapeBounds, backgroundBounds, antiAliasingGap) { x, y, inFgBounds, inBgBounds ->
        if (inFgBounds && !inBgBounds) {
            // Only consider the foreground shape
            if (foregroundPath.contains(x, y, shapeCenter, antiAliasingGap)) {
                pixels.assertPixelColor(shapeColor, x, y)
            }
        } else if (inBgBounds && !inFgBounds) {
            // Only consider the background shape, if there is one
            if (
                backgroundColor != null &&
                    backgroundPath.contains(x, y, backgroundCenter, antiAliasingGap)
            ) {
                pixels.assertPixelColor(backgroundColor, x, y)
            }
        } else if (inFgBounds /* && inBgBounds */) {
            // Need to consider both the foreground and background (if there is one)
            if (foregroundPath.contains(x, y, shapeCenter, antiAliasingGap)) {
                pixels.assertPixelColor(shapeColor, x, y)
            } else if (
                backgroundColor != null &&
                    foregroundPath.notContains(x, y, shapeCenter, antiAliasingGap) &&
                    backgroundPath.contains(x, y, backgroundCenter, antiAliasingGap)
            ) {
                pixels.assertPixelColor(backgroundColor, x, y)
            }
        }
    }
}

private fun ImageBitmap.forEachPixelIn(
    shapeBounds: Rect,
    backgroundBounds: Rect,
    margin: Float,
    block: (x: Int, y: Int, inShapeBounds: Boolean, inBackgroundBounds: Boolean) -> Unit
) {
    // Iterate over all pixels in the background. Usually that's all we have to do.
    for (y in rowIndices(backgroundBounds)) {
        for (x in columnIndices(backgroundBounds)) {
            block.invoke(x, y, shapeBounds.contains(x, y, margin), true)
        }
    }
    // While checking the background area, we potentially checked a lot of foreground area at the
    // same time. Try to avoid checking pixels again.
    val shapeBoundsToCheck = shapeBounds.subtract(backgroundBounds)
    for (y in rowIndices(shapeBoundsToCheck)) {
        for (x in columnIndices(shapeBoundsToCheck)) {
            // Anything contained in the background is already checked
            if (!backgroundBounds.contains(x, y, margin)) {
                block.invoke(x, y, true, false)
            }
        }
    }
}

private fun ImageBitmap.rowIndices(bounds: Rect): IntRange =
    bounds.top.coerceAtLeast(0f) until bounds.bottom.coerceAtMost(height.toFloat())

private fun ImageBitmap.columnIndices(bounds: Rect): IntRange =
    bounds.left.coerceAtLeast(0f) until bounds.right.coerceAtMost(width.toFloat())

private infix fun Float.until(until: Float): IntRange {
    val from = this.roundToInt()
    val to = until.roundToInt()
    if (from <= Int.MIN_VALUE) return IntRange.EMPTY
    return from until to
}

private fun Shape.asPath(bounds: Rect, density: Density): Path {
    return android.graphics.Path().asComposePath().apply {
        addOutline(createOutline(bounds.size, LayoutDirection.Ltr, density))
        // Translate only modifies segments already added, so call it after addOutline
        translate(bounds.topLeft)
    }
}

private fun Path.contains(x: Int, y: Int, center: Offset, margin: Float): Boolean =
    contains(pointFartherFromAnchor(x, y, center, margin))

private fun Path.notContains(x: Int, y: Int, center: Offset, margin: Float): Boolean =
    !contains(pointCloserToAnchor(x, y, center, margin))

/**
 * Tests to see if the given point is within the path. (That is, whether the point would be in the
 * visible portion of the path if the path was used with [Canvas.clipPath].)
 *
 * The `point` argument is interpreted as an offset from the origin.
 *
 * Returns true if the point is in the path, and false otherwise.
 */
@VisibleForTesting
internal fun Path.contains(offset: Offset): Boolean {
    val path = android.graphics.Path()
    path.addRect(
        /* left = */ offset.x - 0.01f,
        /* top = */ offset.y - 0.01f,
        /* right = */ offset.x + 0.01f,
        /* bottom = */ offset.y + 0.01f,
        /* dir = */ android.graphics.Path.Direction.CW
    )
    if (path.op(asAndroidPath(), android.graphics.Path.Op.INTERSECT)) {
        return !path.isEmpty
    }
    return false
}

private fun pointCloserToAnchor(x: Int, y: Int, anchor: Offset, delta: Float): Offset {
    val xx =
        when {
            x > anchor.x -> max(x - delta, anchor.x)
            x < anchor.x -> min(x + delta, anchor.x)
            else -> x.toFloat()
        }
    val yy =
        when {
            y > anchor.y -> max(y - delta, anchor.y)
            y < anchor.y -> min(y + delta, anchor.y)
            else -> y.toFloat()
        }
    return Offset(xx, yy)
}

private fun pointFartherFromAnchor(x: Int, y: Int, anchor: Offset, delta: Float): Offset {
    val xx =
        when {
            x > anchor.x -> x + delta
            x < anchor.x -> x - delta
            else -> x.toFloat()
        }
    val yy =
        when {
            y > anchor.y -> y + delta
            y < anchor.y -> y - delta
            else -> y.toFloat()
        }
    return Offset(xx, yy)
}

private fun Rect.contains(x: Int, y: Int, margin: Float): Boolean =
    left <= x + margin && top <= y + margin && right >= x - margin && bottom >= y - margin

private fun Rect.subtract(other: Rect): Rect {
    // Subtraction can only happen if the other rect is overlapping entirely in a dimension
    if (other.left <= this.left && other.right >= this.right) {
        // Other rect potentially overlaps over entire width
        return if (other.top <= this.top && other.bottom >= this.bottom) {
            // Subtract everything
            Rect.Zero
        } else if (other.top <= this.top && other.bottom > this.top) {
            // Subtract from the top
            Rect(this.left, other.bottom, this.right, this.bottom)
        } else if (other.top < this.bottom && other.bottom >= this.bottom) {
            // Subtract from the bottom
            Rect(this.left, this.top, this.right, other.top)
        } else {
            // Subtract nothing
            this
        }
    } else if (other.top <= this.top && other.bottom >= this.bottom) {
        // Other rect potentially overlaps over entire height
        return if (other.left <= this.left && other.right > this.left) {
            // Subtract from the left
            Rect(other.right, this.top, this.right, this.bottom)
        } else if (other.left < this.right && other.right >= this.right) {
            // Subtract from the right
            Rect(this.left, this.top, other.left, this.bottom)
        } else {
            // Subtract nothing
            this
        }
    } else {
        // Subtract nothing
        return this
    }
}
