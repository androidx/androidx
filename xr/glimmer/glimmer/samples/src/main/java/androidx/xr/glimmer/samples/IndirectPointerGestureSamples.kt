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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.onIndirectPointerGesture
import java.time.Instant

@Composable
fun OnIndirectPointerGestureSampleUsage() {
    var log by remember { mutableStateOf("") }

    Column(
        modifier =
            Modifier.onIndirectPointerGesture(
                    enabled = true,
                    onSwipeForward = { log = "${Instant.now()} - onSwipeForward\n" + log },
                    onSwipeBackward = { log = "${Instant.now()} - onSwipeBackward\n" + log },
                    onClick = { log = "${Instant.now()} - onClick\n" + log },
                )
                .focusTarget()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Perform gestures to see the actions in the UI", fontSize = 16.sp)
        Text(log, fontSize = 14.sp)
    }
}

@Composable
@Sampled
fun OnIndirectPointerGestureSample() {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .onIndirectPointerGesture(
                    enabled = true,
                    onSwipeForward = { /* onSwipeForward */ },
                    onSwipeBackward = { /* onSwipeBackward */ },
                    onClick = { /* onClick */ },
                )
                .focusTarget()
    ) {
        // App()
    }
}

@Preview
@Composable
private fun OnIndirectPointerGestureSampleUsagePreview() {
    GlimmerTheme { OnIndirectPointerGestureSampleUsage() }
}
