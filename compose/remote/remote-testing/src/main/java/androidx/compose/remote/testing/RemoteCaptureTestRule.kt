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

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import java.io.ByteArrayInputStream
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** A rule that allows capturing the document from a Remote Composable. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCaptureTestRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement = base

    /**
     * Capture a RemoteCompose document by rendering the specified [content] Composable and
     * returning a [CoreDocument].
     *
     * @param context the Android [Context] to use for the capture.
     * @param content the Composable content to render and capture.
     * @return a platform independent RemoteCompose document.
     */
    public suspend fun captureDocument(
        context: Context,
        creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(context),
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        clock: RemoteClock = RemoteClock.SYSTEM,
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument =
        decodeDocument(
            bytes = captureDocumentBytes(context, creationDisplayInfo, profile, content),
            profile = profile,
            clock = clock,
        )

    /**
     * Capture a RemoteCompose document by rendering the specified [content] Composable and
     * returning its serialized bytes.
     *
     * Use this instead of [captureDocument] when the document bytes are needed, for example to play
     * the document or to measure its size: [CoreDocument] does not retain its serialized bytes, and
     * re-serializing its inflated operations does not round-trip.
     *
     * @param context the Android [Context] to use for the capture.
     * @param content the Composable content to render and capture.
     * @return the serialized RemoteCompose document.
     */
    public suspend fun captureDocumentBytes(
        context: Context,
        creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(context),
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        content: @Composable @RemoteComposable () -> Unit,
    ): ByteArray =
        captureSingleRemoteDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                content = content,
            )
            .bytes
}

/** Decodes the serialized document [bytes] into a [CoreDocument] for the given [profile]. */
internal fun decodeDocument(bytes: ByteArray, profile: Profile, clock: RemoteClock): CoreDocument =
    CoreDocument(clock).apply {
        ByteArrayInputStream(bytes).use {
            val buffer = RemoteComposeBuffer.fromInputStream(it)
            buffer.setVersion(
                profile.apiLevel,
                profile.operationsProfiles,
                profile.supportedOperations,
            )
            initFromBuffer(buffer)
        }
    }
