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

package androidx.xr.glimmer.demos.remote.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.xr.glimmer.demos.remote.ComposableDemo
import androidx.xr.glimmer.demos.remote.DemoCategory
import androidx.xr.glimmer.remote.RemoteGlimmerTheme
import androidx.xr.glimmer.remote.surface

internal val RemoteThemeDemos =
    DemoCategory(
        "Remote Theme",
        listOf(
            ComposableDemo("Colors") { ThemeDemo("colors.rc") { ColorsDemo() } },
            ComposableDemo("Shapes") { ThemeDemo("shapes.rc") { ShapesDemo() } },
        ),
    )

@Composable
private fun ThemeDemo(fileName: String, content: @Composable @RemoteComposable () -> Unit) {
    val documentBytes by rememberRemoteDocumentState { RemoteGlimmerTheme(content = content) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedRemoteDocumentPlayer(documentBytes = documentBytes, modifier = Modifier.weight(1f))
        SaveRemoteDocumentButton(fileName = fileName, documentBytes = documentBytes)
    }
}

@Composable
@RemoteComposable
private fun ColorsDemo() {
    val colors = RemoteGlimmerTheme.colors
    RemoteColumn(
        verticalArrangement =
            RemoteArrangement.spacedBy(RemoteGlimmerTheme.componentSpacingValues.extraLarge)
    ) {
        ColorItem(colors.primary, colorName = "primary")
        ColorItem(colors.secondary, colorName = "secondary")
        ColorItem(colors.negative, colorName = "negative")
        ColorItem(colors.positive, colorName = "positive")
        ColorItem(colors.background, colorName = "background")
        ColorItem(colors.surface, colorName = "surface")
    }
}

@Composable
@RemoteComposable
private fun ColorItem(color: RemoteColor, colorName: String) {
    // Theme colors are constant, so they can be inspected at creation time.
    val constantColor = Color(color.constantValue)
    val contentColor = (if (constantColor.luminance() < 0.179f) Color.White else Color.Black).rc
    val textStyle = RemoteGlimmerTheme.typography.caption
    RemoteRow(
        modifier = RemoteModifier.fillMaxWidth().background(color),
        horizontalArrangement = RemoteArrangement.SpaceBetween,
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        RemoteText(
            text = colorName.rs,
            color = contentColor,
            style = textStyle,
            modifier = RemoteModifier.padding(16.rdp),
        )
        RemoteText(
            text = constantColor.toHexString().rs,
            color = contentColor,
            style = textStyle,
            modifier = RemoteModifier.padding(16.rdp),
        )
    }
}

/** Returns a hex string, dropping the leading alpha channel and any trailing zeroes. */
private fun Color.toHexString() =
    this.value.toString(16).padStart(16, '0').substring(2, 8).uppercase()

@Composable
@RemoteComposable
private fun ShapesDemo() {
    val shapes = RemoteGlimmerTheme.shapes
    RemoteColumn(
        verticalArrangement =
            RemoteArrangement.spacedBy(RemoteGlimmerTheme.componentSpacingValues.extraLarge)
    ) {
        ShapeItem("small", shape = shapes.small)
        ShapeItem("medium", shape = shapes.medium)
        ShapeItem("large", shape = shapes.large)
    }
}

@Composable
@RemoteComposable
private fun ShapeItem(name: String, shape: RemoteShape) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxWidth().height(120.rdp).surface(shape = shape),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(
            text = name.rs,
            color = Color.White.rc,
            style = RemoteGlimmerTheme.typography.caption,
        )
    }
}
