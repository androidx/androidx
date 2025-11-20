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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil

/**
 * [Painter] implementation that draws a drop shadow with the geometry defined by the specified
 * shape and [Shadow].
 */
class DropShadowPainter
internal constructor(
    private val shape: Shape,
    private val shadow: Shadow,
    private val renderCreator: DropShadowRendererProvider,
) : Painter() {

    /**
     * Create a [DropShadowPainter] with the specified [shape] and [shadow]. It is preferred to
     * obtain an instance of the [DropShadowPainter] through a [ShadowContext] instance instead, as
     * the underlying shadow dependencies can be shared across multiple [DropShadowPainter]
     * instances. However, creating an instance through this constructor will not share resources
     * with any other [DropShadowPainter].
     *
     * @param shape Shape of the shadow
     * @param shadow Parameters used to render the shadow
     */
    constructor(
        shape: Shape,
        shadow: Shadow,
    ) : this(shape, shadow, DropShadowRendererProvider.Default)

    /* Painter properties */
    private var alpha: Float = 1f
    private var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val renderer =
            renderCreator.obtainDropShadowRenderer(shape, size, layoutDirection, this, shadow)
        translate(shadow.offset.x.toPx(), shadow.offset.y.toPx()) {
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

internal fun interface DropShadowRendererProvider {
    fun obtainDropShadowRenderer(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        shadow: Shadow,
    ): DropShadowRenderer

    companion object {
        val Default =
            DropShadowRendererProvider { shape, size, layoutDirection, density, dropShadow ->
                DropShadowRenderer(dropShadow, shape.createOutline(size, layoutDirection, density))
            }
    }
}

/**
 * Class responsible for rendering a drop shadow with the specified [outline]. While each call site
 * may have their own [DropShadowPainter], each instance may share the same [DropShadowRenderer] if
 * the generated outline is the same.
 *
 * Note that the offset in the [Shadow] is ignored. This is an optimisation since the same
 * [DropShadowRenderer] may be reused as long as the outline and the other shadow parameters are the
 * same. The [DropShadowPainter] or other user of [DropShadowRenderer] is responsible for applying
 * the offset.
 */
internal class DropShadowRenderer(val shadow: Shadow, outline: Outline) : ShadowRenderer(outline) {

    private val paint = Paint()
    private var shadowBitmap: ImageBitmap? = null
    private var compositeShader: CompositeShaderBrush? = null

    override fun DrawScope.buildShadow(size: Size, cornerRadius: CornerRadius, path: Path?) {
        val radius = shadow.radius.toPx()
        val spread = shadow.spread.toPx()
        shadowBitmap =
            if (path != null) {
                createOuterShadowBitmap(size, path, radius, spread)
            } else {
                createOuterShadowBitmap(size, radius, spread, cornerRadius)
            }
    }

    private fun obtainCompositeBrush(shadowBitmap: ImageBitmap, brush: Brush): Brush {
        var shader = compositeShader
        // TODO(b/418840915): Reduce unnecessary shader recreation.
        if (shader == null || shader.srcBrush != brush) {
            shader =
                (Brush.composite(
                        ShaderBrush(ImageShader(shadowBitmap)),
                        // Ensure that the shader we are blending against is the same dimensions
                        // as the shadow bitmap
                        if (brush is ShaderBrush) {
                            ShaderBrush(
                                brush.createShader(
                                    Size(
                                        shadowBitmap.width.toFloat(),
                                        shadowBitmap.height.toFloat(),
                                    )
                                )
                            )
                        } else {
                            brush
                        },
                        BlendMode.SrcIn,
                    ) as CompositeShaderBrush)
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
        shadowBitmap?.let { shadowBitmap ->
            val offset = -(shadow.radius.toPx() + shadow.spread.toPx())
            if (brush != null && colorFilter == null) {
                val shaderBrush = obtainCompositeBrush(shadowBitmap, brush)
                // Explicitly translate the DrawScope and draw a rectangle with the
                // size of the shadow including the additional padding for the shadow radius
                // and spread in order to ensure that the ShaderBrush is not clipped
                translate(offset, offset) {
                    drawRect(
                        brush = shaderBrush,
                        size = Size(shadowBitmap.width.toFloat(), shadowBitmap.height.toFloat()),
                        alpha = alpha,
                        blendMode = blendMode,
                    )
                }
            } else {
                drawImage(
                    shadowBitmap,
                    topLeft = Offset(offset, offset),
                    alpha = alpha,
                    colorFilter = colorFilter,
                    blendMode = blendMode,
                )
            }
        }
    }

    private fun createOuterShadowBitmap(
        size: Size,
        path: Path,
        radius: Float,
        spread: Float,
    ): ImageBitmap {
        val outset = radius * 2 + spread * 2
        val shadowWidth = size.width + outset
        val shadowHeight = size.height + outset
        val shadowBitmap =
            ImageBitmap(
                ceil(shadowWidth).toInt(),
                ceil(shadowHeight).toInt(),
                ImageBitmapConfig.Alpha8,
            )
        val shadowCanvas = androidx.compose.ui.graphics.Canvas(shadowBitmap)
        with(shadowCanvas) {
            if (spread > 0f) {
                translate(radius + spread, radius + spread)
                drawPath(path, paint)
                drawPath(
                    path,
                    paint
                        .configureShadow(
                            style = PaintingStyle.Stroke,
                            blurFilter =
                                if (radius > 0) {
                                    BlurFilter(radius)
                                } else {
                                    null
                                },
                        )
                        .apply { strokeWidth = spread * 2f },
                )
            } else {
                paint.configureShadow(
                    blurFilter =
                        if (radius > 0) {
                            BlurFilter(radius)
                        } else {
                            null
                        }
                )
                translate(radius, radius)
                drawPath(path, paint)
            }
        }
        return shadowBitmap
    }

    private fun createOuterShadowBitmap(
        size: Size,
        shadowRadius: Float,
        spread: Float,
        cornerRadius: CornerRadius,
    ): ImageBitmap {
        val outset = shadowRadius * 2 + spread * 2
        val shadowWidth = size.width + outset
        val shadowHeight = size.height + outset
        val shadowBitmap =
            ImageBitmap(
                ceil(shadowWidth).toInt(),
                ceil(shadowHeight).toInt(),
                ImageBitmapConfig.Alpha8,
            )
        val shadowCanvas = androidx.compose.ui.graphics.Canvas(shadowBitmap)
        with(shadowCanvas) {
            drawRoundRect(
                shadowRadius,
                shadowRadius,
                shadowWidth - shadowRadius,
                shadowHeight - shadowRadius,
                cornerRadius.x,
                cornerRadius.y,
                paint.configureShadow(
                    blurFilter =
                        if (shadowRadius > 0) {
                            BlurFilter(shadowRadius)
                        } else {
                            null
                        }
                ),
            )
        }
        return shadowBitmap
    }
}
