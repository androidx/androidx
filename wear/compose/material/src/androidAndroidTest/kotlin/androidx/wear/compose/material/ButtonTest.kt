/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class ButtonBehaviourTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag_on_button_for_image() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").assertExists()
    }

    @Test
    fun supports_testtag_on_button_for_text() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").assertExists()
    }

    @Test
    fun supports_testtag_on_compactbutton_for_image() {
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").assertExists()
    }

    @Test
    fun supports_testtag_on_compactbutton_for_text() {
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").assertExists()
    }

    @Test
    fun has_clickaction_when_enabled_for_image() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_enabled_for_text() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled_for_image() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled_for_text() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled_on_compact_button() {
        var clicked = false

        rule.setContentWithTheme {
            CompactButton(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").performClick()

        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun responds_to_click_when_enabled_on_button() {
        var clicked = false

        rule.setContentWithTheme {
            Button(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item").performClick()

        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun does_not_respond_to_click_when_disabled_on_compact_button() {
        var clicked = false

        rule.setContentWithTheme {
            CompactButton(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").performClick()

        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun does_not_respond_to_click_when_disabled_on_button() {
        var clicked = false

        rule.setContentWithTheme {
            Button(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item").performClick()

        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun has_role_button_for_compact_image() {
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                modifier = Modifier.testTag("test-item")
            ) {
                CreateImage()
            }
        }

        rule.onNodeWithTag("test-item")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun has_role_button_for_text() {
        rule.setContentWithTheme {
            Button(
                onClick = {},
                modifier = Modifier.testTag("test-item")
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag("test-item")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun contains_text_for_button() {
        val text = "Test"
        rule.setContentWithTheme {
            Button(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithText(text).assertExists()
    }

    @Test
    fun contains_text_for_compact_button() {
        val text = "Test"
        rule.setContentWithTheme {
            CompactButton(
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
            Button(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNode(hasText(text)).assertExists()
    }

    @Test
    fun matches_has_text_for_compactbutton() {
        val text = "Test"
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNode(hasText(text)).assertExists()
    }

    @Test
    fun is_circular_under_ltr_for_button() =
        rule.isCircular(LayoutDirection.Ltr) {
            Button(
                modifier = Modifier.testTag("test-item"),
                onClick = {},
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }

    @Test
    @Ignore // This test failing under Treehugger, to be replaced with Screenshot test.
    fun is_circular_under_ltr_for_compact_button() =
        rule.isCircular(LayoutDirection.Ltr, padding = 8.dp) {
            CompactButton(
                modifier = Modifier.testTag("test-item"),
                onClick = {},
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }

    @Test
    fun is_circular_under_rtl_for_button() =
        rule.isCircular(LayoutDirection.Rtl) {
            Button(
                modifier = Modifier.testTag("test-item"),
                onClick = {},
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }

    @Test
    @Ignore // This test failing under Treehugger, to be replaced with Screenshot test.
    fun is_circular_under_rtl_for_compact_button() =
        rule.isCircular(LayoutDirection.Rtl, padding = 8.dp) {
            CompactButton(
                modifier = Modifier.testTag("test-item"),
                onClick = {},
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
}

class ButtonSizeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_compactbutton_correct_tapsize() {
        rule.verifyTapSize(TapSize.Small) {
            CompactButton(
                onClick = {},
            ) {
                Text("xs")
            }
        }
    }

    @Test
    fun gives_button_correct_tapsize() {
        rule.verifyTapSize(TapSize.Default) {
            Button(
                onClick = {},
            ) {
                Text("abc")
            }
        }
    }

    @Test
    fun gives_small_button_correct_tapsize() {
        rule.verifyTapSize(TapSize.Small) {
            Button(
                onClick = {},
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
            ) {
                CreateImage()
            }
        }
    }

    @Test
    fun gives_large_button_correct_tapsize() {
        rule.verifyTapSize(TapSize.Large) {
            Button(
                onClick = {},
                modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
            ) {
                CreateImage()
            }
        }
    }
}

class ButtonColorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_enabled_button_primary_colors() =
        verifyButtonColors(
            Status.Enabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    fun gives_enabled_compact_button_primary_colors() =
        verifyCompactButtonColors(
            Status.Enabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    fun gives_disabled_button_primary_colors() =
        verifyButtonColors(
            Status.Disabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    fun gives_disabled_compact_button_primary_colors() =
        verifyCompactButtonColors(
            Status.Disabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    fun gives_enabled_button_secondary_colors() =
        verifyButtonColors(
            Status.Enabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_enabled_compact_button_secondary_colors() =
        verifyCompactButtonColors(
            Status.Enabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_disabled_button_secondary_colors() =
        verifyButtonColors(
            Status.Disabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_disabled_compact_button_secondary_colors() =
        verifyCompactButtonColors(
            Status.Disabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_enabled_button_icon_colors() =
        verifyButtonColors(
            Status.Enabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_enabled_compact_button_icon_colors() =
        verifyCompactButtonColors(
            Status.Enabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_disabled_button_icon_colors() =
        verifyButtonColors(
            Status.Disabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun gives_disabled_compact_button_icon_colors() =
        verifyCompactButtonColors(
            Status.Disabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun allows_button_custom_enabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(backgroundColor = overrideColor),
                    enabled = true,
                    modifier = Modifier.testTag("test-item")
                ) {
                }
            }
        }

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(overrideColor, 50.0f)
    }

    @Test
    fun allows_compactbutton_custom_enabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(backgroundColor = overrideColor),
                    enabled = true,
                    modifier = Modifier.testTag("test-item")
                ) {
                }
            }
        }

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(overrideColor, 25.0f)
    }

    @Test
    fun allows_button_custom_disabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(disabledBackgroundColor = overrideColor),
                    enabled = false,
                    modifier = Modifier.testTag("test-item")
                ) {
                }
            }
        }

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(overrideColor, 50.0f)
    }

    @Test
    fun allows_compactbutton_custom_disabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(disabledBackgroundColor = overrideColor),
                    enabled = false,
                    modifier = Modifier.testTag("test-item")
                ) {
                }
            }
        }

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(overrideColor, 25.0f)
    }

    @Test
    fun allows_button_custom_enabled_content_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(contentColor = overrideColor),
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    fun allows_compactbutton_custom_enabled_content_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                colors = ButtonDefaults.buttonColors(contentColor = overrideColor),
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    fun allows_button_custom_disabled_content_color_override() {
        val overrideColor = Color.Yellow
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(disabledContentColor = overrideColor),
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    fun allows_compactbutton_custom_disabled_content_color_override() {
        val overrideColor = Color.Yellow
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(disabledContentColor = overrideColor),
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    fun verifyButtonColors(
        status: Status,
        buttonColors: @Composable () -> ButtonColors,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
    ) {
        verifyColors(
            status,
            backgroundColor,
            contentColor,
        ) {
            var actualColor = Color.Transparent
            Button(
                onClick = {},
                colors = buttonColors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag("test-item")
            ) {
                actualColor = LocalContentColor.current
            }
            return@verifyColors actualColor
        }
    }

    fun verifyCompactButtonColors(
        status: Status,
        buttonColors: @Composable () -> ButtonColors,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
    ) {
        verifyColors(
            status,
            backgroundColor,
            contentColor,
        ) {
            var actualColor = Color.Transparent
            CompactButton(
                onClick = {},
                backgroundPadding = 0.dp,
                colors = buttonColors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag("test-item")
            ) {
                actualColor = LocalContentColor.current
            }
            return@verifyColors actualColor
        }
    }

    private fun verifyColors(
        status: Status,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
        threshold: Float = 50.0f,
        content: @Composable () -> Color,
    ) {
        val testBackground = Color.White
        var expectedBackground = Color.Transparent
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent

        rule.setContentWithTheme {
            if (status.enabled()) {
                expectedBackground = backgroundColor()
                expectedContent = contentColor()
            } else {
                expectedBackground =
                    backgroundColor()
                        .copy(alpha = ContentAlpha.disabled).compositeOver(testBackground)
                expectedContent = contentColor().copy(alpha = ContentAlpha.disabled)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                actualContent = content()
            }
        }

        assertEquals(expectedContent, actualContent)

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                if (expectedBackground != Color.Transparent) expectedBackground else testBackground,
                threshold
            )
    }
}

class ButtonTextStyleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_button_correct_font() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            Button(
                onClick = {},
            ) {
                actualTextStyle = LocalTextStyle.current
            }
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_compactbutton_correct_font() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            CompactButton(
                onClick = {},
            ) {
                actualTextStyle = LocalTextStyle.current
            }
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }
}

private fun ComposeContentTestRule.verifyTapSize(
    expected: TapSize,
    content: @Composable () -> Unit
) {
    setContentWithThemeForSizeAssertions {
        content()
    }
        .assertHeightIsEqualTo(expected.size)
        .assertWidthIsEqualTo(expected.size)
}

private fun ComposeContentTestRule.isCircular(
    layoutDirection: LayoutDirection,
    padding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    var background = Color.Transparent
    var surface = Color.Transparent
    setContentWithTheme {
        background = MaterialTheme.colors.primary
        surface = MaterialTheme.colors.surface
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(
                Modifier
                    .padding(padding)
                    .background(surface)
            ) {
                content()
            }
        }
    }

    onNodeWithTag("test-item")
        .captureToImage()
        .assertShape(
            density = density,
            shape = CircleShape,
            horizontalPadding = padding,
            verticalPadding = padding,
            backgroundColor = surface,
            shapeColor = background
        )
}

internal enum class TapSize(val size: Dp) {
    Small(48.dp),
    Default(52.dp),
    Large(60.dp)
}

enum class Status {
    Enabled,
    Disabled;

    fun enabled() = this == Enabled
}
