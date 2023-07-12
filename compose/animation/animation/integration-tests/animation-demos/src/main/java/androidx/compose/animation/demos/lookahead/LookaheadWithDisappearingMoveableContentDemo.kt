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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.animation.demos.lookahead

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.intermediateLayout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LookaheadWithDisappearingMovableContentDemo() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(start = 50.dp, top = 200.dp)
    ) {
        LookaheadScope {
            val icon = remember {
                movableContentOf<Boolean> {
                    MyIcon(it, Modifier.animatePosition())
                }
            }
            val title = remember {
                movableContentOf<Boolean> {
                    Title(visible = it, Modifier.animatePosition())
                }
            }
            val details = remember {
                movableContentOf<Boolean> {
                    Details(visible = it, Modifier.animatePosition())
                }
            }

            val isCompact by produceState(initialValue = false) {
                while (true) {
                    delay(2000)
                    value = !value
                }
            }
            Row(Modifier.background(Color.Yellow), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
fun MyIcon(visible: Boolean, modifier: Modifier = Modifier) {
    AV2(visible, modifier) {
        Box(
            modifier
                .size(40.dp)
                .background(color = Color.Red, CircleShape)
        )
    }
}

@Composable
fun Title(visible: Boolean, modifier: Modifier = Modifier) {
    AV2(visible, modifier) {
        Text("Text", modifier, fontSize = 30.sp)
    }
}

@Composable
fun Details(visible: Boolean, modifier: Modifier = Modifier) {
    AV2(visible, modifier) {
        Text("Detailed Text", fontSize = 18.sp)
    }
}

@Composable
fun AV2(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier.then(
            if (!visible) Modifier
                .size(0.dp)
                .wrapContentSize() else Modifier
        )
    ) {
        content()
    }
}

context(LookaheadScope)
    @SuppressLint("UnnecessaryComposedModifier")
fun Modifier.animatePosition(): Modifier = composed {
    val offsetAnimation = remember {
        DeferredAnimation(IntOffset.VectorConverter)
    }
    this.intermediateLayout { measurable, constraints ->
        measurable.measure(constraints).run {
            layout(width, height) {
                val (x, y) =
                    coordinates?.let { coordinates ->
                        offsetAnimation.updateTarget(
                            lookaheadScopeCoordinates.localLookaheadPositionOf(
                                coordinates
                            )
                                .round(),
                            spring(),
                        )
                        val currentOffset =
                            lookaheadScopeCoordinates.localPositionOf(
                                coordinates,
                                Offset.Zero
                            )
                        (offsetAnimation.value
                            ?: offsetAnimation.target!!) - currentOffset.round()
                    } ?: IntOffset.Zero
                place(x, y)
            }
        }
    }
}