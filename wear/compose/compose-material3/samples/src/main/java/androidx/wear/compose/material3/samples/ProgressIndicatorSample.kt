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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.SegmentedCircularProgressIndicator

@Sampled
@Composable
fun FullScreenProgressIndicatorSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        CircularProgressIndicator(
            progress = { 0.25f },
            startAngle = 120f,
            endAngle = 60f,
        )
    }
}

@Sampled
@Composable
fun MediaButtonProgressIndicatorSample() {
    var isPlaying by remember { mutableStateOf(false) }
    val progressPadding = 4.dp
    val progress = 0.75f

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier =
                Modifier.align(Alignment.Center)
                    .size(IconButtonDefaults.DefaultButtonSize + progressPadding)
        ) {
            CircularProgressIndicator(progress = { progress }, strokeWidth = progressPadding)
            IconButton(
                modifier =
                    Modifier.align(Alignment.Center)
                        .semantics {
                            // Set custom progress semantics for accessibility.
                            contentDescription =
                                String.format(
                                    "Play/pause button, track progress: %.0f%%",
                                    progress * 100
                                )
                        }
                        .padding(progressPadding)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                onClick = { isPlaying = !isPlaying }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
            }
        }
    }
}

@Sampled
@Composable
fun OverflowProgressIndicatorSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        CircularProgressIndicator(
            // Overflow value of 120%
            progress = { 1.2f },
            allowProgressOverflow = true,
            startAngle = 120f,
            endAngle = 60f,
        )
    }
}

@Sampled
@Composable
fun SmallValuesProgressIndicatorSample() {
    Box {
        CircularProgressIndicator(
            // Small progress values like 2% will be rounded up to at least the stroke width.
            progress = { 0.02f },
            modifier =
                Modifier.fillMaxSize().padding(CircularProgressIndicatorDefaults.FullScreenPadding),
            startAngle = 120f,
            endAngle = 60f,
            strokeWidth = 10.dp,
            colors =
                ProgressIndicatorDefaults.colors(
                    indicatorColor = Color.Green,
                    trackColor = Color.White
                ),
        )
    }
}

@Sampled
@Composable
fun IndeterminateProgressIndicatorSample() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Sampled
@Composable
fun SegmentedProgressIndicatorSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        SegmentedCircularProgressIndicator(
            segmentCount = 5,
            progress = { 0.5f },
        )
    }
}

@Sampled
@Composable
fun SegmentedProgressIndicatorOnOffSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        SegmentedCircularProgressIndicator(
            segmentCount = 5,
            completed = { it % 2 != 0 },
        )
    }
}

@Sampled
@Composable
fun SmallSegmentedProgressIndicatorSample() {
    Box(modifier = Modifier.fillMaxSize()) {
        SegmentedCircularProgressIndicator(
            segmentCount = 8,
            completed = { it % 2 != 0 },
            modifier = Modifier.align(Alignment.Center).size(80.dp)
        )
    }
}
