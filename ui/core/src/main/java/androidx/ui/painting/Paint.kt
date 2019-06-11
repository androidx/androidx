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

package androidx.ui.painting

import android.graphics.BlurMaskFilter
import android.graphics.PorterDuffColorFilter
import androidx.ui.graphics.Color

class Paint {

    private var internalPaint = android.graphics.Paint()
    private var porterDuffMode = android.graphics.PorterDuff.Mode.SRC_OVER
    private var blurStyle = android.graphics.BlurMaskFilter.Blur.NORMAL
    private var blurRadius = 0.0f
    private var internalShader: Shader? = null
    private var internalColorFilter: ColorFilter? = null

    fun asFrameworkPaint(): android.graphics.Paint = internalPaint

    var alpha: Float
        get() = internalPaint.alpha / 255.0f
        set(value) {
            internalPaint.alpha = kotlin.math.round(value * 255.0f).toInt()
        }

    // Whether to apply anti-aliasing to lines and images drawn on the
    // canvas.
    //
    // Defaults to true.
    var isAntiAlias: Boolean
            get() = internalPaint.isAntiAlias
            set(value) {
                // We encode true as zero and false as one because the default value, which
                // we always encode as zero, is true.
                // final int encoded = value ? 0 : 1;
                // _data.setInt32(_kIsAntiAliasOffset, encoded, _kFakeHostEndian);
                internalPaint.isAntiAlias = value
            }

    // The color to use when stroking or filling a shape.
    //
    // Defaults to opaque black.
    //
    // See also:
    //
    //  * [style], which controls whether to stroke or fill (or both).
    //  * [colorFilter], which overrides [color].
    //  * [shader], which overrides [color] with more elaborate effects.
    //
    // This color is not used when compositing. To colorize a layer, use
    // [colorFilter].
    var color: Color
        get() = Color(internalPaint.color)
        set(color) {
            internalPaint.color = color.toArgb()
        }

    // A blend mode to apply when a shape is drawn or a layer is composited.
    //
    // The source colors are from the shape being drawn (e.g. from
    // [Canvas.drawPath]) or layer being composited (the graphics that were drawn
    // between the [Canvas.saveLayer] and [Canvas.restore] calls), after applying
    // the [colorFilter], if any.
    //
    // The destination colors are from the background onto which the shape or
    // layer is being composited.
    //
    // Defaults to [BlendMode.srcOver].
    //
    // See also:
    //
    //  * [Canvas.saveLayer], which uses its [Paint]'s [blendMode] to composite
    //    the layer when [restore] is called.
    //  * [BlendMode], which discusses the user of [saveLayer] with [blendMode].
    var blendMode: BlendMode
        get() {
            return porterDuffModeToBlendMode(porterDuffMode)
        }
        set(value) {
            porterDuffMode = value.toPorterDuffMode()
            internalPaint.xfermode = android.graphics.PorterDuffXfermode(porterDuffMode)
        }

    private fun porterDuffModeToBlendMode(porterDuffMode: android.graphics.PorterDuff.Mode):
            BlendMode {
        return when (porterDuffMode) {
            android.graphics.PorterDuff.Mode.CLEAR -> BlendMode.clear
            android.graphics.PorterDuff.Mode.SRC -> BlendMode.src
            android.graphics.PorterDuff.Mode.DST -> BlendMode.dst
            android.graphics.PorterDuff.Mode.SRC_OVER -> BlendMode.srcOver
            android.graphics.PorterDuff.Mode.DST_OVER -> BlendMode.dstOver
            android.graphics.PorterDuff.Mode.SRC_IN -> BlendMode.srcIn
            android.graphics.PorterDuff.Mode.DST_IN -> BlendMode.dstIn
            android.graphics.PorterDuff.Mode.SRC_OUT -> BlendMode.srcOut
            android.graphics.PorterDuff.Mode.DST_OUT -> BlendMode.dstOut
            android.graphics.PorterDuff.Mode.SRC_ATOP -> BlendMode.srcATop
            android.graphics.PorterDuff.Mode.DST_ATOP -> BlendMode.dstATop
            android.graphics.PorterDuff.Mode.XOR -> BlendMode.xor
            android.graphics.PorterDuff.Mode.DARKEN -> BlendMode.darken
            android.graphics.PorterDuff.Mode.LIGHTEN -> BlendMode.lighten
            android.graphics.PorterDuff.Mode.MULTIPLY -> BlendMode.multiply
            android.graphics.PorterDuff.Mode.SCREEN -> BlendMode.screen
            android.graphics.PorterDuff.Mode.ADD -> BlendMode.plus
            android.graphics.PorterDuff.Mode.OVERLAY -> BlendMode.overlay
        }
    }

