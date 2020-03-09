/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.graphics.Color
import androidx.ui.material.Slider
import androidx.ui.material.SliderPosition

@Sampled
@Composable
fun SliderSample() {
    Slider(SliderPosition())
}

@Sampled
@Composable
fun StepsSliderSample() {
    val position = SliderPosition(
        initial = 0f,
        valueRange = 0f..100f,
        steps = 5
    )
    Slider(position, color = Color.Black)
}