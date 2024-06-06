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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun ScrollIndicatorWithSLCSample() {
    val scrollState = rememberScalingLazyListState()
    Box {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
            items(100) { Text(text = "Item $it") }
        }
        ScrollIndicator(modifier = Modifier.align(Alignment.CenterEnd), state = scrollState)
    }
}

@Sampled
@Composable
fun ScrollIndicatorWithLCSample() {
    val scrollState = rememberLazyListState()
    Box {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = scrollState
        ) {
            items(100) { Text(text = "Item $it") }
        }
        ScrollIndicator(modifier = Modifier.align(Alignment.CenterEnd), state = scrollState)
    }
}

@Sampled
@Composable
fun ScrollIndicatorWithColumnSample() {
    val scrollState = rememberScrollState()
    Box {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(100) { Text(text = "Item $it") }
        }
        ScrollIndicator(modifier = Modifier.align(Alignment.CenterEnd), state = scrollState)
    }
}
