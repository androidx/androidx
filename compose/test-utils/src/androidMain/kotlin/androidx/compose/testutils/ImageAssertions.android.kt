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
 * Tests to see if the given point is within the path. (That is, whether the point would be in the
 * visible portion of the path if the path was used with [Canvas.clipPath].)
 *
 * The `point` argument is interpreted as an offset from the origin.
 *
 * Returns true if the point is in the path, and false otherwise.
 */
fun Path.contains(offset: Offset): Boolean {
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

/**
 * Asserts that the given [shape] is drawn in the bitmap with size [shapeSize] in the color
 * [shapeColor], in front of the [backgroundShape] that has size [backgroundSize].
 *
 * The size of a [Shape] is its rectangular bounding box. The centers of both the [shape] and the
 * [backgroundShape] are at [shapeAndBackgroundCenter]. Pixels that are outside the background area
 * are not checked. Pixels that are outside the [shape] and inside the [backgroundShape] must be
 * [backgroundColor]. Pixels that are inside the [shape] must be [shapeColor].
 *
 * To avoid the pixels on the edge of a shape, where anti-aliasing means the pixel is neither the
 * shape color nor the background color, a gap size can be given with [antiAliasingGap]. Pixels that
 * are close to the border of the shape are not checked. A larger [antiAliasingGap] means more
 * pixels are left unchecked, and a gap of 0 pixels means all pixels are tested.
 *
 * @param density current [Density] or the screen
 * @param shape the [Shape] of the foreground
 * @param shapeColor the color of the foreground shape
 * @param shapeSize the [Size] of the [shape]
 * @param shapeAndBackgroundCenter the center of both the [shape] and the [backgroundShape]
 * @param backgroundShape the [Shape] of the background
 * @param backgroundColor the color of the background shape
 * @param backgroundSize the [Size] of the [backgroundShape]
 * @param antiAliasingGap The size of the border area from the shape outline to leave it untested as
 *   it is likely anti-aliased. Only works for convex shapes. The default is 1 pixel
 */
// TODO (mount, malkov) : to investigate why it flakes when shape is not rect
fun ImageBitmap.assertShape(
    density: Density,
    shape: Shape,
    shapeColor: Color,
    backgroundColor: Color,
    backgroundShape: Shape = RectangleShape,
    backgroundSize: Size = Size(width.toFloat(), height.toFloat()),
    shapeSize: Size = backgroundSize,
    shapeAndBackgroundCenter: Offset = Offset(width / 2f, height / 2f),
    antiAliasingGap: Float = 1.0f
) {
    val pixels = toPixelMap()

    // the bounding box of the foreground shape in the bitmap
    val foregroundBounds =
        Rect(
            left = shapeAndBackgroundCenter.x - shapeSize.width / 2f,
            top = shapeAndBackgroundCenter.y - shapeSize.height / 2f,
            right = shapeAndBackgroundCenter.x + shapeSize.width / 2f,
            bottom = shapeAndBackgroundCenter.y + shapeSize.height / 2f,
        )
    // the bounding box of the background shape in the bitmap
    val backgroundBounds =
        Rect(
            left = shapeAndBackgroundCenter.x - backgroundSize.width / 2f,
            top = shapeAndBackgroundCenter.y - backgroundSize.height / 2f,
            right = shapeAndBackgroundCenter.x + backgroundSize.width / 2f,
            bottom = shapeAndBackgroundCenter.y + backgroundSize.height / 2f,
        )

    // Assert that the checked area is fully enclosed in the bitmap
    assert(backgroundBounds.top >= 0.0f) { "background is out of bounds (top side)" }
    assert(backgroundBounds.right <= width) { "background is out of bounds (right side)" }
    assert(backgroundBounds.bottom <= height) { "background is out of bounds (bottom side)" }
    assert(backgroundBounds.left >= 0.0f) { "background is out of bounds (left side)" }

    // Convert the shapes into a paths
    val foregroundPath = shape.asPath(foregroundBounds, density)
    val backgroundPath = backgroundShape.asPath(backgroundBounds, density)

    for (y in backgroundBounds.top until backgroundBounds.bottom) {
        for (x in backgroundBounds.left until backgroundBounds.right) {
            val point = Offset(x.toFloat(), y.toFloat())
            val pointFarther =
                pointFartherFromAnchor(point, shapeAndBackgroundCenter, antiAliasingGap)
            val pointCloser = pointCloserToAnchor(point, shapeAndBackgroundCenter, antiAliasingGap)
            if (!backgroundPath.contains(pointFarther)) {
                continue
            }
            val isInside = foregroundPath.contains(pointFarther)
            val isOutside = !foregroundPath.contains(pointCloser)
            if (isInside) {
                pixels.assertPixelColor(shapeColor, x, y)
            } else if (isOutside) {
                pixels.assertPixelColor(backgroundColor, x, y)
            }
        }
    }
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
    antiAliasingGap: Float = 1.0f
) {
    val shapeSize =
        Size(
            width - with(density) { horizontalPadding.toPx() * 2 },
            height - with(density) { verticalPadding.toPx() * 2 }
        )
    return assertShape(
        density = density,
        shape = shape,
        shapeColor = shapeColor,
        backgroundColor = backgroundColor,
        backgroundShape = RectangleShape,
        shapeSize = shapeSize,
        antiAliasingGap = antiAliasingGap
    )
}

private fun Shape.asPath(bounds: Rect, density: Density): Path {
    return android.graphics.Path().asComposePath().apply {
        addOutline(createOutline(bounds.size, LayoutDirection.Ltr, density))
        // Translate only modifies segments already added, so call it after addOutline
        translate(bounds.topLeft)
    }
}

private infix fun Float.until(until: Float): IntRange {
    val from = this.roundToInt()
    val to = until.roundToInt()
    if (from <= Int.MIN_VALUE) return IntRange.EMPTY
    return from until to
}

private fun pointCloserToAnchor(point: Offset, anchor: Offset, delta: Float): Offset {
    val x =
        when {
            point.x > anchor.x -> max(point.x - delta, anchor.x)
            point.x < anchor.x -> min(point.x + delta, anchor.x)
            else -> point.x
        }
    val y =
        when {
            point.y > anchor.y -> max(point.y - delta, anchor.y)
            point.y < anchor.y -> min(point.y + delta, anchor.y)
            else -> point.y
        }
    return Offset(x, y)
}

private fun pointFartherFromAnchor(point: Offset, anchor: Offset, delta: Float): Offset {
    val x =
        when {
            point.x > anchor.x -> point.x + delta
            point.x < anchor.x -> point.x - delta
            else -> point.x
        }
    val y =
        when {
            point.y > anchor.y -> point.y + delta
            point.y < anchor.y -> point.y - delta
            else -> point.y
        }
    return Offset(x, y)
}
