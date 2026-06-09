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
        val asFloat =
            with(creationState) { constantValueOrNull!!.fastMap { it.floatId }.toFloatArray() }
        return Utils.idFromNan(creationState.document.addFloatArray(asFloat))
    }

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteFloat] index. Performs a
     * dereference operation on a remote float array.
     */
    public operator fun get(v: RemoteFloat): RemoteFloat {
        v.constantValueOrNull?.let {
            return constantValueOrNull!![it.toInt()]
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
    public operator fun get(v: Int): RemoteFloat = constantValueOrNull!![v]

    /**
     * Array access operator for [RemoteFloatArray] with a [RemoteInt] index. Performs a dereference
     * operation on a remote float array.
     */
    public operator fun get(v: RemoteInt): RemoteFloat {
        v.constantValueOrNull?.let {
            return constantValueOrNull!![it]
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
