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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class ExpressiveNavigationBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun item_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("ItemText") },
                selected = true,
                onClick = {}
            )
        }

        rule.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun item_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                enabled = false,
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("ItemText") },
                selected = true,
                onClick = {}
            )
        }

        rule.onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun item_disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                enabled = false,
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("ItemText") },
                selected = true,
                onClick = { clicks++ }
            )
        }

        rule.onNodeWithTag("item")
            .performClick()

        rule.runOnIdle {
            Truth.assertThat(clicks).isEqualTo(0)
        }
    }

    @Test
    fun item_iconSemanticsIsNull_whenLabelIsPresent() {
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, "Favorite") },
                label = { Text("Favorite") },
                selected = true,
                onClick = {}
            )
        }

        val node = rule.onNodeWithTag("item").fetchSemanticsNode()

        Truth.assertThat(node.config.getOrNull(SemanticsProperties.ContentDescription)).isNull()
    }

    @Test
    fun item_unselectedItem_hasIconSemantics_whenLabelNotPresent() {
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = { Icon(Icons.Filled.Favorite, "Favorite") },
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
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = {
                    Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                },
                label = { Text("ItemText") },
                selected = true,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // Assert the item has its minimal width and height values.
        Truth.assertThat(itemBounds.width).isAtLeast(NavigationItemMinWidth)
        Truth.assertThat(itemBounds.height).isAtLeast(NavigationItemMinHeight)

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be horizontally centered in the item.
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The top of the icon is `TopIconIndicatorVerticalPadding` below the top of the item.
            .assertTopPositionInRootIsEqualTo(itemBounds.top + TopIconIndicatorVerticalPadding)

        val iconBottom = iconBounds.top + iconBounds.height
        // Text should be `TopIconIndicatorVerticalPadding + TopIconIndicatorToLabelPadding` from
        // the bottom of the icon.
        rule.onNodeWithText("ItemText", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
            .assertIsEqualTo(
                iconBottom + TopIconIndicatorVerticalPadding + TopIconIndicatorToLabelPadding
            )
    }

    @Test
    fun itemContent_startIconPosition_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = {
                    Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                },
                iconPosition = NavigationItemIconPosition.Start,
                label = { Text("ItemText", Modifier.testTag("label")) },
                selected = true,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val labelBounds = rule.onNodeWithTag("label", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // Assert the item has its minimal width and height values.
        Truth.assertThat(itemBounds.width).isAtLeast(NavigationItemMinWidth)
        Truth.assertThat(itemBounds.height).isAtLeast(NavigationItemMinHeight)

        // Assert width.
        val expectedWidth = StartIconIndicatorHorizontalPadding + iconBounds.width +
            StartIconToLabelPadding + labelBounds.width +
            StartIconIndicatorHorizontalPadding
        Truth.assertThat(itemBounds.width.value).isWithin(1f).of(expectedWidth.value)
        // Assert height. Note: The item's content height is less than its minimum touch target
        // height, so its actual height is the same as NavigationItemMinHeight.
        Truth.assertThat(itemBounds.height).isEqualTo(NavigationItemMinHeight)

        rule.onNodeWithTag("icon", useUnmergedTree = true)
            // The left of the icon is `StartIconIndicatorHorizontalPadding` from the left of the
            // item.
            .assertLeftPositionInRootIsEqualTo(
                itemBounds.left + StartIconIndicatorHorizontalPadding
            )
            // The icon should be vertically centered in the item.
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)

        rule.onNodeWithTag("label", useUnmergedTree = true)
            // The left of the label is `StartIconToLabelPadding` from the right of the icon.
            .assertLeftPositionInRootIsEqualTo(
                itemBounds.left + StartIconIndicatorHorizontalPadding + iconBounds.width +
                    StartIconToLabelPadding
            )
            // The label should be vertically centered in the item.
            .assertTopPositionInRootIsEqualTo((itemBounds.height - labelBounds.height) / 2)
    }

    @Test
    fun itemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            ExpressiveNavigationBarItem(
                modifier = Modifier.testTag("item"),
                icon = {
                    Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon"))
                },
                label = null,
                selected = false,
                onClick = {}
            )
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag("icon", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // The icon should be centered in the item, as there is no text placeable provided
        rule.onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            .assertTopPositionInRootIsEqualTo((itemBounds.height - iconBounds.height) / 2)
    }
}
