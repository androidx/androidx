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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Outlines of the handle icons, independent of their enlarged touch targets. */
internal enum class HandleShape : Shape {
    SelectionLeft,
    SelectionRight,
    Cursor;

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        // Like createHandleImage, use a circle with its upper-left corner filled by a rectangle.
        // The radius comes from the drawing width, including for the non-square cursor icon.
        val radius = size.width / 2f
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(radius, 0f)
                arcTo(
                    rect = Rect(0f, 0f, radius * 2f, radius * 2f),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 270f,
                    forceMoveTo = false,
                )
                close()
            }
        when (this) {
            SelectionLeft ->
                path.transform(
                    Matrix().apply {
                        translate(x = size.width)
                        scale(x = -1f)
                    }
                )
            Cursor ->
                path.transform(
                    Matrix().apply {
                        translate(x = radius)
                        rotateZ(45f)
                    }
                )
            SelectionRight -> Unit
        }
        return Outline.Generic(path)
    }
}
