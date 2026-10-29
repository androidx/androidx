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

package androidx.compose.foundation

import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SweepGradient
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy.Companion.Offscreen
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * BorderRenderer provides rendering and caching logic for drawing borders inside a [DrawScope].
 *
 * BorderRenderer provides the same border drawing that [androidx.compose.foundation.border] uses so
 * you can draw borders from any drawing context, such as a
 * [androidx.compose.ui.node.DrawModifierNode]. This can be useful when you need to render multiple
 * inset borders at once, or efficiently animate borders. For simple cases you can continue to use
 * [androidx.compose.foundation.border].
 *
 * Borders drawn with BorderRenderer are internal borders - they grow inwards as the border width
 * increases.
 *
 * BorderRenderer contains caches to efficiently draw borders over time. As a result, make sure to:
 * - Reuse the same BorderRenderer instance each time you draw the border
 * - Use a separate instance for each border that is drawn at the same time. For example, if you
 *   want to draw two borders, inset from each other, each border should have its own
 *   BorderRenderer. See the samples for more information.
 *
 * Drawing a border from inside a [androidx.compose.ui.node.DrawModifierNode]:
 *
 * @sample androidx.compose.foundation.samples.BorderRendererSample
 *
 * Drawing two borders inset from each other (using separate BorderRenderers):
 *
 * @sample androidx.compose.foundation.samples.BorderRendererMultipleBordersSample
 * @param onProvideGraphicsLayer Provides a [GraphicsLayer] needed for rendering certain borders.
 *   This lambda will be invoked when needed, and may be invoked multiple times. As a result make
 *   sure to cache / return the same valid [GraphicsLayer] to avoid creating many layers. This
 *   [GraphicsLayer] should not be used by any other [BorderRenderer] instances or drawing code at
 *   the same time. The caller is responsible for its lifecycle - make sure to call
 *   [androidx.compose.ui.graphics.GraphicsContext.releaseGraphicsLayer] when you no longer need it
 *   (such as when a Modifier.Node using this is detached). If you release the layer, make sure to
 *   return a new valid [GraphicsLayer] when this is called again (if the node is re-attached
 *   later). See the samples for example usage.
 */
