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

package androidx.compose.ui.viewinterop

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusWappingTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun focusForwardWraps() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Column(Modifier.fillMaxSize().safeContentPadding()) {
                AndroidView(
                    factory = {
                        ComposeView(it).also {
                            it.setContent {
                                Column {
                                    Button(onClick = {}, Modifier.testTag(tag1)) {
                                        Text("Button 1")
                                    }
                                    Button(onClick = {}, Modifier.testTag(tag2)) {
                                        Text("Button 2")
                                    }
                                }
                            }
                        }
                    }
                )
                AndroidView(factory = { TextView(it) })
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        rule.waitForIdle()
        Assume.assumeTrue(inputModeManager.inputMode == InputMode.Keyboard)

        rule.onNodeWithTag(tag2).requestFocus()
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag2).performKeyInput {
            keyDown(Key.Tab)
            keyUp(Key.Tab)
        }
        rule.onNodeWithTag(tag1).assertIsFocused()
    }

    @Test
    fun focusBackwardsWraps() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Column(Modifier.fillMaxSize().safeContentPadding()) {
                AndroidView(
                    factory = {
                        ComposeView(it).also {
                            it.setContent {
                                Column {
                                    Button(onClick = {}, Modifier.testTag(tag1)) {
                                        Text("Button 1")
                                    }
                                    Button(onClick = {}, Modifier.testTag(tag2)) {
                                        Text("Button 2")
                                    }
                                }
                            }
                        }
                    }
                )
                AndroidView(factory = { TextView(it) })
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }

        rule.waitForIdle()
        Assume.assumeTrue(inputModeManager.inputMode == InputMode.Keyboard)

        rule.onNodeWithTag(tag1).requestFocus()
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(tag1).performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.Tab)
            keyUp(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithTag(tag2).assertIsFocused()
    }
}
