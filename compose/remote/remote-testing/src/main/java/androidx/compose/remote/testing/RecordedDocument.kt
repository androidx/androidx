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

package androidx.compose.remote.testing

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteWriterEvents
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * A Remote Compose document recorded by a [RemoteBaseContentTestRule.Creation] and played by a
 * [RemoteBaseContentTestRule.Player].
 *
 * [CoreDocument] does not retain its serialized bytes, and re-serializing its inflated operations
 * does not round-trip, so players that render from the serialized document should use [bytes].
 *
 * @property bytes The serialized document, exactly as produced by creation.
 * @property profile The [Profile] the document was created with.
 * @param clock The [RemoteClock] of [coreDocument].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RecordedDocument(
    public val bytes: ByteArray,
    public val profile: Profile,
    private val clock: RemoteClock = RemoteClock.SYSTEM,
) {
    /** The [CoreDocument] decoded from [bytes]. It is decoded on first access. */
    public val coreDocument: CoreDocument by
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) { decodeDocument(bytes, profile, clock) }
}

/**
 * Captures [content] into a [RecordedDocument], for use by [RemoteBaseContentTestRule.Creation]
 * implementations.
 *
 * @param onCreate Called with the recorded document before it is set on the returned state.
 * @return The state holding the recorded document, or `null` until the capture completes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRecordedDocument(
    creationDisplayInfo: RemoteCreationDisplayInfo,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: RemoteWriterEvents = RemoteWriterEvents(),
    onCreate: ((RecordedDocument) -> Unit)? = null,
    clock: RemoteClock = RemoteClock.SYSTEM,
    content: @Composable @RemoteComposable () -> Unit,
): MutableState<RecordedDocument?> {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val document: MutableState<RecordedDocument?> = remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    creationDisplayInfo = creationDisplayInfo,
                    layoutDirection = layoutDirection,
                    clock = clock,
                    profile = profile,
                    writerEvents = writerEvents,
                    content = content,
                )
                .bytes
        val recordedDocument = RecordedDocument(bytes = bytes, profile = profile, clock = clock)
        onCreate?.invoke(recordedDocument)
        document.value = recordedDocument
    }
    return document
}
