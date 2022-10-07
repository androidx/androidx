/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ChainsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testHorizontalPacked_withConstraintSet() {
        val rootSize = 100.dp
        val boxSizes = arrayOf(10.dp, 20.dp, 30.dp)
        val margin = 10.dp
        val constraintSet = ConstraintSet {
            val box0 = createRefFor("box0")
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")
            val chain0 = createHorizontalChain(box0, box1, chainStyle = ChainStyle.Packed)

            constrain(box0) {
                width = Dimension.value(boxSizes[0])
                height = Dimension.value(boxSizes[0])
                centerVerticallyTo(parent)
            }
            constrain(box1) {
                width = Dimension.value(boxSizes[1])
                height = Dimension.value(boxSizes[1])
                centerVerticallyTo(box0)
            }
            constrain(box2) {
                width = Dimension.value(boxSizes[2])
                height = Dimension.value(boxSizes[2])
                top.linkTo(parent.top, margin)
                start.linkTo(parent.start, margin)
            }
            constrain(chain0) {
                start.linkTo(box2.end, margin)
            }
        }
        rule.setContent {
            ConstraintLayout(
                modifier = Modifier.size(rootSize),
                constraintSet = constraintSet
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .layoutTestId("box0")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .layoutTestId("box1")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Green)
                        .layoutTestId("box2")
                )
            }
        }
        rule.waitForIdle()

        val spaceForChain = rootSize - boxSizes[2] - (margin * 2)
        val spaceAroundChain = spaceForChain - boxSizes[0] - boxSizes[1]
        val spaceAtLeftOfChain = spaceAroundChain * 0.5f
        val offsetFromBox2 = margin + boxSizes[2] + margin

        val box0Left = offsetFromBox2 + spaceAtLeftOfChain
        val box0Top = (rootSize - boxSizes[0]) * 0.5f

        val box1Left = box0Left + boxSizes[0] + 0.5.dp // 0.5dp, compensate for a small solver error
        val box1Top = box0Top - ((boxSizes[1] - boxSizes[0]) * 0.5f)

        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(box0Left, box0Top)
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(box1Left, box1Top)
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(margin, margin)
    }

    @Test
    fun testHorizontalPacked_withModifier() {
        val rootSize = 100.dp
        val boxSizes = arrayOf(10.dp, 20.dp, 30.dp)
        val margin = 10.dp
        rule.setContent {
            ConstraintLayout(
                Modifier
                    .background(Color.LightGray)
                    .size(rootSize)
            ) {
                val (box0, box1, box2) = createRefs()
                val chain0 = createHorizontalChain(box0, box1, chainStyle = ChainStyle.Packed)
                constrain(chain0) {
                    start.linkTo(box2.end, margin)
                }
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .constrainAs(box0) {
                            width = Dimension.value(boxSizes[0])
                            height = Dimension.value(boxSizes[0])
                            centerVerticallyTo(parent)
                        }
                        .layoutTestId("box0")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .constrainAs(box1) {
                            width = Dimension.value(boxSizes[1])
                            height = Dimension.value(boxSizes[1])
                            centerVerticallyTo(box0)
                        }
                        .layoutTestId("box1")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Green)
                        .constrainAs(box2) {
                            width = Dimension.value(boxSizes[2])
                            height = Dimension.value(boxSizes[2])
                            top.linkTo(parent.top, margin)
                            start.linkTo(parent.start, margin)
                        }
                        .layoutTestId("box2")
                )
            }
        }
        rule.waitForIdle()

        val spaceForChain = rootSize - boxSizes[2] - (margin * 2)
        val spaceAroundChain = spaceForChain - boxSizes[0] - boxSizes[1]
        val spaceAtLeftOfChain = spaceAroundChain * 0.5f
        val offsetFromBox2 = margin + boxSizes[2] + margin

        val box0Left = offsetFromBox2 + spaceAtLeftOfChain
        val box0Top = (rootSize - boxSizes[0]) * 0.5f

        val box1Left = box0Left + boxSizes[0] + 0.5.dp // 0.5dp, compensate for a small solver error
        val box1Top = box0Top - ((boxSizes[1] - boxSizes[0]) * 0.5f)

        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(box0Left, box0Top)
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(box1Left, box1Top)
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(margin, margin)
    }

    @Test
    fun testHorizontalPacked_withMargins() {
        val rootSize = 100.dp
        val boxSizes = arrayOf(10.dp, 20.dp, 30.dp)
        val boxMargin = 5.dp
        val boxGoneMargin = 7.dp
        val constraintSet = ConstraintSet {
            val box0 = createRefFor("box0")
            val boxGone = createRefFor("boxGone")
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")

            createHorizontalChain(
                box0.withChainParams(
                    startMargin = 0.dp,
                    endMargin = boxMargin, // Not applied since the next box is Gone
                    endGoneMargin = boxGoneMargin
                ),
                boxGone.withChainParams(
                    // None of these margins should apply since it's Gone
                    startMargin = 100.dp,
                    endMargin = 100.dp,
                    startGoneMargin = 100.dp,
                    endGoneMargin = 100.dp
                ),
                box1,
                box2.withHorizontalChainParams(startMargin = boxMargin, endMargin = 0.dp),
                chainStyle = ChainStyle.Packed
            )

            constrain(box0) {
                width = Dimension.value(boxSizes[0])
                height = Dimension.value(boxSizes[0])
                centerVerticallyTo(parent)
            }
            constrain(boxGone) {
                width = Dimension.value(boxSizes[1])
                height = Dimension.value(boxSizes[1])
                centerVerticallyTo(box0)

                visibility = Visibility.Gone
            }
            constrain(box1) {
                width = Dimension.value(boxSizes[1])
                height = Dimension.value(boxSizes[1])
                centerVerticallyTo(box0)
            }
            constrain(box2) {
                width = Dimension.value(boxSizes[2])
                height = Dimension.value(boxSizes[2])
                centerVerticallyTo(box0)
            }
        }
        rule.setContent {
            ConstraintLayout(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(rootSize),
                constraintSet = constraintSet
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .layoutTestId("box0")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .layoutTestId("box1")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Green)
                        .layoutTestId("box2")
                )
            }
        }
        rule.waitForIdle()

        val totalMargins = boxMargin + boxGoneMargin
        val totalChainSpace = boxSizes[0] + boxSizes[1] + boxSizes[2] + totalMargins

        val box0Left = (rootSize - totalChainSpace) * 0.5f
        val box0Top = (rootSize - boxSizes[0]) * 0.5f

        val box1Left = box0Left + boxSizes[0] + boxGoneMargin
        val box1Top = (rootSize - boxSizes[1]) * 0.5f

        val box2Left = box1Left + boxSizes[1] + boxMargin
        val box2Top = (rootSize - boxSizes[2]) * 0.5f

        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(box0Left, box0Top)
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(box1Left, box1Top)
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(box2Left, box2Top)
    }

    @Test
    fun testVerticalPacked_withMargins() {
        val rootSize = 100.dp
        val boxSizes = arrayOf(10.dp, 20.dp, 30.dp)
        val boxMargin = 5.dp
        val boxGoneMargin = 7.dp
        val constraintSet = ConstraintSet {
            val box0 = createRefFor("box0")
            val boxGone = createRefFor("boxGone")
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")

            createVerticalChain(
                box0.withChainParams(
                    topMargin = 0.dp,
                    bottomMargin = boxMargin, // Not applied since the next box is Gone
                    bottomGoneMargin = boxGoneMargin
                ),
                boxGone.withChainParams(
                    // None of these margins should apply since it's Gone
                    topMargin = 100.dp,
                    bottomMargin = 100.dp,
                    topGoneMargin = 100.dp,
                    bottomGoneMargin = 100.dp
                ),
                box1,
                box2.withVerticalChainParams(topMargin = boxMargin, bottomMargin = 0.dp),
                chainStyle = ChainStyle.Packed
            )

            constrain(box0) {
                width = Dimension.value(boxSizes[0])
                height = Dimension.value(boxSizes[0])
                centerHorizontallyTo(parent)
            }
            constrain(boxGone) {
                width = Dimension.value(100.dp) // Dimensions won't matter since it's Gone
                height = Dimension.value(100.dp) // Dimensions won't matter since it's Gone
                centerHorizontallyTo(box0)

                visibility = Visibility.Gone
            }
            constrain(box1) {
                width = Dimension.value(boxSizes[1])
                height = Dimension.value(boxSizes[1])
                centerHorizontallyTo(box0)
            }
            constrain(box2) {
                width = Dimension.value(boxSizes[2])
                height = Dimension.value(boxSizes[2])
                centerHorizontallyTo(box0)
            }
        }
        rule.setContent {
            ConstraintLayout(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(rootSize),
                constraintSet = constraintSet
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .layoutTestId("box0")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .layoutTestId("box1")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Green)
                        .layoutTestId("box2")
                )
            }
        }
        rule.waitForIdle()

        val totalMargins = boxMargin + boxGoneMargin
        val totalChainSpace = boxSizes[0] + boxSizes[1] + boxSizes[2] + totalMargins

        val box0Left = (rootSize - boxSizes[0]) * 0.5f
        val box0Top = (rootSize - totalChainSpace) * 0.5f

        val box1Left = (rootSize - boxSizes[1]) * 0.5f
        val box1Top = box0Top + boxSizes[0] + boxGoneMargin

        val box2Left = (rootSize - boxSizes[2]) * 0.5f
        val box2Top = box1Top + boxSizes[1] + boxMargin

        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(box0Left, box0Top)
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(box1Left, box1Top)
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(box2Left, box2Top)
    }

    @Test
    fun testHorizontalWeight_withConstraintSet() {
        val rootSize = 100.dp
        val boxSize = 10.dp

        rule.setContent {
            ConstraintLayout(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(rootSize),
                constraintSet = ConstraintSet {
                    val box0 = createRefFor("box0")
                    val box1 = createRefFor("box1")

                    constrain(box0) {
                        width = Dimension.fillToConstraints
                        height = Dimension.value(boxSize)

                        horizontalChainWeight = 1.0f
                        verticalChainWeight = 2.0f // Ignored in horizontal chain
                    }
                    constrain(box1) {
                        width = Dimension.fillToConstraints
                        height = Dimension.value(boxSize)
                    }
                    createHorizontalChain(box0, box1.withChainParams(weight = 0.5f))
                }
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .layoutTestId("box0")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .layoutTestId("box1")
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertWidthIsEqualTo(rootSize * 0.667f)
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(rootSize * 0.667f, 0.dp)
        rule.onNodeWithTag("box1").assertWidthIsEqualTo(rootSize * 0.334f)
    }

    @Test
    fun testVerticalWeight_withConstraintSet() {
        val rootSize = 100.dp
        val boxSize = 10.dp

        rule.setContent {
            ConstraintLayout(
                modifier = Modifier
                    .background(Color.LightGray)
                    .size(rootSize),
                constraintSet = ConstraintSet {
                    val box0 = createRefFor("box0")
                    val box1 = createRefFor("box1")

                    constrain(box0) {
                        width = Dimension.value(boxSize)
                        height = Dimension.fillToConstraints

                        horizontalChainWeight = 2.0f // Ignored in vertical chain
                        verticalChainWeight = 1.0f
                    }
                    constrain(box1) {
                        width = Dimension.value(boxSize)
                        height = Dimension.fillToConstraints
                    }
                    createVerticalChain(box0, box1.withChainParams(weight = 0.5f))
                }
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .layoutTestId("box0")
                )
                Box(
                    modifier = Modifier
                        .background(Color.Blue)
                        .layoutTestId("box1")
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("box0").assertHeightIsEqualTo(rootSize * 0.667f)
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(0.dp, rootSize * 0.667f)
        rule.onNodeWithTag("box1").assertHeightIsEqualTo(rootSize * 0.334f)
    }
}