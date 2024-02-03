/*
 * Copyright (C) 2019 The Android Open Source Project
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
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/* ktlint-disable max-line-length */
@RunWith(AndroidJUnit4::class)
@SmallTest
class PathSvgTest {
    @Test
    fun emptyPath() {
        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 0.0 0.0">
            </svg>

            """.trimIndent(),
            Path().toSvg(asDocument = true)
        )

        assertTrue(Path().toSvg().isEmpty())
    }

    // API 21 has a different behavior for the bounds of an move only path
    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun singleMove() {
        val svg = Path().apply { moveTo(10.0f, 10.0f) }.toSvg(asDocument = true)
        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="10.0 10.0 0.0 0.0">
              <path d="M10.0 10.0"/>
            </svg>

            """.trimIndent(),
            svg
        )
    }

    @Test
    fun twoPaths() {
        val svg = Path().apply {
            addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }.toSvg(asDocument = true)

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 50.0 50.0">
              <path d="M0.0 0.0L10.0 0.0 10.0 10.0 0.0 10.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z"/>
            </svg>

            """.trimIndent(),
            svg
        )
    }

    @Test
    fun bezier() {
        val svg = Path().apply {
            moveTo(10.0f, 10.0f)
            cubicTo(20.0f, 20.0f, 30.0f, 30.0f, 40.0f, 40.0f)
            quadraticTo(50.0f, 50.0f, 60.0f, 60.0f)
        }.toSvg(asDocument = true)

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="10.0 10.0 50.0 50.0">
              <path d="M10.0 10.0C20.0 20.0 30.0 30.0 40.0 40.0Q50.0 50.0 60.0 60.0"/>
            </svg>

            """.trimIndent(),
            svg
        )
    }

    @Test
    fun dataOnly() {
        val svg = Path().apply {
            addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }.toSvg()

        assertEquals(
            "M0.0 0.0L10.0 0.0 10.0 10.0 0.0 10.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z",
            svg
        )
    }

    @Test
    fun hole() {
        val hole = Path().apply {
            addRect(Rect(0.0f, 0.0f, 80.0f, 80.0f), Path.Direction.Clockwise)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 80.0 80.0">
              <path d="M0.0 0.0L80.0 0.0 80.0 80.0 0.0 80.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z"/>
            </svg>

            """.trimIndent(),
            hole.toSvg(asDocument = true)
        )

        hole.fillType = PathFillType.EvenOdd

        assertEquals(
            """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0.0 0.0 80.0 80.0">
              <path fill-rule="evenodd" d="M0.0 0.0L80.0 0.0 80.0 80.0 0.0 80.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z"/>
            </svg>

            """.trimIndent(),
            hole.toSvg(asDocument = true)
        )
    }

    @Test
    fun addSvg() {
        val twoRects = Path().apply {
            addSvg("M0.0 0.0L10.0 0.0 10.0 10.0 0.0 10.0ZM20.0 20.0L50.0 20.0 50.0 50.0 20.0 50.0Z")
        }
        val reference = Path().apply {
            addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }
        assertPathEquals(reference, twoRects)
    }

    @Test(IllegalArgumentException::class)
    fun addInvalidSvg() {
        Path().apply {
            // 'K' is an invalid SVG instruction
            addSvg("M0.0 0.0K10.0 0.0 10.0 10.0 0.0 10.0Z")
        }
    }

    @Test
    fun roundTrip() {
        val original = Path().apply {
            addRect(Rect(0.0f, 0.0f, 10.0f, 10.0f), Path.Direction.Clockwise)
            addRect(Rect(20.0f, 20.0f, 50.0f, 50.0f), Path.Direction.Clockwise)
        }
        val svg = original.toSvg()
        val path = Path().apply { addSvg(svg) }

        assertPathEquals(original, path)
    }
}
/* ktlint-enable max-line-length */

private fun assertPathEquals(a: Path, b: Path) {
    val ita = a.iterator()
    val itb = b.iterator()

    while (ita.hasNext()) {
        assertTrue(itb.hasNext())
        assertEquals(ita.next(), itb.next())
    }

    assertFalse(itb.hasNext())
}
