/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Abstract base class for all remote string representations in Compose Remote, this class extends
 * [RemoteState<String>].
 *
 * @property hasConstantValue A boolean indicating whether this [RemoteString] will always evaluate
 *   to the same [value]. This is a **conservative check**; it might report `false` even for some
 *   expressions that are effectively constant if tracking their dependencies is computationally
 *   expensive.
 */
abstract class RemoteString internal constructor(override val hasConstantValue: Boolean) :
    RemoteState<String> {

    val length: RemoteInt
        get() {
            return RemoteIntExpression(hasConstantValue) { creationState ->
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

    val isEmpty: RemoteBoolean
        get() {
            return RemoteBoolean(
                RemoteIntExpression(hasConstantValue) { creationState ->
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

    val isNotEmpty: RemoteBoolean
        get() {
            return RemoteBoolean(
                RemoteIntExpression(hasConstantValue) { creationState ->
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

    /**
     * Concatenates this [RemoteString] with another [RemoteString].
     *
     * @param v The other [RemoteString] to concatenate.
     * @return A new [MutableRemoteString] representing the concatenated string.
     */
    operator fun plus(v: RemoteString): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue && v.hasConstantValue,
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
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString].
     *
     * @param start The inclusive index of the character at which the substring starts.
     * @return A new [MutableRemoteString] representing the substring.
     */
    fun substring(start: Int): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue,
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
     * Returns a [RemoteString] that evaluates to a substring of this [RemoteString]. The substring
     * starts at a dynamic [start] index (represented by a [RemoteInt]) and extends to the end of
     * the string.
     *
     * @param start The [RemoteInt] representing the inclusive index of the character at which the
     *   substring starts.
     * @return A new [MutableRemoteString] representing the substring.
     */
    fun substring(start: RemoteInt): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue,
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
    fun substring(start: Int, end: Int): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue,
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
    fun substring(start: Int, end: RemoteInt): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue,
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState) =
                    creationState.document.textSubtext(
                        getIdForCreationState(creationState),
                        start.toFloat(),
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
    fun substring(start: RemoteInt, end: RemoteInt): RemoteString {
        return MutableRemoteString(
            mutableStateOf(""),
            hasConstantValue,
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
     * that can't be statically determined (e.g. named strings rely on external state).
     *
     * This is useful if you need to compute the subset of a font that's required to render the
     * string.
     *
     * @param creationState The [RemoteComposeCreationState] context this is being evaluated within.
     * @return The set of unicode code points that can occur in this string, or null if that can't
     *   be statically determined .
     */
    abstract fun computeRequiredCodePointSet(
        creationState: RemoteComposeCreationState
    ): Set<String>?

    companion object {
        /**
         * Creates a [RemoteString] instance from a constant [String] literal.
         *
         * @param v The constant [String] value.
         * @return A [MutableRemoteString] representing the constant string.
         */
        operator fun invoke(v: String): RemoteString {
            return MutableRemoteString(
                mutableStateOf(""),
                true,
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
         * Creates a named [RemoteString] with an initial value. Named remote strings can be set via
         * AndroidRemoteContext.setNamedString.
         *
         * @param name The unique name for this remote string.
         * @param initialValue The initial [String] value for the named remote string.
         * @return A [RemoteString] representing the named string.
         */
        @JvmStatic
        fun createNamedRemoteString(name: String, initialValue: String): RemoteString {
            return MutableRemoteString(
                mutableStateOf(initialValue),
                false,
                object : LazyRemoteString {
                    // TODO: check what happens if the initial value for this is the same as a
                    //  subsequent non-named variable.
                    override fun reserveTextId(creationState: RemoteComposeCreationState) =
                        creationState.document.addNamedString(name, initialValue)

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
            RemoteFloatExpression(a.hasConstantValue && b.hasConstantValue) { creationState2 ->
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
            RemoteIntExpression(a.hasConstantValue && b.hasConstantValue) { creationState2 ->
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
fun selectIfLT(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectFloatImpl(a, b, ifTrue, ifFalse),
    )
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
fun selectIfLT(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectIntImpl(a, b, ifTrue, ifFalse),
    )
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
fun selectIfLE(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectFloatImpl(b, a, ifFalse, ifTrue),
    )
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
fun selectIfLE(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectIntImpl(b, a, ifFalse, ifTrue),
    )
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
fun selectIfGT(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectFloatImpl(a, b, ifFalse, ifTrue),
    )
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
fun selectIfGT(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectIntImpl(a, b, ifFalse, ifTrue),
    )
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
fun selectIfGE(
    a: RemoteFloat,
    b: RemoteFloat,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectFloatImpl(b, a, ifTrue, ifFalse),
    )
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
fun selectIfGE(
    a: RemoteInt,
    b: RemoteInt,
    ifTrue: RemoteString,
    ifFalse: RemoteString,
): RemoteString {
    return MutableRemoteString(
        mutableStateOf(""),
        ifTrue.hasConstantValue &&
            ifFalse.hasConstantValue &&
            a.hasConstantValue &&
            b.hasConstantValue,
        SelectIntImpl(b, a, ifTrue, ifFalse),
    )
}

internal interface LazyRemoteString {
    /**
     * @return The text ID for the RemoteString within the provided [RemoteComposeCreationState].
     */
    fun reserveTextId(creationState: RemoteComposeCreationState): Int

    /**
     * @return The set of unicode code points needed to render this string, or null if that can't be
     *   statically determined.
     */
    fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState): Set<String>?
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

/**
 * An implementation of [RemoteString] that holds its value in a [MutableState<String>].
 *
 * @property content The underlying [MutableState<String>] that stores the current string value.
 * @property hasConstantValue A boolean indicating if this string is constant.
 * @property lazyRemoteString An instance of [LazyRemoteString] that handles deferred operations.
 */
class MutableRemoteString
internal constructor(
    private val content: MutableState<String>,
    hasConstantValue: Boolean,
    private val lazyRemoteString: LazyRemoteString,
) : RemoteString(hasConstantValue), MutableRemoteState<String> {

    constructor(
        content: MutableState<String>,
        id: Int? = null,
    ) : this(
        content,
        false,
        object : LazyRemoteString {
            // TODO: We should add a method that reserves a unique id
            override fun reserveTextId(creationState: RemoteComposeCreationState) =
                id ?: creationState.document.textCreateId(content.value)

            override fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState) =
                content.value.toCodePointSet()
        },
    )

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
        lazyRemoteString.reserveTextId(creationState)

    override fun computeRequiredCodePointSet(creationState: RemoteComposeCreationState) =
        lazyRemoteString.computeRequiredCodePointSet(creationState)

    override var value: String
        get() {
            return content.value
        }
        set(newValue) {
            content.value = newValue
        }

    override operator fun component1(): String = value

    override operator fun component2(): (String) -> Unit = { newValue -> content.value = newValue }
}

/**
 * A Composable function to remember and provide a **named** mutable remote string.
 *
 * @param name The unique name for this remote string, used for identification in the remote
 *   document.
 * @param domain The domain of the remote string (defaults to [RemoteDomains.USER]).
 * @param content A lambda that provides the initial [String] value for this remote string.
 * @return A [MutableRemoteString] instance that will be remembered across recompositions.
 */
@Composable
fun rememberRemoteString(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    content: () -> String,
): MutableRemoteString {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val string = content()
        val id = state.document.textCreateId(string)
        state.document.setStringName(id, "$domain:$name")
        MutableRemoteString(mutableStateOf(string), id)
    }
}

/**
 * A Composable function to remember and provide an anonymous (unnamed) mutable remote string.
 *
 * @param content A lambda that provides the initial [String] value for this remote string.
 * @return A [MutableRemoteString] instance that will be remembered across recompositions.
 */
@Composable
fun rememberRemoteString(content: () -> String): MutableRemoteString {
    val state = LocalRemoteComposeCreationState.current
    return remember {
        val string = content()
        val id = state.document.textCreateId(string)
        MutableRemoteString(mutableStateOf(string), id)
    }
}

/**
 * A convenience Composable function to remember a **system-level** named remote string.
 *
 * This is a specialized version of [rememberRemoteString] where the `domain` is fixed to
 * [RemoteDomains.SYSTEM].
 *
 * @param name The unique name for this system remote string.
 * @param content A lambda that provides the initial [String] value for this remote string.
 * @return A [MutableRemoteString] instance with a system domain, remembered across recompositions.
 */
@Composable
fun rememberSystemRemoteString(name: String, content: () -> String): MutableRemoteString =
    rememberRemoteString(name = name, domain = RemoteDomains.SYSTEM, content)
