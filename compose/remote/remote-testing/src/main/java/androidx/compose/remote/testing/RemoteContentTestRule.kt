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
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Creation
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that allows you to set a Remote Compose content without the necessity to provide a
 * host for the content. The host, such as an Activity, will be created by the test rule.
 *
 * @param _composeTestRule [ComposeContentTestRule] to be used by this [TestRule].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteContentTestRule(private val _composeTestRule: ComposeContentTestRule? = null) :
    TestRule {

    internal val remoteBaseContentTestRule: RemoteBaseContentTestRule =
        RemoteBaseContentTestRule(_composeTestRule)

    /** [ComposeContentTestRule] used by this [TestRule]. */
    public val composeTestRule: ComposeContentTestRule = remoteBaseContentTestRule.composeTestRule

    override fun apply(base: Statement, description: Description): Statement =
        remoteBaseContentTestRule.apply(base, description)

    public fun setContent(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        writerEvents: WriterEvents = WriterEvents(),
        onCreate: ((CoreDocument) -> Unit)? = null,
        clock: RemoteClock = RemoteClock.SYSTEM,
        creationComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit) = {
            it()
        },
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        player: Player = PlayerImpl,
        playComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit) = {
            it()
        },
        composable: @RemoteComposable @Composable () -> Unit,
    ) {
        remoteBaseContentTestRule.setContent(
            creation =
                CreationImpl(
                    remoteCreationDisplayInfo = remoteCreationDisplayInfo,
                    profile = profile,
                    writerEvents = writerEvents,
                    onCreate = onCreate,
                    clock = clock,
                ),
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            player = player,
            size = remoteCreationDisplayInfo.size,
            playComposableWrapper = playComposableWrapper,
            composable = composable,
        )
    }

    private class CreationImpl(
        private val remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
        private val profile: Profile,
        private val writerEvents: WriterEvents,
        private val onCreate: ((CoreDocument) -> Unit)?,
        private val clock: RemoteClock,
    ) : Creation {
        @Composable
        override fun rememberRemoteDocument(
            composable: @RemoteComposable @Composable (() -> Unit)
        ): MutableState<CoreDocument?> =
            rememberRemoteDocument(
                creationDisplayInfo = remoteCreationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                onCreate = onCreate,
                clock = clock,
                content = composable,
            )
    }

    private object PlayerImpl : Player {
        @Composable
        override fun Play(coreDocument: CoreDocument, size: Size) {
            RemoteDocumentPlayer(
                document = coreDocument,
                documentWidth = size.width.toInt(),
                documentHeight = size.height.toInt(),
            )
        }
    }
}

/**
 * Captures the visual content of the root Compose node as an [ImageBitmap].
 *
 * @return The captured hierarchy rendering as an image.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteContentTestRule.captureRootToImage(): ImageBitmap =
    remoteBaseContentTestRule.captureRootToImage()
