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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.ContainedVoiceInputIndicator
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.VoiceInputIndicator
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

internal val VoiceInputIndicatorDemos =
    listOf(
        ComposableDemo("VoiceInputIndicator") { VoiceInputIndicatorDemo() },
        ComposableDemo("ContainedVoiceInputIndicator") { ContainedVoiceInputIndicatorDemo() },
    )

@Composable
private fun VoiceInputIndicatorDemo() {
    var level by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            level = Random.nextFloat()
            delay(200.milliseconds)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VoiceInputIndicator(level = { level })
    }
}

@Composable
private fun ContainedVoiceInputIndicatorDemo() {
    var level by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            level = Random.nextFloat()
            delay(200.milliseconds)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ContainedVoiceInputIndicator(level = { level })
    }
}

@Preview
@Composable
private fun VoiceInputIndicatorPreview() {
    GlimmerTheme { VoiceInputIndicatorDemo() }
}

@Preview
@Composable
private fun ContainedVoiceInputIndicatorPreview() {
    GlimmerTheme { ContainedVoiceInputIndicatorDemo() }
}
