/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import android.graphics.Picture
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Border drawing and caching logic based on androidx.compose.foundation.border, moved out of a draw
 * node and with support for efficient width animation. To draw multiple different borders
 * concurrently, create a new instance of this class for each border.
 *
 * This draws an 'inner' border, so the outer edge of the border lines up with the canvas boundary.
 */
@Suppress("NOTHING_TO_INLINE")
internal class BorderLogic {
    // BorderCache object that is lazily allocated depending on the type of shape
    // This object is only used for generic shapes and rounded rectangles with different corner
    // radius sizes.
    private var borderCache: BorderCache? = null

    private var borderWidth: (() -> Dp)? = null
    private var lastBrush: Brush? = null
    private var lastOutline: Outline? = null
    // Cached draw border that will be reused if the above parameters don't change
    private var drawBorder: (DrawScope.() -> Unit)? = null

    /**
     * Draws a border with the given parameters. If the provided parameters are the same, previous
     * drawing logic can be re-used for successive draws. As a result drawing multiple borders
     * concurrently should use multiple instances of [BorderLogic], to allow each individual border
     * to be cached through draw invalidations if the parameters are the same.
     *
     * Note that [width] can be updated without invalidating cached logic, as it is evaluated during
     * drawing.
     */
    internal fun drawBorder(
        drawScope: DrawScope,
        width: () -> Dp,
        brush: Brush,
        // We accept an outline here instead of a shape, since shapes can be observable and create
        // different outlines over time. This also means that we can avoid creating multiple
        // outlines for cases where we want to draw multiple borders for the same shape - the caller
        // can create one outline and provide to different BorderLogic instances. The alternative
        // would be to observeReads { shape.createOutline(...) } - but it is harder to encapsulate
        // the logic for that inside this class, and observeReads adds notable cost.
        outline: Outline,
    ): Unit =
        with(drawScope) {
            // Changes in border width can be dynamically read during draw, no need to re-create
            // drawing lambdas
            borderWidth = width

            if (brush != lastBrush || outline != lastOutline || drawBorder == null) {
                lastBrush = brush
                lastOutline = outline

                drawBorder =
                    when (outline) {
                        is Outline.Generic -> createDrawGenericBorder(brush, outline)

                        is Outline.Rounded -> createDrawRoundRectBorder(brush, outline)

                        is Outline.Rectangle -> createDrawRectBorder(brush)
                    }
            }
            drawBorder!!()
        }

    /**
     * Calculates the stroke width from the provided width in Dp. [Dp.Hairline] is converted to a
     * width of one pixel. The stroke width returned is at most half of the smallest dimension we
     * are drawing into, to make sure that both sides of the border can fit into the canvas boundary
     * when drawn.
     */
    private inline fun DrawScope.strokeWidthPx(): Float {
        val width = borderWidth!!.invoke()
        return min(
                if (width == Dp.Hairline) 1f else ceil(width.toPx()),
                ceil(size.minDimension / 2),
            )
            .coerceAtLeast(0f)
    }

    /**
     * Adjusted top left position for a stroke with the given [strokeWidthPx]. Strokes are drawn
     * centered around the path - given that we want an internal border, we need to offset the
     * stroke by half the stroke width to ensure that the outer edge of the stroke lines up with the
     * outer edge of component's size.
     */
    private inline fun topLeft(strokeWidthPx: Float): Offset {
        val halfStroke = strokeWidthPx / 2
        return Offset(halfStroke, halfStroke)
    }

    /**
     * @return size of the border - strokes are drawn centered around the path, so this is the
     *   canvas size, with half of the stroke width removed from each side. Drawing a border with
     *   this size will lead to the outer edge of the border being aligned with the canvas boundary.
     */
    private inline fun DrawScope.borderSize(strokeWidthPx: Float): Size {
        return Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
    }