    // Whether to paint inside shapes, the edges of shapes, or both.
    //
    // Defaults to [PaintingStyle.fill].
    var style: PaintingStyle
        get() {
            return when (internalPaint.style) {
                android.graphics.Paint.Style.STROKE -> PaintingStyle.stroke
                else -> PaintingStyle.fill
            }
        }
        set(value) {
            // TODO(Migration/njawad: Platform also supports Paint.Style.FILL_AND_STROKE)
            val style = when (value) {
                PaintingStyle.stroke -> android.graphics.Paint.Style.STROKE
                else -> android.graphics.Paint.Style.FILL
            }
            internalPaint.style = style
        }

    // How wide to make edges drawn when [style] is set to
    // [PaintingStyle.stroke]. The width is given in logical pixels measured in
    // the direction orthogonal to the direction of the path.
    //
    // Defaults to 0.0, which correspond to a hairline width.
    var strokeWidth: Float
        get() = internalPaint.strokeWidth
        set(value) {
            internalPaint.strokeWidth = value
        }

    // The kind of finish to place on the end of lines drawn when
    // [style] is set to [PaintingStyle.stroke].
    //
    // Defaults to [StrokeCap.butt], i.e. no caps.
    var strokeCap: StrokeCap
        get() {
            return when (internalPaint.strokeCap) {
                android.graphics.Paint.Cap.BUTT -> StrokeCap.butt
                android.graphics.Paint.Cap.ROUND -> StrokeCap.round
                android.graphics.Paint.Cap.SQUARE -> StrokeCap.square
                else -> StrokeCap.butt
            }
        }
        set(value) {
            internalPaint.strokeCap = when (value) {
                StrokeCap.square -> android.graphics.Paint.Cap.SQUARE
                StrokeCap.round -> android.graphics.Paint.Cap.ROUND
                StrokeCap.butt -> android.graphics.Paint.Cap.BUTT
            }
        }

    // The kind of finish to place on the joins between segments.
    //
    // This applies to paths drawn when [style] is set to [PaintingStyle.stroke],
    // It does not apply to points drawn as lines with [Canvas.drawPoints].
    //
    // Defaults to [StrokeJoin.miter], i.e. sharp corners. See also
    // [strokeMiterLimit] to control when miters are replaced by bevels.
    var strokeJoin: StrokeJoin
        get() {
            return when (internalPaint.strokeJoin) {
                android.graphics.Paint.Join.MITER -> StrokeJoin.miter
                android.graphics.Paint.Join.BEVEL -> StrokeJoin.bevel
                android.graphics.Paint.Join.ROUND -> StrokeJoin.round
                else -> StrokeJoin.miter
            }
        }
        set(value) {
            internalPaint.strokeJoin = when (value) {
                StrokeJoin.miter -> android.graphics.Paint.Join.MITER
                StrokeJoin.bevel -> android.graphics.Paint.Join.BEVEL
                StrokeJoin.round -> android.graphics.Paint.Join.ROUND
            }
        }

