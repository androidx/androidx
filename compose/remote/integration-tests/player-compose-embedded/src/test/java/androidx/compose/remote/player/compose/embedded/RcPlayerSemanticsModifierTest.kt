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

@file:Suppress(
    "RestrictedApiAndroidX"
) // Referring to RemoteText, background, remote-core, role, text

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.text
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests for the semantics modifier (content description, text, role, CLEAR_AND_SET mode), asserted
 * through the Compose semantics tree — no screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerSemanticsModifierTest {

    @get:Rule val rule = createComposeRule()

    private fun renderDocument(content: @Composable () -> Unit) {
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

    @Test
    fun semanticsRoleSurfacesToTheAccessibilityTree() {
        renderDocument {
            RemoteBox(
                modifier =
                    RemoteModifier.semantics {
                            contentDescription = "switchy".rs
                            role = Role.Switch
                        }
                        .size(40.rdp)
                        .background(Color(0xFF3F51B5).rc)
            )
        }

        rule
            .onNodeWithContentDescription("switchy")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    @Test
    fun semanticsTextSurfacesToTheAccessibilityTree() {
        renderDocument {
            RemoteBox(
                modifier =
                    RemoteModifier.semantics { text = "labelled box".rs }
                        .size(40.rdp)
                        .background(Color(0xFF3F51B5).rc)
            )
        }

        rule.onNodeWithText("labelled box").assertExists()
    }

    @Test
    fun clearAndSetSemanticsReplacesDescendantSemantics() {
        renderDocument {
            RemoteColumn(
                modifier =
                    RemoteModifier.clearAndSetSemantics { contentDescription = "outer".rs }
                        .size(100.rdp)
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "inner".rs }
                            .size(40.rdp)
                            .background(Color(0xFFAA0000).rc)
                )
            }
        }

        rule.onNodeWithContentDescription("outer").assertExists()
        // CLEAR_AND_SET wipes descendant semantics, so the inner description must not surface.
        rule.onNodeWithContentDescription("inner").assertDoesNotExist()
    }

    @Test
    fun mergedSemanticsCombineDescendantsIntoOneNode() {
        renderDocument {
            RemoteColumn(
                modifier =
                    RemoteModifier.semantics(mergeDescendants = true) {
                            contentDescription = "outer".rs
                        }
                        .size(100.rdp)
            ) {
                RemoteText(text = "inner label".rs)
            }
        }

        // MERGE folds the descendants' semantics into the parent node: the merged node carries
        // both the container description and the child text.
        rule.onNodeWithContentDescription("outer").assertTextEquals("inner label")
    }
}