    /**
     * @return true if the drawing area is smaller than the strokes being drawn. If so we can draw a
     *   solid shape, as opposed to a stroked one (since there will be no empty space inside the
     *   stroke).
     */
    private inline fun DrawScope.fillArea(strokeWidthPx: Float): Boolean {
        // The stroke is larger than the drawing area so just draw a full shape instead
        return (strokeWidthPx * 2) > size.minDimension
    }

    /**
     * Border implementation for generic paths. Note it is possible to be given paths that do not
     * make sense in the context of a border (ex. a figure 8 path or a non-enclosed shape) We do not
     * handle that here as we expect developers to give us enclosed, non-overlapping paths.
     */
    private fun createDrawGenericBorder(
        brush: Brush,
        outline: Outline.Generic,
    ): DrawScope.() -> Unit {
        val pathBounds = outline.path.getBounds()
        // Create a mask path that includes a rectangle with the original path cut out of it.
        // Note: borderCache is part of the class that defines this extension function.
        if (borderCache == null) {
            borderCache = BorderCache()
        }
        val maskPath =
            borderCache!!.obtainPath().apply {
                reset()
                addRect(pathBounds)
                op(this, outline.path, PathOperation.Difference)
            }

        val pathBoundsSize =
            IntSize(ceil(pathBounds.width).toInt(), ceil(pathBounds.height).toInt())

        return {
            val strokeWidth = strokeWidthPx()
            val fillArea = fillArea(strokeWidth)
            if (fillArea) {
                drawPath(outline.path, brush = brush)
            } else {
                translate(pathBounds.left, pathBounds.top) {
                    with(borderCache!!) {
                        drawBorderCache(pathBoundsSize) {
                            // Paths can have offsets, so translate to keep the drawn path
                            // within the bounds of the mask
                            translate(-pathBounds.left, -pathBounds.top) {
                                // Draw the path with a stroke width twice the provided value.
                                // Because strokes are centered, this will draw both and inner and
                                // outer stroke with the desired stroke width
                                drawPath(
                                    path = outline.path,
                                    brush = brush,
                                    style = Stroke(strokeWidth * 2),
                                )

                                // Scale the canvas slightly to cover the background that may be
                                // visible
                                // after clearing the outer stroke
                                scale(
                                    (size.width + 1) / size.width,
                                    (size.height + 1) / size.height,
                                ) {
                                    // Remove the outer stroke by clearing the inverted mask path
                                    drawPath(
                                        path = maskPath,
                                        brush = brush,
                                        blendMode = BlendMode.Clear,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Border implementation for simple rounded rects and those with different corner radii */
    private fun createDrawRoundRectBorder(
        brush: Brush,
        outline: Outline.Rounded,
    ): DrawScope.() -> Unit {
        if (outline.roundRect.isSimple) {
            return {
                val strokeWidth = strokeWidthPx()
                val topLeft = topLeft(strokeWidth)
                val borderSize = borderSize(strokeWidth)
                val fillArea = fillArea(strokeWidth)
                val cornerRadius = outline.roundRect.topLeftCornerRadius
                val halfStroke = strokeWidth / 2
                val borderStroke = Stroke(strokeWidth)
                when {
                    fillArea -> {
                        // If the drawing area is smaller than the stroke being drawn
                        // drawn all around it just draw a filled in rounded rect
                        drawRoundRect(brush, cornerRadius = cornerRadius)
                    }
                    cornerRadius.x < halfStroke -> {
                        // If the corner radius is smaller than half of the stroke width
                        // then the interior curvature of the stroke will be a sharp edge
                        // In this case just draw a normal filled in rounded rect with the
                        // desired corner radius but clipping out the interior rectangle
                        clipRect(
                            strokeWidth,
                            strokeWidth,
                            size.width - strokeWidth,
                            size.height - strokeWidth,
                            clipOp = ClipOp.Difference,
                        ) {
                            drawRoundRect(brush, cornerRadius = cornerRadius)
                        }
                    }
                    else -> {
                        // Otherwise draw a stroked rounded rect with the corner radius
                        // shrunk by half of the stroke width. This will ensure that the
                        // outer curvature of the rounded rectangle will have the desired
                        // corner radius.
                        drawRoundRect(
                            brush = brush,
                            topLeft = topLeft,
                            size = borderSize,
                            cornerRadius = cornerRadius.shrink(halfStroke),
                            style = borderStroke,
                        )
                    }
                }
            }
        } else {
            // Note: borderCache is part of the class that defines this extension function.
            if (borderCache == null) {
                borderCache = BorderCache()
            }
            val path = borderCache!!.obtainPath()
            var lastStrokeWidth = Float.NaN
            var roundedRectPath: Path? = null

            return {
                val strokeWidthPx = strokeWidthPx()
                val fillArea = fillArea(strokeWidthPx)
                if (lastStrokeWidth != strokeWidthPx) {
                    roundedRectPath =
                        createRoundRectPath(path, outline.roundRect, strokeWidthPx, fillArea)
                    lastStrokeWidth = strokeWidthPx
                }
                drawPath(roundedRectPath!!, brush = brush)
            }
        }
    }

    /** Border implementation for rectangular borders */
    private fun createDrawRectBorder(brush: Brush): DrawScope.() -> Unit {
        return {
            val strokeWidthPx = strokeWidthPx()
            val topLeft = topLeft(strokeWidthPx)
            val borderSize = borderSize(strokeWidthPx)
            val fillArea = fillArea(strokeWidthPx)
            // If we are drawing a rectangular stroke, just offset it by half the stroke
            // width as strokes are always drawn centered on their geometry.
            // If the border is larger than the drawing area, just fill the area with a
            // solid rectangle
            val rectTopLeft = if (fillArea) Offset.Zero else topLeft
            val size = if (fillArea) size else borderSize
            val style = if (fillArea) Fill else Stroke(strokeWidthPx)
            drawRect(brush = brush, topLeft = rectTopLeft, size = size, style = style)
        }
    }
}

/**
 * Helper object that handles lazily allocating and re-using objects to render the border into a
 * Picture
 */
private class BorderCache {
    private var borderPath: Path? = null
    private var picture: Picture? = null

    inline fun DrawScope.drawBorderCache(borderSize: IntSize, block: DrawScope.() -> Unit) {
        val drawSize = borderSize.toSize()
        picture = picture ?: Picture()
        val canvas = picture!!.beginRecording(drawSize.width.toInt(), drawSize.height.toInt())

        draw(this, layoutDirection, Canvas(canvas), drawSize) { block() }

        picture!!.endRecording()

        drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture!!) }
    }

    fun obtainPath(): Path = borderPath ?: Path().also { borderPath = it }
}

/**
 * Helper method that creates a round rect with the inner region removed by the given stroke width
 */
private fun createRoundRectPath(
    targetPath: Path,
    roundedRect: RoundRect,
    strokeWidth: Float,
    fillArea: Boolean,
): Path =
    targetPath.apply {
        reset()
        addRoundRect(roundedRect)
        if (!fillArea) {
            val insetPath =
                Path().apply { addRoundRect(createInsetRoundedRect(strokeWidth, roundedRect)) }
            op(this, insetPath, PathOperation.Difference)
        }
    }

private fun createInsetRoundedRect(widthPx: Float, roundedRect: RoundRect) =
    RoundRect(
        left = widthPx,
        top = widthPx,
        right = roundedRect.width - widthPx,
        bottom = roundedRect.height - widthPx,
        topLeftCornerRadius = roundedRect.topLeftCornerRadius.shrink(widthPx),
        topRightCornerRadius = roundedRect.topRightCornerRadius.shrink(widthPx),
        bottomLeftCornerRadius = roundedRect.bottomLeftCornerRadius.shrink(widthPx),
        bottomRightCornerRadius = roundedRect.bottomRightCornerRadius.shrink(widthPx),
    )

/**
 * Helper method to shrink the corner radius by the given value, clamping to 0 if the resultant
 * corner radius would be negative
 */
private fun CornerRadius.shrink(value: Float): CornerRadius =
    CornerRadius(max(0f, this.x - value), max(0f, this.y - value))
