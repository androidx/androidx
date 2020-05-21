/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.test.filters.LargeTest
import androidx.ui.text.LastBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.samples.BottomNavigationSample
import androidx.ui.semantics.Semantics
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findAll
import androidx.ui.test.findByText
import androidx.ui.test.isInMutuallyExclusiveGroup
import androidx.ui.unit.dp
import androidx.ui.unit.toPx
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
/**
 * Test for [BottomNavigation] and [BottomNavigationItem].
 */
class BottomNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun bottomNavigation_size() {
        lateinit var parentCoords: LayoutCoordinates
        val height = 56.dp
        composeTestRule.setMaterialContentAndCollectSizes(
            modifier = Modifier.onPositioned { coords: LayoutCoordinates ->
                parentCoords = coords
            }
        ) {
            BottomNavigationSample()
        }
            .assertWidthEqualsTo { parentCoords.size.width }
            .assertHeightEqualsTo(height)
    }

    @Test
    fun bottomNavigationItem_sizeAndPositions() {
        lateinit var parentCoords: LayoutCoordinates
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        composeTestRule.setMaterialContent(Modifier.onPositioned { coords: LayoutCoordinates ->
            parentCoords = coords
        }) {
            Box {
                BottomNavigation {
                    repeat(4) { index ->
                        BottomNavigationItem(
                            icon = { Icon(Icons.Filled.Favorite) },
                            text = { Text("Item $index") },
                            selected = index == 0,
                            onSelected = {},
                            modifier = Modifier.onPositioned { coords: LayoutCoordinates ->
                                itemCoords[index] = coords
                            }
                        )
                    }
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val totalWidth = parentCoords.size.width

            val expectedItemWidth = totalWidth / 4
            val expectedItemHeight = 56.dp.toIntPx()

            Truth.assertThat(itemCoords.size).isEqualTo(4)

            itemCoords.forEach { (index, coord) ->
                Truth.assertThat(coord.size.width).isEqualTo(expectedItemWidth)
                Truth.assertThat(coord.size.height).isEqualTo(expectedItemHeight)
                Truth.assertThat(coord.globalPosition.x)
                    .isEqualTo((expectedItemWidth * index).value.toFloat())
            }
        }
    }

    @Test
    fun bottomNavigationItemContent_withLabel_sizeAndPosition() {
        lateinit var itemCoords: LayoutCoordinates
        lateinit var iconCoords: LayoutCoordinates
        lateinit var textCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                BottomNavigation {
                    BottomNavigationItem(
                        modifier = Modifier.onPositioned { coords: LayoutCoordinates ->
                            itemCoords = coords
                        },
                        icon = {
                            Icon(Icons.Filled.Favorite,
                                Modifier.onPositioned { iconCoords = it }
                            )
                        },
                        text = {
                            Text("Item",
                                Modifier.onPositioned { textCoords = it }
                            )
                        },
                        selected = true,
                        onSelected = {}
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            // Distance from the bottom to the text baseline and from the text baseline to the
            // bottom of the icon
            val textBaseline = 12.dp.toIntPx().value.toFloat()

            // Relative position of the baseline to the top of text
            val relativeTextBaseline = textCoords[LastBaseline]!!.toPx().value
            // Absolute y position of the text baseline
            val absoluteTextBaseline = textCoords.globalPosition.y + relativeTextBaseline

            val itemBottom = itemCoords.size.height.toPx().value + itemCoords.globalPosition.y
            // Text baseline should be 12.dp from the bottom of the item
            Truth.assertThat(absoluteTextBaseline).isEqualTo(itemBottom - textBaseline)

            // The icon should be centered in the item
            val iconExpectedX = (itemCoords.size.width.toPx() - iconCoords.size.width.toPx()) / 2
            // The bottom of the icon is 12.dp above the text baseline
            val iconExpectedY =
                absoluteTextBaseline - 12.dp.toIntPx().value.toFloat() -
                        iconCoords.size.height.value

            Truth.assertThat(iconCoords.globalPosition.x).isWithin(1f).of(iconExpectedX.value)
            Truth.assertThat(iconCoords.globalPosition.y).isEqualTo(iconExpectedY)
        }
    }

    @Test
    fun bottomNavigationItemContent_withLabel_unselected_sizeAndPosition() {
        lateinit var itemCoords: LayoutCoordinates
        lateinit var iconCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                BottomNavigation {
                    BottomNavigationItem(
                        modifier = Modifier.onPositioned { coords: LayoutCoordinates ->
                            itemCoords = coords
                        },
                        icon = {
                            Icon(Icons.Filled.Favorite,
                                Modifier.onPositioned { iconCoords = it }
                            )
                        },
                        text = {
                            // TODO: b/149477576 we need a boundary here so that we don't
                            // merge the text in order for assertIsNotDisplayed to work. If
                            // we merge upwards the text is merged into the main component
                            // which is displayed, so the test fails.
                            Semantics(container = true) {
                                Text("Item")
                            }
                        },
                        selected = false,
                        onSelected = {},
                        alwaysShowLabels = false
                    )
                }
            }
        }

        // The text should not be placed, since the item is not selected and alwaysShowLabels
        // is false
        findByText("Item").assertIsNotDisplayed()

        composeTestRule.runOnIdleComposeWithDensity {
            // The icon should be centered in the item
            val iconExpectedX = (itemCoords.size.width.toPx() - iconCoords.size.width.toPx()) / 2
            val iconExpectedY = (itemCoords.size.height - iconCoords.size.height) / 2

            Truth.assertThat(iconCoords.globalPosition.x).isWithin(1f).of(iconExpectedX.value)
            Truth.assertThat(iconCoords.globalPosition.y).isEqualTo(iconExpectedY.toPx().value)
        }
    }

    @Test
    fun bottomNavigationItemContent_withoutLabel_sizeAndPosition() {
        lateinit var itemCoords: LayoutCoordinates
        lateinit var iconCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                BottomNavigation {
                    BottomNavigationItem(
                        modifier = Modifier.onPositioned { coords: LayoutCoordinates ->
                            itemCoords = coords
                        },
                        icon = {
                            Icon(Icons.Filled.Favorite,
                                Modifier.onPositioned { iconCoords = it }
                            )
                        },
                        text = {},
                        selected = false,
                        onSelected = {}
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            // The icon should be centered in the item, as there is no text placeable provided
            val iconExpectedX = (itemCoords.size.width.toPx() - iconCoords.size.width.toPx()) / 2
            val iconExpectedY = (itemCoords.size.height.toPx() - iconCoords.size.height.toPx()) / 2

            Truth.assertThat(iconCoords.globalPosition.x).isWithin(1f).of(iconExpectedX.value)
            Truth.assertThat(iconCoords.globalPosition.y).isEqualTo(iconExpectedY.value)
        }
    }

    @Test
    fun bottomNavigation_selectNewItem() {
        composeTestRule.setMaterialContent {
            BottomNavigationSample()
        }

        // Find all items and ensure there are 3
        findAll(isInMutuallyExclusiveGroup())
            .assertCountEquals(3)
            // Ensure semantics match for selected state of the items
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsUnselected()
                get(2).assertIsUnselected()
            }
            // Click the last item
            .apply {
                get(2).doClick()
            }
            .apply {
                get(0).assertIsUnselected()
                get(1).assertIsUnselected()
                get(2).assertIsSelected()
            }
    }
}
