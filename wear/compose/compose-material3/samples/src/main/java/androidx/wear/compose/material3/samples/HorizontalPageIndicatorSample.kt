/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.InlineSlider
import androidx.wear.compose.material3.InlineSliderDefaults
import androidx.wear.compose.material3.PageIndicatorState
import androidx.wear.compose.material3.rememberPageIndicatorState

@OptIn(ExperimentalWearMaterial3Api::class)
@Sampled
@Composable
fun HorizontalPageIndicatorSample() {
    val maxPages = 9
    var selectedPage by remember { mutableStateOf(0) }
    var finalValue by remember { mutableStateOf(0) }

    val animatedSelectedPage by animateFloatAsState(
        targetValue = selectedPage.toFloat(), label = "animateSelectedPage",
    ) {
        finalValue = it.toInt()
    }

    val pageIndicatorState: PageIndicatorState =
        rememberPageIndicatorState(maxPages) { animatedSelectedPage }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        InlineSlider(
            modifier = Modifier.align(Alignment.Center),
            value = selectedPage,
            increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
            decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
            valueProgression = 0 until maxPages,
            onValueChange = { selectedPage = it }
        )
        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState
        )
    }
}
