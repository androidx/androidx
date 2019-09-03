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

package androidx.ui.core.test

import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.LargeTest
import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.TestTag
import androidx.ui.core.TextField
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.input.FocusManager
import androidx.ui.input.EditorModel
import androidx.ui.input.TextInputService
import androidx.ui.test.createComposeRule
import androidx.ui.test.waitForIdleCompose
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            val editor = +state { EditorModel() }
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
        val textInputService = mock<TextInputService>()

        val testDataList = listOf(
            FocusTestData("ID1"),
            FocusTestData("ID2"),
            FocusTestData("ID3")
        )

        composeTestRule.setContent {
            FocusManagerAmbient.Provider(value = focusManager) {
                TextInputServiceAmbient.Provider(value = textInputService) {
                    TestTag(tag = "textField") {
                        TextFieldApp(testDataList)
                    }
                }
            }
        }

        composeTestRule.runOnUiThread { focusManager.requestFocusById(testDataList[0].id) }
        waitForIdleCompose()

        assertTrue(testDataList[0].focused)
        assertFalse(testDataList[1].focused)
        assertFalse(testDataList[2].focused)

        composeTestRule.runOnUiThread { focusManager.requestFocusById(testDataList[1].id) }
        waitForIdleCompose()

        assertFalse(testDataList[0].focused)
        assertTrue(testDataList[1].focused)
        assertFalse(testDataList[2].focused)

        composeTestRule.runOnUiThread { focusManager.requestFocusById(testDataList[2].id) }
        waitForIdleCompose()

        assertFalse(testDataList[0].focused)
        assertFalse(testDataList[1].focused)
        assertTrue(testDataList[2].focused)
    }
}
