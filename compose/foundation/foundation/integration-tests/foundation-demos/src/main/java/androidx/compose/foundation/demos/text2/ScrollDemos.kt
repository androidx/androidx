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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text2.input.TextFieldLineLimits.SingleLine
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ScrollableDemos() {
    LazyColumn(Modifier.padding(16.dp)) {
        item {
            TagLine(tag = "SingleLine Horizontal Scroll")
            SingleLineHorizontalScrollableTextField()
        }

        item {
            TagLine(tag = "SingleLine Horizontal Scroll with newlines")
            SingleLineHorizontalScrollableTextFieldWithNewlines()
        }

        item {
            TagLine(tag = "SingleLine Vertical Scroll")
            SingleLineVerticalScrollableTextField()
        }

        item {
            TagLine(tag = "MultiLine Vertical Scroll")
            MultiLineVerticalScrollableTextField()
        }

        item {
            TagLine(tag = "Hoisted ScrollState")
            HoistedHorizontalScroll()
        }

        item {
            TagLine(tag = "Shared Hoisted ScrollState")
            SharedHoistedScroll()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineHorizontalScrollableTextField() {
    val state = remember {
        TextFieldState("When content gets long,this field should scroll horizontally")
    }
    BasicTextField2(
        state = state,
        lineLimits = SingleLine,
        textStyle = TextStyle(fontSize = 24.sp)
    )
}

// TODO this is not supported currently. Add tests for this when supported.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineHorizontalScrollableTextFieldWithNewlines() {
    val state = remember {
        TextFieldState("This \ntext \ncontains \nnewlines \nbut \nis \nsingle-line.")
    }
    BasicTextField2(
        state = state,
        lineLimits = SingleLine,
        textStyle = TextStyle(fontSize = 24.sp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineVerticalScrollableTextField() {
    val state = remember {
        TextFieldState(
            buildString {
                repeat(10) {
                    appendLine("When content gets long, this field should scroll vertically")
                }
            })
    }
    BasicTextField2(
        state = state,
        textStyle = TextStyle(fontSize = 24.sp),
        lineLimits = MultiLine(maxHeightInLines = 1)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiLineVerticalScrollableTextField() {
    val state = remember {
        TextFieldState(
            buildString {
                repeat(10) {
                    appendLine("When content gets long, this field should scroll vertically")
                }
            }
        )
    }
    BasicTextField2(
        state = state,
        textStyle = TextStyle(fontSize = 24.sp),
        modifier = Modifier.heightIn(max = 200.dp),
        lineLimits = MultiLine()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HoistedHorizontalScroll() {
    val state = remember {
        TextFieldState("When content gets long, this field should scroll horizontally")
    }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Column {
        Slider(
            value = scrollState.value.toFloat(),
            onValueChange = {
                coroutineScope.launch { scrollState.scrollTo(it.roundToInt()) }
            },
            valueRange = 0f..scrollState.maxValue.toFloat()
        )
        BasicTextField2(
            state = state,
            scrollState = scrollState,
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier.height(200.dp),
            lineLimits = SingleLine
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedHoistedScroll() {
    val state1 = remember {
        TextFieldState("When content gets long, this field should scroll horizontally")
    }
    val state2 = remember {
        TextFieldState("When content gets long, this field should scroll horizontally")
    }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Column {
        Slider(
            value = scrollState.value.toFloat(),
            onValueChange = {
                coroutineScope.launch { scrollState.scrollTo(it.roundToInt()) }
            },
            valueRange = 0f..scrollState.maxValue.toFloat()
        )
        BasicTextField2(
            state = state1,
            scrollState = scrollState,
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier.fillMaxWidth(),
            lineLimits = SingleLine
        )
        BasicTextField2(
            state = state2,
            scrollState = scrollState,
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier.fillMaxWidth(),
            lineLimits = SingleLine
        )
    }
}