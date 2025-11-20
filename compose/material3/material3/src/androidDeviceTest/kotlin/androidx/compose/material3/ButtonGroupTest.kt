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

import android.annotation.SuppressLint
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ButtonGroupTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val wrapperTestTag = "WrapperTestTag"
    private val aButton = "A"
    private val bButton = "B"
    private val cButton = "C"
    private val dButton = "D"
    private val overflowIndicator = "overflowIndicator"

    @Test
    fun default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(overflowIndicator = {}, verticalAlignment = Alignment.Top) {
                    clickableItem(onClick = {}, label = "A")
                    clickableItem(onClick = {}, label = "B")
                    clickableItem(onClick = {}, label = "C")
                    clickableItem(onClick = {}, label = "D")
                }
            }
        }

        val wrapperBounds = rule.onNodeWithTag(wrapperTestTag).getUnclippedBoundsInRoot()
        val aButtonBounds = rule.onNodeWithText(aButton).getUnclippedBoundsInRoot()
        val bButtonBounds = rule.onNodeWithText(bButton).getUnclippedBoundsInRoot()
        val cButtonBounds = rule.onNodeWithText(cButton).getUnclippedBoundsInRoot()
        val dButtonBounds = rule.onNodeWithText(dButton).getUnclippedBoundsInRoot()

        (aButtonBounds.left - wrapperBounds.left).assertIsEqualTo(0.dp)
        (bButtonBounds.left - aButtonBounds.right).assertIsEqualTo(12.dp)
        (cButtonBounds.left - bButtonBounds.right).assertIsEqualTo(12.dp)
        (dButtonBounds.left - cButtonBounds.right).assertIsEqualTo(12.dp)
        (wrapperBounds.right - dButtonBounds.right).assertIsEqualTo(0.dp)
    }

    @Test
    fun differentHorizontalSpacing_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(overflowIndicator = {}, verticalAlignment = Alignment.Top) {
                    clickableItem(onClick = {}, label = "A")
                    clickableItem(onClick = {}, label = "B")
                    clickableItem(onClick = {}, label = "C")
                    clickableItem(onClick = {}, label = "D")
                }
            }
        }

        val wrapperBounds = rule.onNodeWithTag(wrapperTestTag).getUnclippedBoundsInRoot()
        val aButtonBounds = rule.onNodeWithText(aButton).getUnclippedBoundsInRoot()
        val bButtonBounds = rule.onNodeWithText(bButton).getUnclippedBoundsInRoot()
        val cButtonBounds = rule.onNodeWithText(cButton).getUnclippedBoundsInRoot()
        val dButtonBounds = rule.onNodeWithText(dButton).getUnclippedBoundsInRoot()

        (aButtonBounds.left - wrapperBounds.left).assertIsEqualTo(0.dp)
        (bButtonBounds.left - aButtonBounds.right).assertIsEqualTo(12.dp)
        (cButtonBounds.left - bButtonBounds.right).assertIsEqualTo(12.dp)
        (dButtonBounds.left - cButtonBounds.right).assertIsEqualTo(12.dp)
        (wrapperBounds.right - dButtonBounds.right).assertIsEqualTo(0.dp)
    }

    @Test
    fun default_firstPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.15f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * expandedRatio)
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(aButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(expectedExpandWidth)
        bButton.assertWidthIsEqualTo(expectedCompressWidth)
        cButton.assertWidthIsEqualTo(width)
        dButton.assertWidthIsEqualTo(width)
    }

    @Test
    fun default_secondPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.15f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * (expandedRatio / 2f))
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(bButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(expectedCompressWidth)
        bButton.assertWidthIsEqualTo(expectedExpandWidth)
        cButton.assertWidthIsEqualTo(expectedCompressWidth)
        dButton.assertWidthIsEqualTo(width)
    }

    @Test
    fun default_thirdPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.15f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * (expandedRatio / 2f))
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(cButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(width)
        bButton.assertWidthIsEqualTo(expectedCompressWidth)
        cButton.assertWidthIsEqualTo(expectedExpandWidth)
        dButton.assertWidthIsEqualTo(expectedCompressWidth)
    }

    @Test
    fun default_fourthPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.15f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * expandedRatio)
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(dButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(width)
        bButton.assertWidthIsEqualTo(width)
        cButton.assertWidthIsEqualTo(expectedCompressWidth)
        dButton.assertWidthIsEqualTo(expectedExpandWidth)
    }

    @Test
    fun customAnimateFraction_firstPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.3f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * expandedRatio)
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(aButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(expectedExpandWidth)
        bButton.assertWidthIsEqualTo(expectedCompressWidth)
        cButton.assertWidthIsEqualTo(width)
        dButton.assertWidthIsEqualTo(width)
    }

    @Test
    fun customAnimateFraction_secondPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.3f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * expandedRatio / 2f)
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(bButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(expectedCompressWidth)
        bButton.assertWidthIsEqualTo(expectedExpandWidth)
        cButton.assertWidthIsEqualTo(expectedCompressWidth)
        dButton.assertWidthIsEqualTo(width)
    }

    @Test
    fun customAnimateFraction_thirdPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.3f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * expandedRatio / 2f)
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(cButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(width)
        bButton.assertWidthIsEqualTo(expectedCompressWidth)
        cButton.assertWidthIsEqualTo(expectedExpandWidth)
        dButton.assertWidthIsEqualTo(expectedCompressWidth)
    }

    @Test
    fun customAnimateFraction_fourthPressed_buttonSizing() {
        val width = 75.dp
        val expandedRatio = 0.3f
        val expectedExpandWidth = width + (width * expandedRatio)
        val expectedCompressWidth = width - (width * expandedRatio)
        val interactionSources = List(4) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    expandedRatio = expandedRatio,
                    verticalAlignment = Alignment.Top,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[0])
                                        .testTag(aButton),
                                interactionSource = interactionSources[0],
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[1])
                                        .testTag(bButton),
                                interactionSource = interactionSources[1],
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[2])
                                        .testTag(cButton),
                                interactionSource = interactionSources[2],
                            ) {
                                Text("C")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier =
                                    Modifier.width(width)
                                        .animateWidth(interactionSources[3])
                                        .testTag(dButton),
                                interactionSource = interactionSources[3],
                            ) {
                                Text("D")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(dButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        rule.waitForIdle()

        val aButton = rule.onNodeWithTag(aButton)
        val bButton = rule.onNodeWithTag(bButton)
        val cButton = rule.onNodeWithTag(cButton)
        val dButton = rule.onNodeWithTag(dButton)

        aButton.assertWidthIsEqualTo(width)
        bButton.assertWidthIsEqualTo(width)
        cButton.assertWidthIsEqualTo(expectedCompressWidth)
        dButton.assertWidthIsEqualTo(expectedExpandWidth)
    }

    @Test
    fun overflowIndicator_tooManyItems_Exists() {
        val numButtons = 100
        rule.setContent {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {
                        IconButton(modifier = Modifier.testTag(overflowIndicator), onClick = {}) {}
                    },
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0 until numButtons) {
                        clickableItem(onClick = {}, label = "$i")
                    }
                }
            }
        }
        rule.onNodeWithTag(overflowIndicator).assertIsDisplayed()
    }

    @Test
    fun overflowIndicator_fitsOnScreen_doesNotExists() {
        val numButtons = 4
        rule.setContent {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {
                        IconButton(modifier = Modifier.testTag(overflowIndicator), onClick = {}) {}
                    },
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0 until numButtons) {
                        clickableItem(onClick = {}, label = "$i")
                    }
                }
            }
        }
        rule.onNodeWithTag(overflowIndicator).assertIsNotDisplayed()
    }

    @Test
    fun overflowMenu_tooManyItems_exists() {
        val numButtons = 10
        rule.setContent {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = { menuState ->
                        IconButton(
                            modifier = Modifier.testTag(overflowIndicator),
                            onClick = {
                                if (menuState.isShowing) {
                                    menuState.dismiss()
                                } else {
                                    menuState.show()
                                }
                            },
                        ) {}
                    },
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0 until numButtons) {
                        customItem(
                            buttonGroupContent = { Button(onClick = {}) { Text("$i") } },
                            menuContent = {
                                DropdownMenuItem(
                                    enabled = true,
                                    text = { Text("$i") },
                                    modifier = Modifier.testTag("$i MenuItem"),
                                    onClick = {},
                                )
                            },
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("9 MenuItem").assertIsNotDisplayed()

        rule.onNodeWithTag(overflowIndicator).performClick()

        rule.onNodeWithTag("9 MenuItem").assertIsDisplayed()
    }

    @Test
    fun horizontalScroll_handlingScroll_buttonDisplay() {
        val interactionSources = List(10) { MutableInteractionSource() }

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0..interactionSources.lastIndex) {
                        clickableItem(onClick = {}, label = "$i")
                    }
                }
            }
        }

        rule.onNodeWithText("9").assertIsNotDisplayed()

        rule.onNodeWithTag(wrapperTestTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 1500f, this.center.y),
                durationMillis = 100,
            )
        }

        rule.onNodeWithText("9").assertIsDisplayed()
    }

    @SuppressLint("ConfigurationScreenWidthHeight")
    @Test
    fun horizontalArrangement_startArrangement_buttonGroupPositioning() {
        val interactionSources = List(3) { MutableInteractionSource() }
        var screenWidth: Dp = 0.dp
        rule.setMaterialContent(lightColorScheme()) {
            val configuration = LocalConfiguration.current
            screenWidth = configuration.screenWidthDp.dp
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0..interactionSources.lastIndex) {
                        clickableItem(onClick = {}, label = "$i")
                    }
                }
            }
        }
        val buttonOneBounds = rule.onNodeWithText("0").getUnclippedBoundsInRoot()
        val buttonThreeBounds = rule.onNodeWithText("2").getUnclippedBoundsInRoot()
        val buttonRange = buttonOneBounds.left..buttonThreeBounds.right
        val expectedPositioning = screenWidth / 3
        assertThat(expectedPositioning in buttonRange).isTrue()
    }

    @SuppressLint("ConfigurationScreenWidthHeight")
    @Test
    fun horizontalArrangement_centerArrangement_buttonGroupPositioning() {
        val interactionSources = List(3) { MutableInteractionSource() }
        var screenWidth: Dp = 0.dp
        rule.setMaterialContent(lightColorScheme()) {
            val configuration = LocalConfiguration.current
            screenWidth = configuration.screenWidthDp.dp
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0..interactionSources.lastIndex) {
                        clickableItem(onClick = {}, label = "$i")
                    }
                }
            }
        }
        val buttonOneBounds = rule.onNodeWithText("0").getUnclippedBoundsInRoot()
        val buttonThreeBounds = rule.onNodeWithText("2").getUnclippedBoundsInRoot()
        val buttonRange = buttonOneBounds.left..buttonThreeBounds.right
        val expectedPositioning = screenWidth / 2
        assertThat(expectedPositioning in buttonRange).isTrue()
    }

    @SuppressLint("ConfigurationScreenWidthHeight")
    @Test
    fun horizontalArrangement_endArrangement_buttonGroupPositioning() {
        val interactionSources = List(3) { MutableInteractionSource() }
        var screenWidth: Dp = 0.dp
        rule.setMaterialContent(lightColorScheme()) {
            val configuration = LocalConfiguration.current
            screenWidth = configuration.screenWidthDp.dp
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(
                    overflowIndicator = {},
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top,
                ) {
                    for (i in 0..interactionSources.lastIndex) {
                        clickableItem(onClick = {}, label = "$i")
                    }
                }
            }
        }
        val buttonOneBounds = rule.onNodeWithText("0").getUnclippedBoundsInRoot()
        val buttonThreeBounds = rule.onNodeWithText("2").getUnclippedBoundsInRoot()
        val buttonRange = buttonOneBounds.left..buttonThreeBounds.right
        val expectedPositioning = screenWidth * 2 / 3
        assertThat(expectedPositioning in buttonRange).isTrue()
    }

    @Test
    fun buttonGroup_verticalAlignment_isTop() {
        val button1Tag = "button1"
        val button2Tag = "button2"

        rule.setContent {
            Box {
                ButtonGroup(overflowIndicator = {}, verticalAlignment = Alignment.Top) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(48.dp).testTag(button1Tag),
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(64.dp).testTag(button2Tag),
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        val button1Top = rule.onNodeWithTag(button1Tag).getUnclippedBoundsInRoot().top
        val button2Top = rule.onNodeWithTag(button2Tag).getUnclippedBoundsInRoot().top
        assertThat(button1Top).isEqualTo(button2Top)
    }

    @Test
    fun buttonGroup_verticalAlignment_isCenter() {
        val button1Tag = "button1"
        val button2Tag = "button2"

        rule.setContent {
            Box {
                ButtonGroup(
                    overflowIndicator = {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(48.dp).testTag(button1Tag),
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(64.dp).testTag(button2Tag),
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        val button1Bounds = rule.onNodeWithTag(button1Tag).getUnclippedBoundsInRoot()
        val button2Bounds = rule.onNodeWithTag(button2Tag).getUnclippedBoundsInRoot()
        val button1Center = button1Bounds.top + (button1Bounds.bottom - button1Bounds.top) / 2
        val button2Center = button2Bounds.top + (button2Bounds.bottom - button2Bounds.top) / 2
        assertThat(button1Center.value.roundToInt()).isEqualTo(button2Center.value.roundToInt())
    }

    @Test
    fun buttonGroup_verticalAlignment_isBottom() {
        val button1Tag = "button1"
        val button2Tag = "button2"

        rule.setContent {
            Box {
                ButtonGroup(overflowIndicator = {}, verticalAlignment = Alignment.Bottom) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(48.dp).testTag(button1Tag),
                            ) {
                                Text("A")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = {},
                                modifier = Modifier.height(64.dp).testTag(button2Tag),
                            ) {
                                Text("B")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        val button1Bottom = rule.onNodeWithTag(button1Tag).getUnclippedBoundsInRoot().bottom
        val button2Bottom = rule.onNodeWithTag(button2Tag).getUnclippedBoundsInRoot().bottom
        assertThat(button1Bottom).isEqualTo(button2Bottom)
    }

    @Test
    fun buttonGroup_individualAlignment_alignTop() {
        rule.setContent {
            Box {
                ButtonGroup(overflowIndicator = {}) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                modifier = Modifier.height(50.dp).align(Alignment.Top),
                                onClick = {},
                            ) {
                                Text("Top")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(modifier = Modifier.height(100.dp), onClick = {}) {
                                Text("Tall")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        rule.onNodeWithText("Top").assertTopPositionInRootIsEqualTo(0.dp)
        rule.onNodeWithText("Tall").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun buttonGroup_individualAlignment_alignCenter() {
        rule.setContent {
            Box {
                ButtonGroup(overflowIndicator = {}) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                modifier = Modifier.height(50.dp).align(Alignment.CenterVertically),
                                onClick = {},
                            ) {
                                Text("Center")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(modifier = Modifier.height(100.dp), onClick = {}) {
                                Text("Tall")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        // The group height will be 100.dp. The smaller button is 50.dp.
        // Y-position should be (100 - 50) / 2 = 25.dp
        rule.onNodeWithText("Center").assertTopPositionInRootIsEqualTo(25.dp)
        rule.onNodeWithText("Tall").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun buttonGroup_individualAlignment_alignBottom() {
        rule.setContent {
            Box {
                ButtonGroup(overflowIndicator = {}) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                modifier = Modifier.height(50.dp).align(Alignment.Bottom),
                                onClick = {},
                            ) {
                                Text("Bottom")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(modifier = Modifier.height(100.dp), onClick = {}) {
                                Text("Tall")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        // The group height will be 100.dp. The smaller button is 50.dp.
        // Y-position should be 100 - 50 = 50.dp
        rule.onNodeWithText("Bottom").assertTopPositionInRootIsEqualTo(50.dp)
        rule.onNodeWithText("Tall").assertTopPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun buttonGroup_individualAlignment_overridesGroupAlignment() {
        rule.setContent {
            Box {
                ButtonGroup(overflowIndicator = {}, verticalAlignment = Alignment.Bottom) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                modifier = Modifier.height(50.dp).align(Alignment.Top),
                                onClick = {},
                            ) {
                                Text("Top")
                            }
                        },
                        menuContent = {},
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(modifier = Modifier.height(100.dp), onClick = {}) {
                                Text("Tall")
                            }
                        },
                        menuContent = {},
                    )
                }
            }
        }

        // The individual alignment (Top) should take precedence.
        rule.onNodeWithText("Top").assertTopPositionInRootIsEqualTo(0.dp)
        // The "Tall" button should be aligned to the group's alignment (Bottom).
        rule.onNodeWithText("Tall").assertTopPositionInRootIsEqualTo(0.dp)
    }
}
