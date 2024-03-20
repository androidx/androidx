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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookaheadWithFlowRowDemo() {
    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        var isHorizontal by remember { mutableStateOf(true) }

        Button(
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
            onClick = { isHorizontal = !isHorizontal }
        ) {
            Text("Toggle")
        }
        Column(Modifier.background(Color(0xfffdedac), RoundedCornerShape(10)).padding(10.dp)) {
            Text("LookaheadScope + Modifier.animateBounds")
            LookaheadScope {
                FlowRow(
                    modifier =
                        Modifier.height(200.dp)
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.CenterStart)
                ) {
                    Box(
                        Modifier.height(50.dp)
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                Modifier.fillMaxWidth(if (isHorizontal) 0.4f else 1f)
                            )
                            .background(colors[0], RoundedCornerShape(10))
                    )
                    Box(
                        Modifier.height(50.dp)
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                Modifier.fillMaxWidth(if (isHorizontal) 0.2f else 0.4f)
                            )
                            .background(colors[1], RoundedCornerShape(10))
                    )
                    Box(
                        Modifier.height(50.dp)
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                Modifier.fillMaxWidth(if (isHorizontal) 0.2f else 0.4f)
                            )
                            .background(colors[2], RoundedCornerShape(10))
                    )
                }
                Box(Modifier.size(if (isHorizontal) 100.dp else 60.dp))
            }
        }

        Spacer(Modifier.size(50.dp))

        Column(Modifier.background(Color(0xfffdedac), RoundedCornerShape(10)).padding(10.dp)) {
            Text("Animating Width")
            FlowRow(
                modifier =
                    Modifier.height(200.dp).fillMaxWidth().wrapContentSize(Alignment.CenterStart)
            ) {
                Box(
                    Modifier.height(50.dp)
                        .fillMaxWidth(animateFloatAsState(if (isHorizontal) 0.4f else 1f).value)
                        .background(colors[0], RoundedCornerShape(10))
                )
                Box(
                    Modifier.height(50.dp)
                        .fillMaxWidth(animateFloatAsState(if (isHorizontal) 0.2f else 0.4f).value)
                        .background(colors[1], RoundedCornerShape(10))
                )
                Box(
                    Modifier.height(50.dp)
                        .fillMaxWidth(animateFloatAsState(if (isHorizontal) 0.2f else 0.4f).value)
                        .background(colors[2], RoundedCornerShape(10))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun NestedFlowRowDemo() {
    LookaheadScope {
        FlowRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center,
            maxItemsInEachRow = 3
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier =
                    Modifier.animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            Modifier.widthIn(max = 600.dp)
                        )
                        .background(Color.Red)
            ) {
                val height = animateDpAsState(targetValue = if (expanded) 500.dp else 300.dp)
                Box(
                    modifier =
                        Modifier.animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                Modifier.fillMaxWidth().height(height.value)
                            )
                            .clickable { expanded = !expanded }
                )
            }

            FlowColumn(
                Modifier.layout { measurable, constraints ->
                    measurable.measure(constraints).run { layout(width, height) { place(0, 0) } }
                }
            ) {
                Box(
                    modifier =
                        Modifier.size(200.dp)
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                Modifier.wrapContentWidth().heightIn(min = 156.dp),
                            )
                            .background(Color.Blue)
                ) {
                    Box(modifier = Modifier.size(200.dp))
                }
                Box(
                    modifier =
                        Modifier.size(200.dp)
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                Modifier.wrapContentWidth().heightIn(min = 156.dp),
                            )
                            .background(Color.Yellow)
                ) {
                    Box(modifier = Modifier.size(200.dp))
                }
            }
        }
    }
}

private val colors =
    listOf(Color(0xffff6f69), Color(0xffffcc5c), Color(0xff2a9d84), Color(0xff264653))
