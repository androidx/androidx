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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.LinearProgressIndicator
import androidx.xr.glimmer.LinearWavyProgressIndicator

@Sampled
@Composable
fun LinearProgressIndicatorSample() {
    val progress = 0.5f

    // Animate progress changes, as the indicator doesn't animate between values.
    val animatedProgress by animateFloatAsState(targetValue = progress)
    LinearProgressIndicator(progress = { animatedProgress })
}

@Sampled
@Composable
fun IndeterminateLinearProgressIndicatorSample() {
    LinearProgressIndicator()
}

@Sampled
@Composable
fun LinearWavyProgressIndicatorSample() {
    val progress = 0.5f

    // Animate progress changes, as the indicator doesn't animate between values.
    val animatedProgress by animateFloatAsState(targetValue = progress)
    LinearWavyProgressIndicator(progress = { animatedProgress })
}

@Sampled
@Composable
fun IndeterminateLinearWavyProgressIndicatorSample() {
    LinearWavyProgressIndicator()
}

@Preview
@Composable
private fun LinearProgressIndicatorPreview() {
    GlimmerTheme { LinearProgressIndicatorSample() }
}

@Preview
@Composable
private fun IndeterminateLinearProgressIndicatorPreview() {
    GlimmerTheme { IndeterminateLinearProgressIndicatorSample() }
}

@Preview
@Composable
private fun LinearWavyProgressIndicatorPreview() {
    GlimmerTheme { LinearWavyProgressIndicatorSample() }
}

@Preview
@Composable
private fun IndeterminateLinearWavyProgressIndicatorPreview() {
    GlimmerTheme { IndeterminateLinearWavyProgressIndicatorSample() }
}
