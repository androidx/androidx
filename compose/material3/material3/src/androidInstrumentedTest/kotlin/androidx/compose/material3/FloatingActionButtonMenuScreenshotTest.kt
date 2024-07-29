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
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.ToggleableFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class FloatingActionButtonMenuScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun fabMenu_collapsed_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) { testContent() }

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenu_collapsed_lightTheme")
    }

    @Test
    fun fabMenu_collapsed_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) { testContent() }

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenu_collapsed_darkTheme")
    }

    @Test
    fun fabMenu_expanded_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) { testContent() }

        rule.onNodeWithTag(ToggleableFabTestTag).performClick()

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenu_expanded_lightTheme")
    }

    @Test
    fun fabMenu_expanded_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) { testContent() }

        rule.onNodeWithTag(ToggleableFabTestTag).performClick()

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenu_expanded_darkTheme")
    }

    @Test
    fun fabMenuMediumSecondaryContainer_collapsed_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            testContent(
                containerColor =
                    ToggleableFloatingActionButtonDefaults.containerColor(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.secondary
                    ),
                containerSize = ToggleableFloatingActionButtonDefaults.containerSizeMedium(),
                containerCornerRadius =
                    ToggleableFloatingActionButtonDefaults.containerCornerRadiusMedium(),
                iconColor =
                    ToggleableFloatingActionButtonDefaults.iconColor(
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        MaterialTheme.colorScheme.onSecondary
                    ),
                iconSize = ToggleableFloatingActionButtonDefaults.iconSizeMedium(),
                itemContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "fabMenuMediumSecondaryContainer_collapsed_lightTheme"
            )
    }

    @Test
    fun fabMenuMediumSecondaryContainer_expanded_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            testContent(
                containerColor =
                    ToggleableFloatingActionButtonDefaults.containerColor(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.secondary
                    ),
                containerSize = ToggleableFloatingActionButtonDefaults.containerSizeMedium(),
                containerCornerRadius =
                    ToggleableFloatingActionButtonDefaults.containerCornerRadiusMedium(),
                iconColor =
                    ToggleableFloatingActionButtonDefaults.iconColor(
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        MaterialTheme.colorScheme.onSecondary
                    ),
                iconSize = ToggleableFloatingActionButtonDefaults.iconSizeMedium(),
                itemContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        rule.onNodeWithTag(ToggleableFabTestTag).performClick()

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "fabMenuMediumSecondaryContainer_expanded_lightTheme"
            )
    }

    @Test
    fun fabMenuLargeTertiary_collapsed_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            testContent(
                containerColor =
                    ToggleableFloatingActionButtonDefaults.containerColor(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary
                    ),
                containerSize = ToggleableFloatingActionButtonDefaults.containerSizeLarge(),
                containerCornerRadius =
                    ToggleableFloatingActionButtonDefaults.containerCornerRadiusLarge(),
                iconColor =
                    ToggleableFloatingActionButtonDefaults.iconColor(
                        MaterialTheme.colorScheme.onTertiary,
                        MaterialTheme.colorScheme.onTertiary
                    ),
                iconSize = ToggleableFloatingActionButtonDefaults.iconSizeLarge(),
                itemContainerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenuLargeTertiary_collapsed_lightTheme")
    }

    @Test
    fun fabMenuLargeTertiary_expanded_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            testContent(
                containerColor =
                    ToggleableFloatingActionButtonDefaults.containerColor(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary
                    ),
                containerSize = ToggleableFloatingActionButtonDefaults.containerSizeLarge(),
                containerCornerRadius =
                    ToggleableFloatingActionButtonDefaults.containerCornerRadiusLarge(),
                iconColor =
                    ToggleableFloatingActionButtonDefaults.iconColor(
                        MaterialTheme.colorScheme.onTertiary,
                        MaterialTheme.colorScheme.onTertiary
                    ),
                iconSize = ToggleableFloatingActionButtonDefaults.iconSizeLarge(),
                itemContainerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        rule.onNodeWithTag(ToggleableFabTestTag).performClick()

        rule
            .onNodeWithTag(FabMenuTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenuLargeTertiary_expanded_lightTheme")
    }

    @Test
    fun fabMenuItem_iconOnly_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButtonMenu(expanded = true, button = {}) {
                FloatingActionButtonMenuItem(
                    modifier = Modifier.testTag(FabMenuItemTestTag),
                    onClick = {},
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = {},
                )
            }
        }

        rule
            .onNodeWithTag(FabMenuItemTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenuItem_iconOnly_lightTheme")
    }

    @Test
    fun fabMenuItem_textOnly_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButtonMenu(expanded = true, button = {}) {
                FloatingActionButtonMenuItem(
                    modifier = Modifier.testTag(FabMenuItemTestTag),
                    onClick = {},
                    icon = {},
                    text = { Text(text = "Text") },
                )
            }
        }

        rule
            .onNodeWithTag(FabMenuItemTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenuItem_textOnly_lightTheme")
    }

    @Test
    fun fabMenuItem_minimumTextOnly_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButtonMenu(expanded = true, button = {}) {
                FloatingActionButtonMenuItem(
                    modifier = Modifier.testTag(FabMenuItemTestTag),
                    onClick = {},
                    icon = {},
                    text = { Text(text = ".") },
                )
            }
        }

        rule
            .onNodeWithTag(FabMenuItemTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "fabMenuItem_minimumTextOnly_lightTheme")
    }

    private val FabMenuTestTag = "fabMenu"
    private val FabMenuItemTestTag = "fabMenuItem"
    private val ToggleableFabTestTag = "toggleableFab"

    @Composable
    private fun testContent(
        containerColor: (Float) -> Color = ToggleableFloatingActionButtonDefaults.containerColor(),
        containerSize: (Float) -> Dp = ToggleableFloatingActionButtonDefaults.containerSize(),
        containerCornerRadius: (Float) -> Dp =
            ToggleableFloatingActionButtonDefaults.containerCornerRadius(),
        iconColor: (Float) -> Color = ToggleableFloatingActionButtonDefaults.iconColor(),
        iconSize: (Float) -> Dp = ToggleableFloatingActionButtonDefaults.iconSize(),
        itemContainerColor: Color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box {
            val items =
                listOf(
                    Icons.Filled.Add to "Add",
                    Icons.Filled.Build to "Build",
                    Icons.Filled.Call to "Call",
                    Icons.Filled.Delete to "Delete",
                    Icons.Filled.Email to "Email",
                    Icons.Filled.Face to "Face",
                )

            var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

            Column(
                modifier = Modifier.testTag(FabMenuTestTag).align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButtonMenu(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    expanded = fabMenuExpanded,
                    button = {
                        ToggleableFloatingActionButton(
                            modifier = Modifier.testTag(ToggleableFabTestTag),
                            checked = fabMenuExpanded,
                            onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                            containerColor = containerColor,
                            containerSize = containerSize,
                            containerCornerRadius = containerCornerRadius,
                        ) {
                            val imageVector by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) Icons.Filled.Close
                                    else Icons.Filled.Add
                                }
                            }
                            Icon(
                                painter = rememberVectorPainter(imageVector),
                                contentDescription = null,
                                modifier =
                                    Modifier.animateIcon({ checkedProgress }, iconColor, iconSize)
                            )
                        }
                    }
                ) {
                    items.forEach { item ->
                        FloatingActionButtonMenuItem(
                            onClick = { fabMenuExpanded = !fabMenuExpanded },
                            icon = { Icon(item.first, contentDescription = null) },
                            text = { Text(text = item.second) },
                            containerColor = itemContainerColor
                        )
                    }
                }
            }
        }
    }
}
