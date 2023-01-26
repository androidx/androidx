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
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionLayoutDebugFlags
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.layoutId
import androidx.constraintlayout.compose.rememberMotionLayoutState

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