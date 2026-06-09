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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/** Represents an array of remote strings. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteStringArray
internal constructor(
    public override val constantValueOrNull: List<RemoteString>?,
    internal override val cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<List<RemoteString>>(cacheKey) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        constantValueOrNull: List<RemoteString>?
    ) : this(
        constantValueOrNull,
        constantValueOrNull?.let { values ->
            RemoteOperationCacheKey.create(OperationKey.Create, *values.toTypedArray())
        } ?: RemoteStateInstanceKey(),
    )

    internal enum class OperationKey : DebuggableOperation {
        Create {
            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                "arrayOf(${args.joinToDebugString()})"
        },
        Get {
            override val precedence: Int
                get() = 100

            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                args.formatArrayAccess(precedence)
        },
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        val ids =
            constantValueOrNull!!.fastMap { it.getIdForCreationState(creationState) }.toIntArray()
        val nanId = creationState.document.addStringList(*ids)
        return Utils.idFromNan(nanId)
    }

    /** Array access operator for [RemoteStringArray] with an [Int] index. */
    public operator fun get(v: Int): RemoteString {
        return constantValueOrNull!![v]
    }

    /** Array access operator for [RemoteStringArray] with a [RemoteInt] index. */
    public operator fun get(v: RemoteInt): RemoteString {
        v.constantValueOrNull?.let {
            return constantValueOrNull!![it]
        }
        return MutableRemoteString(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Get, this, v),
            lazyRemoteString = StringArrayDerefImpl(this, v),
        )
    }
}

private class StringArrayDerefImpl(val array: RemoteStringArray, val index: RemoteInt) :
    LazyRemoteString {
    override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
        val arrayId = Utils.asNan(array.getIdForCreationState(creationState))
        val indexId = index.getIdForCreationState(creationState)
        return creationState.document.textLookup(arrayId, indexId)
    }

    override fun computeRequiredCodePointSet(
        creationState: RemoteComposeCreationState
    ): Set<String>? {
        val constIndex = index.constantValueOrNull
        if (constIndex != null && array.constantValueOrNull != null) {
            return array.constantValueOrNull[constIndex].computeRequiredCodePointSet(creationState)
        }
        if (array.constantValueOrNull != null) {
            var merged: Set<String>? = HashSet()
            array.constantValueOrNull.fastForEach { str ->
                merged = mergeSets(merged, str.computeRequiredCodePointSet(creationState))
            }
            return merged
        }
        return null
    }
}
