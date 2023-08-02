/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.kruth

import kotlin.experimental.inv
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * Supplemental tests to [SubjectTest].
 */
class SubjectKruthTest {

    @Test
    fun isEqualTo_byteArray() {
        val a = ByteArray(3) { it.toByte() }
        val b = ByteArray(3) { it.toByte() }
        assertThat(a).isEqualTo(b)

        val c = ByteArray(3) { it.toByte().inv() }
        assertFailsWith<AssertionError> {
            assertThat(a).isEqualTo(c)
        }
    }

    @Test
    fun isEqualTo_typedArray() {
        val a = Array(3) { it.toChar() }
        val b = a.copyOf()
        assertThat(a).isEqualTo(b)

        // Truth does not do its clever primitive casting inside a typed Array.
        val c = Array(3) { it.toShort() }
        assertFailsWith<AssertionError> {
            assertThat(a).isEqualTo(c)
        }
    }

    @Test
    fun isEqualTo_doubleNaN() {
        assertThat(Double.NaN).isEqualTo(Double.NaN)
    }

    @Test
    fun isEqualTo_doublePositiveNegativeZero() {
        assertFailsWith<AssertionError> {
            assertThat(-0.0).isEqualTo(0.0)
        }
    }

    @Test
    fun isEqualTo_doubleExpectedInt() {
        val a = 0.0
        val b = 0
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun isEqualTo_floatNaN() {
        assertThat(Float.NaN).isEqualTo(Float.NaN)
    }

    @Test
    fun isEqualTo_floatPositiveNegativeZero() {
        assertFailsWith<AssertionError> {
            assertThat(-0.0f).isEqualTo(0.0f)
        }
    }

    @Test
    fun isEqualTo_floatExpectedIntConvertToDouble() {
        @Suppress("FloatingPointLiteralPrecision") // Intentional for this test.
        val a: Float = 16_777_217f
        val b: Int = 16_777_217
        assertFailsWith<AssertionError> {
            // 16_777_217f.compareTo(16_777_217) returns 0, but if 16_777_217 is converted to a
            // Double this should throw, because 16_777_217f cannot be represented precisely in
            // single precision Float.
            assertThat(a).isEqualTo(b)
        }
    }

    @Test
    fun isEqualTo_referentialEquality() {
        val a = object {
            override fun equals(other: Any?): Boolean = fail("Should never get here")
        }
        val b = a

        assertThat(a).isEqualTo(b)
    }
}
