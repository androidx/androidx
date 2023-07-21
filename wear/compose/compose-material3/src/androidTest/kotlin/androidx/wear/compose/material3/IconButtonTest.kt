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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.IconButtonDefaults.DefaultButtonSize
import androidx.wear.compose.material3.IconButtonDefaults.ExtraSmallButtonSize
import androidx.wear.compose.material3.IconButtonDefaults.LargeButtonSize
import androidx.wear.compose.material3.IconButtonDefaults.SmallButtonSize
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class IconButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            IconButton(
                modifier = Modifier.testTag(TEST_TAG),
                onClick = {}
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_click_action_when_enabled() {
        rule.setContentWithTheme {
            IconButton(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_click_action_when_disabled() {
        rule.setContentWithTheme {
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            IconButton(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            IconButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            IconButton(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
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
            IconButton(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
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
            IconButton(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
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
    fun gives_default_button_correct_tap_size() {
        rule.verifyTapSize(DefaultButtonSize) { modifier ->
            IconButton(
                onClick = {},
                modifier = modifier.touchTargetAwareSize(DefaultButtonSize)
            ) {
                TestImage()
            }
        }
    }

    @Test
    fun gives_large_button_correct_tap_size() {
        rule.verifyTapSize(LargeButtonSize) { modifier ->
            IconButton(
                onClick = {},
                modifier = modifier.touchTargetAwareSize(LargeButtonSize)
            ) {
                TestImage()
            }
        }
    }

    @Test
    fun gives_small_button_correct_tap_size() {
        rule.verifyTapSize(SmallButtonSize) { modifier ->
            IconButton(
                onClick = {},
                modifier = modifier.touchTargetAwareSize(SmallButtonSize)
            ) {
                TestImage()
            }
        }
    }

    @Test
    fun gives_extra_small_button_correct_tap_size() {
        rule.verifyTapSize(
            expectedSize = MinimumButtonTapSize
        ) { modifier ->
            IconButton(
                onClick = {},
                modifier = modifier.touchTargetAwareSize(ExtraSmallButtonSize)
            ) {
                TestImage()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun default_shape_is_circular() {
        rule.isShape(
            expectedShape = CircleShape,
            colors = { IconButtonDefaults.iconButtonColors() }
        ) { modifier ->
            IconButton(
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
            colors = { IconButtonDefaults.iconButtonColors() }
        ) { modifier ->
            IconButton(
                onClick = {},
                modifier = modifier,
                shape = shape
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Enabled,
            colors = { IconButtonDefaults.iconButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onBackground }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Disabled,
            colors = { IconButtonDefaults.iconButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Enabled,
            colors = { IconButtonDefaults.filledIconButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.primary },
            expectedContentColor = { MaterialTheme.colorScheme.onPrimary }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Disabled,
            colors = { IconButtonDefaults.filledIconButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = DisabledBorderAndContainerAlpha
            ) },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_filled_tonal_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Enabled,
            colors = { IconButtonDefaults.filledTonalIconButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.surface },
            expectedContentColor = { MaterialTheme.colorScheme.onSurfaceVariant }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_filled_tonal_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Disabled,
            colors = { IconButtonDefaults.filledTonalIconButtonColors() },
            expectedContainerColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = DisabledBorderAndContainerAlpha
            ) },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Enabled,
            colors = { IconButtonDefaults.outlinedIconButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.primary }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_icon_button_colors() {
        rule.verifyIconButtonColors(
            status = Status.Disabled,
            colors = { IconButtonDefaults.outlinedIconButtonColors() },
            expectedContainerColor = { Color.Transparent },
            expectedContentColor = { MaterialTheme.colorScheme.onSurface.copy(
                alpha = ContentAlpha.disabled
            ) }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_outlined_icon_button_correct_border_colors() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { MaterialTheme.colorScheme.outline },
            content = { modifier: Modifier ->
                OutlinedIconButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled()
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_outlined_icon_button_correct_border_colors() {
        val status = Status.Disabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = {
                MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledBorderAndContainerAlpha)
            },
            content = { modifier: Modifier ->
                OutlinedIconButton(
                    onClick = {},
                    modifier = modifier,
                    enabled = status.enabled()
                ) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun overrides_outlined_icon_button_border_color() {
        val status = Status.Enabled
        rule.verifyButtonBorderColor(
            expectedBorderColor = { Color.Green },
            content = { modifier: Modifier ->
                OutlinedIconButton(
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
    private fun ComposeContentTestRule.verifyIconButtonColors(
        status: Status,
        colors: @Composable () -> IconButtonColors,
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
                IconButton(
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
    colors: @Composable () -> IconButtonColors,
    content: @Composable (Modifier) -> Unit
) {
    var background = Color.Transparent
    var buttonColor = Color.Transparent
    val padding = 0.dp

    setContentWithTheme {
        background = MaterialTheme.colorScheme.surface
        Box(Modifier.background(background)) {
            buttonColor = colors().containerColor(true).value
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
