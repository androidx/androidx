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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import kotlin.reflect.KClass

/** Represents an array of remote strings. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RemoteStringArray : RemoteValue<List<RemoteString>> {
    override val constantValueOrNull: List<RemoteString>?

    /** Array access operator for [RemoteStringArray] with an [Int] index. */
    public operator fun get(v: Int): RemoteString

    /** Array access operator for [RemoteStringArray] with a [RemoteInt] index. */
    public operator fun get(v: RemoteInt): RemoteString

    public companion object {
        /** Creates a [RemoteStringArray] referencing a remote ID. */
        public fun createForId(id: Int): RemoteStringArray =
            RemoteStringArrayImpl(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id))
    }
}

private enum class StringArrayOperationKey : RemoteOperation {
    Create {
        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            "arrayOf(${args.joinToDebugString()})"

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
            RemoteStringArray(args.fastMap { it as RemoteString })
    },
    Get {
        override val precedence: Int
            get() = 100

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            args.formatArrayAccess(precedence)

        override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
            (args[0] as RemoteStringArray)[args[1] as RemoteInt]
    },
}

private class RemoteStringArrayImpl(
    override val constantValueOrNull: List<RemoteString>?,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteValue<List<RemoteString>>(cacheKey), RemoteStringArray {
    override val equalityType: KClass<*>
        get() = RemoteStringArray::class

    override fun get(v: Int): RemoteString = constantValueOrNull?.get(v) ?: get(RemoteInt(v))

    override fun get(v: RemoteInt): RemoteString {
        val constArray = constantValueOrNull
        val constIndex = v.constantValueOrNull
        if (constArray != null && constIndex != null) {
            return constArray[constIndex]
        }
        return RemoteString(
            constantValueOrNull = null,
            cacheKey = RemoteOperationCacheKey.create(StringArrayOperationKey.Get, this, v),
            lazyRemoteString = StringArrayDerefImpl(this, v),
        )
    }

    override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        // If this instance represents an existing allocated ID (e.g. a formal parameter in a
        // pattern definition), return that ID directly instead of writing to the document.
        if (cacheKey is RemoteStateIdKey) {
            return cacheKey.id
        }
        val ids = constantValueOrNull!!.fastMap { creationState.getId(it) }.toIntArray()
        val nanId = creationState.document.addStringList(*ids)
        return Utils.idFromNan(nanId)
    }
}

/** Creates a [RemoteStringArray] backed by [constantValueOrNull]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteStringArray(constantValueOrNull: List<RemoteString>?): RemoteStringArray =
    RemoteStringArrayImpl(
        constantValueOrNull,
        constantValueOrNull?.let { values ->
            RemoteOperationCacheKey.create(
                StringArrayOperationKey.Create,
                *values.toTypedArray(),
            )
        } ?: RemoteStateInstanceKey(),
    )

private class StringArrayDerefImpl(val array: RemoteStringArray, val index: RemoteInt) :
    LazyRemoteString {
    override fun reserveTextId(creationState: RemoteComposeCreationState): Int {
        val arrayId = Utils.asNan(creationState.getId(array))
        val indexId = creationState.getId(index)
        return creationState.document.textLookup(arrayId, indexId)
    }

    override fun computeRequiredCodePointSet(
        creationState: RemoteComposeCreationState
    ): Set<String>? {
        val strings = array.constantValueOrNull ?: return null
        val constIndex = index.constantValueOrNull
        if (constIndex != null) {
            return strings[constIndex].computeRequiredCodePointSet(creationState)
        }
        return buildSet {
            strings.fastForEach {
                val codePointSet = it.computeRequiredCodePointSet(creationState) ?: return null
                addAll(codePointSet)
            }
        }
    }
}
