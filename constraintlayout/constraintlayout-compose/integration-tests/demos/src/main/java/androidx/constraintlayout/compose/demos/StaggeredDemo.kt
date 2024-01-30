/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene

private const val STAGGERED_VALUE = 0.4f

@Preview
@Composable
fun SimpleStaggeredDemo() {
    var mode by remember { mutableStateOf(StaggeredMode.Normal) }
    var animateToEnd by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animateToEnd) 1f else 0f,
        animationSpec = tween(3000),
    )
    val boxesId: IntArray = remember { IntArray(10) { it } }
    val staggeredValue by remember {
        derivedStateOf {
            when (mode) {
                StaggeredMode.Inverted -> -STAGGERED_VALUE
                else -> STAGGERED_VALUE
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        MotionLayout(
            remember(mode) {
                MotionScene {
                    val refs = boxesId.map { createRefFor(it) }.toTypedArray()
                    val weights = when (mode) {
                        StaggeredMode.Custom -> boxesId.map { it.toFloat() }.shuffled()
                        else -> boxesId.map { Float.NaN }
                    }

                    defaultTransition(
                        constraintSet {
                            createHorizontalChain(*refs, chainStyle = ChainStyle.Packed(0f))
                            refs.forEachIndexed { index, ref ->
                                constrain(ref) {
                                    staggeredWeight = weights[index]
                                }
                            }
                        },
                        constraintSet {
                            createVerticalChain(*refs, chainStyle = ChainStyle.Packed(1f))
                            constrain(*refs) {
                                end.linkTo(parent.end)
                            }
                        }
                    ) {
                        maxStaggerDelay = staggeredValue
                    }
                }
            },
            progress = progress,
            Modifier
                .fillMaxWidth()
                .weight(1f, true)
        ) {
            for (id in boxesId) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .background(Color.Red)
                        .layoutId(id)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { animateToEnd = !animateToEnd }) {
                Text(text = "Run")
            }
            Button(
                onClick = {
                    mode = when (mode) {
                        StaggeredMode.Normal -> StaggeredMode.Inverted
                        StaggeredMode.Inverted -> StaggeredMode.Custom
                        else -> StaggeredMode.Normal
                    }
                }
            ) {
                Text(text = "Mode: ${mode.name}, Value: $staggeredValue")
            }
        }
    }
}

private enum class StaggeredMode {
    Normal,
    Inverted,
    Custom
}
