/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.scrollbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun ScrollbarWithLazyColumnSample() {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier =
            Modifier.fillMaxSize()
                .scrollbar(state.scrollIndicatorState, orientation = Orientation.Vertical),
    ) {
        items(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.fillMaxWidth().height(50.dp).wrapContentSize(Alignment.Center),
            )
        }
    }
}

@Sampled
@Composable
fun ScrollbarWithVerticalScrollSample() {
    val state = rememberScrollState()

    Column(
        modifier =
            Modifier.fillMaxSize()
                // Chain before verticalScroll so the scrollbar doesn't scroll with content.
                .scrollbar(state.scrollIndicatorState, orientation = Orientation.Vertical)
                .verticalScroll(state)
    ) {
        repeat(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.fillMaxWidth().height(50.dp).wrapContentSize(Alignment.Center),
            )
        }
    }
}
