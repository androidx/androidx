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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class WideNavigationRailTest {
    @get:Rule val rule = createComposeRule()

    private val collapsedWidth = 96.dp // TODO: Replace with token.
    private val expandedMinWidth = 220.dp // TODO: Replace with token.
    private val expandedMaxWidth = 360.dp // TODO: Replace with token.

    @Test
    fun rail_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRail {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule
            .onNodeWithTag("item")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test
    fun rail_collapsed_size() {
        rule
            .setMaterialContentForSizeAssertions {
                WideNavigationRail {
                    repeat(3) { index ->
                        WideNavigationRailItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {}
                        )
                    }
                }
            }
            .assertHeightIsEqualTo(rule.rootHeight())
            .assertWidthIsEqualTo(collapsedWidth)
    }

    @Test
    fun rail_expanded_size() {
        rule
            .setMaterialContentForSizeAssertions {
                WideNavigationRail(expanded = true) {
                    repeat(3) { index ->
                        WideNavigationRailItem(
                            railExpanded = true,
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {}
                        )
                    }
                }
            }
            .assertHeightIsEqualTo(rule.rootHeight())
            .assertWidthIsEqualTo(expandedMinWidth)
    }

    @Test
    fun rail_expanded_maxSize() {
        rule
            .setMaterialContentForSizeAssertions {
                WideNavigationRail(expanded = true, header = { Spacer(Modifier.width(400.dp)) }) {
                    repeat(3) { index ->
                        WideNavigationRailItem(
                            railExpanded = true,
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {}
                        )
                    }
                }
            }
            .assertHeightIsEqualTo(rule.rootHeight())
            .assertWidthIsEqualTo(expandedMaxWidth)
    }

    @Test
    fun rail_collapsed_expands() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            WideNavigationRail(
                modifier = Modifier.testTag("rail"),
                expanded = expanded,
                header = {
                    Button(
                        modifier = Modifier.testTag("header"),
                        onClick = { expanded = !expanded }
                    ) {}
                }
            ) {}
        }

        // Assert width is collapsed width.
        rule.onNodeWithTag("rail").assertWidthIsEqualTo(collapsedWidth)
        // Click on header to expand.
        rule.onNodeWithTag("header").performClick()
        // Assert width changed to expanded width.
        rule.onNodeWithTag("rail").assertWidthIsEqualTo(expandedMinWidth)
    }

    @Test
    fun rail_expanded_collapses() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            WideNavigationRail(
                modifier = Modifier.testTag("rail"),
                expanded = expanded,
                header = {
                    Button(
                        modifier = Modifier.testTag("header"),
                        onClick = { expanded = !expanded }
                    ) {}
                }
            ) {}
        }

        // Assert width is expanded width.
        rule.onNodeWithTag("rail").assertWidthIsEqualTo(expandedMinWidth)
        // Click on header to collapse.
        rule.onNodeWithTag("header").performClick()
        // Assert width changed to collapse width.
        rule.onNodeWithTag("rail").assertWidthIsEqualTo(collapsedWidth)
    }

    @Test
    fun rail_respectsWindowInsets() {
        rule.setMaterialContentForSizeAssertions {
            WideNavigationRail(windowInsets = WindowInsets(13.dp, 13.dp, 13.dp, 13.dp)) {
                Box(Modifier.fillMaxSize().testTag("content"))
            }
        }
        rule
            .onNodeWithTag("content")
            .assertTopPositionInRootIsEqualTo(13.dp + WNRVerticalPadding)
            .assertLeftPositionInRootIsEqualTo(13.dp)
    }

    @Test
    fun rail_itemsWithCustomColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val customItemColors =
                WideNavigationRailItemDefaults.colors()
                    .copy(
                        selectedIconColor = Color.Red,
                        selectedTextColor = Color.Blue,
                        unselectedIconColor = Color.Green,
                        unselectedTextColor = Color.White,
                        disabledIconColor = Color.Gray,
                        disabledTextColor = Color.Black,
                    )

            WideNavigationRail {
                WideNavigationRailItem(
                    selected = true,
                    colors = customItemColors,
                    icon = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Red) },
                    label = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Blue) },
                    onClick = {}
                )
                WideNavigationRailItem(
                    selected = false,
                    colors = customItemColors,
                    icon = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Green) },
                    label = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.White) },
                    onClick = {}
                )
                WideNavigationRailItem(
                    enabled = false,
                    selected = false,
                    colors = customItemColors,
                    icon = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Gray) },
                    label = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Black) },
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun rail_selectNewItem() {
        rule.setMaterialContent(lightColorScheme()) {
            var selectedItem by remember { mutableStateOf(0) }
            WideNavigationRail {
                repeat(3) { index ->
                    WideNavigationRailItem(
                        icon = { Icon(Icons.Filled.Favorite, null) },
                        label = { Text("Item $index") },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }

        // Find all items and ensure there are 3
        rule
            .onAllNodes(isSelectable())
            .assertCountEquals(3)
            // Ensure semantics match for selected state of the items.
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }
            // Click the last item.
            .apply { get(2).performClick() }
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun item_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRailItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("ItemText") },
                selected = true,
                onClick = {}
            )
        }

        rule
            .onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun item_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRailItem(
                enabled = false,
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("ItemText") },
                selected = true,
                onClick = {}
            )
        }

        rule
            .onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun item_unselectedItem_hasIconSemantics_whenLabelNotPresent() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRailItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, "Favorite") },
                label = null,
                selected = false,
                onClick = {}
            )
        }

        val node = rule.onNodeWithTag("item").fetchSemanticsNode()

        Truth.assertThat(node.config.getOrNull(SemanticsProperties.ContentDescription))
            .isEqualTo(listOf("Favorite"))
    }

    @Test
    fun item_disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRailItem(
                enabled = false,
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("ItemText") },
                selected = true,
                onClick = { clicks++ }
            )
        }

        rule.onNodeWithTag("item").performClick()

        rule.runOnIdle { Truth.assertThat(clicks).isEqualTo(0) }
    }

    @Test
    fun item_topIconPosition_withLongLabel_automaticallyResizesHeight() {
        val defaultHeight = WNRTopIconItemMinHeight

        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRailItem(
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("Long\nLabel\nMultiple\nLines") },
                selected = true,
                onClick = {},
                modifier = Modifier.testTag("TAG"),
            )
        }

        Truth.assertThat(
                rule.onNodeWithTag("TAG", useUnmergedTree = true).getUnclippedBoundsInRoot().height
            )
            .isGreaterThan(defaultHeight)
    }

    @Test
    fun item_startIconPosition_withLongLabel_automaticallyResizesHeight() {
        var defaultHeight: Dp? = null

        rule.setMaterialContent(lightColorScheme()) {
            defaultHeight = LocalMinimumInteractiveComponentSize.current
            WideNavigationRailItem(
                iconPosition = NavigationItemIconPosition.Start,
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("Long\nLabel\nMultiple\nLines") },
                selected = true,
                onClick = {},
                modifier = Modifier.testTag("TAG"),
            )
        }

        Truth.assertThat(
                rule.onNodeWithTag("TAG", useUnmergedTree = true).getUnclippedBoundsInRoot().height
            )
            .isGreaterThan(defaultHeight)
    }

    @Test
    fun itemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRailItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                label = null,
                selected = true,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()

        val itemSize =
            WNRItemNoLabelIndicatorPadding + iconBounds.height + WNRItemNoLabelIndicatorPadding

        // Assert the item has its minimal width and height values.
        Truth.assertThat(itemBounds.width).isEqualTo(itemSize)
        Truth.assertThat(itemBounds.height).isEqualTo(itemSize)

        // The icon should be centered in the item, as there is no text placeable provided
        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }

    @Test
    fun header_position() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRail(header = { Box(Modifier.testTag("header").size(10.dp)) }) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        val headerBounds = rule.onNodeWithTag("header").getUnclippedBoundsInRoot()

        // Header should always be at the top.
        rule
            .onNodeWithTag("header", useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(WNRVerticalPadding)
            .assertLeftPositionInRootIsEqualTo(0.dp)

        // Item should be `HeaderPadding` below the header in top arrangement.
        rule
            .onNodeWithTag("item", useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(
                WNRVerticalPadding + headerBounds.height + WNRHeaderPadding
            )
    }

    @Test
    fun header_position_centeredArrangement() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRail(
                modifier = Modifier.testTag("rail"),
                arrangement = WideNavigationRailArrangement.Center,
                header = { Box(Modifier.testTag("header").size(10.dp)) }
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        val railBounds = rule.onNodeWithTag("rail").getUnclippedBoundsInRoot()
        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()

        // Header should always be at the top.
        rule
            .onNodeWithTag("header", useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(WNRVerticalPadding)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        // Assert item is centered.
        rule
            .onNodeWithTag("item", useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo((railBounds.height - itemBounds.height) / 2)
    }

    @Test
    fun header_position_bottomArrangement() {
        rule.setMaterialContent(lightColorScheme()) {
            WideNavigationRail(
                modifier = Modifier.testTag("rail"),
                arrangement = WideNavigationRailArrangement.Bottom,
                header = { Box(Modifier.testTag("header").size(10.dp)) }
            ) {
                WideNavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        val railBounds = rule.onNodeWithTag("rail").getUnclippedBoundsInRoot()
        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()

        // Header should always be at the top.
        rule
            .onNodeWithTag("header", useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(WNRVerticalPadding)
            .assertLeftPositionInRootIsEqualTo(0.dp)
        // Assert item is at the bottom.
        rule
            .onNodeWithTag("item", useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(
                (railBounds.height - WNRVerticalPadding - itemBounds.height)
            )
    }
}
