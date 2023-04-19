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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.filters.SdkSuppress
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
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
      ) {
        TestImage()
      }
    }

    rule.onNodeWithTag(TEST_TAG).assertExists()
  }

  @Test
  fun has_clickaction_when_enabled() {
    rule.setContentWithTheme {
      Chip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = true,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
      ) {
        TestImage()
      }
    }

    rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
  }

  @Test
  fun has_clickaction_when_disabled() {
    rule.setContentWithTheme {
      Chip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = false,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
      ) {
        TestImage()
      }
    }

    rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
  }

  @Test
  fun is_correctly_enabled_when_enabled_equals_true() {
    rule.setContentWithTheme {
      Chip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = true,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
      ) {
        TestImage()
      }
    }

    rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
  }

  @Test
  fun is_correctly_disabled_when_enabled_equals_false() {
    rule.setContentWithTheme {
      Chip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = false,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
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
      Chip(
        onClick = { clicked = true },
        colors = ChipDefaults.primaryChipColors(),
        enabled = true,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
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
      Chip(
        onClick = { clicked = true },
        colors = ChipDefaults.primaryChipColors(),
        enabled = false,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
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
      Chip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = false,
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
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
  fun has_role_button_for_three_slot_chip() {
    rule.setContentWithTheme {
      Chip(
        onClick = {},
        label = {},
        secondaryLabel = {},
        icon = { TestImage() },
        colors = ChipDefaults.primaryChipColors(),
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
  fun has_role_button_for_compact_chip() {
    rule.setContentWithTheme {
      CompactChip(
        onClick = {},
        label = {},
        icon = { TestImage() },
        colors = ChipDefaults.primaryChipColors(),
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
  fun is_stadium_shape_under_ltr() =
    rule.isStadiumShape(LayoutDirection.Ltr) {
      Chip(
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder(),
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
      ) { /* omit content to allow us to validate the shape by pixel checking */ }
    }

  @Test
  fun is_stadium_shape_under_rtl() =
    rule.isStadiumShape(LayoutDirection.Rtl) {
      Chip(
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder(),
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

    rule.onRoot().assertWidthIsEqualTo(ChipDefaults.IconOnlyCompactChipWidth)
      .assertHeightIsEqualTo(ChipDefaults.CompactChipHeight)
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

    rule.onRoot().assertHeightIsEqualTo(48.dp)
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

    rule.onRoot().assertWidthIsEqualTo(ChipDefaults.IconOnlyCompactChipWidth)
      .assertHeightIsEqualTo(ChipDefaults.CompactChipHeight)
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
      .assertTopPositionInRootIsEqualTo(
        (itemBounds.height - iconBounds.height) / 2 +
          ChipDefaults.CompactChipTapTargetPadding.calculateTopPadding()
      )
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
      .assertTopPositionInRootIsEqualTo(
        (itemBounds.height - iconBounds.height) / 2 +
          ChipDefaults.CompactChipTapTargetPadding.calculateTopPadding()
      )
  }

  private fun verifyHeight(expectedHeight: Dp) {
    rule.verifyHeight(expectedHeight) {
      Chip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        border = ChipDefaults.chipBorder()
      ) {
        TestImage()
      }
    }
  }
}

@Suppress("DEPRECATION")
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
    verifyContentColors(
      TestChipColors.PrimaryGradient,
      ChipStatus.Enabled,
    ) { MaterialTheme.colors.onSurface }

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
      compactChip = true,
    )

  @Test
  fun three_slot_layout_gives_image_background_enabled_colors() =
    verifySlotContentColors(
      TestChipColors.ImageBackground,
      ChipStatus.Enabled,
      { MaterialTheme.colors.onBackground },
      { MaterialTheme.colors.onBackground },
      { MaterialTheme.colors.onBackground }
    )

  @Test
  fun gives_disabled_primary_chip_contrasting_content_color() =
    verifyColors(
      TestChipColors.Primary,
      ChipStatus.Disabled,
      { MaterialTheme.colors.primary },
      { MaterialTheme.colors.background },
      applyAlphaForDisabledContent = false,
    )

  @Test
  fun gives_disabled_primary_compact_chip_contrasting_content_color() =
    verifySlotColors(
      TestChipColors.Primary,
      ChipStatus.Disabled,
      { MaterialTheme.colors.primary },
      { MaterialTheme.colors.background },
      { MaterialTheme.colors.background },
      { MaterialTheme.colors.background },
      applyAlphaForDisabledContent = false,
      compactChip = true,
    )

  @Test
  fun three_slot_layout_gives_disabled_primary_contrasting_content_color() =
    verifySlotColors(
      TestChipColors.Primary,
      ChipStatus.Disabled,
      { MaterialTheme.colors.primary },
      { MaterialTheme.colors.background },
      { MaterialTheme.colors.background },
      { MaterialTheme.colors.background },
      applyAlphaForDisabledContent = false,
    )

  @Test
  fun three_slot_layout_gives_image_background_disabled_colors() =
    verifySlotContentColors(
      TestChipColors.ImageBackground,
      ChipStatus.Disabled,
      { MaterialTheme.colors.onBackground },
      { MaterialTheme.colors.onBackground },
      { MaterialTheme.colors.onBackground }
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
  fun gives_outlined_enabled_colors() =
    verifyColors(
      TestChipColors.Outlined,
      ChipStatus.Enabled,
      { Color.Transparent },
      { MaterialTheme.colors.primary }
    )

  @Test
  fun gives_image_background_enabled_colors() =
    verifyContentColors(
      TestChipColors.ImageBackground,
      ChipStatus.Enabled,
    ) { MaterialTheme.colors.onBackground }

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
      { MaterialTheme.colors.onSurface },
      { Color.Transparent }
    )

  @Test
  fun gives_outlined_disabled_colors() =
    verifyColors(
      TestChipColors.Outlined,
      ChipStatus.Disabled,
      { Color.Transparent },
      { MaterialTheme.colors.primary },
      { Color.Transparent }
    )

  @Test
  fun gives_image_background_disabled_colors() =
    verifyContentColors(
      TestChipColors.ImageBackground,
      ChipStatus.Disabled,
    ) { MaterialTheme.colors.onSurface }

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

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
  @Test
  fun allows_custom_primary_enabled_background_color_override() {
    val overrideColor = Color.Yellow
    rule.setContentWithTheme {
      Box(modifier = Modifier.fillMaxSize()) {
        Chip(
          onClick = {},
          content = {},
          colors = ChipDefaults.primaryChipColors(backgroundColor = overrideColor),
          enabled = true,
          modifier = Modifier.testTag(TEST_TAG),
          border = ChipDefaults.chipBorder()
        )
      }
    }

    rule.onNodeWithTag(TEST_TAG)
      .captureToImage()
      .assertContainsColor(overrideColor, 50.0f)
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
  @Test
  fun allows_custom_primary_disabled_background_color_override() {
    val overrideColor = Color.Yellow
    rule.setContentWithTheme {
      Box(modifier = Modifier.fillMaxSize()) {
        Chip(
          onClick = {},
          content = {},
          colors = ChipDefaults.chipColors(disabledBackgroundColor = overrideColor),
          enabled = false,
          modifier = Modifier.testTag(TEST_TAG),
          border = ChipDefaults.chipBorder()
        )
      }
    }

    rule.onNodeWithTag(TEST_TAG)
      .captureToImage()
      .assertContainsColor(overrideColor, 50.0f)
  }

  @Test
  fun allows_custom_primary_enabled_content_color_override() {
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
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
      )
    }

    assertEquals(overrideColor, actualContentColor)
  }

  @Test
  fun allows_custom_primary_enabled_secondary_label_color_override() {
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
        modifier = Modifier.testTag(TEST_TAG)
      )
    }
    assertEquals(expectedContent, actualContentColor)
    assertEquals(overrideColor, actualSecondaryContentColor)
  }

  @Test
  fun allows_custom_primary_enabled_icon_tint_color_override() {
    val overrideColor = Color.Red
    var actualContentColor = Color.Transparent
    var actualIconColor = Color.Transparent
    var expectedContent = Color.Transparent
    rule.setContentWithTheme {
      expectedContent = MaterialTheme.colors.onPrimary
      Chip(
        onClick = {},
        colors = ChipDefaults.chipColors(
          iconColor = overrideColor
        ),
        label = {
          actualContentColor = LocalContentColor.current
        },
        icon = {
          actualIconColor = LocalContentColor.current
        },
        enabled = true,
        modifier = Modifier.testTag(TEST_TAG)
      )
    }
    assertEquals(expectedContent, actualContentColor)
    assertEquals(overrideColor, actualIconColor)
  }

  @Test
  fun allows_custom_primary_disabled_content_color_override() {
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
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
      )
    }

    assertEquals(overrideColor, actualContentColor)
  }

  private fun verifyContentColors(
    testChipColors: TestChipColors,
    status: ChipStatus,
    contentColor: @Composable () -> Color,
  ) {
    var expectedContent = Color.Transparent
    var actualContent = Color.Transparent
    val testBackground = Color.White

    rule.setContentWithTheme {
      expectedContent = if (status.enabled()) {
        contentColor()
      } else {
        contentColor().copy(alpha = ContentAlpha.disabled)
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
          modifier = Modifier.testTag(TEST_TAG),
          border = ChipDefaults.chipBorder()
        )
      }
    }

    assertEquals(expectedContent, actualContent)
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
  private fun verifyColors(
    testChipColors: TestChipColors,
    status: ChipStatus,
    backgroundColor: @Composable () -> Color,
    contentColor: @Composable () -> Color,
    disabledBackgroundColor: (@Composable () -> Color)? = null,
    applyAlphaForDisabledContent: Boolean = true,
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
          if (disabledBackgroundColor != null) {
            disabledBackgroundColor().compositeOver(testBackground)
          } else {
            backgroundColor().copy(alpha = ContentAlpha.disabled).compositeOver(testBackground)
          }
        expectedContent =
          if (applyAlphaForDisabledContent)
            contentColor().copy(alpha = ContentAlpha.disabled)
          else
            contentColor()
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(testBackground)
      ) {
        if (testChipColors == TestChipColors.Outlined) {
          OutlinedChip(
            onClick = {},
            colors = testChipColors.chipColors(),
            label = { actualContent = LocalContentColor.current },
            enabled = status.enabled(),
            modifier = Modifier.testTag(TEST_TAG),
            border = ChipDefaults.chipBorder()
          )
        } else {
          Chip(
            onClick = {},
            colors = testChipColors.chipColors(),
            content = { actualContent = LocalContentColor.current },
            enabled = status.enabled(),
            modifier = Modifier.testTag(TEST_TAG),
            border = ChipDefaults.chipBorder()
          )
        }
      }
    }

    assertEquals(expectedContent, actualContent)

    rule.onNodeWithTag(TEST_TAG)
      .captureToImage()
      .assertContainsColor(
        if (expectedBackground != Color.Transparent) expectedBackground else testBackground,
        50.0f
      )
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
  private fun verifySlotColors(
    testChipColors: TestChipColors,
    status: ChipStatus,
    backgroundColor: @Composable () -> Color,
    contentColor: @Composable () -> Color,
    secondaryContentColor: @Composable () -> Color,
    iconColor: @Composable () -> Color,
    compactChip: Boolean = false,
    applyAlphaForDisabledContent: Boolean = true,
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
        if (applyAlphaForDisabledContent) {
          expectedContent = contentColor().copy(alpha = ContentAlpha.disabled)
          expectedSecondaryContent = secondaryContentColor()
            .copy(alpha = ContentAlpha.disabled)
          expectedIcon = iconColor().copy(alpha = ContentAlpha.disabled)
        } else {
          expectedContent = contentColor()
          expectedSecondaryContent = secondaryContentColor()
          expectedIcon = iconColor()
        }
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
            modifier = Modifier.testTag(TEST_TAG)
          )
        } else {
          Chip(
            onClick = {},
            colors = testChipColors.chipColors(),
            label = { actualContent = LocalContentColor.current },
            secondaryLabel = { actualSecondaryContent = LocalContentColor.current },
            icon = { actualIcon = LocalContentColor.current },
            enabled = status.enabled(),
            modifier = Modifier.testTag(TEST_TAG)
          )
        }
      }
    }

    assertEquals(expectedContent, actualContent)
    if (!compactChip) {
      assertEquals(expectedSecondaryContent, actualSecondaryContent)
    }
    assertEquals(expectedIcon, actualIcon)

    rule.onNodeWithTag(TEST_TAG)
      .captureToImage()
      .assertContainsColor(
        if (expectedBackground != Color.Transparent) expectedBackground else testBackground,
        50.0f
      )
  }

  private fun verifySlotContentColors(
    testChipColors: TestChipColors,
    status: ChipStatus,
    contentColor: @Composable () -> Color,
    secondaryContentColor: @Composable () -> Color,
    iconColor: @Composable () -> Color,
    compactChip: Boolean = false,
  ) {
    var expectedContent = Color.Transparent
    var expectedSecondaryContent = Color.Transparent
    var expectedIcon = Color.Transparent
    var actualContent = Color.Transparent
    var actualSecondaryContent = Color.Transparent
    var actualIcon = Color.Transparent
    val testBackground = Color.White

    rule.setContentWithTheme {
      if (status.enabled()) {
        expectedContent = contentColor()
        expectedSecondaryContent = secondaryContentColor()
        expectedIcon = iconColor()
      } else {
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
            modifier = Modifier.testTag(TEST_TAG)
          )
        } else {
          Chip(
            onClick = {},
            colors = testChipColors.chipColors(),
            label = { actualContent = LocalContentColor.current },
            secondaryLabel = { actualSecondaryContent = LocalContentColor.current },
            icon = { actualIcon = LocalContentColor.current },
            enabled = status.enabled(),
            modifier = Modifier.testTag(TEST_TAG)
          )
        }
      }
    }

    assertEquals(expectedContent, actualContent)
    if (!compactChip) {
      assertEquals(expectedSecondaryContent, actualSecondaryContent)
    }
    assertEquals(expectedIcon, actualIcon)
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
        modifier = Modifier.testTag(TEST_TAG),
        border = ChipDefaults.chipBorder()
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
        modifier = Modifier.testTag(TEST_TAG)
      )
    }
    assertEquals(expectedTextStyle, actualLabelTextStyle)
    assertEquals(expectedSecondaryTextStyle, actualSecondaryLabelTextStyle)
  }
}

