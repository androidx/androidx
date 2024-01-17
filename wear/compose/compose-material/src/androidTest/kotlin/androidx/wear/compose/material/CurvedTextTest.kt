/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.O)
class CurvedTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val testText = "TestText"

    @Test
    fun color_parameter_overrides_styleColor() {
        rule.setContent {
            CurvedLayout {
                curvedRow {
                    curvedText(
                        text = testText,
                        color = Color.Red,
                        style = CurvedTextStyle(
                            color = Color.Blue
                        )
                    )
                }
            }
        }

        val curvedTextImage = rule.onNodeWithContentDescription(testText).captureToImage()
        curvedTextImage.assertContainsColor(Color.Red)
        curvedTextImage.assertDoesNotContainColor(Color.Blue)
    }

    @Test
    fun styleColor_overrides_LocalContentColor() {
        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Yellow) {
                CurvedLayout {
                    curvedRow {
                        curvedText(
                            text = testText,
                            style = CurvedTextStyle(
                                color = Color.Blue
                            )
                        )
                    }
                }
            }
        }

        val curvedTextImage = rule.onNodeWithContentDescription(testText).captureToImage()
        curvedTextImage.assertContainsColor(Color.Blue)
        curvedTextImage.assertDoesNotContainColor(Color.Yellow)
    }

    @Test
    fun uses_LocalContentColor_as_fallback() {
        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Yellow) {
                CurvedLayout {
                    curvedRow {
                        curvedText(
                            text = testText,
                        )
                    }
                }
            }
        }

        rule.onNodeWithContentDescription(testText).captureToImage()
            .assertContainsColor(Color.Yellow)
    }
}
