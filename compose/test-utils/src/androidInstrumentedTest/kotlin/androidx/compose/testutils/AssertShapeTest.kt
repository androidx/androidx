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

package androidx.compose.testutils

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AssertShapeTest {
    @Test
    fun testAssertShape_rectInRect_inCenter_alignedWithRaster() {
        testAssertShape(
            shape = RectangleShape,
            shapeColor = Color.Red,
            shapeSize = Size(10f, 10f),
            backgroundShape = RectangleShape,
            backgroundSize = Size(20f, 20f),
        )
    }

    @Test
    fun testAssertShape_rectInRect_inCenter_misalignedWithRaster() {
        testAssertShape(
            shape = RectangleShape,
            shapeColor = Color.Red,
            shapeSize = Size(11.3f, 11.3f),
            backgroundShape = RectangleShape,
            backgroundSize = Size(21.1f, 21.1f),
        )
    }

    @Test
    fun testAssertShape_circleInRect_inCenter() {
        testAssertShape(
            shape = CircleShape,
            shapeColor = Color.Red,
            shapeSize = Size(50f, 50f),
            backgroundShape = RectangleShape,
            backgroundSize = Size(56f, 56f),
            antiAliasingGap = 2f,
        )
    }

    @Test
    fun testAssertShape_circleInRect_offCenter() {
        testAssertShape(
            shape = CircleShape,
            shapeColor = Color.Red,
            shapeSize = Size(50f, 40f),
            shapeCenter = Offset(60f, 40f),
            backgroundShape = RectangleShape,
            backgroundSize = Size(40f, 60f),
            backgroundCenter = Offset(60f, 40f),
            antiAliasingGap = 1.21f,
        )
    }

    @Test
    fun testAssertShape_roundedRectInOval() {
        testAssertShape(
            shape = RoundedCornerShape(10f),
            shapeColor = Color.Red,
            shapeSize = Size(30f, 30f),
            backgroundShape = CircleShape,
            backgroundSize = Size(70f, 40f),
            antiAliasingGap = 1.1f,
        )
    }

    @Test
    fun testAssertShape_rectOnRect_partialOverlap_1() =
        testAssertShape_partialOverlap_1(RectangleShape, RectangleShape)

    @Test
    fun testAssertShape_circleOnRect_partialOverlap_1() =
        testAssertShape_partialOverlap_1(CircleShape, RectangleShape, 1.2f)

    @Test
    fun testAssertShape_circleOnCircle_partialOverlap_1() =
        testAssertShape_partialOverlap_1(CircleShape, CircleShape, 1.2f)

    private fun testAssertShape_partialOverlap_1(
        shape: Shape,
        backgroundShape: Shape,
        antiAliasingGap: Float = 1f,
    ) {
        //    +-----+
        // +--| . . |--+
        // |  |     |  |
        // +--| . . |--+
        //    +-----+
        testAssertShape(
            shape = shape,
            shapeColor = Color.Red,
            shapeSize = Size(20f, 30f),
            backgroundShape = backgroundShape,
            backgroundSize = Size(30f, 20f),
            antiAliasingGap = antiAliasingGap,
        )
    }

    @Test
    fun testAssertShape_rectOnRect_partialOverlap_2() =
        testAssertShape_partialOverlap_2(RectangleShape, RectangleShape)

    @Test
    fun testAssertShape_circleOnRect_partialOverlap_2() =
        testAssertShape_partialOverlap_2(CircleShape, RectangleShape, 1.2f)

    @Test
    fun testAssertShape_circleOnCircle_partialOverlap_2() =
        testAssertShape_partialOverlap_2(CircleShape, CircleShape, 1.2f)

    private fun testAssertShape_partialOverlap_2(
        shape: Shape,
        backgroundShape: Shape,
        antiAliasingGap: Float = 1f,
    ) {
        //    +-----+
        //    |  + .|----+
        //    |  .  |    |
        //    |  + .|----+
        //    +-----+
        testAssertShape(
            shape = shape,
            shapeColor = Color.Red,
            shapeSize = Size(20f, 30f),
            backgroundShape = backgroundShape,
            backgroundSize = Size(30f, 20f),
            backgroundCenter = Offset(65f, 50f),
            antiAliasingGap = antiAliasingGap,
        )
    }

    @Test
    fun testAssertShape_rectOnRect_partialOverlap_3() =
        testAssertShape_partialOverlap_3(RectangleShape, RectangleShape)

    @Test
    fun testAssertShape_circleOnRect_partialOverlap_3() =
        testAssertShape_partialOverlap_3(CircleShape, RectangleShape, 1.2f)

    @Test
    fun testAssertShape_circleOnCircle_partialOverlap_3() =
        testAssertShape_partialOverlap_3(CircleShape, CircleShape, 1.2f)

    private fun testAssertShape_partialOverlap_3(
        shape: Shape,
        backgroundShape: Shape,
        antiAliasingGap: Float = 1f,
    ) {
        //    +-----+
        //    |  + .|---+
        //    |  .  |   |
        //    +-----+   |
        //       +------+
        testAssertShape(
            shape = shape,
            shapeColor = Color.Red,
            shapeSize = Size(20f, 20f),
            backgroundShape = backgroundShape,
            backgroundSize = Size(20f, 20f),
            backgroundCenter = Offset(60f, 60f),
            antiAliasingGap = antiAliasingGap,
        )
    }

    /**
     * Tests [ImageBitmap.assertShape] by creating a bitmap reflecting the asserted values, and then
     * asserting that bitmap with assertShape.
     */
    private fun testAssertShape(
        bitmapSize: IntSize = IntSize(100, 100),
        shape: Shape,
        shapeColor: Color,
        shapeSize: Size,
        shapeCenter: Offset = Offset(bitmapSize.width / 2f, bitmapSize.height / 2f),
        backgroundShape: Shape = RectangleShape,
        backgroundColor: Color = Color.Yellow,
        backgroundSize: Size = shapeSize,
        backgroundCenter: Offset = Offset(bitmapSize.width / 2f, bitmapSize.height / 2f),
        antiAliasingGap: Float = 1f,
    ) {
        val bitmap =
            createTestBitmap(
                bitmapSize = bitmapSize,
                shape = shape,
                shapeColor = shapeColor,
                shapeSize = shapeSize,
                shapeCenter = shapeCenter,
                backgroundShape = backgroundShape,
                backgroundColor = backgroundColor,
                backgroundSize = backgroundSize,
                backgroundCenter = backgroundCenter,
            )
        bitmap.assertShape(
            density = Density(1f),
            shape = shape,
            shapeColor = shapeColor,
            shapeSize = shapeSize,
            shapeCenter = shapeCenter,
            backgroundShape = backgroundShape,
            backgroundColor = backgroundColor,
            backgroundSize = backgroundSize,
            backgroundCenter = backgroundCenter,
            antiAliasingGap = antiAliasingGap,
        )
    }

    // Test that our createTestBitmap function below creates the correct bitmap. We check pixels
    // directly with assertPixels in case assertShape is broken.
    @Test
    fun testCreateTestBitmap_withAssertPixels() {
        // This test creates the following bitmap, where Y = yellow, R = red:
        //
        //    0  1  2  3  4  5  6  7  8  9
        // 0  .  .  .  .  .  .  .  .  .  .
        // 1  .  .  Y  Y  Y  Y  Y  Y  Y  Y
        // 2  .  .  Y  Y  R  R  R  R  Y  Y
        // 3  .  .  Y  Y  R  R  R  R  Y  Y
        // 4  .  .  Y  Y  R  R  R  R  Y  Y
        // 5  .  .  Y  Y  R  R  R  R  Y  Y
        // 6  .  .  Y  Y  Y  Y  Y  Y  Y  Y
        // 7  .  .  .  .  .  .  .  .  .  .
        // 8  .  .  .  .  .  .  .  .  .  .
        // 9  .  .  .  .  .  .  .  .  .  .
        val bitmap =
            createTestBitmap(
                bitmapSize = IntSize(width = 10, height = 10),
                shape = RectangleShape,
                shapeColor = Color.Red,
                shapeSize = Size(4f, 4f),
                shapeCenter = Offset(6f, 4f),
                backgroundShape = RectangleShape,
                backgroundColor = Color.Yellow,
                backgroundSize = Size(8f, 6f),
                backgroundCenter = Offset(6f, 4f),
            )
        bitmap.assertPixels(IntSize(10, 10)) { pos ->
            // If the pixel is within the (larger) background area..
            if (pos.x in 2..9 && pos.y in 1..6) {
                // And if it also within the (smaller) shape area..
                if (pos.x in 4..7 && pos.y in 2..5) {
                    // Then it's red
                    Color.Red
                } else {
                    // So it's in the outer, but not the inner area. It's yellow
                    Color.Yellow
                }
            } else {
                // This pixel hasn't been drawn in this test, skip it
                null
            }
        }
    }

    // Test that the bitmap used in testCreateTestBitmap also passes when verifying with assertShape
    @Test
    fun testCreateTestBitmap_withAssertShape() {
        testAssertShape(
            bitmapSize = IntSize(width = 10, height = 10),
            shape = RectangleShape,
            shapeColor = Color.Red,
            shapeSize = Size(4f, 4f),
            shapeCenter = Offset(6f, 4f),
            backgroundShape = RectangleShape,
            backgroundColor = Color.Yellow,
            backgroundSize = Size(8f, 6f),
            backgroundCenter = Offset(6f, 4f),
        )
    }

    private fun createTestBitmap(
        bitmapSize: IntSize,
        shape: Shape,
        shapeColor: Color,
        shapeSize: Size,
        shapeCenter: Offset,
        backgroundShape: Shape,
        backgroundColor: Color,
        backgroundSize: Size,
        backgroundCenter: Offset,
    ): ImageBitmap {
        val bitmap = ImageBitmap(bitmapSize.width, bitmapSize.height)
        val canvas = Canvas(bitmap)
        canvas.drawShape(backgroundShape, backgroundColor, backgroundSize, backgroundCenter)
        canvas.drawShape(shape, shapeColor, shapeSize, shapeCenter)
        return bitmap
    }

    private fun Size.asOffset(): Offset = Offset(width, height)

    private fun Canvas.drawShape(
        shape: Shape,
        color: Color,
        size: Size,
        center: Offset,
        density: Density = Density(1f),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr
    ) {
        val backgroundOffset = center - (size / 2f).asOffset()
        val backgroundOutline = shape.createOutline(size, layoutDirection, density)
        CanvasDrawScope().draw(density, layoutDirection, this, size) {
            translate(backgroundOffset.x, backgroundOffset.y) {
                drawOutline(backgroundOutline, color)
            }
        }
    }
}
