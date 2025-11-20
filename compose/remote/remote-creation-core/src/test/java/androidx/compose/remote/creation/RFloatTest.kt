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

package androidx.compose.remote.creation

import org.junit.Assert.assertArrayEquals
import org.junit.Test

/** Test of RFloat class verifying internal consistency */
class RFloatTest {
    @Test
    fun times_withRFloat() {
        val zero = RFloat(0f)
        val one = RFloat(1f)
        val two = RFloat(2f)
        val negativeOne = RFloat(-1f)
        val negativeTwo = RFloat(-2f)

        val zeroTimesOne = zero * one
        val zeroTimesNegativeOne = zero * negativeOne
        val oneTimesTwo = one * two
        val negativeOneTimesNegativeTwo = negativeOne * negativeTwo

        assertArrayEquals(
            floatArrayOf(0.0f, 1.0f, Rc.FloatExpression.MUL).toTypedArray(),
            zeroTimesOne.toArray().toTypedArray(),
        )
        assertArrayEquals(
            floatArrayOf(0.0f, -1.0f, Rc.FloatExpression.MUL).toTypedArray(),
            zeroTimesNegativeOne.toArray().toTypedArray(),
        )
        assertArrayEquals(
            floatArrayOf(1.0f, 2.0f, Rc.FloatExpression.MUL).toTypedArray(),
            oneTimesTwo.toArray().toTypedArray(),
        )
        assertArrayEquals(
            floatArrayOf(-1.0f, -2.0f, Rc.FloatExpression.MUL).toTypedArray(),
            negativeOneTimesNegativeTwo.toArray().toTypedArray(),
        )
    }
}
