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

package androidx.compose.ui.graphics.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun GradientBrushSample() {
    Column(modifier = Modifier.fillMaxSize().wrapContentSize()) {

        // Create a linear gradient that shows red in the top left corner
        // and blue in the bottom right corner
        val linear = Brush.linearGradient(listOf(Color.Red, Color.Blue))

        Box(modifier = Modifier.size(120.dp).background(linear))

        Spacer(modifier = Modifier.size(20.dp))

        // Create a radial gradient centered about the drawing area that is green on
        // the outer
        // edge of the circle and magenta towards the center of the circle
        val radial = Brush.radialGradient(listOf(Color.Green, Color.Magenta))
        Box(modifier = Modifier.size(120.dp).background(radial))

        Spacer(modifier = Modifier.size(20.dp))

        // Create a radial gradient centered about the drawing area that is green on
        // the outer
        // edge of the circle and magenta towards the center of the circle
        val sweep = Brush.sweepGradient(listOf(Color.Cyan, Color.Magenta))
        Box(modifier = Modifier.size(120.dp).background(sweep))
    }
}

@Sampled
fun LinearGradientColorStopSample() {
    Brush.linearGradient(
        0.0f to Color.Red,
        0.3f to Color.Green,
        1.0f to Color.Blue,
        start = Offset(0.0f, 50.0f),
        end = Offset(0.0f, 100.0f)
    )
}

@Sampled
fun LinearGradientSample() {
    Brush.linearGradient(
        listOf(Color.Red, Color.Green, Color.Blue),
        start = Offset(0.0f, 50.0f),
        end = Offset(0.0f, 100.0f)
    )
}

@Sampled
fun HorizontalGradientSample() {
    Brush.horizontalGradient(
        listOf(Color.Red, Color.Green, Color.Blue),
        startX = 10.0f,
        endX = 20.0f
    )
}

@Sampled
fun HorizontalGradientColorStopSample() {
    Brush.horizontalGradient(
        0.0f to Color.Red,
        0.3f to Color.Green,
        1.0f to Color.Blue,
        startX = 0.0f,
        endX = 100.0f
    )
}

@Sampled
fun VerticalGradientColorStopSample() {
    Brush.verticalGradient(
        0.1f to Color.Red,
        0.3f to Color.Green,
        0.5f to Color.Blue,
        startY = 0.0f,
        endY = 100.0f
    )
}

@Sampled
fun VerticalGradientSample() {
    Brush.verticalGradient(
        listOf(Color.Red, Color.Green, Color.Blue),
        startY = 0.0f,
        endY = 100.0f
    )
}

@Sampled
fun RadialBrushColorStopSample() {
    val side1 = 100
    val side2 = 200
    Brush.radialGradient(
        0.0f to Color.Red,
        0.3f to Color.Green,
        1.0f to Color.Blue,
        center = Offset(side1 / 2.0f, side2 / 2.0f),
        radius = side1 / 2.0f,
        tileMode = TileMode.Repeated
    )
}

@Sampled
fun RadialBrushSample() {
    val side1 = 100
    val side2 = 200
    Brush.radialGradient(
        listOf(Color.Red, Color.Green, Color.Blue),
        center = Offset(side1 / 2.0f, side2 / 2.0f),
        radius = side1 / 2.0f,
        tileMode = TileMode.Repeated
    )
}

@Sampled
fun SweepGradientColorStopSample() {
    Brush.sweepGradient(
        0.0f to Color.Red,
        0.3f to Color.Green,
        1.0f to Color.Blue,
        center = Offset(0.0f, 100.0f)
    )
}

@Sampled
fun SweepGradientSample() {
    Brush.sweepGradient(
        listOf(Color.Red, Color.Green, Color.Blue),
        center = Offset(10.0f, 20.0f)
    )
}