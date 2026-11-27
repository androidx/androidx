/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.remote.document.RemoteArrayId
import kotlin.reflect.KClass

/** Represents an array of remote integers. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RemoteIntArray : RemoteValue<List<RemoteInt>> {
    override val constantValueOrNull: List<RemoteInt>?

    /** Array access operator for [RemoteIntArray] with an [Int] index. */
    public operator fun get(v: Int): RemoteInt

    /**
     * Array access operator for [RemoteIntArray] with a [RemoteInt] index. Performs an ID lookup on
     * a remote integer array.
     */
    public operator fun get(v: RemoteInt): RemoteInt

    /**
     * Array access operator for [RemoteIntArray] with a [RemoteFloat] index. Performs an ID lookup
     * on a remote integer array.
     */
    public operator fun get(v: RemoteFloat): RemoteInt

    public companion object {
        /** Creates a [RemoteIntArray] referencing a remote ID. */
        public fun createForId(id: Int): RemoteIntArray =
            RemoteIntArrayImpl(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id))
    }
}

internal enum class IntArrayOperationKey : RemoteOperation {
    Create {
        override val resultKind: RemoteValueKind
            get() = RemoteValueKind.INT_ARRAY

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "arrayOf(${args.joinToDebugString()})"

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
            RemoteIntArray(args.fastMap { it as RemoteInt })
    },
    Get {
        override val precedence: Int
            get() = 100

        override val resultKind: RemoteValueKind
            get() = RemoteValueKind.INT

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            args.formatArrayAccess(precedence)

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> {
            val array = args[0] as RemoteIntArray
            return when (val index = args[1]) {
                is RemoteInt -> array[index]
                is RemoteFloat -> array[index]
                else -> throw IllegalArgumentException("Unsupported index type: $index")
            }
        }

        override fun canFailOnOperandIndex(operandIndex: Int): Boolean = operandIndex == 1
    },
}

private class RemoteIntArrayImpl(
    override val constantValueOrNull: List<RemoteInt>?,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteValue<List<RemoteInt>>(cacheKey), RemoteIntArray {
    override val equalityType: KClass<*>
        get() = RemoteIntArray::class

    override val valueKind: RemoteValueKind
        get() = RemoteValueKind.INT_ARRAY

    override fun createForIdOrNull(id: Int): RemoteValue<*> = RemoteIntArray.createForId(id)

    override fun get(v: Int): RemoteInt = constantValueOrNull?.get(v) ?: get(RemoteInt(v))

    override fun get(v: RemoteInt): RemoteInt {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex]
        }
        return RemoteInt(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(IntArrayOperationKey.Get, this, v),
        ) { creationState ->
            val arrayId = RemoteArrayId(getId(creationState))
            val resultId =
                creationState.document.idLookup(
                    arrayId,
                    creationState.getMaybeVariableFloat(v),
                )
            longArrayOf(resultId.toLong() + 0x100000000L)
        }
    }

    override fun get(v: RemoteFloat): RemoteInt {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex.toInt()]
        }
        return RemoteInt(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(IntArrayOperationKey.Get, this, v),
        ) { creationState ->
            val arrayId = RemoteArrayId(getId(creationState))
            val resultId =
                creationState.document.idLookup(
                    arrayId,
                    creationState.getMaybeVariableFloat(v),
                )
            longArrayOf(resultId.toLong() + 0x100000000L)
        }
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        // If this instance represents an existing allocated ID (e.g. a formal parameter in a
        // pattern definition), return that ID directly instead of writing to the document.
        if (cacheKey is RemoteStateIdKey) {
            return cacheKey.id
        }
        val values = constantValueOrNull!!.fastMap { creationState.getId(it) }.toIntArray()
        return creationState.document.addIdList(values).id
    }
}

/** Creates a [RemoteIntArray] backed by [constantValueOrNull]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteIntArray(constantValueOrNull: List<RemoteInt>?): RemoteIntArray =
    RemoteIntArrayImpl(
        constantValueOrNull,
        constantValueOrNull?.let { values ->
            RemoteOperationCacheKey.create(IntArrayOperationKey.Create, *values.toTypedArray())
        } ?: RemoteStateInstanceKey(),
    )

/** Creates a [RemoteIntArray] from constant [Int] [values]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteIntArray(vararg values: Int): RemoteIntArray =
    RemoteIntArray(values.map { RemoteInt(it) })
