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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class TextToggleButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun textToggleButtonEnabledAndChecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleTextToggleButton() },
        )

    @Test
    fun textToggleButtonEnabledAndUnchecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleTextToggleButton(checked = false) },
        )

    @Test
    fun textToggleButtonDisabledAndChecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleTextToggleButton(enabled = false) },
        )

    @Test
    fun textToggleButtonDisabledAndUnchecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleTextToggleButton(enabled = false, checked = false) },
        )

    @Test
    fun textToggleButtonWithOffset() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleTextToggleButton(modifier = Modifier.offset(10.dp)) },
        )

    @Ignore("TODO: b/345199060 work out how to show pressed state in test")
    @Test
    fun animatedTextToggleButtonPressed() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                val interactionSource = remember {
                    MutableInteractionSource().apply {
                        tryEmit(PressInteraction.Press(Offset(0f, 0f)))
                    }
                }
                sampleTextToggleButton(
                    checked = false,
                    shapes = TextToggleButtonDefaults.variantAnimatedShapes(),
                    interactionSource = interactionSource,
                )
            },
        )

    @Test
    fun animatedTextToggleButtonChecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                sampleTextToggleButton(
                    checked = true,
                    shapes = TextToggleButtonDefaults.variantAnimatedShapes(),
                )
            },
        )

    @Test
    fun animatedTextToggleButtonUnchecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                sampleTextToggleButton(
                    checked = false,
                    shapes = TextToggleButtonDefaults.variantAnimatedShapes(),
                )
            },
        )

    @Test
    fun text_toggle_button_inset_focus_ring() {
        val checkedInteractionSource = MutableInteractionSource()
        val uncheckedInteractionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalRippleThemeConfiguration provides
                    RippleDefaults.InsetFocusRingThemeConfiguration
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier.testTag(TEST_TAG),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            interactionSource = checkedInteractionSource,
                        ) {
                            Text(text = "ON")
                        }
                        TextToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            interactionSource = uncheckedInteractionSource,
                        ) {
                            Text(text = "OFF")
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            checkedInteractionSource.tryEmit(FocusInteraction.Focus())
            uncheckedInteractionSource.tryEmit(FocusInteraction.Focus())
        }
        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun text_toggle_button_animated_shapes_inset_focus_ring() {
        val animatedCheckedInteractionSource = MutableInteractionSource()
        val animatedUncheckedInteractionSource = MutableInteractionSource()
        val variantCheckedInteractionSource = MutableInteractionSource()
        val variantUncheckedInteractionSource = MutableInteractionSource()
        rule.setContentWithTheme {
            CompositionLocalProvider(
                LocalRippleThemeConfiguration provides
                    RippleDefaults.InsetFocusRingThemeConfiguration
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier.testTag(TEST_TAG),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            shapes = TextToggleButtonDefaults.animatedShapes(),
                            interactionSource = animatedCheckedInteractionSource,
                        ) {
                            Text(text = "ON")
                        }
                        TextToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            shapes = TextToggleButtonDefaults.animatedShapes(),
                            interactionSource = animatedUncheckedInteractionSource,
                        ) {
                            Text(text = "OFF")
                        }
                        TextToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            shapes = TextToggleButtonDefaults.variantAnimatedShapes(),
                            interactionSource = variantCheckedInteractionSource,
                        ) {
                            Text(text = "ON")
                        }
                        TextToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            shapes = TextToggleButtonDefaults.variantAnimatedShapes(),
                            interactionSource = variantUncheckedInteractionSource,
                        ) {
                            Text(text = "OFF")
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            animatedCheckedInteractionSource.tryEmit(FocusInteraction.Focus())
            animatedCheckedInteractionSource.tryEmit(PressInteraction.Press(Offset.Zero))
            animatedUncheckedInteractionSource.tryEmit(FocusInteraction.Focus())
            animatedUncheckedInteractionSource.tryEmit(PressInteraction.Press(Offset.Zero))
            variantCheckedInteractionSource.tryEmit(FocusInteraction.Focus())
            variantCheckedInteractionSource.tryEmit(PressInteraction.Press(Offset.Zero))
            variantUncheckedInteractionSource.tryEmit(FocusInteraction.Focus())
            variantUncheckedInteractionSource.tryEmit(PressInteraction.Press(Offset.Zero))
        }
        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Composable
    private fun sampleTextToggleButton(
        enabled: Boolean = true,
        checked: Boolean = true,
        modifier: Modifier = Modifier,
        shapes: TextToggleButtonShapes = TextToggleButtonDefaults.shapes(),
        interactionSource: MutableInteractionSource? = null,
    ) {
        TextToggleButton(
            checked = checked,
            onCheckedChange = {},
            enabled = enabled,
            modifier = modifier.testTag(TEST_TAG),
            shapes = shapes,
            interactionSource = interactionSource,
        ) {
            Text(text = if (checked) "ON" else "OFF")
        }
    }
}
