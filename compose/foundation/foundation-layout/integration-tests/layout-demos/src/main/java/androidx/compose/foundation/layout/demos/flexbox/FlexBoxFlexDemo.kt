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
import androidx.compose.foundation.layout.FlexBasis
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
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
fun FlexBoxFlexDemo() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))

        Text(text = "Row with flexGrow", fontSize = 20.sp)
        FlexBoxRowFlexGrowSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row with flexShrink", fontSize = 20.sp)
        FlexBoxRowFlexShrinkSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row with flexBasis", fontSize = 20.sp)
        FlexBoxRowFlexBasisSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column with flexGrow", fontSize = 20.sp)
        FlexBoxColumnFlexGrowSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column with flexShrink", fontSize = 20.sp)
        FlexBoxColumnFlexShrinkSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column with flexBasis", fontSize = 20.sp)
        FlexBoxColumnFlexBasisSample()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowFlexGrowSample() {
    FlexBox(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "50dp", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex {
                        grow(1f)
                        basis(50.dp)
                    }
        ) {
            Text(text = "grow=1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "50dp", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowFlexShrinkSample() {
    FlexBox(modifier = Modifier.width(300.dp).border(1.dp, Color.Black)) {
        Box(
            modifier =
                Modifier.width(150.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "150dp", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(150.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { shrink(0f) }
        ) {
            Text(text = "shrink=0", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(150.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "150dp", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowFlexBasisSample() {
    FlexBox(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { basis(100.dp) }
        ) {
            Text(text = "basis=100dp", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex {
                        basis(50.dp)
                        grow(1f)
                    }
        ) {
            Text(text = "basis=50dp, grow=1", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnFlexGrowSample() {
    FlexBox(
        config = { direction(FlexDirection.Column) },
        modifier = Modifier.height(300.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .fillMaxWidth()
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "50dp", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .fillMaxWidth()
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { grow(1f) }
        ) {
            Text(text = "grow=1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .fillMaxWidth()
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "50dp", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnFlexShrinkSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
        },
        modifier = Modifier.height(300.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(150.dp)
                    .fillMaxWidth()
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "150dp", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(150.dp)
                    .fillMaxWidth()
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { shrink(0f) }
        ) {
            Text(text = "shrink=0", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(150.dp)
                    .fillMaxWidth()
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "150dp", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnFlexBasisSample() {
    FlexBox(
        config = { direction(FlexDirection.Column) },
        modifier = Modifier.height(300.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex { basis(FlexBasis.Dp(100.dp)) }
        ) {
            Text(text = "basis=100dp", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
                    .flex {
                        basis(50.dp)
                        grow(1f)
                    }
        ) {
            Text(text = "basis=50dp, grow=1", modifier = Modifier.align(Alignment.Center))
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(red = random.nextInt(256), green = random.nextInt(256), blue = random.nextInt(256))
}
