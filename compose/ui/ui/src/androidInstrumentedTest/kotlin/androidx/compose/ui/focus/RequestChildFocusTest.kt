/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RequestChildFocusTest {

    @get:Rule val rule = createComposeRule()

    /**
     *      __________________
     *     |    container     |
     *     |  _____________   | 0
     *     | |    box-0    |  |
     *     | |             |  |
     *     | |_____________|  |
     *     |  _____________   | 100
     *     | |    box-1    |  |
     *     | |             |  |
     *     | |_____________|  |
     *     |  _____________   | 200
     *     | |    box-2    |  |
     *     | |  □ <- focus |  |
     *     | |_____________|  |
     *     |       ...        | 300
     *     |__________________|
     */
    @Test
    fun requestedRect_is_insideTheNode() {
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 200.dp, height = 600.dp)) {
                repeat(6) { id ->
                    Box(
                        Modifier.testTag("box-$id")
                            .requiredSize(width = 200.dp, height = 100.dp)
                            .border(1.dp, Color.Black)
                            .focusable()
                    )
                }
            }
        }

        requestFocusForRectAndCheck(
            l = 50.dp,
            t = 240.dp,
            r = 60.dp,
            b = 250.dp,
            expectedFocusedTag = "box-2",
        )
    }

    /**
     *      __________________
     *     |    container     |
     *     |  _____________   | 0
     *     | |    box-0    |  |
     *     | |             |  |
     *     | |_____________|  |
     *     |  _____________   | 100
     *     | |    box-1    |  |
     *     | |  • <- focus |  |
     *     | |_____________|  |
     *     |  _____________   | 200
     *     | |    box-2    |  |
     *     | |             |  |
     *     | |_____________|  |
     *     |       ...        | 300
     *     |__________________|
     */
    @Test
    fun requestedRect_is_onePixelRect() {
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 200.dp, height = 600.dp)) {
                repeat(6) { id ->
                    Box(
                        Modifier.testTag("box-$id")
                            .requiredSize(width = 200.dp, height = 100.dp)
                            .border(1.dp, Color.Black)
                            .focusable()
                    )
                }
            }
        }

        requestFocusForRectAndCheck(
            l = 10.dp,
            t = 150.dp,
            r = 10.dp,
            b = 150.dp,
            expectedFocusedTag = "box-1",
        )
    }

    /**
     *      __________________
     *     |    container     |
     *     |  _____________   | 0
     *     | |    box-0    |  |
     *     | |             |  |
     *     | |_____________|  |
     *     |  _____________   | 100
     *     | |    box-1    |  |
     *     | |      &      |  |
     *     | |    focus    |  |
     *     | |_____________|  |
     *     |  _____________   | 200
     *     | |    box-2    |  |
     *     | |             |  |
     *     | |_____________|  |
     *     |       ...        | 300
     *     |__________________|
     */
    @Test
    fun requestedRect_is_theSameAsTheNodeBounds() {
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 200.dp, height = 600.dp)) {
                repeat(6) { id ->
                    Box(
                        Modifier.testTag("box-$id")
                            .requiredSize(width = 200.dp, height = 100.dp)
                            .border(1.dp, Color.Black)
                            .focusable()
                    )
                }
            }
        }

        requestFocusForRectAndCheck(
            l = 0.dp,
            t = 100.dp,
            r = 200.dp,
            b = 200.dp,
            expectedFocusedTag = "box-1",
        )
    }

    /**
     *     0                    200                   400
     *      __________________       __________________
     *     |   container-A    |     |   container-B    |
     *     |  _____________   |     |  _____________   |
     *     | |    box-A    |  |     | |    box-B    |  |
     *     | |             |  |     | |             |  |
     *     | |             |  |     | |  □ <- focus |  |
     *     | |_____________|  |     | |_____________|  |
     *     |__________________|     |__________________|
     */
    @Test
    fun requestedRect_belongsToAnotherContainer() {
        rule.setContent {
            Row(Modifier.requiredSize(width = 400.dp, height = 200.dp)) {
                Box(Modifier.testTag("container-A").requiredSize(200.dp)) {
                    Box(Modifier.testTag("box-A").fillMaxSize().focusable())
                }
                Box(Modifier.testTag("container-B").requiredSize(200.dp)) {
                    Box(Modifier.testTag("box-B").fillMaxSize().focusable())
                }
            }
        }

        val focusArea = DpRect(250.dp, 0.dp, 350.dp, 200.dp)
        val focusRequested = rule.onNodeWithTag("container-A").requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isFalse()
        rule.onNodeWithTag("box-A").assertIsNotFocused()
        rule.onNodeWithTag("box-B").assertIsNotFocused()
    }

    /**
     *      __________________
     *     |   container-A    |
     *     |                  |
     *     |                  |
     *     |    □ <- focus    |
     *     |                  |
     *     |__________________|
     */
    @Test
    fun containerItself_doesNotGetFocused() {
        rule.setContent { Box(Modifier.testTag(CONTAINER_TAG).requiredSize(100.dp).focusable()) }

        val focusArea = DpRect(0.dp, 0.dp, 100.dp, 100.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isFalse()
        rule.onNodeWithTag(CONTAINER_TAG).assertIsNotFocused()
    }

    /**
     *      __________________
     *     |    container     |
     *     |  _____________   | 0
     *     | |    box-0    |  |
     *     | |             |  |
     *     | |_____________|  | 100
     *     |                  |
     *     |    □ <- focus    |
     *     |  _____________   | 150
     *     | |    box-1    |  |
     *     | |             |  |
     *     | |_____________|  | 250
     *     |__________________|
     */
    @Test
    fun nonOverlappingRect_doesNotRequestFocus() {
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 200.dp, height = 600.dp)) {
                Box(
                    Modifier.testTag("box-A")
                        .requiredSize(width = 200.dp, height = 100.dp)
                        .border(1.dp, Color.Black)
                        .focusable()
                )
                Spacer(Modifier.height(50.dp))
                Box(
                    Modifier.testTag("box-B")
                        .requiredSize(width = 200.dp, height = 100.dp)
                        .border(1.dp, Color.Black)
                        .focusable()
                )
            }
        }

        val focusArea = DpRect(50.dp, 125.dp, 100.dp, 130.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isFalse()
        rule.onNodeWithTag("box-A").assertIsNotFocused()
        rule.onNodeWithTag("box-B").assertIsNotFocused()
    }

    @Test
    fun focusIsNotRequested_if_layoutNode_is_alreadyFocused() {
        val focusRequester = FocusRequester()
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 100.dp, height = 100.dp)) {
                Box(
                    Modifier.testTag("focused-box")
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
        }

        rule.runOnUiThread { focusRequester.requestFocus() }
        rule.onNodeWithTag("focused-box").assertIsFocused()

        val focusArea = DpRect(50.dp, 50.dp, 50.dp, 50.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isFalse()
        rule.onNodeWithTag("focused-box").assertIsFocused()
    }

    /**
     *      _________________  <- box-0
     *     | ________________| <- box-1
     *     || _____________ || <- box-2
     *     ||| □ <- focus  |||
     *     |||_____________|||
     *     ||_______________||
     *     |_________________|
     */
    @Test
    fun highestLayoutNode_getsFocused() {
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 300.dp, height = 300.dp)) {
                Box(Modifier.testTag("box-0").fillMaxSize().focusable()) {
                    Box(Modifier.testTag("box-1").fillMaxSize().focusable()) {
                        Box(Modifier.testTag("box-2").fillMaxSize().focusable())
                    }
                }
            }
        }

        requestFocusForRectAndCheck(
            l = 140.dp,
            t = 140.dp,
            r = 160.dp,
            b = 160.dp,
            expectedFocusedTag = "box-0",
        )
    }

    /**
     *      _________________  <- LayoutNode
     *     | ________________| <- focusable modifier #1 (expected)
     *     || _____________ || <- focusable modifier #2
     *     ||| ___________ ||| <- focusable modifier #3
     *     ||||  □<-focus ||||
     *     ||||___________||||
     *     |||_____________|||
     *     ||_______________||
     *     |_________________|
     */
    @Test
    fun firstFocusableModifier_getsFocused_with_innerMostBounds() {
        val focusStates = Array(3) { false }
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 300.dp, height = 300.dp)) {
                Box(
                    Modifier.testTag("multiple-modifiers")
                        .fillMaxSize()
                        .onFocusChanged { focusStates[0] = it.isFocused }
                        .focusable()
                        .padding(10.dp)
                        .onFocusChanged { focusStates[1] = it.isFocused }
                        .focusable()
                        .padding(10.dp)
                        .onFocusChanged { focusStates[2] = it.isFocused }
                        .focusable()
                )
            }
        }

        requestFocusForRectAndCheck(
            l = 150.dp,
            t = 150.dp,
            r = 150.dp,
            b = 150.dp,
            expectedFocusedTag = "multiple-modifiers",
        )

        Truth.assertThat(focusStates[0]).isTrue()
        Truth.assertThat(focusStates[1]).isFalse()
        Truth.assertThat(focusStates[2]).isFalse()
    }

    /**
     *      _________________  <- LayoutNode
     *     | ________________| 0.dp  <- focusable modifier #1 (expected)
     *     || _____________ || 10.dp <- focusable modifier #2
     *     |||   □<-focus  |||
     *     ||| ___________ ||| 20.dp <- focusable modifier #3
     *     ||||           ||||
     *     ||||___________||||
     *     |||_____________|||
     *     ||_______________||
     *     |_________________|
     */
    @Test
    fun firstFocusableModifier_getsFocused_with_intermediateBounds() {
        val focusStates = Array(3) { false }
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 300.dp, height = 300.dp)) {
                Box(
                    Modifier.testTag("multiple-modifiers")
                        .fillMaxSize()
                        .onFocusChanged { focusStates[0] = it.isFocused }
                        .focusable()
                        .padding(10.dp)
                        .onFocusChanged { focusStates[1] = it.isFocused }
                        .focusable()
                        .padding(10.dp)
                        .onFocusChanged { focusStates[2] = it.isFocused }
                        .focusable()
                )
            }
        }

        requestFocusForRectAndCheck(
            l = 150.dp,
            t = 15.dp,
            r = 150.dp,
            b = 15.dp,
            expectedFocusedTag = "multiple-modifiers",
        )

        Truth.assertThat(focusStates[0]).isTrue()
        Truth.assertThat(focusStates[1]).isFalse()
        Truth.assertThat(focusStates[2]).isFalse()
    }

    /**
     *      _________________  <- LayoutNode
     *     | ________________| 0.dp  <- focusable modifier #1 (expected)
     *     ||    □<-focus   ||
     *     || _____________ || 10.dp <- focusable modifier #2
     *     ||| ___________ ||| 20.dp <- focusable modifier #3
     *     ||||           ||||
     *     ||||___________||||
     *     |||_____________|||
     *     ||_______________||
     *     |_________________|
     */
    @Test
    fun firstFocusableModifier_getsFocused_with_outermostBounds() {
        val focusStates = Array(3) { false }
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 300.dp, height = 300.dp)) {
                Box(
                    Modifier.testTag("multiple-modifiers")
                        .fillMaxSize()
                        .onFocusChanged { focusStates[0] = it.isFocused }
                        .focusable()
                        .padding(10.dp)
                        .onFocusChanged { focusStates[1] = it.isFocused }
                        .focusable()
                        .padding(10.dp)
                        .onFocusChanged { focusStates[2] = it.isFocused }
                        .focusable()
                )
            }
        }

        requestFocusForRectAndCheck(
            l = 150.dp,
            t = 5.dp,
            r = 150.dp,
            b = 5.dp,
            expectedFocusedTag = "multiple-modifiers",
        )

        Truth.assertThat(focusStates[0]).isTrue()
        Truth.assertThat(focusStates[1]).isFalse()
        Truth.assertThat(focusStates[2]).isFalse()
    }

    /**
     *      _________________  0.dp <- LayoutNode
     *     |                 |
     *     |    □<-focus     |
     *     |                 |
     *     |  _____________  | 100.dp <- offset
     *     | |             | |
     *     | |  focusable  | |
     *     | |  modifier   | |
     *     | |    with     | |
     *     | |   offset    | |
     *     | |_____________| |
     *     |_________________| 300.dp
     */
    @Test
    fun internalLogic_respects_modifierOffsets_focusIsOutsideFocusableBounds() {
        rule.setContent {
            Column(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 300.dp, height = 300.dp)) {
                Box(
                    Modifier.testTag("focusable-offset")
                        .fillMaxSize()
                        .offset(x = 100.dp, y = 100.dp)
                        .focusable()
                )
            }
        }

        val focusArea = DpRect(50.dp, 50.dp, 50.dp, 50.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isFalse()
        rule.onNodeWithTag("focusable-offset").assertIsNotFocused()
    }

    /**
     *       LayoutNode    LayoutNode
     *         bounds        bounds
     *      (container)     (child)
     *      ___________   ___________
     *     |           | |           |
     *     |           | |  □<-focus |
     *     |           | |           |
     *     |___________| |___________|
     *     0         100 110        210
     */
    @Test
    fun layoutNode_thatIs_outside_of_containerLayoutNodeBounds_willBeFocused() {
        rule.setContent {
            // We need that big box on the outside, or the test rule
            // will clip the content by the size of the root composable.
            Box(Modifier.requiredSize(width = 300.dp, height = 100.dp)) {
                // Container
                Box(Modifier.testTag(CONTAINER_TAG).requiredSize(100.dp)) {
                    // Auxiliary node to move the child LayoutNode outside of the container.
                    Box(Modifier.requiredSize(100.dp).offset(x = 110.dp, y = 0.dp).focusable()) {
                        // Child
                        Box(Modifier.testTag("child-outside-bounds").fillMaxSize().focusable())
                    }
                }
            }
        }

        val focusArea = DpRect(150.dp, 50.dp, 150.dp, 50.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isTrue()
        rule.onNodeWithTag("child-outside-bounds").assertIsFocused()
    }

    /**
     *       LayoutNode   Modifier.Node
     *         bounds        bounds
     *      ___________   ___________
     *     |           | |           |
     *     |           | |  □<-focus |
     *     |           | |           |
     *     |___________| |___________|
     *     0         100 110        210
     */
    @Test
    fun modifierNode_that_doesNotOverlap_with_layoutNode_willNotBeFocused() {
        rule.setContent {
            Box(Modifier.requiredSize(width = 300.dp, height = 100.dp)) {
                Box(Modifier.testTag(CONTAINER_TAG).requiredSize(width = 100.dp, height = 100.dp)) {
                    Box(
                        Modifier.testTag("focusable-offset")
                            .requiredSize(100.dp, 100.dp)
                            .offset(x = 110.dp, y = 0.dp)
                            .focusable()
                    )
                }
            }
        }

        val focusArea = DpRect(150.dp, 50.dp, 150.dp, 50.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isFalse()
        rule.onNodeWithTag("focusable-offset").assertIsNotFocused()
    }

    /**
     *      LayoutNode   Modifier.Node
     *        bounds        bounds
     *      ________________________
     *     |        |   |           |
     *     |        | □ |<- focus   |
     *     |        |   |           |
     *     |________|___|___________|
     *     0       90  100        190
     */
    @Test
    fun modifierNode_that_overlaps_with_layoutNode_willBeFocused() {
        rule.setContent {
            Box(Modifier.requiredSize(width = 300.dp, height = 100.dp)) {
                Box(Modifier.testTag(CONTAINER_TAG).requiredSize(100.dp)) {
                    Box(
                        Modifier.testTag("focusable-offset")
                            .requiredSize(100.dp)
                            .offset(x = 90.dp, y = 0.dp)
                            .focusable()
                    )
                }
            }
        }

        val focusArea = DpRect(95.dp, 50.dp, 95.dp, 50.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isTrue()
        rule.onNodeWithTag("focusable-offset").assertIsFocused()
    }

    /**
     *      _________________
     *     |    container    |
     *     |  _____________  | 0.dp
     *     | |  Sibling-A  | |
     *     | | □<-focus(1) | |
     *     | |_____________| | 100.dp
     *     | | overlap A-B | |
     *     | | □<-focus(2) | |
     *     | |_____________| | 200.dp
     *     | |             | |
     *     | |  Sibling-B  | |
     *     | |_____________| | 300.dp
     *     |_________________|
     */
    @Test
    fun overlappingSibling_focusTheFirstOne_andThen_focusOverlapArea() {
        rule.setContent {
            Layout(
                modifier = Modifier.testTag(CONTAINER_TAG),
                content = {
                    Box(
                        Modifier.testTag("sibling-A")
                            .requiredSize(width = 100.dp, height = 200.dp)
                            .focusable()
                    )
                    Box(
                        Modifier.testTag("sibling-B")
                            .requiredSize(width = 100.dp, height = 200.dp)
                            .focusable()
                    )
                },
            ) { measurables, constraints ->
                val siblingA = measurables[0].measure(constraints)
                val siblingB = measurables[1].measure(constraints)
                layout(width = 100.dp.roundToPx(), height = 300.dp.roundToPx()) {
                    siblingA.placeRelative(x = 0, y = 0)
                    siblingB.placeRelative(x = 0, y = 100.dp.roundToPx())
                }
            }
        }

        // Request focus for Sibling-A
        val firstFocusArea = DpRect(left = 50.dp, top = 50.dp, right = 50.dp, bottom = 50.dp)
        val firstFocusRequested =
            rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(firstFocusArea)

        // Check Sibling-A is focused
        Truth.assertThat(firstFocusRequested).isTrue()
        rule.onNodeWithTag("sibling-A").assertIsFocused()

        // Request focus for overlapping area
        val secondFocusArea = DpRect(left = 50.dp, top = 150.dp, right = 50.dp, bottom = 150.dp)
        val secondFocusRequested =
            rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(secondFocusArea)

        // Check Sibling-A is still focused
        Truth.assertThat(secondFocusRequested).isFalse() // since it's already focused
        rule.onNodeWithTag("sibling-A").assertIsFocused()
    }

    /**
     *      _________________
     *     |    container    |
     *     |  _____________  | 0.dp
     *     | |  Sibling-A  | |
     *     | |             | |
     *     | |_____________| | 100.dp
     *     | | overlap A-B | |
     *     | | □<-focus(2) | |
     *     | |_____________| | 200.dp
     *     | | □<-focus(1) | |
     *     | |  Sibling-B  | |
     *     | |_____________| | 300.dp
     *     |_________________|
     */
    @Test
    fun overlappingSibling_focusTheSecondOne_andThen_focusOverlapArea() {
        rule.setContent {
            Layout(
                modifier = Modifier.testTag(CONTAINER_TAG),
                content = {
                    Box(
                        Modifier.testTag("sibling-A")
                            .requiredSize(width = 100.dp, height = 200.dp)
                            .focusable()
                    )
                    Box(
                        Modifier.testTag("sibling-B")
                            .requiredSize(width = 100.dp, height = 200.dp)
                            .focusable()
                    )
                },
            ) { measurables, constraints ->
                val siblingA = measurables[0].measure(constraints)
                val siblingB = measurables[1].measure(constraints)
                layout(width = 100.dp.roundToPx(), height = 300.dp.roundToPx()) {
                    siblingA.placeRelative(x = 0, y = 0)
                    siblingB.placeRelative(x = 0, y = 100.dp.roundToPx())
                }
            }
        }

        // Request focus for Sibling-B
        val firstFocusArea = DpRect(left = 50.dp, top = 250.dp, right = 50.dp, bottom = 250.dp)
        val firstFocusRequested =
            rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(firstFocusArea)

        // Check Sibling-B is focused
        Truth.assertThat(firstFocusRequested).isTrue()
        rule.onNodeWithTag("sibling-B").assertIsFocused()

        // Request focus for overlapping area
        val secondFocusArea = DpRect(left = 50.dp, top = 150.dp, right = 50.dp, bottom = 150.dp)
        val secondFocusRequested =
            rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(secondFocusArea)

        // Check Sibling-B is still focused
        Truth.assertThat(secondFocusRequested).isFalse() // since it's already focused
        rule.onNodeWithTag("sibling-B").assertIsFocused()
    }

    /**
     *      _________________
     *     |    container    |
     *     |  _____________  | 0.dp
     *     | |  Sibling-A  | |
     *     | |             | |
     *     | |_____________| | 100.dp
     *     | | overlap A-B | |
     *     | |   □<-focus  | |
     *     | |_____________| | 200.dp
     *     | |             | |
     *     | |  Sibling-B  | |
     *     | |_____________| | 300.dp
     *     |_________________|
     */
    @Test
    fun overlappingSiblings_when_requestFocusForOverlapArea_focusesTheFirstPlacedChild() {
        rule.setContent {
            Layout(
                modifier = Modifier.testTag(CONTAINER_TAG),
                content = {
                    Box(
                        Modifier.testTag("sibling-A")
                            .requiredSize(width = 100.dp, height = 200.dp)
                            .focusable()
                    )
                    Box(
                        Modifier.testTag("sibling-B")
                            .requiredSize(width = 100.dp, height = 200.dp)
                            .focusable()
                    )
                },
            ) { measurables, constraints ->
                val siblingA = measurables[0].measure(constraints)
                val siblingB = measurables[1].measure(constraints)
                layout(width = 100.dp.roundToPx(), height = 300.dp.roundToPx()) {
                    siblingA.placeRelative(x = 0, y = 0)
                    siblingB.placeRelative(x = 0, y = 100.dp.roundToPx())
                }
            }
        }

        // Click on overlapping
        val focusArea = DpRect(left = 50.dp, top = 150.dp, right = 50.dp, bottom = 150.dp)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        // Check Sibling-A is focused
        Truth.assertThat(focusRequested).isTrue()
        rule.onNodeWithTag("sibling-A").assertIsFocused()
    }

    private fun requestFocusForRectAndCheck(
        l: Dp,
        t: Dp,
        r: Dp,
        b: Dp,
        expectedFocusedTag: String,
    ) {
        rule.onNodeWithTag(expectedFocusedTag).assertIsNotFocused()

        val focusArea = DpRect(l, t, r, b)
        val focusRequested = rule.onNodeWithTag(CONTAINER_TAG).requestChildFocusInRect(focusArea)

        Truth.assertThat(focusRequested).isTrue()
        rule.onNodeWithTag(expectedFocusedTag).assertIsFocused()
    }

    private fun SemanticsNodeInteraction.requestChildFocusInRect(localRect: DpRect): Boolean {
        val localPixelRect = with(rule.density) { localRect.toRect() }
        val layoutNode = fetchSemanticsNode().layoutNode
        val modifierNode = layoutNode.nodes.head

        val coordinates = layoutNode.coordinates
        val globalTopLeft = coordinates.localToRoot(localPixelRect.topLeft)
        val globalBottomRight = coordinates.localToRoot(localPixelRect.bottomRight)

        return rule.runOnIdle {
            modifierNode.requestFocusForChildInRootBounds(
                left = globalTopLeft.x.fastRoundToInt(),
                top = globalTopLeft.y.fastRoundToInt(),
                right = globalBottomRight.x.fastRoundToInt(),
                bottom = globalBottomRight.y.fastRoundToInt(),
            )
        }
    }

    companion object {
        private const val CONTAINER_TAG: String = "container"
    }
}
