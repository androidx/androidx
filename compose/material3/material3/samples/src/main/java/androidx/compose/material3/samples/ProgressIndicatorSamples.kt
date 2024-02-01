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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Slider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Sampled
@Composable
fun LinearProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            progress = { animatedProgress },
        )
        Spacer(Modifier.requiredHeight(30.dp))
        Slider(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp)
                .semantics {
                    val progressPercent = (progress * 100).toInt()
                    if (progressPercent in progressBreakpoints) {
                        stateDescription = "Progress $progressPercent%"
                    }
                },
            value = progress,
            valueRange = 0f..1f,
            steps = 100,
            onValueChange = {
                progress = it
            })
    }
}

@Preview
@Composable
fun LegacyLinearProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = null
        )
        Spacer(Modifier.requiredHeight(30.dp))
        Slider(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp)
                .semantics {
                    val progressPercent = (progress * 100).toInt()
                    if (progressPercent in progressBreakpoints) {
                        stateDescription = "Progress $progressPercent%"
                    }
                },
            value = progress,
            valueRange = 0f..1f,
            steps = 100,
            onValueChange = {
                progress = it
            })
    }
}

@Preview
@Sampled
@Composable
fun IndeterminateLinearProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator()
    }
}

@Preview
@Composable
fun LegacyIndeterminateLinearProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
    }
}

@Preview
@Sampled
@Composable
fun CircularProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(progress = { animatedProgress })
        Spacer(Modifier.requiredHeight(30.dp))
        Slider(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp)
                .semantics {
                    val progressPercent = (progress * 100).toInt()
                    if (progressPercent in progressBreakpoints) {
                        stateDescription = "Progress $progressPercent%"
                    }
                },
            value = progress,
            valueRange = 0f..1f,
            steps = 100,
            onValueChange = {
                progress = it
            })
    }
}

@Preview
@Composable
fun LegacyCircularProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.1f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
        Spacer(Modifier.requiredHeight(30.dp))
        Slider(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp)
                .semantics {
                    val progressPercent = (progress * 100).toInt()
                    if (progressPercent in progressBreakpoints) {
                        stateDescription = "Progress $progressPercent%"
                    }
                },
            value = progress,
            valueRange = 0f..1f,
            steps = 100,
            onValueChange = {
                progress = it
            })
    }
}

@Preview
@Sampled
@Composable
fun IndeterminateCircularProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

@Preview
@Composable
fun LegacyIndeterminateCircularProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            strokeCap = StrokeCap.Butt
        )
    }
}

private val progressBreakpoints = listOf(20, 40, 60, 80, 100)
