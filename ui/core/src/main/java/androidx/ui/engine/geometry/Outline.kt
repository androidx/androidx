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

package androidx.ui.engine.geometry

import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.Path

/**
 * Defines a simple shape, used for bounding graphical regions.
 *
 * Can be used for defining a shape of the component background, a shape of
 * shadows cast by the component, or to clip the contents.
 */
sealed class Outline {
    /**
     * Rectangular area.
     */
    data class Rectangle(val rect: Rect) : Outline()
    /**
     * Rectangular area with rounded corners.
     */
    data class Rounded(val rrect: RRect) : Outline()
    /**
     * An area defined as a path.
     *
     * Note that only convex paths can be used for drawing the shadow. See [Path.isConvex].
     */
    data class Generic(val path: Path) : Outline()
}

/**
 * Converts an [Outline] to a [Path].
 */
fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRRect(rrect) }
    is Outline.Generic -> path
}

/**
 * Draws the [Outline] on a [Canvas].
 *
 * @param outline the outline to draw.
 * @param paint the paint used for the drawing.
 */
fun Canvas.drawOutline(outline: Outline, paint: Paint) = when (outline) {
    is Outline.Rectangle -> drawRect(outline.rect, paint)
    is Outline.Rounded -> drawRRect(outline.rrect, paint)
    is Outline.Generic -> drawPath(outline.path, paint)
}
