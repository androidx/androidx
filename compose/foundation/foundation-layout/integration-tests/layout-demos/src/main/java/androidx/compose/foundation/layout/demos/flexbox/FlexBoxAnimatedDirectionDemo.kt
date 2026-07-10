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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import kotlin.random.Random

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
fun FlexBoxAnimatedDirectionDemo() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var isRow by remember { mutableStateOf(true) }

        Button(onClick = { isRow = !isRow }, modifier = Modifier.padding(16.dp)) {
            Text(if (isRow) "Switch to Column" else "Switch to Row")
        }

        LookaheadScope {
            FlexBox(
                config = { direction(if (isRow) FlexDirection.Row else FlexDirection.Column) },
                modifier =
                    Modifier.fillMaxWidth().height(400.dp).padding(16.dp).border(1.dp, Color.Gray),
            ) {
                // We add some items
                for (i in 1..5) {
                    Box(
                        modifier =
                            Modifier.animateBounds(
                                    lookaheadScope = this@LookaheadScope,
                                    modifier =
                                        Modifier.width(if (isRow) 50.dp else 100.dp)
                                            .height(if (isRow) 100.dp else 50.dp),
                                )
                                .background(color = remember { randomColor() })
                                .border(1.dp, Color.Black)
                                .wrapContentSize(Alignment.Center)
                    ) {
                        Text(text = i.toString(), modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(red = random.nextInt(256), green = random.nextInt(256), blue = random.nextInt(256))
}
