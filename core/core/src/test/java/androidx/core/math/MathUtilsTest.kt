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
package androidx.core.math

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MathUtilsTest {

    @Test
    fun testAddExact() {
        // zero + zero
        Assert.assertEquals(0, MathUtils.addExact(0, 0).toLong())
        Assert.assertEquals(0L, MathUtils.addExact(0L, 0L))
        // positive + positive
        Assert.assertEquals(2, MathUtils.addExact(1, 1).toLong())
        Assert.assertEquals(2L, MathUtils.addExact(1L, 1L))
        // negative + negative
        Assert.assertEquals(-2, MathUtils.addExact(-1, -1).toLong())
        Assert.assertEquals(-2L, MathUtils.addExact(-1L, -1L))
        // positive + negative
        Assert.assertEquals(0, MathUtils.addExact(1, -1).toLong())
        Assert.assertEquals(0L, MathUtils.addExact(1L, -1L))
        Assert.assertEquals(-1, MathUtils.addExact(1, -2).toLong())
        Assert.assertEquals(-1L, MathUtils.addExact(1L, -2L))
        Assert.assertEquals(1, MathUtils.addExact(2, -1).toLong())
        Assert.assertEquals(1L, MathUtils.addExact(2L, -1L))
        // negative + positive
        Assert.assertEquals(0, MathUtils.addExact(-1, 1).toLong())
        Assert.assertEquals(0L, MathUtils.addExact(-1L, 1L))
        Assert.assertEquals(1, MathUtils.addExact(-1, 2).toLong())
        Assert.assertEquals(1L, MathUtils.addExact(-1L, 2L))
        Assert.assertEquals(-1, MathUtils.addExact(-2, 1).toLong())
        Assert.assertEquals(-1L, MathUtils.addExact(-2L, 1L))
        // zero + positive, positive + zero
        Assert.assertEquals(1, MathUtils.addExact(0, 1).toLong())
        Assert.assertEquals(1L, MathUtils.addExact(0L, 1L))
        Assert.assertEquals(1, MathUtils.addExact(1, 0).toLong())
        Assert.assertEquals(1L, MathUtils.addExact(1L, 0L))
        // zero + negative, negative + zero
        Assert.assertEquals(-1, MathUtils.addExact(0, -1).toLong())
        Assert.assertEquals(-1L, MathUtils.addExact(0L, -1L))
        Assert.assertEquals(-1, MathUtils.addExact(-1, 0).toLong())
        Assert.assertEquals(-1L, MathUtils.addExact(-1L, 0L))
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Int.MAX_VALUE,
                1
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Long.MAX_VALUE,
                1L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Int.MIN_VALUE,
                -1
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Long.MIN_VALUE,
                -1L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Integer.MIN_VALUE,
                Integer.MIN_VALUE
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Long.MIN_VALUE,
                Long.MIN_VALUE
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.addExact(
                Long.MAX_VALUE,
                Long.MAX_VALUE
            )
        }
    }

    @Test
    fun testSubtractExact() {
        // zero - zero
        Assert.assertEquals(0, MathUtils.subtractExact(0, 0).toLong())
        Assert.assertEquals(0L, MathUtils.subtractExact(0L, 0L))
        // positive - positive
        Assert.assertEquals(0, MathUtils.subtractExact(1, 1).toLong())
        Assert.assertEquals(0L, MathUtils.subtractExact(1L, 1L))
        Assert.assertEquals(1, MathUtils.subtractExact(2, 1).toLong())
        Assert.assertEquals(1L, MathUtils.subtractExact(2L, 1L))
        Assert.assertEquals(-1, MathUtils.subtractExact(1, 2).toLong())
        Assert.assertEquals(-1L, MathUtils.subtractExact(1L, 2L))
        // negative - negative
        Assert.assertEquals(0, MathUtils.subtractExact(-1, -1).toLong())
        Assert.assertEquals(0L, MathUtils.subtractExact(-1L, -1L))
        Assert.assertEquals(-1, MathUtils.subtractExact(-2, -1).toLong())
        Assert.assertEquals(-1L, MathUtils.subtractExact(-2L, -1L))
        Assert.assertEquals(1, MathUtils.subtractExact(-1, -2).toLong())
        Assert.assertEquals(1L, MathUtils.subtractExact(-1L, -2L))
        // positive - negative, negative - positive
        Assert.assertEquals(2, MathUtils.subtractExact(1, -1).toLong())
        Assert.assertEquals(2L, MathUtils.subtractExact(1L, -1L))
        Assert.assertEquals(-2, MathUtils.subtractExact(-1, 1).toLong())
        Assert.assertEquals(-2L, MathUtils.subtractExact(-1L, 1L))
        // zero - positive, positive - zero
        Assert.assertEquals(-1, MathUtils.subtractExact(0, 1).toLong())
        Assert.assertEquals(-1L, MathUtils.subtractExact(0L, 1L))
        Assert.assertEquals(1, MathUtils.subtractExact(1, 0).toLong())
        Assert.assertEquals(1L, MathUtils.subtractExact(1L, 0L))
        // zero - negative, negative - zero
        Assert.assertEquals(1, MathUtils.subtractExact(0, -1).toLong())
        Assert.assertEquals(1L, MathUtils.subtractExact(0L, -1L))
        Assert.assertEquals(-1, MathUtils.subtractExact(-1, 0).toLong())
        Assert.assertEquals(-1L, MathUtils.subtractExact(-1L, 0))
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.subtractExact(
                Int.MAX_VALUE,
                -1
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.subtractExact(
                Long.MAX_VALUE,
                -1L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.subtractExact(
                Int.MIN_VALUE,
                1
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.subtractExact(
                Long.MIN_VALUE,
                1L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.subtractExact(
                0,
                Int.MIN_VALUE
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.subtractExact(
                0,
                Long.MIN_VALUE
            )
        }
    }

    @Test
    fun testMultiplyExact() {
        Assert.assertEquals(0, MathUtils.multiplyExact(0, 0).toLong())
        Assert.assertEquals(4, MathUtils.multiplyExact(2, 2).toLong())
        Assert.assertEquals(0L, MathUtils.multiplyExact(0L, 0L))
        Assert.assertEquals(4L, MathUtils.multiplyExact(2L, 2L))
        Assert.assertEquals(0, MathUtils.multiplyExact(2, 0).toLong())
        Assert.assertEquals(0L, MathUtils.multiplyExact(2L, 0L))
        Assert.assertEquals(-4, MathUtils.multiplyExact(2, -2).toLong())
        Assert.assertEquals(-4L, MathUtils.multiplyExact(2L, -2L))
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Int.MAX_VALUE,
                2
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Long.MAX_VALUE,
                2L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Int.MIN_VALUE,
                2
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Long.MIN_VALUE,
                2L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Int.MAX_VALUE / 2 + 1,
                2
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Long.MAX_VALUE / 2L + 1L,
                2L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Int.MIN_VALUE / 2 - 1,
                2
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Long.MIN_VALUE / 2L - 1L,
                2L
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Int.MIN_VALUE,
                -1
            )
        }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) {
            MathUtils.multiplyExact(
                Long.MIN_VALUE,
                -1L
            )
        }
    }

    @Test
    fun testIncrementExact() {
        Assert.assertEquals(1, MathUtils.incrementExact(0).toLong())
        Assert.assertEquals(1L, MathUtils.incrementExact(0L))
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.incrementExact(Int.MAX_VALUE) }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.incrementExact(Long.MAX_VALUE) }
    }

    @Test
    fun testDecrementExact() {
        Assert.assertEquals(-1, MathUtils.decrementExact(0).toLong())
        Assert.assertEquals(-1L, MathUtils.decrementExact(0L))
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.decrementExact(Int.MIN_VALUE) }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.decrementExact(Long.MIN_VALUE) }
    }

    @Test
    fun testNegateExact() {
        Assert.assertEquals(
            (Int.MIN_VALUE + 1).toLong(),
            MathUtils.negateExact(Int.MAX_VALUE).toLong()
        )
        Assert.assertEquals(Long.MIN_VALUE + 1, MathUtils.negateExact(Long.MAX_VALUE))
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.negateExact(Int.MIN_VALUE) }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.negateExact(Long.MIN_VALUE) }
    }

    @Test
    fun testToIntExact() {
        Assert.assertEquals(1, MathUtils.toIntExact(1L).toLong())
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.toIntExact(Long.MAX_VALUE) }
        Assert.assertThrows(
            ArithmeticException::class.java
        ) { MathUtils.toIntExact(Long.MIN_VALUE) }
    }
}
