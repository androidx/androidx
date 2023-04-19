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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_click_action_when_enabled() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_click_action_when_disabled() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
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
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            Button(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun has_role_button() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
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

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun gives_base_button_correct_text_style() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.buttonMedium
            Button(
                onClick = {},
            ) {
                actualTextStyle = LocalTextStyle.current
            }
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
            expectedTextStyle = MaterialTheme.typography.buttonMedium
            expectedSecondaryTextStyle = MaterialTheme.typography.captionLarge
            Button(
                onClick = {},
                label = {
                    actualLabelTextStyle = LocalTextStyle.current
                },
                secondaryLabel = {
                    actualSecondaryLabelTextStyle = LocalTextStyle.current
                }
            )
        }
        assertEquals(expectedTextStyle, actualLabelTextStyle)
        assertEquals(expectedSecondaryTextStyle, actualSecondaryLabelTextStyle)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun default_shape_is_stadium() {
        rule.isShape(
            expectedShape = RoundedCornerShape(CornerSize(50)),
            colors = { ButtonDefaults.buttonColors() }
        ) { modifier ->
            Button(
                onClick = {},
                modifier = modifier
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(
            expectedShape = shape,
            colors = { ButtonDefaults.buttonColors() }
        ) { modifier ->
            Button(
                onClick = {},
                modifier = modifier,
                shape = shape
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @Test
    fun gives_base_button_correct_height() {
        rule.setContentWithThemeForSizeAssertions {
            Button(
                onClick = {}
            ) {}
        }
            .assertHeightIsEqualTo(ButtonDefaults.Height)
    }

    @Test
    fun gives_three_slot_button_correct_height() {
        rule.setContentWithThemeForSizeAssertions {
            Button(
                onClick = {},
                label = { Text("Label") }
            )
        }
            .assertHeightIsEqualTo(ButtonDefaults.Height)
    }

    @Test
    fun has_icon_in_correct_location_for_three_slot_button_and_label_only() {
        val iconTag = "TestIcon"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                Button(
                    onClick = {},
                    label = { Text("Blue green orange") },
                    icon = { TestImage(iconTag) },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        val itemBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(iconTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.filledButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.primary },
            expectedContentColor = { MaterialTheme.colorScheme.onPrimary },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.filledButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = DisabledContainerAlpha
            ) },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.filledTonalButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.surface },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.filledTonalButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = DisabledContainerAlpha
            ) },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.outlinedButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.outlinedButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_child_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.childButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_child_base_button_correct_colors() {
        rule.verifyButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.childButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = DisabledContainerAlpha
            ) },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) },
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.filledButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.filledButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.filledTonalButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.filledTonalButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.outlinedButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.outlinedButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_child_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Enabled,
            colors = { ButtonDefaults.childButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_child_three_slot_button_correct_colors() {
        rule.verifyThreeSlotButtonColors(
            status = Status.Disabled,
            colors = { ButtonDefaults.childButtonColors() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_button_correct_border_colors() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { MaterialTheme.colorScheme.outline },
            content = { modifier: Modifier ->
                OutlinedButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled()
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_button_correct_border_colors() {
        val status = Status.Disabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) },
            content = { modifier: Modifier ->
                OutlinedButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    border = ButtonDefaults.outlinedButtonBorder(
                        enabled = status.enabled(),
                    )
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                    border = ButtonDefaults.outlinedButtonBorder(
                        enabled = status.enabled(),
                        borderColor = Color.Green,
                        disabledBorderColor = Color.Red
                    )
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                    border = ButtonDefaults.outlinedButtonBorder(
                        enabled = status.enabled(),
                        borderColor = Color.Green,
                        disabledBorderColor = Color.Red
                    )
                ) {}
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyButtonColors(
    status: Status,
    colors: @Composable () -> ButtonColors,
    expectedContainerColor: @Composable () -> Color,
    expectedContentColor: @Composable () -> Color,
) {
    verifyColors(
        status = status,
        expectedContainerColor = expectedContainerColor,
        expectedContentColor = expectedContentColor,
        applyAlphaForDisabled = false,
        content = {
            var actualContentColor = Color.Transparent
            Button(
                onClick = {},
                enabled = status.enabled(),
                colors = colors(),
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                actualContentColor = LocalContentColor.current
            }
            return@verifyColors actualContentColor
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.verifyThreeSlotButtonColors(
    status: Status,
    colors: @Composable () -> ButtonColors
) {
    val testBackgroundColor = Color.White
    var containerColor = Color.Transparent
    var labelColor = Color.Transparent
    var secondaryLabelColor = Color.Transparent
    var iconColor = Color.Transparent
    var actualLabelColor = Color.Transparent
    var actualSecondaryLabelColor = Color.Transparent
    var actualIconColor = Color.Transparent

    setContentWithTheme {
        containerColor = ((colors().containerPainter(status.enabled()).value as ColorPainter).color)
            .compositeOver(testBackgroundColor)
        labelColor = colors().contentColor(status.enabled()).value
        secondaryLabelColor = colors().secondaryContentColor(status.enabled()).value
        iconColor = colors().iconColor(status.enabled()).value

        Box(
            Modifier
                .fillMaxSize()
                .background(testBackgroundColor)
        ) {
            Button(
                onClick = {},
                colors = colors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag(TEST_TAG),
                label = { actualLabelColor = LocalContentColor.current },
                secondaryLabel = { actualSecondaryLabelColor = LocalContentColor.current },
                icon = { actualIconColor = LocalContentColor.current }
            )
        }
    }

    assertEquals(actualLabelColor, labelColor)
    assertEquals(actualSecondaryLabelColor, secondaryLabelColor)
    assertEquals(actualIconColor, iconColor)

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(
            if (containerColor != Color.Transparent) containerColor else testBackgroundColor,
        )
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyButtonBorderColor(
    expectedBorderColor: @Composable () -> Color,
    content: @Composable (Modifier) -> Unit
) {
    val testBackground = Color.Black
    var finalExpectedBorderColor = Color.Transparent

    setContentWithTheme {
        finalExpectedBorderColor = expectedBorderColor().compositeOver(testBackground)
        Box(
            Modifier
                .fillMaxSize()
                .background(testBackground)
        ) {
            content(Modifier.testTag(TEST_TAG))
        }
    }

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(finalExpectedBorderColor)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.isShape(
    expectedShape: Shape,
    colors: @Composable () -> ButtonColors,
    content: @Composable (Modifier) -> Unit
) {
    var background = Color.Transparent
    var buttonColor = Color.Transparent
    val padding = 0.dp

    setContentWithTheme {
        background = MaterialTheme.colorScheme.surface
        Box(Modifier.background(background)) {
            buttonColor = (colors().containerPainter(true).value as ColorPainter).color
            if (buttonColor == Color.Transparent) {
                buttonColor = background
            }
            content(
                Modifier
                    .testTag(TEST_TAG)
                    .padding(padding)
            )
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
            shapeOverlapPixelCount = 2.0f,
            shape = expectedShape,
        )
}

const val DisabledContainerAlpha = 0.12f