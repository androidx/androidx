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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.FullScreenProgressIndicatorSample
import androidx.wear.compose.material3.samples.LinearProgressIndicatorSample
import androidx.wear.compose.material3.samples.MediaButtonProgressIndicatorSample
import androidx.wear.compose.material3.samples.OverflowProgressIndicatorSample
import androidx.wear.compose.material3.samples.SegmentedProgressIndicatorOnOffSample
import androidx.wear.compose.material3.samples.SegmentedProgressIndicatorSample
import androidx.wear.compose.material3.samples.SmallValuesProgressIndicatorSample

val ProgressIndicatorDemos =
    listOf(
        ComposableDemo("Full screen") { Centralize { FullScreenProgressIndicatorSample() } },
        ComposableDemo("Media button wrapping") {
            Centralize { MediaButtonProgressIndicatorSample() }
        },
        ComposableDemo("Overflow progress (>100%)") {
            Centralize { OverflowProgressIndicatorSample() }
        },
        ComposableDemo("Small sized indicator") {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                Button({ /* No op */ }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Loading...", modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.size(10.dp))
                    CircularProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier.size(IconButtonDefaults.DefaultButtonSize),
                        startAngle = 120f,
                        endAngle = 60f,
                        strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                        colors = ProgressIndicatorDefaults.colors(indicatorColor = Color.Red)
                    )
                }
            }
        },
        ComposableDemo("Small progress values") {
            Centralize { SmallValuesProgressIndicatorSample() }
        },
        ComposableDemo("Segmented progress") { Centralize { SegmentedProgressIndicatorSample() } },
        ComposableDemo("Progress segments on/off") {
            Centralize { SegmentedProgressIndicatorOnOffSample() }
        },
        ComposableDemo("Linear progress indicator") {
            Centralize { LinearProgressIndicatorSamples() }
        },
    )

@Composable
fun LinearProgressIndicatorSamples() {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text("Progress 0%") } }
            item { LinearProgressIndicatorSample(progress = { 0f }) }
            item { ListHeader { Text("Progress 1%") } }
            item { LinearProgressIndicatorSample(progress = { 0.01f }) }
            item { ListHeader { Text("Progress 50%") } }
            item { LinearProgressIndicatorSample(progress = { 0.5f }) }
            item { ListHeader { Text("Progress 100%") } }
            item { LinearProgressIndicatorSample(progress = { 1f }) }
            item { ListHeader { Text("Disabled 0%") } }
            item { LinearProgressIndicatorSample(progress = { 0f }, enabled = false) }
            item { ListHeader { Text("Disabled 50%") } }
            item { LinearProgressIndicatorSample(progress = { 0.5f }, enabled = false) }
            item { ListHeader { Text("Disabled 100%") } }
            item { LinearProgressIndicatorSample(progress = { 1f }, enabled = false) }
        }
    }
}
