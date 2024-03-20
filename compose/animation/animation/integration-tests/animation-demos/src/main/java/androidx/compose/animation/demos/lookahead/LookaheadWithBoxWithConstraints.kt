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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.demos.gesture.pastelColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun LookaheadWithBoxWithConstraints() {
    Box(Modifier.fillMaxSize()) {
        LookaheadScope {
            Column {
                var halfSize by remember { mutableStateOf(false) }
                Button(onClick = { halfSize = !halfSize }, Modifier.padding(20.dp).fillMaxWidth()) {
                    Text(if (halfSize) "Full Size" else "Half Size")
                }
                Column(
                    Modifier.fillMaxHeight()
                        .animateBounds(
                            this@LookaheadScope,
                            if (halfSize) Modifier.fillMaxSize(0.5f) else Modifier.fillMaxWidth()
                        )
                        .background(pastelColors[2]),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        Modifier.border(1.dp, Color.Black, RoundedCornerShape(5.dp))
                            .padding(top = 20.dp, bottom = 20.dp)
                    ) {
                        Text("Regular Row: ")
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MyButton()
                            MyButton()
                        }
                    }
                    Column {
                        var animate by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { animate = true }
                        ) {
                            RadioButton(selected = animate, onClick = { animate = true })
                            Text("Animate Bounds")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { animate = false }
                        ) {
                            RadioButton(selected = !animate, onClick = { animate = false })
                            Text("No animation")
                        }
                        BoxWithConstraints {
                            Column(
                                if (animate) {
                                        Modifier.animateBounds(
                                            lookaheadScope = this@LookaheadScope,
                                            Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                    .then(
                                        Modifier.border(1.dp, Color.Black, RoundedCornerShape(5.dp))
                                            .padding(top = 20.dp, bottom = 20.dp)
                                    ),
                            ) {
                                Text("SubcomposeLayout: ")
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MyButton()
                                    MyButton()
                                }
                            }
                        }

                        BoxWithConstraints {
                            if (maxWidth > 300.dp) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MyButton()
                                    MyButton()
                                }
                            } else {
                                Column {
                                    MyButton()
                                    MyButton()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.MyButton() {
    Box(
        modifier =
            Modifier.weight(1f)
                .padding(5.dp)
                .height(40.dp)
                .background(pastelColors[0], RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Text("Button")
    }
}

@Composable
fun MyButton() {
    Box(
        modifier =
            Modifier.fillMaxWidth(0.5f)
                .padding(5.dp)
                .height(40.dp)
                .background(pastelColors[0], RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Text("Button")
    }
}
