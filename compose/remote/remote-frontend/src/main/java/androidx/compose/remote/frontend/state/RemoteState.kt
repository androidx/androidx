/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State

// TODO: Remove this and APIs using it.
object FallbackCreationState {
    private var state_: RemoteComposeCreationState? = null

    /** The [RemoteComposeCreationState] to use when the state isn't passed in. */
    var state: RemoteComposeCreationState
        get() = state_ ?: NoRemoteCompose()
        set(value) {
            state_ = value
        }
}

/** Common base interface for all Remote types. */
interface BaseRemoteState {
    /** Whether or not this remote value always evaluates to the same result. */
    val hasConstantValue: Boolean

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated
     * @return The ID of this remote value, for the given [creationState]
     */
    fun getIdForCreationState(creationState: RemoteComposeCreationState): Int {
        val currentId = creationState.remoteVariableToId.get(this)
        if (currentId != null) {
            return currentId
        }
        val id = writeToDocument(creationState)
        creationState.remoteVariableToId.put(this, id)
        return id
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated
     * @return The ID of this remote value, for the given [creationState] as a long
     */
    fun getLongIdForCreationState(creationState: RemoteComposeCreationState): Long {
        return getIdForCreationState(creationState).toLong() + 0x100000000L
    }

    /**
     * @param creationState The [RemoteComposeCreationState] for which the ID will be generated
     * @return The ID of this remote value encoded in a Float NaN, for the given [creationState]
     */
    fun getFloatIdForCreationState(creationState: RemoteComposeCreationState) =
        Utils.asNan(getIdForCreationState(creationState))

    /**
     * Writes the Remote Value to the [creationState] and returns the allocated ID.
     *
     * @param creationState The [RemoteComposeCreationState] to write to
     * @return The ID allocated by the [RemoteComposeWriter]
     */
    fun writeToDocument(creationState: RemoteComposeCreationState): Int
}

/**
 * A readable but not writable Remote Compose State value.
 *
 * It may represent either a mutable direct value (var), or some expression that might change
 * externally.
 *
 * In Remote Compose recording mode, the type specific id should be used.
 *
 * In preview mode, this type must honour the [Stable] contract.
 */
@Stable interface RemoteState<T> : State<T>, BaseRemoteState

/**
 * A readable and writable Remote Compose State value.
 *
 * It represents a direct value (var).
 *
 * In Remote Compose recording mode, the type specific id should be used.
 *
 * In preview mode, this type must honour the [Stable] contract.
 */
@Stable interface MutableRemoteState<T> : RemoteState<T>, MutableState<T>
