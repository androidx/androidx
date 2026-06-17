/*
 * Copyright 2020 The Android Open Source Project
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

import org.jetbrains.skia.Paint as SkPaint
import org.jetbrains.skia.PaintMode as SkPaintMode
import org.jetbrains.skia.PaintStrokeCap as SkPaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin as SkPaintStrokeJoin

@Deprecated(
    message = "Use org.jetbrains.skia.Paint directly instead",
    replaceWith = ReplaceWith("org.jetbrains.skia.Paint"),
    level = DeprecationLevel.ERROR,
)
actual typealias NativePaint = SkPaint

actual fun Paint(): Paint = SkiaBackedPaint()

/**
 * Convert the [org.jetbrains.skia.Paint] instance into a Compose-compatible Paint
 */
// TODO: Multiple calls will NOT return the same instance,
//  consider to replace to `fun Paint(skiaPaint: org.jetbrains.skia.Paint)`
fun SkPaint.asComposePaint(): Paint = SkiaBackedPaint(this)

/**
 * Provides access to the underlying [org.jetbrains.skia.Paint] instance.
 *
 * It throws an exception if accessed on unsupported types.
 */
val Paint.skiaPaint: SkPaint
    get() {
        requirePrecondition(this is SkiaBackedPaint) {
            "Extracting skia paint reference is only supported from androidx.compose.ui.graphics.SkiaBackedPaint instances but received ${this::class}"
        }
        return internalSkiaPaint
    }

internal class SkiaBackedPaint(
    val internalSkiaPaint: SkPaint = SkPaint()
) : Paint {
    @Deprecated(
        message = "Use [Paint.nativePaint] extension instead",
        replaceWith = ReplaceWith("skiaPaint", "androidx.compose.ui.graphics.skiaPaint"),
        level = DeprecationLevel.ERROR,
    )
    override fun asFrameworkPaint(): SkPaint = internalSkiaPaint

    private var mAlphaMultiplier = 1.0f

    var alphaMultiplier: Float
        get() = mAlphaMultiplier
        set(value) {
            val multiplier = value.coerceIn(0f, 1f)
            updateAlpha(multiplier = multiplier)
            mAlphaMultiplier = multiplier
        }

    private fun updateAlpha(alpha: Float = this.alpha, multiplier: Float = this.mAlphaMultiplier) {
        internalSkiaPaint.color = Color(internalSkiaPaint.color).copy(alpha = alpha * multiplier).toArgb()
    }

    override var alpha: Float
        get() = Color(internalSkiaPaint.color).alpha
        set(value) {
            updateAlpha(alpha = value)
        }

    override var isAntiAlias: Boolean
        get() = internalSkiaPaint.isAntiAlias
        set(value) {
            internalSkiaPaint.isAntiAlias = value
        }

    override var color: Color
        get() = Color(internalSkiaPaint.color)
        set(color) {
            internalSkiaPaint.color = color.toArgb()
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            internalSkiaPaint.blendMode = value.toSkia()
            field = value
        }

    override var style: PaintingStyle = PaintingStyle.Fill
        set(value) {
            internalSkiaPaint.mode = value.toSkia()
            field = value
        }

    override var strokeWidth: Float
        get() = internalSkiaPaint.strokeWidth
        set(value) {
            internalSkiaPaint.strokeWidth = value
        }

    override var strokeCap: StrokeCap = StrokeCap.Butt
        set(value) {
            internalSkiaPaint.strokeCap = value.toSkia()
            field = value
        }

    override var strokeJoin: StrokeJoin = StrokeJoin.Round
        set(value) {
            internalSkiaPaint.strokeJoin = value.toSkia()
            field = value
        }

    override var strokeMiterLimit: Float = 0f
        set(value) {
            internalSkiaPaint.strokeMiter = value
            field = value
        }

    override var filterQuality: FilterQuality = FilterQuality.Medium

    override var shader: Shader? = null
        set(value) {
            internalSkiaPaint.shader = value?.skiaShader
            field = value
        }

    override var colorFilter: ColorFilter? = null
        set(value) {
            internalSkiaPaint.colorFilter = value?.asSkiaColorFilter()
            field = value
        }

    override var pathEffect: PathEffect? = null
        set(value) {
            internalSkiaPaint.pathEffect = (value as SkiaBackedPathEffect?)?.asSkiaPathEffect()
            field = value
        }

    private fun PaintingStyle.toSkia() = when (this) {
        PaintingStyle.Fill -> SkPaintMode.FILL
        PaintingStyle.Stroke -> SkPaintMode.STROKE
        else -> SkPaintMode.FILL
    }

    private fun StrokeCap.toSkia() = when (this) {
        StrokeCap.Butt -> SkPaintStrokeCap.BUTT
        StrokeCap.Round -> SkPaintStrokeCap.ROUND
        StrokeCap.Square -> SkPaintStrokeCap.SQUARE
        else -> SkPaintStrokeCap.BUTT
    }

    private fun StrokeJoin.toSkia() = when (this) {
        StrokeJoin.Miter -> SkPaintStrokeJoin.MITER
        StrokeJoin.Round -> SkPaintStrokeJoin.ROUND
        StrokeJoin.Bevel -> SkPaintStrokeJoin.BEVEL
        else -> SkPaintStrokeJoin.MITER
    }
}

actual fun BlendMode.isSupported(): Boolean = true
