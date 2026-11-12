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

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public open class RemoteBoolean
internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val intValue: RemoteInt,
    internal val floatComparison: SelectFloatCondition.FloatComparison? = null,
    internal val intComparison: SelectIntCondition.IntComparison? = null,
) : BaseRemoteState<Boolean>(RemoteStateInstanceKey()) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val cacheKey: RemoteStateCacheKey
        get() = intValue.cacheKey

    internal enum class OperationKey : RemoteOperation {
        SelectString,
        SelectFloat,
        SelectInt,
        SelectBoolean;

        override val precedence: Int
            get() = 0

        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            val condStr =
                when (val cond = args[0].toOperandString(1)) {
                    "1" -> "true"
                    "0" -> "false"
                    else -> cond
                }
            return "$condStr ? ${args[1].toOperandString(0)} : ${args[2].toOperandString(0)}"
        }

        override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
            return when (this) {
                SelectString ->
                    RemoteBoolean(args[0] as RemoteInt)
                        .select(args[1] as RemoteString, args[2] as RemoteString)
                SelectFloat ->
                    RemoteBoolean(args[0] as RemoteInt)
                        .select(args[1] as RemoteFloat, args[2] as RemoteFloat)
                SelectInt ->
                    RemoteBoolean(args[0] as RemoteInt)
                        .select(args[1] as RemoteInt, args[2] as RemoteInt)
                SelectBoolean ->
                    RemoteBoolean(args[0] as RemoteInt)
                        .select(
                            RemoteBoolean(args[1] as RemoteInt),
                            RemoteBoolean(args[2] as RemoteInt),
                        )
                        .intValue
            }
        }
    }

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Boolean?
        get() = intValue.constantValueOrNull?.let { it != 0 }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val asEncoded: RemoteInt
        get() = intValue

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    public operator fun not(): RemoteBoolean = RemoteBoolean(intValue xor RemoteInt(1))

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String {
        val str = super.toDebugString()
        return when (str) {
            "1" -> "true"
            "0" -> "false"
            else -> str
        }
    }

    public fun toRemoteInt(): RemoteInt = intValue

    public fun select(ifTrue: RemoteString, ifFalse: RemoteString): RemoteString {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        return MutableRemoteString(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(OperationKey.SelectString, this, ifTrue, ifFalse),
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    return creationState.document.textLookup(
                        creationState.document.addStringList(
                            ifFalse.getIdForCreationState(creationState),
                            ifTrue.getIdForCreationState(creationState),
                        ),
                        intValue.getIdForCreationState(creationState),
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    if (hasConstantValue) {
                        val selected = if (constantValue) ifTrue else ifFalse
                        return selected.computeRequiredCodePointSet(creationState)
                    }
                    return mergeSets(
                        ifTrue.computeRequiredCodePointSet(creationState),
                        ifFalse.computeRequiredCodePointSet(creationState),
                    )
                }
            },
        )
    }

    public fun select(ifTrue: RemoteFloat, ifFalse: RemoteFloat): RemoteFloat {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        val condition = floatComparison ?: SelectFloatCondition.BooleanCondition(this)
        return RemoteFloatSelect(
            condition = condition,
            ifTrue = ifTrue,
            ifFalse = ifFalse,
            cacheKey =
                RemoteOperationCacheKey.create(OperationKey.SelectFloat, this, ifTrue, ifFalse),
        )
    }

    public fun select(ifTrue: RemoteInt, ifFalse: RemoteInt): RemoteInt {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        val condition = intComparison ?: SelectIntCondition.BooleanCondition(this)
        return RemoteIntSelect(
            condition = condition,
            ifTrue = ifTrue,
            ifFalse = ifFalse,
            cacheKey =
                RemoteOperationCacheKey.create(OperationKey.SelectInt, this, ifTrue, ifFalse),
        )
    }

    public fun select(ifTrue: RemoteBoolean, ifFalse: RemoteBoolean): RemoteBoolean {
        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        // Peephole: both branches are constants
        val ifTrueConst = ifTrue.constantValueOrNull
        val ifFalseConst = ifFalse.constantValueOrNull
        if (ifTrueConst != null && ifFalseConst != null) {
            if (ifTrueConst == ifFalseConst) {
                return ifTrue
            }
            // flag ? true : false  =>  flag
            // flag ? false : true  =>  !flag
            return if (ifTrueConst) this else !this
        }

        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        // Peephole: flag ? flag : ifFalse
        if (cacheKey == ifTrue.cacheKey) {
            // flag ? flag : false  =>  flag
            if (ifFalseConst == false) return this
            // flag ? flag : true   =>  true
            if (ifFalseConst == true) return ifFalse
        }

        // Peephole: flag ? ifTrue : flag
        if (cacheKey == ifFalse.cacheKey) {
            // flag ? true : flag   =>  flag
            if (ifTrueConst == true) return this
            // flag ? false : flag  =>  false
            if (ifTrueConst == false) return ifTrue
        }

        return RemoteBoolean(
            RemoteInt(
                constantValueOrNull = null,
                cacheKey =
                    RemoteOperationCacheKey.create(
                        OperationKey.SelectBoolean,
                        this,
                        ifTrue,
                        ifFalse,
                    ),
                arrayProvider = { creationState ->
                    combineToLongArray(
                        creationState,
                        arrayOf(ifFalse.intValue, ifTrue.intValue, intValue),
                        0x100000000L + IntegerExpressionEvaluator.I_IFELSE,
                    )
                },
            )
        )
    }

    /**
     * If this [RemoteBoolean] evaluates to `true` then the returned value evaluates to [ifTrue]
     * otherwise it evaluates to [ifFalse].
     *
     * @param ifTrue The color to be selected if this boolean is `true` (as an [Int] representing an
     *   ARGB color).
     * @param ifFalse The color to be selected if this boolean is `false` (as an [Int] representing
     *   an ARGB color).
     * @return A new [RemoteColor] representing the conditionally selected color.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun select(@ColorInt ifTrue: Int, @ColorInt ifFalse: Int): RemoteColor {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                RemoteColor(ifTrue)
            } else {
                RemoteColor(ifFalse)
            }
        }

        if (ifTrue == ifFalse) {
            return RemoteColor(ifTrue)
        }

        return lerp(ifFalse, ifTrue, intValue.toRemoteFloat())
    }

    public fun select(ifTrue: RemoteColor, ifFalse: RemoteColor): RemoteColor {
        intValue.constantValueOrNull?.let {
            return if (it != 0) {
                ifTrue
            } else {
                ifFalse
            }
        }

        if (ifTrue.cacheKey == ifFalse.cacheKey) {
            return ifTrue
        }

        return lerp(ifFalse, ifTrue, intValue.toRemoteFloat())
    }

    public fun select(ifTrue: RemoteDp, ifFalse: RemoteDp): RemoteDp {
        return select(ifTrue.value, ifFalse.value).asRemoteDp()
    }

    public fun isEqualTo(other: RemoteBoolean): RemoteBoolean = intValue.isEqualTo(other.intValue)

    public fun isNotEqualTo(other: RemoteBoolean): RemoteBoolean =
        intValue.isNotEqualTo(other.intValue)

    public infix fun or(other: RemoteBoolean): RemoteBoolean =
        RemoteBoolean(intValue or other.intValue)

    public infix fun and(other: RemoteBoolean): RemoteBoolean =
        RemoteBoolean(intValue and other.intValue)

    public infix fun xor(other: RemoteBoolean): RemoteBoolean =
        RemoteBoolean(intValue xor other.intValue)

    public companion object {
        /**
         * Creates a [RemoteBoolean] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteInt]).
         * @return A [RemoteBoolean] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteBoolean =
            RemoteBoolean(RemoteInt.createForId(0x100000000L + id))

        @JvmStatic
        public fun createNamedRemoteBoolean(
            name: String,
            defaultValue: Boolean,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteBoolean {
            return RemoteBoolean(
                createNamedRemoteInt(
                    name = name,
                    defaultValue = if (defaultValue) 1 else 0,
                    domain = domain,
                )
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableRemoteBoolean
@RememberInComposition
internal constructor(remoteInt: MutableRemoteInt) :
    RemoteBoolean(remoteInt), MutableRemoteState<Boolean> {

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: Boolean?
        get() =
            when (intValue.constantValueOrNull) {
                0 -> false
                is Int -> true
                null -> null
            }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val asEncodedMutable: MutableRemoteInt
        get() = intValue as MutableRemoteInt

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    internal companion object {
        /**
         * Creates a [MutableRemoteBoolean] for a given ID.
         *
         * @param id The ID for this mutable boolean.
         * @return A [MutableRemoteBoolean] instance.
         */
        internal fun createMutableForId(id: Long): MutableRemoteBoolean =
            MutableRemoteBoolean(MutableRemoteInt.createMutableForId(id))
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteBoolean(value: Boolean): RemoteBoolean =
    RemoteBoolean(
        if (value) {
            RemoteInt(1)
        } else {
            RemoteInt(0)
        }
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RememberInComposition
public fun MutableRemoteBoolean(initialValue: Boolean): MutableRemoteBoolean =
    MutableRemoteBoolean(MutableRemoteInt(if (initialValue) 1 else 0))

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Boolean.rb: RemoteBoolean
    get() {
        return RemoteBoolean(this)
    }
