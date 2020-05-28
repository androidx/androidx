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

import androidx.compose.Composable
import androidx.compose.state
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.LargeTest
import androidx.ui.core.Modifier
import androidx.ui.core.focus.FocusModifier
import androidx.ui.input.EditorValue
import androidx.ui.layout.width
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.text.CoreTextField
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class TextFieldFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun TextFieldApp(dataList: List<FocusTestData>) {
        for (data in dataList) {
            val editor = state { EditorValue() }
            CoreTextField(
                value = editor.value,
                modifier = Modifier.width(10.dp) + data.id,
                onValueChange = {
                    editor.value = it
                },
                onFocusChange = {
                    data.focused = it
                }
            )
        }
    }

    data class FocusTestData(val id: FocusModifier, var focused: Boolean = false)

    @Test
    @UiThreadTest
    fun requestFocus() {
        lateinit var testDataList: List<FocusTestData>

        runOnUiThread {
            composeTestRule.setContent {
                testDataList = listOf(
                    FocusTestData(FocusModifier()),
                    FocusTestData(FocusModifier()),
                    FocusTestData(FocusModifier())
                )

                TextFieldApp(testDataList)
            }
        }

        runOnIdleCompose { testDataList[0].id.requestFocus() }

        runOnIdleCompose {
            assertThat(testDataList[0].focused).isTrue()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isFalse()
        }

        runOnIdleCompose { testDataList[1].id.requestFocus() }
        runOnIdleCompose {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isTrue()
            assertThat(testDataList[2].focused).isFalse()
        }

        runOnIdleCompose { testDataList[2].id.requestFocus() }
        runOnIdleCompose {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isTrue()
        }
    }
}
