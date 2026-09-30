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

package androidx.wear.compose.remote.integration.demos.bookends.material3

import androidx.compose.runtime.staticCompositionLocalOf

/** How the bookends components are emitted into the remote document. */
enum class BookendsImplementation {
    /**
     * Emit `Custom` components, rendered by the host with the Wear Compose Material3 components
     * (see the `player` package).
     */
    WearMaterial3,

    /**
     * Delegate to the equivalent `androidx.wear.compose.remote.material3` component, drawn with
     * remote primitives. Only supported by the components that have a remote-material3 equivalent
     * ([RemoteButton], [RemoteText] and [RemoteTimeText]); the others always use [WearMaterial3].
     */
    RemoteMaterial3,
}

/**
 * Selects the [BookendsImplementation] used by the bookends components, allowing the same content
 * to be rendered with Wear Compose Material3 on the host or with remote-material3.
 */
val LocalBookendsImplementation = staticCompositionLocalOf { BookendsImplementation.WearMaterial3 }
