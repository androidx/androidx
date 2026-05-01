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
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that allows you to set a Remote Compose content without the necessity to provide a
 * host for the content. The host, such as an Activity, will be created by the test rule.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteDocContentTestRule : TestRule {
    internal val remoteBaseDocContentTestRule: RemoteBaseDocContentTestRule =
        RemoteBaseDocContentTestRule()

    /** [ComposeContentTestRule] used by this [TestRule]. */
    public val composeTestRule: ComposeContentTestRule =
        remoteBaseDocContentTestRule.composeTestRule

    override fun apply(base: Statement, description: Description): Statement =
        remoteBaseDocContentTestRule.apply(base, description)

    /** Sets the given CoreDocument as a content of the current screen. */
    public fun setContent(
        coreDocument: CoreDocument,
        player: RemoteBaseDocContentTestRule.Player = PlayerImpl,
        size: Size,
    ) {

        remoteBaseDocContentTestRule.setContent(
            coreDocument = coreDocument,
            player = player,
            size = size,
        )
    }

    private object PlayerImpl : RemoteBaseDocContentTestRule.Player {
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
