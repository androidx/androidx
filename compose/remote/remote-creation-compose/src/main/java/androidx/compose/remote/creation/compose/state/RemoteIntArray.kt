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
import androidx.compose.ui.util.fastMap

/** Represents an array of remote integers. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteIntArray
internal constructor(
    public override val constantValueOrNull: List<RemoteInt>?,
    internal override val cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<List<RemoteInt>>(cacheKey) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        constantValueOrNull: List<RemoteInt>?
    ) : this(
        constantValueOrNull,
        constantValueOrNull?.let { values ->
            RemoteOperationCacheKey.create(OperationKey.Create, *values.toTypedArray())
        } ?: RemoteStateInstanceKey(),
    )

    public constructor(vararg values: Int) : this(values.map { RemoteInt(it) })

    internal enum class OperationKey : RemoteOperation {
        Create {
            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                "arrayOf(${args.joinToDebugString()})"

            override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> =
                RemoteIntArray(args.fastMap { it as RemoteInt })
        },
        Get {
            override val precedence: Int
                get() = 100

            override fun toDebugString(args: List<RemoteStateCacheKey>) =
                args.formatArrayAccess(precedence)

            override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
                val array = args[0] as RemoteIntArray
                return when (val index = args[1]) {
                    is RemoteInt -> array[index]
                    is RemoteFloat -> array[index]
                    else -> throw IllegalArgumentException("Unsupported index type: $index")
                }
            }
        },
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        // If this instance represents an existing allocated ID (e.g. a formal parameter in a
        // pattern definition), return that ID directly instead of writing to the document.
        if (cacheKey is RemoteStateIdKey) {
            return (cacheKey as RemoteStateIdKey).id
        }
        val values =
            constantValueOrNull!!.fastMap { it.getIdForCreationState(creationState) }.toIntArray()
        return Utils.idFromNan(creationState.document.addList(values))
    }

    /** Array access operator for [RemoteIntArray] with an [Int] index. */
    public operator fun get(v: Int): RemoteInt = constantValueOrNull?.get(v) ?: get(RemoteInt(v))

    /**
     * Array access operator for [RemoteIntArray] with a [RemoteInt] index. Performs an ID lookup on
     * a remote integer array.
     */
    public operator fun get(v: RemoteInt): RemoteInt {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex]
        }
        return RemoteIntExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Get, this, v),
        ) { creationState ->
            val arrayId = getIdForCreationState(creationState)
            val resultId =
                creationState.document.idLookup(
                    Utils.asNan(arrayId),
                    v.getFloatIdForCreationState(creationState),
                )
            longArrayOf(resultId.toLong() + 0x100000000L)
        }
    }

    /**
     * Array access operator for [RemoteIntArray] with a [RemoteFloat] index. Performs an ID lookup
     * on a remote integer array.
     */
    public operator fun get(v: RemoteFloat): RemoteInt {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex.toInt()]
        }
        return RemoteIntExpression(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(OperationKey.Get, this, v),
        ) { creationState ->
            val arrayId = getIdForCreationState(creationState)
            val resultId =
                creationState.document.idLookup(
                    Utils.asNan(arrayId),
                    v.getFloatIdForCreationState(creationState),
                )
            longArrayOf(resultId.toLong() + 0x100000000L)
        }
    }
}
