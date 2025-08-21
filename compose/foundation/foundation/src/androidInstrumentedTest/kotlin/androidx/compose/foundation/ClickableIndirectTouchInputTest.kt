/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Indirect touch input tests for [clickable] similar to those in
 * [ClickableParameterizedKeyInputTest], for interactions not shared with pointer input. Common
 * tests that apply to both pointer input and indirect input are in [ClickableTest].
 */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableIndirectTouchInputTest() {

    @get:Rule val rule = createComposeRule()

    @Test
    fun clickWithIndirectTouch() {
        var counter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable").focusRequester(focusRequester).clickable {
                        counter++
                    },
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun clickWithIndirectTouch_notInvokedIfFocusIsLostWhilePressed() {
        var counter = 0
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp).focusRequester(outerFocusRequester).focusTarget()) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(clickableFocusRequester)
                            .clickable { counter++ },
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            clickableFocusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            // Remove focus from the clickable
            outerFocusRequester.requestFocus()
        }

        // (clickable won't see this event as it is no longer focused, but emit for clarity)
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent()

        // The clickable should never see the up event, so it should never invoke onClick
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
    }

    @Test
    fun clickWithIndirectTouch_notInvokedIfCorrespondingDownEventWasNotReceived() {
        var counter = 0
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier.testTag("outerBox")
                    .padding(10.dp)
                    .focusRequester(outerFocusRequester)
                    .focusTarget()
            ) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(clickableFocusRequester)
                            .clickable { counter++ },
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            outerFocusRequester.requestFocus()
        }

        // Press down on the outer box
        rule.onNodeWithTag("outerBox").sendIndirectTouchPressEvent()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            // Focus the clickable, while still pressing down
            clickableFocusRequester.requestFocus()
        }

        // Release
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent()

        // The clickable should not invoke onClick because it only saw the up event, not the
        // corresponding down, and hence should not be considered pressed
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
    }

    @Test
    fun indirectTouchPress_emitsInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").sendIndirectTouchPressEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").sendIndirectTouchReleaseEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun indirectTouchPress_emitsCancelInteractionWhenFocusIsRemovedWhilePressed() {
        val interactionSource = MutableInteractionSource()
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp).focusRequester(outerFocusRequester).focusTarget()) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable")
                            .focusRequester(clickableFocusRequester)
                            .clickable(interactionSource = interactionSource, indication = null) {},
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            clickableFocusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").sendIndirectTouchPressEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            // Remove focus from the clickable, while it is still 'pressed'
            outerFocusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            // We should cancel the existing press, since the clickable is no longer focused
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            // We should be unfocused
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
        }
    }

    @Test
    fun doubleIndirectTouchPress_emitsFurtherInteractions() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        val clickableNode = rule.onNodeWithTag("clickable")

        clickableNode.sendIndirectPressReleaseEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
        }

        clickableNode.sendIndirectTouchPressEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
        }

        clickableNode.sendIndirectTouchReleaseEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun interruptedIndirectTouchClick_emitsCancelInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        val enabled = mutableStateOf(true)
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled.value,
                        ) {},
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        val clickableNode = rule.onNodeWithTag("clickable")

        clickableNode.sendIndirectTouchPressEvent()

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        enabled.value = false

        clickableNode.assertIsNotEnabled()

        rule.runOnIdle {
            // Filter out focus interactions.
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }

        // Release should not result in interactions.
        clickableNode.sendIndirectTouchReleaseEvent()

        // Make sure nothing has changed.
        rule.runOnIdle {
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }
    }

    @Test
    fun modifierReusedBetweenIndirectTouchDownAndIndirectTouchUp_doesNotCallListeners() {
        var counter = 0
        var reuseKey by mutableStateOf(0)
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            ReusableContent(reuseKey) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .clickable(onClick = { ++counter }),
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent()
        rule.runOnIdle { reuseKey = 1 }
        rule.waitForIdle()
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
    }
}