@Suppress("NOTHING_TO_INLINE")
public class BorderRenderer
@RememberInComposition
constructor(private val onProvideGraphicsLayer: () -> GraphicsLayer) {
    // BorderPath object that is lazily allocated depending on the type of shape
    // This object is only used for generic shapes (where it contains the mask path) and rounded
    // rectangles with different corner radius sizes (where it contains the border path).
    private var borderPath: Path? = null
    // The last stroke width drawn (if known). This is only used for generic shapes and rounded
    // rectangles with different corner radius sizes.
    private var lastStrokeWidth: Float = Float.NaN
    // This path is only used for rounded rectangles with different corner radius sizes.
    private var roundRectInsetPath: Path? = null
    // Outline.Rounded.bounds allocates a new Rect each time it is called, so we cache this.
    // This only used for rounded rectangles.
    private var roundRectBounds: Rect = Rect.Zero
    // RoundRect.isSimple compares every corner radius, so we cache this.
    // This only used for rounded rectangles.
    private var isSimpleRoundRect: Boolean = false

    // Only used for generic shapes
    private var pathBounds: Rect = Rect.Zero
    private var pathBoundsSize: IntSize = IntSize.Zero

    // Lazily allocated when offscreen ImageBitmap rendering is used for generic shapes.
    private var colorFilter: ColorFilter? = null
    private var imageBitmapConfig: ImageBitmapConfig = ImageBitmapConfig.Argb8888
    private var imageBitmap: ImageBitmap? = null
    private var canvas: Canvas? = null
    private var canvasDrawScope: CanvasDrawScope? = null

    // Retrieved from onProvideGraphicsLayer when graphics layer rendering is used for generic
    // shapes.
    private var lastGraphicsLayer: GraphicsLayer? = null

    private var lastBrush: Brush? = null
    private var lastOutline: Outline? = null

    // Stroke is immutable, so reuse the last one if the width hasn't changed.
    private var stroke: Stroke? = null

    /**
     * Draws a border defined by [outline], with [width] and [brush], into [drawScope]. The border
     * will be drawn using the size defined by [outline], not [DrawScope.size]. Borders drawn are
     * internal borders (drawn within the bounding box, not outside), and grow inwards as [width]
     * increases.
     *
     * To modify the drawn border you can use [DrawScope] transformation APIs such as [translate],
     * just as for any other drawing commands.
     *
     * To draw several different borders at the same time, use a separate [BorderRenderer] for each
     * one.
     *
     * @sample androidx.compose.foundation.samples.BorderRendererSample
     * @param drawScope The [DrawScope] to draw the border into.
     * @param width The width of the border in [Dp].
     * @param brush The [Brush] to paint the border with.
     * @param outline The [Outline] of the border.
     */
    @OptIn(ExperimentalFoundationApi::class)
    public fun drawBorder(
        drawScope: DrawScope,
        width: Dp,
        brush: Brush,
        outline: Outline,
    ): Unit =
        with(drawScope) {
            val widthPx =
                when (width) {
                    Dp.Hairline -> 1f
                    Dp.Unspecified -> 0f
                    else -> ceil(width.toPx())
                }
            // Note that when using outline bounds we intentionally don't check the size of the
            // DrawScope here - the border is drawn using the bounds of the outline, so the outline
            // bounds are checked when drawing.
            val hasValidBorderParams =
                widthPx > 0f &&
                    (ComposeFoundationFlags.isBorderMatchesOutlineBoundsFixEnabled ||
                        size.minDimension > 0f)
            if (!hasValidBorderParams) {
                return
            }

            // We accept an outline here instead of a shape,
            // since shapes can be observable and create different outlines over time. This also
            // means we can avoid creating multiple outlines for cases where we want to draw
            // multiple borders for the same shape - the caller can create one outline and provide
            // it to different BorderRenderer instances. The alternative would be to `observeReads {
            // shape.createOutline(...) }` but it is harder to encapsulate the logic for that inside
            // this class, and `observeReads` adds notable cost.
            val brushChanged = brush != lastBrush
            val outlineChanged = outline != lastOutline
            lastBrush = brush
            lastOutline = outline

            when (outline) {
                is Outline.Generic ->
                    drawGenericBorder(
                        widthPx = widthPx,
                        brush = brush,
                        outline = outline,
                        brushChanged = brushChanged,
                        outlineChanged = outlineChanged,
                    )

                is Outline.Rounded ->
                    drawRoundRectBorder(
                        widthPx = widthPx,
                        brush = brush,
                        outline = outline,
                        outlineChanged = outlineChanged,
                    )

                is Outline.Rectangle -> drawRectBorder(widthPx, brush, outline)
            }
        }

    /**
     * @return the bounds used to calculate the size and position of the border. This is
     *   [outlineBounds], unless [ComposeFoundationFlags.isBorderMatchesOutlineBoundsFixEnabled] is
     *   disabled, in which case the bounds of this [DrawScope] are used instead.
     */
    @OptIn(ExperimentalFoundationApi::class)
    private fun DrawScope.borderBounds(outlineBounds: Rect): Rect =
        if (ComposeFoundationFlags.isBorderMatchesOutlineBoundsFixEnabled) outlineBounds
        else Rect(Offset.Zero, size)

    /** @return a [Stroke] with the given [width], reusing the previous [Stroke] if possible. */
    private fun obtainStroke(width: Float): Stroke {
        val stroke = stroke
        return if (stroke != null && stroke.width == width) {
            stroke
        } else {
            Stroke(width).also { this.stroke = it }
        }
    }

    /**
     * Calculates the stroke width from the provided width in pixels. The stroke width returned is
     * at most half of the smallest dimension of the [bounds] we are drawing into, to make sure that
     * both sides of the border can fit into the bounds when drawn.
     */
    private inline fun strokeWidthPx(widthPx: Float, bounds: Rect): Float {
        return min(ceil(widthPx.coerceAtLeast(0f)), ceil(bounds.minDimension / 2)).coerceAtLeast(0f)
    }

    /**
     * Adjusted top left position for a stroke with the given [strokeWidthPx]. Strokes are drawn
     * centered around the path - given that we want an internal border, we need to offset the
     * stroke by half the stroke width to ensure that the outer edge of the stroke lines up with the
     * outer edge of the [bounds].
     */
    private inline fun topLeft(strokeWidthPx: Float, bounds: Rect): Offset {
        val halfStroke = strokeWidthPx / 2
        return Offset(bounds.left + halfStroke, bounds.top + halfStroke)
    }

    /**
     * @return size of the border - strokes are drawn centered around the path, so this is the size
     *   of the [bounds], with half of the stroke width removed from each side. Drawing a border
     *   with this size will lead to the outer edge of the border being aligned with the [bounds].
     */
    private inline fun borderSize(strokeWidthPx: Float, bounds: Rect): Size {
        return Size(bounds.width - strokeWidthPx, bounds.height - strokeWidthPx)
    }

    /**
     * @return true if the [bounds] are smaller than the strokes being drawn. If so we can draw a
     *   solid shape, as opposed to a stroked one (since there will be no empty space inside the
     *   stroke).
     */
    private inline fun fillArea(strokeWidthPx: Float, bounds: Rect): Boolean {
        // The stroke is larger than the drawing area so just draw a full shape instead
        return (strokeWidthPx * 2) > bounds.minDimension
    }

    /**
     * Border implementation for generic paths. Note it is possible to be given paths that do not
     * make sense in the context of a border (ex. a figure 8 path or a non-enclosed shape) We do not
     * handle that here as we expect developers to give us enclosed, non-overlapping paths.
     */
    private fun DrawScope.drawGenericBorder(
        widthPx: Float,
        brush: Brush,
        outline: Outline.Generic,
        brushChanged: Boolean,
        outlineChanged: Boolean,
    ) {
        // The mask path is only rebuilt when the outline changes.
        val maskPath = obtainPath()
        if (outlineChanged) {
            val bounds = outline.path.getBounds()
            pathBounds = bounds
            pathBoundsSize = IntSize(ceil(bounds.width).toInt(), ceil(bounds.height).toInt())
            // Create a mask path that includes a rectangle with the original path cut out of it.
            maskPath.apply {
                reset()
                addRect(bounds)
                op(this, outline.path, PathOperation.Difference)
            }
        }

        // RuntimeShaders can't be drawn in a software bitmap. Special case known types that don't
        // need a runtime shader so we can render in software, and avoid minor visual regressions
        // for Modifier.border b/570069301.
        // Note that it's possible to use a ShaderBrush with a non-runtime shader, such as manually
        // passing LinearGradientShader to a ShaderBrush, but these cases are rare and difficult to
        // check for. In any case the only change will be minor antialiasing differences.
        val brushCanDrawInSoftware =
            brush !is ShaderBrush ||
                brush is LinearGradient ||
                brush is RadialGradient ||
                brush is SweepGradient

        if (shouldUseGraphicsLayerForGenericBorder() || !brushCanDrawInSoftware) {
            drawGenericBorderWithGraphicsLayer(
                widthPx = widthPx,
                brush = brush,
                outline = outline,
                pathBounds = pathBounds,
                maskPath = maskPath,
                pathBoundsSize = pathBoundsSize,
                brushChanged = brushChanged,
                outlineChanged = outlineChanged,
            )
        } else {
            drawGenericBorderWithImageBitmap(
                widthPx = widthPx,
                brush = brush,
                outline = outline,
                pathBounds = pathBounds,
                maskPath = maskPath,
                pathBoundsSize = pathBoundsSize,
                brushChanged = brushChanged,
                outlineChanged = outlineChanged,
            )
        }
    }

    private fun DrawScope.drawGenericBorderWithGraphicsLayer(
        widthPx: Float,
        brush: Brush,
        outline: Outline.Generic,
        pathBounds: Rect,
        maskPath: Path,
        pathBoundsSize: IntSize,
        brushChanged: Boolean,
        outlineChanged: Boolean,
    ) {
        val bounds = borderBounds(pathBounds)
        if (bounds.isEmpty) return
        if (brushChanged || outlineChanged) {
            lastStrokeWidth = Float.NaN
        }
        val strokeWidth = strokeWidthPx(widthPx, bounds)
        val fillArea = fillArea(strokeWidth, bounds)
        if (fillArea) {
            drawPath(outline.path, brush = brush)
        } else {
            val layer = onProvideGraphicsLayer()
            if (layer.isReleased) {
                lastGraphicsLayer = null
                return
            }
            // If nothing else changed but we have a new graphics layer, we need to re-record.
            if (layer != lastGraphicsLayer) {
                lastGraphicsLayer = layer
                lastStrokeWidth = Float.NaN
            }
            layer.compositingStrategy = Offscreen
            translate(pathBounds.left, pathBounds.top) {
                // TODO: if we use colorfilter we can skip re-recording for colors, for solid color
                if (lastStrokeWidth != strokeWidth) {
                    layer.record(pathBoundsSize) {
                        // Paths can have offsets, so translate to keep the drawn path
                        // within the bounds of the mask
                        translate(-pathBounds.left, -pathBounds.top) {
                            // Draw the path with a stroke width twice the provided value.
                            // Because strokes are centered, this will draw both and inner and
                            // outer stroke with the desired stroke width
                            drawPath(
                                path = outline.path,
                                brush = brush,
                                style = obtainStroke(strokeWidth * 2),
                            )

                            // Scale the canvas slightly to cover the background that may be
                            // visible after clearing the outer stroke
                            scale(
                                (pathBoundsSize.width + 1f) / pathBoundsSize.width,
                                (pathBoundsSize.height + 1f) / pathBoundsSize.height,
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
                    lastStrokeWidth = strokeWidth
                }
                drawLayer(layer)
            }
        }
    }

    /**
     * Fallback generic border implementation for API < 28, where [GraphicsLayer] uses the legacy
     * OpenGL ES pipeline and rasterizes paths and [BlendMode.Clear] differently from Skia. Uses an
     * offscreen [ImageBitmap] + [Canvas] for consistent rendering.
     */
    private fun DrawScope.drawGenericBorderWithImageBitmap(
        widthPx: Float,
        brush: Brush,
        outline: Outline.Generic,
        pathBounds: Rect,
        maskPath: Path,
        pathBoundsSize: IntSize,
        brushChanged: Boolean,
        outlineChanged: Boolean,
    ) {
        val bounds = borderBounds(pathBounds)
        if (bounds.isEmpty) return
        if (brushChanged) {
            if (brush is SolidColor) {
                imageBitmapConfig = ImageBitmapConfig.Alpha8
                colorFilter = ColorFilter.tint(brush.value.copy(alpha = 1f))
            } else {
                imageBitmapConfig = ImageBitmapConfig.Argb8888
                colorFilter = null
            }
        }

        if (brushChanged || outlineChanged) {
            lastStrokeWidth = Float.NaN
        }

        val strokeWidth = strokeWidthPx(widthPx, bounds)
        val fillArea = fillArea(strokeWidth, bounds)
        if (fillArea) {
            drawPath(outline.path, brush = brush)
        } else {
            // Only re-render the cached bitmap if something changed - otherwise draw the cached
            // imageBitmap from the previous frame.
            if (lastStrokeWidth != strokeWidth || imageBitmap == null) {
                cacheBorderToImageBitmap(pathBoundsSize, imageBitmapConfig) {
                    translate(-pathBounds.left, -pathBounds.top) {
                        drawPath(
                            path = outline.path,
                            brush = brush,
                            style = obtainStroke(strokeWidth * 2),
                        )
                        scale(
                            (pathBoundsSize.width + 1f) / pathBoundsSize.width,
                            (pathBoundsSize.height + 1f) / pathBoundsSize.height,
                        ) {
                            drawPath(
                                path = maskPath,
                                brush = brush,
                                blendMode = BlendMode.Clear,
                            )
                        }
                    }
                }
                lastStrokeWidth = strokeWidth
            }
            translate(pathBounds.left, pathBounds.top) {
                drawImage(
                    imageBitmap!!,
                    srcSize = pathBoundsSize,
                    colorFilter = colorFilter,
                )
            }
        }
    }

    /**
     * Draws the border into the cached [imageBitmap], (re)allocating it if needed. [imageBitmap]
     * contains the drawn border after this call.
     */
    private inline fun DrawScope.cacheBorderToImageBitmap(
        borderSize: IntSize,
        config: ImageBitmapConfig,
        block: DrawScope.() -> Unit,
    ) {
        var targetImageBitmap = imageBitmap
        var targetCanvas = canvas
        val compatibleConfig =
            targetImageBitmap?.config == ImageBitmapConfig.Argb8888 ||
                config == targetImageBitmap?.config
        if (
            targetImageBitmap == null ||
                targetCanvas == null ||
                borderSize.width > targetImageBitmap.width ||
                borderSize.height > targetImageBitmap.height ||
                !compatibleConfig
        ) {
            targetImageBitmap =
                ImageBitmap(borderSize.width, borderSize.height, config = config).also {
                    imageBitmap = it
                }
            targetCanvas = Canvas(targetImageBitmap).also { canvas = it }
        }

        val targetDrawScope = canvasDrawScope ?: CanvasDrawScope().also { canvasDrawScope = it }
        val drawSize = borderSize.toSize()
        targetDrawScope.draw(this, layoutDirection, targetCanvas, drawSize) {
            drawRect(color = Color.Black, size = drawSize, blendMode = BlendMode.Clear)
            block()
        }
        targetImageBitmap.prepareToDraw()
    }

    private fun obtainPath(): Path = borderPath ?: Path().also { borderPath = it }

    private fun obtainRoundRectInsetPath(): Path =
        roundRectInsetPath ?: Path().also { roundRectInsetPath = it }

    /** Border implementation for simple rounded rects and those with different corner radii */
    private fun DrawScope.drawRoundRectBorder(
        widthPx: Float,
        brush: Brush,
        outline: Outline.Rounded,
        outlineChanged: Boolean,
    ) {
        val roundRect = outline.roundRect
        if (outlineChanged) {
            roundRectBounds = outline.bounds
            isSimpleRoundRect = roundRect.isSimple
            lastStrokeWidth = Float.NaN
        }
        val bounds = borderBounds(roundRectBounds)
        if (bounds.isEmpty) return
        if (isSimpleRoundRect) {
            val strokeWidth = strokeWidthPx(widthPx, bounds)
            val topLeft = topLeft(strokeWidth, bounds)
            val borderSize = borderSize(strokeWidth, bounds)
            val fillArea = fillArea(strokeWidth, bounds)
            val cornerRadius = roundRect.topLeftCornerRadius
            val halfStroke = strokeWidth / 2
            when {
                fillArea -> {
                    // If the drawing area is smaller than the stroke being drawn
                    // drawn all around it just draw a filled in rounded rect
                    drawRoundRect(
                        brush = brush,
                        topLeft = bounds.topLeft,
                        size = bounds.size,
                        cornerRadius = cornerRadius,
                    )
                }
                cornerRadius.x < halfStroke -> {
                    // If the corner radius is smaller than half of the stroke width
                    // then the interior curvature of the stroke will be a sharp edge
                    // In this case just draw a normal filled in rounded rect with the
                    // desired corner radius but clipping out the interior rectangle
                    clipRect(
                        bounds.left + strokeWidth,
                        bounds.top + strokeWidth,
                        bounds.right - strokeWidth,
                        bounds.bottom - strokeWidth,
                        clipOp = ClipOp.Difference,
                    ) {
                        drawRoundRect(
                            brush = brush,
                            topLeft = bounds.topLeft,
                            size = bounds.size,
                            cornerRadius = cornerRadius,
                        )
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
                        style = obtainStroke(strokeWidth),
                    )
                }
            }
        } else {
            // The border path is only rebuilt when the stroke width changes (or the outline
            // changes).
            val path = obtainPath()
            val strokeWidthPx = strokeWidthPx(widthPx, bounds)
            if (lastStrokeWidth != strokeWidthPx) {
                val fillArea = fillArea(strokeWidthPx, bounds)
                val insetPath = obtainRoundRectInsetPath()
                setRoundRectPath(path, insetPath, roundRect, strokeWidthPx, fillArea)
                lastStrokeWidth = strokeWidthPx
            }
            drawPath(path, brush = brush)
        }
    }

    /** Border implementation for rectangular borders */
    private fun DrawScope.drawRectBorder(
        widthPx: Float,
        brush: Brush,
        outline: Outline.Rectangle,
    ) {
        val bounds = borderBounds(outline.rect)
        if (bounds.isEmpty) return
        val strokeWidthPx = strokeWidthPx(widthPx, bounds)
        val topLeft = topLeft(strokeWidthPx, bounds)
        val borderSize = borderSize(strokeWidthPx, bounds)
        val fillArea = fillArea(strokeWidthPx, bounds)
        // If we are drawing a rectangular stroke, just offset it by half the stroke
        // width as strokes are always drawn centered on their geometry.
        // If the border is larger than the drawing area, just fill the area with a
        // solid rectangle
        val rectTopLeft = if (fillArea) bounds.topLeft else topLeft
        val size = if (fillArea) bounds.size else borderSize
        val style = if (fillArea) Fill else obtainStroke(strokeWidthPx)
        drawRect(brush = brush, topLeft = rectTopLeft, size = size, style = style)
    }
}

/**
 * Helper method that sets [targetPath] to a round rect with the inner region removed by the given
 * stroke width. [insetPath] is used as a temporary path for the inner region.
 */
private fun setRoundRectPath(
    targetPath: Path,
    insetPath: Path,
    roundedRect: RoundRect,
    strokeWidth: Float,
    fillArea: Boolean,
) {
    targetPath.reset()
    targetPath.addRoundRect(roundedRect)
    if (!fillArea) {
        insetPath.reset()
        insetPath.addRoundRect(
            RoundRect(
                left = roundedRect.left + strokeWidth,
                top = roundedRect.top + strokeWidth,
                right = roundedRect.right - strokeWidth,
                bottom = roundedRect.bottom - strokeWidth,
                topLeftCornerRadius = roundedRect.topLeftCornerRadius.shrink(strokeWidth),
                topRightCornerRadius = roundedRect.topRightCornerRadius.shrink(strokeWidth),
                bottomLeftCornerRadius = roundedRect.bottomLeftCornerRadius.shrink(strokeWidth),
                bottomRightCornerRadius = roundedRect.bottomRightCornerRadius.shrink(strokeWidth),
            )
        )
        targetPath.op(targetPath, insetPath, PathOperation.Difference)
    }
}

/**
 * Helper method to shrink the corner radius by the given value, clamping to 0 if the resultant
 * corner radius would be negative
 */
private fun CornerRadius.shrink(value: Float): CornerRadius =
    CornerRadius(max(0f, this.x - value), max(0f, this.y - value))

/**
 * Whether generic border rendering should use a [GraphicsLayer] with [Offscreen] compositing, or
 * fall back to an offscreen [ImageBitmap].
 */
internal expect fun shouldUseGraphicsLayerForGenericBorder(): Boolean
