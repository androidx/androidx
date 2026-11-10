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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

/**
 * A class representing a remote enum value.
 *
 * [RemoteEnum] internally stores its state as a [RemoteInt], using the Enum ordinal.
 *
 * @param T The enum type.
 * @property enumEntries The [EnumEntries] containing all values of the enum.
 */
@Stable
public open class RemoteEnum<T : Enum<T>>
/**
 * Constructs a [RemoteEnum] instance.
 *
 * @param intValue The [RemoteInt] representing the enum ordinal.
 * @param enumEntries The [EnumEntries] containing all values of the enum.
 * @param cacheKey The [RemoteStateCacheKey] identifying this state.
 */
internal constructor(
    internal val intValue: RemoteInt,
    /** All entries of the enum type [T]. */
    public val enumEntries: EnumEntries<T>,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<T>(cacheKey) {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override val cacheKey: RemoteStateCacheKey
        get() = constantValueOrNull?.let { RemoteConstantCacheKey(it) } ?: intValue.cacheKey

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: T?
        get() = intValue.constantValueOrNull?.let { enumEntries[it] }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String = constantValueOrNull?.name ?: super.toDebugString()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val asEncoded: RemoteInt
        get() = intValue

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    internal enum class OperationKey : RemoteOperation {
        ToString;

        override val precedence: Int
            get() = 100

        override fun toDebugString(args: List<RemoteStateCacheKey>): String {
            return "${args[0].toOperandString(precedence)}.toRemoteString()"
        }

        override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
            return when (this) {
                ToString -> {
                    val index =
                        when (val arg = args[0]) {
                            is RemoteEnum<*> -> arg.intValue
                            is RemoteInt -> arg
                            else -> throw IllegalArgumentException("Unsupported index: $arg")
                        }
                    val stringArray = args[1] as RemoteStringArray
                    stringArray[index]
                }
            }
        }
    }

    /**
     * Converts this [RemoteEnum] to its underlying [RemoteInt] representation, using the Enum
     * ordinal.
     *
     * @return The [RemoteInt] that holds the enum's value.
     */
    public val ordinal: RemoteInt
        get() = intValue

    /**
     * Converts this [RemoteEnum] to a [RemoteString] representation.
     *
     * This method maps each possible enum entry to a string using the provided [mapping] function.
     * At runtime, the [RemoteString] will resolve to the string corresponding to the current value
     * of the enum. Defaults to calling [Any.toString] on the enum entry.
     *
     * @param mapping A function that defines how each enum constant should be converted to a
     *   [RemoteString].
     * @return A [RemoteString] that tracks the string value of this enum.
     */
    public fun toRemoteString(mapping: (T) -> RemoteString = { it.toString().rs }): RemoteString {
        constantValueOrNull?.let {
            return mapping(it)
        }

        val stringArray = RemoteStringArray(enumEntries.fastMap(mapping))

        return MutableRemoteString(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToString, this, stringArray),
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    val strings = stringArray.constantValueOrNull!!
                    val stringIds =
                        IntArray(strings.size) { strings[it].getIdForCreationState(creationState) }
                    return creationState.document.textLookup(
                        creationState.document.addStringList(*stringIds),
                        intValue.getIdForCreationState(creationState),
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ): Set<String>? {
                    val strings = stringArray.constantValueOrNull ?: return null
                    return buildSet {
                        strings.fastForEach {
                            val codePointSet =
                                it.computeRequiredCodePointSet(creationState) ?: return null
                            addAll(codePointSet)
                        }
                    }
                }
            },
        )
    }

    /**
     * Converts this [RemoteEnum] to a [RemoteInt] representation using a provided mapping.
     *
     * This method maps each possible enum entry to a [RemoteInt] using the provided [mapping]
     * function. At runtime, the resulting [RemoteInt] will resolve to the value corresponding to
     * the current state of the enum.
     *
     * @param mapping A function that defines how each enum constant should be converted to a
     *   [RemoteInt].
     * @return A [RemoteInt] that tracks the integer value associated with this enum.
     */
    public fun toRemoteInt(mapping: (T) -> RemoteInt): RemoteInt {
        constantValueOrNull?.let {
            return mapping(it)
        }

        return enumEntries.fastFold((-1).ri) { acc, value ->
            intValue.isEqualTo(value.ordinal.ri).select(mapping(value), acc)
        }
    }

    public companion object {
        /**
         * Creates a [RemoteEnum] referencing a remote ID.
         *
         * @param id The remote ID (stored as a [RemoteInt]).
         * @return A [RemoteEnum] referencing the ID.
         */
        internal inline fun <reified T : Enum<T>> createForId(id: Int): RemoteEnum<T> =
            RemoteEnum(RemoteInt.createForId(0x100000000L + id), enumEntries())

        /**
         * Creates a named [RemoteEnum] with an initial value.
         *
         * @param name The unique name for this remote enum.
         * @param domain The domain of the named enum (defaults to [RemoteState.Domain.User]).
         * @param defaultValue The initial [Enum] value for the named remote enum.
         * @return A [RemoteEnum] representing the named enum.
         */
        @JvmStatic
        public inline fun <reified T : Enum<T>> createNamedRemoteEnum(
            name: String,
            defaultValue: T,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteEnum<T> {
            return RemoteEnum(
                createNamedRemoteInt(
                    name = name,
                    defaultValue = defaultValue.ordinal,
                    domain = domain,
                ),
                enumEntries(),
            )
        }
    }
}

