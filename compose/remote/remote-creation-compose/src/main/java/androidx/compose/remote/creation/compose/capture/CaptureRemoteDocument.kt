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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.v2.captureSingleRemoteDocumentV2
import androidx.compose.remote.creation.compose.widgets.toLayoutDirection
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Capture a RemoteCompose document by rendering the specified [content] Composable in a virtual
 * display and returning the resulting bytes.
 *
 * This can be used for testing, or for generating documents on the fly to be sent to a remote
 * client.
 *
 * This API is experimental and is likely to change in the future before becoming API stable.
 *
 * @param context the Android [Context] to use for the capture.
 * @param creationDisplayInfo details about the virtual display to create.
 * @param profile the [Profile] to use for the capture, determining which operations are supported.
 * @param content the Composable content to render and capture.
 * @return a [ByteArray] containing the RemoteCompose document.
 */
public suspend fun captureSingleRemoteDocument(
    context: Context,
    creationDisplayInfo: CreationDisplayInfo = createCreationDisplayInfo(context),
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    content: @Composable @RemoteComposable () -> Unit,
): CapturedDocument {
    val layoutDirection = toLayoutDirection(context.resources.configuration.layoutDirection)

    if (RemoteComposeCreationComposeFlags.isRemoteApplierEnabled) {
        return captureSingleRemoteDocumentV2(
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = layoutDirection,
            profile = profile,
            content = content,
            context = context,
        )
    }

    return suspendCancellableCoroutine { continuation ->
        val virtualDisplay = DisplayPool.allocate(context, creationDisplayInfo)

        val writerEvents = WriterEvents()

        RemoteComposeCapture(
            context = context,
            virtualDisplay = virtualDisplay,
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = layoutDirection,
            immediateCapture = true,
            onPaint = { _, writer ->
                if (continuation.isActive) {
                    val docBytes = writer.encodeToByteArray()
                    continuation.resume(CapturedDocument(docBytes, writerEvents.pendingIntents))
                    DisplayPool.release(virtualDisplay)
                }
                true
            },
            onCaptureReady = @Composable {},
            profile = profile,
            writerEvents = writerEvents,
            content = content,
        )

        continuation.invokeOnCancellation { DisplayPool.release(virtualDisplay) }
    }
}
