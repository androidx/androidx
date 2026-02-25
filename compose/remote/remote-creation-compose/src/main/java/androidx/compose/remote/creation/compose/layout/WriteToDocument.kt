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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

/** Break glass mechanism to make calls direct to the RecordingCanvas or document. */
@Composable
public fun WriteToDocument(message: String? = null, content: RecordingCanvas.() -> Unit) {
    RemoteCanvas(modifier = RemoteModifier.size(0.rdp)) { content(remoteCanvas.internalCanvas) }
}

@Composable
public fun RecordingCanvas(content: RecordingCanvas.() -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier.drawBehind { drawIntoRemoteCanvas { it.content() } }
    )
}

@Composable
@RemoteComposable
public fun <T : RemoteState<*>> T.withGlobalScope(): T {
    with(LocalRemoteComposeCreationState.current) {
        this.document.beginGlobal()
        this@withGlobalScope.floatId
        this.document.endGlobal()
    }

    return this
}
