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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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
class ShortNavigationBarTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun bar_selectableGroupSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBar {
                ShortNavigationBarItem(
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
    fun bar_size() {
        val height = 64.dp // TODO: Replace with token.
        rule
            .setMaterialContentForSizeAssertions {
                ShortNavigationBar {
                    repeat(3) { index ->
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {}
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(rule.rootWidth())
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun bar_respectContentPadding() {
        rule.setMaterialContentForSizeAssertions {
            ShortNavigationBar(windowInsets = WindowInsets(17.dp, 17.dp, 17.dp, 17.dp)) {
                Box(Modifier.fillMaxSize().testTag("content"))
            }
        }
        rule
            .onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(17.dp)
            .assertTopPositionInRootIsEqualTo(17.dp)
    }

    @Test
    fun bar_equalWeightArrangement_sizeAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent(
            lightColorScheme(),
            Modifier.onGloballyPositioned { coords: LayoutCoordinates -> parentCoords = coords }
        ) {
            Box {
                ShortNavigationBar {
                    repeat(4) { index ->
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            val width = parentCoords.size.width
            val expectedItemWidth = (width / 4f)
            val expectedItemHeight = 64.dp.toPx() // TODO: Replace with token.

            Truth.assertThat(itemCoords.size).isEqualTo(4)
            itemCoords.forEach { (index, coord) ->
                Truth.assertThat(coord.size.width.toFloat()).isWithin(1f).of(expectedItemWidth)
                Truth.assertThat(coord.size.height.toFloat()).isWithin(1f).of(expectedItemHeight)
                Truth.assertThat(coord.positionInWindow().x)
                    .isWithin(1f)
                    .of(expectedItemWidth * index)
            }
        }
    }

    @Test
    fun bar_equalWeightArrangement_topIconItemWithLongLabel_automaticallyResizesHeight() {
        val defaultHeight = 64.dp // TODO: Replace with token.

        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBar(modifier = Modifier.testTag("TAG")) {
                repeat(4) { index ->
                    ShortNavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, null) },
                        label = { Text("Long\nLabel\nMultiple\nLines") },
                        selected = index == 0,
                        onClick = {},
                    )
                }
            }
        }

        Truth.assertThat(rule.onNodeWithTag("TAG").getUnclippedBoundsInRoot().height)
            .isGreaterThan(defaultHeight)
    }

    @Test
    fun bar_centeredArrangement_threeItems_widthAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setContentWithSimulatedSize(
            1000.dp,
            200.dp,
            lightColorScheme(),
            Modifier.onGloballyPositioned { coords: LayoutCoordinates -> parentCoords = coords }
        ) {
            Box {
                ShortNavigationBar(arrangement = ShortNavigationBarArrangement.Centered) {
                    repeat(3) { index ->
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            iconPosition = NavigationItemIconPosition.Start,
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            assertWidthAndPositions(
                paddingPercentage = .2f,
                itemsSpacePercentage = .6f,
                numberOfItems = 3,
                parentCoords = parentCoords,
                itemCoords = itemCoords
            )
        }
    }

    @Test
    fun bar_centeredArrangement_fourItems_widthAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setContentWithSimulatedSize(
            1000.dp,
            200.dp,
            lightColorScheme(),
            Modifier.onGloballyPositioned { coords: LayoutCoordinates -> parentCoords = coords }
        ) {
            Box {
                ShortNavigationBar(arrangement = ShortNavigationBarArrangement.Centered) {
                    repeat(4) { index ->
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            iconPosition = NavigationItemIconPosition.Start,
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            assertWidthAndPositions(
                paddingPercentage = .15f,
                itemsSpacePercentage = .7f,
                numberOfItems = 4,
                parentCoords = parentCoords,
                itemCoords = itemCoords
            )
        }
    }

    @Test
    fun bar_centeredArrangement_fiveItems_widthAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setContentWithSimulatedSize(
            1000.dp,
            200.dp,
            lightColorScheme(),
            Modifier.onGloballyPositioned { coords: LayoutCoordinates -> parentCoords = coords }
        ) {
            Box {
                ShortNavigationBar(arrangement = ShortNavigationBarArrangement.Centered) {
                    repeat(5) { index ->
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            iconPosition = NavigationItemIconPosition.Start,
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            assertWidthAndPositions(
                paddingPercentage = .1f,
                itemsSpacePercentage = .8f,
                numberOfItems = 5,
                parentCoords = parentCoords,
                itemCoords = itemCoords
            )
        }
    }

    @Test
    fun bar_centeredArrangement_sixItems_widthAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setContentWithSimulatedSize(
            1000.dp,
            200.dp,
            lightColorScheme(),
            Modifier.onGloballyPositioned { coords: LayoutCoordinates -> parentCoords = coords }
        ) {
            Box {
                ShortNavigationBar(arrangement = ShortNavigationBarArrangement.Centered) {
                    repeat(6) { index ->
                        ShortNavigationBarItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            iconPosition = NavigationItemIconPosition.Start,
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            assertWidthAndPositions(
                paddingPercentage = .05f,
                itemsSpacePercentage = .9f,
                numberOfItems = 6,
                parentCoords = parentCoords,
                itemCoords = itemCoords
            )
        }
    }

    @Test
    fun bar_centeredArrangement_startIconItemWithLongLabel_automaticallyResizesHeight() {
        val defaultHeight = 64.dp // TODO: Replace with token.

        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBar(
                modifier = Modifier.testTag("TAG"),
                arrangement = ShortNavigationBarArrangement.Centered
            ) {
                repeat(4) { index ->
                    ShortNavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, null) },
                        iconPosition = NavigationItemIconPosition.Start,
                        label = { Text("Long\nLabel\nMultiple\nLines") },
                        selected = index == 0,
                        onClick = {},
                    )
                }
            }
        }

        Truth.assertThat(rule.onNodeWithTag("TAG").getUnclippedBoundsInRoot().height)
            .isGreaterThan(defaultHeight)
    }

    @Test
    fun bar_itemsWithCustomColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val customItemColors =
                ShortNavigationBarItemDefaults.colors()
                    .copy(
                        selectedIconColor = Color.Red,
                        selectedTextColor = Color.Blue,
                        unselectedIconColor = Color.Green,
                        unselectedTextColor = Color.White,
                        disabledIconColor = Color.Gray,
                        disabledTextColor = Color.Black,
                    )

            ShortNavigationBar {
                ShortNavigationBarItem(
                    selected = true,
                    colors = customItemColors,
                    icon = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Red) },
                    label = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Blue) },
                    onClick = {}
                )
                ShortNavigationBarItem(
                    selected = false,
                    colors = customItemColors,
                    icon = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.Green) },
                    label = { Truth.assertThat(LocalContentColor.current).isEqualTo(Color.White) },
                    onClick = {}
                )
                ShortNavigationBarItem(
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
    fun item_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBarItem(
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
            ShortNavigationBarItem(
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
    fun item_disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBarItem(
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
    fun item_unselectedItem_hasIconSemantics_whenLabelNotPresent() {
        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBarItem(
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
    fun itemContent_topIconPosition_sizeAndPosition() {
        var minSize: Dp? = null
        rule.setMaterialContent(lightColorScheme()) {
            minSize = LocalMinimumInteractiveComponentSize.current
            ShortNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                label = { Text("ItemText") },
                selected = true,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // Assert the item has its minimal width and height values.
        Truth.assertThat(itemBounds.width).isAtLeast(minSize)
        Truth.assertThat(itemBounds.height).isAtLeast(minSize)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be horizontally centered in the item.
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The top of the icon is `TopIconIndicatorVerticalPadding + TopIconItemVerticalPadding`
            // below the top of the item.
            .assertTopPositionInRootIsEqualTo(
                itemBounds.top + TopIconIndicatorVerticalPadding + TopIconItemVerticalPadding
            )

        val iconBottom = iconBounds.top + iconBounds.height
        // Text should be `TopIconIndicatorVerticalPadding + TopIconIndicatorToLabelPadding` from
        // the bottom of the icon.
        rule
            .onNodeWithText("ItemText", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .assertIsEqualTo(
                iconBottom + TopIconIndicatorVerticalPadding + TopIconIndicatorToLabelPadding
            )
    }

    @Test
    fun itemContent_startIconPosition_sizeAndPosition() {
        var minSize: Dp? = null
        rule.setMaterialContent(lightColorScheme()) {
            minSize = LocalMinimumInteractiveComponentSize.current
            ShortNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                iconPosition = NavigationItemIconPosition.Start,
                label = { Text("ItemText", Modifier.testTag("label")) },
                selected = true,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val labelBounds =
            rule.onNodeWithTag("label", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // Assert the item has its minimal width and height values.
        Truth.assertThat(itemBounds.width).isAtLeast(minSize)
        Truth.assertThat(itemBounds.height).isAtLeast(minSize)

        // Assert width.
        val expectedWidth =
            StartIconIndicatorHorizontalPadding +
                iconBounds.width +
                StartIconToLabelPadding +
                labelBounds.width +
                StartIconIndicatorHorizontalPadding
        Truth.assertThat(itemBounds.width.value).isWithin(1f).of(expectedWidth.value)
        // Assert height. Note: The item's content height is less than its minimum touch target
        // height, so its actual height is the same as LocalMinimumInteractiveComponentSize.
        Truth.assertThat(itemBounds.height).isEqualTo(minSize)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            // The left of the icon is `StartIconIndicatorHorizontalPadding` from the left of the
            // item.
            .assertLeftPositionInRootIsEqualTo(
                itemBounds.left + StartIconIndicatorHorizontalPadding
            )
            // The icon should be vertically centered in the item.
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)

        rule
            .onNodeWithTag("label", useUnmergedTree = true)
            // The left of the label is `StartIconToLabelPadding` from the right of the icon.
            .assertLeftPositionInRootIsEqualTo(
                itemBounds.left +
                    StartIconIndicatorHorizontalPadding +
                    iconBounds.width +
                    StartIconToLabelPadding
            )
            // The label should be vertically centered in the item.
            .assertTopPositionInRootIsEqualTo((itemBounds.height - labelBounds.height) / 2)
    }

    @Test
    fun itemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            ShortNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                label = null,
                selected = false,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // The icon should be centered in the item, as there is no text placeable provided
        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }
}

private fun Density.assertWidthAndPositions(
    paddingPercentage: Float,
    itemsSpacePercentage: Float,
    numberOfItems: Int,
    parentCoords: LayoutCoordinates,
    itemCoords: Map<Int, LayoutCoordinates>
) {
    val width = parentCoords.size.width
    val padding = width * paddingPercentage
    val expectedItemWidth = (width * itemsSpacePercentage / numberOfItems).toInt()

    Truth.assertThat(itemCoords.size).isEqualTo(numberOfItems)
    itemCoords.forEach { (index, coord) ->
        Truth.assertThat(coord.size.width.toFloat()).isWithin(1f).of(expectedItemWidth.toFloat())
        Truth.assertThat(coord.positionInWindow().x)
            .isWithin(1f)
            .of((expectedItemWidth * index) + padding)
    }
}

private fun ComposeContentTestRule.setContentWithSimulatedSize(
    simulatedWidth: Dp,
    simulatedHeight: Dp,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    setContent {
        val currentDensity = LocalDensity.current
        val currentConfiguration = LocalConfiguration.current
        val simulatedDensity =
            Density(
                currentDensity.density * (currentConfiguration.screenWidthDp.dp / simulatedWidth)
            )
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = modifier) {
                CompositionLocalProvider(LocalDensity provides simulatedDensity) {
                    Box(
                        Modifier.fillMaxWidth().height(simulatedHeight),
                    ) {
                        composable()
                    }
                }
            }
        }
    }
}
