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
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material3.samples.FilledTonalCompactButtonSample
import androidx.wear.compose.material3.samples.SimpleButtonSample
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun simple_button_sample_builds() {
        rule.setContentWithTheme { SimpleButtonSample() }
    }

    @Test
    fun filled_tonal_compact_button_sample_builds() {
        rule.setContentWithTheme { FilledTonalCompactButtonSample() }
    }

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_click_action_when_enabled() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_click_action_when_disabled() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            Button(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun responds_to_long_click_when_enabled() {
        var longClicked = false

        responds_to_long_click(true, { longClicked = true }) { assertEquals(true, longClicked) }
    }

    @Test
    fun onLongClickLabel_includedInSemantics() {
        val testLabel = "Long click action"

        rule.setContentWithTheme {
            Button(
                modifier = Modifier.testTag(TEST_TAG),
                onClick = {},
                onLongClick = {},
                onLongClickLabel = testLabel,
            ) {
                Text("Button")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertOnLongClickLabelMatches(testLabel)
    }

    @Test
    fun does_not_respond_to_click_when_disabled() {
        var longClicked = false

        responds_to_long_click(false, { longClicked = true }) { assertEquals(false, longClicked) }
    }

    @Test
    fun does_not_respond_to_long_click_when_disabled() {
        var longClicked = false

        rule.setContentWithTheme {
            Button(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(false, longClicked) }
    }

    @Test
    fun has_role_button() {
        rule.setContentWithTheme {
            Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun has_role_button_for_three_slot_chip() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                label = {},
                secondaryLabel = {},
                icon = { TestImage() },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun gives_base_button_correct_text_style() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.labelMedium
            Button(onClick = {}) { actualTextStyle = LocalTextStyle.current }
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_three_slot_button_correct_text_style() {
        var actualLabelTextStyle = TextStyle.Default
        var actualSecondaryLabelTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default
        var expectedSecondaryTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.labelMedium
            expectedSecondaryTextStyle = MaterialTheme.typography.labelSmall
            Button(
                onClick = {},
                label = { actualLabelTextStyle = LocalTextStyle.current },
                secondaryLabel = { actualSecondaryLabelTextStyle = LocalTextStyle.current },
            )
        }
        assertEquals(expectedTextStyle, actualLabelTextStyle)
        assertEquals(expectedSecondaryTextStyle, actualSecondaryLabelTextStyle)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun default_shape_is_stadium() {
        rule.isShape(
            expectedShape = RoundedCornerShape(CornerSize(50)),
            colors = { ButtonDefaults.buttonColors() },
        ) { modifier ->
            Button(onClick = {}, modifier = modifier) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(expectedShape = shape, colors = { ButtonDefaults.buttonColors() }) { modifier
            ->
            Button(onClick = {}, modifier = modifier, shape = shape) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @Test
    fun gives_base_button_correct_height() {
        rule
            .setContentWithThemeForSizeAssertions { Button(onClick = {}) {} }
            .assertHeightIsEqualTo(ButtonDefaults.Height)
    }

    @Test
    fun gives_base_button_has_adjustable_height() {
        val minHeight = ButtonDefaults.Height + 1.dp

        rule
            .setContentWithThemeForSizeAssertions {
                Button(onClick = {}) {
                    Text(
                        text =
                            "Button with multiple lines of text to exceed default" +
                                " minimum height. This should exceed the minimum height for the button."
                    )
                }
            }
            .assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun gives_three_slot_button_correct_height() {
        rule
            .setContentWithThemeForSizeAssertions {
                Button(onClick = {}, label = { Text("Label") })
            }
            .assertHeightIsEqualTo(ButtonDefaults.Height)
    }

    @Test
    fun gives_three_slot_button_has_adjustable_height() {
        val minHeight = ButtonDefaults.Height + 1.dp

        rule
            .setContentWithThemeForSizeAssertions {
                Button(
                    onClick = {},
                    label = {
                        Text(
                            text =
                                "Button with multiple lines of text to exceed default" +
                                    " minimum height. This should exceed the minimum height for the button."
                        )
                    },
                )
            }
            .assertHeightIsAtLeast(minHeight)
    }

    @Test
    fun button_animate_content_size_animates_height() {
        val boxHeight = mutableStateOf(60.dp)
        val frames = 14
        val animationMillis = frames * 16
        val buttonPadding = ButtonDefaults.ButtonVerticalPadding

        rule.setContentWithTheme {
            Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG).fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier.animateContentSize(
                                animationSpec = tween(animationMillis, easing = LinearEasing)
                            )
                            .fillMaxWidth()
                            .requiredHeight(boxHeight.value)
                ) {}
            }
        }
        // Verify initial height
        rule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(60.dp + buttonPadding * 2)

        // Set autoAdvance off to test the content size animation
        rule.mainClock.autoAdvance = false
        boxHeight.value = 100.dp
        // Advance to the actual start of the animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Advance to middle of animation
        rule.mainClock.advanceTimeBy(animationMillis / 2L)
        rule.waitForIdle()
        // Verify that the animation is halfway finished
        rule
            .onNodeWithTag(TEST_TAG)
            .assertHeightIsEqualTo(80.dp + buttonPadding * 2, tolerance = 2.dp)

        // Set autoAdvance back on to finish the animation
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        // Verify end height is correct
        rule.onNodeWithTag(TEST_TAG).assertHeightIsEqualTo(100.dp + buttonPadding * 2)
    }

    @Test
    fun has_icon_in_correct_location_for_three_slot_button_and_label_only() {
        val iconTag = "TestIcon"
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            Button(
                onClick = {},
                label = { Text("Blue green orange") },
                icon = { TestImage(iconTag) },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        val itemBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag(iconTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        rule
            .onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            expectedContainerColor = { MaterialTheme.colorScheme.primary },
            expectedContentColor = { MaterialTheme.colorScheme.onPrimary },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            expectedContainerColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
            },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            expectedContainerColor = { MaterialTheme.colorScheme.surfaceContainer },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface },
            content = { FilledTonalButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            expectedContainerColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
            },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            },
            content = { FilledTonalButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_button_correct_filled_variant_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            expectedContainerColor = { MaterialTheme.colorScheme.primaryContainer },
            expectedContentColor = { MaterialTheme.colorScheme.onPrimaryContainer },
            content = { FilledVariantButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_button_correct_filled_variant_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            expectedContainerColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
            },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            },
            content = { FilledVariantButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface },
            content = { OutlinedButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            },
            content = { OutlinedButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_child_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface },
            content = { ChildButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_child_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            },
            content = { ChildButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            expectedColor = { ButtonDefaults.buttonColors() },
            content = { ThreeSlotFilledButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            expectedColor = { ButtonDefaults.buttonColors() },
            content = { ThreeSlotFilledButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            expectedColor = { ButtonDefaults.filledTonalButtonColors() },
            content = { ThreeSlotFilledTonalButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            expectedColor = { ButtonDefaults.filledTonalButtonColors() },
            content = { ThreeSlotFilledTonalButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            expectedColor = { ButtonDefaults.outlinedButtonColors() },
            content = { ThreeSlotOutlinedButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            expectedColor = { ButtonDefaults.outlinedButtonColors() },
            content = { ThreeSlotOutlinedButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_child_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            expectedColor = { ButtonDefaults.childButtonColors() },
            content = { ThreeSlotChildButton(Status.Enabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_child_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            expectedColor = { ButtonDefaults.childButtonColors() },
            content = { ThreeSlotChildButton(Status.Disabled) },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_button_correct_border_colors() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { MaterialTheme.colorScheme.outline },
            content = { modifier: Modifier ->
                OutlinedButton(onClick = {}, modifier = modifier, enabled = status.enabled()) {}
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_button_correct_border_colors() {
        val status = Status.Disabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledBorderAlpha)
            },
            content = { modifier: Modifier ->
                OutlinedButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = status.enabled()),
                ) {}
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun overrides_enabled_outlined_button_border_color() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { Color.Green },
            content = { modifier: Modifier ->
                OutlinedButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    border =
                        ButtonDefaults.outlinedButtonBorder(
                            enabled = status.enabled(),
                            borderColor = Color.Green,
                            disabledBorderColor = Color.Red,
                        ),
                ) {}
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun overrides_disabled_outlined_button_border_color() {
        val status = Status.Disabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { Color.Red },
            content = { modifier: Modifier ->
                OutlinedButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    border =
                        ButtonDefaults.outlinedButtonBorder(
                            enabled = status.enabled(),
                            borderColor = Color.Green,
                            disabledBorderColor = Color.Red,
                        ),
                ) {}
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun outlined_button_border_updates_color_when_state_changes() {
        var enabled by mutableStateOf(true)
        val enabledBorderColor = Color.Green
        val disabledBorderColor = Color.Red
        val testBackground = Color.Black

        rule.setContent {
            MaterialTheme {
                Box(Modifier.background(testBackground)) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.testTag(TEST_TAG),
                        enabled = enabled,
                        border =
                            ButtonDefaults.outlinedButtonBorder(
                                enabled = enabled,
                                borderColor = enabledBorderColor,
                                disabledBorderColor = disabledBorderColor,
                            ),
                    ) {}
                }
            }
        }
        rule.verifyBorderColor(
            contentBorderColor = enabledBorderColor,
            backgroundColor = testBackground,
        )

        rule.runOnIdle { enabled = false }

        rule.verifyBorderColor(
            contentBorderColor = disabledBorderColor,
            backgroundColor = testBackground,
        )
    }

    @Test
    fun gives_compact_button_correct_text_style() {
        var actualLabelTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.labelSmall
            CompactButton(onClick = {}, label = { actualLabelTextStyle = LocalTextStyle.current })
        }
        assertEquals(expectedTextStyle, actualLabelTextStyle)
    }

    @Test
    fun icon_only_compact_button_has_correct_default_width_and_height() {
        val iconTag = "TestIcon"
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            CompactButton(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                icon = { TestImage(iconTag) },
            )
        }

        rule
            .onRoot()
            .assertWidthIsEqualTo(ButtonDefaults.IconOnlyCompactButtonWidth)
            .assertHeightIsEqualTo(ButtonDefaults.CompactButtonHeight)
    }

    @Test
    fun label_only_compact_button_has_correct_default_height() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            CompactButton(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                label = { Text("Test") },
            )
        }

        rule.onRoot().assertHeightIsEqualTo(ButtonDefaults.CompactButtonHeight)
    }

    @Test
    fun no_content_compact_button_has_correct_default_width_and_height() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            CompactButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG))
        }

        rule
            .onRoot()
            .assertWidthIsEqualTo(ButtonDefaults.IconOnlyCompactButtonWidth)
            .assertHeightIsEqualTo(ButtonDefaults.CompactButtonHeight)
    }

    @Test
    fun icon_only_compact_button_can_have_width_overridden() {
        val iconTag = "TestIcon"
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            CompactButton(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG).width(100.dp),
                icon = { TestImage(iconTag) },
            )
        }

        rule.onRoot().assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun has_icon_in_correct_location_in_compact_button() {
        val iconTag = "TestIcon"
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            CompactButton(
                onClick = {},
                label = { Text("Blue green orange") },
                icon = { TestImage(iconTag) },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        val itemBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag(iconTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        rule
            .onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(
                (itemBounds.height - iconBounds.height) / 2 +
                    ButtonDefaults.CompactButtonTapTargetPadding.calculateTopPadding()
            )
    }

    @Test
    fun has_icon_in_correct_location_when_icon_only_compact_button() {
        val iconTag = "TestIcon"
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            CompactButton(
                onClick = {},
                icon = { TestImage(iconTag) },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        val itemBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag(iconTag, useUnmergedTree = true).getUnclippedBoundsInRoot()

        rule
            .onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(
                (itemBounds.height - iconBounds.height) / 2 +
                    ButtonDefaults.CompactButtonTapTargetPadding.calculateTopPadding()
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.buttonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.buttonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.filledTonalButtonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.filledTonalButtonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.outlinedButtonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.outlinedButtonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_child_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.childButtonColors() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_child_compact_button_correct_colors() {
        rule.verifyCompactButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.childButtonColors() },
        )
    }

    @Test
    fun button_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            Button(
                onClick = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
                secondaryLabel = {
                    secondaryLabelOverflow = LocalTextConfiguration.current.overflow
                },
            )
        }

        assertEquals(TextOverflow.Ellipsis, labelOverflow)
        assertEquals(TextOverflow.Ellipsis, secondaryLabelOverflow)
    }

    @Test
    fun button_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            Button(
                onClick = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
                secondaryLabel = {
                    secondaryLabelMaxLines = LocalTextConfiguration.current.maxLines
                },
            )
        }

        assertEquals(3, labelMaxLines)
        assertEquals(2, secondaryLabelMaxLines)
    }

    @Test
    fun button_defines_start_alignment() {
        var labelAlignment: TextAlign? = null
        var secondaryLabelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            Button(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
                secondaryLabel = {
                    secondaryLabelAlignment = LocalTextConfiguration.current.textAlign
                },
            )
        }

        assertEquals(TextAlign.Start, labelAlignment)
        assertEquals(TextAlign.Start, secondaryLabelAlignment)
    }

    @Test
    fun button_defines_center_alignment_for_label_only() {
        var labelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            Button(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
            )
        }

        assertEquals(TextAlign.Center, labelAlignment)
    }

    @Test
    fun filled_tonal_button_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            FilledTonalButton(
                onClick = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
                secondaryLabel = {
                    secondaryLabelOverflow = LocalTextConfiguration.current.overflow
                },
            )
        }

        assertEquals(TextOverflow.Ellipsis, labelOverflow)
        assertEquals(TextOverflow.Ellipsis, secondaryLabelOverflow)
    }

    @Test
    fun filled_tonal_button_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            FilledTonalButton(
                onClick = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
                secondaryLabel = {
                    secondaryLabelMaxLines = LocalTextConfiguration.current.maxLines
                },
            )
        }

        assertEquals(3, labelMaxLines)
        assertEquals(2, secondaryLabelMaxLines)
    }

    @Test
    fun filled_tonal_button_defines_start_alignment() {
        var labelAlignment: TextAlign? = null
        var secondaryLabelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            FilledTonalButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
                secondaryLabel = {
                    secondaryLabelAlignment = LocalTextConfiguration.current.textAlign
                },
            )
        }

        assertEquals(TextAlign.Start, labelAlignment)
        assertEquals(TextAlign.Start, secondaryLabelAlignment)
    }

    @Test
    fun filled_tonal_button_defines_center_alignment_for_label_only() {
        var labelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            FilledTonalButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
            )
        }

        assertEquals(TextAlign.Center, labelAlignment)
    }

    @Test
    fun outlined_button_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            OutlinedButton(
                onClick = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
                secondaryLabel = {
                    secondaryLabelOverflow = LocalTextConfiguration.current.overflow
                },
            )
        }

        assertEquals(TextOverflow.Ellipsis, labelOverflow)
        assertEquals(TextOverflow.Ellipsis, secondaryLabelOverflow)
    }

    @Test
    fun outlined_button_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            OutlinedButton(
                onClick = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
                secondaryLabel = {
                    secondaryLabelMaxLines = LocalTextConfiguration.current.maxLines
                },
            )
        }

        assertEquals(3, labelMaxLines)
        assertEquals(2, secondaryLabelMaxLines)
    }

    @Test
    fun outlined_tonal_button_defines_start_alignment() {
        var labelAlignment: TextAlign? = null
        var secondaryLabelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            OutlinedButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
                secondaryLabel = {
                    secondaryLabelAlignment = LocalTextConfiguration.current.textAlign
                },
            )
        }

        assertEquals(TextAlign.Start, labelAlignment)
        assertEquals(TextAlign.Start, secondaryLabelAlignment)
    }

    @Test
    fun outlined_button_defines_center_alignment_for_label_only() {
        var labelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            OutlinedButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
            )
        }

        assertEquals(TextAlign.Center, labelAlignment)
    }

    @Test
    fun child_button_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null
        var secondaryLabelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            ChildButton(
                onClick = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
                secondaryLabel = {
                    secondaryLabelOverflow = LocalTextConfiguration.current.overflow
                },
            )
        }

        assertEquals(TextOverflow.Ellipsis, labelOverflow)
        assertEquals(TextOverflow.Ellipsis, secondaryLabelOverflow)
    }

    @Test
    fun child_button_defines_default_maxlines() {
        var labelMaxLines: Int? = null
        var secondaryLabelMaxLines: Int? = null

        rule.setContentWithTheme {
            ChildButton(
                onClick = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
                secondaryLabel = {
                    secondaryLabelMaxLines = LocalTextConfiguration.current.maxLines
                },
            )
        }

        assertEquals(3, labelMaxLines)
        assertEquals(2, secondaryLabelMaxLines)
    }

    @Test
    fun child_button_defines_start_alignment() {
        var labelAlignment: TextAlign? = null
        var secondaryLabelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            ChildButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
                secondaryLabel = {
                    secondaryLabelAlignment = LocalTextConfiguration.current.textAlign
                },
            )
        }

        assertEquals(TextAlign.Start, labelAlignment)
        assertEquals(TextAlign.Start, secondaryLabelAlignment)
    }

    @Test
    fun child_button_defines_center_alignment_for_label_only() {
        var labelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            ChildButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
            )
        }

        assertEquals(TextAlign.Center, labelAlignment)
    }

    @Test
    fun compact_button_defines_default_overflow() {
        var labelOverflow: TextOverflow? = null

        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                label = { labelOverflow = LocalTextConfiguration.current.overflow },
            )
        }

        assertEquals(TextOverflow.Ellipsis, labelOverflow)
    }

    @Test
    fun compact_button_defines_default_maxlines() {
        var labelMaxLines: Int? = null

        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                label = { labelMaxLines = LocalTextConfiguration.current.maxLines },
            )
        }

        assertEquals(1, labelMaxLines)
    }

    @Test
    fun compact_button_defines_start_alignment() {
        var labelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
                icon = {},
            )
        }

        assertEquals(TextAlign.Start, labelAlignment)
    }

    @Test
    fun compact_button_defines_center_alignment_for_label_only() {
        var labelAlignment: TextAlign? = null

        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                label = { labelAlignment = LocalTextConfiguration.current.textAlign },
            )
        }

        assertEquals(TextAlign.Center, labelAlignment)
    }

    @Test
    fun button_long_click_triggers_haptic() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))

        rule.setContentWithTheme {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                Button(
                    onClick = { /* Do nothing */ },
                    onLongClick = {},
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    Text("Test")
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        assertThat(results).hasSize(1)
        assertThat(results).containsKey(HapticFeedbackType.LongPress)
        assertThat(results[HapticFeedbackType.LongPress]).isEqualTo(1)
    }

    @Test
    fun compactbutton_long_click_triggers_haptic() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))

        rule.setContentWithTheme {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                CompactButton(
                    onClick = { /* Do nothing */ },
                    onLongClick = {},
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    Text("Test")
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        assertThat(results).hasSize(1)
        assertThat(results).containsKey(HapticFeedbackType.LongPress)
        assertThat(results[HapticFeedbackType.LongPress]).isEqualTo(1)
    }

    private fun responds_to_long_click(
        enabled: Boolean,
        onLongClick: () -> Unit,
        assert: () -> Unit,
    ) {

        rule.setContentWithTheme {
            Button(
                onClick = { /* Do nothing */ },
                onLongClick = onLongClick,
                enabled = enabled,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assert() }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyButtonColors(
    status: Status,
    expectedContainerColor: @Composable () -> Color,
    expectedContentColor: @Composable () -> Color,
    content: @Composable () -> Color = { FilledButton(status) },
) {
    verifyColors(
        status = status,
        expectedContainerColor = expectedContainerColor,
        expectedContentColor = expectedContentColor,
        applyAlphaForDisabled = false,
        content = {
            return@verifyColors content()
        },
    )
}

@Composable
private fun FilledButton(status: Status): Color {
    var actualContentColor = Color.Transparent
    Button(onClick = {}, enabled = status.enabled(), modifier = Modifier.testTag(TEST_TAG)) {
        actualContentColor = LocalContentColor.current
    }
    return actualContentColor
}

@Composable
private fun FilledTonalButton(status: Status): Color {
    var actualContentColor = Color.Transparent
    FilledTonalButton(
        onClick = {},
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
    ) {
        actualContentColor = LocalContentColor.current
    }
    return actualContentColor
}

@Composable
private fun FilledVariantButton(status: Status): Color {
    var actualContentColor = Color.Transparent
    Button(
        onClick = {},
        colors = ButtonDefaults.filledVariantButtonColors(),
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
    ) {
        actualContentColor = LocalContentColor.current
    }
    return actualContentColor
}

@Composable
private fun OutlinedButton(status: Status): Color {
    var actualContentColor = Color.Transparent
    OutlinedButton(
        onClick = {},
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
    ) {
        actualContentColor = LocalContentColor.current
    }
    return actualContentColor
}

@Composable
private fun ChildButton(status: Status): Color {
    var actualContentColor = Color.Transparent
    ChildButton(onClick = {}, enabled = status.enabled(), modifier = Modifier.testTag(TEST_TAG)) {
        actualContentColor = LocalContentColor.current
    }
    return actualContentColor
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyThreeSlotButtonColors(
    status: Status,
    expectedColor: @Composable () -> ButtonColors,
    content: @Composable () -> ThreeSlotButtonColors,
) {
    val testBackgroundColor = Color.White
    var containerColor = Color.Transparent
    var labelColor = Color.Transparent
    var secondaryLabelColor = Color.Transparent
    var iconColor = Color.Transparent
    lateinit var threeSlotButtonColors: ThreeSlotButtonColors

    setContentWithTheme {
        val buttonColors = expectedColor()
        containerColor =
            (buttonColors.containerColor(status.enabled())).compositeOver(testBackgroundColor)
        labelColor = buttonColors.contentColor(status.enabled())
        secondaryLabelColor = buttonColors.secondaryContentColor(status.enabled())
        iconColor = buttonColors.iconColor(status.enabled())

        Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
            threeSlotButtonColors = content()
        }
    }

    assertEquals(threeSlotButtonColors.labelColor, labelColor)
    assertEquals(threeSlotButtonColors.secondaryLabelColor, secondaryLabelColor)
    assertEquals(threeSlotButtonColors.iconColor, iconColor)

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(
            if (containerColor != Color.Transparent) containerColor else testBackgroundColor
        )
}

@Composable
private fun ThreeSlotFilledButton(status: Status): ThreeSlotButtonColors {
    var actualLabelColor: Color = Color.Transparent
    var actualSecondaryLabelColor: Color = Color.Transparent
    var actualIconColor: Color = Color.Transparent
    Button(
        onClick = {},
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
        label = { actualLabelColor = LocalContentColor.current },
        secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
        icon = { actualIconColor = LocalContentColor.current },
    )
    return ThreeSlotButtonColors(actualLabelColor, actualSecondaryLabelColor, actualIconColor)
}

@Composable
private fun ThreeSlotFilledTonalButton(status: Status): ThreeSlotButtonColors {
    var actualLabelColor: Color = Color.Transparent
    var actualSecondaryLabelColor: Color = Color.Transparent
    var actualIconColor: Color = Color.Transparent
    FilledTonalButton(
        onClick = {},
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
        label = { actualLabelColor = LocalContentColor.current },
        secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
        icon = { actualIconColor = LocalContentColor.current },
    )
    return ThreeSlotButtonColors(actualLabelColor, actualSecondaryLabelColor, actualIconColor)
}

@Composable
private fun ThreeSlotOutlinedButton(status: Status): ThreeSlotButtonColors {
    var actualLabelColor: Color = Color.Transparent
    var actualSecondaryLabelColor: Color = Color.Transparent
    var actualIconColor: Color = Color.Transparent
    OutlinedButton(
        onClick = {},
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
        label = { actualLabelColor = LocalContentColor.current },
        secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
        icon = { actualIconColor = LocalContentColor.current },
    )
    return ThreeSlotButtonColors(actualLabelColor, actualSecondaryLabelColor, actualIconColor)
}

@Composable
private fun ThreeSlotChildButton(status: Status): ThreeSlotButtonColors {
    var actualLabelColor: Color = Color.Transparent
    var actualSecondaryLabelColor: Color = Color.Transparent
    var actualIconColor: Color = Color.Transparent
    ChildButton(
        onClick = {},
        enabled = status.enabled(),
        modifier = Modifier.testTag(TEST_TAG),
        label = { actualLabelColor = LocalContentColor.current },
        secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
        icon = { actualIconColor = LocalContentColor.current },
    )
    return ThreeSlotButtonColors(actualLabelColor, actualSecondaryLabelColor, actualIconColor)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyButtonBorderColor(
    expectedBorderColor: @Composable () -> Color,
    content: @Composable (Modifier) -> Unit,
) {
    val testBackground = Color.Black
    var finalExpectedBorderColor = Color.Transparent

    setContentWithTheme {
        finalExpectedBorderColor = expectedBorderColor().compositeOver(testBackground)
        Box(Modifier.fillMaxSize().background(testBackground)) {
            content(Modifier.testTag(TEST_TAG))
        }
    }

    onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(finalExpectedBorderColor)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.isShape(
    expectedShape: Shape,
    colors: @Composable () -> ButtonColors,
    content: @Composable (Modifier) -> Unit,
) {
    var background = Color.Transparent
    var buttonColor = Color.Transparent
    val padding = 0.dp

    setContentWithTheme {
        background = MaterialTheme.colorScheme.surfaceContainer
        Box(Modifier.background(background)) {
            buttonColor = colors().containerColor(true)
            if (buttonColor == Color.Transparent) {
                buttonColor = background
            }
            content(Modifier.testTag(TEST_TAG).padding(padding))
        }
    }

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertShape(
            density = density,
            horizontalPadding = 0.dp,
            verticalPadding = 0.dp,
            shapeColor = buttonColor,
            backgroundColor = background,
            antiAliasingGap = 2.0f,
            shape = expectedShape,
        )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyCompactButtonColors(
    status: Status,
    colors: @Composable () -> ButtonColors,
) {
    val testBackgroundColor = Color.White
    var containerColor = Color.Transparent
    var labelColor = Color.Transparent
    var iconColor = Color.Transparent
    var actualLabelColor = Color.Transparent
    var actualIconColor = Color.Transparent

    setContentWithTheme {
        containerColor =
            (colors().containerColor(status.enabled())).compositeOver(testBackgroundColor)
        labelColor = colors().contentColor(status.enabled())
        iconColor = colors().iconColor(status.enabled())

        Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
            CompactButton(
                onClick = {},
                colors = colors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag(TEST_TAG),
                label = { actualLabelColor = LocalContentColor.current },
                icon = { actualIconColor = LocalContentColor.current },
            )
        }
    }

    assertEquals(actualLabelColor, labelColor)
    assertEquals(actualIconColor, iconColor)

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(
            if (containerColor != Color.Transparent) containerColor else testBackgroundColor
        )
}

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyBorderColor(
    contentBorderColor: Color,
    backgroundColor: Color,
) {
    val expectedColor = contentBorderColor.compositeOver(backgroundColor)
    onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
}

val MinimumButtonTapSize = 48.dp

private data class ThreeSlotButtonColors(
    val labelColor: Color,
    val secondaryLabelColor: Color,
    val iconColor: Color,
)
