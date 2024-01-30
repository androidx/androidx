
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
package androidx.wear.compose.materialcore

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private val DEFAULT_COMPACT_CHIP_VERTICAL_PADDING = 8.dp
private val BACKGROUND_ENABLED_COLOR = Color.Green
private val BACKGROUND_DISABLED_COLOR = Color.Red
private val BORDER_ENABLED_COLOR = Color.Blue
private val BORDER_DISABLED_COLOR = Color.Yellow

@Suppress("DEPRECATION")
class ChipTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun basechip_supports_testtag() {
        rule.setContent {
            BaseChipWithDefaults(modifier = Modifier.testTag(TEST_TAG)) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun threeslotchip_supports_testtag() {
        rule.setContent {
            ThreeSlotChipWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun compactchip_supports_testtag() {
        rule.setContent {
            CompactChipWithDefaults(modifier = Modifier.testTag(TEST_TAG))
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun basechip_has_clickaction_when_enabled() {
        rule.setContent {
            BaseChipWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun threeslotchip_has_clickaction_when_enabled() {
        rule.setContent {
            ThreeSlotChipWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun compactchip_has_clickaction_when_enabled() {
        rule.setContent {
            CompactChipWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun basechip_has_clickaction_when_disabled() {
        rule.setContent {
            BaseChipWithDefaults(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun threeslotchip_has_clickaction_when_disabled() {
        rule.setContent {
            ThreeSlotChipWithDefaults(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun compactchip_has_clickaction_when_disabled() {
        rule.setContent {
            CompactChipWithDefaults(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun basechip_is_correctly_enabled() {
        rule.setContent {
            BaseChipWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun threeslotchip_is_correctly_enabled() {
        rule.setContent {
            ThreeSlotChipWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun compactchip_is_correctly_enabled() {
        rule.setContent {
            CompactChipWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun basechip_is_correctly_disabled() {
        rule.setContent {
            BaseChipWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun threeslotchip_is_correctly_disabled() {
        rule.setContent {
            ThreeSlotChipWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun compactchip_is_correctly_disabled() {
        rule.setContent {
            CompactChipWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun basechip_responds_to_click_when_enabled() {
        var clicked = false
        rule.setContent {
            BaseChipWithDefaults(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun threeslotchip_responds_to_click_when_enabled() {
        var clicked = false
        rule.setContent {
            ThreeSlotChipWithDefaults(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun compactchip_responds_to_click_when_enabled() {
        var clicked = false
        rule.setContent {
            CompactChipWithDefaults(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun basechip_does_not_respond_to_click_when_disabled() {
        var clicked = false
        rule.setContent {
            BaseChipWithDefaults(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun threeslotchip_does_not_respond_to_click_when_disabled() {
        var clicked = false
        rule.setContent {
            ThreeSlotChipWithDefaults(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun compactchip_does_not_respond_to_click_when_disabled() {
        var clicked = false
        rule.setContent {
            CompactChipWithDefaults(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun basechip_uses_semantic_role_override() {
        rule.setContent {
            BaseChipWithDefaults(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                role = Role.Image
            ) {
            }
        }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Image
                )
            )
    }

    @Test
    fun threeslotchip_uses_semantic_role_override() {
        rule.setContent {
            ThreeSlotChipWithDefaults(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG),
                role = Role.Image
            )
        }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Image
                )
            )
    }

    @Test
    fun compactchip_uses_semantic_role_override() {
        rule.setContent {
            CompactChipWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                role = Role.Image
            )
        }
        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Image
                )
            )
    }

    @Test
    fun basechip_uses_shape_override() =
        rule.isShape(CircleShape, LayoutDirection.Ltr) {
            BaseChipWithDefaults(
                shape = CircleShape,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }

    @Test
    fun threeslotchip_uses_shape_override() =
        rule.isShape(CircleShape, LayoutDirection.Ltr) {
            ThreeSlotChipWithDefaults(
                shape = CircleShape,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

    @Test
    fun compactchip_uses_shape_override() =
        rule.isShape(CircleShape, LayoutDirection.Ltr) {
            CompactChipWithDefaults(
                shape = CircleShape,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

    @Test
    fun threeslotchip_has_icon_in_correct_location_when_only_single_line_of_text() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                ThreeSlotChipWithDefaults(
                    label = { TestText("Blue green orange") },
                    icon = { TestImage(iconLabel = iconTag) },
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
    fun icon_only_compactchip_has_correct_default_width_and_height() {
        val iconTag = "TestIcon"
        val chipTag = "chip"
        val width = 52.dp
        val height = 48.dp
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                CompactChipWithDefaults(
                    modifier = Modifier.testTag(chipTag),
                    icon = { TestImage(iconLabel = iconTag) },
                    defaultIconOnlyCompactChipWidth = width,
                    height = height,
                )
            }
        rule.onRoot()
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun label_only_compactchip_has_correct_default_height() {
        val chipTag = "chip"
        val height = 48.dp
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                CompactChipWithDefaults(
                    modifier = Modifier.testTag(chipTag),
                    label = { TestText("Test") },
                    height = height,
                )
            }
        rule.onRoot().assertHeightIsEqualTo(height)
    }

    @Test
    fun no_content_compactchip_has_correct_default_width_and_height() {
        val width = 52.dp
        val height = 48.dp
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                CompactChipWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    defaultIconOnlyCompactChipWidth = width,
                    height = height,
                )
            }
        rule.onRoot()
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun icon_only_compactchip_can_have_width_overridden() {
        val iconTag = "TestIcon"
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                CompactChipWithDefaults(
                    modifier = Modifier
                        .testTag(TEST_TAG)
                        .width(100.dp),
                    icon = { TestImage(iconLabel = iconTag) }
                )
            }
        rule.onRoot().assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun compactchip_has_icon_in_correct_location() {
        val iconTag = "TestIcon"
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                CompactChipWithDefaults(
                    label = { TestText("Test label") },
                    icon = { TestImage(iconLabel = iconTag) },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        val itemBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(iconTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        rule.onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(
                (itemBounds.height - iconBounds.height) / 2 + DEFAULT_COMPACT_CHIP_VERTICAL_PADDING
            )
    }

    @Test
    fun icon_only_compactchip_has_icon_in_correct_location() {
        val iconTag = "TestIcon"
        rule
            .setContentForSizeAssertions(useUnmergedTree = true) {
                CompactChipWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    icon = { TestImage(iconLabel = iconTag) }
                )
            }
        val itemBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(iconTag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        rule.onNodeWithContentDescription(iconTag, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(
                (itemBounds.height - iconBounds.height) / 2 + DEFAULT_COMPACT_CHIP_VERTICAL_PADDING
            )
    }

    @Test
    fun gives_enabled_basechip_correct_colors() =
        verifyColors(
            expectedBackgroundColor = BACKGROUND_ENABLED_COLOR,
            expectedBorderColor = BORDER_ENABLED_COLOR,
            content = { BaseChipWithColor(enabled = true) }
        )

    @Test
    fun gives_disabled_basechip_correct_colors() =
        verifyColors(
            expectedBackgroundColor = BACKGROUND_DISABLED_COLOR,
            expectedBorderColor = BORDER_DISABLED_COLOR,
            content = { BaseChipWithColor(enabled = false) }
        )

    @Test
    fun gives_enabled_threeslotchip_correct_colors() =
        verifyColors(
            expectedBackgroundColor = BACKGROUND_ENABLED_COLOR,
            expectedBorderColor = BORDER_ENABLED_COLOR,
            content = { ThreeSlotChipWithColor(enabled = true) }
        )

    @Test
    fun gives_disabled_threeslotchip_correct_colors() =
        verifyColors(
            expectedBackgroundColor = BACKGROUND_DISABLED_COLOR,
            expectedBorderColor = BORDER_DISABLED_COLOR,
            content = { ThreeSlotChipWithColor(enabled = false) }
        )

    @Test
    fun gives_enabled_compactchip_correct_colors() =
        verifyColors(
            expectedBackgroundColor = BACKGROUND_ENABLED_COLOR,
            expectedBorderColor = BORDER_ENABLED_COLOR,
            content = { CompactChipWithColor(enabled = true) }
        )

    @Test
    fun gives_disabled_compactchip_correct_colors() =
        verifyColors(
            expectedBackgroundColor = BACKGROUND_DISABLED_COLOR,
            expectedBorderColor = BORDER_DISABLED_COLOR,
            content = { CompactChipWithColor(enabled = false) }
        )

    @Test
    fun basechip_obeys_content_provider_values() {
        var data = -1
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                BaseChipWithDefaults(
                    content = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    },
                )
            }
        }
        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @Test
    fun threeslotchip_obeys_icon_provider_values() {
        var data = -1
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ThreeSlotChipWithDefaults(
                    icon = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    },
                )
            }
        }
        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @Test
    fun threeslotchip_obeys_label_provider_values() {
        var data = -1
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ThreeSlotChipWithDefaults(
                    label = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    },
                )
            }
        }
        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @Test
    fun threeslotchip_obeys_secondary_label_provider_values() {
        var data = -1
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ThreeSlotChipWithDefaults(
                    secondaryLabel = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    },
                )
            }
        }
        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @Test
    fun compactchip_obeys_icon_provider_values() {
        var data = -1
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactChipWithDefaults(
                    icon = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    },
                )
            }
        }
        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @Test
    fun compactchip_obeys_label_provider_values() {
        var data = -1
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactChipWithDefaults(
                    label = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    },
                )
            }
        }
        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @Composable
    private fun BaseChipWithDefaults(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        background: @Composable (enabled: Boolean) -> State<Painter> = {
            remember { mutableStateOf(ColorPainter(DEFAULT_SHAPE_COLOR)) }
        },
        border: @Composable (enabled: Boolean) -> State<BorderStroke?>? = { null },
        enabled: Boolean = true,
        contentPadding: PaddingValues = PaddingValues(14.dp, 6.dp),
        shape: Shape = RoundedCornerShape(corner = CornerSize(50)),
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        role: Role? = Role.Button,
        height: Dp = 52.dp,
        content: @Composable RowScope.() -> Unit,
    ) = Chip(
        modifier = modifier.height(height),
        onClick = onClick,
        background = background,
        border = border,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        interactionSource = interactionSource,
        role = role,
        content = content
    )

    @Composable
    private fun ThreeSlotChipWithDefaults(
        modifier: Modifier = Modifier,
        label: @Composable RowScope.() -> Unit = {},
        onClick: () -> Unit = {},
        background: @Composable (enabled: Boolean) -> State<Painter> = {
            remember { mutableStateOf(ColorPainter(DEFAULT_SHAPE_COLOR)) }
        },
        secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
        icon: (@Composable BoxScope.() -> Unit)? = null,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        contentPadding: PaddingValues = PaddingValues(14.dp, 6.dp),
        shape: Shape = RoundedCornerShape(corner = CornerSize(50)),
        border: @Composable (enabled: Boolean) -> State<BorderStroke?>? = { null },
        defaultIconSpacing: Dp = 6.dp,
        height: Dp = 52.dp,
        role: Role? = Role.Button,
    ) = Chip(
        modifier = modifier.height(height),
        label = label,
        onClick = onClick,
        background = background,
        secondaryLabel = secondaryLabel,
        icon = icon,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        border = border,
        defaultIconSpacing = defaultIconSpacing,
        role = role,
    )

    @Composable
    private fun CompactChipWithDefaults(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        background: @Composable (enabled: Boolean) -> State<Painter> = {
            remember { mutableStateOf(ColorPainter(DEFAULT_SHAPE_COLOR)) }
        },
        label: (@Composable RowScope.() -> Unit)? = null,
        icon: (@Composable BoxScope.() -> Unit)? = null,
        enabled: Boolean = true,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        contentPadding: PaddingValues = PaddingValues(14.dp, 6.dp),
        shape: Shape = RoundedCornerShape(corner = CornerSize(50)),
        border: @Composable (enabled: Boolean) -> State<BorderStroke?>? = { null },
        defaultIconOnlyCompactChipWidth: Dp = 52.dp,
        defaultCompactChipTapTargetPadding: PaddingValues = PaddingValues(
            vertical = DEFAULT_COMPACT_CHIP_VERTICAL_PADDING
        ),
        defaultIconSpacing: Dp = 6.dp,
        height: Dp = 48.dp,
        role: Role? = Role.Button,
    ) = CompactChip(
        modifier = modifier.height(height),
        onClick = onClick,
        background = background,
        label = label,
        icon = icon,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        border = border,
        defaultIconOnlyCompactChipWidth = defaultIconOnlyCompactChipWidth,
        defaultCompactChipTapTargetPadding = defaultCompactChipTapTargetPadding,
        defaultIconSpacing = defaultIconSpacing,
        role = role,
    )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun verifyColors(
        expectedBackgroundColor: Color,
        expectedBorderColor: Color,
        backgroundThreshold: Float = 50.0f,
        borderThreshold: Float = 1.0f,
        content: @Composable () -> Unit,
    ) {
        val testBackground = Color.White
        val expectedColor = { color: Color ->
            if (color != Color.Transparent)
                color.compositeOver(testBackground)
            else
                testBackground
        }
        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                content()
            }
        }

        val bitmap = rule.onNodeWithTag(TEST_TAG).captureToImage()
        bitmap.assertContainsColor(expectedColor(expectedBackgroundColor), backgroundThreshold)
        bitmap.assertContainsColor(expectedColor(expectedBorderColor), borderThreshold)
    }

    @Composable
    private fun BaseChipWithColor(enabled: Boolean) =
        BaseChipWithDefaults(
            background = {
                rememberUpdatedState(
                    ColorPainter(if (it) BACKGROUND_ENABLED_COLOR else BACKGROUND_DISABLED_COLOR)
                )
            },
            border = {
                rememberUpdatedState(
                    BorderStroke(2.dp, if (it) BORDER_ENABLED_COLOR else BORDER_DISABLED_COLOR)
                )
            },
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        ) {
        }

    @Composable
    private fun ThreeSlotChipWithColor(enabled: Boolean) =
        ThreeSlotChipWithDefaults(
            background = {
                rememberUpdatedState(
                    ColorPainter(if (it) BACKGROUND_ENABLED_COLOR else BACKGROUND_DISABLED_COLOR)
                )
            },
            border = {
                rememberUpdatedState(
                    BorderStroke(2.dp, if (it) BORDER_ENABLED_COLOR else BORDER_DISABLED_COLOR)
                )
            },
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        )

    @Composable
    private fun CompactChipWithColor(enabled: Boolean) =
        CompactChipWithDefaults(
            background = {
                rememberUpdatedState(
                    ColorPainter(if (it) BACKGROUND_ENABLED_COLOR else BACKGROUND_DISABLED_COLOR)
                )
            },
            border = {
                rememberUpdatedState(
                    BorderStroke(2.dp, if (it) BORDER_ENABLED_COLOR else BORDER_DISABLED_COLOR)
                )
            },
            enabled = enabled,
            modifier = Modifier.testTag(TEST_TAG)
        )
}
