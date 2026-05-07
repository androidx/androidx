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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientRenderer
import androidx.compose.ui.graphics.meshGradient
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun MeshGradientModifierSample() {
    val rows = 1
    val columns = 1
    Box(
        Modifier.size(300.dp).meshGradient(rows, columns) {
            // (row, column, position, color)
            setVertex(
                0,
                0,
                Offset(0f, 0f),
                Color.Red,
                rightControlPoint = Offset(0.5f, 0.5f),
            ) // Top-Left
            setVertex(0, 1, Offset(1f, 0f), Color.Blue) // Top-Right
            setVertex(1, 0, Offset(0f, 1f), Color.Green) // Bottom-Left
            setVertex(1, 1, Offset(1f, 1f), Color.Yellow) // Bottom-Right
        }
    )
}

@Sampled
@Composable
fun MeshGradientRendererSample() {
    val rows = 2
    val columns = 2
    val positions = remember {
        floatArrayOf(
            0f,
            0f,
            0.5f,
            0f,
            1.0f,
            0.0f,
            0f,
            0.5f,
            0.5f,
            0.5f,
            1.0f,
            0.5f,
            0f,
            1.0f,
            0.5f,
            1.0f,
            1.0f,
            1.0f,
        )
    }
    val colors = remember {
        intArrayOf(
            Color(0xFFFF0000).toArgb(),
            Color(0xFFFFA500).toArgb(),
            Color(0xFFFFFF00).toArgb(),
            Color(0xFF00FF00).toArgb(),
            Color(0xFF00FFFF).toArgb(),
            Color(0xFF0000FF).toArgb(),
            Color(0xFF800080).toArgb(),
            Color(0xFFFFC0CB).toArgb(),
            Color(0xFFFFFFFF).toArgb(),
        )
    }
    val renderer = remember { MeshGradientRenderer() }

    Box(
        Modifier.size(100.dp).drawBehind {
            with(renderer) { draw(rows, columns, positions, colors, null, null, null, null, true) }
        }
    )
}
