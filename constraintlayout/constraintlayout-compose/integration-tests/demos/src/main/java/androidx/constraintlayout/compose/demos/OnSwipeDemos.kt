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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose.demos

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionLayoutDebugFlags
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.OnSwipe
import androidx.constraintlayout.compose.SwipeDirection
import androidx.constraintlayout.compose.SwipeMode
import androidx.constraintlayout.compose.SwipeSide
import androidx.constraintlayout.compose.SwipeTouchUp
import androidx.constraintlayout.compose.layoutId
import androidx.constraintlayout.compose.rememberMotionLayoutState

/**
 * Shows how to define swipe-driven transitions with `KeyPositions` and custom colors using the
 * JSON5 syntax for MotionLayout.
 */
@Preview
@Composable
fun SimpleOnSwipe() {
    var mode by remember {
        mutableStateOf("spring")
    }
    var toEnd by remember { mutableStateOf(true) }
    val motionLayoutState = rememberMotionLayoutState(key = mode)

    val motionSceneContent = remember(mode) {
        // language=json5
        """
       {
         Header: { exportAs: 'swipeExpr' },
         ConstraintSets: {
           start: {
             box: {
               width: 50, height: 50,
               bottom: ['parent', 'bottom', 10],
               start: ['parent', 'start', 10],
               custom: {
                 bColor: '#ff0000'
               }
             }
           },
           end: {
             Extends: 'start',
             box: {
               clear: ['constraints'],
                  width: 100, height: 400,
               top: ['parent', 'top', 10],
               end: ['parent', 'end', 10],
               custom: {
                 bColor: '#0000ff'
               }
             }
           }
         },
         Transitions: {
           default: {
              from: 'start',
              to: 'end',
              KeyFrames: {
                KeyPositions: [{
                  target: ['box'],
                  frames: [25, 50, 75],
                  percentX: [0.25, 0.5, 0.75],
                  percentY: [0.25, 0.5, 0.75]
                }],
              },
              onSwipe: {
                anchor: 'box',
                direction: 'end',
                side: 'start',
                mode: '$mode',
                springMass: 1,
                springDamping: 10,
                springStiffness: 70,
              }
           }
         }
       }
        """.trimIndent()
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { motionLayoutState.snapTo(0f) }) {
                Text(text = "Reset")
            }
            Button(onClick = {
                val target = if (toEnd) 1f else 0f
                motionLayoutState.animateTo(target, tween(2000))
                toEnd = !toEnd
            }) {
                Text(text = if (toEnd) "End" else "Start")
            }
            Button(onClick = {
                if (motionLayoutState.isInDebugMode) {
                    motionLayoutState.setDebugMode(MotionLayoutDebugFlags.NONE)
                } else {
                    motionLayoutState.setDebugMode(MotionLayoutDebugFlags.SHOW_ALL)
                }
            }) {
                Text("Debug")
            }
            Button(onClick = {
                mode = when (mode) {
                    "spring" -> "velocity"
                    else -> "spring"
                }
            }) {
                Text(text = "Mode: $mode")
            }
        }
        MotionLayout(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f, fill = true),
            motionLayoutState = motionLayoutState,
            motionScene = MotionScene(content = motionSceneContent)
        ) {
            Box(
                modifier = Modifier
                    .background(customProperties(id = "box").color("bColor"))
                    .layoutId("box")
            )
        }
        Text(text = "Current progress: ${motionLayoutState.currentProgress}")
    }
}

@Preview
@Composable
fun MultiSwipeDsl() {
    val modes = remember { arrayOf(SwipeMode.Velocity, SwipeMode.Spring) }
    val touchUps = remember {
        arrayOf(
            SwipeTouchUp.AutoComplete,
            SwipeTouchUp.ToStart,
            SwipeTouchUp.ToEnd,
            SwipeTouchUp.Stop,
            SwipeTouchUp.Decelerate,
            SwipeTouchUp.NeverCompleteStart,
            SwipeTouchUp.NeverCompleteEnd,
        )
    }
    val endWidth = arrayOf(50, 200)
    val simpleSwipeConfigs = remember {
        val configCombinations = mutableListOf<SimpleSwipeConfig>()
        touchUps.forEach { touchUp ->
            endWidth.forEach { width ->
                modes.forEach { mode ->
                    configCombinations.add(
                        SimpleSwipeConfig(
                            mode,
                            width,
                            touchUp
                        )
                    )
                }
            }
        }
        return@remember configCombinations
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        simpleSwipeConfigs.forEach { config ->
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )
            SimpleSwipeDsl(config)
        }
    }
}

@Stable
@Immutable
private data class SimpleSwipeConfig(
    val mode: SwipeMode,
    val endWidth: Int,
    val touchUp: SwipeTouchUp
)

@Composable
private fun SimpleSwipeDsl(config: SimpleSwipeConfig) {
    val mode = config.mode
    val endWidth = config.endWidth
    val touchUp = config.touchUp
    val titleText = "(${mode.name} $endWidth ${touchUp.name})"

    MotionLayout(
        modifier = Modifier
            .height(70.dp)
            .fillMaxWidth()
            .background(Color.White),
        motionScene = MotionScene {
            val title = createRefFor("title")
            val box = createRefFor("box")

            val from = constraintSet {
                constrain(title) {
                    width = Dimension.wrapContent
                    height = Dimension.value(50.dp)
                    centerTo(parent)
                    customFloat("mValue", 0.0f)
                    customColor("back", Color(0xffffffff))
                }
                constrain(box) {
                    width = Dimension.value(50.dp)
                    height = Dimension.value(50.dp)
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                    start.linkTo(parent.start, 70.dp)
                    rotationZ = 0f
                    customColor("boxColor", Color(0xff00ffff))
                }
            }
            val to = constraintSet(extendConstraintSet = from) {
                constrain(title) {
                    customFloat("mValue", 100.0f)
                    customColor("back", Color(0xffFF88FF))
                }
                constrain(box) {
                    width = Dimension.value(endWidth.dp)
                    clearHorizontal()
                    end.linkTo(parent.end, 70.dp)
                    rotationZ = 360f
                    customColor("boxColor", Color(0xFF00FF00))
                }
            }
            defaultTransition(
                from = from,
                to = to
            ) {
                onSwipe = OnSwipe(
                    anchor = box,
                    direction = SwipeDirection.Right,
                    side = SwipeSide.Left,
                    mode = mode,
                    onTouchUp = touchUp
                )
            }
        },
        progress = 0f
    ) {
        val progress = customFloat("title", "mValue")
        val textBackColor = customColor("title", "back")

        Text(
            text = "$titleText  $progress",
            modifier = Modifier
                .layoutId("title")
                .background(textBackColor),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .background(customProperties("box").color("boxColor"))
                .layoutId("box")
        )
    }
}