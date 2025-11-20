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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.surface

@Composable
fun ShapesSample() {
    val shapes = GlimmerTheme.shapes
    VerticalList(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { ShapeItem("small", shape = shapes.small) }
        item { ShapeItem("medium", shape = shapes.medium) }
        item { ShapeItem("large", shape = shapes.large) }
    }
}

@Preview
@Composable
private fun ShapesPreview() {
    GlimmerTheme { ShapesSample() }
}

@Composable
private fun ShapeItem(name: String, shape: Shape, modifier: Modifier = Modifier) {
    Box(
        modifier.aspectRatio(2.5f).fillMaxWidth().surface(focusable = false, shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name)
    }
}
