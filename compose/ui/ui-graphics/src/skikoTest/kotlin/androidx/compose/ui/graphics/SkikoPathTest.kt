/*
 * Copyright 2026 The Android Open Source Project
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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.skia.PathBuilder as SkPathBuilder

class SkikoPathTest {

    @Test
    fun asSkiaPath_observesSubsequentComposeMutations() {
        val path = Path().apply {
            moveTo(10f, 20f)
            lineTo(30f, 40f)
        }

        val retainedPath = path.asSkiaPath()

        path.lineTo(50f, 60f)

        val expected = Path().apply {
            moveTo(10f, 20f)
            lineTo(30f, 40f)
            lineTo(50f, 60f)
        }

        assertEquals(expected.toSvg(), path.toSvg())
        assertContentEquals(expected.asSkiaPath().serializeToBytes(), retainedPath.serializeToBytes())
    }

    @Test
    fun asComposePath_marksOriginalSkiaPathAsObserved() {
        val skiaPath = SkPathBuilder()
            .moveTo(10f, 20f)
            .lineTo(30f, 40f)
            .snapshot()
        val composePath = skiaPath.asComposePath()

        composePath.lineTo(50f, 60f)

        val expected = Path().apply {
            moveTo(10f, 20f)
            lineTo(30f, 40f)
            lineTo(50f, 60f)
        }

        assertEquals(expected.toSvg(), composePath.toSvg())
        assertContentEquals(expected.asSkiaPath().serializeToBytes(), skiaPath.serializeToBytes())
    }

    @Test
    fun isEmpty() {
        val path = Path()
        assertTrue(path.isEmpty)

        path.addRect(Rect(0f, 0f, 16f, 16f))

        assertFalse(path.isEmpty)
    }

    @Test
    fun isConvex() {
        val path = Path()
        assertTrue(path.isConvex)

        path.addRect(Rect(0f, 0f, 8f, 8f))
        assertTrue(path.isConvex)

        path.addRect(Rect(8f, 8f, 16f, 16f))
        assertFalse(path.isConvex)
    }

    @Test
    fun getBounds() {
        val path = Path()
        assertEquals(Rect(0f, 0f, 0f, 0f), path.getBounds())

        path.addRect(Rect(0f, 0f, 8f, 8f))
        assertEquals(Rect(0f, 0f, 8f, 8f), path.getBounds())

        path.addRect(Rect(8f, 8f, 16f, 16f))
        assertEquals(Rect(0f, 0f, 16f, 16f), path.getBounds())
    }

    @Test
    fun initialParameters() {
        val path = Path()

        assertEquals(PathFillType.NonZero, path.fillType)
    }

    @Test
    fun resetPreservesFillType() {
        val path = Path()

        path.fillType = PathFillType.EvenOdd
        path.reset()

        assertEquals(PathFillType.EvenOdd, path.fillType)
    }

    @Test
    fun rewindClearsPath() {
        val path = Path().apply {
            addRect(Rect(0f, 0f, 100f, 200f))
        }
        assertFalse(path.isEmpty)

        path.rewind()

        assertTrue(path.isEmpty)
    }
}
