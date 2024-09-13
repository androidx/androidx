/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SimplePicker() {
    val items = listOf("One", "Two", "Three", "Four", "Five")
    val state = rememberPickerState(items.size)
    val contentDescription by remember { derivedStateOf { "${state.selectedOptionIndex + 1}" } }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            text = "Selected: ${items[state.selectedOptionIndex]}"
        )
        Picker(
            modifier = Modifier.size(100.dp, 100.dp),
            state = state,
            contentDescription = contentDescription,
        ) {
            Text(items[it])
        }
    }
}

@Sampled
@Composable
fun PickerScrollToOption() {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberPickerState(initialNumberOfOptions = 10)
    val contentDescription by remember { derivedStateOf { "${state.selectedOptionIndex + 1}" } }
    Picker(
        state = state,
        spacing = 4.dp,
        contentDescription = contentDescription,
    ) {
        Button(
            onClick = { coroutineScope.launch { state.scrollToOption(it) } },
            label = { Text("$it") }
        )
    }
}

@Sampled
@Composable
fun PickerAnimateScrollToOption() {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberPickerState(initialNumberOfOptions = 10)
    val contentDescription by remember { derivedStateOf { "${state.selectedOptionIndex + 1}" } }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Picker(
            state = state,
            spacing = 4.dp,
            contentDescription = contentDescription,
        ) {
            Button(
                onClick = { coroutineScope.launch { state.animateScrollToOption(it) } },
                label = { Text("$it") }
            )
        }
    }
}
