/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for the Material Menus.
 *
 * Note that currently nodes in a popup cannot be captured to bitmaps. A [DropdownMenu] is
 * displaying its content in a popup, so the tests here focus on the [DropdownMenuContent].
 */
// TODO(b/208991956): Update to include DropdownMenu when popups can be captured into bitmaps.
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class MenuScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val testTag = "dropdown_menu"

    @Test
    fun dropdownMenu_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) { TestMenu(enabledItems = true) }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_lightTheme")
    }

    @Test
    fun dropdownMenu_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) { TestMenu(enabledItems = true) }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_darkTheme")
    }

    @Test
    fun segmentedDropdownMenu_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) { TestSegmentedMenu() }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_lightTheme")
    }

    @Test
    fun segmentedDropdownMenu_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) { TestSegmentedMenu() }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_darkTheme")
    }

    @Test
    fun dropdownMenu_disabled_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) { TestMenu(enabledItems = false) }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_disabled_lightTheme")
    }

    @Test
    fun dropdownMenu_disabled_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) { TestMenu(enabledItems = false) }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_disabled_darkTheme")
    }

    @Test
    fun segmentedDropdownMenu_disabled_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            TestSegmentedMenu(enabledItems = false)
        }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_disabled_lightTheme")
    }

    @Test
    fun segmentedDropdownMenu_disabled_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            TestSegmentedMenu(enabledItems = false)
        }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_disabled_darkTheme")
    }

    @Test
    fun dropdownMenu_customAppearance() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            TestMenu(
                enabledItems = true,
                shape = CutCornerShape(12.dp),
                containerColor = Color.Yellow,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.Black),
            )
        }
        assertAgainstGolden(goldenIdentifier = "dropdownMenu_customAppearance")
    }

    @Test
    fun segmentedDropdownMenu_toggledItems() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            TestSegmentedMenu(
                editChecked = true,
                settingChecked = true,
                homeChecked = true,
                moreOptionChecked = true,
            )
        }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_toggledItems")
    }

    @Test
    fun segmentedDropdownMenu_rtl() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TestSegmentedMenu()
            }
        }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_lightTheme_rtl")
    }

    @Test
    fun segmentedDropdownMenu_toggledItems_rtl() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TestSegmentedMenu(
                    editChecked = true,
                    settingChecked = true,
                    homeChecked = true,
                    moreOptionChecked = true,
                )
            }
        }
        assertAgainstGolden(goldenIdentifier = "segmentedDropdownMenu_lightTheme_toggledItems_rtl")
    }

    @Composable
    private fun TestMenu(
        enabledItems: Boolean,
        shape: Shape = MenuDefaults.shape,
        containerColor: Color = MenuDefaults.containerColor,
        tonalElevation: Dp = MenuDefaults.TonalElevation,
        shadowElevation: Dp = MenuDefaults.ShadowElevation,
        border: BorderStroke? = null,
    ) {
        Box(Modifier.testTag(testTag).padding(20.dp), contentAlignment = Alignment.Center) {
            DropdownMenuContent(
                modifier = Modifier,
                expandedState = MutableTransitionState(initialState = true),
                transformOriginState = remember { mutableStateOf(TransformOrigin.Center) },
                scrollState = rememberScrollState(),
                shape = shape,
                containerColor = containerColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
                border = border,
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {},
                    enabled = enabledItems,
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {},
                    enabled = enabledItems,
                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    trailingIcon = { Text("F11", textAlign = TextAlign.Center) },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Send Feedback") },
                    onClick = {},
                    enabled = enabledItems,
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun TestSegmentedMenu(
        enabledItems: Boolean = true,
        editChecked: Boolean = false,
        settingChecked: Boolean = false,
        homeChecked: Boolean = false,
        moreOptionChecked: Boolean = false,
    ) {
        Box(Modifier.testTag(testTag).padding(20.dp), contentAlignment = Alignment.Center) {
            DropdownMenuPopupContent(
                Modifier,
                expandedState = MutableTransitionState(initialState = true),
                transformOriginState = remember { mutableStateOf(TransformOrigin.Center) },
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes(shape = MenuDefaults.leadingGroupShape)
                ) {
                    DropdownMenuItem(
                        checked = editChecked,
                        onCheckedChange = {},
                        enabled = enabledItems,
                        text = { Text("Edit") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Edit,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        checkedLeadingIcon = {
                            Icon(
                                Icons.Filled.Edit,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        shapes = MenuDefaults.itemShapes(MenuDefaults.leadingItemShape),
                    )
                    DropdownMenuItem(
                        checked = settingChecked,
                        onCheckedChange = {},
                        enabled = enabledItems,
                        text = { Text("Settings") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Settings,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        checkedLeadingIcon = {
                            Icon(
                                Icons.Filled.Settings,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        shapes = MenuDefaults.itemShapes(MenuDefaults.trailingItemShape),
                    )
                }
                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes(shape = MenuDefaults.trailingGroupShape)
                ) {
                    MenuDefaults.Label { Text("Group Label") }
                    HorizontalDivider(
                        modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
                    )
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShapes(shape = MenuDefaults.leadingItemShape),
                        text = { Text("Home") },
                        checked = homeChecked,
                        enabled = enabledItems,
                        onCheckedChange = {},
                        checkedLeadingIcon = {
                            Icon(
                                Icons.Filled.Check,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (homeChecked) {
                                Icon(
                                    Icons.Filled.Home,
                                    modifier = Modifier.size(MenuDefaults.TrailingIconSize),
                                    contentDescription = null,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Home,
                                    modifier = Modifier.size(MenuDefaults.TrailingIconSize),
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("More Options") },
                        checked = moreOptionChecked,
                        enabled = enabledItems,
                        onCheckedChange = {},
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Info,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        checkedLeadingIcon = {
                            Icon(
                                Icons.Filled.Info,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.MoreVert,
                                modifier = Modifier.size(MenuDefaults.TrailingIconSize),
                                contentDescription = null,
                            )
                        },
                        shapes = MenuDefaults.itemShapes(MenuDefaults.trailingItemShape),
                    )
                }
            }
        }
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        composeTestRule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
