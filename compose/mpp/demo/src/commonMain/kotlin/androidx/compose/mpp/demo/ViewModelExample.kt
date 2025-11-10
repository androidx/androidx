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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(SavedStateHandleSaveableApi::class)
private class DemoViewModel(
    savedState: SavedStateHandle
) : ViewModel() {
    var counter by savedState.saveable { mutableStateOf(0) }
}

@Composable
private fun DemoItem(index: Int) {
    val viewModel = viewModel<DemoViewModel>(key = index.toString()) {
        DemoViewModel(createSavedStateHandle())
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("[$index] Counter: ${viewModel.counter}")
        Spacer(Modifier.weight(1f))
        Button(onClick = { viewModel.counter++ }) {
            Text("Increment")
        }
    }
}

@Composable
fun ViewModelExample() {
    LazyColumn(Modifier.fillMaxSize()) {
        items(100) { index -> DemoItem(index) }
    }
}