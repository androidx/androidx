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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import androidx.wear.compose.remote.material3.util.TestImageVectors
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RemoteButtonA11yTest {
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun button_isFocusable() {
        renderDocument {
            RemoteButton(onClick = hostAction("click".rs)) { RemoteText("text".rs) }
        }

        rule.onNodeWithText("text").assertIsButtonAndFocusable()
    }

    @Test
    fun compactButton_isFocusable() {
        renderDocument {
            RemoteCompactButton(onClick = hostAction("click".rs)) { RemoteText("text".rs) }
        }

        rule.onNodeWithText("text").assertIsButtonAndFocusable()
    }

    @Test
    fun buttonWithSecondaryLabelAndIcon_isFocusable() {
        renderDocument {
            RemoteButton(
                onClick = hostAction("click".rs),
                secondaryLabel = { RemoteText("text".rs) },
                icon = {
                    RemoteIcon(TestImageVectors.VolumeUp, contentDescription = "VolumeUp".rs)
                },
            ) {}
        }

        rule.onNodeWithText("text").assertIsButtonAndFocusable()
    }

    @Test
    fun iconButton_isFocusable() {
        renderDocument {
            RemoteIconButton(onClick = hostAction("click".rs)) {
                RemoteIcon(TestImageVectors.VolumeUp, contentDescription = "Add".rs)
            }
        }

        rule.onNodeWithContentDescription("Add").assertIsButtonAndFocusable()
    }

    @Test
    fun textButton_isFocusable() {
        renderDocument {
            RemoteTextButton(onClick = hostAction("click".rs)) { RemoteText("text".rs) }
        }

        rule.onNodeWithText("text").assertIsButtonAndFocusable()
    }

    private fun SemanticsNodeInteraction.assertIsButtonAndFocusable() {
        assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertHasClickAction()
        assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
    }

    private fun renderDocument(content: @Composable @RemoteComposable () -> Unit) {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(context = context, content = content).bytes
            val document =
                CoreDocument().apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }
            rule.mainClock.advanceTimeBy(100)
        }
    }
}
