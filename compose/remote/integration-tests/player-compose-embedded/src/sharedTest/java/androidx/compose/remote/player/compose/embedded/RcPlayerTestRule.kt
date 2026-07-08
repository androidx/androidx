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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.testing.RemoteBaseContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] for testing the embedded player (`RcPlayer`).
 *
 * Uses [RemoteBaseContentTestRule] to set Remote Compose content and renders it via [RcPlayer].
 */
class RcPlayerTestRule(val baseRule: RemoteBaseContentTestRule = RemoteBaseContentTestRule()) :
    TestRule by baseRule, ComposeContentTestRule by baseRule {

    val composeRule: ComposeContentTestRule
        get() = baseRule.composeTestRule

    override fun apply(base: Statement, description: Description): Statement =
        baseRule.apply(base, description)

    /**
     * Captures a remote document from [content] and sets it on [RcPlayer].
     *
     * @return The captured [CoreDocument].
     */
    fun setRemoteContent(
        autoUpdate: Boolean = false,
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo =
            createCreationDisplayInfo(
                context = ApplicationProvider.getApplicationContext(),
                size =
                    run {
                        val context = ApplicationProvider.getApplicationContext<Context>()
                        val density = context.resources.displayMetrics.density
                        Size(100f * density, 100f * density)
                    },
            ),
        playComposableWrapper: @Composable (composable: @Composable () -> Unit) -> Unit =
            { content ->
                Box(
                    modifier =
                        Modifier.size(
                            remoteCreationDisplayInfo.widthDp,
                            remoteCreationDisplayInfo.heightDp,
                        )
                ) {
                    content()
                }
            },
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument {
        var createdDocument: CoreDocument? = null

        baseRule.setContent(
            creation =
                object : RemoteBaseContentTestRule.Creation {
                    @Composable
                    override fun rememberRemoteDocument(
                        composable: @RemoteComposable @Composable () -> Unit
                    ): MutableState<CoreDocument?> {
                        return rememberRemoteDocument(
                            creationDisplayInfo = remoteCreationDisplayInfo,
                            content = composable,
                        )
                    }
                },
            player =
                object : RemoteBaseContentTestRule.Player {
                    @Composable
                    override fun Play(coreDocument: CoreDocument, size: Size) {
                        RcPlayer(document = coreDocument, autoUpdate = autoUpdate)
                    }
                },
            size =
                Size(
                    remoteCreationDisplayInfo.widthDp.value,
                    remoteCreationDisplayInfo.heightDp.value,
                ),
            playComposableWrapper = playComposableWrapper,
            onCoreDocumentCreated = { doc -> createdDocument = doc },
            composable = content,
        )

        while (createdDocument == null) {
            waitForIdle()
            mainClock.advanceTimeByFrame()
        }
        return createdDocument!!
    }
}
