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

package androidx.window.embedding

import android.graphics.Color
import androidx.window.embedding.DividerAttributes.DragRange.Companion.DRAG_RANGE_SYSTEM_DEFAULT
import androidx.window.embedding.DividerAttributes.DragRange.SplitRatioDragRange
import androidx.window.embedding.DividerAttributes.DraggableDividerAttributes
import androidx.window.embedding.DividerAttributes.FixedDividerAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Test class to verify [DividerAttributes] */
class DividerAttributesTest {

    @Test
    fun testDividerAttributesEquals() {
        val attrs1 = FixedDividerAttributes.Builder().build()
        val attrs2 = DraggableDividerAttributes.Builder().build()
        val attrs3 =
            DraggableDividerAttributes.Builder()
                .setWidthDp(20)
                .setDragRange(SplitRatioDragRange(0.3f, 0.7f))
                .build()
        val attrs4 =
            DraggableDividerAttributes.Builder()
                .setWidthDp(DividerAttributes.WIDTH_SYSTEM_DEFAULT)
                .setDragRange(DRAG_RANGE_SYSTEM_DEFAULT)
                .build()

        assertNotEquals(attrs1, attrs2)
        assertNotEquals(attrs1.hashCode(), attrs2.hashCode())

        assertNotEquals(attrs2, attrs3)
        assertNotEquals(attrs2.hashCode(), attrs3.hashCode())

        assertNotEquals(attrs3, attrs4)
        assertNotEquals(attrs3.hashCode(), attrs4.hashCode())

        // attrs2 and attrs4 must be equal because attrs4 uses default values.
        assertEquals(attrs2, attrs4)
        assertEquals(attrs2.hashCode(), attrs4.hashCode())
    }

    @Test
    fun testBuilder() {
        val attrs =
            DraggableDividerAttributes.Builder()
                .setWidthDp(20)
                .setDragRange(SplitRatioDragRange(0.3f, 0.7f))
                .setColor(Color.GRAY)
                .build()

        assertEquals(20, attrs.widthDp)
        assertEquals(SplitRatioDragRange(0.3f, 0.7f), attrs.dragRange)
        assertEquals(Color.GRAY, attrs.color)
    }

    @Test
    fun testBuilder_defaultValues() {
        val attrs = DraggableDividerAttributes.Builder().build()

        assertEquals(DividerAttributes.WIDTH_SYSTEM_DEFAULT, attrs.widthDp)
        assertEquals(DRAG_RANGE_SYSTEM_DEFAULT, attrs.dragRange)
        assertEquals(Color.BLACK, attrs.color)
    }

    @Test
    fun testSplitRatioDragRange_validation() {
        // Valid range
        SplitRatioDragRange(minRatio = 0.2f, maxRatio = 0.8f)

        // Invalid minRatio and maxRatio
        assertThrows(IllegalArgumentException::class.java) {
            SplitRatioDragRange(minRatio = 0.0f, maxRatio = 1.0f)
        }

        // Invalid minRatio
        assertThrows(IllegalArgumentException::class.java) {
            SplitRatioDragRange(minRatio = -1.0f, maxRatio = 0.8f)
        }

        // Invalid maxRatio
        assertThrows(IllegalArgumentException::class.java) {
            SplitRatioDragRange(minRatio = 0.2f, maxRatio = 1.2f)
        }

        // minRatio should not be less than maxRatio
        assertThrows(IllegalArgumentException::class.java) {
            SplitRatioDragRange(minRatio = 0.6f, maxRatio = 0.4f)
        }
    }
}
