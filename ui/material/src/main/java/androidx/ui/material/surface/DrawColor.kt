/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.surface

import androidx.ui.core.Draw
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Draws the provided [color] within the parent layout's bounds.
 *
 * TODO("Andrey: Find the proper module and package for it")
 */
@Composable
fun DrawColor(color: Color?) {
    if (color != null && color.alpha > 0) {
        val paint = Paint()
        paint.color = color
        paint.style = PaintingStyle.fill
        Draw { canvas, parentSize ->
            canvas.drawRect(parentSize.toRect(), paint)
        }
    }
}
