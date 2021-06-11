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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToggleButtonBehaviourTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { CreateImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                enabled = true,
                content = { CreateImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                enabled = false,
                content = { CreateImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { CreateImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNode(isToggleable()).assertExists()
    }

    @Test
    fun is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { CreateImage() },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { CreateImage() },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun is_on_when_checked() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { CreateImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun is_off_when_unchecked() {
        rule.setContentWithTheme {
            ToggleButton(
                checked = false,
                onCheckedChange = {},
                content = { CreateImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            ToggleButton(
                content = { CreateImage() },
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            ToggleButton(
                content = { CreateImage() },
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            ToggleButton(
                content = { CreateImage() },
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun has_role_checkbox() {
        rule.setContentWithTheme {
            ToggleButton(
                content = { CreateImage() },
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox
                )
            )
    }

    @Test
    fun is_circular_under_ltr() =
        rule.isCircular(LayoutDirection.Ltr) {
            ToggleButton(
                content = {},
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

    @Test
    fun is_circular_under_rtl() =
        rule.isCircular(LayoutDirection.Rtl) {
            ToggleButton(
                content = {},
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

    @Test
    fun displays_text_content() {
        val textContent = "abc"

        rule.setContentWithTheme {
            ToggleButton(
                content = { Text(textContent) },
                checked = true,
                onCheckedChange = {},
            )
        }

        rule.onNodeWithText(textContent).assertExists()
    }
}

class ToggleButtonSizeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_default_correct_tapsize() {
        rule.verifyTapSize(TapSize.Default) {
            ToggleButton(
                content = { Text("abc") },
                checked = true,
                onCheckedChange = {},
            )
        }
    }

    @Test
    fun gives_small_correct_tapsize() {
        rule.verifyTapSize(TapSize.Small) {
            ToggleButton(
                content = { CreateImage() },
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.size(ToggleButtonDefaults.SmallToggleButtonSize)
            )
        }
    }
}

class ToggleButtonColorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_checked_primary_colors() =
        verifyColors(
            Status.Enabled,
            checked = true,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary }
        )

    @Test
    fun gives_unchecked_secondary_colors() =
        verifyColors(
            Status.Enabled,
            checked = false,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun gives_checked_disabled_alpha() =
        verifyColors(
            Status.Disabled,
            checked = true,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary }
        )

    @Test
    fun gives_unchecked_disabled_alpha() =
        verifyColors(
            Status.Disabled,
            checked = false,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun allows_custom_checked_background_override() {
        val override = Color.Yellow

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedBackgroundColor =
                            override
                    ),
                    content = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(override, 50.0f)
    }

    @Test
    fun allows_custom_checked_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        checkedContentColor =
                            override
                    ),
                    content = { actualContentColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(override, actualContentColor)
    }

    @Test
    fun allows_custom_unchecked_background_override() {
        val override = Color.Red

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        uncheckedBackgroundColor =
                            override
                    ),
                    content = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(override, 50.0f)
    }

    @Test
    fun allows_custom_unchecked_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    enabled = true,
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        uncheckedContentColor =
                            override
                    ),
                    content = { actualContentColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(override, actualContentColor)
    }

    @Test
    fun allows_custom_checked_disabled_background_override() {
        val override = Color.Yellow

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                    colors = ToggleButtonDefaults.toggleButtonColors
                    (disabledCheckedBackgroundColor = override),
                    content = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(override, 50.0f)
    }

    @Test
    fun allows_custom_checked_disabled_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        disabledCheckedContentColor =
                            override
                    ),
                    content = {
                        actualContentColor = LocalContentColor.current
                    },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(override, actualContentColor)
    }

    @Test
    fun allows_custom_unchecked_disabled_background_override() {
        val override = Color.Red

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    colors = ToggleButtonDefaults.toggleButtonColors
                    (disabledUncheckedBackgroundColor = override),
                    content = {},
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(override, 50.0f)
    }

    @Test
    fun allows_custom_unchecked_disabled_content_override() {
        val override = Color.Green
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    colors = ToggleButtonDefaults.toggleButtonColors
                    (disabledUncheckedContentColor = override),
                    content = { actualContentColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(override, actualContentColor)
    }

    private fun verifyColors(
        status: Status,
        checked: Boolean,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color
    ) {
        var expectedBackground = Color.Transparent
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent
        var actualDisabledAlpha = 0f

        rule.setContentWithTheme {
            expectedBackground = backgroundColor()
            expectedContent = contentColor()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(expectedBackground)
            ) {
                ToggleButton(
                    checked = checked,
                    onCheckedChange = {},
                    enabled = status.enabled(),
                    content = {
                        actualContent = LocalContentColor.current
                        actualDisabledAlpha = ContentAlpha.disabled
                    },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        if (status.enabled()) {
            assertEquals(expectedContent, actualContent)
        } else {
            assertEquals(expectedContent.copy(alpha = actualDisabledAlpha), actualContent)
        }
        if (expectedBackground != Color.Transparent) {
            rule.onNodeWithTag(TEST_TAG)
                .captureToImage()
                .assertContainsColor(expectedBackground, 50.0f)
        }
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

    onNodeWithTag(TEST_TAG)
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
