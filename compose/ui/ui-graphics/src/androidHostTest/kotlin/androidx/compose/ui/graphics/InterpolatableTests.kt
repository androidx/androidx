/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterpolatableTests {
    @Test
    fun testBothInterpolatableSameType() {
        val a1 = A(100f)
        val a2 = A(200f)
        val result = Interpolatable.lerp(a1, a2, 0.5f)
        assertIs<A>(result)
        assertEquals(150f, result.value)
    }

    @Test
    fun testLeftInterpolatableRightNot() {
        val a = A(100f)
        val x = X(200f)

        val result = Interpolatable.lerp(x, a, 0.51f)
        assertIs<A>(result) // right is chosen for > 0.5
        assertEquals(100f, result.value)

        val result2 = Interpolatable.lerp(x, a, 0.49f)
        assertIs<X>(result2) // left is chosen for < 0.5
        assertEquals(200f, result2.value)
    }

    @Test
    fun testLeftInterpolatableRightNotButConverted() {
        val a = A(100f)
        val x = Y(200f)
        val result = Interpolatable.lerp(a, x, 0.5f)
        assertIs<A>(result)
        assertEquals(150f, result.value)
    }

    @Test
    fun testRightInterpolatableLeftNot() {
        val x = X(100f)
        val a = A(200f)

        val result = Interpolatable.lerp(x, a, 0.51f)
        assertIs<A>(result) // right is chosen for > 0.5
        assertEquals(200f, result.value)

        val result2 = Interpolatable.lerp(x, a, 0.49f)
        assertIs<X>(result2) // left is chosen for < 0.5
        assertEquals(100f, result2.value)
    }

    @Test
    fun testRightInterpolatableLeftNotButConverted() {
        val y = Y(100f) // A knows how to convert Y
        val a = A(200f)
        val result = Interpolatable.lerp(y, a, 0.5f)
        assertIs<A>(result)
        assertEquals(150f, result.value)
    }

    @Test
    fun testLeftNull() {
        val left = null
        val right = A(100f)
        val result = Interpolatable.lerp(left, right, 0.5f)
        assertIs<A>(result)
        assertEquals(50f, result.value)
    }

    @Test
    fun testRightNull() {
        val left = A(100f)
        val right = null
        val result = Interpolatable.lerp(left, right, 0.5f)
        assertIs<A>(result)
        assertEquals(50f, result.value)
    }

    @Test
    fun testDifferentTypes() {
        val a = A(100f)
        val b = B(200f)
        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<A>(result)
        assertEquals(150f, result.value)
    }

    @Test
    fun testBothInterpolatableOnlyOneConverts() {
        val a = A(100f)
        val b = B(200f)

        val result = Interpolatable.lerp(a, b, 0.5f)
        assertIs<A>(result)
        assertEquals(150f, result.value)

        val result2 = Interpolatable.lerp(b, a, 0.5f)
        assertIs<A>(result2)
        assertEquals(150f, result2.value)
    }

    @Test
    fun testNeitherInterpolatable() {
        val a = X(100f)
        val b = X(200f)
        assertEquals(a, Interpolatable.lerp(a, b, 0.4f))
        assertEquals(b, Interpolatable.lerp(a, b, 0.6f))
    }

    @Test
    fun testSameObject() {
        val a = A(100f)
        assertEquals(a, Interpolatable.lerp(a, a, 0.4f))
        assertEquals(a, Interpolatable.lerp(a, a, 0.6f))
    }

    private data class A(val value: Float) : Interpolatable {
        override fun lerp(other: Any?, t: Float): Any? {
            if (other == null) {
                return A(lerp(0f, value, t))
            }
            if (other is Y) {
                return A(lerp(value, other.value, t))
            }
            if (other is A) {
                return A(lerp(value, other.value, t))
            }
            if (other is B) {
                return A(lerp(value, other.value, t))
            }
            return null
        }
    }

    private data class B(val value: Float) : Interpolatable {
        override fun lerp(other: Any?, t: Float): Any? {
            if (other == null) {
                return B(lerp(0f, value, t))
            }
            return null
        }
    }

    private data class X(val value: Float)

    private data class Y(val value: Float)
}
