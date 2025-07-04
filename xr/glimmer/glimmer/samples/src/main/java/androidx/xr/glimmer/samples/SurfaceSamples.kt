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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    VerticalList(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { SurfaceSample() }
        item { ClickableSurfaceSample() }
        item { ToggleableSurfaceSample() }
        item {
            Box(
                Modifier.surface(
                        border = SurfaceDefaults.border(color = GlimmerTheme.colors.positive),
                        onClick = {},
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a positive surface")
            }
        }
        item {
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
                        onClick = {},
                    )
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
fun ClickableSurfaceSample() {
    Box(Modifier.surface(onClick = {}).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text("This is a clickable surface")
    }
}

@Sampled
@Composable
fun ToggleableSurfaceSample() {
    var checked by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier.surface(
                // Disable focus on the surface, since toggleable is already focusable
                focusable = false,
                // Provide the same interaction source here and to toggleable to make sure that
                // surface appears focused and pressed when interacted with
                interactionSource = interactionSource,
            )
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                onValueChange = { checked = it },
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text("Checked: $checked")
    }
}

@Preview
@Composable
private fun SurfacePreview() {
    GlimmerTheme { SurfaceSample() }
}

@Preview
@Composable
private fun ClickableSurfacePreview() {
    GlimmerTheme { ClickableSurfaceSample() }
}

@Preview
@Composable
private fun ToggleableSurfacePreview() {
    GlimmerTheme { ToggleableSurfaceSample() }
}
