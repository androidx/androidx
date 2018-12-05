/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting.borders

import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path

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

    // TODO("Migration|Andrey: Needs EdgeInsetsGeometry")
//    @override
//    EdgeInsetsGeometry get dimensions {
//        return new EdgeInsets.all(side.width);
//    }

    override fun scale(t: Double) = CircleBorder(side = side.scale(t))

    override fun lerpFrom(a: ShapeBorder?, t: Double): ShapeBorder? {
        if (a is CircleBorder)
            return CircleBorder(side = lerp(a.side, side, t))
        return super.lerpFrom(a, t)
    }

    override fun lerpTo(b: ShapeBorder?, t: Double): ShapeBorder? {
        if (b is CircleBorder)
            return CircleBorder(side = lerp(side, b.side, t))
        return super.lerpTo(b, t)
    }

    override fun getInnerPath(rect: Rect, textDirection: TextDirection?): Path {
        return Path().apply {
            addOval(
                Rect.fromCircle(
                    center = rect.getCenter(),
                    radius = Math.max(0.0, rect.getShortestSide() / 2.0 - side.width)
                )
            )
        }
    }

    override fun getOuterPath(rect: Rect, textDirection: TextDirection?): Path {
        return Path().apply {
            addOval(
                Rect.fromCircle(
                    center = rect.getCenter(),
                    radius = rect.getShortestSide() / 2.0
                )
            )
        }
    }

    override fun paint(canvas: Canvas, rect: Rect, textDirection: TextDirection?) {
        when (side.style) {
            BorderStyle.NONE -> {
            }
            BorderStyle.SOLID ->
                canvas.drawCircle(
                    rect.getCenter(),
                    (rect.getShortestSide() - side.width) / 2.0,
                    side.toPaint()
                )
        }
    }
}
