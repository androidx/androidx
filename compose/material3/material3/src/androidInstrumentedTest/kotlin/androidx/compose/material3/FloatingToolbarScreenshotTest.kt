/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class FloatingToolbarScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun horizontalFloatingToolbar() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(expanded = false) { ToolbarContent() }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "horizontalFloatingToolbar_${scheme.name}")
    }

    @Test
    fun verticalFloatingToolbar() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(expanded = false) { ToolbarContent() }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingToolbar_${scheme.name}")
    }

    @Test
    fun horizontalFloatingToolbar_leading() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    leadingContent = { ToolbarLeadingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "horizontalFloatingToolbar_leading_${scheme.name}")
    }

    @Test
    fun horizontalFloatingToolbar_trailing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_trailing_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing_vibrant() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_vibrant_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing_collapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_collapsed_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing_collapsed_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_collapsed_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing_expanded_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_expanded_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing_collapsed_vibrant() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_collapsed_vibrant_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_leading_trailing_rtl() {
        rule.setMaterialContent(scheme.colorScheme) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.testTag(FloatingToolbarTestTag)) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        leadingContent = { ToolbarLeadingContent() },
                        trailingContent = { ToolbarTrailingContent() },
                    ) {
                        ToolbarContent()
                    }
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_leading_trailing_rtl_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_balancedPaddingWhenCollapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    FilledIconButton(
                        onClick = { /* doSomething() */ },
                        modifier = Modifier.width(52.dp),
                    ) {
                        Icon(Icons.Outlined.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_balancedPaddingWhenCollapsed${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_imbalancedPadding() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val labels = listOf("S", "M", "T", "SA", "W", "All")
                var selectedIndex = 2
                HorizontalFloatingToolbar(expanded = false) {
                    labels.forEachIndexed { index, labelString ->
                        if (selectedIndex == index) {
                            FilledIconButton(onClick = {}) { Text(text = labelString) }
                        } else {
                            IconButton(onClick = {}) { Text(text = labelString) }
                        }
                    }
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_imbalancedPadding_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_leading() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = true,
                    leadingContent = { ToolbarLeadingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingToolbar_leading_${scheme.name}")
    }

    @Test
    fun verticalFloatingToolbar_trailing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = true,
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingToolbar_trailing_${scheme.name}")
    }

    @Test
    fun verticalFloatingToolbar_leading_trailing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = true,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_leading_trailing_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_leading_trailing_collapsed_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                VerticalFloatingToolbar(
                    expanded = false,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_leading_trailing_collapsed_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_leading_trailing_expanded_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                VerticalFloatingToolbar(
                    expanded = true,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_leading_trailing_expanded_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_leading_trailing_vibrant() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_leading_trailing_vibrant_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_leading_trailing_collapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = false,
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_leading_trailing_collapsed_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_leading_trailing_collapsed_vibrant() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = false,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_leading_trailing_collapsed_vibrant_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_balancedPaddingWhenCollapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                VerticalFloatingToolbar(
                    expanded = false,
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    leadingContent = { ToolbarLeadingContent() },
                    trailingContent = { ToolbarTrailingContent() },
                ) {
                    FilledIconButton(
                        onClick = { /* doSomething() */ },
                        modifier = Modifier.height(52.dp),
                    ) {
                        Icon(Icons.Outlined.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_balancedPaddingWhenCollapsed_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_imbalancedPadding() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val labels = listOf("S", "M", "T", "SA", "W", "All")
                var selectedIndex = 2
                VerticalFloatingToolbar(expanded = false) {
                    labels.forEachIndexed { index, labelString ->
                        if (selectedIndex == index) {
                            FilledIconButton(onClick = {}) { Text(text = labelString) }
                        } else {
                            IconButton(onClick = {}) { Text(text = labelString) }
                        }
                    }
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_imbalancedPadding_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_expanded_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_expanded_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_expanded_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_expanded_vibrant() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = true) },
                    colors = colors,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_expanded_vibrant_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_collapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_collapsed_${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_customFabShape_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = false, shape = CircleShape) },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_customFabShape_expanded${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_customFabShape_collapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    floatingActionButton = { ToolbarFab(isVibrant = false, shape = CircleShape) },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_customFabShape_collapsed${scheme.name}",
            )
    }

    @Test
    fun horizontalFloatingToolbar_withFab_collapsed_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = false,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                    expandedShadowElevation = ElevationTokens.Level4,
                    collapsedShadowElevation = ElevationTokens.Level2,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingToolbar_withFab_collapsed_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_withFab_expanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_withFab_expanded_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_withFab_expanded_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                VerticalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_withFab_expanded_customShadowElevation_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_withFab_expanded_vibrant() {
        rule.setMaterialContent(scheme.colorScheme) {
            val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = { ToolbarFab(isVibrant = true) },
                    colors = colors,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_withFab_expanded_vibrant_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_withFab_collapsed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(FloatingToolbarTestTag)) {
                VerticalFloatingToolbar(
                    expanded = false,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_withFab_collapsed_${scheme.name}",
            )
    }

    @Test
    fun verticalFloatingToolbar_withFab_collapsed_customShadowElevation() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(
                Modifier.testTag(FloatingToolbarTestTag).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                VerticalFloatingToolbar(
                    expanded = false,
                    floatingActionButton = { ToolbarFab(isVibrant = false) },
                    expandedShadowElevation = ElevationTokens.Level2,
                    collapsedShadowElevation = ElevationTokens.Level4,
                ) {
                    ToolbarContent()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingToolbarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingToolbar_withFab_collapsed_customShadowElevation_${scheme.name}",
            )
    }

    @Composable
    private fun ToolbarContent() {
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Check, contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
        }
        FilledIconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Outlined.Favorite, contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Add, contentDescription = "Localized description")
        }
    }

    @Composable
    private fun ToolbarLeadingContent() {
        IconButton(
            onClick = { /* doSomething() */ },
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
        ) {
            Icon(Icons.Filled.Create, contentDescription = "Localized description")
        }
    }

    @Composable
    private fun ToolbarTrailingContent() {
        IconButton(
            onClick = { /* doSomething() */ },
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Localized description")
        }
    }

    @Composable
    private fun ToolbarFab(isVibrant: Boolean, shape: Shape = FloatingActionButtonDefaults.shape) {
        if (isVibrant) {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = { /* doSomething() */ },
                shape = shape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Localized description")
            }
        } else {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = { /* doSomething() */ },
                shape = shape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Localized description")
            }
        }
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    private val FloatingToolbarTestTag = "floatingToolbar"
}
