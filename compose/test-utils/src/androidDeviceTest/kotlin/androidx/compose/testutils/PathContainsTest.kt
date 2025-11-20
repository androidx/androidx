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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathContainsTest {
    @Test
    fun testRectangleContains() {
        val errors = mutableListOf<String>()
        val path = Path()
        path.addOutline(
            RectangleShape.createOutline(Size(1f, 1f), LayoutDirection.Ltr, Density(1f))
        )
        fun assertContains(point: Offset, expected: Boolean) {
            if (path.contains(point) != expected) {
                val toBeOrNotToBe = if (expected) "be" else "not be"
                errors.add(
                    "Point(${point.x}, ${point.y}) should $toBeOrNotToBe within the Rect(0, 0, 1, 1)"
                )
            }
        }
        // Verify the corners
        assertContains(Offset(0f, 0f), true)
        assertContains(Offset(0f, 1f), true)
        assertContains(Offset(1f, 0f), true)
        assertContains(Offset(1f, 1f), true)
        // Verify just outside the corners
        assertContains(Offset(-.1f, -.1f), false)
        assertContains(Offset(0f, -.1f), false)
        assertContains(Offset(1f, -.1f), false)
        assertContains(Offset(1.1f, -.1f), false)
        assertContains(Offset(1.1f, 0f), false)
        assertContains(Offset(1.1f, 1f), false)
        assertContains(Offset(1.1f, 1.1f), false)
        assertContains(Offset(1f, 1.1f), false)
        assertContains(Offset(0f, 1.1f), false)
        assertContains(Offset(-.1f, 1.1f), false)
        assertContains(Offset(-.1f, 1f), false)
        assertContains(Offset(-.1f, 0f), false)

        if (errors.isNotEmpty()) {
            throw AssertionError("Found the following errors:\n${errors.joinToString("\n")}")
        }
    }

    @Test
    fun testCircleContains() {
        val path = Path()
        path.addOutline(CircleShape.createOutline(Size(50f, 50f), LayoutDirection.Ltr, Density(1f)))
        path.translate(Offset(25f, 25f))
        val errors = mutableListOf<String>()
        fun assertContains(point: Offset, expected: Boolean) {
            if (path.contains(point) != expected) {
                val toBeOrNotToBe = if (expected) "be" else "not be"
                errors.add(
                    "Point(${point.x}, ${point.y}) should $toBeOrNotToBe within the Circle(50x50)"
                )
            }
        }
        assertContains(Offset(25f, 25f), false)
        assertContains(Offset(50f, 25f), true)
        assertContains(Offset(75f, 25f), false)
        assertContains(Offset(25f, 50f), true)
        assertContains(Offset(50f, 50f), true)
        assertContains(Offset(75f, 50f), true)
        assertContains(Offset(25f, 75f), false)
        assertContains(Offset(50f, 75f), true)
        assertContains(Offset(75f, 75f), false)

        if (errors.isNotEmpty()) {
            throw AssertionError("Found the following errors:\n${errors.joinToString("\n")}")
        }
    }
}
