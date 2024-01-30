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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TextFieldLineLimitsDemos() {
    Column(Modifier.padding(16.dp)) {
        TagLine(tag = "Default")
        DefaultLineLimits()

        TagLine(tag = "Single")
        SingleLineLimits()

        TagLine(tag = "MultiLine")
        MultiLineLimits()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DefaultLineLimits() {
    Text("Default")

    BasicTextField2(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.Default,
        textStyle = LocalTextStyle.current,
        modifier = demoTextFieldModifiers
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineLimits() {
    Text("Single Line")

    BasicTextField2(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = LocalTextStyle.current,
        modifier = demoTextFieldModifiers
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiLineLimits() {
    Text("Multi Line")

    var minLines by remember { mutableIntStateOf(2) }
    var maxLines by remember { mutableIntStateOf(5) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Min: $minLines")
        Slider(
            value = minLines.toFloat(),
            onValueChange = {
                minLines = it.roundToInt()
            },
            valueRange = 1f..10f,
            steps = 9
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Max: $maxLines")
        Slider(
            value = maxLines.toFloat(),
            onValueChange = {
                maxLines = it.roundToInt()
            },
            valueRange = 1f..10f,
            steps = 9
        )
    }

    maxLines = maxLines.coerceAtLeast(minLines)

    BasicTextField2(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = minLines,
            maxHeightInLines = maxLines
        ),
        textStyle = LocalTextStyle.current,
        modifier = demoTextFieldModifiers
    )
}
