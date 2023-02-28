/*
 * Copyright (C) 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for motions using the Grid Helper
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MotionGridTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setup() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun rowToColumnGrid() {
        val rootSize = 200.dp
        var animateToEnd by mutableStateOf(false)
        rule.setContent {
            val progress by animateFloatAsState(targetValue = if (animateToEnd) 1f else 0f)

            MotionLayout(
                modifier = Modifier
                    .size(rootSize),
                motionScene = MotionScene(
                    """
            {
                ConstraintSets: {
                  start: {
                    grid1: {
                        width: 200,
                        height: 200,
                        type: "grid",
                        orientation: 0,
                        rows: 0,
                        columns: 1,
                        hGap: 0,
                        vGap: 0,
                        spans: "",
                        skips: "",
                        rowWeights: "",
                        columnWeights: "",
                        contains: ["box1", "box2", "box3", "box4"],
                      }
                  },
                  end: {
                    grid2: {
                        width: 200,
                        height: 200,
                        type: "grid",
                        orientation: 0,
                        rows: 1,
                        columns: 0,
                        hGap: 0,
                        vGap: 0,
                        spans: "",
                        skips: "",
                        rowWeights: "",
                        columnWeights: "",
                        contains: ["box1", "box2", "box3", "box4"],
                      }
                  }
                },
                Transitions: {
                  default: {
                    from: 'start', to: 'end',
                  }
                }
            }
            """
                ),
                progress = progress
            ) {
                val numArray = arrayOf("box1", "box2", "box3", "box4")
                for (num in numArray) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .layoutId(num)
                            .testTag(num)
                            .background(Color.Red)
                    )
                }
            }
        }
        rule.waitForIdle()

        val boxesCount = 4
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        var hGapSize = (rootSize - 10.dp) / 2f
        var vGapSize = (rootSize - (10.dp * 4f)) / (boxesCount * 2f)
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box4").assertPositionInRootIsEqualTo(expectedX, expectedY)

        animateToEnd = true
        rule.waitForIdle()

        expectedX = 0.dp
        expectedY = 0.dp

        // 10.dp is the size of a singular box
        hGapSize = (rootSize - (10.dp * 4f)) / (boxesCount * 2f)
        vGapSize = (rootSize - 10.dp) / 2f

        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box4").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @OptIn(ExperimentalMotionApi::class)
    @Test
    fun wrapToSpreadGrid() {
        val rootSize = 200.dp
        var animateToEnd by mutableStateOf(false)
        val hGap = 10.dp
        val vGap = 20.dp
        val hGapVal = Math.round(hGap.value)
        val vGapVal = Math.round(vGap.value)
        rule.setContent {
            val progress by animateFloatAsState(targetValue = if (animateToEnd) 1f else 0f)

            MotionLayout(
                modifier = Modifier
                    .size(rootSize),
                motionScene = MotionScene(
                    """
            {
                ConstraintSets: {
                  start: {
                    grid1: {
                        width: 200,
                        height: 200,
                        type: "grid",
                        orientation: 0,
                        rows: 2,
                        columns: 2,
                        hGap: 0,
                        vGap: 0,
                        contains: ["box1", "box2", "box3", "box4"],
                      }
                  },
                  end: {
                    grid2: {
                        width: 200,
                        height: 200,
                        type: "grid",
                        orientation: 0,
                        rows: 2,
                        columns: 2,
                        hGap: $hGapVal,
                        vGap: $vGapVal,
                        contains: ["box1", "box2", "box3", "box4"],
                      },
                      box1: {
                        width: 'spread',
                        height: 'spread',
                      },
                      box2: {
                        width: 'spread',
                        height: 'spread',
                      },
                      box3: {
                        width: 'spread',
                        height: 'spread',
                      },
                      box4: {
                        width: 'spread',
                        height: 'spread',
                      }
                  }
                },
                Transitions: {
                  default: {
                    from: 'start', to: 'end',
                  }
                }
            }
            """
                ),
                progress = progress
            ) {
                val numArray = arrayOf("box1", "box2", "box3", "box4")
                for (num in numArray) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .layoutId(num)
                            .testTag(num)
                            .background(Color.Red)
                    )
                }
            }
        }
        rule.waitForIdle()

        val columns = 2
        var leftX = 0.dp
        var topY = 0.dp
        var rightX: Dp
        var bottomY: Dp

        // 10.dp is the size of a singular box
        val gapSize = (rootSize - (10.dp * 2f)) / (columns * 2f)
        rule.waitForIdle()
        leftX += gapSize
        topY += gapSize
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(leftX, topY)
        rightX = leftX + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(rightX, topY)
        bottomY = topY + 10.dp + gapSize + gapSize
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(leftX, bottomY)
        rule.onNodeWithTag("box4").assertPositionInRootIsEqualTo(rightX, bottomY)

        animateToEnd = true
        rule.waitForIdle()

        var expectedLeft = 0.dp
        var expectedTop = 0.dp

        val boxWidth = (rootSize - hGap) / 2f
        val boxHeight = (rootSize - vGap) / 2f

        rule.waitForIdle()
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(0.dp, 0.dp)
        expectedLeft += boxWidth + hGap
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedLeft, 0.dp)
        expectedTop += boxHeight + vGap
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(0.dp, expectedTop)
        rule.onNodeWithTag("box4").assertPositionInRootIsEqualTo(expectedLeft, expectedTop)
    }

    // convert dp to px
    @Composable
    private fun dpToPx(dpVal: Dp): Float {
        val density = LocalDensity.current
        return with(density) { dpVal.toPx() }
    }
}