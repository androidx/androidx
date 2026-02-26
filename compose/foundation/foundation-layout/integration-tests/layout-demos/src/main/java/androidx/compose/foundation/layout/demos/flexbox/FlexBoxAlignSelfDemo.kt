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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexAlignSelf
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun FlexBoxAlignSelfDemo() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))

        Text(text = "Row with AlignItems.Start", fontSize = 20.sp)
        FlexBoxRowAlignSelfSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column with AlignItems.Start", fontSize = 20.sp)
        FlexBoxColumnAlignSelfSample()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignSelfSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Start)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "Tall", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "Start", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { alignSelf(FlexAlignSelf.End) }
        ) {
            Text(text = "End", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { alignSelf(FlexAlignSelf.Center) }
        ) {
            Text(text = "Center", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { alignSelf(FlexAlignSelf.Stretch) }
        ) {
            Text(text = "Stretch", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnAlignSelfSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            alignItems(FlexAlignItems.Start)
        },
        modifier = Modifier.height(300.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "Wide", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "Start", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { alignSelf(FlexAlignSelf.End) }
        ) {
            Text(text = "End", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { alignSelf(FlexAlignSelf.Center) }
        ) {
            Text(text = "Center", modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier =
                Modifier.height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { alignSelf(FlexAlignSelf.Stretch) }
        ) {
            Text(text = "Stretch", modifier = Modifier.align(Alignment.Center))
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(red = random.nextInt(256), green = random.nextInt(256), blue = random.nextInt(256))
}
