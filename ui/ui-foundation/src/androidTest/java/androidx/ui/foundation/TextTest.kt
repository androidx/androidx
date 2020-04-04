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

package androidx.ui.foundation

import androidx.test.filters.MediumTest
import androidx.ui.graphics.Color
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.sp
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class TextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ExpectedTextStyle = TextStyle(
        color = Color.Blue,
        textAlign = TextAlign.End,
        fontSize = 32.sp,
        fontStyle = FontStyle.Italic
    )

    private val TestText = "TestText"

    @Test
    fun inheritsCurrentTextStyle() {
        var textColor: Color? = null
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    Text(TestText, onTextLayout = { textColor = it.layoutInput.style.color })
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(textColor).isEqualTo(ExpectedTextStyle.color)
        }
    }

    @Test
    fun settingCustomTextStyle() {
        var textColor: Color? = null
        val testStyle = TextStyle(color = Color.Green)
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    Text(
                        TestText,
                        style = testStyle,
                        onTextLayout = { textColor = it.layoutInput.style.color }
                    )
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(textColor).isEqualTo(testStyle.color)
        }
    }

    @Test
    fun settingColorExplicitly() {
        var textColor: Color? = null
        val expectedColor = Color.Green
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    Text(
                        TestText,
                        color = expectedColor,
                        onTextLayout = { textColor = it.layoutInput.style.color }
                    )
                }
            }
        }

        runOnIdleCompose {
            // `color` parameter should override style.
            Truth.assertThat(textColor).isEqualTo(expectedColor)
        }
    }

    // Not really an expected use-case, but we should ensure the behavior here is consistent.
    @Test
    fun settingColorAndTextStyle() {
        var textColor: Color? = null
        val expectedColor = Color.Green
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    // Set both color and style
                    Text(
                        TestText,
                        color = expectedColor,
                        style = ExpectedTextStyle,
                        onTextLayout = { textColor = it.layoutInput.style.color }
                    )
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(textColor).isEqualTo(expectedColor)
        }
    }
}
