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

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CompactChip
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