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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput

@Sampled
fun keyInputClick() {
    var counter = 0
    val focusRequester = FocusRequester()
    lateinit var inputModeManager: InputModeManager
    composeTestRule.setContent {
        inputModeManager = LocalInputModeManager.current
        BasicText(
            "ClickableText",
            modifier =
                Modifier.testTag("myClickable").focusRequester(focusRequester).clickable {
                    counter++
                },
        )
    }
    composeTestRule.runOnIdle {
        inputModeManager.requestInputMode(Keyboard)
        focusRequester.requestFocus()
    }

    composeTestRule.onNodeWithTag("myClickable").performKeyInput { keyDown(Key.Enter) }

    composeTestRule.runOnIdle { assert(counter == 0) }

    composeTestRule.onNodeWithTag("myClickable").performKeyInput { keyUp(Key.Enter) }

    composeTestRule.runOnIdle { assert(counter == 1) }
}
