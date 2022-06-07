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

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// This mimics truth APis. not that we would merge it but makes moving tests and trying it
// easier
// TODO: move to its own module
open class KruthAssertion<T>(
    val actual: T?
) {
    fun isEqualTo(
        expected: T?
    ) {
        // TODO truth does some clever conversions here. e.g. you can say byte is equal to int
        assertEquals(
            expected = expected,
            actual = actual
        )
    }

    fun isNotNull() {
        assertNotNull(actual)
    }
}

open class KruthStringAssertion(
    actual: String?
) : KruthAssertion<String>(actual) {
    fun startsWith(prefix: String) {
        assertTrue(actual?.startsWith(prefix) ?: false)
    }
    fun endsWith(suffix: String) {
        assertTrue(actual?.endsWith(suffix) ?: false)
    }
    fun contains(chars: CharSequence) {
        assertTrue(actual is CharSequence)
        assertContains(actual, chars)
    }
}

class KruthBooleanAssertion(
    actual: Boolean?
) : KruthAssertion<Boolean>(actual) {
    fun isTrue() {
        assertTrue(actual is Boolean && actual)
    }
    fun isFalse() {
        assertTrue(actual is Boolean && !actual)
    }
}

class KTruthThrowableAssertion(
    actual: Throwable?
) : KruthAssertion<Throwable>(actual) {

    fun hasMessageThat(): KruthStringAssertion {
        return KruthStringAssertion(actual?.message)
    }
}

fun <T> assertThat(
    actual: T?
) = KruthAssertion(actual)

fun assertThat(
    actual: Boolean?
) = KruthBooleanAssertion(actual)

inline fun <reified E : Exception> assertThrows(test: () -> Unit): KTruthThrowableAssertion {
    try {
        test()
        assertTrue(false, "Exception ${E::class} not thrown")
        throw AssertionError("Impossible to get here")
    } catch (ex: Exception) {
        assertTrue(ex is E, "Expected thrown ${E::class}, got ${ex::class}")
        return KTruthThrowableAssertion(ex)
    }
}