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
import androidx.compose.remote.document.RemoteIntId
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries
import kotlin.reflect.KClass

/**
 * A class representing a remote enum value.
 *
 * [RemoteEnum] internally stores its state as a [RemoteInt], using the Enum ordinal.
 *
 * Comparing [RemoteEnum] values with `==` checks structural equality (matching constant values or
 * expression DAGs, as well as [enumEntries] and mutability), whereas [MutableRemoteEnum] instances
 * compare by their underlying mutable variable and [enumEntries]; see [RemoteValue].
 *
 * @param T The enum type.
 * @property enumEntries The [EnumEntries] containing all values of the enum.
 */
@Stable
public sealed interface RemoteEnum<T : Enum<T>> : RemoteValue<T> {
    /** All entries of the enum type [T]. */
    public val enumEntries: EnumEntries<T>

    @get:Suppress("AutoBoxing") public override val constantValueOrNull: T?

    /**
     * Converts this [RemoteEnum] to its underlying [RemoteInt] representation, using the Enum
     * ordinal.
     *
     * @return The [RemoteInt] that holds the enum's value.
     */
    public val ordinal: RemoteInt

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
    public fun toRemoteString(mapping: (T) -> RemoteString = { it.toString().rs }): RemoteString

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
    public fun toRemoteInt(mapping: (T) -> RemoteInt): RemoteInt

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
         * @param domain The domain of the named enum (defaults to [RemoteValue.Domain.User]).
         * @param defaultValue The initial [Enum] value for the named remote enum.
         * @return A [RemoteEnum] representing the named enum.
         */
        @JvmStatic
        public inline fun <reified T : Enum<T>> createNamedRemoteEnum(
            name: String,
            defaultValue: T,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
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

internal val <T : Enum<T>> RemoteEnum<T>.intValue: RemoteInt
    get() = (this as BaseRemoteEnum<T>).intValue

private abstract class BaseRemoteEnum<T : Enum<T>>(override val enumEntries: EnumEntries<T>) :
    BaseRemoteValue<T>(RemoteStateInstanceKey()), RemoteEnum<T> {
    override val equalityType: KClass<*>
        get() = RemoteEnum::class

    abstract val intValue: RemoteInt

    override val cacheKey: RemoteStateCacheKey
        get() = constantValueOrNull?.let { RemoteConstantCacheKey(it) } ?: intValue.cacheKey

    override fun equals(other: Any?): Boolean =
        super.equals(other) && other is BaseRemoteEnum<*> && enumEntries == other.enumEntries

    override fun hashCode(): Int = 31 * super.hashCode() + enumEntries.hashCode()

    override val asEncoded: RemoteInt
        get() = intValue

    override val constantValueOrNull: T?
        get() = intValue.constantValueOrNull?.let { enumEntries[it] }

    override val ordinal: RemoteInt
        get() = intValue

    override fun toRemoteString(mapping: (T) -> RemoteString): RemoteString {
        constantValueOrNull?.let {
            return mapping(it)
        }

        val stringArray = RemoteStringArray(enumEntries.fastMap(mapping))

        return RemoteString(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(EnumOperationKey.ToString, this, stringArray),
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    val strings = stringArray.constantValueOrNull!!
                    val stringIds = IntArray(strings.size) { creationState.getId(strings[it]) }
                    return creationState.document
                        .textLookup(
                            creationState.document.addIdList(stringIds),
                            RemoteIntId(creationState.getId(intValue)),
                        )
                        .id
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

    override fun toRemoteInt(mapping: (T) -> RemoteInt): RemoteInt {
        constantValueOrNull?.let {
            return mapping(it)
        }

        return enumEntries.fastFold((-1).ri) { acc, value ->
            intValue.isEqualTo(value.ordinal.ri).select(mapping(value), acc)
        }
    }

    override fun toString(): String = constantValueOrNull?.name ?: cacheKey.toDebugString()

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)
}

private class RemoteEnumImpl<T : Enum<T>>(
    override val intValue: RemoteInt,
    enumEntries: EnumEntries<T>,
) : BaseRemoteEnum<T>(enumEntries)

private enum class EnumOperationKey : RemoteOperation {
    ToString;

    override val precedence: Int
        get() = 100

    override fun toDebugString(args: List<RemoteStateCacheKey>): String {
        return "${args[0].toOperandString(precedence)}.toRemoteString()"
    }

    override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
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
 * Creates a [RemoteEnum] backed by [intValue] and [enumEntries].
 *
 * @param intValue The [RemoteInt] representing the enum ordinal.
 * @param enumEntries The [EnumEntries] containing all values of the enum.
 */
public fun <T : Enum<T>> RemoteEnum(
    intValue: RemoteInt,
    enumEntries: EnumEntries<T>,
): RemoteEnum<T> = RemoteEnumImpl(intValue, enumEntries)

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
 * Two [MutableRemoteEnum] instances are `==` if and only if they have the same [enumEntries] and
 * reference the same underlying mutable variable (for example, wrapping the same [remoteInt]);
 * separately created instances (e.g., via `MutableRemoteEnum(initialValue)`) represent distinct
 * mutable variables and are never `==`.
 *
 * @param T The enum type.
 */
@Stable
public sealed interface MutableRemoteEnum<T : Enum<T>> : RemoteEnum<T>, MutableRemoteValue<T> {
    public val remoteInt: MutableRemoteInt

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
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

private class MutableRemoteEnumImpl<T : Enum<T>>(
    override val remoteInt: MutableRemoteInt,
    enumEntries: EnumEntries<T>,
) : BaseRemoteEnum<T>(enumEntries), MutableRemoteEnum<T> {
    override val intValue: RemoteInt
        get() = remoteInt

    override val asEncodedMutable: MutableRemoteInt
        get() = remoteInt
}

/**
 * Creates a [MutableRemoteEnum] backed by [remoteInt] and [enumEntries].
 *
 * @param remoteInt The [MutableRemoteInt] representing the mutable enum ordinal.
 * @param enumEntries The [EnumEntries] containing all values of the enum.
 */
@RememberInComposition
public fun <T : Enum<T>> MutableRemoteEnum(
    remoteInt: MutableRemoteInt,
    enumEntries: EnumEntries<T>,
): MutableRemoteEnum<T> = MutableRemoteEnumImpl(remoteInt, enumEntries)

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
