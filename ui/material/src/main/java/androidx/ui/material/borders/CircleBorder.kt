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

package androidx.ui.material.borders

import androidx.ui.core.Density
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path
import kotlin.math.max

/**
 * A border that fits a circle within the available space.
 *
 * Typically used with [ShapeDecoration] to draw a circle.
 *
 * The [dimensions] assume that the border is being used in a square space.
 * When applied to a rectangular space, the border paints in the center of the
 * rectangle.
 *
 * See also:
 *
 *  * [BorderSide], which is used to describe each side of the box.
 *  * [Border], which, when used with [BoxDecoration], can also
 *    describe a circle.
 */
data class CircleBorder(
    /** The style of this border. */
    val side: BorderSide = BorderSide.None
) : ShapeBorder() {

    override val borderStyle: BorderStyle
        get() = side.style

    // TODO("Migration|Andrey: Needs EdgeInsetsGeometry")
//    @override
//    EdgeInsetsGeometry get dimensions {
//        return new EdgeInsets.all(side.width);
//    }

    override fun scale(t: Float) = CircleBorder(side = side.scale(t))

    override fun lerpFrom(a: ShapeBorder?, t: Float): ShapeBorder? {
        if (a is CircleBorder)
            return CircleBorder(side = lerp(a.side, side, t))
        return super.lerpFrom(a, t)
    }

    override fun lerpTo(b: ShapeBorder?, t: Float): ShapeBorder? {
        if (b is CircleBorder)
            return CircleBorder(side = lerp(side, b.side, t))
        return super.lerpTo(b, t)
    }

    override fun getInnerPath(rect: Rect, density: Density, textDirection: TextDirection?): Path =
        withDensity(density) {
            Path().apply {
                addOval(
                    Rect.fromCircle(
                        rect.getCenter(),
                        max(0.0f, rect.getShortestSide() / 2.0f - side.width.toPx().value)
                    )
                )
            }
        }

    override fun getOuterPath(rect: Rect, density: Density, textDirection: TextDirection?): Path {
        return Path().apply {
            addOval(
                Rect.fromCircle(
                    center = rect.getCenter(),
                    radius = rect.getShortestSide() / 2.0f
                )
            )
        }
    }

    override fun paint(
        canvas: Canvas,
        density: Density,
        rect: Rect,
        textDirection: TextDirection?
    ) = withDensity(density) {
        when (side.style) {
            BorderStyle.None -> {
            }
            BorderStyle.Solid ->
                canvas.drawCircle(
                    rect.getCenter(),
                    (rect.getShortestSide() - side.width.toPx().value) / 2.0f,
                    side.toPaint(density)
                )
        }
    }
}
