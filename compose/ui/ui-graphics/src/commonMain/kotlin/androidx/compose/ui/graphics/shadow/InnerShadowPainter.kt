/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics.shadow

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositeShaderBrush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toShaderBrush
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.max

/**
 * [Painter] implementation that draws an inner shadow with the geometry defined by the specified
 * shape and [Shadow].
 */
class InnerShadowPainter
internal constructor(
    private val shape: Shape,
    private val shadow: Shadow,
    private val renderCreator: InnerShadowRendererProvider = InnerShadowRendererProvider.Default,
) : Painter() {

    /**
     * Create an [InnerShadowPainter] with the specified [shape] and [shadow]. It is preferred to
     * obtain an instance of the [InnerShadowPainter] through a [ShadowContext] instance instead, as
     * the underlying shadow dependencies can be shared across multiple [InnerShadowPainter]
     * instances. However, creating an instance through this constructor will not share resources
     * with any other [InnerShadowPainter].
     *
     * @param shape Shape of the shadow
     * @param shadow Parameters used to render the shadow
     */
    constructor(
        shape: Shape,
        shadow: Shadow,
    ) : this(shape, shadow, InnerShadowRendererProvider.Default)

    /* Painter properties */
    private var alpha: Float = 1f
    private var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val renderer =
            renderCreator.obtainInnerShadowRenderer(shape, size, layoutDirection, this, shadow)
        with(renderer) {
            drawShadow(
                colorFilter,
                size,
                shadow.color,
                shadow.brush,
                (alpha * shadow.alpha).coerceIn(0f, 1f),
                shadow.blendMode,
            )
        }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        this.layoutDirection = layoutDirection
        return true
    }
}

internal fun interface InnerShadowRendererProvider {
    fun obtainInnerShadowRenderer(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        shadow: Shadow,
    ): InnerShadowRenderer

    companion object {
        val Default =
            InnerShadowRendererProvider { shape, size, layoutDirection, density, innerShadow ->
                InnerShadowRenderer(
                    innerShadow,
                    shape.createOutline(size, layoutDirection, density),
                )
            }
    }
}

/**
 * Class responsible for rendering an inner shadow with the specified [outline] and [shadow]. While
 * each call site may have their own [InnerShadowPainter], each instance may share the same
 * [InnerShadowRenderer] if the generated outline is the same.
 */
