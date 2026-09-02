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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.ui.util.fastMap

/** Represents an array of floats. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteFloatArray
internal constructor(
    public override val constantValueOrNull: List<RemoteFloat>?,
    internal override val cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<List<RemoteFloat>>(cacheKey) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        constantValueOrNull: List<RemoteFloat>?
    ) : this(
        constantValueOrNull,
        constantValueOrNull?.let { values ->
            RemoteOperationCacheKey.create(OperationKey.Create, *values.toTypedArray())
        } ?: RemoteStateInstanceKey(),
    )

    internal enum class OperationKey : RemoteOperation {
        Create {
            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                "arrayOf(${args.joinToDebugString()})"

            override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> =
                RemoteFloatArray(args.fastMap { it as RemoteFloat })
        },
        Get {
            override val precedence: Int
                get() = 100

            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                args.formatArrayAccess(precedence)

            override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
                val array = args[0] as RemoteFloatArray
                return when (val index = args[1]) {
                    is RemoteFloat -> array[index]
                    is RemoteInt -> array[index]
                    else -> throw IllegalArgumentException("Unsupported index type: $index")
                }
            }
        },
        IdList {
            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                "${args[0].toDebugString()}.asIdList()"

            override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> = args[0]
        },
    }

    private val idListCacheKey: RemoteStateCacheKey =
        RemoteOperationCacheKey.create(OperationKey.IdList, this)

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        // If this instance represents an existing allocated ID (e.g. a formal parameter in a
        // pattern definition), return that ID directly instead of writing to the document.
        if (cacheKey is RemoteStateIdKey) {
            return (cacheKey as RemoteStateIdKey).id
        }
        val asFloat =
            with(creationState) { constantValueOrNull!!.fastMap { it.floatId }.toFloatArray() }
        return Utils.idFromNan(creationState.document.addFloatArray(asFloat))
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
    internal fun getIdListForCreationState(creationState: RemoteComposeCreationState): Int {
        if (cacheKey is RemoteStateIdKey) {
            return (cacheKey as RemoteStateIdKey).id
        }
        return creationState.getOrPutVariableId(idListCacheKey) {
            val ids =
                constantValueOrNull!!
                    .fastMap { it.getIdForCreationState(creationState) }
                    .toIntArray()
            Utils.idFromNan(creationState.document.addList(ids))
        }
    }

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteFloat] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: RemoteFloat): RemoteFloat {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex.toInt()]
        }
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Get, this, v),
        ) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                *v.arrayForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    /**
     * Array access operator for [RemoteFloatArray] with an [Int] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: Int): RemoteFloat =
        constantValueOrNull?.get(v) ?: get(RemoteFloat(v.toFloat()))

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteInt] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: RemoteInt): RemoteFloat {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex]
        }
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Get, this, v),
        ) { creationState ->
            floatArrayOf(
                *arrayForCreationState(creationState),
                v.getFloatIdForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    private fun arrayForCreationState(creationState: RemoteComposeCreationState): FloatArray {
        return creationState.getOrPutFloatArray(cacheKey) {
            floatArrayOf(getFloatIdForCreationState(creationState))
        }
    }
}
