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

import kotlin.test.assertIs
import kotlin.test.assertTrue

// As opposed to Truth, which limits visibility on `actual` and the generic type, we purposely make
// them visible in Kruth to allow for an easier time extending in Kotlin.
// See: https://github.com/google/truth/issues/536

/**
 * An object that lets you perform checks on the value under test. For example, [Subject]
 * contains [isEqualTo] and [isInstanceOf], and [StringSubject] contains [StringSubject.contains]
 *
 * To create a [Subject] instance, most users will call an [assertThat] method.
 */
open class Subject<T>(val actual: T?) {

    /**
     *  Fails if the subject is not null.
     */
    fun isNull() {
        actual.standardIsEqualTo(null)
    }

    /**
     * Fails if the subject is not equal to the given object. For the purposes of this comparison,
     * two objects are equal if any of the following is true:
     * * they are both `null`
     * * they are equal according to [equals]
     * * they are [Array]s and are considered equal by [Array.contentEquals]
     * * they are boxed integer types ([Byte], [Short], [Char], [Int], or [Long]) and they are
     * numerically equal when converted to [Long].
     * * the actual value is a boxed floating-point type ([Double] or [Float]), the expected value
     * is an [Int], and the two are numerically equal when converted to [Double]. (This allows
     * assertThat(someDouble).isEqualTo(0) to pass.)
     *
     * Note: This method does not test the [equals] implementation itself; it assumes that method is
     * functioning correctly according to its contract. Testing an equals implementation requires a
     * utility such as guava-testlib's EqualsTester.
     *
     * In some cases, this method might not even call [equals]. It may instead perform other tests
     * that will return the same result as long as [equals] is implemented according to the contract
     * for its type.
     */
    fun isEqualTo(expected: Any?) {
        actual.standardIsEqualTo(expected)
    }

    /**
     * Fails if the subject is not an instance of the given class.
     */
    inline fun <reified V> isInstanceOf() = assertIs<V>(actual)
}

private fun Any?.standardIsEqualTo(expected: Any?) {
    assertTrue(
        compareForEquality(expected),
        "expected: ${expected.toStringForAssert()} but was: ${toStringForAssert()}",
    )
}

/**
 * Returns whether [this] equals [expected].
 *
 * The equality check follows the rules described on [Subject.isEqualTo].
 */
private fun Any?.compareForEquality(expected: Any?): Boolean {
    @Suppress("SuspiciousEqualsCombination") // Intentional for behaviour compatibility.
    // This is migrated from Truth's equality helper, which has very specific logic for handling the
    // magic "casting" they do between types. See:
    // https://github.com/google/truth/blob/master/core/src/main/java/com/google/common/truth/Subject.java#L210
    return when {
        this == null && expected == null -> true
        this == null || expected == null -> false
        this is ByteArray && expected is ByteArray -> contentEquals(expected)
        this is Array<*> && expected is Array<*> -> contentEquals(expected)
        isIntegralBoxedPrimitive() && expected.isIntegralBoxedPrimitive() -> {
            integralValue() == expected.integralValue()
        }
        this is Double && expected is Double -> compareTo(expected) == 0
        this is Float && expected is Float -> compareTo(expected) == 0
        this is Double && expected is Int -> compareTo(expected.toDouble()) == 0
        this is Float && expected is Int -> toDouble().compareTo(expected.toDouble()) == 0
        else -> this === expected || this == expected
    }
}

private fun Any?.isIntegralBoxedPrimitive(): Boolean {
    return this is Byte || this is Short || this is Char || this is Int || this is Long
}

private fun Any?.integralValue(): Long = when (this) {
    is Char -> code.toLong()
    is Number -> toLong()
    else -> throw AssertionError("$this must be either a Char or a Number.")
}

private fun Any?.toStringForAssert(): String = when {
    this == null -> toString()
    isIntegralBoxedPrimitive() -> "${this::class.qualifiedName}<$this>"
    else -> toString()
}
