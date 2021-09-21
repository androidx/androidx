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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MaterialThemeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun sets_default_color() {
        var expectedBackground = Color.Transparent
        rule.setContentWithTheme {
            expectedBackground = MaterialTheme.colors.primary
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(), // make sure using the primary color
                label = { Text("Test") },
            )
        }

        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(expectedBackground, 50.0f)
    }

    @Test
    fun overrides_color_when_nested() {
        // MaterialTheme in 'setWearContent' sets the primary background
        // to cornflower blue. The nested theme should override that for primary.
        rule.setContentWithTheme {
            MaterialTheme(colors = MaterialTheme.colors.copy(primary = Color.Cyan)) {
                Chip(
                    onClick = {},
                    colors = ChipDefaults.primaryChipColors(), // make sure using the primary color
                    label = { Text("Test") },
                )
            }
        }

        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(Color.Cyan, 50.0f)
    }

    @Test
    fun can_be_overridden_by_component_color_explicitly() {
        rule.setContentWithTheme {
            Chip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(backgroundColor = Color.Yellow),
                label = { Text("Test") },
            )
        }

        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(Color.Yellow, 50.0f)
    }

    @Test
    fun sets_default_textstyle() {
        var expectedStyle: TextStyle? = null

        rule.setContentWithTheme {
            expectedStyle = MaterialTheme.typography.button
            Chip(
                onClick = {},
                label = { Text("Test") },
            )
        }

        assertTextTypographyEquals(expectedStyle!!, rule.textStyleOf("Test"))
    }

    @Test
    fun overrides_textstyle_when_nested() {
        val override = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.sp
        )
        rule.setContentWithTheme {
            MaterialTheme(
                typography = MaterialTheme.typography
                    .copy(button = override)
            ) {
                Chip(
                    onClick = {},
                    label = { Text("Test") },
                )
            }
        }

        assertTextTypographyEquals(override, rule.textStyleOf("Test"))
    }

    @Test
    fun sets_primary_color_dynamically() =
        verifyBackgroundColorIsDynamic(
            initial = { MaterialTheme.colors.primary },
            selectChipColors = { ChipDefaults.primaryChipColors() },
            updateThemeColors = { colors, primary -> colors.copy(primary = primary) }
        )

    @Test
    fun sets_primaryvariant_color_dynamically() =
        verifyBackgroundColorIsDynamic(
            initial = { MaterialTheme.colors.primaryVariant },
            selectChipColors = {
                ChipDefaults
                    .primaryChipColors(backgroundColor = MaterialTheme.colors.primaryVariant)
            },
            updateThemeColors =
                { colors, primaryVariant -> colors.copy(primaryVariant = primaryVariant) }
        )

    @Test
    fun sets_secondary_color_dynamically() =
        verifyBackgroundColorIsDynamic(
            initial = { MaterialTheme.colors.secondary },
            selectChipColors = {
                ChipDefaults
                    .secondaryChipColors(backgroundColor = MaterialTheme.colors.secondary)
            },
            updateThemeColors = { colors, secondary -> colors.copy(secondary = secondary) }
        )

    @Test
    fun sets_secondaryvariant_color_dynamically() =
        verifyBackgroundColorIsDynamic(
            initial = { MaterialTheme.colors.secondaryVariant },
            selectChipColors = {
                ChipDefaults
                    .secondaryChipColors(backgroundColor = MaterialTheme.colors.secondaryVariant)
            },
            updateThemeColors =
                { colors, secondaryVariant -> colors.copy(secondaryVariant = secondaryVariant) }
        )

    @Test
    fun sets_error_color_dynamically() =
        verifyBackgroundColorIsDynamic(
            initial = { MaterialTheme.colors.error },
            selectChipColors = {
                ChipDefaults
                    .secondaryChipColors(backgroundColor = MaterialTheme.colors.error)
            },
            updateThemeColors = { colors, error -> colors.copy(error = error) }
        )

    @Test
    fun sets_colors_dynamically() {
        var initialBackground = Color.Transparent
        val overrideBackground = Color.Cyan

        rule.setContentWithTheme {
            initialBackground = MaterialTheme.colors.primary
            val colors = Colors()
            val rememberedColors = remember { mutableStateOf(colors) }
            MaterialTheme(colors = rememberedColors.value) {
                Column {
                    Chip(
                        onClick = {},
                        colors = ChipDefaults.primaryChipColors(),
                        label = { Text("Test") },
                    )
                    Chip(
                        onClick = {
                            rememberedColors.value = colors.copy(primary = overrideBackground)
                        },
                        label = { },
                        modifier = Modifier.testTag("button")
                    )
                }
            }
        }

        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(initialBackground, 60.0f)
        rule.onNodeWithTag("button")
            .performClick()
        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(overrideBackground, 60.0f)
    }

    @Test
    fun sets_button_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.button },
            updateTextStyle = { typography, button -> typography.copy(button = button) }
        )

    @Test
    fun sets_display1_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.display1 },
            updateTextStyle = { typography, display1 -> typography.copy(display1 = display1) }
        )

    @Test
    fun sets_display2_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.display2 },
            updateTextStyle = { typography, display2 -> typography.copy(display2 = display2) }
        )

    @Test
    fun sets_display3_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.display3 },
            updateTextStyle = { typography, display3 -> typography.copy(display3 = display3) }
        )

    @Test
    fun sets_title1_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.title1 },
            updateTextStyle = { typography, title1 -> typography.copy(title1 = title1) }
        )

    @Test
    fun sets_title2_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.title2 },
            updateTextStyle = { typography, title2 -> typography.copy(title2 = title2) }
        )

    @Test
    fun sets_title3_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.title3 },
            updateTextStyle = { typography, title3 -> typography.copy(title3 = title3) }
        )

    @Test
    fun sets_body1_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.body1 },
            updateTextStyle = { typography, body1 -> typography.copy(body1 = body1) }
        )

    @Test
    fun sets_body2_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.body2 },
            updateTextStyle = { typography, body2 -> typography.copy(body2 = body2) }
        )

    @Test
    fun sets_caption1_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.caption1 },
            updateTextStyle = { typography, caption1 -> typography.copy(caption1 = caption1) }
        )

    @Test
    fun sets_caption2_textstyle_dynamically() =
        verifyTextStyleIsDynamic(
            selectStyle = { it.caption2 },
            updateTextStyle = { typography, caption2 -> typography.copy(caption2 = caption2) }
        )

    @Test
    fun sets_typography_dynamically() {
        val initialStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.sp
        )
        val overrideTextStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 8.sp,
            letterSpacing = 0.sp
        )

        rule.setContentWithTheme {
            val typography = Typography()
            val rememberedTypography = remember { mutableStateOf(typography) }
            MaterialTheme(typography = rememberedTypography.value) {
                Column {
                    Chip(
                        onClick = {},
                        colors = ChipDefaults.primaryChipColors(),
                        label = { Text("Test") },
                    )
                    Chip(
                        onClick = {
                            rememberedTypography.value =
                                typography.copy(button = overrideTextStyle)
                        },
                        label = { },
                        modifier = Modifier.testTag("button")
                    )
                }
            }
        }

        assertTextTypographyEquals(initialStyle, rule.textStyleOf("Test"))
        rule.onNodeWithTag("button").performClick()
        assertTextTypographyEquals(overrideTextStyle, rule.textStyleOf("Test"))
    }

    private fun verifyBackgroundColorIsDynamic(
        initial: @Composable () -> Color,
        selectChipColors: @Composable () -> ChipColors,
        updateThemeColors: (Colors, Color) -> Colors
    ) {
        var initialColor = Color.Transparent
        val overrideColor = Color.Cyan
        val colors = Colors()

        rule.setContentWithTheme {
            initialColor = initial()
            val dynamicColor = remember { mutableStateOf(initialColor) }
            val themeColors = updateThemeColors(colors, dynamicColor.value)
            MaterialTheme(colors = themeColors) {
                Column {
                    Chip(
                        onClick = {},
                        colors = selectChipColors(),
                        label = { Text("Test") },
                    )
                    Chip(
                        onClick = { dynamicColor.value = overrideColor },
                        label = { },
                        modifier = Modifier.testTag("button")
                    )
                }
            }
        }

        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(initialColor, 60.0f)
        rule.onNodeWithTag("button")
            .performClick()
        rule.onNodeWithText("Test")
            .captureToImage()
            .assertContainsColor(overrideColor, 60.0f)
    }

    private fun verifyContentColorIsDynamic(
        initial: @Composable () -> Color,
        selectChipColors: @Composable () -> ChipColors,
        updateThemeColors: (Colors, Color) -> Colors
    ) {
        var initialColor = Color.White
        val overrideColor = Color.Cyan
        val colors = Colors()

        rule.setContentWithTheme {
            initialColor = initial()
            val dynamicColor = remember { mutableStateOf(initialColor) }
            val themeColors = updateThemeColors(colors, dynamicColor.value)
            MaterialTheme(colors = themeColors) {
                Column {
                    Chip(
                        onClick = {},
                        colors = selectChipColors(),
                        label = { Text("Test") },
                    )
                    Chip(
                        onClick = { dynamicColor.value = overrideColor },
                        label = { Text("Test") },
                        modifier = Modifier.testTag("button")
                    )
                }
            }
        }

        assertEquals(initialColor, rule.textStyleOf("Test").color)
        rule.onNodeWithTag("button")
            .performClick()
        assertEquals(overrideColor, rule.textStyleOf("Test").color)
    }

    private fun verifyTextStyleIsDynamic(
        selectStyle: (Typography) -> TextStyle,
        updateTextStyle: (Typography, TextStyle) -> Typography
    ) {
        var initialStyle = TextStyle()
        val overrideTextStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 8.sp,
            letterSpacing = 0.sp
        )
        val typography = Typography()

        rule.setContentWithTheme {
            initialStyle = selectStyle(typography)
            val dynamicStyle = remember { mutableStateOf(initialStyle) }
            val rememberedTypography =
                updateTextStyle(typography, dynamicStyle.value)
            MaterialTheme(
                // WearChip always uses 'button' style for text, so assign the style under test to button.
                typography = rememberedTypography.copy(button = selectStyle(rememberedTypography))
            ) {
                Column {
                    Chip(
                        onClick = {},
                        label = { Text("Test") },
                    )
                    Chip(
                        onClick = { dynamicStyle.value = overrideTextStyle },
                        label = { },
                        modifier = Modifier.testTag("button")
                    )
                }
            }
        }

        assertTextTypographyEquals(initialStyle, rule.textStyleOf("Test"))
        rule.onNodeWithTag("button").performClick()
        assertTextTypographyEquals(overrideTextStyle, rule.textStyleOf("Test"))
    }
}