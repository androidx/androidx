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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Abstract base class for all remote string representations.
 *
 * `RemoteString` represents a string value that can be a constant, a named variable, or a dynamic
 * expression (e.g., a concatenation).
 */
@Stable
public abstract class RemoteString internal constructor() : BaseRemoteState<String>() {

    public val length: RemoteInt
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // Restricts operator
        get() {
            constantValueOrNull?.let {
                return RemoteInt(it.length)
            }

            return RemoteIntExpression(constantValueOrNull = null) { creationState ->
                longArrayOf(
                    0x100000000L +
                        Utils.idFromNan(
                                creationState.document.textLength(
                                    getIdForCreationState(creationState)
                                )
                            )
                            .toLong()
                )
            }
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val isEmpty: RemoteBoolean
        get() {
            constantValueOrNull?.let {
                return RemoteBoolean(it.isEmpty())
            }

            return RemoteBoolean(
                RemoteIntExpression(constantValueOrNull = null) { creationState ->
                    longArrayOf(
                        1,
                        0,
                        0x100000000L +
                            Utils.idFromNan(
                                    creationState.document.textLength(
                                        getIdForCreationState(creationState)
                                    )
                                )
                                .toLong(),
                        0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                    )
                }
            )
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val isNotEmpty: RemoteBoolean
        get() {
            constantValueOrNull?.let {
                return RemoteBoolean(it.isNotEmpty())
            }

            return RemoteBoolean(
                RemoteIntExpression(constantValueOrNull = null) { creationState ->
                    longArrayOf(
                        0,
                        1,
                        0x100000000L +
                            Utils.idFromNan(
                                    creationState.document.textLength(
                                        getIdForCreationState(creationState)
                                    )
                                )
                                .toLong(),
                        0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                    )
                }
            )
        }

    /**
     * Concatenates this [RemoteString] with another [RemoteString].
     *
     * @param v The other [RemoteString] to concatenate.
     * @return A new [MutableRemoteString] representing the concatenated string.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: RemoteString): RemoteString {
        if (constantValueOrNull != null && v.constantValueOrNull != null) {
            return RemoteString(constantValueOrNull!! + v.constantValueOrNull!!)
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textMerge(
                        getIdForCreationState(creationState),
                        v.getIdForCreationState(creationState),
                    )

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) =
                    mergeSets(
                        this@RemoteString.computeRequiredCodePointSet(creationState),
                        v.computeRequiredCodePointSet(creationState),
                    )
            },
        )
    }

    /**
     * Concatenates this [RemoteString] with a [String].
     *
     * @param v The [String] to concatenate.
     * @return A new [MutableRemoteString] representing the concatenated string.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun plus(v: String): RemoteString {
        return this + RemoteString(v)
    }

    /**
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString].
     *
     * @param start The inclusive index of the character at which the substring starts.
     * @return A new [MutableRemoteString] representing the substring.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun substring(start: Int): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(it.substring(start))
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textSubtext(
                        getIdForCreationState(creationState),
                        start.toFloat(),
                        -1f,
                    )

                // TODO(b/): This is probably overestimate, consider refactoring.
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = this@RemoteString.computeRequiredCodePointSet(creationState)
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to a upper case version of this [RemoteString] using
     * the system default locale.
     *
     * @return A new [MutableRemoteString] representing the upper case version of this string.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun uppercase(): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(it.uppercase())
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textTransform(
                        getIdForCreationState(creationState),
                        0f,
                        -1f,
                        TextTransform.TEXT_TO_UPPERCASE,
                    )

                // Is this correct in all locales?
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) =
                    this@RemoteString.computeRequiredCodePointSet(creationState)?.mapTo(HashSet()) {
                        it.uppercase()
                    }
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to a lower case version of this [RemoteString] using
     * the system default locale.
     *
     * @return A new [MutableRemoteString] representing the lower case version of this string.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun lowercase(): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(it.lowercase())
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textTransform(
                        getIdForCreationState(creationState),
                        0f,
                        -1f,
                        TextTransform.TEXT_TO_LOWERCASE,
                    )

                // Is this correct in all locales?
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) =
                    this@RemoteString.computeRequiredCodePointSet(creationState)?.mapTo(HashSet()) {
                        it.lowercase()
                    }
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to the trimmed version of this this [RemoteString]
     * where leading and trailing whitespace characters have been removed.
     *
     * @return A new [MutableRemoteString] representing the trimmed version of this string.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun trim(): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(it.trim())
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textTransform(
                        getIdForCreationState(creationState),
                        0f,
                        -1f,
                        TextTransform.TEXT_TRIM,
                    )

                // This is likely an overestimate, but whitespace glyphs are typically encoded as
                // a space so optimizing doesn't seem worthwhile.
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = this@RemoteString.computeRequiredCodePointSet(creationState)
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString]. The substring
     * starts at a dynamic [start] index (represented by a [RemoteInt]) and extends to the end of
     * the string.
     *
     * @param start The [RemoteInt] representing the inclusive index of the character at which the
     *   substring starts.
     * @return A new [MutableRemoteString] representing the substring.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun substring(start: RemoteInt): RemoteString {
        val constV = constantValueOrNull
        val constStart = start.constantValueOrNull
        if (constV != null && constStart != null) {
            return RemoteString(constV.substring(constStart))
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textSubtext(
                        getIdForCreationState(creationState),
                        start.getFloatIdForCreationState(creationState),
                        -1f,
                    )

                // TODO(b/): This is probably overestimate, consider refactoring.
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = this@RemoteString.computeRequiredCodePointSet(creationState)
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString]. The substring
     * starts at a fixed [start] index and ends before a fixed [end] index.
     *
     * @param start The inclusive index of the character at which the substring starts.
     * @param end The exclusive index after the last character of the substring.
     * @return A new [MutableRemoteString] representing the substring.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun substring(start: Int, end: Int): RemoteString {
        constantValueOrNull?.let {
            return RemoteString(it.substring(start, end))
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textSubtext(
                        getIdForCreationState(creationState),
                        start.toFloat(),
                        (end - start).toFloat(),
                    )

                // TODO(b/): This is probably overestimate, consider refactoring.
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = this@RemoteString.computeRequiredCodePointSet(creationState)
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString]. The substring
     * starts at a fixed [start] index and ends before a dynamic [end] index.
     *
     * @param start The inclusive index of the character at which the substring starts.
     * @param end The [RemoteInt] representing the exclusive index after the last character of the
     *   substring.
     * @return A new [MutableRemoteString] representing the substring.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun substring(start: Int, end: RemoteInt): RemoteString {
        val constV = constantValueOrNull
        val constEnd = end.constantValueOrNull
        if (constV != null && constEnd != null) {
            return RemoteString(constV.substring(start, constEnd))
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textSubtext(
                        getIdForCreationState(creationState),
                        start.getFloatIdForCreationState(creationState),
                        (end - start).getFloatIdForCreationState(creationState),
                    )

                // TODO(b/): This is probably overestimate, consider refactoring.
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = this@RemoteString.computeRequiredCodePointSet(creationState)
            },
        )
    }

    /**
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString]. Both the
     * [start] and [end] indices are dynamic, represented by [RemoteInt]s.
     *
     * @param start The [RemoteInt] representing the inclusive index of the character at which the
     *   substring starts.
     * @param end The [RemoteInt] representing the exclusive index after the last character of the
     *   substring.
     * @return A new [MutableRemoteString] representing the substring.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun substring(start: RemoteInt, end: RemoteInt): RemoteString {
        val constV = constantValueOrNull
        val constStart = start.constantValueOrNull
        val constEnd = end.constantValueOrNull
        if (constV != null && constStart != null && constEnd != null) {
            return RemoteString(constV.substring(constStart, constEnd))
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textSubtext(
                        getIdForCreationState(creationState),
                        start.getFloatIdForCreationState(creationState),
                        (end - start).getFloatIdForCreationState(creationState),
                    )

                // TODO(b/): This is probably overestimate, consider refactoring.
                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = this@RemoteString.computeRequiredCodePointSet(creationState)
            },
        )
    }

    /**
     * Attempts to compute the set of unicode code points that can occur in this string, or null if
     * that can\'t be statically determined (e.g. named strings rely on external state).
     *
     * This is useful if you need to compute the subset of a font that\'s required to render the
     * string.
     *
     * @param creationState The [RemoteComposeCreationState] context this is being evaluated within.
     * @return The set of unicode code points that can occur in this string, or null if that can\'t
     *   be statically determined .
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun computeRequiredCodePointSet(
        creationState: RemoteComposeCreationState
    ): Set<String>?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteString] instance from a constant [String] literal.
         *
         * @param v The constant [String] value.
         * @return A [MutableRemoteString] representing the constant string.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(v: String): RemoteString {
            return MutableRemoteString(
                constantValueOrNull = v,
                object : LazyRemoteString {
                    override fun reserveTextId(creationState: RemoteComposeCreationState) =
                        creationState.document.textCreateId(v)

                    override fun computeRequiredCodePointSet(
                        creationState: RemoteComposeCreationState
                    ) = v.toCodePointSet()
                },
            )
        }

        /**
         * Creates a [RemoteString] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteString] referencing the ID.
         */
        internal fun createForId(id: Int): RemoteString = MutableRemoteString(id)

        /**
         * Creates a named [RemoteString] with an initial value.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param defaultValue The initial [String] value for the named remote string.
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @return A [RemoteString] representing the named string.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createNamedRemoteString(
            name: String,
            defaultValue: String,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteString {
            return MutableRemoteString(
                constantValueOrNull = null,
                object : LazyRemoteString {
                    override fun reserveTextId(creationState: RemoteComposeCreationState) =
                        creationState.document.addNamedString("$domain:$name", defaultValue)

                    // Named strings can change so we can't statically determine the needed glyphs
                    override fun computeRequiredCodePointSet(
                        creationState: RemoteComposeCreationState
                    ) = null
                },
            )
        }
    }
}

private class SelectFloatImpl(
    val a: RemoteFloat,
    val b: RemoteFloat,
    val ifTrue: RemoteString,
    val ifFalse: RemoteString,
) : LazyRemoteString {
    override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
        val select =
            RemoteFloatExpression(constantValueOrNull = null) { creationState2 ->
                floatArrayOf(
                    1f,
                    0f,
                    a.getFloatIdForCreationState(creationState2),
                    b.getFloatIdForCreationState(creationState2),
                    AnimatedFloatExpression.SUB,
                    AnimatedFloatExpression.IFELSE,
                )
            }
        return creationState.document.textLookup(
            creationState.document.addStringList(
                ifFalse.getIdForCreationState(creationState),
                ifTrue.getIdForCreationState(creationState),
            ),
            select.getIdForCreationState(creationState),
        )
    }

    override fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState) =
        mergeSets(
            ifTrue.computeRequiredCodePointSet(creationState),
            ifFalse.computeRequiredCodePointSet(creationState),
        )
}

private class SelectIntImpl(
    val a: RemoteInt,
    val b: RemoteInt,
    val ifTrue: RemoteString,
    val ifFalse: RemoteString,
) : LazyRemoteString {
    override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
        val select =
            RemoteIntExpression(constantValueOrNull = null) { creationState2 ->
                longArrayOf(
                    1,
                    0,
                    0x100000000L + a.getIdForCreationState(creationState2).toLong(),
                    0x100000000L + b.getIdForCreationState(creationState2).toLong(),
                    0x100000000L + IntegerExpressionEvaluator.I_SUB,
                    0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                )
            }
        return creationState.document.textLookup(
            creationState.document.addStringList(
                ifFalse.getIdForCreationState(creationState),
                ifTrue.getIdForCreationState(creationState),
            ),
            select.getIdForCreationState(creationState),
        )
    }

    override fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState) =
        mergeSets(
            ifTrue.computeRequiredCodePointSet(creationState),
            ifFalse.computeRequiredCodePointSet(creationState),
        )
}

/**
 * Returns a [RemoteFloat] that evaluates to [ifTrue] if [a] is less than [b], otherwise returns
 * [ifFalse].
 *
 * @param a The left-hand side [RemoteFloat] for the less-than comparison.
 * @param b The right-hand side [RemoteFloat] for the less-than comparison.
 * @param ifTrue The [RemoteFloat] expression to return if `a < b` evaluates to true.
 * @param ifFalse The [RemoteFloat] expression to return if `a < b` evaluates to false.
 * @return A new [RemoteFloat] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLt(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA < constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectFloatImpl(b, a, ifFalse, ifTrue))
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] is less than [b], otherwise returns
 * [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the less-than comparison.
 * @param b The right-hand side [RemoteInt] for the less-than comparison.
 * @param ifTrue The [RemoteInt] expression to return if `a < b` evaluates to true.
 * @param ifFalse The [RemoteInt] expression to return if `a < b` evaluates to false.
 * @return A new [RemoteInt] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLt(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA < constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectIntImpl(b, a, ifFalse, ifTrue))
}

/**
 * Returns a [RemoteFloat] that evaluates to [ifTrue] if [a] is less than or equal to [b], otherwise
 * returns [ifFalse].
 *
 * @param a The left-hand side [RemoteFloat] for the comparison.
 * @param b The right-hand side [RemoteFloat] for the comparison.
 * @param ifTrue The [RemoteFloat] expression to return if `a <= b` evaluates to true.
 * @param ifFalse The [RemoteFloat] expression to return if `a <= b` evaluates to false.
 * @return A new [RemoteFloat] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLe(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA <= constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectFloatImpl(a, b, ifTrue, ifFalse))
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] is less than or equal to [b], otherwise
 * returns [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] expression to return if `a <= b` evaluates to true.
 * @param ifFalse The [RemoteInt] expression to return if `a <= b` evaluates to false.
 * @return A new [RemoteInt] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfLe(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA <= constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectIntImpl(a, b, ifTrue, ifFalse))
}

/**
 * Returns a [RemoteFloat] that evaluates to [ifTrue] if [a] is greater than [b], otherwise returns
 * [RemoteFloat].
 *
 * @param a The left-hand side [RemoteFloat] for the comparison.
 * @param b The right-hand side [RemoteFloat] for the comparison.
 * @param ifTrue The [RemoteFloat] expression to return if `a > b` evaluates to true.
 * @param ifFalse The [RemoteFloat] expression to return if `a > b` evaluates to false.
 * @return A new [RemoteFloat] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGt(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA > constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectFloatImpl(a, b, ifFalse, ifTrue))
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] is greater than [b], otherwise returns
 * [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] expression to return if `a > b` evaluates to true.
 * @param ifFalse The [RemoteInt] expression to return if `a > b` evaluates to false.
 * @return A new [RemoteInt] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGt(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA > constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectIntImpl(a, b, ifFalse, ifTrue))
}

/**
 * Returns a [RemoteFloat] that evaluates to [ifTrue] if [a] is greater than or equal to [b],
 * otherwise returns [ifFalse].
 *
 * @param a The left-hand side [RemoteFloat] for the comparison.
 * @param b The right-hand side [RemoteFloat] for the comparison.
 * @param ifTrue The [RemoteFloat] expression to return if `a >= b` evaluates to true.
 * @param ifFalse The [RemoteFloat] expression to return if `a >= b` evaluates to false.
 * @return A new [RemoteFloat] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGe(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA >= constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectFloatImpl(b, a, ifTrue, ifFalse))
}

/**
 * Returns a [RemoteInt] that evaluates to [ifTrue] if [a] is greater than or equal to [b],
 * otherwise returns [ifFalse].
 *
 * @param a The left-hand side [RemoteInt] for the comparison.
 * @param b The right-hand side [RemoteInt] for the comparison.
 * @param ifTrue The [RemoteInt] expression to return if `a >= b` evaluates to true.
 * @param ifFalse The [RemoteInt] expression to return if `a >= b` evaluates to false.
 * @return A new [RemoteInt] representing the selected value, evaluated remotely.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun selectIfGe(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    val constA = a.constantValueOrNull
    val constB = b.constantValueOrNull
    if (constA != null && constB != null) {
        return if (constA >= constB) {
            ifTrue
        } else {
            ifFalse
        }
    }

    return MutableRemoteString(constantValueOrNull = null, SelectIntImpl(b, a, ifTrue, ifFalse))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface LazyRemoteString {
    /**
     * @return The text ID for the RemoteString within the provided [RemoteComposeCreationState].
     */
    public fun reserveTextId(creationState: RemoteComposeCreationState): Int

    /**
     * @return The set of unicode code points needed to render this string, or null if that can\'t
     *   be statically determined.
     */
    public fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState): Set<String>?
}

/** @return The string split up into a set of unicode code points. */
internal fun String.toCodePointSet(): Set<String> {
    val s = HashSet<String>()
    for (cPoint in codePoints()) {
        s.add(Character.toString(cPoint))
    }
    return s
}

internal fun mergeSets(a: Set<String>?, b: Set<String>?): Set<String>? {
    if (a == null || b == null) {
        return null
    }
    return a + b
}

/** An implementation of [RemoteString] that holds its value in a [MutableState<String>]. */
public class MutableRemoteString
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: String?,
    private val lazyRemoteString: LazyRemoteString,
) : RemoteString(), MutableRemoteState<String> {

    /** Create a MutableRemoteString from an existing id. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        id: Int
    ) : this(
        constantValueOrNull = null,
        object : LazyRemoteString {
            override fun reserveTextId(creationState: RemoteComposeCreationState) = id

            override fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState) =
                null
        },
    )

    /** Create a MutableRemoteString for a default value. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        value: String
    ) : this(
        constantValueOrNull = null,
        object : LazyRemoteString {
            override fun reserveTextId(creationState: RemoteComposeCreationState) =
                creationState.document.addText(value)

            override fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState) =
                null
        },
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        lazyRemoteString.reserveTextId(creationState)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun computeRequiredCodePointSet(
        creationState: RemoteComposeCreationState
    ): Set<String>? = lazyRemoteString.computeRequiredCodePointSet(creationState)

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteString] instance.
         */
        public fun createMutable(initialValue: String): MutableRemoteString =
            MutableRemoteString(initialValue)

        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteString] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Int): MutableRemoteString = MutableRemoteString(id)
    }
}

