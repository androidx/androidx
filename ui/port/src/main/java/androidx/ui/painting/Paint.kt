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
import java.nio.ByteBuffer

class Paint {

    private var internalPaint = android.graphics.Paint()
    private var porterDuffMode = android.graphics.PorterDuff.Mode.SRC_OVER
    private var blurStyle = android.graphics.BlurMaskFilter.Blur.NORMAL
    private var blurRadius = 0.0f

    companion object {
        // Paint objects are encoded in two buffers:
        //
        // * _data is binary data in four-byte fields, each of which is either a
        //   uint32_t or a float. The default value for each field is encoded as
        //   zero to make initialization trivial. Most values already have a default
        //   value of zero, but some, such as color, have a non-zero default value.
        //   To encode or decode these values, XOR the value with the default value.
        //
        // * _objects is a list of unencodable objects, typically wrappers for native
        //   objects. The objects are simply stored in the list without any additional
        //   encoding.
        //
        // The binary format must match the deserialization code in paint.cc.

        // If you add more fields, remember to update DATA_BYTE_COUNT.
        const val DATA_BYTE_COUNT = 75

        val _data = ByteBuffer.allocate(DATA_BYTE_COUNT)
        const val IS_ANTI_ALIAS_INDEX: Int = 0
        const val COLOR_INDEX = 1
        const val BLENDMODE_INDEX = 2
        const val STYLE_INDEX = 3
        const val STROKE_WIDTH_INDEX = 4
        const val STROKE_CAP_INDEX = 5
        const val STROKE_JOIN_INDEX = 6
        const val STROKE_MITER_LIMIT_INDEX = 7
        const val FILTER_QUALITY_INDEX = 8
        const val COLOR_FILTER_INDEX = 9
        const val COLOR_FILTER_COLOR_INDEX = 10
        const val COLOR_FILTER_BLENDMODE_INDEX = 11
        const val MASK_FILTER_INDEX = 12
        const val MASK_FILTER_BLUR_STYLE_INDEX = 13
        const val MASK_FILTER_SIGMA_INDEX = 14

        const val IS_ANTIALIAS_OFFSET = IS_ANTI_ALIAS_INDEX.shl(2)
        const val COLOR_OFFSET = COLOR_INDEX.shl(2)
        const val BLENDMODE_OFFSET = BLENDMODE_INDEX.shl(2)
        const val STYLE_OFFSET = STYLE_INDEX.shl(2)
        const val STROKEWIDTH_OFFSET = STROKE_WIDTH_INDEX.shl(2)
        const val STROKE_CAP_OFFSET = STROKE_CAP_INDEX.shl(2)
        const val STROKE_JOIN_OFFSET = STROKE_JOIN_INDEX.shl(2)
        const val STROKE_MITER_LIMIT_OFFSET = STROKE_MITER_LIMIT_INDEX.shl(2)
        const val FILTER_QUALITY_OFFSET = FILTER_QUALITY_INDEX.shl(2)
        const val COLOR_FILTER_OFFSET = COLOR_FILTER_INDEX.shl(2)
        const val COLOR_FILTER_COLOR_OFFSET = COLOR_FILTER_COLOR_INDEX.shl(2)
        const val COLOR_FILTER_BLENDMODE_OFFSET = COLOR_FILTER_BLENDMODE_INDEX.shl(2)
        const val MASK_FILTER_OFFSET = MASK_FILTER_INDEX.shl(2)
        const val MASK_FILTER_BLUR_STYLE_OFFSET = MASK_FILTER_BLUR_STYLE_INDEX.shl(2)
        const val MASK_FILTER_SIGMA_OFFSET = MASK_FILTER_SIGMA_INDEX.shl(2)

        // // Binary format must match the deserialization code in paint.cc.
        // val List<dynamic> _objects;
        const val SHADER_INDEX = 0
        const val OBJECT_COUNT = 1 // Must be one larger than the largest index.

        // Must be kept in sync with the default in paint.cc.
        const val COLOR_DEFAULT = 0xFF000000

        //  Must be kept in sync with the default in paint.cc.
        val BLENDMODE_DEFAULT = BlendMode.srcOver.ordinal

        // Must be kept in sync with the default in paint.cc.
        const val STROKE_MITER_LIMIT_DEFAULT = 4.0
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
            internalPaint.color = color.value
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
            porterDuffMode = blendModeToPorterDuffMode(value)
            internalPaint.xfermode = android.graphics.PorterDuffXfermode(porterDuffMode)
        }

