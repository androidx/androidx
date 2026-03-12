/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.layout.demos.flexbox

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
fun FlexBoxAnimatedOrderDemo() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val itemCount = 12

        var itemOrders by remember { mutableStateOf(IntArray(itemCount) { it }) }

        Button(
            onClick = {
                val newOrder = itemOrders.clone()
                newOrder.shuffle()
                itemOrders = newOrder
            },
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Shuffle Order")
        }

        LookaheadScope {
            FlexBox(
                config = {
                    direction(FlexDirection.Row)
                    wrap(FlexWrap.Wrap)
                    gap(8.dp)
                },
                modifier =
                    Modifier.fillMaxWidth().padding(16.dp).border(1.dp, Color.Gray).padding(16.dp),
            ) {
                for (i in 0 until itemCount) {
                    Box(
                        modifier =
                            Modifier.flex { order(itemOrders[i]) }
                                .animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    modifier = Modifier.size(80.dp),
                                )
                                .background(color = remember(i) { randomColor() })
                                .border(1.dp, Color.Black)
                    ) {
                        Text(
                            text = "${i + 1}",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(
        red = 150 + random.nextInt(106),
        green = 150 + random.nextInt(106),
        blue = 150 + random.nextInt(106),
    )
}
