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

package androidx.compose.remote.tooling.preview

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.RememberRemoteDocumentInline
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Display a Remote Compose Composable in the Android Studio Preview. */
@Composable
public fun RemotePreview(
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    content: @RemoteComposable @Composable () -> Unit,
) {
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
        RememberRemoteDocumentInline(
            profile = profile,
            onDocument = { doc ->
                println("Document generated: $doc")
                if (documentState == null) {
                    // Generate seems to get called again with a partial document
                    // Essentially re-recording but with existing state, so document is incomplete
                    documentState = RemoteDocument(doc)
                }
            },
            content = content,
        )

        documentState?.let { RemoteDocPreview(it) }
    }
}
