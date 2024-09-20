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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class IconToggleButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testTag() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickAction_when_enabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_clickAction_when_disabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = false,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsToggleable()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = false,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun is_on_when_checked() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun is_off_when_unchecked() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOn().performClick().assertIsOff()
    }

    @Test
    fun does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            IconToggleButton(
                enabled = false,
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick().assertIsOff()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun is_circular_under_ltr() =
        rule.isShape(
            shape = CircleShape,
            layoutDirection = LayoutDirection.Ltr,
            shapeColorComposable = { shapeColor() }
        ) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun is_circular_under_rtl() =
        rule.isShape(
            shape = CircleShape,
            layoutDirection = LayoutDirection.Rtl,
            shapeColorComposable = { shapeColor() }
        ) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_shape_overrides() =
        rule.isShape(
            shape = RectangleShape,
            layoutDirection = LayoutDirection.Ltr,
            shapeColorComposable = { shapeColor() }
        ) {
            IconToggleButton(
                enabled = true,
                checked = true,
                shape = RectangleShape,
                onCheckedChange = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

    @Test
    fun gives_default_correct_tapSize() =
        rule.verifyTapSize(52.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.DefaultButtonSize)
            )
        }

    @Test
    fun gives_small_correct_tapSize() =
        rule.verifyTapSize(48.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.SmallButtonSize)
            )
        }

    @Test
    fun gives_large_correct_tapSize() =
        rule.verifyTapSize(60.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.LargeButtonSize)
            )
        }

    @Test
    fun gives_extraLarge_correct_tapSize() =
        rule.verifyTapSize(72.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.ExtraLargeButtonSize)
            )
        }

    @Test
    fun gives_default_correct_size() =
        rule.verifyActualSize(52.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.DefaultButtonSize)
            )
        }

    @Test
    fun gives_small_correct_size() =
        rule.verifyActualSize(48.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.SmallButtonSize)
            )
        }

    @Test
    fun gives_extraLarge_correct_size() =
        rule.verifyActualSize(72.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.ExtraLargeButtonSize)
            )
        }

    @Test
    fun gives_large_correct_size() =
        rule.verifyActualSize(60.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.LargeButtonSize)
            )
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_checked_primary_colors() =
        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = true,
            colors = { IconToggleButtonDefaults.iconToggleButtonColors() },
            containerColor = { MaterialTheme.colorScheme.primary },
            contentColor = { MaterialTheme.colorScheme.onPrimary }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_unchecked_surface_colors() =
        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = false,
            colors = { IconToggleButtonDefaults.iconToggleButtonColors() },
            containerColor = { MaterialTheme.colorScheme.surfaceContainer },
            contentColor = { MaterialTheme.colorScheme.onSurfaceVariant }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_unchecked_surface_colors_with_alpha() =
        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = false,
            colors = { IconToggleButtonDefaults.iconToggleButtonColors() },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() }
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_primary_checked_contrasting_content_color() =
        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = true,
            colors = { IconToggleButtonDefaults.iconToggleButtonColors() },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() },
        )

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_background_override() {
        val overrideColor = Color.Yellow

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = true,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    checkedContainerColor = overrideColor
                )
            },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onPrimary }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = true,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(checkedContentColor = overrideColor)
            },
            containerColor = { MaterialTheme.colorScheme.primary },
            contentColor = { overrideColor }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_background_override() {
        val overrideColor = Color.Red

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = false,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    uncheckedContainerColor = overrideColor
                )
            },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onSurfaceVariant }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = false,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    uncheckedContentColor = overrideColor
                )
            },
            containerColor = { MaterialTheme.colorScheme.surfaceContainer },
            contentColor = { overrideColor }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_disabled_background_override() {
        val overrideColor = Color.Yellow

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = true,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    // Apply the content color override for the content alpha to be applied
                    disabledCheckedContainerColor = overrideColor
                )
            },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_disabled_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = true,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    // Apply the content color override for the content alpha to be applied
                    disabledCheckedContentColor = overrideColor
                )
            },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
            contentColor = { overrideColor }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_disabled_background_override() {
        val overrideColor = Color.Red

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = false,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    // Apply the content color override for the content alpha to be applied
                    disabledUncheckedContainerColor = overrideColor
                )
            },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_disabled_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = false,
            colors = {
                IconToggleButtonDefaults.iconToggleButtonColors(
                    // Apply the content color override for the content alpha to be applied
                    disabledUncheckedContentColor = overrideColor
                )
            },
            contentColor = { overrideColor },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            }
        )
    }

    @Test
    fun default_role_checkbox() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                enabled = false,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun allows_custom_role() {
        val overrideRole = Role.Button

        rule.setContentWithTheme {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                enabled = false,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG).semantics { role = overrideRole }
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, overrideRole))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyIconToggleButtonColors(
        status: Status,
        checked: Boolean,
        colors: @Composable () -> IconToggleButtonColors,
        containerColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
    ) {
        verifyColors(
            expectedContainerColor = containerColor,
            expectedContentColor = contentColor,
            content = {
                var actualContentColor = Color.Transparent
                IconToggleButton(
                    onCheckedChange = {},
                    enabled = status.enabled(),
                    checked = checked,
                    colors = colors(),
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    actualContentColor = LocalContentColor.current
                }
                return@verifyColors actualContentColor
            }
        )
    }

    @Composable
    private fun shapeColor(): Color {
        return IconToggleButtonDefaults.iconToggleButtonColors()
            .containerColor(enabled = true, checked = true)
            .value
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.isShape(
        shape: Shape = CircleShape,
        layoutDirection: LayoutDirection,
        padding: Dp = 0.dp,
        backgroundColor: Color = Color.Red,
        shapeColorComposable: @Composable () -> Color,
        content: @Composable () -> Unit
    ) {
        var shapeColor = Color.Transparent
        setContentWithTheme {
            shapeColor = shapeColorComposable.invoke()
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(Modifier.padding(padding).background(backgroundColor)) { content() }
            }
        }

        this.waitForIdle()
        onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertShape(
                density = density,
                shape = shape,
                horizontalPadding = padding,
                verticalPadding = padding,
                backgroundColor = backgroundColor,
                shapeOverlapPixelCount = 2.0f,
                shapeColor = shapeColor
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyColors(
        expectedContainerColor: @Composable () -> Color,
        expectedContentColor: @Composable () -> Color,
        content: @Composable () -> Color
    ) {
        val testBackgroundColor = Color.White
        var finalExpectedContainerColor = Color.Transparent
        var finalExpectedContent = Color.Transparent
        var actualContentColor = Color.Transparent
        setContentWithTheme {
            finalExpectedContainerColor =
                expectedContainerColor().compositeOver(testBackgroundColor)
            finalExpectedContent = expectedContentColor()
            Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
                actualContentColor = content()
            }
        }
        Assert.assertEquals(finalExpectedContent, actualContentColor)
        onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(finalExpectedContainerColor)
    }
}
