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
        private const val DELTA = 0.01f
    }

    @Test
    fun `BoxConstraints toString`() {
        assertTrue(BoxConstraints.expand().toString().contains("biggest"))
        assertTrue(BoxConstraints().toString().contains("unconstrained"))
        assertTrue(BoxConstraints.tightFor(width = 50.0f).toString().contains("w=50"))
    }

    @Test
    fun `BoxConstraints copyWith`() {
        val constraints = BoxConstraints(
                minWidth = 3.0f,
                maxWidth = 7.0f,
                minHeight = 11.0f,
                maxHeight = 17.0f
        )
        var copy = constraints.copyWith()
        assertEquals(constraints, copy)
        copy = constraints.copyWith(
                minWidth = 13.0f,
                maxWidth = 17.0f,
                minHeight = 111.0f,
                maxHeight = 117.0f
        )
        assertEquals(13.0f, copy.minWidth, DELTA)
        assertEquals(17.0f, copy.maxWidth, DELTA)
        assertEquals(111.0f, copy.minHeight, DELTA)
        assertEquals(117.0f, copy.maxHeight, DELTA)
        assertNotEquals(constraints, copy)
        assertNotEquals(constraints.hashCode(), copy.hashCode())
    }

    @Test
    fun `BoxConstraints operators`() {
        val constraints = BoxConstraints(
                minWidth = 3.0f,
                maxWidth = 7.0f,
                minHeight = 11.0f,
                maxHeight = 17.0f
        )
        var copy = constraints * 2.0f
        assertEquals(6.0f, copy.minWidth, DELTA)
        assertEquals(14.0f, copy.maxWidth, DELTA)
        assertEquals(22.0f, copy.minHeight, DELTA)
        assertEquals(34.0f, copy.maxHeight, DELTA)
        assertEquals(constraints, copy / 2.0f)
        copy = constraints.truncDiv(2.0f)
        assertEquals(1.0f, copy.minWidth, DELTA)
        assertEquals(3.0f, copy.maxWidth, DELTA)
        assertEquals(5.0f, copy.minHeight, DELTA)
        assertEquals(8.0f, copy.maxHeight, DELTA)
        copy = constraints % 3.0f
        assertEquals(0.0f, copy.minWidth, DELTA)
        assertEquals(1.0f, copy.maxWidth, DELTA)
        assertEquals(2.0f, copy.minHeight, DELTA)
        assertEquals(2.0f, copy.maxHeight, DELTA)
    }

    @Test
    fun `BoxConstraints lerp`() {
        assertNull(BoxConstraints.lerp(null, null, 0.5f))
        val constraints = BoxConstraints(
                minWidth = 3.0f,
                maxWidth = 7.0f,
                minHeight = 11.0f,
                maxHeight = 17.0f
        )
        var copy = BoxConstraints.lerp(null, constraints, 0.5f)!!
        assertEquals(1.5f, copy.minWidth, DELTA)
        assertEquals(3.5f, copy.maxWidth, DELTA)
        assertEquals(5.5f, copy.minHeight, DELTA)
        assertEquals(8.5f, copy.maxHeight, DELTA)
        copy = BoxConstraints.lerp(constraints, null, 0.5f)!!
        assertEquals(1.5f, copy.minWidth, DELTA)
        assertEquals(3.5f, copy.maxWidth, DELTA)
        assertEquals(5.5f, copy.minHeight, DELTA)
        assertEquals(8.5f, copy.maxHeight, DELTA)
        copy = BoxConstraints.lerp(BoxConstraints(
                minWidth = 13.0f,
                maxWidth = 17.0f,
                minHeight = 111.0f,
                maxHeight = 117.0f
        ), constraints, 0.2f)!!
        assertEquals(11.0f, copy.minWidth, DELTA)
        assertEquals(15.0f, copy.maxWidth, DELTA)
        assertEquals(91.0f, copy.minHeight, DELTA)
        assertEquals(97.0f, copy.maxHeight, DELTA)
    }

    @Test
    fun `BoxConstraints lerp with unbounded width`() {
        val constraints1 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = 10.0f,
                maxHeight = 20.0f
        )
        val constraints2 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = 20.0f,
                maxHeight = 30.0f
        )
        val constraints3 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = 15.0f,
                maxHeight = 25.0f
        )
        assertEquals(BoxConstraints.lerp(constraints1, constraints2, 0.5f), constraints3)
    }

    @Test
    fun `BoxConstraints lerp with unbounded height`() {
        val constraints1 = BoxConstraints(
                minWidth = 10.0f,
                maxWidth = 20.0f,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        val constraints2 = BoxConstraints(
                minWidth = 20.0f,
                maxWidth = 30.0f,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        val constraints3 = BoxConstraints(
                minWidth = 15.0f,
                maxWidth = 25.0f,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        assertEquals(BoxConstraints.lerp(constraints1, constraints2, 0.5f), constraints3)
    }

    @Test(expected = AssertionError::class)
    fun `BoxConstraints lerp from bounded to unbounded 1`() {
        val constraints1 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        val constraints2 = BoxConstraints(
                minWidth = 20.0f,
                maxWidth = 30.0f,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        BoxConstraints.lerp(constraints1, constraints2, 0.5f)
    }

    @Test(expected = AssertionError::class)
    fun `BoxConstraints lerp from bounded to unbounded 2`() {
        val constraints1 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        val constraints3 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = 20.0f,
                maxHeight = 30.0f
        )
        BoxConstraints.lerp(constraints1, constraints3, 0.5f)
    }

    @Test(expected = AssertionError::class)
    fun `BoxConstraints lerp from bounded to unbounded 3`() {
        val constraints2 = BoxConstraints(
                minWidth = 20.0f,
                maxWidth = 30.0f,
                minHeight = Float.POSITIVE_INFINITY,
                maxHeight = Float.POSITIVE_INFINITY
        )
        val constraints3 = BoxConstraints(
                minWidth = Float.POSITIVE_INFINITY,
                maxWidth = Float.POSITIVE_INFINITY,
                minHeight = 20.0f,
                maxHeight = 30.0f
        )
        BoxConstraints.lerp(constraints2, constraints3, 0.5f)
    }

    @Test
    fun `BoxConstraints normalize`() {
        val constraints = BoxConstraints(
                minWidth = 3.0f,
                maxWidth = 2.0f,
                minHeight = 11.0f,
                maxHeight = 18.0f
        )
        val copy = constraints.normalize()
        assertEquals(3.0f, copy.minWidth, DELTA)
        assertEquals(3.0f, copy.maxWidth, DELTA)
        assertEquals(11.0f, copy.minHeight, DELTA)
        assertEquals(18.0f, copy.maxHeight, DELTA)
    }
}
