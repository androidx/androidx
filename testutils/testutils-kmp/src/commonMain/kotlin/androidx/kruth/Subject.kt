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

import kotlin.reflect.typeOf

// As opposed to Truth, which limits visibility on `actual` and the generic type, we purposely make
// them visible in Kruth to allow for an easier time extending in Kotlin.
// See: https://github.com/google/truth/issues/536

/**
 * An object that lets you perform checks on the value under test. For example, [Subject]
 * contains [isEqualTo] and [isInstanceOf], and [StringSubject] contains [StringSubject.contains]
 *
 * To create a [Subject] instance, most users will call an [assertThat] method.
 */
open class Subject<out T> internal constructor(
    val actual: T?,
    private val metadata: FailureMetadata = FailureMetadata(),
) {

    internal val asserter: KruthAsserter get() = asserter()

    internal fun asserter(withActual: Boolean = false): KruthAsserter =
        KruthAsserter(
            formatMessage = { message ->
                formatFailureMessage(message = message, withActual = withActual)
            },
        )

    private fun formatFailureMessage(message: String?, withActual: Boolean): String =
        if (withActual) {
            val actualString = actual.toString()
            if ('\n' in actualString) {
                metadata.formatMessage(
                    message,
                    "But was:",
                    actual.toString().prependIndent(),
                )
            } else {
                metadata.formatMessage(message, "But was: $actualString")
            }
        } else {
            metadata.formatMessage(message)
        }

    /**
     *  Fails if the subject is not null.
     */
    open fun isNull() {
        actual.standardIsEqualTo(null)
    }

    /**
     * Fails if the subject is null.
     **/
    open fun isNotNull() {
        actual.standardIsNotEqualTo(null)
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
    open fun isEqualTo(expected: Any?) {
        actual.standardIsEqualTo(expected)
    }

    /**
     * Fails if the subject is equal to the given object. The meaning of equality is the same as for
     * the [isEqualTo] method.
     */
    open fun isNotEqualTo(unexpected: Any?) {
        actual.standardIsNotEqualTo(unexpected)
    }

    /** Fails if the subject is not the same instance as the given object.  */
    open fun isSameInstanceAs(expected: Any?) {
        if (actual !== expected) {
            asserter.fail(
                "Expected ${actual.toStringForAssert()} to be the same instance as " +
                    "${expected.toStringForAssert()}, but was not"
            )
        }
    }

    /** Fails if the subject is the same instance as the given object.  */
    open fun isNotSameInstanceAs(unexpected: Any?) {
        if (actual === unexpected) {
            asserter.fail(
                "Expected ${actual.toStringForAssert()} not to be specific instance, but it was"
            )
        }
    }

    /**
     * Fails if the subject is not an instance of the given class.
     */
    inline fun <reified V> isInstanceOf() {
        if (actual !is V) {
            doFail("Expected $actual to be an instance of ${typeOf<V>()} but it was not")
        }
    }

    /**
     * Fails if the subject is an instance of the given class.
     **/
    inline fun <reified V> isNotInstanceOf() {
        if (actual is V) {
            doFail("Expected $actual to be not an instance of ${typeOf<V>()} but it was")
        }
    }

    protected fun failWithActual(vararg messages: String): Nothing {
        asserter(withActual = true).fail(messages.joinToString(separator = "\n"))
    }

    @PublishedApi
    internal fun doFail(message: String) {
        asserter.fail(message = message)
    }

    /** Fails unless the subject is equal to any element in the given [iterable]. */
    open fun isIn(iterable: Iterable<*>?) {
        if (actual !in requireNonNull(iterable)) {
            asserter.fail("Expected $actual to be in $iterable, but was not")
        }
    }

    /** Fails unless the subject is equal to any of the given elements. */
    open fun isAnyOf(first: Any?, second: Any?, vararg rest: Any?) {
        isIn(listOf(first, second, *rest))
    }

    /** Fails if the subject is equal to any element in the given [iterable]. */
    open fun isNotIn(iterable: Iterable<*>?) {
        if (actual in requireNonNull(iterable)) {
            asserter.fail("Expected $actual not to be in $iterable, but it was")
        }
    }

    /** Fails if the subject is equal to any of the given elements.  */
    open fun isNoneOf(first: Any?, second: Any?, vararg rest: Any?) {
        isNotIn(listOf(first, second, *rest))
    }

    private fun Any?.standardIsEqualTo(expected: Any?) {
        asserter.assertTrue(
            compareForEquality(expected),
            "expected: ${expected.toStringForAssert()} but was: ${toStringForAssert()}",
        )
    }

    private fun Any?.standardIsNotEqualTo(unexpected: Any?) {
        asserter.assertFalse(
            compareForEquality(unexpected),
            "expected ${toStringForAssert()} not be equal to ${unexpected.toStringForAssert()}, " +
                "but it was",
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
            this is IntArray && expected is IntArray -> contentEquals(expected)
            this is LongArray && expected is LongArray -> contentEquals(expected)
            this is FloatArray && expected is FloatArray -> contentEquals(expected)
            this is DoubleArray && expected is DoubleArray -> contentEquals(expected)
            this is ShortArray && expected is ShortArray -> contentEquals(expected)
            this is CharArray && expected is CharArray -> contentEquals(expected)
            this is Array<*> && expected is Array<*> -> contentDeepEquals(expected)
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
        else -> asserter.fail("$this must be either a Char or a Number.")
    }

    private fun Any?.toStringForAssert(): String = when {
        this == null -> toString()
        isIntegralBoxedPrimitive() -> "${this::class.qualifiedName}<$this>"
        else -> toString()
    }
}
