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

/**
 * Library-group contract implemented by [RemoteState] implementations to support expression caching
 * and document ID resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteStateAccess {
    /** The cache key identifying this state within [RemoteComposeCreationState]. */
    public val cacheKey: RemoteStateCacheKey

    /** The underlying encoded [RemoteState] representation for this state. */
    public val asEncoded: RemoteState<*>
        get() = this as RemoteState<*>

    /** The underlying encoded [MutableRemoteState] representation for this state. */
    public val asEncodedMutable: MutableRemoteState<*>
        get() = this as MutableRemoteState<*>

    /**
     * Returns a new or cached id for this [RemoteState] within the [RemoteComposeCreationState].
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
        Utils.asNan(getId(creationState))
}

internal fun RemoteState<*>.requireRemoteStateAccess(): RemoteStateAccess =
    this as? RemoteStateAccess
        ?: throw IllegalArgumentException(
            "${this::class.qualifiedName ?: this::class.simpleName} must implement RemoteStateAccess"
        )
