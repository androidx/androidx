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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection

/** Scope for accessing remote state IDs. */
public interface RemoteStateScope {
    /** The [RemoteComposeCreationState] associated with the document being drawn into. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val parentScope: RemoteStateScope

    /** The [RemoteDensity] associated with the document being drawn into. */
    public val remoteDensity: RemoteDensity

    /** The [LayoutDirection] associated with the document being drawn into. */
    public val layoutDirection: LayoutDirection

    /** The [RemoteComposeWriter] associated with the document being drawn into. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val document: RemoteComposeWriter
        get() = parentScope.document

    /** Returns the ID for this state within the scope. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val RemoteState<*>.id: Int
        get() = (this as BaseRemoteState<*>).getIdForCreationState(creationState)

    /** Returns the float ID for this state within the scope. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val RemoteState<*>.floatId: Float
        get() = (this as BaseRemoteState<*>).getFloatIdForCreationState(creationState)

    /** Returns the long ID for this state within the scope. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val RemoteState<*>.longId: Long
        get() = (this as BaseRemoteState<*>).getLongIdForCreationState(creationState)
}

/** The [RemoteComposeCreationState] associated with the document being drawn into. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val RemoteStateScope.creationState: RemoteComposeCreationState
    get() = this as? RemoteComposeCreationState ?: parentScope.creationState

/**
 * Allocates the RemoteState in global scope allowing assigning to global state IDs and
 * document-level properties.
 */
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : RemoteState<*>> T.withGlobalScope(): T {
    with(LocalRemoteComposeCreationState.current) {
        this.document.beginGlobal()
        // Force commit to the document via RemoteStateScope cache
        this@withGlobalScope.id
        this.document.endGlobal()
    }

    return this
}
