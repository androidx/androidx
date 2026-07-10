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

package androidx.compose.remote.a11y

import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.text
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.RcPlayerTestRule
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.text.DecimalFormat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RcPlayerBasicA11yTest {
    @get:Rule val playerRule = RcPlayerTestRule()

    @Test
    fun textSemantics() {
        playerRule.setRemoteContent {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize(),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Hello World")
            }
        }

        playerRule.composeRule.onNodeWithText("Hello World").assertExists()
    }

    @Test
    fun textSemanticHierarchy() {
        playerRule.setRemoteContent {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().semantics { text = "Item 1".rs },
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
                verticalArrangement = RemoteArrangement.Center,
            ) {
                RemoteText("Item 1.1")
                RemoteColumn(
                    modifier = RemoteModifier.fillMaxWidth().semantics { text = "Item 1.2".rs },
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    verticalArrangement = RemoteArrangement.Center,
                ) {
                    RemoteText("Item 1.2.1", modifier = RemoteModifier.padding(10.rf))
                    RemoteText("Item 1.2.2", modifier = RemoteModifier.padding(10.rf))
                }
                RemoteText("Item 1.3")
            }
        }

        playerRule.composeRule.onNodeWithText("Item 1").assertExists()
        playerRule.composeRule.onNodeWithText("Item 1.1").assertExists()
        playerRule.composeRule.onNodeWithText("Item 1.2").assertExists()
        playerRule.composeRule.onNodeWithText("Item 1.2.1").assertExists()
        playerRule.composeRule.onNodeWithText("Item 1.2.2").assertExists()
        playerRule.composeRule.onNodeWithText("Item 1.3").assertExists()
    }

    @Test
    fun textValueChange() {
        playerRule.setRemoteContent {
            val text = rememberMutableRemoteString("Initial")
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .clickable(valueChange(text, "Updated".rs))
                        .background(Color.White),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text)
            }
        }

        val node = playerRule.composeRule.onNode(hasClickAction())
        node.assertExists()
        node.assertTextEquals("Initial")

        node.performClick()
        playerRule.composeRule.waitForIdle()

        node.assertTextEquals("Updated")
    }

    @Test
    fun intValueChange() {
        playerRule.setRemoteContent {
            val decimalFormat = remember { DecimalFormat("##0") }
            val remoteInt = rememberMutableRemoteInt(0)
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxSize()
                        .clickable(valueChange(remoteInt, remoteInt + 1))
                        .background(Color.White),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(remoteInt.toRemoteString(decimalFormat))
            }
        }

        val node = playerRule.composeRule.onNode(hasClickAction())
        node.assertExists()
        node.assertTextEquals("0")

        node.performClick()
        playerRule.composeRule.waitForIdle()
        node.assertTextEquals("1")

        node.performClick()
        playerRule.composeRule.waitForIdle()
        node.assertTextEquals("2")
    }
}
