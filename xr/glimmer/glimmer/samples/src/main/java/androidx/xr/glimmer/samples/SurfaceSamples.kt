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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.SurfaceDefaults
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.surface

@Composable
fun SurfaceSampleUsage() {
    VerticalList {
        item { SurfaceSample() }
        item { FocusableSurfaceSample() }
        item { ClickableSurfaceSample() }
        item {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier.surface(
                        border = SurfaceDefaults.border(color = GlimmerTheme.colors.positive),
                        interactionSource = interactionSource,
                    )
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a positive surface")
            }
        }
        item {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier.surface(
                        border =
                            BorderStroke(
                                width = 2.dp,
                                brush =
                                    Brush.sweepGradient(
                                        listOf(Color.Red, Color.Green, Color.Blue, Color.Red)
                                    ),
                            ),
                        interactionSource = interactionSource,
                    )
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("Sweep gradient")
            }
        }
    }
}

@Sampled
@Composable
fun SurfaceSample() {
    Box(Modifier.surface().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text("This is a surface")
    }
}

@Sampled
@Composable
fun FocusableSurfaceSample() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier.surface(
                // Provide the same interaction source here and to focusable to make sure that
                // surface appears focused when interacted with.
                interactionSource = interactionSource
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text("This is a focusable surface")
    }
}

@Sampled
@Composable
fun ClickableSurfaceSample() {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier.surface(
                // Provide the same interaction source here and to clickable to make sure that
                // surface appears focused and pressed when interacted with
                interactionSource = interactionSource
            )
            .clickable(interactionSource = interactionSource, onClick = {})
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text("This is a clickable surface")
    }
}

@Preview
@Composable
private fun SurfacePreview() {
    GlimmerTheme { SurfaceSample() }
}

@Preview
@Composable
private fun FocusableSurfacePreview() {
    GlimmerTheme { FocusableSurfaceSample() }
}

@Preview
@Composable
private fun ClickableSurfacePreview() {
    GlimmerTheme { ClickableSurfaceSample() }
}
