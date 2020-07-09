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

package androidx.ui.graphics

import androidx.compose.InternalComposeApi

actual typealias NativePaint = android.graphics.Paint

@Suppress("DEPRECATION_ERROR")
@OptIn(InternalComposeApi::class)
actual fun Paint(): Paint = GraphicsFactory.paint()

class AndroidPaint : Paint {

    private var internalPaint = makeNativePaint()
    private var _blendMode = BlendMode.srcOver
    private var internalShader: Shader? = null
    private var internalColorFilter: ColorFilter? = null

    override fun asFrameworkPaint(): NativePaint = internalPaint

    override var alpha: Float
        get() = internalPaint.getNativeAlpha()
        set(value) {
            internalPaint.setNativeAlpha(value)
        }

    override var isAntiAlias: Boolean
        get() = internalPaint.getNativeAntiAlias()
        set(value) {
            internalPaint.setNativeAntiAlias(value)
        }

    override var color: Color
        get() = internalPaint.getNativeColor()
        set(color) {
            internalPaint.setNativeColor(color)
        }

    override var blendMode: BlendMode
        get() = _blendMode
        set(value) {
            _blendMode = value
            internalPaint.setNativeBlendMode(value)
        }

    override var style: PaintingStyle
        get() = internalPaint.getNativeStyle()
        set(value) {
            internalPaint.setNativeStyle(value)
        }

    override var strokeWidth: Float
        get() = internalPaint.getNativeStrokeWidth()
        set(value) {
            internalPaint.setNativeStrokeWidth(value)
        }

    override var strokeCap: StrokeCap
        get() = internalPaint.getNativeStrokeCap()
        set(value) {
            internalPaint.setNativeStrokeCap(value)
        }

    override var strokeJoin: StrokeJoin
        get() = internalPaint.getNativeStrokeJoin()
        set(value) {
            internalPaint.setNativeStrokeJoin(value)
        }

    override var strokeMiterLimit: Float
        get() = internalPaint.getNativeStrokeMiterLimit()
        set(value) {
            internalPaint.setNativeStrokeMiterLimit(value)
        }

    // TODO(ianh): verify that the image drawing methods actually respect this
    override var filterQuality: FilterQuality
        get() = internalPaint.getNativeFilterQuality()
        set(value) {
            internalPaint.setNativeFilterQuality(value)
        }

    override var shader: Shader?
        get() = internalShader
        set(value) {
            internalShader = value
            internalPaint.setNativeShader(internalShader)
        }

    override var colorFilter: ColorFilter?
        get() = internalColorFilter
        set(value) {
            internalColorFilter = value
            internalPaint.setNativeColorFilter(value)
        }

    override var nativePathEffect: NativePathEffect? = null
        set(value) {
            internalPaint.setNativePathEffect(value)
            field = value
        }
}

internal fun makeNativePaint() =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

internal fun NativePaint.setNativeBlendMode(value: BlendMode) {
    this.xfermode = android.graphics.PorterDuffXfermode(value.toPorterDuffMode())
}

internal fun NativePaint.setNativeColorFilter(value: ColorFilter?) {
    if (value != null) {
        this.colorFilter = android.graphics.PorterDuffColorFilter(
                value.color.toArgb(),
                value.blendMode.toPorterDuffMode()
        )
    } else {
        this.colorFilter = null
    }
}

internal fun NativePaint.getNativeAlpha() = this.alpha / 255f

internal fun NativePaint.setNativeAlpha(value: Float) {
    this.alpha = kotlin.math.round(value * 255.0f).toInt()
}

internal fun NativePaint.getNativeAntiAlias(): Boolean = this.isAntiAlias

internal fun NativePaint.setNativeAntiAlias(value: Boolean) {
    this.isAntiAlias = value
}

internal fun NativePaint.getNativeColor(): Color = Color(this.color)

internal fun NativePaint.setNativeColor(value: Color) {
    this.color = value.toArgb()
}

internal fun NativePaint.setNativeStyle(value: PaintingStyle) {
    // TODO(njawad): Platform also supports Paint.Style.FILL_AND_STROKE)
    this.style = when (value) {
        PaintingStyle.stroke -> android.graphics.Paint.Style.STROKE
        else -> android.graphics.Paint.Style.FILL
    }
}

internal fun NativePaint.getNativeStyle() = when (this.style) {
    android.graphics.Paint.Style.STROKE -> PaintingStyle.stroke
    else -> PaintingStyle.fill
}

internal fun NativePaint.getNativeStrokeWidth(): Float =
    this.strokeWidth

internal fun NativePaint.setNativeStrokeWidth(value: Float) {
    this.strokeWidth = value
}

internal fun NativePaint.getNativeStrokeCap(): StrokeCap = when (this.strokeCap) {
    android.graphics.Paint.Cap.BUTT -> StrokeCap.butt
    android.graphics.Paint.Cap.ROUND -> StrokeCap.round
    android.graphics.Paint.Cap.SQUARE -> StrokeCap.square
    else -> StrokeCap.butt
}

internal fun NativePaint.setNativeStrokeCap(value: StrokeCap) {
    this.strokeCap = when (value) {
        StrokeCap.square -> android.graphics.Paint.Cap.SQUARE
        StrokeCap.round -> android.graphics.Paint.Cap.ROUND
        StrokeCap.butt -> android.graphics.Paint.Cap.BUTT
    }
}

internal fun NativePaint.getNativeStrokeJoin(): StrokeJoin =
    when (this.strokeJoin) {
        android.graphics.Paint.Join.MITER -> StrokeJoin.miter
        android.graphics.Paint.Join.BEVEL -> StrokeJoin.bevel
        android.graphics.Paint.Join.ROUND -> StrokeJoin.round
        else -> StrokeJoin.miter
    }

internal fun NativePaint.setNativeStrokeJoin(value: StrokeJoin) {
    this.strokeJoin = when (value) {
        StrokeJoin.miter -> android.graphics.Paint.Join.MITER
        StrokeJoin.bevel -> android.graphics.Paint.Join.BEVEL
        StrokeJoin.round -> android.graphics.Paint.Join.ROUND
    }
}

internal fun NativePaint.getNativeStrokeMiterLimit(): Float =
    this.strokeMiter

internal fun NativePaint.setNativeStrokeMiterLimit(value: Float) {
    this.strokeMiter = value
}

internal fun NativePaint.getNativeFilterQuality(): FilterQuality =
    if (!this.isFilterBitmap) {
        FilterQuality.none
    } else {
        // TODO(njawad): Align with Framework APIs)
        // Framework only supports bilinear filtering which maps to FilterQuality.low
        // FilterQuality.medium and FilterQuailty.high refer to a combination of
        // bilinear interpolation, pyramidal parameteric prefiltering (mipmaps) as well as
        // bicubic interpolation respectively
        FilterQuality.low
    }

internal fun NativePaint.setNativeFilterQuality(value: FilterQuality) {
    this.isFilterBitmap = value != FilterQuality.none
}

internal fun NativePaint.setNativeShader(value: Shader?) {
    this.shader = value
}

internal fun NativePaint.setNativePathEffect(value: NativePathEffect?) {
    this.pathEffect = value
}