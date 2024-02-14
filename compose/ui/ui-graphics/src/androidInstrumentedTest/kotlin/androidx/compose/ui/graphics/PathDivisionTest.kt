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

import androidx.compose.ui.geometry.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathDivisionTest {
    @Test
    fun emptyPath() {
        val paths = Path().divide()
        assertEquals(0, paths.size)
    }

    @Test
    fun singleMove() {
        val paths = Path().apply { moveTo(10.0f, 10.0f) }.divide()
        assertEquals(0, paths.size)
    }

    @Test
    fun divide() {
        val paths = Path().apply {
            addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }.divide()

        assertEquals(2, paths.size)

        val sourcePaths = listOf(
            Path().apply {
                addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            },
            Path().apply {
                addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
            }
        )

        val points1 = FloatArray(8)
        val points2 = FloatArray(8)

        for (i in paths.indices) {
            val path1 = sourcePaths[i]
            val path2 = paths[i]

            assertPathEquals(path1, path2, points1, points2)
        }
    }

    @Test
    fun divideWithEmptyContour() {
        val paths = Path().apply {
            addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            moveTo(10.0f, 10.0f)
            moveTo(100.0f, 100.0f)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }.divide()

        assertEquals(2, paths.size)
    }
}

private fun valueCountForType(type: PathSegment.Type) = when (type) {
    PathSegment.Type.Move -> 2
    PathSegment.Type.Line -> 4
    PathSegment.Type.Quadratic -> 6
    PathSegment.Type.Conic -> 8
    PathSegment.Type.Cubic -> 8
    PathSegment.Type.Close -> 0
    PathSegment.Type.Done -> 0
}

private fun assertPathEquals(
    expected: Path,
    actual: Path,
    points1: FloatArray = FloatArray(8),
    points2: FloatArray = FloatArray(8)
) {
    val iterator1 = expected.iterator()
    val iterator2 = actual.iterator()

    assertEquals(iterator1.calculateSize(), iterator2.calculateSize())

    while (iterator1.hasNext() && iterator2.hasNext()) {
        val type1 = iterator1.next(points1)
        val type2 = iterator2.next(points2)

        points1.fill(0.0f, valueCountForType(type1))
        points2.fill(0.0f, valueCountForType(type2))

        assertEquals(type1, type2)

        assertArrayEquals(points1, points2, 1e-7f)
    }
}
