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
import androidx.ui.foundation.background
import androidx.ui.geometry.Offset
import androidx.ui.graphics.BlendMode
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.Paint
import androidx.ui.graphics.painter.Painter
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutPadding
import androidx.ui.core.toModifier
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp

@Sampled
@Composable
fun PainterModifierSample() {
    class CustomPainter : Painter() {

        val paint = Paint().apply {
            this.color = Color.Red
        }

        override val intrinsicSize: PxSize
            get() = PxSize(
                Px(300.0f),
                Px(300.0f)
            )

        override fun onDraw(canvas: Canvas, bounds: PxSize) {
            val size = intrinsicSize
            val width = size.width.value
            val height = size.height.value
            canvas.drawCircle(
                Offset(
                    width / 2.0f,
                    height / 2.0f
                ),
                width / 2.0f,
                paint
            )
        }
    }

    Container(modifier =
        background(Color.Gray) +
        LayoutPadding(30.dp) +
        background(Color.Yellow) +
        CustomPainter().toModifier(
            alpha = 0.5f,
            colorFilter = ColorFilter(Color.Cyan, BlendMode.srcIn)
        ),
        width = 300.dp,
        height = 300.dp
    ) { /* intentionally empty */ }
}