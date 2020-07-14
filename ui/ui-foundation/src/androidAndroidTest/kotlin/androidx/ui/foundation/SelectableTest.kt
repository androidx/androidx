/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.foundation.selection.selectable
import androidx.ui.layout.Stack
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.performClick
import androidx.ui.test.performPartialGesture
import androidx.ui.test.onNode
import androidx.ui.test.onAllNodes
import androidx.ui.test.onNodeWithText
import androidx.ui.test.onFirst
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.test.runOnIdle
import androidx.ui.test.down
import androidx.ui.test.up
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SelectableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectable_defaultSemantics() {
        composeTestRule.setContent {
            Text(
                "Text in item",
                modifier = Modifier.selectable(selected = true, onClick = {})
            )
        }

        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(1)
            .onFirst()
            .assertIsInMutuallyExclusiveGroup()
            .assertIsSelected()
    }

    @Test
    fun selectable_defaultClicks() {
        composeTestRule.setContent {
            val (selected, onSelected) = state { false }
            Text(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = { onSelected(!selected) }
                )
            )
        }

        onNode(isInMutuallyExclusiveGroup())
            .assertIsUnselected()
            .performClick()
            .assertIsSelected()
            .performClick()
            .assertIsUnselected()
    }

    @Test
    fun selectable_noClicksNoChanges() {
        composeTestRule.setContent {
            val (selected, _) = state { false }
            Text(
                "Text in item",
                modifier = Modifier.selectable(
                    selected = selected,
                    onClick = {})
            )
        }

        onNode(isInMutuallyExclusiveGroup())
            .assertIsUnselected()
            .performClick()
            .assertIsUnselected()
    }

    @Test
    fun selectableTest_interactionState() {
        val interactionState = InteractionState()

        composeTestRule.setContent {
            Stack {
                Box(Modifier.selectable(
                    selected = true,
                    interactionState = interactionState,
                    onClick = {}
                )) {
                    Text("SelectableText")
                }
            }
        }

        runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        onNodeWithText("SelectableText")
            .performPartialGesture { down(center) }

        runOnIdle {
            Truth.assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        onNodeWithText("SelectableText")
            .performPartialGesture { up() }

        runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }

    @Test
    fun selectableTest_interactionState_resetWhenDisposed() {
        val interactionState = InteractionState()
        var emitSelectableText by mutableStateOf(true)

        composeTestRule.setContent {
            Stack {
                if (emitSelectableText) {
                    Box(Modifier.selectable(
                        selected = true,
                        interactionState = interactionState,
                        onClick = {}
                    )) {
                        Text("SelectableText")
                    }
                }
            }
        }

        runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }

        onNodeWithText("SelectableText")
            .performPartialGesture { down(center) }

        runOnIdle {
            Truth.assertThat(interactionState.value).contains(Interaction.Pressed)
        }

        // Dispose selectable
        runOnIdle {
            emitSelectableText = false
        }

        runOnIdle {
            Truth.assertThat(interactionState.value).doesNotContain(Interaction.Pressed)
        }
    }
}