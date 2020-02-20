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

package androidx.ui.framework.demos.gestures

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.foundation.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.Dp
import androidx.ui.unit.Px

@Composable
internal fun DrawingBox(xOffset: Px, yOffset: Px, width: Dp, height: Dp, color: Color) {
    val paint = remember { Paint() }
    Canvas(modifier = LayoutSize.Fill) {
        paint.color = color
        val centerX = size.width.value / 2 + xOffset.value
        val centerY = size.height.value / 2 + yOffset.value
        val widthPx = width.toPx()
        val heightPx = height.toPx()
        val widthValue = if (widthPx.value < 0) size.width.value else widthPx.value
        val heightValue = if (heightPx.value < 0) size.height.value else heightPx.value
        drawRect(
            androidx.ui.geometry.Rect(
                centerX - widthValue / 2,
                centerY - heightValue / 2,
                centerX + widthValue / 2,
                centerY + heightValue / 2
            ),
            paint
        )
    }
}