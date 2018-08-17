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

package androidx.ui.rendering.box

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CachedIntrinsicsTest {

    companion object {
        private const val DELTA = 0.01
    }

    private class RenderTestBox : RenderBox() {
        var value: Double = 0.0

        fun next(): Double {
            value += 1.0; return value; }

        override fun computeMinIntrinsicWidth(height: Double) = next()
        override fun computeMaxIntrinsicWidth(height: Double) = next()
        override fun computeMinIntrinsicHeight(width: Double) = next()
        override fun computeMaxIntrinsicHeight(width: Double) = next()
    }

    @Test
    fun `Intrinsics cache`() {
        val test = RenderTestBox()

        assertEquals(1.0, test.getMinIntrinsicWidth(0.0), DELTA)
        assertEquals(2.0, test.getMinIntrinsicWidth(100.0), DELTA)
        assertEquals(3.0, test.getMinIntrinsicWidth(200.0), DELTA)
        assertEquals(1.0, test.getMinIntrinsicWidth(0.0), DELTA)
        assertEquals(2.0, test.getMinIntrinsicWidth(100.0), DELTA)
        assertEquals(3.0, test.getMinIntrinsicWidth(200.0), DELTA)

        assertEquals(4.0, test.getMaxIntrinsicWidth(0.0), DELTA)
        assertEquals(5.0, test.getMaxIntrinsicWidth(100.0), DELTA)
        assertEquals(6.0, test.getMaxIntrinsicWidth(200.0), DELTA)
        assertEquals(4.0, test.getMaxIntrinsicWidth(0.0), DELTA)
        assertEquals(5.0, test.getMaxIntrinsicWidth(100.0), DELTA)
        assertEquals(6.0, test.getMaxIntrinsicWidth(200.0), DELTA)

        assertEquals(7.0, test.getMinIntrinsicHeight(0.0), DELTA)
        assertEquals(8.0, test.getMinIntrinsicHeight(100.0), DELTA)
        assertEquals(9.0, test.getMinIntrinsicHeight(200.0), DELTA)
        assertEquals(7.0, test.getMinIntrinsicHeight(0.0), DELTA)
        assertEquals(8.0, test.getMinIntrinsicHeight(100.0), DELTA)
        assertEquals(9.0, test.getMinIntrinsicHeight(200.0), DELTA)

        assertEquals(10.0, test.getMaxIntrinsicHeight(0.0), DELTA)
        assertEquals(11.0, test.getMaxIntrinsicHeight(100.0), DELTA)
        assertEquals(12.0, test.getMaxIntrinsicHeight(200.0), DELTA)
        assertEquals(10.0, test.getMaxIntrinsicHeight(0.0), DELTA)
        assertEquals(11.0, test.getMaxIntrinsicHeight(100.0), DELTA)
        assertEquals(12.0, test.getMaxIntrinsicHeight(200.0), DELTA)

        // now read them all again backwards
        assertEquals(12.0, test.getMaxIntrinsicHeight(200.0), DELTA)
        assertEquals(11.0, test.getMaxIntrinsicHeight(100.0), DELTA)
        assertEquals(10.0, test.getMaxIntrinsicHeight(0.0), DELTA)
        assertEquals(9.0, test.getMinIntrinsicHeight(200.0), DELTA)
        assertEquals(8.0, test.getMinIntrinsicHeight(100.0), DELTA)
        assertEquals(7.0, test.getMinIntrinsicHeight(0.0), DELTA)
        assertEquals(6.0, test.getMaxIntrinsicWidth(200.0), DELTA)
        assertEquals(5.0, test.getMaxIntrinsicWidth(100.0), DELTA)
        assertEquals(4.0, test.getMaxIntrinsicWidth(0.0), DELTA)
        assertEquals(3.0, test.getMinIntrinsicWidth(200.0), DELTA)
        assertEquals(2.0, test.getMinIntrinsicWidth(100.0), DELTA)
        assertEquals(1.0, test.getMinIntrinsicWidth(0.0), DELTA)
    }
}