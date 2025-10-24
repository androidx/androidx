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

package androidx.compose.ui.graphics

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.roundToIntRect
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PathReversionTest {
    @Test
    fun emptyPath() {
        val path = Path().reverse()
        assertTrue(path.isEmpty)
    }

    @Test
    fun singleMove() {
        val path = Path().apply { moveTo(10.0f, 10.0f) }.reverse()

        val iterator = path.iterator()
        assertTrue(iterator.hasNext())

        val segment = iterator.next()
        assertEquals(PathSegment.Type.Move, segment.type)
        assertPointEquals(Offset(10.0f, 10.0f), segment.points, 0)
    }

    @Test
    fun lineTo() {
        val path =
            Path()
                .apply {
                    moveTo(10.0f, 10.0f)
                    lineTo(20.0f, 20.0f)
                }
                .reverse()

        val iterator = path.iterator()
        assertTrue(iterator.hasNext())

        assertEquals(2, iterator.calculateSize(false))

        var segment = iterator.next()
        assertEquals(PathSegment.Type.Move, segment.type)
        assertPointEquals(Offset(20.0f, 20.0f), segment.points, 0)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(10.0f, 10.0f), segment.points, 1)
    }

    @Test
    fun close() {
        val path =
            Path()
                .apply {
                    moveTo(10.0f, 10.0f)
                    lineTo(20.0f, 20.0f)
                    lineTo(10.0f, 30.0f)
                    close()
                }
                .reverse()

        val iterator = path.iterator()
        assertTrue(iterator.hasNext())

        assertEquals(4, iterator.calculateSize(false))

        var segment = iterator.next()
        assertEquals(PathSegment.Type.Move, segment.type)
        assertPointEquals(Offset(10.0f, 30.0f), segment.points, 0)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(20.0f, 20.0f), segment.points, 1)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(10.0f, 10.0f), segment.points, 1)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Close, segment.type)
    }

    @Test
    fun multipleContours() {
        val path =
            Path()
                .apply {
                    moveTo(10.0f, 10.0f)
                    lineTo(20.0f, 20.0f)
                    lineTo(10.0f, 30.0f)
                    close()

                    moveTo(50.0f, 50.0f)
                    lineTo(70.0f, 70.0f)
                    lineTo(50.0f, 90.0f)
                }
                .reverse()

        val iterator = path.iterator()
        assertTrue(iterator.hasNext())

        assertEquals(7, iterator.calculateSize(false))

        var segment = iterator.next()
        assertEquals(PathSegment.Type.Move, segment.type)
        assertPointEquals(Offset(50.0f, 90.0f), segment.points, 0)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(70.0f, 70.0f), segment.points, 1)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(50.0f, 50.0f), segment.points, 1)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Move, segment.type)
        assertPointEquals(Offset(10.0f, 30.0f), segment.points, 0)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(20.0f, 20.0f), segment.points, 1)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Line, segment.type)
        assertPointEquals(Offset(10.0f, 10.0f), segment.points, 1)

        segment = iterator.next()
        assertEquals(PathSegment.Type.Close, segment.type)
    }

    @Test
    fun directionIsReversed() {
        val path = createSvgPath(SvgShape.Heart)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())

        val reversed = path.reverse()
        assertEquals(Path.Direction.CounterClockwise, reversed.computeDirection())
    }

    @Test
    fun contoursDirectionIsReversed() {
        val path = createSvgPath(SvgShape.Cubics) // this shape has a cutout
        val reversed = path.reverse()

        assertTrue(
            path
                .divide()
                .zip(reversed.divide().reversed()) { a, b ->
                    a.computeDirection() != b.computeDirection()
                }
                .all { it }
        )
    }

    @Test
    fun pixelComparison() {
        val paint =
            Paint().apply {
                style = Paint.Style.FILL
                color = Color.RED
                isAntiAlias = false
            }

        for (svg in listOf(SvgShape.Cubics, SvgShape.Quads, SvgShape.Heart, SvgShape.Lines)) {
            val path = createSvgPath(svg)
            val reversed = path.reverse()

            val bounds1 = path.getBounds().roundToIntRect()
            val bounds2 = reversed.getBounds().roundToIntRect()
            assertEquals(bounds1, bounds2)

            val reference =
                createBitmap(bounds1.width, bounds1.height).applyCanvas {
                    drawPath(path.asAndroidPath(), paint)
                }
            val result =
                createBitmap(bounds2.width, bounds2.height).applyCanvas {
                    drawPath(reversed.asAndroidPath(), paint)
                }

            compareBitmaps(reference, result, 0)
        }
    }
}
