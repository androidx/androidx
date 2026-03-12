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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun SimpleColumnFlexBox() {

    Row(modifier = Modifier.height(500.dp).horizontalScroll(rememberScrollState())) {
        Spacer(Modifier.width(24.dp))

        Column {
            Text(text = "Column", fontSize = 16.sp)
            FlexBoxColumnDemo()
        }

        Spacer(Modifier.width(24.dp))
        Column(modifier = Modifier.height(500.dp)) {
            Text(text = "Wrap", fontSize = 16.sp)
            FlexBoxColumnWrapDemo()
        }

        Spacer(Modifier.width(24.dp))

        Column(modifier = Modifier.height(500.dp)) {
            Text(text = "ColumnReverse", fontSize = 16.sp)
            FlexBoxColumnReverseDemo()
        }
        Spacer(Modifier.width(24.dp))

        Column(modifier = Modifier.height(500.dp)) {
            Text(text = "WrapReverse", fontSize = 16.sp)
            FlexBoxColumnWrapReverseDemo()
        }
        Spacer(Modifier.width(24.dp))
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnDemo() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            rowGap(6.dp)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.size(50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnWrapDemo() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
        },
        modifier = Modifier.fillMaxHeight(),
    ) {
        repeat(10) {
            Box(
                modifier =
                    Modifier.size(50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ColumnReverse sample
@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnReverseDemo() {
    FlexBox(
        config = { direction(FlexDirection.ColumnReverse) },
        modifier = Modifier.fillMaxHeight(),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.size(50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// WrapReverse sample

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnWrapReverseDemo() {
    FlexBox(
        config = {
            direction(FlexDirection.ColumnReverse)
            wrap(FlexWrap.WrapReverse)
        },
        modifier = Modifier.fillMaxHeight(),
    ) {
        repeat(10) {
            Box(
                modifier =
                    Modifier.size(50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(red = random.nextInt(256), green = random.nextInt(256), blue = random.nextInt(256))
}
