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
import androidx.compose.ui.geometry.RoundRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathDirectionTest {
    @Test
    fun directionForEmptyPath() {
        val path = Path()
        assertEquals(Path.Direction.Clockwise, path.computeDirection())
    }

    @Test
    fun directionForMoves() {
        val path = Path()
        path.moveTo(10.0f, 10.0f)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())

        path.moveTo(20.0f, 20.0f)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())
    }

    @Test
    fun directionForSingleLine() {
        val path = Path()
        path.moveTo(10.0f, 10.0f)
        path.lineTo(30.0f, 30.0f)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())
    }

    @Test
    fun directionForLines() {
        val path = Path()
        path.moveTo(0.0f, 0.0f)
        path.lineTo(30.0f, 30.0f)
        path.lineTo(60.0f, 0.0f)
        assertEquals(Path.Direction.CounterClockwise, path.computeDirection())

        path.rewind()

        path.moveTo(60.0f, 0.0f)
        path.lineTo(30.0f, 30.0f)
        path.lineTo(0.0f, 0.0f)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())
    }

    @Test
    fun directionForClosedLines() {
        val path = Path()
        path.addRect(Rect(10.0f, 10.0f, 40.0f, 40.0f), Path.Direction.Clockwise)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())

        path.rewind()

        path.addRect(Rect(100.0f, 10.0f, 140.0f, 40.0f), Path.Direction.CounterClockwise)

        assertEquals(Path.Direction.CounterClockwise, path.computeDirection())
    }

    @Test
    fun directionForMultipleContours() {
        val path = Path()
        path.addRect(Rect(10.0f, 10.0f, 40.0f, 40.0f), Path.Direction.Clockwise)

        path.addRect(Rect(100.0f, 10.0f, 140.0f, 40.0f), Path.Direction.CounterClockwise)

        assertEquals(Path.Direction.Clockwise, path.computeDirection())
    }

    @Test
    fun directionForQuadratics() {
        // Rounded rects use conics which are converted to quadratics
        val path = Path()
        path.addRoundRect(
            RoundRect(10.0f, 10.0f, 40.0f, 40.0f, 6.0f, 6.0f),
            Path.Direction.Clockwise
        )
        assertEquals(Path.Direction.Clockwise, path.computeDirection())

        path.rewind()

        path.addRoundRect(
            RoundRect(10.0f, 10.0f, 40.0f, 40.0f, 6.0f, 6.0f),
            Path.Direction.CounterClockwise
        )
        assertEquals(Path.Direction.CounterClockwise, path.computeDirection())
    }

    @Test
    fun directionForCubics() {
        val path = Path()
        path.addSvg(SvgShape.Cubics.pathData)
        assertEquals(Path.Direction.Clockwise, path.computeDirection())
    }
}
