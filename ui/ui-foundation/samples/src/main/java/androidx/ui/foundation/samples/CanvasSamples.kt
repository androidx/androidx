/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Canvas2
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.painter.CanvasScope
import androidx.ui.graphics.painter.Stroke
import androidx.ui.graphics.painter.inset
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp
import androidx.ui.unit.toRect

@Sampled
@Composable
@Suppress("DEPRECATION")
fun CanvasSample() {
    val paint = remember { Paint().apply { color = Color.Magenta } }
    Canvas(modifier = Modifier.preferredSize(100.dp)) {
        drawRect(size.toRect(), paint)
    }
}

/**
 * Sample showing how to create a composable that supports issuing
 * drawing commands through a [CanvasScope]
 */
@Sampled
@Composable
fun Canvas2Sample() {
    Canvas2(modifier = Modifier.preferredSize(20.dp)) {
        drawRect(color = Color.White)
        inset(10.0f) {
            drawLine(
                p1 = Offset.zero,
                p2 = Offset(size.width, size.height),
                stroke = Stroke(width = 5.0f),
                color = Color.Red
            )
        }
    }
}
