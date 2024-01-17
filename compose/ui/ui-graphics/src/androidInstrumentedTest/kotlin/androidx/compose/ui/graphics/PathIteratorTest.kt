/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

fun assertPointsEquals(p1: Offset, p2: Offset) {
    assertEquals(p1.x, p2.x, 1e-6f)
    assertEquals(p1.y, p2.y, 1e-6f)
}

fun assertPointsEquals(p1: FloatArray, offset: Int, p2: Offset) {
    assertEquals(p1[0 + offset * 2], p2.x, 1e-6f)
    assertEquals(p1[1 + offset * 2], p2.y, 1e-6f)
}

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathIteratorTest {
    @Test
    fun emptyIterator() {
        val path = Path()

        val iterator = path.iterator()
        assertFalse(iterator.hasNext())

        var count = 0
        for (segment in path) {
            assertEquals(PathSegment.Type.Done, segment.type)
            count++
        }

        assertEquals(0, count)
    }

    @Test
    fun nonEmptyIterator() {
        val path = Path().apply {
            moveTo(1.0f, 1.0f)
            lineTo(2.0f, 2.0f)
            close()
        }

        val iterator = path.iterator()
        assertTrue(iterator.hasNext())

        val types = arrayOf(
            PathSegment.Type.Move,
            PathSegment.Type.Line,
            PathSegment.Type.Close
        )
        val points = arrayOf(
            Offset(1.0f, 1.0f),
            Offset(2.0f, 2.0f)
        )

        var count = 0
        for (segment in path) {
            assertEquals(types[count], segment.type)
            when (segment.type) {
                PathSegment.Type.Move -> {
                    assertEquals(points[count], Offset(segment.points[0], segment.points[1]))
                }
                PathSegment.Type.Line -> {
                    assertEquals(points[count - 1], Offset(segment.points[0], segment.points[1]))
                    assertEquals(points[count], Offset(segment.points[2], segment.points[3]))
                }
                else -> { }
            }
            count++
        }

        assertEquals(3, count)
    }

    @Test
    fun iteratorStyles() {
        val path = Path().apply {
            moveTo(1.0f, 1.0f)
            lineTo(2.0f, 2.0f)
            cubicTo(3.0f, 3.0f, 4.0f, 4.0f, 5.0f, 5.0f)
            quadraticTo(7.0f, 7.0f, 8.0f, 8.0f)
            moveTo(10.0f, 10.0f)
            // addRoundRect() will generate conic curves on certain API levels
            addRoundRect(
                RoundRect(12.0f, 12.0f, 36.0f, 36.0f, 8.0f, 8.0f)
            )
            close()
        }

        val iterator1 = path.iterator(PathIterator.ConicEvaluation.AsConic)
        val iterator2 = path.iterator(PathIterator.ConicEvaluation.AsConic)
        val iterator3 = path.iterator(PathIterator.ConicEvaluation.AsConic)

        val points = FloatArray(8)
        val points2 = FloatArray(16)

        while (iterator1.hasNext() || iterator2.hasNext() || iterator3.hasNext()) {
            val segment = iterator1.next()
            val type = iterator2.next(points)
            val type2 = iterator3.next(points2, 8)

            assertEquals(type, segment.type)
            assertEquals(type2, segment.type)

            val p = segment.points
            when (type) {
                PathSegment.Type.Move -> {
                    assertPointsEquals(points, 0, Offset(p[0], p[1]))
                    assertPointsEquals(points2, 4, Offset(p[0], p[1]))
                }
                PathSegment.Type.Line -> {
                    assertPointsEquals(points, 0, Offset(p[0], p[1]))
                    assertPointsEquals(points, 1, Offset(p[2], p[3]))
                    assertPointsEquals(points2, 4, Offset(p[0], p[1]))
                    assertPointsEquals(points2, 5, Offset(p[2], p[3]))
                }
                PathSegment.Type.Quadratic -> {
                    assertPointsEquals(points, 0, Offset(p[0], p[1]))
                    assertPointsEquals(points, 1, Offset(p[2], p[3]))
                    assertPointsEquals(points, 2, Offset(p[4], p[5]))
                    assertPointsEquals(points2, 4, Offset(p[0], p[1]))
                    assertPointsEquals(points2, 5, Offset(p[2], p[3]))
                    assertPointsEquals(points2, 6, Offset(p[4], p[5]))
                }
                PathSegment.Type.Conic -> {
                    assertPointsEquals(points, 0, Offset(p[0], p[1]))
                    assertPointsEquals(points, 1, Offset(p[2], p[3]))
                    assertPointsEquals(points, 2, Offset(p[4], p[5]))
                    assertEquals(points[6], segment.weight)

                    assertPointsEquals(points2, 4, Offset(p[0], p[1]))
                    assertPointsEquals(points2, 5, Offset(p[2], p[3]))
                    assertPointsEquals(points2, 6, Offset(p[4], p[5]))
                    assertEquals(points2[14], segment.weight)
                }
                PathSegment.Type.Cubic -> {
                    assertPointsEquals(points, 0, Offset(p[0], p[1]))
                    assertPointsEquals(points, 1, Offset(p[2], p[3]))
                    assertPointsEquals(points, 2, Offset(p[4], p[5]))
                    assertPointsEquals(points, 3, Offset(p[6], p[7]))

                    assertPointsEquals(points2, 4, Offset(p[0], p[1]))
                    assertPointsEquals(points2, 5, Offset(p[2], p[3]))
                    assertPointsEquals(points2, 6, Offset(p[4], p[5]))
                    assertPointsEquals(points2, 7, Offset(p[6], p[7]))
                }
                PathSegment.Type.Close -> { }
                PathSegment.Type.Done -> { }
            }
        }
    }

    @Test
    fun done() {
        val path = Path().apply {
            close()
        }

        val segment = path.iterator().next()

        assertEquals(PathSegment.Type.Done, segment.type)
        assertEquals(0, segment.points.size)
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun close() {
        val path = Path().apply {
            lineTo(10.0f, 12.0f)
            close()
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()
        // Swallow the line
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Close, segment.type)
        assertEquals(0, segment.points.size)
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun moveTo() {
        val path = Path().apply {
            moveTo(10.0f, 12.0f)
        }

        val segment = path.iterator().next()

        assertEquals(PathSegment.Type.Move, segment.type)
        val points = segment.points
        assertEquals(2, points.size)
        assertPointsEquals(Offset(10.0f, 12.0f), Offset(points[0], points[1]))
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun lineTo() {
        val path = Path().apply {
            moveTo(4.0f, 6.0f)
            lineTo(10.0f, 12.0f)
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Line, segment.type)
        val points = segment.points
        assertEquals(4, points.size)
        assertPointsEquals(Offset(4.0f, 6.0f), Offset(points[0], points[1]))
        assertPointsEquals(Offset(10.0f, 12.0f), Offset(points[2], points[3]))
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun quadraticTo() {
        val path = Path().apply {
            moveTo(4.0f, 6.0f)
            quadraticTo(10.0f, 12.0f, 20.0f, 24.0f)
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Quadratic, segment.type)
        val points = segment.points
        assertEquals(6, points.size)
        assertPointsEquals(Offset(4.0f, 6.0f), Offset(points[0], points[1]))
        assertPointsEquals(Offset(10.0f, 12.0f), Offset(points[2], points[3]))
        assertPointsEquals(Offset(20.0f, 24.0f), Offset(points[4], points[5]))
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun cubicTo() {
        val path = Path().apply {
            moveTo(4.0f, 6.0f)
            cubicTo(10.0f, 12.0f, 20.0f, 24.0f, 30.0f, 36.0f)
        }

        val iterator = path.iterator()
        // Swallow the move
        iterator.next()

        val segment = iterator.next()

        assertEquals(PathSegment.Type.Cubic, segment.type)
        val points = segment.points
        assertEquals(8, points.size)
        assertPointsEquals(Offset(4.0f, 6.0f), Offset(points[0], points[1]))
        assertPointsEquals(Offset(10.0f, 12.0f), Offset(points[2], points[3]))
        assertPointsEquals(Offset(20.0f, 24.0f), Offset(points[4], points[5]))
        assertPointsEquals(Offset(30.0f, 36.0f), Offset(points[6], points[7]))
        assertEquals(0.0f, segment.weight)
    }

    @Test
    fun conicTo() {
        if (Build.VERSION.SDK_INT >= 25) {
            val path = Path().apply {
                addRoundRect(RoundRect(12.0f, 12.0f, 24.0f, 24.0f, 8.0f, 8.0f))
            }

            val iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)
            // Swallow the move
            iterator.next()

            val segment = iterator.next()

            assertEquals(PathSegment.Type.Conic, segment.type)
            val points = segment.points
            assertEquals(6, points.size)

            assertPointsEquals(Offset(12.0f, 18.0f), Offset(points[0], points[1]))
            assertPointsEquals(Offset(12.0f, 24.0f), Offset(points[2], points[3]))
            assertPointsEquals(Offset(18.0f, 24.0f), Offset(points[4], points[5]))
            assertEquals(0.70710677f, segment.weight)
        }
    }

    @Test
    fun conicAsQuadratics() {
        val path = Path().apply {
            addRoundRect(RoundRect(12.0f, 12.0f, 24.0f, 24.0f, 8.0f, 8.0f))
        }

        for (segment in path) {
            if (segment.type == PathSegment.Type.Conic) fail("Found conic, none expected: $segment")
        }
    }

    @Test
    fun sizes() {
        val path = Path()
        var iterator = path.iterator()

        assertEquals(0, iterator.calculateSize())

        path.addRoundRect(RoundRect(12.0f, 12.0f, 64.0f, 64.0f, 8.0f, 8.0f))

        if (Build.VERSION.SDK_INT > 22) {
            // Preserve conics and count
            iterator = path.iterator(PathIterator.ConicEvaluation.AsConic)
            assertEquals(10, iterator.calculateSize())
            assertEquals(iterator.calculateSize(false), iterator.calculateSize())
        }

        // Convert conics and count
        iterator = path.iterator(PathIterator.ConicEvaluation.AsQuadratics)
        if (Build.VERSION.SDK_INT > 22) {
            // simple size, not including conic conversion
            assertEquals(10, iterator.calculateSize(false))
        } else {
            // round rects pre-API22 used line/quad/quad for each corner
            assertEquals(14, iterator.calculateSize(false))
        }
        // now get the size with converted conics
        assertEquals(14, iterator.calculateSize())
    }
}
