/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PickerScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    private val screenHeight = 150.dp

    @Test
    fun picker() = verifyScreenshot {
        samplePicker()
    }

    @Test
    fun picker_without_gradient() = verifyScreenshot {
        samplePicker(gradientRatio = 0f)
    }

    @Test
    fun picker_negative_separation() = verifyScreenshot {
        samplePicker(separation = -8.dp)
    }

    @Test
    fun dual_picker() = verifyScreenshot {
        dualPicker()
    }

    @Test
    fun dual_picker_with_readonlylabel() = verifyScreenshot {
        dualPicker(readOnlyLabel = "Min")
    }

    @Composable
    private fun samplePicker(
        gradientRatio: Float = PickerDefaults.DefaultGradientRatio,
        separation: Dp = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .height(screenHeight).fillMaxWidth().background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            val items = listOf("One", "Two", "Three", "Four", "Five")
            val state = rememberPickerState(items.size)
            Picker(
                modifier = Modifier.fillMaxSize().testTag(TEST_TAG),
                state = state,
                gradientRatio = gradientRatio,
                separation = separation,
                contentDescription = "",
            ) {
                Text(items[it])
            }
        }
    }

    @Composable
    private fun dualPicker(readOnlyLabel: String? = null) {
        // This test verifies read-only mode alongside an 'editable' Picker.
        val textStyle = MaterialTheme.typography.display1

        @Composable
        fun Option(color: Color, text: String) = Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = text, style = textStyle, color = color,
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentSize()
            )
        }

        Row(
            modifier = Modifier
                .height(screenHeight)
                .fillMaxWidth()
                .background(MaterialTheme.colors.background)
                .testTag(TEST_TAG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Picker(
                state = rememberPickerState(
                    initialNumberOfOptions = 100,
                    initiallySelectedOption = 11
                ),
                contentDescription = "",
                readOnly = false,
                modifier = Modifier.size(64.dp, 100.dp),
                option = { Option(MaterialTheme.colors.secondary, "%2d".format(it)) }
            )
            Spacer(Modifier.width(8.dp))
            Text(text = ":", style = textStyle, color = MaterialTheme.colors.onBackground)
            Spacer(Modifier.width(8.dp))
            Picker(
                state = rememberPickerState(
                    initialNumberOfOptions = 100,
                    initiallySelectedOption = 100
                ),
                contentDescription = "",
                readOnly = true,
                readOnlyLabel = { if (readOnlyLabel != null) LabelText(readOnlyLabel) },
                modifier = Modifier.size(64.dp, 100.dp),
                option = { Option(MaterialTheme.colors.onBackground, "%02d".format(it)) }
            )
        }
    }

    @Composable
    private fun BoxScope.LabelText(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 8.dp)
        )
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                content()
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
