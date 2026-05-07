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
import androidx.collection.MutableIntObjectMap
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState

/**
 * Represents a mutable array of floats.
 *
 * @property size The size of the array
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteMutableFloatArray
/** Constructs a [RemoteMutableFloatArray] with [size] elements which are initialized to 0 */
constructor(public val size: Int) : BaseRemoteState<List<RemoteFloat>>() {

    override val cacheKey: RemoteStateCacheKey = RemoteStateInstanceKey()

    public override val constantValueOrNull: List<RemoteFloat>? = null

    private class PendingSet(val index: RemoteInt, val value: RemoteFloat, val generation: Int)

    private val pendingSetList = mutableListOf<PendingSet>()
    private val constantValueCache =
        MutableIntObjectMap<RemoteFloat>().apply {
            val zero = RemoteFloat(0f)
            for (i in 0 until this@RemoteMutableFloatArray.size) {
                this[i] = zero
            }
        }
    private var generation = 0

    internal enum class OperationKey {
        Create,
        Get,
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return Utils.idFromNan(creationState.document.addDynamicFloatArray(size.toFloat()))
    }

    /**
     * Array access operator for [RemoteMutableFloatArray] with an [Int] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: Int): RemoteFloat = get(RemoteInt(v))

    /**
     * Array access operator for [RemoteMutableFloatArray] with a [RemoteInt] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: RemoteInt): RemoteFloat {
        if (v.hasConstantValue) {
            constantValueCache[v.constantValue]?.let {
                return it
            }
        }

        val currentGeneration = generation
        return UncachedRemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey =
                RemoteOperationCacheKey.create(
                    OperationKey.Get,
                    this,
                    v,
                    RemoteConstantCacheKey(currentGeneration),
                ),
        ) { creationState ->
            maybeWritePendingSets(currentGeneration, creationState)

            floatArrayOf(
                *arrayForCreationState(creationState),
                v.getFloatIdForCreationState(creationState),
                AnimatedFloatExpression.A_DEREF,
            )
        }
    }

    public operator fun set(index: Int, v: RemoteFloat) {
        constantValueCache[index] = v
        pendingSetList.add(PendingSet(RemoteInt(index), v, generation++))
    }

    public operator fun set(index: RemoteInt, v: RemoteFloat) {
        if (index.hasConstantValue) {
            constantValueCache[index.constantValue] = v
        } else {
            // We've no idea which index is getting written to, so we have to throw away cached
            // values.
            constantValueCache.clear()
        }
        pendingSetList.add(PendingSet(index, v, generation++))
    }

    private fun maybeWritePendingSets(upTo: Int, creationState: RemoteComposeCreationState) {
        val iterator = pendingSetList.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element.generation <= upTo) {
                iterator.remove()
                creationState.document.setArrayValue(
                    getIdForCreationState(creationState),
                    element.index.getFloatIdForCreationState(creationState),
                    element.value.getFloatIdForCreationState(creationState),
                )
            }
        }
    }

    private fun arrayForCreationState(creationState: RemoteComposeCreationState): FloatArray {
        return creationState.getOrPutFloatArray(cacheKey) {
            floatArrayOf(getFloatIdForCreationState(creationState))
        }
    }
}
