/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.Providers
import androidx.compose.state
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.input.TextInputService
import androidx.ui.layout.fillMaxSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.find
import androidx.ui.test.hasInputMethodsSupport
import androidx.ui.test.runOnIdleCompose
import androidx.ui.text.SoftwareKeyboardController
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SoftwareKeyboardTest {
    @get:Rule
    val composeTestRule = createComposeRule().also {
        it.clockTestRule.pauseClock()
    }

    @Test
    fun textField_onTextLayoutCallback() {
        val textInputService = mock<TextInputService>()
        val inputSessionToken = 10 // any positive number is fine.

        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        val onTextInputStarted: (SoftwareKeyboardController) -> Unit = mock()
        composeTestRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                val state = state { TextFieldValue("") }
                TextField(
                    value = state.value,
                    modifier = Modifier.fillMaxSize(),
                    onValueChange = {
                        state.value = it
                    },
                    onTextInputStarted = onTextInputStarted
                )
            }
        }

        // Perform click to focus in.
        find(hasInputMethodsSupport())
            .doClick()

        runOnIdleCompose {
            verify(onTextInputStarted, times(1)).invoke(any())
        }
    }
}
