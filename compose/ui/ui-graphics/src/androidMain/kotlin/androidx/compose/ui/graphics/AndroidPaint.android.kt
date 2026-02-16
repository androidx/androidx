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

package androidx.compose.ui.graphics

import android.graphics.Paint.Cap
import android.graphics.Paint.Style
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.annotation.RequiresApi

@Deprecated(
    message = "Use android.graphics.Paint directly instead",
    replaceWith = ReplaceWith("android.graphics.Paint"),
)
actual typealias NativePaint = android.graphics.Paint

actual fun Paint(): Paint = AndroidPaint()

/** Convert an [android.graphics.Paint] instance into a Compose-compatible [Paint] */
// TODO: Multiple calls will NOT return the same instance,
//  consider to replace to `fun Paint(androidPaint: android.graphics.Paint)`
fun android.graphics.Paint.asComposePaint(): Paint = AndroidPaint(this)

/** Convert a Compose [Paint] instance into an [android.graphics.Paint]. */
val Paint.nativePaint: android.graphics.Paint
    get() {
        requirePrecondition(this is AndroidPaint) {
            "Extracting native reference is only supported from androidx.compose.ui.graphics.AndroidPaint instances but received ${this::class.qualifiedName}"
        }
        return internalPaint
    }

/**
 * Create a Compose [Paint] instance backed by an [android.graphics.Paint] object to be consumed by
 * Compose applications running on the Android platform
 *
 * @param internalPaint [android.graphics.Paint] to be wrapped by the [AndroidPaint] instance
 */
// TODO: This type was never supposed to be public and used directly, consider to deprecate
// @Deprecated(
//    message = "This type is not supposed to be used directly",
//    replaceWith = ReplaceWith("androidx.compose.ui.graphics.Paint"),
// )
class AndroidPaint(internal var internalPaint: android.graphics.Paint) : Paint {

    /** Create a new [AndroidPaint] instance backed by a newly created [android.graphics.Paint] */
    constructor() : this(makeNativePaint())

    private var _blendMode = BlendMode.SrcOver
    private var internalShader: Shader? = null
    private var internalColorFilter: ColorFilter? = null

    @Deprecated(
        message = "Use [nativePaint] extension instead",
        replaceWith = ReplaceWith("nativePaint"),
    )
    override fun asFrameworkPaint(): android.graphics.Paint = internalPaint

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
            if (_blendMode != value) {
                _blendMode = value
                internalPaint.setNativeBlendMode(value)
            }
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

    override var pathEffect: PathEffect? = null
        set(value) {
            internalPaint.setNativePathEffect(value)
            field = value
        }
}

internal fun makeNativePaint() =
    android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG or
            android.graphics.Paint.DITHER_FLAG or
            android.graphics.Paint.FILTER_BITMAP_FLAG
    )

internal fun android.graphics.Paint.setNativeBlendMode(mode: BlendMode) {
    if (Build.VERSION.SDK_INT >= 29) {
        // All blend modes supported in Q
        WrapperVerificationHelperMethods.setBlendMode(this, mode)
    } else {
        // Else fall back on platform alternatives
        this.xfermode = PorterDuffXfermode(mode.toPorterDuffMode())
    }
}

internal fun android.graphics.Paint.setNativeColorFilter(value: ColorFilter?) {
    colorFilter = value?.asAndroidColorFilter()
}

internal fun android.graphics.Paint.getNativeAlpha() = this.alpha / 255f

internal fun android.graphics.Paint.setNativeAlpha(value: Float) {
    this.alpha = kotlin.math.round(value * 255.0f).toInt()
}

internal fun android.graphics.Paint.getNativeAntiAlias(): Boolean = this.isAntiAlias

internal fun android.graphics.Paint.setNativeAntiAlias(value: Boolean) {
    this.isAntiAlias = value
}

internal fun android.graphics.Paint.getNativeColor(): Color = Color(this.color)

internal fun android.graphics.Paint.setNativeColor(value: Color) {
    this.color = value.toArgb()
}

internal fun android.graphics.Paint.setNativeStyle(value: PaintingStyle) {
    // TODO(njawad): Platform also supports Paint.Style.FILL_AND_STROKE)
    this.style =
        when (value) {
            PaintingStyle.Stroke -> Style.STROKE
            else -> Style.FILL
        }
}

internal fun android.graphics.Paint.getNativeStyle() =
    when (this.style) {
        Style.STROKE -> PaintingStyle.Stroke
        else -> PaintingStyle.Fill
    }

internal fun android.graphics.Paint.getNativeStrokeWidth(): Float = this.strokeWidth

internal fun android.graphics.Paint.setNativeStrokeWidth(value: Float) {
    this.strokeWidth = value
}

internal fun android.graphics.Paint.getNativeStrokeCap(): StrokeCap =
    when (this.strokeCap) {
        Cap.BUTT -> StrokeCap.Butt
        Cap.ROUND -> StrokeCap.Round
        Cap.SQUARE -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

internal fun android.graphics.Paint.setNativeStrokeCap(value: StrokeCap) {
    this.strokeCap =
        when (value) {
            StrokeCap.Square -> Cap.SQUARE
            StrokeCap.Round -> Cap.ROUND
            StrokeCap.Butt -> Cap.BUTT
            else -> Cap.BUTT
        }
}

internal fun android.graphics.Paint.getNativeStrokeJoin(): StrokeJoin =
    when (this.strokeJoin) {
        android.graphics.Paint.Join.MITER -> StrokeJoin.Miter
        android.graphics.Paint.Join.BEVEL -> StrokeJoin.Bevel
        android.graphics.Paint.Join.ROUND -> StrokeJoin.Round
        else -> StrokeJoin.Miter
    }

internal fun android.graphics.Paint.setNativeStrokeJoin(value: StrokeJoin) {
    this.strokeJoin =
        when (value) {
            StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
            StrokeJoin.Bevel -> android.graphics.Paint.Join.BEVEL
            StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
            else -> android.graphics.Paint.Join.MITER
        }
}

internal fun android.graphics.Paint.getNativeStrokeMiterLimit(): Float = this.strokeMiter

internal fun android.graphics.Paint.setNativeStrokeMiterLimit(value: Float) {
    this.strokeMiter = value
}

internal fun android.graphics.Paint.getNativeFilterQuality(): FilterQuality =
    if (!this.isFilterBitmap) {
        FilterQuality.None
    } else {
        // TODO b/162284721 (njawad): Align with Framework APIs)
        // Framework only supports bilinear filtering which maps to FilterQuality.low
        // FilterQuality.medium and FilterQuailty.high refer to a combination of
        // bilinear interpolation, pyramidal parameteric prefiltering (mipmaps) as well as
        // bicubic interpolation respectively
        FilterQuality.Low
    }

internal fun android.graphics.Paint.setNativeFilterQuality(value: FilterQuality) {
    this.isFilterBitmap = value != FilterQuality.None
}

internal fun android.graphics.Paint.setNativeShader(value: Shader?) {
    this.shader = value
}

internal fun android.graphics.Paint.setNativePathEffect(value: PathEffect?) {
    this.pathEffect = (value as AndroidPathEffect?)?.nativePathEffect
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal object WrapperVerificationHelperMethods {
    fun setBlendMode(paint: android.graphics.Paint, mode: BlendMode) {
        paint.blendMode = mode.toAndroidBlendMode()
    }
}
