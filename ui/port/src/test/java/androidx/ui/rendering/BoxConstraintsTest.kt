/*
 * Copyright 2018 The Android Open Source Project
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

/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering

import androidx.ui.rendering.box.BoxConstraints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BoxConstraintsTest {

    companion object {
        private const val DELTA = 0.01
    }

    @Test
    fun `BoxConstraints toString`() {
        assertTrue(BoxConstraints.expand().toString().contains("biggest"))
        assertTrue(BoxConstraints().toString().contains("unconstrained"))
        assertTrue(BoxConstraints.tightFor(width = 50.0).toString().contains("w=50"))
    }

    @Test
    fun `BoxConstraints copyWith`() {
        val constraints = BoxConstraints(
                minWidth = 3.0,
                maxWidth = 7.0,
                minHeight = 11.0,
                maxHeight = 17.0
        )
        var copy = constraints.copyWith()
        assertEquals(constraints, copy)
        copy = constraints.copyWith(
                minWidth = 13.0,
                maxWidth = 17.0,
                minHeight = 111.0,
                maxHeight = 117.0
        )
        assertEquals(13.0, copy.minWidth, DELTA)
        assertEquals(17.0, copy.maxWidth, DELTA)
        assertEquals(111.0, copy.minHeight, DELTA)
        assertEquals(117.0, copy.maxHeight, DELTA)
        assertNotEquals(constraints, copy)
        assertNotEquals(constraints.hashCode(), copy.hashCode())
    }

    @Test
    fun `BoxConstraints operators`() {
        val constraints = BoxConstraints(
                minWidth = 3.0,
                maxWidth = 7.0,
                minHeight = 11.0,
                maxHeight = 17.0
        )
        var copy = constraints * 2.0
        assertEquals(6.0, copy.minWidth, DELTA)
        assertEquals(14.0, copy.maxWidth, DELTA)
        assertEquals(22.0, copy.minHeight, DELTA)
        assertEquals(34.0, copy.maxHeight, DELTA)
        assertEquals(constraints, copy / 2.0)
        copy = constraints.truncDiv(2.0)
        assertEquals(1.0, copy.minWidth, DELTA)
        assertEquals(3.0, copy.maxWidth, DELTA)
        assertEquals(5.0, copy.minHeight, DELTA)
        assertEquals(8.0, copy.maxHeight, DELTA)
        copy = constraints % 3.0
        assertEquals(0.0, copy.minWidth, DELTA)
        assertEquals(1.0, copy.maxWidth, DELTA)
        assertEquals(2.0, copy.minHeight, DELTA)
        assertEquals(2.0, copy.maxHeight, DELTA)
    }

    @Test
    fun `BoxConstraints lerp`() {
        assertNull(BoxConstraints.lerp(null, null, 0.5))
        val constraints = BoxConstraints(
                minWidth = 3.0,
                maxWidth = 7.0,
                minHeight = 11.0,
                maxHeight = 17.0
        )
        var copy = BoxConstraints.lerp(null, constraints, 0.5)!!
        assertEquals(1.5, copy.minWidth, DELTA)
        assertEquals(3.5, copy.maxWidth, DELTA)
        assertEquals(5.5, copy.minHeight, DELTA)
        assertEquals(8.5, copy.maxHeight, DELTA)
        copy = BoxConstraints.lerp(constraints, null, 0.5)!!
        assertEquals(1.5, copy.minWidth, DELTA)
        assertEquals(3.5, copy.maxWidth, DELTA)
        assertEquals(5.5, copy.minHeight, DELTA)
        assertEquals(8.5, copy.maxHeight, DELTA)
        copy = BoxConstraints.lerp(BoxConstraints(
                minWidth = 13.0,
                maxWidth = 17.0,
                minHeight = 111.0,
                maxHeight = 117.0
        ), constraints, 0.2)!!
        assertEquals(11.0, copy.minWidth, DELTA)
        assertEquals(15.0, copy.maxWidth, DELTA)
        assertEquals(91.0, copy.minHeight, DELTA)
        assertEquals(97.0, copy.maxHeight, DELTA)
    }

    @Test
    fun `BoxConstraints lerp with unbounded width`() {
        val constraints1 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = 10.0,
                maxHeight = 20.0
        )
        val constraints2 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = 20.0,
                maxHeight = 30.0
        )
        val constraints3 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = 15.0,
                maxHeight = 25.0
        )
        assertEquals(BoxConstraints.lerp(constraints1, constraints2, 0.5), constraints3)
    }

    @Test
    fun `BoxConstraints lerp with unbounded height`() {
        val constraints1 = BoxConstraints(
                minWidth = 10.0,
                maxWidth = 20.0,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        val constraints2 = BoxConstraints(
                minWidth = 20.0,
                maxWidth = 30.0,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        val constraints3 = BoxConstraints(
                minWidth = 15.0,
                maxWidth = 25.0,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        assertEquals(BoxConstraints.lerp(constraints1, constraints2, 0.5), constraints3)
    }

    @Test(expected = AssertionError::class)
    fun `BoxConstraints lerp from bounded to unbounded 1`() {
        val constraints1 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        val constraints2 = BoxConstraints(
                minWidth = 20.0,
                maxWidth = 30.0,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        BoxConstraints.lerp(constraints1, constraints2, 0.5)
    }

    @Test(expected = AssertionError::class)
    fun `BoxConstraints lerp from bounded to unbounded 2`() {
        val constraints1 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        val constraints3 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = 20.0,
                maxHeight = 30.0
        )
        BoxConstraints.lerp(constraints1, constraints3, 0.5)
    }

    @Test(expected = AssertionError::class)
    fun `BoxConstraints lerp from bounded to unbounded 3`() {
        val constraints2 = BoxConstraints(
                minWidth = 20.0,
                maxWidth = 30.0,
                minHeight = Double.POSITIVE_INFINITY,
                maxHeight = Double.POSITIVE_INFINITY
        )
        val constraints3 = BoxConstraints(
                minWidth = Double.POSITIVE_INFINITY,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = 20.0,
                maxHeight = 30.0
        )
        BoxConstraints.lerp(constraints2, constraints3, 0.5)
    }

    @Test
    fun `BoxConstraints normalize`() {
        val constraints = BoxConstraints(
                minWidth = 3.0,
                maxWidth = 2.0,
                minHeight = 11.0,
                maxHeight = 18.0
        )
        val copy = constraints.normalize()
        assertEquals(3.0, copy.minWidth, DELTA)
        assertEquals(3.0, copy.maxWidth, DELTA)
        assertEquals(11.0, copy.minHeight, DELTA)
        assertEquals(18.0, copy.maxHeight, DELTA)
    }
}
