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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.NavigationBarTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/**
 * Test for [NavigationBar] and [NavigationBarItem].
 */
class NavigationBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar {
                NavigationBarItem(
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null)
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()

        rule.onNodeWithTag("item")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar {
                NavigationBarItem(
                    enabled = false,
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null)
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun navigationBarItem_clearsIconSemantics_whenLabelIsPresent() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar {
                NavigationBarItem(
                    modifier = Modifier.testTag("item1"),
                    icon = {
                        Icon(Icons.Filled.Favorite, "Favorite")
                    },
                    label = {
                        Text("Favorite")
                    },
                    selected = true,
                    alwaysShowLabel = false,
                    onClick = {}
                )
                NavigationBarItem(
                    modifier = Modifier.testTag("item2"),
                    icon = {
                        Icon(Icons.Filled.Favorite, "Favorite")
                    },
                    label = {
                        Text("Favorite")
                    },
                    selected = false,
                    alwaysShowLabel = false,
                    onClick = {}
                )
                NavigationBarItem(
                    modifier = Modifier.testTag("item3"),
                    icon = {
                        Icon(Icons.Filled.Favorite, "Favorite")
                    },
                    selected = false,
                    onClick = {}
                )
            }
        }

        val node1 = rule.onNodeWithTag("item1").fetchSemanticsNode()
        val node2 = rule.onNodeWithTag("item2").fetchSemanticsNode()
        val node3 = rule.onNodeWithTag("item3").fetchSemanticsNode()

