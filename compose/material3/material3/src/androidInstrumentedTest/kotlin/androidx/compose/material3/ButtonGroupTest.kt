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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ButtonGroupTest {
    @get:Rule val rule = createComposeRule()

    private val wrapperTestTag = "WrapperTestTag"
    private val aButton = "AButton"
    private val bButton = "BButton"
    private val cButton = "CButton"
    private val dButton = "DButton"

    @Test
    fun default_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(modifier = Modifier.testTag(aButton), onClick = {}) { Text("A") }
                    Button(modifier = Modifier.testTag(bButton), onClick = {}) { Text("B") }
                    Button(modifier = Modifier.testTag(cButton), onClick = {}) { Text("C") }
                    Button(modifier = Modifier.testTag(dButton), onClick = {}) { Text("D") }
                }
            }
        }

        val wrapperBounds = rule.onNodeWithTag(wrapperTestTag).getUnclippedBoundsInRoot()
        val aButtonBounds = rule.onNodeWithTag(aButton).getUnclippedBoundsInRoot()
        val bButtonBounds = rule.onNodeWithTag(bButton).getUnclippedBoundsInRoot()
        val cButtonBounds = rule.onNodeWithTag(cButton).getUnclippedBoundsInRoot()
        val dButtonBounds = rule.onNodeWithTag(dButton).getUnclippedBoundsInRoot()

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
                ButtonGroup(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(modifier = Modifier.testTag(aButton), onClick = {}) { Text("A") }
                    Button(modifier = Modifier.testTag(bButton), onClick = {}) { Text("B") }
                    Button(modifier = Modifier.testTag(cButton), onClick = {}) { Text("C") }
                    Button(modifier = Modifier.testTag(dButton), onClick = {}) { Text("D") }
                }
            }
        }

        val wrapperBounds = rule.onNodeWithTag(wrapperTestTag).getUnclippedBoundsInRoot()
        val aButtonBounds = rule.onNodeWithTag(aButton).getUnclippedBoundsInRoot()
        val bButtonBounds = rule.onNodeWithTag(bButton).getUnclippedBoundsInRoot()
        val cButtonBounds = rule.onNodeWithTag(cButton).getUnclippedBoundsInRoot()
        val dButtonBounds = rule.onNodeWithTag(dButton).getUnclippedBoundsInRoot()

        (aButtonBounds.left - wrapperBounds.left).assertIsEqualTo(0.dp)
        (bButtonBounds.left - aButtonBounds.right).assertIsEqualTo(4.dp)
        (cButtonBounds.left - bButtonBounds.right).assertIsEqualTo(4.dp)
        (dButtonBounds.left - cButtonBounds.right).assertIsEqualTo(4.dp)
        (wrapperBounds.right - dButtonBounds.right).assertIsEqualTo(0.dp)
    }

    @Test
    fun default_firstPressed_buttonSizing() {
        val width = 75.dp
        val animateFraction = 0.15f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * animateFraction)

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.15f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * (animateFraction / 2f))

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.15f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * (animateFraction / 2f))

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.15f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * animateFraction)

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.3f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * animateFraction)

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(animateFraction = animateFraction) {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.3f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * animateFraction / 2f)

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(animateFraction = animateFraction) {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.3f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * animateFraction / 2f)

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(animateFraction = animateFraction) {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
        val animateFraction = 0.3f
        val expectedExpandWidth = width + (width * animateFraction)
        val expectedCompressWidth = width - (width * animateFraction)

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup(animateFraction = animateFraction) {
                    Button(modifier = Modifier.width(width).testTag(aButton), onClick = {}) {
                        Text("A")
                    }
                    Button(modifier = Modifier.width(width).testTag(bButton), onClick = {}) {
                        Text("B")
                    }
                    Button(modifier = Modifier.width(width).testTag(cButton), onClick = {}) {
                        Text("C")
                    }
                    Button(modifier = Modifier.width(width).testTag(dButton), onClick = {}) {
                        Text("D")
                    }
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
}
