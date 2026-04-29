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

package androidx.compose.remote.player.compose.test.utils.screenshot.rule

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.testing.RemoteBaseContentTestRule
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that uses [RemoteContentTestRule] to set the Remote Compose content and provides
 * tracking of interactions with the UI.
 */
class RemoteInteractionTestRule(private val remoteCreationDisplayInfo: RemoteCreationDisplayInfo) :
    TestRule {

    constructor(
        context: Context
    ) : this(remoteCreationDisplayInfo = createCreationDisplayInfo(context))

    val clickEvents: MutableList<Pair<String, Any?>> = mutableListOf()

    private val remoteContentTestRule = RemoteContentTestRule()

    private val player = PlayerImpl(clickEvents = clickEvents)

    override fun apply(base: Statement, description: Description): Statement =
        remoteContentTestRule.apply(base, description)

    fun setContent(
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            onCoreDocumentCreated = onCoreDocumentCreated,
            player = player,
            composable = composable,
        )
    }

    private class PlayerImpl(private val clickEvents: MutableList<Pair<String, Any?>>) :
        RemoteBaseContentTestRule.Player {
        @Composable
        override fun Play(coreDocument: CoreDocument, size: Size) {
            RemoteDocumentPlayer(
                document = coreDocument,
                documentWidth = size.width.toInt(),
                documentHeight = size.height.toInt(),
                onNamedAction = { name: String, value: Any?, _: StateUpdater ->
                    clickEvents.add(Pair(name, value))
                },
            )
        }
    }
}
