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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChipBehaviourTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            Chip(
                onClick = { clicked = true },
                colors = ChipDefaults.primaryChipColors(),
                enabled = true,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0).performClick()

        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            Chip(
                onClick = { clicked = true },
                colors = ChipDefaults.primaryChipColors(),
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0).performClick()

        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun has_role_button() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                enabled = false,
                modifier = Modifier.testTag("test-item")
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag("test-item").onChildAt(0)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun is_stadium_shape_under_ltr() =
        rule.isStadiumShape(LayoutDirection.Ltr) {
            Chip(
                modifier = Modifier.testTag("test-item"),
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
            ) { /* omit content to allow us to validate the shape by pixel checking */ }
        }

    @Test
    fun is_stadium_shape_under_rtl() =
        rule.isStadiumShape(LayoutDirection.Rtl) {
            Chip(
                modifier = Modifier.testTag("test-item"),
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
            ) { /* omit content to allow us to validate the shape by pixel checking */ }
        }
}

class ChipSizeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_base_chip_correct_height() =
        verifyHeight(ChipDefaults.Height)

    @Test
    fun has_icon_in_correct_location_when_only_single_line_of_text() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                Chip(
                    onClick = {},
                    label = { Text("Blue green orange") },
                    icon = { TestImage(iconTag) },
                    modifier = Modifier.testTag(chipTag)
                )
            }
        val itemBounds = rule.onNodeWithTag(chipTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(iconTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun icon_only_compact_chip_has_correct_default_width_and_height() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                CompactChip(
                    onClick = {},
                    modifier = Modifier.testTag(chipTag),
                    icon = { TestImage(iconTag) }
                )
            }

        rule.onRoot().assertWidthIsEqualTo(52.dp).assertHeightIsEqualTo(32.dp)
    }

    @Test
    fun label_only_compact_chip_has_correct_default_height() {
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                CompactChip(
                    onClick = {},
                    modifier = Modifier.testTag(chipTag),
                    label = { Text("Test") }
                )
            }

        rule.onRoot().assertHeightIsEqualTo(32.dp)
    }

    @Test
    fun no_content_compact_chip_has_correct_default_width_and_height() {
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                CompactChip(
                    onClick = {},
                    modifier = Modifier.testTag(chipTag),
                )
            }

        rule.onRoot().assertWidthIsEqualTo(52.dp).assertHeightIsEqualTo(32.dp)
    }

    @Test
    fun icon_only_compact_chip_can_have_width_overridden() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                CompactChip(
                    onClick = {},
                    modifier = Modifier
                        .testTag(chipTag)
                        .width(100.dp),
                    icon = { TestImage(iconTag) }
                )
            }

        rule.onRoot().assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun has_icon_in_correct_location_when_compact_chip() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                CompactChip(
                    onClick = {},
                    label = { Text("Blue green orange") },
                    icon = { TestImage(iconTag) },
                    modifier = Modifier.testTag(chipTag)
                )
            }
        val itemBounds = rule.onNodeWithTag(chipTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(iconTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun has_icon_in_correct_location_when_icon_only_chip() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                CompactChip(
                    onClick = {},
                    modifier = Modifier.testTag(chipTag),
                    icon = { TestImage(iconTag) }
                )
            }
        val itemBounds = rule.onNodeWithTag(chipTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(iconTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    private fun verifyHeight(expectedHeight: Dp) {
        rule.verifyHeight(expectedHeight) {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
            ) {
                TestImage()
            }
        }
    }
}

class ChipColorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_primary_enabled_colors() =
        verifyColors(
            TestChipColors.Primary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    fun gives_primary_gradient_enabled_colors() =
        verifyGradientBackgroundColors(
            TestChipColors.PrimaryGradient,
            ChipStatus.Enabled,
            { MaterialTheme.colors.onSurface },
        )

    @Test
    fun three_slot_layout_gives_primary_enabled_colors() =
        verifySlotColors(
            TestChipColors.Primary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
            { MaterialTheme.colors.onPrimary },
            { MaterialTheme.colors.onPrimary }
        )

    @Test
    fun compact_chip_gives_primary_enabled_colors() =
        verifySlotColors(
            TestChipColors.Primary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
            { MaterialTheme.colors.onPrimary },
            { MaterialTheme.colors.onPrimary },
            compactChip = true
        )

    @Test
    fun gives_primary_disabled_colors() =
        verifyColors(
            TestChipColors.Primary,
            ChipStatus.Disabled,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    fun three_slot_layout_gives_primary_disabled_colors() =
        verifySlotColors(
            TestChipColors.Primary,
            ChipStatus.Disabled,
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
            { MaterialTheme.colors.onPrimary },
            { MaterialTheme.colors.onPrimary }
        )

    @Test
    fun gives_secondary_enabled_colors() =
        verifyColors(
            TestChipColors.Secondary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun gives_child_enabled_colors() =
        verifyColors(
            TestChipColors.Child,
            ChipStatus.Enabled,
            { Color.Transparent },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun three_slot_layout_gives_secondary_enabled_colors() =
        verifySlotColors(
            TestChipColors.Secondary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun gives_secondary_disabled_colors() =
        verifyColors(
            TestChipColors.Secondary,
            ChipStatus.Disabled,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun gives_child_disabled_colors() =
        verifyColors(
            TestChipColors.Child,
            ChipStatus.Disabled,
            { Color.Transparent },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun three_slot_layout_gives_secondary_disabled_colors() =
        verifySlotColors(
            TestChipColors.Secondary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface }
        )

    @Test
    fun compact_chip_gives_secondary_disabled_colors() =
        verifySlotColors(
            TestChipColors.Secondary,
            ChipStatus.Enabled,
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            { MaterialTheme.colors.onSurface },
            compactChip = true
        )

    @Test
    fun allows_custom_enabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Chip(
                    onClick = {},
                    content = {},
                    colors = ChipDefaults.primaryChipColors(backgroundColor = overrideColor),
                    enabled = true,
                    modifier = Modifier.testTag("test-item")
                )
            }
        }

        rule.onNodeWithTag("test-item")
            .onChild() // skip the 'outer' surface
            .captureToImage()
            .assertContainsColor(overrideColor, 50.0f)
    }

    @Test
    fun allows_custom_disabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Chip(
                    onClick = {},
                    content = {},
                    colors = ChipDefaults.chipColors(disabledBackgroundColor = overrideColor),
                    enabled = false,
                    modifier = Modifier.testTag("test-item")
                )
            }
        }

        rule.onNodeWithTag("test-item")
            .onChild() // skip the 'outer' surface
            .captureToImage()
            .assertContainsColor(overrideColor, 50.0f)
    }

    @Test
    fun allows_custom_enabled_content_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(
                    contentColor = overrideColor
                ),
                content = {
                    actualContentColor = LocalContentColor.current
                },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            )
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    fun allows_custom_enabled_secondary_label_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        var actualSecondaryContentColor = Color.Transparent
        var expectedContent = Color.Transparent
        rule.setContentWithTheme {
            expectedContent = MaterialTheme.colors.onPrimary
            Chip(
                onClick = {},
                colors = ChipDefaults.chipColors(
                    secondaryContentColor = overrideColor
                ),
                label = {
                    actualContentColor = LocalContentColor.current
                },
                secondaryLabel = {
                    actualSecondaryContentColor = LocalContentColor.current
                },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            )
        }
        assertEquals(expectedContent, actualContentColor)
        assertEquals(overrideColor, actualSecondaryContentColor)
    }

    @Test
    fun allows_custom_enabled_icon_tint_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        var actualIconTintColor = Color.Transparent
        var expectedContent = Color.Transparent
        rule.setContentWithTheme {
            expectedContent = MaterialTheme.colors.onPrimary
            Chip(
                onClick = {},
                colors = ChipDefaults.chipColors(
                    iconTintColor = overrideColor
                ),
                label = {
                    actualContentColor = LocalContentColor.current
                },
                icon = {
                    actualIconTintColor = LocalContentColor.current
                },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            )
        }
        assertEquals(expectedContent, actualContentColor)
        assertEquals(overrideColor, actualIconTintColor)
    }

    @Test
    fun allows_custom_disabled_content_color_override() {
        val overrideColor = Color.Yellow
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.chipColors(
                    disabledContentColor = overrideColor
                ),
                content = {
                    actualContentColor = LocalContentColor.current
                },
                enabled = false,
                modifier = Modifier.testTag("test-item")
            )
        }

        assertEquals(overrideColor, actualContentColor)
    }

    private fun verifyGradientBackgroundColors(
        testChipColors: TestChipColors,
        status: ChipStatus,
        contentColor: @Composable () -> Color
    ) {
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            if (status.enabled()) {
                expectedContent = contentColor()
            } else {
                expectedContent = contentColor().copy(alpha = ContentAlpha.disabled)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                Chip(
                    onClick = {},
                    colors = testChipColors.chipColors(),
                    content = { actualContent = LocalContentColor.current },
                    enabled = status.enabled(),
                    modifier = Modifier.testTag("test-item")
                )
            }
        }

        assertEquals(expectedContent, actualContent)

        // Background checks are clearly missing here. There is no good way to check that
        // a gradient background matches with this approach.
    }

    private fun verifyColors(
        testChipColors: TestChipColors,
        status: ChipStatus,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color
    ) {
        var expectedBackground = Color.Transparent
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            if (status.enabled()) {
                expectedBackground = backgroundColor()
                expectedContent = contentColor()
            } else {
                expectedBackground =
                    backgroundColor().copy(alpha = ContentAlpha.disabled)
                        .compositeOver(testBackground)
                expectedContent = contentColor().copy(alpha = ContentAlpha.disabled)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                Chip(
                    onClick = {},
                    colors = testChipColors.chipColors(),
                    content = { actualContent = LocalContentColor.current },
                    enabled = status.enabled(),
                    modifier = Modifier.testTag("test-item")
                )
            }
        }

        assertEquals(expectedContent, actualContent)

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                if (expectedBackground != Color.Transparent) expectedBackground else testBackground,
                50.0f
            )
    }

    private fun verifySlotColors(
        testChipColors: TestChipColors,
        status: ChipStatus,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
        secondaryContentColor: @Composable () -> Color,
        iconColor: @Composable () -> Color,
        compactChip: Boolean = false
    ) {
        var expectedBackground = Color.Transparent
        var expectedContent = Color.Transparent
        var expectedSecondaryContent = Color.Transparent
        var expectedIcon = Color.Transparent
        var actualContent = Color.Transparent
        var actualSecondaryContent = Color.Transparent
        var actualIcon = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            if (status.enabled()) {
                expectedBackground = backgroundColor()
                expectedContent = contentColor()
                expectedSecondaryContent = secondaryContentColor()
                expectedIcon = iconColor()
            } else {
                expectedBackground =
                    backgroundColor().copy(alpha = ContentAlpha.disabled)
                        .compositeOver(testBackground)
                expectedContent = contentColor().copy(alpha = ContentAlpha.disabled)
                expectedSecondaryContent = secondaryContentColor()
                    .copy(alpha = ContentAlpha.disabled)
                expectedIcon = iconColor().copy(alpha = ContentAlpha.disabled)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                if (compactChip) {
                    CompactChip(
                        onClick = {},
                        colors = testChipColors.chipColors(),
                        label = { actualContent = LocalContentColor.current },
                        icon = { actualIcon = LocalContentColor.current },
                        enabled = status.enabled(),
                        modifier = Modifier.testTag("test-item")
                    )
                } else {
                    Chip(
                        onClick = {},
                        colors = testChipColors.chipColors(),
                        label = { actualContent = LocalContentColor.current },
                        secondaryLabel = { actualSecondaryContent = LocalContentColor.current },
                        icon = { actualIcon = LocalContentColor.current },
                        enabled = status.enabled(),
                        modifier = Modifier.testTag("test-item")
                    )
                }
            }
        }

        assertEquals(expectedContent, actualContent)
        if (! compactChip) {
            assertEquals(expectedSecondaryContent, actualSecondaryContent)
        }
        assertEquals(expectedIcon, actualIcon)

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                if (expectedBackground != Color.Transparent) expectedBackground else testBackground,
                50.0f
            )
    }
}

class ChipFontTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_correct_text_style_base() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default
        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                content = {
                    actualTextStyle = LocalTextStyle.current
                },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            )
        }
        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_correct_text_style_three_slot_chip() {
        var actualLabelTextStyle = TextStyle.Default
        var actualSecondaryLabelTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default
        var expectedSecondaryTextStyle = TextStyle.Default
        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            expectedSecondaryTextStyle = MaterialTheme.typography.caption2
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                label = {
                    actualLabelTextStyle = LocalTextStyle.current
                },
                secondaryLabel = {
                    actualSecondaryLabelTextStyle = LocalTextStyle.current
                },
                enabled = true,
                modifier = Modifier.testTag("test-item")
            )
        }
        assertEquals(expectedTextStyle, actualLabelTextStyle)
        assertEquals(expectedSecondaryTextStyle, actualSecondaryLabelTextStyle)
    }
}

private fun ComposeContentTestRule.verifyHeight(expected: Dp, content: @Composable () -> Unit) {
    setContentWithThemeForSizeAssertions {
        content()
    }
        .assertHeightIsEqualTo(expected)
}

// Determine whether the chip has stadium shape.
// https://en.wikipedia.org/wiki/Stadium_(geometry)#:~:text=A%20stadium%20is%20a%20two,%2C%20obround%2C%20or%20sausage%20body.
private fun ComposeContentTestRule.isStadiumShape(
    layoutDirection: LayoutDirection,
    content: @Composable () -> Unit
) {
    val padding = 0.dp
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
            shape = RoundedCornerShape(corner = CornerSize(50)),
            horizontalPadding = padding,
            verticalPadding = padding,
            backgroundColor = surface,
            shapeColor = background
        )
}

internal enum class ChipStatus {
    Enabled,
    Disabled;

    fun enabled() = this == Enabled
}

private enum class TestChipColors {
    Primary {
        @Composable override fun chipColors(): ChipColors {
            return ChipDefaults.primaryChipColors()
        }
    },
    PrimaryGradient {
        @Composable override fun chipColors(): ChipColors {
            return ChipDefaults.gradientBackgroundChipColors()
        }
    },
    Secondary {
        @Composable override fun chipColors(): ChipColors {
            return ChipDefaults.secondaryChipColors()
        }
    },
    Child {
        @Composable override fun chipColors(): ChipColors {
            return ChipDefaults.childChipColors()
        }
    };

    @Composable abstract fun chipColors(): ChipColors
}
