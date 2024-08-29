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

package androidx.compose.animation.demos.lookahead

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Preview
@Composable
fun LookaheadWithDisappearingMovableContentDemo() {
    LookaheadScope {
        val isCompact by
            produceState(initialValue = false) {
                while (true) {
                    delay(3000)
                    value = !value
                }
            }
        Column {
            Box(Modifier.padding(start = 50.dp, top = 200.dp, bottom = 100.dp)) {
                val icon = remember { movableContentOf<Boolean> { MyIcon(it) } }
                val title = remember {
                    movableContentOf<Boolean> {
                        Title(visible = it, Modifier.animatePosition(this@LookaheadScope))
                    }
                }
                val details = remember { movableContentOf<Boolean> { Details(visible = it) } }

                Row(
                    Modifier.background(Color.Yellow).animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCompact) {
                        icon(true)
                        Column {
                            title(true)
                            details(true)
                        }
                    } else {
                        icon(false)
                        Column {
                            title(true)
                            details(false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyIcon(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut() + slideOutHorizontally { -it },
        modifier = modifier
    ) {
        Box(modifier.size(40.dp).background(color = Color.Red, CircleShape))
    }
}

@Composable
fun Title(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text("Text", modifier, fontSize = 30.sp)
    }
}

@Composable
fun Details(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        Text("Detailed Text", fontSize = 18.sp)
    }
}

@OptIn(ExperimentalAnimatableApi::class)
@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.animatePosition(lookaheadScope: LookaheadScope): Modifier =
    with(lookaheadScope) {
        composed {
            val offsetAnimation = remember { DeferredTargetAnimation(IntOffset.VectorConverter) }
            val coroutineScope = rememberCoroutineScope()
            this.approachLayout(
                isMeasurementApproachInProgress = { false },
                isPlacementApproachInProgress = {
                    offsetAnimation.updateTarget(
                        lookaheadScopeCoordinates.localLookaheadPositionOf(it).round(),
                        coroutineScope,
                        spring(stiffness = Spring.StiffnessMediumLow)
                    )
                    !offsetAnimation.isIdle
                }
            ) { measurable, constraints ->
                measurable.measure(constraints).run {
                    layout(width, height) {
                        val (x, y) =
                            coordinates?.let { coordinates ->
                                val origin = this.lookaheadScopeCoordinates
                                val animOffset =
                                    offsetAnimation.updateTarget(
                                        origin.localLookaheadPositionOf(coordinates).round(),
                                        coroutineScope,
                                        spring(stiffness = Spring.StiffnessMediumLow),
                                    )
                                val currentOffset = origin.localPositionOf(coordinates, Offset.Zero)
                                animOffset - currentOffset.round()
                            } ?: IntOffset.Zero
                        place(x, y)
                    }
                }
            }
        }
    }
