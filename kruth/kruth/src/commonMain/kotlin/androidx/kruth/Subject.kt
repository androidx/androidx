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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import androidx.kruth.OldAndNewValuesAreSimilar.DIFFERENT
import androidx.kruth.OldAndNewValuesAreSimilar.SIMILAR
import kotlin.reflect.typeOf

// As opposed to Truth, which limits visibility on `actual` and the generic type, we purposely make
// them visible in Kruth to allow for an easier time extending in Kotlin.
// See: https://github.com/google/truth/issues/536

/**
 * An object that lets you perform checks on the value under test. For example, [Subject] contains
 * [isEqualTo] and [isInstanceOf], and [StringSubject] contains [StringSubject.contains]
 *
 * To create a [Subject] instance, most users will call an [assertThat] method.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
open class Subject<out T>
internal constructor(
    val actual: T?,
    metadata: FailureMetadata,
    private val typeDescriptionOverride: String?
) {
    /**
     * Constructor for use by subclasses. If you want to create an instance of this class itself,
     * call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
     */
    protected constructor(metadata: FailureMetadata, actual: T?) : this(actual, metadata, null)

    val metadata: FailureMetadata by lazy { metadata.updateForSubject(this) }

    protected fun check(): StandardSubjectBuilder =
        StandardSubjectBuilder(metadata = metadata.updateForCheckCall())

    /** Fails if the subject is not null. */
    open fun isNull() {
        actual.standardIsEqualTo(null)
    }

    /** Fails if the subject is null. */
    open fun isNotNull() {
        actual.standardIsNotEqualTo(null)
    }

    /**
     * Fails if the subject is not equal to the given object. For the purposes of this comparison,
     * two objects are equal if any of the following is true:
     * * they are both 'null'
     * * they are equal according to [equals]
     * * they are [Array]s and are considered equal by [Array.contentEquals]
     * * they are boxed integer types ([Byte], [Short], [Char], [Int], or [Long]) and they are
     *   numerically equal when converted to [Long].
     * * the actual value is a boxed floating-point type ([Double] or [Float]), the expected value
     *   is an [Int], and the two are numerically equal when converted to [Double]. (This allows
     *   assertThat(someDouble).isEqualTo(0) to pass.)
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

    /** Fails if the subject is not the same instance as the given object. */
    open fun isSameInstanceAs(expected: Any?) {
        if (actual !== expected) {
            metadata.fail(
                listOf(
                    // TODO(dustinlam): This error string does not match the one from Truth.
                    simpleFact(
                        "Expected ${actual.toStringForAssert()} to be the same instance  as " +
                            "${expected.toStringForAssert()}, but was not"
                    )
                )
            )
        }
    }

    /** Fails if the subject is the same instance as the given object. */
    open fun isNotSameInstanceAs(unexpected: Any?) {
        if (actual === unexpected) {
            failWithoutActual(fact("expected not to be specific instance", actual))
        }
    }

    /** Fails if the subject is not an instance of the given class. */
    // TODO(dustinlam): Add a JVM-only non inline version for compatibility and java users.
    inline fun <reified V> isInstanceOf() {
        if (actual !is V) {
            doFail(fact("expected instance of", typeOf<V>()), fact("but was", actual.toString()))
        }
    }

    /** Fails if the subject is an instance of the given class. */
    // TODO(dustinlam): Add a JVM-only non inline version for compatibility and java users.
    inline fun <reified V> isNotInstanceOf() {
        if (actual is V) {
            doFail(
                fact("expected not to be an instance of", typeOf<V>()),
                fact("but was", actual.toString())
            )
        }
    }

    // TODO(KT-20427): Only needed to enable extensions in internal sources.
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun failWithActualInternal(key: String, value: Any? = null) {
        failWithActual(key = key, value = value)
    }

    // TODO(KT-20427): Only needed to enable extensions in internal sources.
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun failWithActualInternal(fact: Fact, vararg facts: Fact) {
        failWithActual(fact, *facts)
    }

    /**
     * Fails, reporting a message with two "[facts][Fact]":
     * * _key_: _value_
     * * but was: _actual value_.
     *
     * This is the simplest failure API. For more advanced needs, see `failWithActual(Fact,
     * Fact...)` the other overload, and `failWithoutActual(Fact, Fact...)`.
     *
     * Example usage: The check `contains(String)` calls `failWithActual("expected to contain",
     * string)`.
     */
    protected fun failWithActual(key: String, value: Any?) {
        failWithActual(fact(key, value))
    }

    /**
     * Fails, reporting a message with the given facts, followed by an automatically added fact of
     * the form:
     * * but was: _actual value_.
     *
     * If you have only one fact to report (and it's a key-value [Fact]), prefer
     * `failWithActual(String, Any?)`, the simpler overload).
     *
     * Example usage: The check `isEmpty()` calls `failWithActual(simpleFact("expected to be
     * empty"))`.
     */
    protected fun failWithActual(first: Fact, vararg rest: Fact) {
        metadata.fail(
            listOf(
                first,
                *rest,
                // TODO(dustinlam): Value should be .actualCustomStringRepresentation()
                fact("but was", actual)
            )
        )
    }

    // TODO(KT-20427): Only needed to enable extensions in internal sources.
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun failWithoutActualInternal(first: Fact, vararg rest: Fact) {
        failWithoutActual(first, *rest)
    }

    /**
     * Assembles a failure message without a given subject and passes it to the FailureStrategy
     *
     * @param check the check being asserted
     */
    @Deprecated(
        "Prefer to construct Fact-style methods, typically by using " +
            "failWithoutActual(Fact, Fact...). However, if you want to preserve your exact " +
            "failure message as a migration aid, you can inline this method (and then inline the " +
            "resulting method call, as well).",
        ReplaceWith(
            "failWithoutActual(simpleFact(\"Not true that the subject \$check\"))",
            "androidx.kruth.Fact.Companion.simpleFact"
        )
    )
    internal fun failWithoutActual(check: String) {
        failWithoutActual(simpleFact("Not true that the subject $check"))
    }

    /**
     * Fails, reporting a message with the given facts, _without automatically adding the actual
     * value._
     *
     * Most failure messages should report the actual value, so most checks should call
     * `failWithActual(Fact, Fact...)` instead. However, [failWithoutActual] is useful in some
     * cases:
     * * when the actual value is obvious from the rest of the message. For example, `isNotEmpty()`
     *   calls `failWithoutActual(simpleFact("expected not to be empty")`.
     * * when the actual value shouldn't come last or should have a different key than the default
     *   of "but was." For example, `isNotWithin(...).of(...)` calls `failWithoutActual` so that it
     *   can put the expected and actual values together, followed by the tolerance.
     *
     * Example usage: The check `isEmpty()` calls `failWithActual(simpleFact("expected to be
     * empty"))`.
     */
    protected fun failWithoutActual(first: Fact, vararg rest: Fact) {
        metadata.fail(
            buildList {
                add(first)
                addAll(rest)
            }
        )
    }

    @PublishedApi // Required to allow isInstanceOf to be implemented via inline reified type.
    internal fun doFail(vararg facts: Fact) {
        metadata.fail(facts.asList())
    }

    /** Fails unless the subject is equal to any element in the given [iterable]. */
    open fun isIn(iterable: Iterable<*>?) {
        if (actual !in requireNonNull(iterable)) {
            metadata.fail(listOf(simpleFact("Expected $actual to be in $iterable, but was not")))
        }
    }

    /** Fails unless the subject is equal to any of the given elements. */
    open fun isAnyOf(first: Any?, second: Any?, vararg rest: Any?) {
        isIn(listOf(first, second, *rest))
    }

    /** Fails if the subject is equal to any element in the given [iterable]. */
    open fun isNotIn(iterable: Iterable<*>?) {
        if (actual in requireNonNull(iterable)) {
            failWithActual(fact("expected not to be any of", iterable))
        }
    }

    /** Fails if the subject is equal to any of the given elements. */
    open fun isNoneOf(first: Any?, second: Any?, vararg rest: Any?) {
        isNotIn(listOf(first, second, *rest))
    }

    /**
     * Supplies the direct string representation of the actual value to other methods which may
     * prefix or otherwise position it in an error message. This should only be overridden to
     * provide an improved string representation of the value under test, as it would appear in any
     * given error message, and should not be used for additional prefixing.
     *
     * Subjects should override this with care.
     *
     * By default, this returns `actual.toString()`.
     */
    // TODO(cpovirk): Consider whether this API pulls its weight. If users want to format the actual
    //  value, maybe they should do so themselves? Of course, they won't have a chance to use a
    //  custom format for inherited implementations like isEqualTo(). But if they want to format the
    //  actual value specially, then it seems likely that they'll want to format the expected value
    //  specially, too. And that applies just as well to APIs like isIn(). Maybe we'll want an API
    //  that supports formatting those values, too (like formatActualOrExpected below)? See also the
    //  related b/70930431. But note that we are likely to use this from FailureMetadata, at least
    //  in the short term, for better or for worse.
    protected open fun actualCustomStringRepresentation(): String {
        // TODO(dustinlam): This should check for ByteArray to return ByteArray.toHexString()
        //  when we move to Kotlin 1.9
        return actual.toString()
    }

    private fun Any?.standardIsEqualTo(expected: Any?) {
        metadata.assertTrue(compareForEquality(expected)) {
            "expected: ${expected.toStringForAssert()} but was: ${toStringForAssert()}"
        }
    }

    private fun Any?.standardIsNotEqualTo(unexpected: Any?) {
        if (compareForEquality(unexpected)) {
            failWithoutActual(
                fact("expected not to be", unexpected),
                fact("but was; string representation of actual value", actual)
            )
        }
    }

    /**
     * Returns whether [this] equals [expected].
     *
     * The equality check follows the rules described on [Subject.isEqualTo].
     */
    private fun Any?.compareForEquality(expected: Any?): Boolean {
        @Suppress("SuspiciousEqualsCombination") // Intentional for behaviour compatibility.
        // This is migrated from Truth's equality helper, which has very specific logic for handling
        // the
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
            this is BooleanArray && expected is BooleanArray -> contentEquals(expected)
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

    private fun Any?.integralValue(): Long =
        when (this) {
            is Char -> code.toLong()
            is Number -> toLong()
            // This is intentionally AssertionError and not AssertionErrorWithFacts to stay
            // behaviour
            // compatible with Truth.
            else -> throw AssertionError("$this must be either a Char or a Number.")
        }

    private fun Any?.toStringForAssert(): String =
        when {
            this == null -> toString()
            isIntegralBoxedPrimitive() -> "${this::class.qualifiedName}<$this>"
            else -> toString()
        }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun checkInternal(format: String, vararg args: Any): StandardSubjectBuilder =
        check(format, *args)

    /**
     * Returns a builder for creating a derived subject.
     *
     * Derived subjects retain the [FailureStrategy] and [StandardSubjectBuilder.withMessage] of the
     * current subject, and in some cases, they automatically supplement their failure message with
     * information about the original subject.
     *
     * For example, [ThrowableSubject.hasMessageThat], which returns a [StringSubject], is
     * implemented with `check("getMessage()").that(actual.getMessage())`.
     *
     * The arguments to [check] describe how the new subject was derived from the old, formatted
     * like a chained method call. This allows Truth to include that information in its failure
     * messages. For example, `assertThat(caught).hasCauseThat().hasMessageThat()` will produce a
     * failure message that includes the string "throwable.getCause().getMessage()," thanks to
     * internal [check] calls that supplied "getCause()" and "getMessage()" as arguments.
     *
     * If the method you're delegating to accepts parameters, you can pass [check] a format string.
     * For example, [MultimapSubject.valuesForKey] calls `check("valuesForKey(%s)", key)`.
     *
     * If you aren't really delegating to an instance method on the actual value -- maybe you're
     * calling a static method, or you're calling a chain of several methods -- you can supply
     * whatever string will be most useful to users. For example, if you're delegating to
     * `getOnlyElement(actual.colors())`, you might call `check("onlyColor()")`.
     *
     * @param format a template with `%s` placeholders
     * @param args the arguments to be inserted into those placeholders
     */
    protected fun check(format: String, vararg args: Any): StandardSubjectBuilder {
        validatePlaceholders(format, args)
        return doCheck(DIFFERENT, lenientFormat(format, *args))
    }

    // TODO(b/134064106): Figure out a public API for this.
    internal fun checkNoNeedToDisplayBothValues(
        format: String,
        vararg args: Any
    ): StandardSubjectBuilder {
        validatePlaceholders(format, args)
        return doCheck(SIMILAR, lenientFormat(format, *args))
    }

    private fun validatePlaceholders(format: String, args: Array<out Any?>) {
        var placeholders = 0
        var index = format.indexOf("%s")
        while (index != -1) {
            placeholders++
            index = format.indexOf("%s", index + 1)
        }
        require(placeholders == args.size) {
            "Incorrect number of args (${args.size}) for the given placeholders ($placeholders) " +
                "in string template:\"$format\""
        }
    }

    private fun doCheck(
        valuesAreSimilar: OldAndNewValuesAreSimilar,
        message: String
    ): StandardSubjectBuilder {
        return StandardSubjectBuilder(
            metadata.updateForCheckCall(valuesAreSimilar) { input: String? -> "$input.$message" }
        )
    }

    internal fun typeDescription(): String {
        if (typeDescriptionOverride != null) return typeDescriptionOverride

        /**
         * j2cl doesn't store enough metadata to know whether "Foo$BarSubject" is a nested class, so
         * it can't tell whether the simple name is "Foo$BarSubject" or just "BarSubject":
         * b/71808768. It returns "Foo$BarSubject" to err on the side of preserving information. We
         * want just "BarSubject," so we strip any likely enclosing type ourselves.
         */
        val subjectClass: String? = this::class.simpleName?.replaceFirst(Regex(".*[$]"), "")
        val actualClass: String =
            if (
                subjectClass != null &&
                    subjectClass.endsWith("Subject") &&
                    subjectClass != "Subject"
            ) {
                subjectClass.substring(0, subjectClass.length - "Subject".length)
            } else {
                "Object"
            }
        return actualClass.replaceFirstChar { it.lowercaseChar() }
    }

    protected fun ignoreCheck(): StandardSubjectBuilder {
        return StandardSubjectBuilder.forCustomFailureStrategy {}
    }

    /**
     * In a fluent assertion chain, the argument to the common overload of
     * [StandardSubjectBuilder.about], the method that specifies what kind of [Subject] to create.
     *
     * For more information about the fluent chain, see
     * [this FAQ entry](https://truth.dev/faq#full-chain).
     *
     * **For people extending Kruth**
     *
     * When you write a custom subject, see [our doc on extensions](https://truth.dev/extension). It
     * explains where [Factory] fits into the process.
     */
    fun interface Factory<out SubjectT : Subject<ActualT>, ActualT> {
        fun createSubject(metadata: FailureMetadata, actual: ActualT?): SubjectT
    }
}

internal fun lenientFormat(template: String, vararg args: Any?): String {
    val argsToLenientStrings =
        args.map {
            if (it == null) {
                return@map "null"
            }

            try {
                it.toString()
            } catch (e: Exception) {
                // Default toString() behavior - see Object.toString()
                val className = it::class.simpleName
                val exceptionClassName = e::class.simpleName
                val hashCodeHexString = it.hashCode().toUInt().toString(16)
                "<$$className@$hashCodeHexString threw $exceptionClassName>"
            }
        }

    var i = 0
    val formattedString =
        template.replace(Regex("%s")) { matchResult ->
            val result =
                when {
                    i <= argsToLenientStrings.lastIndex -> argsToLenientStrings[i]
                    else -> matchResult.value
                }
            i++
            return@replace result
        }

    return when {
        i >= argsToLenientStrings.size -> formattedString
        else -> "$formattedString [${argsToLenientStrings.subList(i, argsToLenientStrings.size)}]"
    }
}
