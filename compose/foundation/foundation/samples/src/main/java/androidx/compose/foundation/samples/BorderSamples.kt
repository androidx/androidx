/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@Sampled
fun BorderSample() {
    Text("Text with  square border", modifier = Modifier.border(4.dp, Color.Magenta).padding(10.dp))
}

@Composable
@Sampled
fun BorderSampleWithBrush() {
    val gradientBrush =
        Brush.horizontalGradient(
            colors = listOf(Color.Red, Color.Blue, Color.Green),
            startX = 0.0f,
            endX = 500.0f,
            tileMode = TileMode.Repeated
        )
    Text(
        "Text with gradient border",
        modifier =
            Modifier.border(width = 2.dp, brush = gradientBrush, shape = CircleShape).padding(10.dp)
    )
}

@Composable
@Sampled
fun BorderSampleWithDataClass() {
    Text(
        "Text with gradient border",
        modifier =
            Modifier.border(border = BorderStroke(2.dp, Color.Blue), shape = CutCornerShape(8.dp))
                .padding(10.dp)
    )
}

@Composable
@Sampled
fun BorderSampleWithDynamicData() {
    val widthRange = (1..10)
    var width by remember { mutableStateOf((widthRange.random()).dp) }

    val shapes = remember { listOf(CutCornerShape(8.dp), CircleShape, RoundedCornerShape(20)) }
    var selectedShape by remember { mutableStateOf(shapes.random()) }

    val colors =
        listOf(
            Color.Black,
            Color.DarkGray,
            Color.Gray,
            Color.LightGray,
            Color.White,
            Color.Red,
            Color.Blue,
            Color.Green,
            Color.Yellow,
            Color.Cyan,
            Color.Magenta
        )
    var gradientBrush by remember {
        mutableStateOf(
            Brush.horizontalGradient(
                colors = listOf(colors.random(), colors.random(), colors.random()),
                startX = 0.0f,
                endX = 500.0f,
                tileMode = TileMode.Repeated
            )
        )
    }

    Column(Modifier.padding(2.dp)) {
        Text(text = "Update border with buttons")
        Row {
            Button(
                modifier = Modifier.width(60.dp),
                onClick = { width = (widthRange.random()).dp }
            ) {
                Text(fontSize = 8.sp, text = "width")
            }
            Button(
                modifier = Modifier.width(60.dp),
                onClick = {
                    gradientBrush =
                        Brush.horizontalGradient(
                            colors = listOf(colors.random(), colors.random(), colors.random()),
                            startX = 0.0f,
                            endX = 500.0f,
                            tileMode = TileMode.Repeated
                        )
                }
            ) {
                Text(fontSize = 8.sp, text = "brush")
            }
            Button(
                modifier = Modifier.width(60.dp),
                onClick = { selectedShape = shapes.random() }
            ) {
                Text(fontSize = 8.sp, text = "shape")
            }
        }
        Text(
            "Dynamic border",
            modifier =
                Modifier.border(width = width, brush = gradientBrush, shape = selectedShape)
                    .padding(10.dp)
        )
    }
}
