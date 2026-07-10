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
    ): CoreDocument {
        val document: ByteArray =
            captureSingleRemoteDocument(
                    context = context,
                    creationDisplayInfo = creationDisplayInfo,
                    profile = profile,
                    content = content,
                )
                .bytes

        val remoteComposeDocument =
            CoreDocument(clock).apply {
                ByteArrayInputStream(document).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        return remoteComposeDocument
    }
}