    // The limit for miters to be drawn on segments when the join is set to
    // [StrokeJoin.miter] and the [style] is set to [PaintingStyle.stroke]. If
    // this limit is exceeded, then a [StrokeJoin.bevel] join will be drawn
    // instead. This may cause some 'popping' of the corners of a path if the
    // angle between line segments is animated.
    //
    // This limit is expressed as a limit on the length of the miter.
    //
    // Defaults to 4.0.  Using zero as a limit will cause a [StrokeJoin.bevel]
    // join to be used all the time.
    var strokeMiterLimit: Float
        get() = internalPaint.strokeMiter
        set(value) {
            internalPaint.strokeMiter = value
        }

    // A mask filter (for example, a blur) to apply to a shape after it has been
    // drawn but before it has been composited into the image.
    //
    // See [MaskFilter] for details.
    var maskFilter: MaskFilter
        get() {
            val style = when (blurStyle) {
                android.graphics.BlurMaskFilter.Blur.NORMAL -> BlurStyle.normal
                android.graphics.BlurMaskFilter.Blur.SOLID -> BlurStyle.solid
                android.graphics.BlurMaskFilter.Blur.OUTER -> BlurStyle.outer
                android.graphics.BlurMaskFilter.Blur.INNER -> BlurStyle.inner
            }
            // sigma is equivalent to roughly half the radius: sigma = radius / 2
            return MaskFilter(style, blurRadius / 2.0f)
        }
        set(value) {
            val blur = when (value.style) {
                BlurStyle.inner -> android.graphics.BlurMaskFilter.Blur.INNER
                BlurStyle.normal -> android.graphics.BlurMaskFilter.Blur.NORMAL
                BlurStyle.outer -> android.graphics.BlurMaskFilter.Blur.OUTER
                BlurStyle.solid -> android.graphics.BlurMaskFilter.Blur.SOLID
            }

            // radius is equivalent to roughly twice the sigma: radius = sigma * 2
            // TODO(Migration/njawad: Add support for framework EmbossMaskFilter?)
            internalPaint.maskFilter = BlurMaskFilter((value.sigma * 2), blur)
        }

    // Controls the performance vs quality trade-off to use when applying
    // filters, such as [maskFilter], or when drawing images, as with
    // [Canvas.drawImageRect] or [Canvas.drawImageNine].
    //
    // Defaults to [FilterQuality.none].
    // TODO(ianh): verify that the image drawing methods actually respect this
    var filterQuality: FilterQuality
        get() =
            if (!internalPaint.isFilterBitmap) {
                FilterQuality.none
            } else {
                // TODO(Migration/njawad: Align with Framework APIs)
                // Framework only supports bilinear filtering which maps to FilterQuality.low
                // FilterQuality.medium and FilterQuailty.high refer to a combination of
                // bilinear interpolation, pyramidal parameteric prefiltering (mipmaps) as well as
                // bicubic interpolation respectively
                FilterQuality.low
            }
        set(value) {
            internalPaint.isFilterBitmap = value != FilterQuality.none
        }

    // The shader to use when stroking or filling a shape.
    //
    // When this is null, the [color] is used instead.
    //
    // See also:
    //
    //  * [Gradient], a shader that paints a color gradient.
    //  * [ImageShader], a shader that tiles an [Image].
    //  * [colorFilter], which overrides [shader].
    //  * [color], which is used if [shader] and [colorFilter] are null.
    var shader: Shader?
        get() = internalShader
        set(value) {
            internalShader = value
            internalPaint.shader = internalShader?.toFrameworkShader()
        }

    // A color filter to apply when a shape is drawn or when a layer is
    // composited.
    //
    // See [ColorFilter] for details.
    //
    // When a shape is being drawn, [colorFilter] overrides [color] and [shader].
    var colorFilter: ColorFilter?
        get() = internalColorFilter
        set(value) {
            internalColorFilter = value
            if (value != null) {
                internalPaint.colorFilter = PorterDuffColorFilter(
                        value.color.toArgb(),
                        value.blendMode.toPorterDuffMode()
                )
            } else {
                internalPaint.colorFilter = null
            }
        }
}
