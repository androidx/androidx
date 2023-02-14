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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnSwipeTest {
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

    @Ignore("Fails on online devices, box left position is 48 instead of 51.6")
    @Test
    fun simpleCornerToCornerRightSwipe_Json() {
        testMotionLayoutSwipe { OnSwipeTestJson() }
    }

    @Ignore("Fails on online devices, box left position is 48 instead of 51.6")
    @Test
    fun simpleCornerToCornerRightSwipe_Dsl() {
        testMotionLayoutSwipe { OnSwipeTestDsl() }
    }

    private fun testMotionLayoutSwipe(content: @Composable () -> Unit) {
        rule.setContent(content)
        rule.waitForIdle()
        val motionSemantic = rule.onNodeWithTag("MyMotion")
        motionSemantic
            .assertExists()
            .performTouchInput {
                // Do a periodic swipe between two points that lasts 500ms
                val start = Offset(right * 0.25f, centerY)
                val end = Offset(right * 0.5f, centerY)
                val durationMillis = 500L
                val durationMillisFloat = durationMillis.toFloat()

                // Start touch input
                down(0, start)

                val steps = (durationMillisFloat / eventPeriodMillis.toFloat()).roundToInt()
                var step = 0

                val getPositionAt: (Long) -> Offset = {
                    lerp(start, end, it.toFloat() / durationMillis)
                }

                var tP = 0L
                while (step++ < steps) {
                    val progress = step / steps.toFloat()
                    val tn = lerp(0, durationMillis, progress)
                    updatePointerTo(0, getPositionAt(tn))
                    move(tn - tP)
                    tP = tn
                }
            }
        rule.onNodeWithTag("box").assertPositionInRootIsEqualTo(51.6.dp, 128.3.dp)
        motionSemantic
            .performTouchInput {
                up()
            }
        // Wait a frame for the Touch Up animation to start
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.onNodeWithTag("box").assertPositionInRootIsEqualTo(170.dp, 10.dp)
    }
}

@Composable
private fun OnSwipeTestJson() {
    MotionLayout(
        modifier = Modifier
            .testTag("MyMotion")
            .size(200.dp),
        motionScene = MotionScene(
            content = """
       {
         ConstraintSets: {
           start: {
             box: {
               width: 20, height: 20,
               bottom: ['parent', 'bottom', 10],
               start: ['parent', 'start', 10]
             }
           },
           end: {
             Extends: 'start',
             box: {
               clear: ['constraints'],
               top: ['parent', 'top', 10],
               end: ['parent', 'end', 10],
             }
           }
         },
         Transitions: {
           default: {
              from: 'start',
              to: 'end',
              onSwipe: {
                anchor: 'box',
                direction: 'end',
                side: 'end',
                mode: 'spring',
                touchUp: 'neverCompleteStart',
                stopThreshold: 0.0001
              }
           }
         }
       }
        """.trimIndent()
        ),
        progress = 0.0f
    ) {
        Box(
            modifier = Modifier
                .background(Color.Red)
                .layoutId("box")
                .testTag("box")
        )
    }
}

@Composable
private fun OnSwipeTestDsl() {
    MotionLayout(
        modifier = Modifier
            .testTag("MyMotion")
            .size(200.dp),
        motionScene = MotionScene {
            val box = createRefFor("box")
            val from: ConstraintSetRef = constraintSet {
                constrain(box) {
                    width = Dimension.value(20.dp)
                    height = Dimension.value(20.dp)
                    bottom.linkTo(parent.bottom, 10.dp)
                    start.linkTo(parent.start, 10.dp)
                }
            }
            val to = constraintSet(extendConstraintSet = from) {
                constrain(box) {
                    clearConstraints()
                    top.linkTo(parent.top, 10.dp)
                    end.linkTo(parent.end, 10.dp)
                }
            }
            defaultTransition(from, to) {
                onSwipe = OnSwipe(
                    anchor = box,
                    direction = SwipeDirection.End,
                    side = SwipeSide.End,
                    mode = SwipeMode.Spring(threshold = 0.0001f),
                    onTouchUp = SwipeTouchUp.NeverCompleteStart,
                )
            }
        },
        progress = 0.0f
    ) {
        Box(
            modifier = Modifier
                .background(Color.Red)
                .layoutId("box")
                .testTag("box")
        )
    }
}