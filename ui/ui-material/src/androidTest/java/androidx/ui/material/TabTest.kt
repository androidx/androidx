/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.LargeTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.background
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.samples.ScrollingTextTabs
import androidx.ui.material.samples.TextTabs
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.performClick
import androidx.ui.test.onAllNodes
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.getBoundsInRoot
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class TabTest {

    private val ExpectedSmallTabHeight = 48.dp
    private val ExpectedLargeTabHeight = 72.dp

    private val icon = Icons.Filled.Favorite

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun textTab_height() {
        composeTestRule
            .setMaterialContentForSizeAssertions {
                Tab(text = { Text("Text") }, selected = true, onSelected = {})
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun iconTab_height() {
        composeTestRule
            .setMaterialContentForSizeAssertions {
                Tab(icon = { Icon(icon) }, selected = true, onSelected = {})
            }
            .assertHeightIsEqualTo(ExpectedSmallTabHeight)
    }

    @Test
    fun textAndIconTab_height() {
        composeTestRule
            .setMaterialContentForSizeAssertions {
                Surface {
                    Tab(
                        text = { Text("Text and Icon") },
                        icon = { Icon(icon) },
                        selected = true,
                        onSelected = {}
                    )
                }
            }
            .assertHeightIsEqualTo(ExpectedLargeTabHeight)
    }

    @Test
    fun fixedTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB 1", "TAB 2")

            val indicatorContainer = @Composable { tabPositions: List<TabRow.TabPosition> ->
                TabRow.IndicatorContainer(tabPositions, state) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .preferredHeight(indicatorHeight)
                            .background(color = Color.Red)
                        .testTag("indicator")
                    )
                }
            }

            Box(Modifier.testTag("tabRow")) {
                TabRow(
                    items = titles,
                    selectedIndex = state,
                    indicatorContainer = indicatorContainer
                ) { index, text ->
                    Tab(
                        text = { Text(text) },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        val tabRowBounds = onNodeWithTag("tabRow").getBoundsInRoot()

        onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                expectedLeft = 0.dp,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        onAllNodes(isInMutuallyExclusiveGroup())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                expectedLeft = (tabRowBounds.width / 2),
                expectedTop = tabRowBounds.height - indicatorHeight
            )
    }

    @Test
    fun singleLineTab_textBaseline() {
        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    items = titles,
                    selectedIndex = state
                ) { index, text ->
                    Tab(
                        text = {
                            Text(text, Modifier.testTag("text"))
                        },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        val expectedBaseline = 18.dp
        val indicatorHeight = 2.dp
        val expectedBaselineDistance = expectedBaseline + indicatorHeight

        val tabRowBounds = onNodeWithTag("tabRow").getBoundsInRoot()
        val textBounds = onNodeWithTag("text").getBoundsInRoot()
        val textBaselinePos = onNodeWithTag("text").getLastBaselinePosition()

        val baselinePositionY = textBounds.top + textBaselinePos
        val expectedPositionY = tabRowBounds.height - expectedBaselineDistance
        baselinePositionY.assertIsEqualTo(expectedPositionY, "baseline y-position")
    }

    @Test
    fun singleLineTab_withIcon_textBaseline() {
        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    items = titles,
                    selectedIndex = state
                ) { index, text ->
                    Tab(
                        text = {
                            Text(text, Modifier.testTag("text"))
                        },
                        icon = { Icon(Icons.Filled.Favorite) },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        val expectedBaseline = 14.dp
        val indicatorHeight = 2.dp
        val expectedBaselineDistance = expectedBaseline + indicatorHeight

        val tabRowBounds = onNodeWithTag("tabRow").getBoundsInRoot()
        val textBounds = onNodeWithTag("text").getBoundsInRoot()
        val textBaselinePos = onNodeWithTag("text").getLastBaselinePosition()

        val baselinePositionY = textBounds.top + textBaselinePos
        val expectedPositionY = tabRowBounds.height - expectedBaselineDistance
        baselinePositionY.assertIsEqualTo(expectedPositionY, "baseline y-position")
    }

    @Test
    fun twoLineTab_textBaseline() {
        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("Two line \n text")

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    items = titles,
                    selectedIndex = state
                ) { index, text ->
                    Tab(
                        text = {
                            Text(text, Modifier.testTag("text"), maxLines = 2)
                        },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        val expectedBaseline = 10.dp
        val indicatorHeight = 2.dp

        val tabRowBounds = onNodeWithTag("tabRow").getBoundsInRoot()
        val textBounds = onNodeWithTag("text").getBoundsInRoot()
        val textBaselinePos = onNodeWithTag("text").getLastBaselinePosition()

        val expectedBaselineDistance = expectedBaseline + indicatorHeight

        val baselinePositionY = textBounds.top + textBaselinePos
        val expectedPositionY = (tabRowBounds.height - expectedBaselineDistance)
        baselinePositionY.assertIsEqualTo(expectedPositionY, "baseline y-position")
    }

    @Test
    fun scrollableTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp
        val scrollableTabRowOffset = 52.dp
        val minimumTabWidth = 90.dp

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB 1", "TAB 2")

            val indicatorContainer = @Composable { tabPositions: List<TabRow.TabPosition> ->
                TabRow.IndicatorContainer(tabPositions, state) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .preferredHeight(indicatorHeight)
                            .background(color = Color.Red)
                        .testTag("indicator")
                    )
                }
            }

            Box {
                TabRow(
                    modifier = Modifier.testTag("tabRow"),
                    items = titles,
                    scrollable = true,
                    selectedIndex = state,
                    indicatorContainer = indicatorContainer
                ) { index, text ->
                    Tab(
                        text = { Text(text) },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        val tabRowBounds = onNodeWithTag("tabRow").getBoundsInRoot()

        // Indicator should be placed in the bottom left of the first tab
        onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                // Tabs in a scrollable tab row are offset 52.dp from each end
                expectedLeft = scrollableTabRowOffset,
                expectedTop = tabRowBounds.height - indicatorHeight
            )

        // Click the second tab
        onAllNodes(isInMutuallyExclusiveGroup())[1].performClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        onNodeWithTag("indicator")
            .assertPositionInRootIsEqualTo(
                expectedLeft = scrollableTabRowOffset + minimumTabWidth,
                expectedTop = tabRowBounds.height - indicatorHeight
            )
    }

    @Test
    fun fixedTabRow_initialTabSelected() {
        composeTestRule
            .setMaterialContent {
                TextTabs()
            }

        // Only the first tab should be selected
        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsUnselected()
                get(2).assertIsUnselected()
            }
    }

    @Test
    fun fixedTabRow_selectNewTab() {
        composeTestRule
            .setMaterialContent {
                TextTabs()
            }

        // Only the first tab should be selected
        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsUnselected()
                get(2).assertIsUnselected()
            }

        // Click the last tab
        onAllNodes(isInMutuallyExclusiveGroup())[2].performClick()

        // Now only the last tab should be selected
        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsUnselected()
                get(1).assertIsUnselected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun scrollableTabRow_initialTabSelected() {
        composeTestRule
            .setMaterialContent {
                ScrollingTextTabs()
            }

        // Only the first tab should be selected
        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsUnselected()
                }
            }
    }

    @Test
    fun scrollableTabRow_selectNewTab() {
        composeTestRule
            .setMaterialContent {
                ScrollingTextTabs()
            }

        // Only the first tab should be selected
        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsUnselected()
                }
            }

        // Click the second tab
        onAllNodes(isInMutuallyExclusiveGroup())[1].performClick()

        // Now only the second tab should be selected
        onAllNodes(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsUnselected()
                get(1).assertIsSelected()
                (2..9).forEach {
                    get(it).assertIsUnselected()
                }
            }
    }
}
