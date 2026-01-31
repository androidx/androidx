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

/**
 * A remote-compatible drawing scope for RemoteCompose that provides access to the content of the
 * component being drawn.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteDrawWithContentScope(
    remoteCanvas: RemoteCanvas,
    private val content: RemoteDrawScope.() -> Unit = {
        remoteCanvas.internalCanvas.document.drawComponentContent()
    },
) : RemoteDrawScope(remoteCanvas) {

    /** Draws the content of the component. */
    public fun drawContent() {
        content()
    }
}
