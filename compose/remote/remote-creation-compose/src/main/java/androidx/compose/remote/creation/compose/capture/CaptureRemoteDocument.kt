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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Capture a RemoteCompose document by rendering the specified [content] Composable in a virtual
 * display and returning the resulting bytes.
 *
 * This can be used for testing, or for generating documents on the fly to be sent to a remote
 * client.
 *
 * @param context the Android [Context] to use for the capture.
 * @param creationDisplayInfo details about the virtual display to create.
 * @param profile the [Profile] to use for the capture, determining which operations are supported.
 * @param content the Composable content to render and capture.
 * @return a [ByteArray] containing the RemoteCompose document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: CreationDisplayInfo = createCreationDisplayInfo(context),
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerCallbacks: WriterCallback? = null,
    content: @Composable @RemoteComposable () -> Unit,
): ByteArray = suspendCoroutine { continuation ->
    var completed = false
    RemoteComposeCapture(
        context = context,
        creationDisplayInfo = creationDisplayInfo,
        immediateCapture = true,
        onPaint = { view, writer ->
            if (!completed) {
                completed = true
                continuation.resume(writer.encodeToByteArray())
            }
            true
        },
        onCaptureReady = @Composable {},
        profile = profile,
        writerCallbacks = writerCallbacks,
        content = content,
    )
}