        assertThat(node1.config.getOrNull(SemanticsProperties.ContentDescription)).isNull()
        assertThat(node2.config.getOrNull(SemanticsProperties.ContentDescription))
            .isEqualTo(listOf("Favorite"))
        assertThat(node3.config.getOrNull(SemanticsProperties.ContentDescription))
            .isEqualTo(listOf("Favorite"))
    }

    @Test
    fun navigationBar_size() {
        val height = NavigationBarTokens.ContainerHeight
        rule.setMaterialContentForSizeAssertions {
            val items = listOf("Songs", "Artists", "Playlists")
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text(item) },
                        selected = index == 0,
                        onClick = { /* do something */ }
                    )
                }
            }
        }
            .assertWidthIsEqualTo(rule.rootWidth())
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun navigationBar_respectContentPadding() {
        rule.setMaterialContentForSizeAssertions {
            NavigationBar(windowInsets = WindowInsets(17.dp, 17.dp, 17.dp, 17.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("content")
                )
            }
        }
        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(17.dp)
            .assertTopPositionInRootIsEqualTo(17.dp)
    }

    @Test
    fun navigationBarItem_sizeAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent(
            lightColorScheme(),
            Modifier.onGloballyPositioned { coords: LayoutCoordinates ->
                parentCoords = coords
            }
        ) {
            Box {
                NavigationBar {
                    repeat(4) { index ->
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned { coords ->
                                itemCoords[index] = coords
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            val totalWidth = parentCoords.size.width
            val availableWidth =
                totalWidth.toFloat() - (NavigationBarItemHorizontalPadding.toPx() * 3)

            val expectedItemWidth = (availableWidth / 4)
            val expectedItemHeight = NavigationBarTokens.ContainerHeight.toPx()

            assertThat(itemCoords.size).isEqualTo(4)

            itemCoords.forEach { (index, coord) ->
                // Rounding differences for width can occur on smaller screens
                assertThat(coord.size.width.toFloat()).isWithin(1f).of(expectedItemWidth)
                assertThat(coord.size.height.toFloat()).isWithin(1f).of(expectedItemHeight)
                assertThat(coord.positionInWindow().x).isWithin(1f)
                    .of((expectedItemWidth + NavigationBarItemHorizontalPadding.toPx()) * index)
            }
        }
    }

    @Test
    fun navigationBarItem_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(NavigationBarTokens.ActiveIconColor.value)
                    },
                    label = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(NavigationBarTokens.ActiveLabelTextColor.value)
                    },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(NavigationBarTokens.InactiveIconColor.value)
                    },
                    label = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(NavigationBarTokens.InactiveLabelTextColor.value)
                    },
                    selected = false,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun navigationBarItem_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val customNavigationBarItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                unselectedTextColor = Color.Green,
            )

            NavigationBar {
                NavigationBarItem(
                    colors = customNavigationBarItemColors,
                    icon = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(Color.Red)
                    },
                    label = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(NavigationBarTokens.ActiveLabelTextColor.value)
                    },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    colors = customNavigationBarItemColors,
                    icon = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(NavigationBarTokens.InactiveIconColor.value)
                    },
                    label = {
                        assertThat(LocalContentColor.current)
                            .isEqualTo(Color.Green)
                    },
                    selected = false,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun navigationBarItem_withLongLabel_automaticallyResizesHeight() {
        val defaultHeight = NavigationBarTokens.ContainerHeight

        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar(modifier = Modifier.testTag("TAG")) {
                repeat(4) { index ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, null) },
                        label = { Text("Long\nLabel\nMultiple\nLines") },
                        selected = index == 0,
                        onClick = {},
                    )
                }
            }
        }

        assertThat(rule.onNodeWithTag("TAG").getUnclippedBoundsInRoot().height)
            .isGreaterThan(defaultHeight)
    }

    @Test
    fun navigationBarItemContent_withLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar {
                NavigationBarItem(
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = {}
                )
            }
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // Distance from the top of the item to the top of the icon for the default height
        val verticalPadding = 16.dp

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be horizontally centered in the item
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The top of the icon is `verticalPadding` below the top of the item
            .assertTopPositionInRootIsEqualTo(itemBounds.top + verticalPadding)

        val iconBottom = iconBounds.top + iconBounds.height
        // Text should be `IndicatorVerticalPadding + NavigationBarIndicatorToLabelPadding` from the
        // bottom of the icon
        rule.onNodeWithText("ItemText", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .assertIsEqualTo(
                iconBottom + IndicatorVerticalPadding + NavigationBarIndicatorToLabelPadding
            )
    }

    @Test
    fun navigationBarItemContent_withLabel_unselected_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                NavigationBar {
                    NavigationBarItem(
                        modifier = Modifier.testTag("item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                        },
                        label = {
                            Text("ItemText")
                        },
                        selected = false,
                        onClick = {},
                        alwaysShowLabel = false
                    )
                }
            }
        }

        // The text should not be placed, since the item is not selected and alwaysShowLabels
        // is false
        rule.onNodeWithText("ItemText", useUnmergedTree = true).assertIsNotDisplayed()

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun navigationBarItemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                NavigationBar {
                    NavigationBarItem(
                        modifier = Modifier.testTag("item"),
                        icon = {
                            Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                        },
                        label = null,
                        selected = false,
                        onClick = {}
                    )
                }
            }
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // The icon should be centered in the item, as there is no text placeable provided
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun navigationBarItemContent_customHeight_withLabel_sizeAndPosition() {
        val defaultHeight = NavigationBarTokens.ContainerHeight
        val customHeight = 64.dp

        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar(Modifier.height(customHeight)) {
                NavigationBarItem(
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                    },
                    label = { Text("Label") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        // Vertical padding is removed symmetrically from top and bottom for smaller heights
        val verticalPadding = 16.dp - (defaultHeight - customHeight) / 2

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be horizontally centered in the item
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The top of the icon is `verticalPadding` below the top of the item
            .assertTopPositionInRootIsEqualTo(itemBounds.top + verticalPadding)

        val iconBottom = iconBounds.top + iconBounds.height
        // Text should be `IndicatorVerticalPadding + NavigationBarIndicatorToLabelPadding` from the
        // bottom of the item
        rule.onNodeWithText("Label", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .assertIsEqualTo(
                iconBottom + IndicatorVerticalPadding + NavigationBarIndicatorToLabelPadding
            )
    }

    @Test
    fun navigationBar_selectNewItem() {
        rule.setMaterialContent(lightColorScheme()) {
            var selectedItem by remember { mutableStateOf(0) }
            val items = listOf("Songs", "Artists", "Playlists")

            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }

        // Find all items and ensure there are 3
        rule.onAllNodes(isSelectable())
            .assertCountEquals(3)
            // Ensure semantics match for selected state of the items
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }
            // Click the last item
            .apply {
                get(2).performClick()
            }
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            NavigationBar {
                NavigationBarItem(
                    enabled = false,
                    modifier = Modifier.testTag("item"),
                    icon = {
                        Icon(Icons.Filled.Favorite, null)
                    },
                    label = {
                        Text("ItemText")
                    },
                    selected = true,
                    onClick = { clicks++ }
                )
            }
        }

        rule.onNodeWithTag("item")
            .performClick()

        rule.runOnIdle {
            Truth.assertThat(clicks).isEqualTo(0)
        }
    }
}
