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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material.InlineSlider

@Sampled
@Composable
fun InlineSliderSample() {
    var value by remember { mutableStateOf(4.5f) }
    InlineSlider(
        value = value,
        onValueChange = { value = it },
        valueRange = 3f..6f,
        steps = 5,
        segmented = false
    )
}

@Sampled
@Composable
fun InlineSliderSegmentedSample() {
    var value by remember { mutableStateOf(2f) }
    InlineSlider(
        value = value,
        onValueChange = { value = it },
        valueRange = 1f..4f,
        steps = 7,
        segmented = true
    )
}
