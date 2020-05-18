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

actual typealias NativePaint = android.graphics.Paint
internal actual fun makeNativePaint() =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

internal actual fun NativePaint.setNativeBlendMode(value: BlendMode) {
    this.xfermode = android.graphics.PorterDuffXfermode(value.toPorterDuffMode())
}

internal actual fun NativePaint.setNativeColorFilter(value: ColorFilter?) {
    if (value != null) {
        this.colorFilter = android.graphics.PorterDuffColorFilter(
                value.color.toArgb(),
                value.blendMode.toPorterDuffMode()
        )
    } else {
        this.colorFilter = null
    }
}

internal actual fun NativePaint.getNativeAlpha() = this.alpha / 255f

internal actual fun NativePaint.setNativeAlpha(value: Float) {
    this.alpha = kotlin.math.round(value * 255.0f).toInt()
}

internal actual fun NativePaint.getNativeAntiAlias(): Boolean = this.isAntiAlias

internal actual fun NativePaint.setNativeAntiAlias(value: Boolean) {
    this.isAntiAlias = value
}

internal actual fun NativePaint.getNativeColor(): Color = Color(this.color)

internal actual fun NativePaint.setNativeColor(value: Color) {
    this.color = value.toArgb()
}

internal actual fun NativePaint.setNativeStyle(value: PaintingStyle) {
    // TODO(njawad): Platform also supports Paint.Style.FILL_AND_STROKE)
    this.style = when (value) {
        PaintingStyle.stroke -> android.graphics.Paint.Style.STROKE
        else -> android.graphics.Paint.Style.FILL
    }
}

internal actual fun NativePaint.getNativeStyle() = when (this.style) {
    android.graphics.Paint.Style.STROKE -> PaintingStyle.stroke
    else -> PaintingStyle.fill
}

internal actual fun NativePaint.getNativeStrokeWidth(): Float =
    this.strokeWidth

internal actual fun NativePaint.setNativeStrokeWidth(value: Float) {
    this.strokeWidth = value
}

internal actual fun NativePaint.getNativeStrokeCap(): StrokeCap = when (this.strokeCap) {
    android.graphics.Paint.Cap.BUTT -> StrokeCap.butt
    android.graphics.Paint.Cap.ROUND -> StrokeCap.round
    android.graphics.Paint.Cap.SQUARE -> StrokeCap.square
    else -> StrokeCap.butt
}

internal actual fun NativePaint.setNativeStrokeCap(value: StrokeCap) {
    this.strokeCap = when (value) {
        StrokeCap.square -> android.graphics.Paint.Cap.SQUARE
        StrokeCap.round -> android.graphics.Paint.Cap.ROUND
        StrokeCap.butt -> android.graphics.Paint.Cap.BUTT
    }
}

internal actual fun NativePaint.getNativeStrokeJoin(): StrokeJoin =
    when (this.strokeJoin) {
        android.graphics.Paint.Join.MITER -> StrokeJoin.miter
        android.graphics.Paint.Join.BEVEL -> StrokeJoin.bevel
        android.graphics.Paint.Join.ROUND -> StrokeJoin.round
        else -> StrokeJoin.miter
    }

internal actual fun NativePaint.setNativeStrokeJoin(value: StrokeJoin) {
    this.strokeJoin = when (value) {
        StrokeJoin.miter -> android.graphics.Paint.Join.MITER
        StrokeJoin.bevel -> android.graphics.Paint.Join.BEVEL
        StrokeJoin.round -> android.graphics.Paint.Join.ROUND
    }
}

internal actual fun NativePaint.getNativeStrokeMiterLimit(): Float =
    this.strokeMiter

internal actual fun NativePaint.setNativeStrokeMiterLimit(value: Float) {
    this.strokeMiter = value
}

internal actual fun NativePaint.getNativeFilterQuality(): FilterQuality =
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

internal actual fun NativePaint.setNativeFilterQuality(value: FilterQuality) {
    this.isFilterBitmap = value != FilterQuality.none
}

internal actual fun NativePaint.setNativeShader(value: NativeShader?) {
    this.shader = value
}

internal actual fun NativePaint.setNativePathEffect(value: NativePathEffect?) {
    this.pathEffect = value
}