/**
 * Factory composable for mutable remote string state.
 *
 * @param initialValue The initial [String] value.
 * @return A [MutableRemoteString] instance that will be remembered across recompositions.
 */
@Composable
public fun rememberMutableRemoteString(initialValue: String): MutableRemoteString {
    return remember { MutableRemoteString(initialValue) }
}

/** Factory composable for mutable remote string state. */
@Composable
@Deprecated("Use rememberMutableRemoteString(content())")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRemoteString(content: () -> String): MutableRemoteString {
    return remember {
        val string = content()
        MutableRemoteString(string)
    }
}

/**
 * Remembers a named remote string expression.
 *
 * @param name The unique name for this remote string.
 * @param domain The domain of the named string (defaults to [RemoteState.Domain.User]).
 * @param defaultValue The initial [String] value for this remote string.
 * @return A [RemoteString] representing the named remote string expression.
 */
@Composable
public fun rememberNamedRemoteString(
    name: String,
    defaultValue: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteString {
    return rememberNamedState(name, domain) {
        MutableRemoteString(
            constantValueOrNull = null,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    return creationState.document.addNamedString("$domain:$name", defaultValue)
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? = defaultValue.rs.computeRequiredCodePointSet(creationState)
            },
        )
    }
}

/**
 * A Composable function to remember and provide an anonymous (unnamed) mutable remote string.
 *
 * @param content A lambda that provides the initial [String] value for this remote string.
 * @return A [MutableRemoteString] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@Deprecated("Use rememberNamedRemoteString(name, domain, content = { RemoteString(content()) })")
public fun rememberRemoteString(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    content: () -> String,
): RemoteString {
    return rememberNamedRemoteString(name = name, defaultValue = content(), domain = domain)
}

/**
 * A convenience Composable function to remember a **system-level** named remote string.
 *
 * This is a specialized version of [rememberRemoteString] where the `domain` is fixed to
 * [RemoteState.Domain.System].
 *
 * @param name The unique name for this system remote string.
 * @param content A lambda that provides the initial [String] value for this remote string.
 * @return A [MutableRemoteString] instance with a system domain, remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@Deprecated("Use rememberRemoteString with SYSTEM domain")
public fun rememberSystemRemoteString(name: String, content: () -> String): RemoteString =
    rememberNamedRemoteString(
        name = name,
        domain = RemoteState.Domain.System,
        defaultValue = content(),
    )

/** Extension property to convert a [String] to a [RemoteString]. */
public val String.rs: RemoteString
    get() {
        return RemoteString(this)
    }
