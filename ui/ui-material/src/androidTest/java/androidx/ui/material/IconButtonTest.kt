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

import androidx.compose.emptyContent
import androidx.test.filters.LargeTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInParent
import androidx.ui.foundation.Box
import androidx.ui.layout.preferredSize
import androidx.ui.material.samples.IconButtonSample
import androidx.ui.material.samples.IconToggleButtonSample
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.find
import androidx.ui.test.isToggleable
import androidx.ui.unit.dp
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
/**
 * Test for [IconButton] and [IconToggleButton].
 */
class IconButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun iconButton_size() {
        val width = 48.dp
        val height = 48.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                IconButtonSample()
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Test
    fun iconButton_materialIconSize_iconPositioning() {
        val diameter = 24.dp
        lateinit var iconCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                IconButton(onClick = {}) {
                    Box(
                        Modifier.preferredSize(diameter).onPositioned { iconCoords = it },
                        children = emptyContent()
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val iconX = iconCoords.positionInParent.x
            val iconY = iconCoords.positionInParent.y
            // Icon should be centered inside the IconButton
            Truth.assertThat(iconX).isEqualTo(12.dp.toIntPx().value.toFloat())
            Truth.assertThat(iconY).isEqualTo(12.dp.toIntPx().value.toFloat())
        }
    }

    @Test
    fun iconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        lateinit var iconCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                IconButton(onClick = {}) {
                    Box(
                        Modifier.preferredSize(width, height).onPositioned { iconCoords = it },
                        children = emptyContent()
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val iconX = iconCoords.positionInParent.x
            val iconY = iconCoords.positionInParent.y

            val expectedX = ((48.dp - width) / 2).toIntPx().value.toFloat()
            val expectedY = ((48.dp - height) / 2).toIntPx().value.toFloat()
            // Icon should be centered inside the IconButton
            Truth.assertThat(iconX).isEqualTo(expectedX)
            Truth.assertThat(iconY).isEqualTo(expectedY)
        }
    }

    @Test
    fun iconToggleButton_size() {
        val width = 48.dp
        val height = 48.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                IconToggleButtonSample()
            }
            .assertWidthEqualsTo(width)
            .assertHeightEqualsTo(height)
    }

    @Test
    fun iconToggleButton_materialIconSize_iconPositioning() {
        val diameter = 24.dp
        lateinit var iconCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(
                        Modifier.preferredSize(diameter).onPositioned { iconCoords = it },
                        children = emptyContent()
                    )
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val iconX = iconCoords.positionInParent.x
            val iconY = iconCoords.positionInParent.y
            // Icon should be centered inside the IconButton
            Truth.assertThat(iconX).isEqualTo(12.dp.toIntPx().value.toFloat())
            Truth.assertThat(iconY).isEqualTo(12.dp.toIntPx().value.toFloat())
        }
    }

    @Test
    fun iconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        lateinit var iconCoords: LayoutCoordinates
        composeTestRule.setMaterialContent {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(
                        Modifier.preferredSize(width, height).onPositioned { iconCoords = it },
                        children = emptyContent())
                }
            }
        }

        composeTestRule.runOnIdleComposeWithDensity {
            val iconX = iconCoords.positionInParent.x
            val iconY = iconCoords.positionInParent.y

            val expectedX = ((48.dp - width) / 2).toIntPx().value.toFloat()
            val expectedY = ((48.dp - height) / 2).toIntPx().value.toFloat()
            // Icon should be centered inside the IconButton
            Truth.assertThat(iconX).isEqualTo(expectedX)
            Truth.assertThat(iconY).isEqualTo(expectedY)
        }
    }

    @Test
    fun iconToggleButton_semantics() {
        composeTestRule.setMaterialContent {
            IconToggleButtonSample()
        }
        find(isToggleable()).apply {
            assertIsOff()
            doClick()
            assertIsOn()
        }
    }
}
