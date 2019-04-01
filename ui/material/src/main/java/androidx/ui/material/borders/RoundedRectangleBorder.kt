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
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.shrink
import androidx.ui.engine.text.TextDirection
import androidx.ui.lerp
import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.Path
import androidx.ui.toStringAsFixed

/**
 * A rectangular border with rounded corners.
 *
 * Typically used with [ShapeDecoration] to draw a box with a rounded
 * rectangle.
 *
 * This shape can interpolate to and from [CircleBorder].
 *
 * See also:
 *
 *  * [BorderSide], which is used to describe each side of the box.
 *  * [Border], which, when used with [BoxDecoration], can also
 *    describe a rounded rectangle.
 */
data class RoundedRectangleBorder(
    /** The style of this border. */
    val side: BorderSide = BorderSide.None,
    /** The radii for each corner. */
    val borderRadius: BorderRadius = BorderRadius.Zero
) : ShapeBorder() {

    override val borderStyle: BorderStyle
        get() = side.style

    // TODO("Migration|Andrey: Needs EdgeInsetsGeometry")
//    @override
//    EdgeInsetsGeometry get dimensions {
//        return new EdgeInsets.all(side.width);
//    }

    override fun scale(t: Float): ShapeBorder {
        return RoundedRectangleBorder(
            side = side.scale(t),
            borderRadius = borderRadius * t
        )
    }

    override fun lerpFrom(a: ShapeBorder?, t: Float): ShapeBorder? {
        if (a is RoundedRectangleBorder) {
            return RoundedRectangleBorder(
                side = lerp(a.side, side, t),
                borderRadius = lerp(a.borderRadius, borderRadius, t)!!
            )
        }
        if (a is CircleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(a.side, side, t),
                borderRadius = borderRadius,
                circleness = 1.0f - t
            )
        }
        return super.lerpFrom(a, t)
    }

    override fun lerpTo(b: ShapeBorder?, t: Float): ShapeBorder? {
        if (b is RoundedRectangleBorder) {
            return RoundedRectangleBorder(
                side = lerp(side, b.side, t),
                borderRadius = lerp(borderRadius, b.borderRadius, t)!!
            )
        }
        if (b is CircleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(side, b.side, t),
                borderRadius = borderRadius,
                circleness = t
            )
        }
        return super.lerpTo(b, t)
    }

    override fun getInnerPath(rect: Rect, density: Density, textDirection: TextDirection?): Path =
        withDensity(density) {
            Path().apply {
                addRRect(
                    borderRadius.resolve(textDirection).toRRect(rect)
                        .shrink(side.width.toPx().value)
                )
            }
        }

    override fun getOuterPath(rect: Rect, density: Density, textDirection: TextDirection?): Path {
        return Path().apply {
            addRRect(borderRadius.resolve(textDirection).toRRect(rect))
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
            BorderStyle.Solid -> {
                val width = side.width
                if (width == 0.dp) {
                    canvas.drawRRect(
                        borderRadius.resolve(textDirection).toRRect(rect),
                        side.toPaint(density)
                    )
                } else {
                    val outer = borderRadius.resolve(textDirection).toRRect(rect)
                    val inner = outer.shrink(width.toPx().value)
                    val paint = Paint().apply {
                        color = side.color
                    }
                    canvas.drawDRRect(outer, inner, paint)
                }
            }
        }
    }
}

private data class RoundedRectangleToCircleBorder(
    val circleness: Float,
    val side: BorderSide = BorderSide.None,
    val borderRadius: BorderRadius = BorderRadius.Zero
) : ShapeBorder() {

    override val borderStyle: BorderStyle
        get() = side.style

    // TODO("Migration|Andrey: Needs EdgeInsetsGeometry")
//    @override
//    EdgeInsetsGeometry get dimensions {
//        return new EdgeInsets.all(side.width);
//    }

    override fun scale(t: Float): ShapeBorder {
        return RoundedRectangleToCircleBorder(
            circleness = t,
            side = side.scale(t),
            borderRadius = borderRadius * t
        )
    }

    override fun lerpFrom(a: ShapeBorder?, t: Float): ShapeBorder? {
        if (a is RoundedRectangleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(a.side, side, t),
                borderRadius = lerp(a.borderRadius, borderRadius, t)!!,
                circleness = circleness * t
            )
        }
        if (a is CircleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(a.side, side, t),
                borderRadius = borderRadius,
                circleness = circleness + (1.0f - circleness) * (1.0f - t)
            )
        }
        if (a is RoundedRectangleToCircleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(a.side, side, t),
                borderRadius = lerp(a.borderRadius, borderRadius, t)!!,
                circleness = lerp(a.circleness, circleness, t)
            )
        }
        return super.lerpFrom(a, t)
    }

    override fun lerpTo(b: ShapeBorder?, t: Float): ShapeBorder? {
        if (b is RoundedRectangleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(side, b.side, t),
                borderRadius = lerp(borderRadius, b.borderRadius, t)!!,
                circleness = circleness * (1.0f - t)
            )
        }
        if (b is CircleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(side, b.side, t),
                borderRadius = borderRadius,
                circleness = circleness + (1.0f - circleness) * t
            )
        }
        if (b is RoundedRectangleToCircleBorder) {
            return RoundedRectangleToCircleBorder(
                side = lerp(side, b.side, t),
                borderRadius = lerp(borderRadius, b.borderRadius, t)!!,
                circleness = lerp(circleness, b.circleness, t)
            )
        }
        return super.lerpTo(b, t)
    }

    private fun adjustRect(rect: Rect): Rect {
        if (circleness == 0.0f || rect.width == rect.height)
            return rect
        if (rect.width < rect.height) {
            val delta = circleness * (rect.height - rect.width) / 2.0f
            return Rect.fromLTRB(
                rect.left,
                rect.top + delta,
                rect.right,
                rect.bottom - delta
            )
        } else {
            val delta = circleness * (rect.width - rect.height) / 2.0f
            return Rect.fromLTRB(
                rect.left + delta,
                rect.top,
                rect.right - delta,
                rect.bottom
            )
        }
    }

    private fun adjustBorderRadius(rect: Rect): BorderRadius {
        if (circleness == 0.0f)
            return borderRadius
        return lerp(
            borderRadius,
            BorderRadius.circular(rect.getShortestSide() / 2.0f),
            circleness
        )!!
    }

    override fun getInnerPath(
        rect: Rect,
        density: Density,
        textDirection: TextDirection?
    ): Path = withDensity(density) {
        Path().apply {
            addRRect(
                adjustBorderRadius(rect).toRRect(adjustRect(rect))
                    .shrink(side.width.toPx().value)
            )
        }
    }

    override fun getOuterPath(rect: Rect, density: Density, textDirection: TextDirection?): Path {
        return Path().apply {
            addRRect(adjustBorderRadius(rect).toRRect(adjustRect(rect)))
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
            BorderStyle.Solid -> {
                val width = side.width
                if (width == 0.dp) {
                    canvas.drawRRect(
                        adjustBorderRadius(rect).toRRect(adjustRect(rect)),
                        side.toPaint(density)
                    )
                } else {
                    val outer = adjustBorderRadius(rect).toRRect(adjustRect(rect))
                    val inner = outer.shrink(width.toPx().value)
                    val paint = Paint().apply {
                        color = side.color
                    }
                    canvas.drawDRRect(outer, inner, paint)
                }
            }
        }
    }

    override fun toString(): String {
        return "RoundedRectangleBorder($side, $borderRadius, ${(circleness * 100)
            .toStringAsFixed(1)}% of the way to being a CircleBorder)"
    }
}