internal class InnerShadowRenderer(private val shadow: Shadow, outline: Outline) :
    ShadowRenderer(outline) {

    /** Paint used to draw the shadow with a Blur */
    private val paint = Paint()

    private var shadowMask: ShaderBrush? = null
    private var compositeShader: CompositeShaderBrush? = null

    /** Optional transform used translate the inner shadow on the corresponding shader mask */
    private var matrix: Matrix? = null

    /** Return the previously cached Matrix or allocate a new one if none exists */
    private fun obtainMatrix(): Matrix = matrix ?: Matrix().also { matrix = it }

    override fun DrawScope.buildShadow(size: Size, cornerRadius: CornerRadius, path: Path?) {
        val radius = shadow.radius.toPx()
        val spread = shadow.spread.toPx()
        val offsetX = shadow.offset.x.toPx()
        val offsetY = shadow.offset.y.toPx()
        shadowMask =
            if (path != null) {
                createInnerPathShadowBrush(size, path, radius, spread, offsetX, offsetY)
            } else {
                createInnerShadowBrush(size, radius, spread, offsetX, offsetY, cornerRadius)
            }
    }

    private fun obtainCompositeBrush(shadowMask: ShaderBrush, brush: Brush): CompositeShaderBrush {
        var shader = compositeShader
        if (shader == null || shader.srcBrush != brush) {
            shader =
                CompositeShaderBrush(
                        dstBrush = shadowMask.toShaderBrush(),
                        srcBrush = brush.toShaderBrush(),
                        blendMode = BlendMode.SrcIn,
                    )
                    .also { compositeShader = it }
        }
        return shader
    }

    override fun DrawScope.onDrawShadow(
        size: Size,
        cornerRadius: CornerRadius,
        path: Path?,
        alpha: Float,
        colorFilter: ColorFilter?,
        brush: Brush?,
        blendMode: BlendMode,
    ) {
        shadowMask?.let { mask ->
            val targetBrush =
                if (shadow.brush is ShaderBrush) {
                    // If we have a Brush to blend against then create/reuse a ComposeShader with
                    // the shadow texture blended against the gradient
                    obtainCompositeBrush(mask, shadow.brush)
                } else {
                    // Otherwise, draw with the BitmapShader of the shadow itself
                    mask
                }
            if (path != null) {
                // We have a generic path based shadow geometry. Draw the shadow with the specified
                // brush and path.
                drawPath(
                    path,
                    brush = targetBrush,
                    colorFilter = colorFilter,
                    alpha = alpha,
                    blendMode = blendMode,
                )
            } else if (cornerRadius == CornerRadius.Zero) {
                // We have a shadow geometry that can be represented by a rect.
                drawRect(
                    brush = targetBrush,
                    colorFilter = colorFilter,
                    alpha = alpha,
                    blendMode = blendMode,
                )
            } else {
                // We have a shadow geometry that can be represented by a round rect.
                drawRoundRect(
                    brush = targetBrush,
                    cornerRadius = cornerRadius,
                    colorFilter = colorFilter,
                    alpha = alpha,
                    blendMode = shadow.blendMode,
                )
            }
        }
    }

    private fun createInnerPathShadowBrush(
        size: Size,
        path: Path,
        radius: Float,
        spread: Float,
        offsetX: Float,
        offsetY: Float,
    ): ShaderBrush {
        val pathBitmap: ImageBitmap?
        val widthPx = ceil(size.width).toInt()
        val heightPx = ceil(size.height).toInt()
        // If a non-zero spread is provided, create an intermediate bitmap used to draw the path
        // and mask off the spread with a stroke of the original path geometry. Because the stroke
        // width
        // is centered, half of the spread is applied to the exterior and interior of the path, so
        // double the spread to mask off the path accordingly
        if (spread > 0f) {
            val pathBounds = path.getBounds()
            val pathWidth = pathBounds.width
            val pathHeight = pathBounds.height
            pathBitmap =
                ImageBitmap(
                    ceil(pathWidth).toInt(),
                    ceil(pathHeight).toInt(),
                    ImageBitmapConfig.Alpha8,
                )
            val pathCanvas = androidx.compose.ui.graphics.Canvas(pathBitmap)
            with(pathCanvas) {
                drawPath(path, paint)
                clipRect(0f, 0f, pathWidth, pathHeight)
                drawPath(
                    path,
                    paint
                        .configureShadow(style = PaintingStyle.Stroke, blendMode = BlendMode.Clear)
                        .apply { strokeWidth = spread * 2f },
                )
            }
        } else {
            pathBitmap = null
        }

        // Add a pixel on each size to inset the drawn content. This ensures
        // that the edges do not get clamped when being consumed by the
        // ImageShader
        val clampPadding = ceil(radius).toInt()
        val shadowBitmap =
            ImageBitmap(
                widthPx + clampPadding * 2,
                heightPx + clampPadding * 2,
                ImageBitmapConfig.Alpha8,
            )

        val shadowCanvas = androidx.compose.ui.graphics.Canvas(shadowBitmap)
        with(shadowCanvas) {
            if (pathBitmap != null) {
                drawRect(
                    0f,
                    0f,
                    shadowBitmap.width.toFloat(),
                    shadowBitmap.height.toFloat(),
                    paint.configureShadow(), // Draw with default params
                )
                // If we have a pathBitmap then we have a non-zero spread and we needed to rasterize
                // the path into an intermediate bitmap in order to mask off the path with a stroke
                // by
                // the spread size

                drawImage(
                    pathBitmap,
                    Offset(offsetX, offsetY),
                    paint.configureShadow(
                        blurFilter =
                            if (radius > 0f) {
                                BlurFilter(radius)
                            } else {
                                null
                            },
                        blendMode = BlendMode.Xor,
                    ),
                )

                return ShaderBrush(ImageShader(shadowBitmap))
            } else {
                save()
                translate(offsetX, offsetY)

                // ... otherwise we can just draw the path with the blur applied to it
                drawPath(
                    path,
                    paint.configureShadow(
                        blurFilter =
                            if (radius > 0f) {
                                BlurFilter(radius)
                            } else {
                                null
                            }
                    ),
                )
                restore()
                drawRect(
                    0f,
                    0f,
                    shadowBitmap.width.toFloat(),
                    shadowBitmap.height.toFloat(),
                    paint.configureShadow(blendMode = BlendMode.Xor),
                )

                return ShaderBrush(ImageShader(shadowBitmap))
            }
        }
    }

    private fun createInnerShadowBrush(
        size: Size,
        radius: Float,
        spread: Float,
        offsetX: Float,
        offsetY: Float,
        cornerRadius: CornerRadius,
    ): ShaderBrush {
        val shadowBitmap =
            ImageBitmap(
                ceil(size.width).toInt(),
                ceil(size.height).toInt(),
                ImageBitmapConfig.Alpha8,
            )
        val shadowCanvas = androidx.compose.ui.graphics.Canvas(shadowBitmap)
        with(shadowCanvas) {
            val left = offsetX + spread
            val top = offsetY + spread
            val right = max(left, offsetX + size.width - spread)
            val bottom = max(top, offsetY + size.height - spread)

            drawRoundRect(
                left,
                top,
                right,
                bottom,
                cornerRadius.x,
                cornerRadius.y,
                paint.configureShadow(
                    blurFilter =
                        if (radius > 0) {
                            BlurFilter(radius)
                        } else {
                            null
                        }
                ),
            )
            drawRect(
                0f,
                0f,
                shadowBitmap.width.toFloat(),
                shadowBitmap.height.toFloat(),
                paint.configureShadow(blendMode = BlendMode.Xor),
            )
        }

        return ShaderBrush(ImageShader(shadowBitmap))
    }
}
