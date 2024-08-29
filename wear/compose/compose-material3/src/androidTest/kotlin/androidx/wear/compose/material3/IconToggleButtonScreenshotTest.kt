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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class IconToggleButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun iconToggleButtonEnabledAndChecked() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton() }
        )

    @Test
    fun iconToggleButtonEnabledAndUnchecked() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(checked = false) }
        )

    @Test
    fun iconToggleButtonDisabledAndChecked() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(enabled = false) }
        )

    @Test
    fun iconToggleButtonDisabledAndUnchecked() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(enabled = false, checked = false) }
        )

    @Test
    fun iconToggleButtonWithOffset() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(modifier = Modifier.offset(10.dp)) }
        )

    @Ignore("TODO: b/345199060 work out how to show pressed state in test")
    @Test
    fun animatedIconToggleButtonPressed() {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    val interactionSource = remember {
                        MutableInteractionSource().apply {
                            tryEmit(PressInteraction.Press(Offset(0f, 0f)))
                        }
                    }
                    sampleIconToggleButton(
                        checked = false,
                        shape =
                            IconToggleButtonDefaults.animatedToggleButtonShape(
                                interactionSource = interactionSource,
                                checked = false
                            ),
                        interactionSource = interactionSource
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(rule = screenshotRule, goldenIdentifier = testName.methodName)
    }

    @Test
    fun animatedIconToggleButtonChecked() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                val interactionSource = remember { MutableInteractionSource() }
                sampleIconToggleButton(
                    checked = true,
                    shape =
                        IconToggleButtonDefaults.animatedToggleButtonShape(
                            interactionSource = interactionSource,
                            checked = true
                        ),
                    interactionSource = interactionSource
                )
            }
        )

    @Test
    fun animatedIconToggleButtonUnchecked() =
        rule.verifyScreenshot(
            methodName = testName.methodName,
            screenshotRule = screenshotRule,
            content = {
                val interactionSource = remember { MutableInteractionSource() }
                sampleIconToggleButton(
                    checked = false,
                    shape =
                        IconToggleButtonDefaults.animatedToggleButtonShape(
                            interactionSource = interactionSource,
                            checked = false
                        ),
                    interactionSource = interactionSource
                )
            }
        )

    @Composable
    private fun sampleIconToggleButton(
        enabled: Boolean = true,
        checked: Boolean = true,
        modifier: Modifier = Modifier,
        shape: Shape = TextButtonDefaults.shape,
        interactionSource: MutableInteractionSource? = null
    ) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = {},
            enabled = enabled,
            modifier = modifier.testTag(TEST_TAG),
            shape = shape,
            interactionSource = interactionSource
        ) {
            Icon(imageVector = Icons.Outlined.Star, contentDescription = "Favourite")
        }
    }
}
