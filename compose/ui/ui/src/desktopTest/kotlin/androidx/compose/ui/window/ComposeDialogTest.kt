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
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import javax.swing.JFrame
import kotlin.test.Test

class ComposeDialogTest {
    @Test
    fun testComposeDialogClearFocusOnMouseDownEnabled() =
        testComposeDialogClearFocusOnMouseDownEnabledFlag(true)

    @Test
    fun testComposeDialogClearFocusOnMouseDownDisabled() =
        testComposeDialogClearFocusOnMouseDownEnabledFlag(false)

    fun testComposeDialogClearFocusOnMouseDownEnabledFlag(enabled: Boolean) = runApplicationTest {
        val focusRequester = FocusRequester()
        var textFieldIsFocused = false

        val window = JFrame()
        val dialog = ComposeDialog(window)
        try {
            window.size = Dimension(800, 600)
            dialog.isClearFocusOnMouseDownEnabled = enabled
            dialog.setContent {
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

            dialog.size = Dimension(300, 400)
            dialog.isVisible = true

            awaitIdle()

            assertThat(textFieldIsFocused).isTrue()
            dialog.sendMousePress(x = 100, y = 300)
            dialog.sendMouseRelease(x = 100, y = 300)
            awaitIdle()

            assertThat(textFieldIsFocused).isEqualTo(!enabled)
        } finally {
            dialog.dispose()
            window.dispose()
        }
    }
}