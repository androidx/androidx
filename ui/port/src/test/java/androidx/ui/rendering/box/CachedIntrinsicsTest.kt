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
        private const val DELTA = 0.01f
    }

    private class RenderTestBox : RenderBox() {
        var value: Float = 0.0f

        fun next(): Float {
            value += 1.0f; return value; }

        override fun computeMinIntrinsicWidth(height: Float) = next()
        override fun computeMaxIntrinsicWidth(height: Float) = next()
        override fun computeMinIntrinsicHeight(width: Float) = next()
        override fun computeMaxIntrinsicHeight(width: Float) = next()
    }

    @Test
    fun `Intrinsics cache`() {
        val test = RenderTestBox()

        assertEquals(1.0f, test.getMinIntrinsicWidth(0.0f), DELTA)
        assertEquals(2.0f, test.getMinIntrinsicWidth(100.0f), DELTA)
        assertEquals(3.0f, test.getMinIntrinsicWidth(200.0f), DELTA)
        assertEquals(1.0f, test.getMinIntrinsicWidth(0.0f), DELTA)
        assertEquals(2.0f, test.getMinIntrinsicWidth(100.0f), DELTA)
        assertEquals(3.0f, test.getMinIntrinsicWidth(200.0f), DELTA)

        assertEquals(4.0f, test.getMaxIntrinsicWidth(0.0f), DELTA)
        assertEquals(5.0f, test.getMaxIntrinsicWidth(100.0f), DELTA)
        assertEquals(6.0f, test.getMaxIntrinsicWidth(200.0f), DELTA)
        assertEquals(4.0f, test.getMaxIntrinsicWidth(0.0f), DELTA)
        assertEquals(5.0f, test.getMaxIntrinsicWidth(100.0f), DELTA)
        assertEquals(6.0f, test.getMaxIntrinsicWidth(200.0f), DELTA)

        assertEquals(7.0f, test.getMinIntrinsicHeight(0.0f), DELTA)
        assertEquals(8.0f, test.getMinIntrinsicHeight(100.0f), DELTA)
        assertEquals(9.0f, test.getMinIntrinsicHeight(200.0f), DELTA)
        assertEquals(7.0f, test.getMinIntrinsicHeight(0.0f), DELTA)
        assertEquals(8.0f, test.getMinIntrinsicHeight(100.0f), DELTA)
        assertEquals(9.0f, test.getMinIntrinsicHeight(200.0f), DELTA)

        assertEquals(10.0f, test.getMaxIntrinsicHeight(0.0f), DELTA)
        assertEquals(11.0f, test.getMaxIntrinsicHeight(100.0f), DELTA)
        assertEquals(12.0f, test.getMaxIntrinsicHeight(200.0f), DELTA)
        assertEquals(10.0f, test.getMaxIntrinsicHeight(0.0f), DELTA)
        assertEquals(11.0f, test.getMaxIntrinsicHeight(100.0f), DELTA)
        assertEquals(12.0f, test.getMaxIntrinsicHeight(200.0f), DELTA)

        // now read them all again backwards
        assertEquals(12.0f, test.getMaxIntrinsicHeight(200.0f), DELTA)
        assertEquals(11.0f, test.getMaxIntrinsicHeight(100.0f), DELTA)
        assertEquals(10.0f, test.getMaxIntrinsicHeight(0.0f), DELTA)
        assertEquals(9.0f, test.getMinIntrinsicHeight(200.0f), DELTA)
        assertEquals(8.0f, test.getMinIntrinsicHeight(100.0f), DELTA)
        assertEquals(7.0f, test.getMinIntrinsicHeight(0.0f), DELTA)
        assertEquals(6.0f, test.getMaxIntrinsicWidth(200.0f), DELTA)
        assertEquals(5.0f, test.getMaxIntrinsicWidth(100.0f), DELTA)
        assertEquals(4.0f, test.getMaxIntrinsicWidth(0.0f), DELTA)
        assertEquals(3.0f, test.getMinIntrinsicWidth(200.0f), DELTA)
        assertEquals(2.0f, test.getMinIntrinsicWidth(100.0f), DELTA)
        assertEquals(1.0f, test.getMinIntrinsicWidth(0.0f), DELTA)
    }
}