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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class IconToggleButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun iconToggleButtonEnabledAndChecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton() },
        )

    @Test
    fun iconToggleButtonEnabledAndUnchecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(checked = false) },
        )

    @Test
    fun iconToggleButtonDisabledAndChecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(enabled = false) },
        )

    @Test
    fun iconToggleButtonDisabledAndUnchecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(enabled = false, checked = false) },
        )

    @Test
    fun iconToggleButtonWithOffset() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { sampleIconToggleButton(modifier = Modifier.offset(10.dp)) },
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
                        shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
                        interactionSource = interactionSource,
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)

        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun animatedIconToggleButtonChecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                sampleIconToggleButton(
                    checked = true,
                    shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
                )
            },
        )

    @Test
    fun animatedIconToggleButtonUnchecked() =
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = {
                sampleIconToggleButton(
                    checked = false,
                    shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
                )
            },
        )

    @Test
    fun icon_toggle_button_inset_focus_ring() {
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
                        IconToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            interactionSource = checkedInteractionSource,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favourite",
                            )
                        }
                        IconToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            interactionSource = uncheckedInteractionSource,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favourite",
                            )
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
    fun icon_toggle_button_animated_shapes_inset_focus_ring() {
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
                        IconToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            shapes = IconToggleButtonDefaults.animatedShapes(),
                            interactionSource = animatedCheckedInteractionSource,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favourite",
                            )
                        }
                        IconToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            shapes = IconToggleButtonDefaults.animatedShapes(),
                            interactionSource = animatedUncheckedInteractionSource,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favourite",
                            )
                        }
                        IconToggleButton(
                            checked = true,
                            onCheckedChange = {},
                            shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
                            interactionSource = variantCheckedInteractionSource,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favourite",
                            )
                        }
                        IconToggleButton(
                            checked = false,
                            onCheckedChange = {},
                            shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
                            interactionSource = variantUncheckedInteractionSource,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favourite",
                            )
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
    private fun sampleIconToggleButton(
        enabled: Boolean = true,
        checked: Boolean = true,
        modifier: Modifier = Modifier,
        shapes: IconToggleButtonShapes = IconToggleButtonDefaults.shapes(),
        interactionSource: MutableInteractionSource? = null,
    ) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = {},
            enabled = enabled,
            modifier = modifier.testTag(TEST_TAG),
            shapes = shapes,
            interactionSource = interactionSource,
        ) {
            Icon(imageVector = Icons.Outlined.Star, contentDescription = "Favourite")
        }
    }
}