    private fun blendModeToPorterDuffMode(blendmode: BlendMode): android.graphics.PorterDuff.Mode {
        return when (blendmode) {
            BlendMode.clear -> android.graphics.PorterDuff.Mode.CLEAR
            BlendMode.src -> android.graphics.PorterDuff.Mode.SRC
            BlendMode.dst -> android.graphics.PorterDuff.Mode.DST
            BlendMode.srcOver -> android.graphics.PorterDuff.Mode.SRC_OVER
            BlendMode.dstOver -> android.graphics.PorterDuff.Mode.DST_OVER
            BlendMode.srcIn -> android.graphics.PorterDuff.Mode.SRC_IN
            BlendMode.dstIn -> android.graphics.PorterDuff.Mode.DST_IN
            BlendMode.srcOut -> android.graphics.PorterDuff.Mode.SRC_OUT
            BlendMode.dstOut -> android.graphics.PorterDuff.Mode.DST_OUT
            BlendMode.srcATop -> android.graphics.PorterDuff.Mode.SRC_ATOP
            BlendMode.dstATop -> android.graphics.PorterDuff.Mode.DST_ATOP
            BlendMode.xor -> android.graphics.PorterDuff.Mode.XOR
            BlendMode.darken -> android.graphics.PorterDuff.Mode.DARKEN
            BlendMode.lighten -> android.graphics.PorterDuff.Mode.LIGHTEN
            BlendMode.multiply -> android.graphics.PorterDuff.Mode.MULTIPLY
            BlendMode.screen -> android.graphics.PorterDuff.Mode.SCREEN
            BlendMode.overlay -> android.graphics.PorterDuff.Mode.OVERLAY
            BlendMode.plus -> android.graphics.PorterDuff.Mode.ADD
            BlendMode.color ->
                TODO("Migration/njawad: PorterDuff.Mode.COLOR is not supported")
            BlendMode.colorBurn ->
                TODO("Migration/njawad: PorterDuff.Mode.COLOR_BURN is not supported")
            BlendMode.colorDodge ->
                TODO("Migration/njawad: PorterDuff.Mode.COLOR_DODGE is not supported")
            BlendMode.difference ->
                TODO("Migration/njawad: PorterDuff.Mode.DIFFERENCE is not supported")
            BlendMode.exclusion ->
                TODO("Migration/njawad: PorterDuff.Mode.EXCLUSION is not supported")
            BlendMode.hardLight ->
                TODO("Migration/njawad: PorterDuff.Mode.HARD_LIGHT is not supported")
            BlendMode.hue ->
                TODO("Migration/njawad: PorterDuff.Mode.HUE is not supported")
            BlendMode.luminosity ->
                TODO("Migration/njawad: PorterDuff.Mode.LUMINOSITY is not supported")
            BlendMode.modulate ->
                TODO("Migration/njawad: PorterDuff.Mode.MODULATE is not supported")
            BlendMode.saturation ->
                TODO("Migration/njawad: PorterDuff.Mode.SATURATION is not supported")
            BlendMode.softLight ->
                TODO("Migration/njawad: PorterDuff.Mode.SOFT_LIGHT is not supported")
        }
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
    var strokeWidth: Double
        get() = internalPaint.strokeWidth.toDouble()
        set(value) {
            internalPaint.strokeWidth = value.toFloat()
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
    var strokeMiterLimit: Double
        get() = internalPaint.strokeMiter.toDouble()
        set(value) {
            internalPaint.strokeMiter = value.toFloat()
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
            return MaskFilter(style, blurRadius / 2.0)
        }
        set(value) {
            val blur = when (value.style) {
                BlurStyle.inner -> android.graphics.BlurMaskFilter.Blur.INNER
                BlurStyle.normal -> android.graphics.BlurMaskFilter.Blur.NORMAL
                BlurStyle.outer -> android.graphics.BlurMaskFilter.Blur.OUTER
                BlurStyle.solid -> android.graphics.BlurMaskFilter.Blur.SOLID
            }

            // radius is equivalent to roughly twice the sigma: radius = sigma * 2
            internalPaint.maskFilter = BlurMaskFilter((value.sigma * 2).toFloat(), blur)
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

//    // The shader to use when stroking or filling a shape.
//    //
//    // When this is null, the [color] is used instead.
//    //
//    // See also:
//    //
//    //  * [Gradient], a shader that paints a color gradient.
//    //  * [ImageShader], a shader that tiles an [Image].
//    //  * [colorFilter], which overrides [shader].
//    //  * [color], which is used if [shader] and [colorFilter] are null.
    // TODO(Migration/njawad: port over Shader implementations + add Shader support)
//    Shader get shader {
//        if (_objects == null)
//            return null;
//        return _objects[SHADER_INDEX];
//    }
//    set shader(Shader value) {
//        _objects ??= new List<dynamic>(OBJECT_COUNT);
//        _objects[SHADER_INDEX] = value;
//    }
//
//    // A color filter to apply when a shape is drawn or when a layer is
//    // composited.
//    //
//    // See [ColorFilter] for details.
//    //
//    // When a shape is being drawn, [colorFilter] overrides [color] and [shader].
    // TODO(Migration/njawad: port over ColorFilter implementations + add ColorFilter support)

//    ColorFilter get colorFilter {
//        final bool isNull = _data.getInt32(_kColorFilterOffset, _kFakeHostEndian) == 0;
//        if (isNull)
//            return null;
//        return new ColorFilter.mode(
//                new Color(_data.getInt32(_kColorFilterColorOffset, _kFakeHostEndian)),
//        BlendMode.values[_data.getInt32(_kColorFilterBlendModeOffset, _kFakeHostEndian)]
//        );
//    }
//    set colorFilter(ColorFilter value) {
//        if (value == null) {
//            _data.setInt32(_kColorFilterOffset, 0, _kFakeHostEndian);
//            _data.setInt32(_kColorFilterColorOffset, 0, _kFakeHostEndian);
//            _data.setInt32(_kColorFilterBlendModeOffset, 0, _kFakeHostEndian);
//        } else {
//            assert(value._color != null);
//            assert(value._blendMode != null);
//            _data.setInt32(_kColorFilterOffset, 1, _kFakeHostEndian);
//            _data.setInt32(_kColorFilterColorOffset, value._color.value, _kFakeHostEndian);
//            _data.setInt32(_kColorFilterBlendModeOffset, value._blendMode.index, _kFakeHostEndian);
//        }
//    }

//    @override
//    String toString() {
//        final StringBuffer result = new StringBuffer();
//        String semicolon = '';
//        result.write('Paint(');
//        if (style == PaintingStyle.stroke) {
//            result.write('$style');
//            if (strokeWidth != 0.0)
//                result.write(' ${strokeWidth.toStringAsFixed(1)}');
//            else
//                result.write(' hairline');
//            if (strokeCap != StrokeCap.butt)
//                result.write(' $strokeCap');
//            if (strokeJoin == StrokeJoin.miter) {
//                if (strokeMiterLimit != STROKE_MITER_LIMIT_DEFAULT)
//                    result.write(' $strokeJoin up to ${strokeMiterLimit.toStringAsFixed(1)}');
//            } else {
//                result.write(' $strokeJoin');
//            }
//            semicolon = '; ';
//        }
//        if (isAntiAlias != true) {
//            result.write('${semicolon}antialias off');
//            semicolon = '; ';
//        }
//        if (color != const Color(COLOR_DEFAULT)) {
//            if (color != null)
//                result.write('$semicolon$color');
//            else
//                result.write('${semicolon}no color');
//            semicolon = '; ';
//        }
//        if (blendMode.index != BLENDMODE_DEFAULT) {
//            result.write('$semicolon$blendMode');
//            semicolon = '; ';
//        }
//        if (colorFilter != null) {
//            result.write('${semicolon}colorFilter: $colorFilter');
//            semicolon = '; ';
//        }
//        if (maskFilter != null) {
//            result.write('${semicolon}maskFilter: $maskFilter');
//            semicolon = '; ';
//        }
//        if (filterQuality != FilterQuality.none) {
//            result.write('${semicolon}filterQuality: $filterQuality');
//            semicolon = '; ';
//        }
//        if (shader != null)
//            result.write('${semicolon}shader: $shader');
//        result.write(')');
//        return result.toString();
//    }
}