/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material.samples

import android.view.MotionEvent
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SimplePicker() {
    val items = listOf("One", "Two", "Three", "Four", "Five")
    val state = rememberPickerState(items.size)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            text = "Selected: ${items[state.selectedOption]}"
        )
        Picker(
            modifier = Modifier.size(100.dp, 100.dp),
            state = state
        ) {
            Text(items[it])
        }
    }
}

@Sampled
@Composable
fun OptionChangePicker() {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberPickerState(initialNumberOfOptions = 10)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(top = 20.dp).align(Alignment.TopCenter),
            text = "Selected: ${state.selectedOption}"
        )
        Picker(
            state = state,
            modifier = Modifier
                .size(200.dp, 200.dp)
                .background(color = Color.Black),
            separation = 20.dp
        ) {
            CompactChip(
                onClick = {
                    coroutineScope.launch { state.scrollToOption(it) }
                },
                label = {
                    Text("$it")
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun DualPicker() {
    var selectedColumn by remember { mutableStateOf(0) }
    val textStyle = MaterialTheme.typography.display1
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Picker(
            readOnly = selectedColumn != 0,
            state = rememberPickerState(initialNumberOfOptions = 12,
                initiallySelectedOption = 6),
            modifier = Modifier.size(64.dp, 100.dp), separation = (-10).dp,
            readOnlyLabel = {
                Text(text = "Hour", style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 8.dp)
                )
            }
        ) { hour: Int -> Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "%02d".format(hour + 1), style = textStyle,
                color = if (selectedColumn == 0) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .align(Alignment.Center).wrapContentSize()
                    .pointerInteropFilter {
                    if (it.action == MotionEvent.ACTION_DOWN) selectedColumn = 0
                    true
                }
            ) }
        }
        Spacer(Modifier.width(8.dp))
        Text(text = ":", style = textStyle, color = MaterialTheme.colors.onBackground)
        Spacer(Modifier.width(8.dp))
        Picker(
            readOnly = selectedColumn != 1,
            state = rememberPickerState(initialNumberOfOptions = 60),
            modifier = Modifier.size(64.dp, 100.dp), separation = (-10).dp,
            readOnlyLabel = {
                Text(text = "Minute", style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 8.dp)
                )
            }
        ) { minute: Int -> Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "%02d".format(minute), style = textStyle,
                color = if (selectedColumn == 1) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .align(Alignment.Center).wrapContentSize()
                    .pointerInteropFilter {
                    if (it.action == MotionEvent.ACTION_DOWN) selectedColumn = 1
                    true
                }
            ) }
        }
    }
}