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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import kotlin.test.Test

class ComposeWindowTest {
    @Test
    fun testComposeWindowClearFocusOnMouseDownEnabled() =
        testComposeWindowClearFocusOnMouseDownEnabledFlag(true)

    @Test
    fun testComposeWindowClearFocusOnMouseDownDisabled() =
        testComposeWindowClearFocusOnMouseDownEnabledFlag(false)

    fun testComposeWindowClearFocusOnMouseDownEnabledFlag(enabled: Boolean) = runApplicationTest {
        val focusRequester = FocusRequester()
        var textFieldIsFocused = false

        val window = ComposeWindow()
        try {
            window.isClearFocusOnMouseDownEnabled = enabled
            window.setContent {
                Column(Modifier.size(300.dp, 400.dp)) {
                    BasicTextField(
                        state = rememberTextFieldState(),
                        modifier = Modifier
                            .testTag("textField")
                            .fillMaxWidth()
                            .height(100.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                textFieldIsFocused = it.isFocused
                            }
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    Box(Modifier.testTag("box").fillMaxWidth().weight(1f))
                }
            }
            window.size = Dimension(300, 400)
            window.isVisible = true

            awaitIdle()

            assertThat(textFieldIsFocused).isTrue()
            window.sendMousePress(x = 100, y = 300)
            window.sendMouseRelease(x = 100, y = 300)
            awaitIdle()

            assertThat(textFieldIsFocused).isEqualTo(!enabled)
        } finally {
            window.dispose()
        }
    }
}