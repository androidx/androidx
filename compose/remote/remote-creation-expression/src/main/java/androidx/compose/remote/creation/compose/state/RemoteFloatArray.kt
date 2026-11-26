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
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import kotlin.reflect.KClass

/** Represents an array of floats. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RemoteFloatArray : RemoteValue<List<RemoteFloat>> {
    override val constantValueOrNull: List<RemoteFloat>?

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteFloat] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: RemoteFloat): RemoteFloat

    /**
     * Array access operator for [RemoteFloatArray] with an [Int] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: Int): RemoteFloat

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteInt] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: RemoteInt): RemoteFloat

    public companion object {
        /** Creates a [RemoteFloatArray] referencing a remote ID. */
        public fun createForId(id: Int): RemoteFloatArray =
            RemoteFloatArrayImpl(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id))

        /**
         * Creates a named [RemoteFloatArray].
         *
         * @param name The unique name for this remote float array.
         * @param defaultValue The initial [FloatArray] value for the named array.
         * @param domain The domain of the named array (defaults to [RemoteValue.Domain.User]).
         * @return A [RemoteFloatArray] representing the named float array.
         */
        @JvmStatic
        public fun createNamedRemoteFloatArray(
            name: String,
            defaultValue: FloatArray,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
        ): RemoteFloatArray {
            return RemoteFloatArrayImpl(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
                idProvider = { creationState ->
                    val id = creationState.document.addFloatArray(defaultValue).id
                    creationState.document.setNamedVariable(
                        id,
                        domain.prefixed(name),
                        NamedVariable.FLOAT_ARRAY_TYPE,
                    )
                    id
                },
            )
        }
    }
}

/**
 * Returns an ID for this array represented as an ID list collection.
 *
 * Writes each float element as a variable into the document and collects their IDs into a
 * [androidx.compose.remote.core.operations.DataListIds], suitable for pattern iteration.
 *
 * @param creationState creation state associated with the document being written
 * @return document ID allocated for the ID list
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloatArray.getIdListForCreationState(
    creationState: RemoteComposeCreationState
): Int =
    when (this) {
        is RemoteFloatArrayImpl -> getIdListForCreationState(creationState)
    }

private fun RemoteFloatArray.arrayForCreationState(
    creationState: RemoteComposeCreationState
): FloatArray {
    return creationState.getOrPutFloatArray(cacheKey) {
        floatArrayOf(creationState.getFloatId(this))
    }
}

private enum class FloatArrayOperationKey : RemoteOperation {
    Create {
        override val resultKind: RemoteValueKind
            get() = RemoteValueKind.FLOAT_ARRAY

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "arrayOf(${args.joinToDebugString()})"

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
            RemoteFloatArray(args.fastMap { it as RemoteFloat })
    },
    Get {
        override val precedence: Int
            get() = 100

        override val resultKind: RemoteValueKind
            get() = RemoteValueKind.FLOAT

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            args.formatArrayAccess(precedence)

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
            val array = args[0] as RemoteFloatArray
            return when (val index = args[1]) {
                is RemoteFloat -> array[index]
                is RemoteInt -> array[index]
                else -> throw IllegalArgumentException("Unsupported index type: $index")
            }
        }

        override fun canFailOnOperandIndex(operandIndex: Int): Boolean = operandIndex == 1
    },
    IdList {
        override val resultKind: RemoteValueKind
            get() = RemoteValueKind.FLOAT_ARRAY

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "${args[0].toDebugString()}.asIdList()"

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> = args[0]
    },
}

private class RemoteFloatArrayImpl(
    override val constantValueOrNull: List<RemoteFloat>?,
    cacheKey: RemoteStateCacheKey,
    private val idProvider: ((creationState: RemoteComposeCreationState) -> Int)? = null,
) : BaseRemoteValue<List<RemoteFloat>>(cacheKey), RemoteFloatArray {
    override val equalityType: KClass<*>
        get() = RemoteFloatArray::class

    override val valueKind: RemoteValueKind
        get() = RemoteValueKind.FLOAT_ARRAY

    override fun createForIdOrNull(id: Int): RemoteValue<*> = RemoteFloatArray.createForId(id)

    private val idListCacheKey: RemoteStateCacheKey =
        RemoteOperationCacheKey.create(FloatArrayOperationKey.IdList, this)

    override fun get(v: RemoteFloat): RemoteFloat {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex.toInt()]
        }
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(FloatArrayOperationKey.Get, this, v),
        ) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                *v.arrayForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    override fun get(v: Int): RemoteFloat =
        constantValueOrNull?.get(v) ?: get(RemoteFloat(v.toFloat()))

    override fun get(v: RemoteInt): RemoteFloat {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex]
        }
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(FloatArrayOperationKey.Get, this, v),
        ) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                creationState.getFloatId(v),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        // If this instance represents an existing allocated ID (e.g. a formal parameter in a
        // pattern definition), return that ID directly instead of writing to the document.
        if (cacheKey is RemoteStateIdKey) {
            return cacheKey.id
        }
        if (idProvider != null) {
            return idProvider.invoke(creationState)
        }
        val asFloat = constantValueOrNull!!.fastMap { creationState.getFloatId(it) }.toFloatArray()
        return creationState.document.addFloatArray(asFloat).id
    }

    fun getIdListForCreationState(creationState: RemoteComposeCreationState): Int {
        if (cacheKey is RemoteStateIdKey) {
            return cacheKey.id
        }
        return creationState.getOrPutVariableId(idListCacheKey) {
            val ids = constantValueOrNull!!.fastMap { creationState.getId(it) }.toIntArray()
            creationState.document.addIdList(ids).id
        }
    }
}

/** Creates a [RemoteFloatArray] backed by [constantValueOrNull]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloatArray(constantValueOrNull: List<RemoteFloat>?): RemoteFloatArray =
    RemoteFloatArrayImpl(
        constantValueOrNull,
        constantValueOrNull?.let { values ->
            RemoteOperationCacheKey.create(FloatArrayOperationKey.Create, *values.toTypedArray())
        } ?: RemoteStateInstanceKey(),
    )

/** Creates a [RemoteFloatArray] from constant [Float] [values]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloatArray(vararg values: Float): RemoteFloatArray =
    RemoteFloatArray(values.map { RemoteFloat(it) })

/** Creates a [RemoteFloatArray] from [RemoteFloat] [values]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteFloatArray(vararg values: RemoteFloat): RemoteFloatArray =
    RemoteFloatArray(values.toList())
