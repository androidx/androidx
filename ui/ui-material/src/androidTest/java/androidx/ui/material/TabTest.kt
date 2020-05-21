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
import androidx.ui.text.LastBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredWidth
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.samples.ScrollingTextTabs
import androidx.ui.material.samples.TextTabs
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findAll
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.toPx
import com.google.common.truth.Truth.assertThat
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
            .setMaterialContentAndCollectSizes {
                Box {
                    Tab(text = { Text("Text") }, selected = true, onSelected = {})
                }
            }
            .assertHeightEqualsTo(ExpectedSmallTabHeight)
    }

    @Test
    fun iconTab_height() {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Box {
                    Tab(icon = { Icon(icon) }, selected = true, onSelected = {})
                }
            }
            .assertHeightEqualsTo(ExpectedSmallTabHeight)
    }

    @Test
    fun textAndIconTab_height() {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Box {
                    Surface {
                        Tab(
                            text = { Text("Text and Icon") },
                            icon = { Icon(icon) },
                            selected = true,
                            onSelected = {}
                        )
                    }
                }
            }
            .assertHeightEqualsTo(ExpectedLargeTabHeight)
    }

    @Test
    fun fixedTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp
        lateinit var tabRowCoords: LayoutCoordinates
        lateinit var indicatorCoords: LayoutCoordinates

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB 1", "TAB 2")

            val indicatorContainer = @Composable { tabPositions: List<TabRow.TabPosition> ->
                TabRow.IndicatorContainer(tabPositions, state) {
                    Box(Modifier
                        .onPositioned { indicatorCoords = it }
                        .fillMaxWidth()
                        .preferredHeight(indicatorHeight)
                        .drawBackground(Color.Red)
                    )
                }
            }

            Box(Modifier.onChildPositioned { tabRowCoords = it }) {
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

        val (tabRowWidth, tabRowHeight) = composeTestRule.runOnIdleComposeWithDensity {
            val tabRowWidth = tabRowCoords.size.width
            val tabRowHeight = tabRowCoords.size.height

            val indicatorPositionX = indicatorCoords.localToGlobal(PxPosition.Origin).x
            val expectedPositionX = 0.dp.toPx()
            assertThat(indicatorPositionX).isEqualTo(expectedPositionX)

            val indicatorPositionY = indicatorCoords.localToGlobal(PxPosition.Origin).y
            val expectedPositionY = (tabRowHeight - indicatorHeight.toIntPx()).value.toFloat()
            assertThat(indicatorPositionY).isEqualTo(expectedPositionY)

            tabRowWidth to tabRowHeight
        }

        // Click the second tab
        findAll(isInMutuallyExclusiveGroup())[1].doClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        runOnIdleCompose {
            with(composeTestRule.density) {
                val indicatorPositionX = indicatorCoords.localToGlobal(PxPosition.Origin).x
                val expectedPositionX = (tabRowWidth / 2).value.toFloat()
                assertThat(indicatorPositionX).isEqualTo(expectedPositionX)

                val indicatorPositionY = indicatorCoords.localToGlobal(PxPosition.Origin).y
                val expectedPositionY =
                    (tabRowHeight - indicatorHeight.toIntPx()).value.toFloat()
                assertThat(indicatorPositionY).isEqualTo(expectedPositionY)
            }
        }
    }

    @Test
    fun singleLineTab_textBaseline() {
        lateinit var tabRowCoords: LayoutCoordinates
        lateinit var textCoords: LayoutCoordinates
        var textBaseline = Float.NEGATIVE_INFINITY

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.onPositioned { tabRowCoords = it },
                    items = titles,
                    selectedIndex = state
                ) { index, text ->
                    Tab(
                        text = {
                            Text(text, Modifier.onPositioned { coords: LayoutCoordinates ->
                                textCoords = coords
                                textBaseline = coords[LastBaseline]!!.toPx().value
                            })
                        },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val expectedBaseline = 18.dp
            val indicatorHeight = 2.dp
            val expectedBaselineDistance =
                (expectedBaseline.toIntPx() + indicatorHeight.toIntPx()).toPx()

            val tabRowHeight = tabRowCoords.size.height

            val textPositionY = textCoords.localToGlobal(PxPosition.Origin).y
            val baselinePositionY = textPositionY + textBaseline
            val expectedPositionY = (tabRowHeight.toPx() - expectedBaselineDistance).value
            assertThat(baselinePositionY).isEqualTo(expectedPositionY)
        }
    }

    @Test
    fun singleLineTab_withIcon_textBaseline() {
        lateinit var tabRowCoords: LayoutCoordinates
        lateinit var textCoords: LayoutCoordinates
        var textBaseline = Float.NEGATIVE_INFINITY

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB")

            Box {
                TabRow(
                    modifier = Modifier.onPositioned { tabRowCoords = it },
                    items = titles,
                    selectedIndex = state
                ) { index, text ->
                    Tab(
                        text = {
                            Text(text, Modifier.onPositioned { coords: LayoutCoordinates ->
                                textCoords = coords
                                textBaseline = coords[LastBaseline]!!.toPx().value
                            })
                        },
                        icon = { Icon(Icons.Filled.Favorite) },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val expectedBaseline = 14.dp
            val indicatorHeight = 2.dp
            val expectedBaselineDistance =
                (expectedBaseline.toIntPx() + indicatorHeight.toIntPx()).toPx()

            val tabRowHeight = tabRowCoords.size.height

            val textPositionY = textCoords.localToGlobal(PxPosition.Origin).y
            val baselinePositionY = textPositionY + textBaseline
            val expectedPositionY = (tabRowHeight.toPx() - expectedBaselineDistance).value
            assertThat(baselinePositionY).isEqualTo(expectedPositionY)
        }
    }

    @Test
    fun twoLineTab_textBaseline() {
        lateinit var tabRowCoords: LayoutCoordinates
        lateinit var textCoords: LayoutCoordinates
        var textBaseline = Float.NEGATIVE_INFINITY

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("VERY LONG TAB TITLE THAT WILL BE FORCED TO GO TO TWO LINES")

            Box {
                TabRow(
                    modifier = Modifier.onPositioned { tabRowCoords = it },
                    items = titles,
                    selectedIndex = state
                ) { index, text ->
                    Tab(
                        text = {
                            Text(text, Modifier.preferredWidth(100.dp).onPositioned { coords ->
                                textCoords = coords
                                textBaseline = coords[LastBaseline]!!.toPx().value
                            }, maxLines = 2)
                        },
                        selected = state == index,
                        onSelected = { state = index }
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val expectedBaseline = 10.dp
            val indicatorHeight = 2.dp
            val expectedBaselineDistance =
                (expectedBaseline.toIntPx() + indicatorHeight.toIntPx()).toPx()

            val tabRowHeight = tabRowCoords.size.height

            val textPositionY = textCoords.localToGlobal(PxPosition.Origin).y
            val baselinePositionY = textPositionY + textBaseline
            val expectedPositionY = (tabRowHeight.toPx() - expectedBaselineDistance).value
            assertThat(baselinePositionY).isEqualTo(expectedPositionY)
        }
    }

    @Test
    fun scrollableTabRow_indicatorPosition() {
        val indicatorHeight = 1.dp
        val scrollableTabRowOffset = 52.dp
        val minimumTabWidth = 90.dp
        lateinit var tabRowCoords: LayoutCoordinates
        lateinit var indicatorCoords: LayoutCoordinates

        composeTestRule.setMaterialContent {
            var state by state { 0 }
            val titles = listOf("TAB 1", "TAB 2")

            val indicatorContainer = @Composable { tabPositions: List<TabRow.TabPosition> ->
                TabRow.IndicatorContainer(tabPositions, state) {
                    Box(Modifier
                        .onPositioned { indicatorCoords = it }
                        .fillMaxWidth()
                        .preferredHeight(indicatorHeight)
                        .drawBackground(Color.Red)
                    )
                }
            }

            Box {
                TabRow(
                    modifier = Modifier.onPositioned { tabRowCoords = it },
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

        val tabRowHeight = composeTestRule.runOnIdleComposeWithDensity {
            val tabRowHeight = tabRowCoords.size.height

            // Indicator should be placed in the bottom left of the first tab
            val indicatorPositionX = indicatorCoords.localToGlobal(PxPosition.Origin).x
            // Tabs in a scrollable tab row are offset 52.dp from each end
            val expectedPositionX = scrollableTabRowOffset.toIntPx().value.toFloat()
            assertThat(indicatorPositionX).isEqualTo(expectedPositionX)

            val indicatorPositionY = indicatorCoords.localToGlobal(PxPosition.Origin).y
            val expectedPositionY = (tabRowHeight - indicatorHeight.toIntPx()).value.toFloat()
            assertThat(indicatorPositionY).isEqualTo(expectedPositionY)

            tabRowHeight
        }

        // Click the second tab
        findAll(isInMutuallyExclusiveGroup())[1].doClick()

        // Indicator should now be placed in the bottom left of the second tab, so its x coordinate
        // should be in the middle of the TabRow
        composeTestRule.runOnIdleComposeWithDensity {
            val indicatorPositionX = indicatorCoords.localToGlobal(PxPosition.Origin).x
            val expectedPositionX =
                (scrollableTabRowOffset + minimumTabWidth).toIntPx().value.toFloat()
            assertThat(indicatorPositionX).isEqualTo(expectedPositionX)

            val indicatorPositionY = indicatorCoords.localToGlobal(PxPosition.Origin).y
            val expectedPositionY =
                (tabRowHeight - indicatorHeight.toIntPx()).value.toFloat()
            assertThat(indicatorPositionY).isEqualTo(expectedPositionY)
        }
    }

    @Test
    fun fixedTabRow_initialTabSelected() {
        composeTestRule
            .setMaterialContent {
                TextTabs()
            }

        // Only the first tab should be selected
        findAll(isInMutuallyExclusiveGroup())
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
        findAll(isInMutuallyExclusiveGroup())
            .assertCountEquals(3)
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsUnselected()
                get(2).assertIsUnselected()
            }

        // Click the last tab
        findAll(isInMutuallyExclusiveGroup())[2].doClick()

        // Now only the last tab should be selected
        findAll(isInMutuallyExclusiveGroup())
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
        findAll(isInMutuallyExclusiveGroup())
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
        findAll(isInMutuallyExclusiveGroup())
            .assertCountEquals(10)
            .apply {
                get(0).assertIsSelected()
                (1..9).forEach {
                    get(it).assertIsUnselected()
                }
            }

        // Click the second tab
        findAll(isInMutuallyExclusiveGroup())[1].doClick()

        // Now only the second tab should be selected
        findAll(isInMutuallyExclusiveGroup())
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
