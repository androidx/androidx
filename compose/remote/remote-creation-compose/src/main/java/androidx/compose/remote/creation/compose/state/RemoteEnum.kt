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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastMap
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

/**
 * A class representing a remote enum value.
 *
 * [RemoteInt] internally stores its state as a [RemoteInt], using the Enum ordinal.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteEnum<T : Enum<T>>(
    internal val intValue: RemoteInt,
    internal val enumEntries: EnumEntries<T>,
) : BaseRemoteState<T>() {
    override val cacheKey: RemoteStateCacheKey
        get() = intValue.cacheKey

    @get:Suppress("AutoBoxing")
    public override val constantValueOrNull: T?
        get() = intValue.constantValueOrNull?.let { enumEntries[it] }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val asEncoded: RemoteInt
        get() = intValue

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

    internal enum class OperationKey {
        ToString
    }

    /**
     * Constructor for creating a [RemoteEnum] instance from a standard [Enum].
     *
     * It converts the standard enum value into a [RemoteInt] using the Enum ordinal.
     *
     * @param value The standard enum value to convert.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(value: T, enumEntries: EnumEntries<T>) : this(value.ordinal.ri, enumEntries)

    /**
     * Converts this [RemoteEnum] to its underlying [RemoteInt] representation, using the Enum
     * ordinal.
     *
     * @return The [RemoteInt] that holds the enum\'s value.
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

        val strings = enumEntries.fastMap(mapping).toTypedArray()

        return MutableRemoteString(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.ToString, this),
            object : LazyRemoteString {
                override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
                    val stringIds =
                        IntArray(strings.size) { strings[it].getIdForCreationState(creationState) }
                    return creationState.document.textLookup(
                        creationState.document.addStringList(*stringIds),
                        intValue.getIdForCreationState(creationState),
                    )
                }

                override fun computeRequiredCodePointSet(
                    creationState: RemoteComposeCreationState
                ) = buildSet {
                    strings.forEach {
                        val codePointSet =
                            it.computeRequiredCodePointSet(creationState) ?: return@buildSet
                        addAll(codePointSet)
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
            intValue.eq(value.ordinal.ri).select(mapping(value), acc)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteEnum] from a literal constant.
         *
         * @param value The constant [Enum] value.
         * @return A [RemoteEnum] representing the constant enum.
         */
        public inline operator fun <reified T : Enum<T>> invoke(value: T): RemoteEnum<T> =
            RemoteEnum(value = value, enumEntries())

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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public inline fun <reified T : Enum<T>> createNamedRemoteEnum(
            name: String,
            defaultValue: T,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteEnum<T> {
            return RemoteEnum(
                RemoteInt.createNamedRemoteInt(
                    name = name,
                    defaultValue = defaultValue.ordinal,
                    domain = domain,
                ),
                enumEntries(),
            )
        }
    }
}

/** A mutable implementation of [RemoteEnum]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableRemoteEnum<T : Enum<T>>(
    public val remoteInt: MutableRemoteInt,
    enumEntries: EnumEntries<T>,
) : RemoteEnum<T>(remoteInt, enumEntries), MutableRemoteState<T> {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override val asEncodedMutable: MutableRemoteInt
        get() = intValue as MutableRemoteInt

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        intValue.writeToDocument(creationState)

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

        /**
         * Creates a [MutableRemoteEnum] with an initial value.
         *
         * @param initialValue The initial value for this mutable enum.
         * @return A [MutableRemoteEnum] instance.
         */
        public inline fun <reified T : Enum<T>> createMutable(
            initialValue: T
        ): MutableRemoteEnum<T> =
            MutableRemoteEnum(MutableRemoteInt.createMutable(initialValue.ordinal), enumEntries())
    }
}

/**
 * Factory composable for mutable remote enum state.
 *
 * @param initialValue The initial [Enum] value.
 * @return A [MutableRemoteEnum] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
public inline fun <reified T : Enum<T>> rememberMutableRemoteEnum(
    initialValue: T
): MutableRemoteEnum<T> {
    return remember {
        MutableRemoteEnum(MutableRemoteInt.createMutable(initialValue.ordinal), enumEntries())
    }
}

/**
 * Remembers a named remote enum expression.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param initialValue The initial [Enum] value.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteEnum] instance representing the named expression.
 */
@Composable
@RemoteComposable
public inline fun <reified T : Enum<T>> rememberNamedRemoteEnum(
    name: String,
    initialValue: T,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteEnum<T> {
    return rememberNamedRemoteEnum(
        name = name,
        initialValue = initialValue,
        enumEntries = enumEntries(),
        domain = domain,
    )
}

@Composable
@RemoteComposable
public fun <T : Enum<T>> rememberNamedRemoteEnum(
    name: String,
    initialValue: T,
    enumEntries: EnumEntries<T>,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteEnum<T> {
    return rememberNamedState(name, domain) {
        RemoteEnum(RemoteInt.createNamedRemoteInt(name, initialValue.ordinal, domain), enumEntries)
    }
}
