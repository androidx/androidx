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

package android.graphics

import org.jetbrains.skija.FilterQuality

@Suppress("unused")
open class Paint {
    enum class Style(val skija: org.jetbrains.skija.PaintMode) {
        FILL(org.jetbrains.skija.PaintMode.FILL),
        FILL_AND_STROKE(org.jetbrains.skija.PaintMode.STROKE_AND_FILL),
        STROKE(org.jetbrains.skija.PaintMode.STROKE)
    }

    enum class Cap(val skija: org.jetbrains.skija.PaintStrokeCap) {
        BUTT(org.jetbrains.skija.PaintStrokeCap.BUTT),
        ROUND(org.jetbrains.skija.PaintStrokeCap.ROUND),
        SQUARE(org.jetbrains.skija.PaintStrokeCap.SQUARE)
    }

    enum class Join(val skija: org.jetbrains.skija.PaintStrokeJoin) {
        BEVEL(org.jetbrains.skija.PaintStrokeJoin.BEVEL),
        MITER(org.jetbrains.skija.PaintStrokeJoin.MITER),
        ROUND((org.jetbrains.skija.PaintStrokeJoin.ROUND))
    }

    open class FontMetricsInt {
        @JvmField
        var top: Int = 0
        @JvmField
        var ascent: Int = 0
        @JvmField
        var descent: Int = 0
        @JvmField
        var bottom: Int = 0
        @JvmField
        var leading: Int = 0

        override fun toString(): String {
            return "FontMetricsInt: top=" + top + " ascent=" + ascent +
                    " descent=" + descent + " bottom=" + bottom +
                    " leading=" + leading
        }
    }

    companion object {
        const val ANTI_ALIAS_FLAG = 1
        const val FILTER_BITMAP_FLAG = 2
    }

    val skijaPaint = org.jetbrains.skija.Paint()

    constructor()

    constructor(flags: Int) {
        if (flags and ANTI_ALIAS_FLAG == ANTI_ALIAS_FLAG) {
            isAntiAlias = true
        }
        if (flags and FILTER_BITMAP_FLAG == FILTER_BITMAP_FLAG) {
            isFilterBitmap = true
        }
    }

    var color: Int
        get() = skijaPaint.getColor()
        set(value) {
            skijaPaint.setColor(value)
        }
    var strokeWidth: Float
        get() = skijaPaint.getStrokeWidth()
        set(value) {
            skijaPaint.setStrokeWidth(value)
        }
    var style: Style
        get() = Style.values().first { it.skija == skijaPaint.getMode() }
        set(value) {
            skijaPaint.setMode(value.skija)
        }
    var strokeCap: Cap
        get() = Cap.values().first { it.skija == skijaPaint.getStrokeCap() }
        set(value) {
            skijaPaint.setStrokeCap(value.skija)
        }
    var strokeMiter: Float
        get() = skijaPaint.getStrokeMiter().toFloat()
        set(value) {
            skijaPaint.setStrokeMiter(value)
        }
    var strokeJoin: Join
        get() = Join.values().first { it.skija == skijaPaint.getStrokeJoin() }
        set(value) {
            skijaPaint.setStrokeJoin(value.skija)
        }
    var isAntiAlias: Boolean
        get() = skijaPaint.isAntiAlias()
        set(value) {
            skijaPaint.setAntiAlias(value)
        }
    var isFilterBitmap: Boolean
        get() = skijaPaint.filterQuality != FilterQuality.NONE
        set(value) {
            skijaPaint.filterQuality = if (value) {
                FilterQuality.LOW
            } else {
                FilterQuality.NONE
            }
        }

    var alpha: Int
        get() = (color shr 24) and 0xff
        set(value) {
            color = (value shl 24) or (color and 0x00ffffff)
        }

    var textSize: Float = 20f

    private var typeface: Typeface? = null
    fun getTypeface(): Typeface? = typeface
    fun setTypeface(typeface: Typeface?): Typeface? {
        this.typeface = typeface
        return typeface
    }

    private var xfermode: Xfermode? = null
    fun getXfermode(): Xfermode? = xfermode
    fun setXfermode(xfermode: Xfermode?): Xfermode? {
        xfermode as PorterDuffXfermode?
        this.xfermode = xfermode
        skijaPaint.blendMode = xfermode?.mode?.toSkia()
        return xfermode
    }

    private var colorFilter: ColorFilter? = null
    fun getColorFilter(): ColorFilter? = colorFilter
    fun setColorFilter(colorFilter: ColorFilter?): ColorFilter? {
        colorFilter as PorterDuffColorFilter?
        this.colorFilter = colorFilter
        if (colorFilter != null) {
            skijaPaint.colorFilter = org.jetbrains.skija.ColorFilter.makeBlend(
                colorFilter.color, colorFilter.mode.toSkia()
            )
        } else {
            skijaPaint.colorFilter = null
        }
        return colorFilter
    }

    private var shader: Shader? = null
    fun getShader(): Shader? = shader
    fun setShader(shader: Shader?): Shader? {
        this.shader = shader
        skijaPaint.shader = shader?.skija
        return shader
    }

    private var pathEffect: PathEffect? = null
    fun getPathEffect(): PathEffect? = pathEffect
    fun setPathEffect(pathEffect: PathEffect?): PathEffect? {
        this.pathEffect = pathEffect
        skijaPaint.pathEffect = pathEffect?.skija
        return pathEffect
    }

    var textLocale: java.util.Locale = java.util.Locale.getDefault()
        set(value) {
            field = value
            println("Paint.textLocale not implemented yet")
        }

    var fontFeatureSettings: String? = null
        set(value) {
            field = value
            println("Paint.fontFeatureSettings not implemented yet")
        }

    var textScaleX: Float = 1f
        set(value) {
            field = value
            println("Paint.textScaleX not implemented yet")
        }
    var textSkewX: Float = 1f
        set(value) {
            field = value
            println("Paint.textSkewX not implemented yet")
        }
    var letterSpacing: Float = 0f
        set(value) {
            field = value
            println("Paint.letterSpacing not implemented yet")
        }
    var underlineText: Boolean = false
        set(value) {
            field = value
            println("Paint.underlineText not implemented yet")
        }
    var strikeThruText: Boolean = false
        set(value) {
            field = value
            println("Paint.strikeThruText not implemented yet")
        }

    fun setShadowLayer(radius: Float, dx: Float, dy: Float, shadowColor: Int) {
        println("Paint.setShadowLayer not implemented yet")
    }
}
