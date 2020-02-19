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

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.state
import androidx.test.filters.LargeTest
import androidx.ui.core.input.FocusManager
import androidx.ui.input.TextInputService
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class TextFieldFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    data class FocusTestData(val id: String, var focused: Boolean = false)

    @Composable
    private fun TextFieldApp(dataList: List<FocusTestData>) {
        for (data in dataList) {
            val editor = state { "" }
            TextField(
                value = editor.value,
                onValueChange = {
                    editor.value = it
                },
                focusIdentifier = data.id,
                onFocus = { data.focused = true },
                onBlur = { data.focused = false }
            )
        }
    }

    @Test
    fun requestFocus() {
        val focusManager = FocusManager()
        val inputSessionToken = 10
        val textInputService = mock<TextInputService>()
        whenever(textInputService.startInput(any(), any(), any(), any(), any()))
            .thenReturn(inputSessionToken)

        val testDataList = listOf(
            FocusTestData("ID1"),
            FocusTestData("ID2"),
            FocusTestData("ID3")
        )

        composeTestRule.setContent {
            Providers(
                FocusManagerAmbient provides focusManager,
                TextInputServiceAmbient provides textInputService
            ) {
                TestTag(tag = "textField") {
                    TextFieldApp(testDataList)
                }
            }
        }

        composeTestRule.runOnUiThread { focusManager.requestFocusById(testDataList[0].id) }
        composeTestRule.runOnIdleCompose {
            assertThat(testDataList[0].focused).isTrue()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isFalse()
        }

        composeTestRule.runOnUiThread { focusManager.requestFocusById(testDataList[1].id) }
        composeTestRule.runOnIdleCompose {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isTrue()
            assertThat(testDataList[2].focused).isFalse()
        }

        composeTestRule.runOnUiThread { focusManager.requestFocusById(testDataList[2].id) }
        composeTestRule.runOnIdleCompose {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isTrue()
        }
    }
}
