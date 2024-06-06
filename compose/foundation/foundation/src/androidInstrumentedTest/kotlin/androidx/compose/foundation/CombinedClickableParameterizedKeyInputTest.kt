/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class CombinedClickableParameterizedKeyInputTest(keyCode: Long) {
    private val key: Key = Key(keyCode)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "keyCode={0}")
        // @Parameterized doesn't handle value classes correctly, which is why we use key codes.
        fun parameters() =
            listOf(
                Key.Enter.keyCode,
                Key.NumPadEnter.keyCode,
                Key.DirectionCenter.keyCode,
                Key.Spacebar.keyCode
            )
    }

    @get:Rule val rule = createComposeRule()

    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    fun clickWithKey() {
        var counter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable { counter++ }
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(key) }

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(key) }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    fun longClickWithKey() {
        var clickCounter = 0
        var longClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onLongClick = { ++longClickCounter },
                            onClick = { ++clickCounter }
                        )
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(InputMode.Keyboard)
            // The press duration is 100ms longer than the minimum required for a long press.
            val durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
            pressKey(key, durationMillis)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(0)
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun keyPress_emitsIndication() {
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
                        Modifier.testTag("clickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }
}
