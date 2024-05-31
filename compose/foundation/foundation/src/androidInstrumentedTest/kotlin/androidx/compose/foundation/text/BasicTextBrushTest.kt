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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextBrushTest {

    @get:Rule val rule = createComposeRule()

    private val TAG = "TAG"

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun toggleSolidColorBrush() {
        val colorState = mutableStateOf<Brush>(SolidColor(Color.Red))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicText(
                    text = "Hello",
                    style =
                        TextStyle(
                            brush = colorState.value,
                            fontFamily = TEST_FONT_FAMILY,
                            fontSize = 20.sp
                        ),
                    modifier = Modifier.background(Color.Black).testTag(TAG)
                )
            }
        }

        with(rule.onNodeWithTag(TAG).captureToImage()) {
            assertContainsColor(Color.Black)
            assertContainsColor(Color.Red)
            assertDoesNotContainColor(Color.Blue)
        }

        colorState.value = SolidColor(Color.Blue)

        with(rule.onNodeWithTag(TAG).captureToImage()) {
            assertContainsColor(Color.Black)
            assertContainsColor(Color.Blue)
            assertDoesNotContainColor(Color.Red)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun togglePredefinedShaderBrush() {
        val brushState = mutableStateOf(Brush.horizontalGradient(listOf(Color.Red, Color.Red)))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicText(
                    text = "Hello",
                    style =
                        TextStyle(
                            brush = brushState.value,
                            fontFamily = TEST_FONT_FAMILY,
                            fontSize = 20.sp
                        ),
                    modifier = Modifier.background(Color.Black).testTag(TAG)
                )
            }
        }

        with(rule.onNodeWithTag(TAG).captureToImage()) {
            assertContainsColor(Color.Black)
            assertContainsColor(Color.Red)
            assertDoesNotContainColor(Color.Blue)
        }

        brushState.value = Brush.horizontalGradient(listOf(Color.Blue, Color.Blue))

        with(rule.onNodeWithTag(TAG).captureToImage()) {
            assertContainsColor(Color.Black)
            assertContainsColor(Color.Blue)
            assertDoesNotContainColor(Color.Red)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun toggleCustomShaderBrush() {
        var color by mutableStateOf(Color.Red)
        val brush =
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    return LinearGradientShader(
                        Offset.Zero,
                        Offset(200f, 200f),
                        listOf(color, color),
                    )
                }
            }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicText(
                    text = "Hello",
                    style =
                        TextStyle(brush = brush, fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    modifier = Modifier.background(Color.Black).testTag(TAG)
                )
            }
        }

        with(rule.onNodeWithTag(TAG).captureToImage()) {
            assertContainsColor(Color.Black)
            assertContainsColor(Color.Red)
            assertDoesNotContainColor(Color.Blue)
        }

        color = Color.Blue

        with(rule.onNodeWithTag(TAG).captureToImage()) {
            assertContainsColor(Color.Black)
            assertContainsColor(Color.Blue)
            assertDoesNotContainColor(Color.Red)
        }
    }
}
