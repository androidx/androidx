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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.LinearProgressIndicator
import androidx.xr.glimmer.LinearWavyProgressIndicator
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.GlimmerLazyColumn

internal val ProgressIndicatorDemos =
    listOf(
        ComposableDemo("LinearProgressIndicator") { LinearProgressIndicatorDemo() },
        ComposableDemo("LinearWavyProgressIndicator") { LinearWavyProgressIndicatorDemo() },
    )

@Composable
private fun LinearProgressIndicatorDemo() {
    GlimmerLazyColumn {
        item {
            ProgressIndicatorDemoCard("Default") {
                LinearProgressIndicator(progress = { 0.6f })
                LinearProgressIndicator()
            }
        }
        item {
            val color = Color.Red
            val trackColor = Color(0xFFA60100)
            ProgressIndicatorDemoCard("Custom colors") {
                LinearProgressIndicator(progress = { 0.6f }, color = color, trackColor = trackColor)
                LinearProgressIndicator(color = color, trackColor = trackColor)
            }
        }
        item {
            ProgressIndicatorDemoCard("Thick stroke (12.dp)") {
                LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.height(12.dp))
            }
        }
        item {
            ProgressIndicatorDemoCard("Thin stroke (2.dp)") {
                LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.height(2.dp))
                LinearProgressIndicator(modifier = Modifier.height(2.dp))
            }
        }
        item {
            ProgressIndicatorDemoCard("No gap") {
                LinearProgressIndicator(progress = { 0.6f }, gapSize = 0.dp)
                LinearProgressIndicator(gapSize = 0.dp)
            }
        }
        item {
            ProgressIndicatorDemoCard("Wide gap (16.dp)") {
                LinearProgressIndicator(progress = { 0.6f }, gapSize = 16.dp)
                LinearProgressIndicator(gapSize = 16.dp)
            }
        }
    }
}

@Composable
private fun LinearWavyProgressIndicatorDemo() {
    GlimmerLazyColumn {
        item {
            ProgressIndicatorDemoCard("Default") {
                LinearWavyProgressIndicator(progress = { 0.6f })
                LinearWavyProgressIndicator()
            }
        }
        item {
            val color = Color.Red
            val trackColor = Color(0xFFA60100)
            ProgressIndicatorDemoCard("Custom colors") {
                LinearWavyProgressIndicator(
                    progress = { 0.6f },
                    color = color,
                    trackColor = trackColor,
                )
                LinearWavyProgressIndicator(color = color, trackColor = trackColor)
            }
        }
        item {
            ProgressIndicatorDemoCard("Thick stroke (12.dp)") {
                LinearWavyProgressIndicator(
                    progress = { 0.6f },
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 12.dp,
                )
                LinearWavyProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 12.dp)
            }
        }
        item {
            ProgressIndicatorDemoCard("Tall (18.dp)") {
                LinearWavyProgressIndicator(progress = { 0.6f }, modifier = Modifier.height(18.dp))
                LinearWavyProgressIndicator(modifier = Modifier.height(18.dp))
            }
        }
        item {
            ProgressIndicatorDemoCard("Long wavelength (80.dp)") {
                LinearWavyProgressIndicator(progress = { 0.6f }, wavelength = 80.dp)
                LinearWavyProgressIndicator(wavelength = 80.dp)
            }
        }
    }
}

@Composable
private fun ProgressIndicatorDemoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(title = { Text(title) }) {
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Preview
@Composable
private fun LinearProgressIndicatorPreview() {
    GlimmerTheme { LinearProgressIndicatorDemo() }
}

@Preview
@Composable
private fun LinearWavyProgressIndicatorPreview() {
    GlimmerTheme { LinearWavyProgressIndicatorDemo() }
}
