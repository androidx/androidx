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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.TextButtonDefaults.DefaultButtonSize
import androidx.wear.compose.material3.TextButtonDefaults.LargeButtonSize
import androidx.wear.compose.material3.TextButtonDefaults.SmallButtonSize
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TextButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            TextButton(modifier = Modifier.testTag(TEST_TAG), onClick = {}) { Text("Test") }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun contains_text() {
        val text = "Test"
        rule.setContentWithTheme {
            TextButton(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithText(text).assertExists()
    }

    @Test
    fun matches_has_text_for_button() {
        val text = "Test"
        rule.setContentWithTheme {
            TextButton(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNode(hasText(text)).assertExists()
    }

    @Test
    fun has_click_action_when_enabled() {
        rule.setContentWithTheme {
            TextButton(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_click_action_when_disabled() {
        rule.setContentWithTheme {
            TextButton(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            TextButton(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            TextButton(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            TextButton(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
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

        rule.setContentWithTheme {
            TextButton(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(true, longClicked) }
    }

    @Test
    fun onLongClickLabel_includedInSemantics() {
        val testLabel = "Long click action"

        rule.setContentWithTheme {
            TextButton(
                modifier = Modifier.testTag(TEST_TAG),
                onClick = {},
                onLongClick = {},
                onLongClickLabel = testLabel
            ) {
                Text("Button")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertOnLongClickLabelMatches(testLabel)
    }

    @Test
    fun does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            TextButton(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(false, clicked) }
    }

    @Test
    fun does_not_respond_to_long_click_when_disabled() {
        var longClicked = false

        rule.setContentWithTheme {
            TextButton(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
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
            TextButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun allows_custom_role() {
        val overrideRole = Role.Checkbox

        rule.setContentWithTheme {
            TextButton(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG).semantics { role = overrideRole }
            ) {
                Text("Test")
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, overrideRole))
    }

    @Test
    fun sets_correct_font() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.labelMedium
            TextButton(
                onClick = {},
            ) {
                actualTextStyle = LocalTextStyle.current
            }
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_default_button_correct_tap_size() {
        rule.verifyTapSize(DefaultButtonSize) { modifier ->
            TextButton(onClick = {}, modifier = modifier.touchTargetAwareSize(DefaultButtonSize)) {
                Text("ABC")
            }
        }
    }

    @Test
    fun gives_large_button_correct_tap_size() {
        rule.verifyTapSize(LargeButtonSize) { modifier ->
            TextButton(onClick = {}, modifier = modifier.touchTargetAwareSize(LargeButtonSize)) {
                Text("Large")
            }
        }
    }

    @Test
    fun gives_small_button_correct_tap_size() {
        rule.verifyTapSize(SmallButtonSize) { modifier ->
            TextButton(onClick = {}, modifier = modifier.touchTargetAwareSize(SmallButtonSize)) {
                Text("abc")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun default_shape_is_circular() {
        rule.isShape(
            expectedShape = CircleShape,
            colors = { TextButtonDefaults.textButtonColors() }
        ) { modifier ->
            TextButton(onClick = {}, modifier = modifier) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(expectedShape = shape, colors = { TextButtonDefaults.textButtonColors() }) {
            modifier ->
            TextButton(onClick = {}, modifier = modifier, shape = shape) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Enabled,
            colors = { TextButtonDefaults.textButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Disabled,
            colors = { TextButtonDefaults.textButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Enabled,
            colors = { TextButtonDefaults.filledTextButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.primary },
            expectedContentColor = { MaterialTheme.colorScheme.onPrimary }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Disabled,
            colors = { TextButtonDefaults.filledTextButtonColors() },
            expectedContainerColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
            },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_variant_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Enabled,
            colors = { TextButtonDefaults.filledVariantTextButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.primaryContainer },
            expectedContentColor = { MaterialTheme.colorScheme.onPrimaryContainer }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_variant_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Disabled,
            colors = { TextButtonDefaults.filledVariantTextButtonColors() },
            expectedContainerColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
            },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Enabled,
            colors = { TextButtonDefaults.filledTonalTextButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.surfaceContainer },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Disabled,
            colors = { TextButtonDefaults.filledTonalTextButtonColors() },
            expectedContainerColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
            },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Enabled,
            colors = { TextButtonDefaults.outlinedTextButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_text_button_colors() {
        rule.verifyTextButtonColors(
            status = Status.Disabled,
            colors = { TextButtonDefaults.outlinedTextButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_text_button_correct_border_colors() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { MaterialTheme.colorScheme.outline },
            content = { modifier: Modifier ->
                TextButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    colors = TextButtonDefaults.outlinedTextButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = status.enabled())
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_text_button_correct_border_colors() {
        val status = Status.Disabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledBorderAlpha)
            },
            content = { modifier: Modifier ->
                TextButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    colors = TextButtonDefaults.outlinedTextButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = status.enabled())
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun overrides_outlined_text_button_border_color() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { Color.Green },
            content = { modifier: Modifier ->
                TextButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled(),
                    border =
                        ButtonDefaults.outlinedButtonBorder(
                            enabled = status.enabled(),
                            borderColor = Color.Green,
                            disabledBorderColor = Color.Red
                        )
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyTextButtonColors(
        status: Status,
        colors: @Composable () -> TextButtonColors,
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
                TextButton(
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
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComposeContentTestRule.isShape(
    expectedShape: Shape,
    colors: @Composable () -> TextButtonColors,
    content: @Composable (Modifier) -> Unit
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
            shapeOverlapPixelCount = 2.0f,
            shape = expectedShape,
        )
}
