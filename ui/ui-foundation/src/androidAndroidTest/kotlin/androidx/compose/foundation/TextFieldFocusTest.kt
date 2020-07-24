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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.LargeTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.width
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.ui.FocusModifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(b/161297615): Replace the deprecated FocusModifier with the new Focus API.
@Suppress("DEPRECATION")
@LargeTest
@RunWith(JUnit4::class)
class TextFieldFocusTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun TextFieldApp(dataList: List<FocusTestData>) {
        for (data in dataList) {
            val editor = state { TextFieldValue() }
            CoreTextField(
                value = editor.value,
                modifier = Modifier.width(10.dp).then(data.id),
                onValueChange = {
                    editor.value = it
                },
                onFocusChanged = {
                    data.focused = it
                }
            )
        }
    }

    // TODO(b/161297615): Replace FocusModifier with Modifier.focus()
    data class FocusTestData(val id: FocusModifier, var focused: Boolean = false)

    @Test
    @UiThreadTest
    fun requestFocus() {
        lateinit var testDataList: List<FocusTestData>

        runOnUiThread {
            composeTestRule.setContent {
                testDataList = listOf(
                    // TODO(b/161297615): Replace FocusModifier with Modifier.focus()
                    FocusTestData(FocusModifier()),
                    FocusTestData(FocusModifier()),
                    FocusTestData(FocusModifier())
                )

                TextFieldApp(testDataList)
            }
        }

        runOnIdle { testDataList[0].id.requestFocus() }

        runOnIdle {
            assertThat(testDataList[0].focused).isTrue()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isFalse()
        }

        runOnIdle { testDataList[1].id.requestFocus() }
        runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isTrue()
            assertThat(testDataList[2].focused).isFalse()
        }

        runOnIdle { testDataList[2].id.requestFocus() }
        runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isTrue()
        }
    }
}
