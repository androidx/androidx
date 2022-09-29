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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MotionFlowTest {
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
    fun horizontalToVerticalFlow() {
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
                    flow1: {
                        width: 'parent',
                        height: 'parent',
                        type: 'hFlow',
                        wrap: 'none',
                        centerVertically: 'parent',
                        centerHorizontally: 'parent',
                        contains: ['1', '2', '3', '4'],
                      }
                  },
                  end: {
                    flow2: {
                        width: 'parent',
                        height: 'parent',
                        type: 'vFlow',
                        wrap: 'none',
                        centerVertically: 'parent',
                        centerHorizontally: 'parent',
                        contains: ['1', '2', '3', '4'],
                      }
                  }
                },
                Transitions: {
                  default: {
                    from: 'start',   to: 'end',
                  }
                }
            }
            """
                ),
                progress = progress
            ) {
                val numArray = arrayOf("1", "2", "3", "4")
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

        // (rootSize - (boxSize * boxCount)) / (boxCount + 1)
        val gapSize = (rootSize - (10.dp * 4)) / 5

        var expectedPosition = gapSize
        rule.onNodeWithTag("1").assertPositionInRootIsEqualTo(expectedPosition, 94.9.dp)
        expectedPosition += gapSize + 10.dp
        rule.onNodeWithTag("2").assertPositionInRootIsEqualTo(expectedPosition, 94.9.dp)
        expectedPosition += gapSize + 10.dp
        rule.onNodeWithTag("3").assertPositionInRootIsEqualTo(expectedPosition, 94.9.dp)
        expectedPosition += gapSize + 10.dp
        rule.onNodeWithTag("4").assertPositionInRootIsEqualTo(expectedPosition, 94.9.dp)

        animateToEnd = true
        rule.waitForIdle()

        expectedPosition = gapSize
        rule.onNodeWithTag("1").assertPositionInRootIsEqualTo(94.9.dp, expectedPosition)
        expectedPosition += gapSize + 10.dp
        rule.onNodeWithTag("2").assertPositionInRootIsEqualTo(94.9.dp, expectedPosition)
        expectedPosition += gapSize + 10.dp
        rule.onNodeWithTag("3").assertPositionInRootIsEqualTo(94.9.dp, expectedPosition)
        expectedPosition += gapSize + 10.dp
        rule.onNodeWithTag("4").assertPositionInRootIsEqualTo(94.9.dp, expectedPosition)
    }
}