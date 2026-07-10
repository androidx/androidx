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
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun SimpleRowFlexBox() {

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Row", fontSize = 32.sp)
        FlexBoxRowDemo()

        Spacer(Modifier.height(24.dp))

        Text(text = "Wrap", fontSize = 32.sp)
        FlexBoxRowWrapDemo()

        Spacer(Modifier.height(24.dp))

        Text(text = "RowReverse", fontSize = 32.sp)
        FlexBoxRowReverseDemo()
        Spacer(Modifier.height(24.dp))

        Text(text = "WrapReverse", fontSize = 32.sp)
        FlexBoxRowWrapReverseDemo()
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowDemo() {
    FlexBox(config = { direction(FlexDirection.Row) }, modifier = Modifier.fillMaxWidth()) {
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
private fun FlexBoxRowWrapDemo() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
        }
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

// RowReverse sample
@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowReverseDemo() {
    FlexBox(config = { direction(FlexDirection.RowReverse) }, modifier = Modifier.fillMaxWidth()) {
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
private fun FlexBoxRowWrapReverseDemo() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.WrapReverse)
        }
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