/**
 * Creates a [RemoteEnum] backed by [intValue] and [enumEntries].
 *
 * @param T The enum type.
 * @param intValue The [RemoteInt] representing the enum ordinal.
 * @param enumEntries The [EnumEntries] containing all values of the enum.
 * @return A [RemoteEnum] wrapping the given [RemoteInt].
 */
public fun <T : Enum<T>> RemoteEnum(
    intValue: RemoteInt,
    enumEntries: EnumEntries<T>,
): RemoteEnum<T> = RemoteEnum(intValue, enumEntries, RemoteStateInstanceKey())

/**
 * Creates a [RemoteEnum] from a literal constant.
 *
 * @param T The enum type.
 * @param value The constant [Enum] value.
 * @return A [RemoteEnum] representing the constant enum.
 */
public inline fun <reified T : Enum<T>> RemoteEnum(value: T): RemoteEnum<T> =
    RemoteEnum(value.ordinal.ri, enumEntries())

/**
 * A mutable implementation of [RemoteEnum].
 *
 * @param T The enum type.
 */
public class MutableRemoteEnum<T : Enum<T>>
@RememberInComposition
internal constructor(
    public val remoteInt: MutableRemoteInt,
    enumEntries: EnumEntries<T>,
    cacheKey: RemoteStateCacheKey,
) : RemoteEnum<T>(remoteInt, enumEntries, cacheKey), MutableRemoteState<T> {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val asEncodedMutable: MutableRemoteInt
        get() = intValue as MutableRemoteInt

    internal companion object {
        /**
         * Creates a [MutableRemoteEnum] for a given ID.
         *
         * @param id The ID for this mutable enum.
         * @return A [MutableRemoteEnum] instance.
         */
        internal inline fun <reified T : Enum<T>> createMutableForId(
            id: Long
        ): MutableRemoteEnum<T> =
            MutableRemoteEnum(MutableRemoteInt.createMutableForId(id), enumEntries())
    }
}

/**
 * Creates a [MutableRemoteEnum] backed by [remoteInt] and [enumEntries].
 *
 * @param T The enum type.
 * @param remoteInt The [MutableRemoteInt] representing the mutable enum ordinal.
 * @param enumEntries The [EnumEntries] containing all values of the enum.
 * @return A [MutableRemoteEnum] wrapping the given [MutableRemoteInt].
 */
@RememberInComposition
public fun <T : Enum<T>> MutableRemoteEnum(
    remoteInt: MutableRemoteInt,
    enumEntries: EnumEntries<T>,
): MutableRemoteEnum<T> = MutableRemoteEnum(remoteInt, enumEntries, RemoteStateInstanceKey())

/**
 * Creates a [MutableRemoteEnum] with an initial value.
 *
 * @param T The enum type.
 * @param initialValue The initial value for this mutable enum.
 * @return A [MutableRemoteEnum] instance.
 */
@RememberInComposition
public inline fun <reified T : Enum<T>> MutableRemoteEnum(initialValue: T): MutableRemoteEnum<T> =
    MutableRemoteEnum(MutableRemoteInt(initialValue.ordinal), enumEntries())
