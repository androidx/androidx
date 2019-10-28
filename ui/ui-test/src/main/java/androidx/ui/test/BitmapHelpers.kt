/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.ui.core.Density
import androidx.ui.core.IntPxPosition
import androidx.ui.core.IntPxSize
import androidx.ui.core.PxSize
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Shape
import androidx.ui.engine.geometry.addOutline
import androidx.ui.graphics.Color
import androidx.ui.graphics.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Captures the underlying component's surface into bitmap.
 *
 * This has currently several limitations. Currently we assume that the component is hosted in
 * Activity's window. Also if there is another window covering part of the component if won't occur
 * in the bitmap as this is taken from the component's window surface.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToBitmap(): Bitmap {
    return semanticsTreeInteraction.captureNodeToBitmap(semanticsTreeNode)
}

/**
 * A helper function to run asserts on [Bitmap].
 *
 * @param expectedSize The expected size of the bitmap. Leave null to skip the check.
 * @param expectedColorProvider Returns the expected color for the provided pixel position.
 * The returned color is then asserted as the expected one on the given bitmap.
 *
 * @throws AssertionError if size or colors don't match.
 */
fun Bitmap.assertPixels(
    expectedSize: IntPxSize? = null,
    expectedColorProvider: (pos: IntPxPosition) -> Color?
) {
    if (expectedSize != null) {
        if (width != expectedSize.width.value || height != expectedSize.height.value) {
            throw AssertionError("Bitmap size is wrong! Expected '$expectedSize' but got " +
                    "'$width x $height'")
        }
    }

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pxPos = IntPxPosition(x.ipx, y.ipx)
            val color = Color(pixels[width * y + x])
            val expectedClr = expectedColorProvider(pxPos)
            if (expectedClr != null && expectedClr != color) {
                throw AssertionError("Comparison failed for $pxPos: expected $expectedClr $ " +
                        "but received $color")
            }
        }
    }
}

/**
 * Asserts that the color at a specific pixel in the bitmap at ([x], [y]) is [expected].
 */
fun Bitmap.assertPixelColor(
    expected: Color,
    x: Int,
    y: Int,
    error: (Color) -> String = { color -> "Pixel($x, $y) expected to be $expected, but was $color" }
) {
    val color = Color(getPixel(x, y))
    val errorString = error(color)
    assertEquals(errorString, expected.red, color.red, 0.01f)
    assertEquals(errorString, expected.green, color.green, 0.01f)
    assertEquals(errorString, expected.blue, color.blue, 0.01f)
    assertEquals(errorString, expected.alpha, color.alpha, 0.01f)
}

/**
 * Asserts that the given [shape] is drawn within the bitmap with the size the dimensions
 * [shapeSizeX] x [shapeSizeY], centered at ([centerX], [centerY]) with the color [shapeColor].
 * The bitmap area examined is [sizeX] x [sizeY], centered at ([centerX], [centerY]) and everything
 * outside the shape is expected to be color [backgroundColor].
 *
 * The border area of 1 pixel from the shape outline is left untested as it is likely anti-aliased.
 */
fun Bitmap.assertShape(
    backgroundColor: Color,
    sizeX: Int = width,
    sizeY: Int = height,
    shape: Shape,
    shapeColor: Color,
    shapeSizeX: Int = sizeX,
    shapeSizeY: Int = sizeY,
    centerX: Int = width / 2,
    centerY: Int = height / 2
) {
    assertTrue(centerX + sizeX / 2 <= width)
    assertTrue(centerX - sizeX / 2 >= 0)
    assertTrue(centerY + sizeY / 2 <= height)
    assertTrue(centerY - sizeY / 2 >= 0)
    val outline = shape.createOutline(PxSize(shapeSizeX.px, shapeSizeY.px), Density(1f))
    val path = Path()
    path.addOutline(outline)
    val shapeOffset = Offset(centerX.toFloat() - shapeSizeX.toFloat() / 2f,
        centerY.toFloat() - shapeSizeY.toFloat() / 2f)
    for (x in centerX - sizeX / 2 until centerX + sizeX / 2) {
        for (y in centerY - sizeY / 2 until centerY + sizeY / 2) {
            val offset = Offset(x.toFloat(), y.toFloat()) - shapeOffset
            val isInside = path.contains(pixelFartherFromCenter(offset, shapeSizeX, shapeSizeY))
            val isOutside = !path.contains(pixelCloserToCenter(offset, shapeSizeX, shapeSizeY))
            if (isInside) {
                assertPixelColor(shapeColor, x, y)
            } else if (isOutside) {
                assertPixelColor(backgroundColor, x, y)
            }
        }
    }
}

private fun pixelCloserToCenter(offset: Offset, shapeSizeX: Int, shapeSizeY: Int): Offset {
    val centerX = shapeSizeX.toFloat() / 2f
    val centerY = shapeSizeY.toFloat() / 2f
    val x = when {
        offset.dx > centerX -> offset.dx - 1
        offset.dx < centerX -> offset.dx + 1
        else -> offset.dx
    }
    val y = when {
        offset.dy > centerY -> offset.dy - 1
        offset.dy < centerY -> offset.dy + 1
        else -> offset.dy
    }
    return Offset(x, y)
}

private fun pixelFartherFromCenter(offset: Offset, shapeSizeX: Int, shapeSizeY: Int): Offset {
    val centerX = shapeSizeX.toFloat() / 2f
    val centerY = shapeSizeY.toFloat() / 2f
    val x = when {
        offset.dx > centerX -> offset.dx + 1
        offset.dx < centerX -> offset.dx - 1
        else -> offset.dx
    }
    val y = when {
        offset.dy > centerY -> offset.dy + 1
        offset.dy < centerY -> offset.dy - 1
        else -> offset.dy
    }
    return Offset(x, y)
}