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
import androidx.compose.remote.document.MaybeVariableFloat
import androidx.compose.remote.document.NanEncoding
import androidx.compose.remote.document.toMaybeVariableFloat

/**
 * Library-group contract implemented by [RemoteValue] implementations to support expression caching
 * and document ID resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteValueAccess {
    /** The cache key identifying this [RemoteValue] within [RemoteComposeCreationState]. */
    public val cacheKey: RemoteStateCacheKey

    /** The underlying encoded [RemoteValue] representation for this value. */
    public val asEncoded: RemoteValue<*>
        get() = this as RemoteValue<*>

    /** The underlying encoded [MutableRemoteValue] representation for this value. */
    public val asEncodedMutable: MutableRemoteValue<*>
        get() = this as MutableRemoteValue<*>

    /**
     * Returns a new or cached id for this [RemoteValue] within the [RemoteComposeCreationState].
     *
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value, for the given [creationState].
     */
    public fun getId(creationState: RemoteComposeCreationState): Int

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value, for the given [creationState] as a long.
     */
    public fun getLongId(creationState: RemoteComposeCreationState): Long {
        return getId(creationState).toLong() + 0x100000000L
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated.
     * @return The ID of this remote value encoded in a Float NaN, for the given [creationState].
     */
    public fun getFloatId(creationState: RemoteComposeCreationState): Float =
        NanEncoding.asNan(getId(creationState))

    /**
     * Returns this value as a [MaybeVariableFloat] (either a literal float or a variable reference)
     * for [creationState].
     */
    public fun getMaybeVariableFloat(
        creationState: RemoteComposeCreationState
    ): MaybeVariableFloat = getFloatId(creationState).toMaybeVariableFloat()
}

internal fun RemoteValue<*>.requireRemoteValueAccess(): RemoteValueAccess =
    this as? RemoteValueAccess
        ?: throw IllegalArgumentException(
            "${this::class.qualifiedName ?: this::class.simpleName} must implement RemoteValueAccess"
        )