class ChipShapeTest {
  @get:Rule
  val rule = createComposeRule()

  @Test
  fun default_chip_shape_is_circle() {
    rule.isShape(RoundedCornerShape(corner = CornerSize(50))) { modifier ->
      Chip(
        onClick = {},
        label = {},
        enabled = true,
        colors = ChipDefaults.primaryChipColors(),
        modifier = modifier
      )
    }
  }

  @Test
  fun allows_custom_chip_shape_override() {
    val shape = CutCornerShape(4.dp)

    rule.isShape(shape) { modifier ->
      Chip(
        onClick = {},
        label = {},
        enabled = true,
        colors = ChipDefaults.primaryChipColors(),
        shape = shape,
        modifier = modifier
      )
    }
  }

  @Test
  fun default_compact_chip_shape_is_circle() {
    rule.isShape(RoundedCornerShape(corner = CornerSize(50))) { modifier ->
      CompactChip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = true,
        modifier = modifier
      )
    }
  }

  @Test
  fun allows_custom_compact_chip_shape_override() {
    val shape = CutCornerShape(4.dp)

    rule.isShape(shape) { modifier ->
      CompactChip(
        onClick = {},
        colors = ChipDefaults.primaryChipColors(),
        enabled = true,
        shape = shape,
        modifier = modifier
      )
    }
  }

  @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
  private fun ComposeContentTestRule.isShape(
    expectedShape: Shape,
    content: @Composable (Modifier) -> Unit,
  ) {
    var background = Color.Transparent
    var chipColor = Color.Transparent
    val padding = 0.dp

    rule.setContentWithTheme {
      background = MaterialTheme.colors.surface
      chipColor = MaterialTheme.colors.primary
      content(
        Modifier
          .testTag(TEST_TAG)
          .padding(padding)
          .background(background))
    }

    rule.onNodeWithTag(TEST_TAG)
      .captureToImage()
      .assertShape(
        density = rule.density,
        horizontalPadding = 0.dp,
        verticalPadding = 0.dp,
        shapeColor = chipColor,
        backgroundColor = background,
        shape = expectedShape
      )
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
private fun ComposeContentTestRule.isStadiumShape(
  layoutDirection: LayoutDirection,
  content: @Composable () -> Unit,
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

  onNodeWithTag(TEST_TAG)
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
    @Composable
    override fun chipColors(): ChipColors {
      return ChipDefaults.primaryChipColors()
    }
  },
  PrimaryGradient {
    @Composable
    override fun chipColors(): ChipColors {
      return ChipDefaults.gradientBackgroundChipColors()
    }
  },
  Secondary {
    @Composable
    override fun chipColors(): ChipColors {
      return ChipDefaults.secondaryChipColors()
    }
  },
  Child {
    @Composable
    override fun chipColors(): ChipColors {
      return ChipDefaults.childChipColors()
    }
  },
  Outlined {
    @Composable
    override fun chipColors(): ChipColors {
      return ChipDefaults.outlinedChipColors()
    }
  },
  ImageBackground {
    @Composable
    override fun chipColors(): ChipColors {
      return ChipDefaults.imageBackgroundChipColors(
        backgroundImagePainter = rememberVectorPainter(image = Icons.Outlined.Add)
      )
    }
  };

  @Composable
  abstract fun chipColors(): ChipColors
}
