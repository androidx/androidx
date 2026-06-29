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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontWeightAdjustment
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.material3.tokens.TypographyTokens
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class CurvedTextTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val testText = "TestText"

    @Test
    fun color_parameter_overrides_styleColor() {
        rule.setContent {
            CurvedLayout {
                curvedRow {
                    curvedText(
                        text = testText,
                        color = Color.Red,
                        style = CurvedTextStyle(color = Color.Blue),
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
                        curvedText(text = testText, style = CurvedTextStyle(color = Color.Blue))
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
                CurvedLayout { curvedRow { curvedText(text = testText) } }
            }
        }

        rule
            .onNodeWithContentDescription(testText)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
    }

    @Test
    fun uses_ArcMedium_style() {
        rule.setContent {
            MaterialTheme(
                typography =
                    Typography(arcMedium = TypographyTokens.ArcMedium.copy(color = Color.Yellow))
            ) {
                CurvedLayout { curvedText(text = testText) }
            }
        }

        rule
            .onNodeWithContentDescription(testText)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
    }

    @Test
    fun fontScaleIncreased_increasesWidth() {
        val text = "SizeTest"
        val fontScale = mutableStateOf(1f)

        rule.setContent {
            val currentDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density = currentDensity, fontScale = fontScale.value)
            ) {
                CurvedLayout { curvedText(text = text, fontFamily = FontFamily.Default) }
            }
        }

        val width1 = rule.onNodeWithContentDescription(text).fetchSemanticsNode().size.width

        fontScale.value = 2f
        rule.waitForIdle()

        val width2 = rule.onNodeWithContentDescription(text).fetchSemanticsNode().size.width

        assertWithMessage("Expected width to increase with font scale")
            .that(width2)
            .isGreaterThan(width1)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun boldTextSetting_increasesWidth() {
        val text = "WeightTest"
        val fontWeightAdjustment = mutableIntStateOf(0)

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.FontWeightAdjustment(fontWeightAdjustment.intValue)
            ) {
                CurvedLayout { curvedText(text = text, fontFamily = FontFamily.Default) }
            }
        }

        val width1 = rule.onNodeWithContentDescription(text).fetchSemanticsNode().size.width

        // +300 corresponds to the system "Bold Text" accessibility setting weight adjustment
        fontWeightAdjustment.intValue = 300
        rule.waitForIdle()

        val width2 = rule.onNodeWithContentDescription(text).fetchSemanticsNode().size.width

        assertWithMessage("Expected curved text width to increase when system Bold Text is enabled")
            .that(width2)
            .isGreaterThan(width1)
    }
}
