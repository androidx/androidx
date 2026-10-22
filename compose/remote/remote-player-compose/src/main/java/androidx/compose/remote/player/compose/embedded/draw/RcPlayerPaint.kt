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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.draw

import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.embedded.resources.buildFontFeatureSettings
import androidx.compose.remote.player.compose.embedded.resources.documentFontTypeface
import androidx.compose.remote.player.compose.embedded.resources.rcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.resources.resolveFontFamily
import androidx.compose.remote.player.compose.embedded.resources.resolveTypeface
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density

/*
 * Paint state + enum mappers for the embedded player's canvas draw path. PaintBundle decoding and
 * shader/path-effect construction live in RcPlayerPaintBundle.kt.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeLocalPaint {
    // Paint defaults to opaque black; isColorSet remains false until an explicit color is
    // configured via PaintBundle.
    public var color: Int = 0xFF000000.toInt()
    public var isColorSet: Boolean = false
    public var strokeWidth: Float = 1f
    public var isStrokeWidthSet: Boolean = false
    public var strokeMiter: Float = Stroke.DefaultMiter
    public var isStroke: Boolean = false
    public var isStyleSet: Boolean = false
    public var strokeCap: Int = 0
    public var isStrokeCapSet: Boolean = false
    public var strokeJoin: Int = 0
    public var isStrokeJoinSet: Boolean = false
    public var pathEffect: PathEffect? = null
    public var textSize: Float = Float.NaN
    public var isTextSizeSet: Boolean = false
    public var fontFamily: Int = 0
    public var isTypefaceSet: Boolean = false
    public var fontWeight: Int = 400
    public var fontStyle: FontStyle = FontStyle.Normal
    internal var fontAxis: IntArray? = null
    internal var fontAxisValues: FloatArray? = null
    public var brush: Brush? = null
    // The framework shader backing [brush] (SHADER/TEXTURE), kept so SHADER_MATRIX can set a local
    // matrix on it.
    public var nativeShader: Shader? = null
    public var colorFilter: ColorFilter? = null
    public var blendMode: BlendMode = BlendMode.SrcOver
    public var isBlendModeSet: Boolean = false
    public var filterQuality: FilterQuality = FilterQuality.Low

    /**
     * Paint alpha in [0,1] from the PaintBundle ALPHA op; multiplies the draw color's own alpha.
     */
    public var alpha: Float = 1f

    /**
     * The [PaintBundle]s applied to this paint state, in application order. Replayed into a core
     * [PaintContext] when a draw op is bridged to the View player implementation (see
     * RcPlayerParticles), so paint set outside that subtree still applies.
     */
    public val sourceBundles: MutableList<PaintBundle> = mutableListOf()

    /** The fill color with the paint's [alpha] folded into its alpha channel. */
    public fun effectiveColor(): Color = Color(color).let { it.copy(alpha = it.alpha * alpha) }

    internal fun toDrawStyle(): DrawStyle =
        if (isStroke) {
            Stroke(
                width = strokeWidth,
                miter = strokeMiter,
                cap = mapStrokeCap(strokeCap),
                join = mapStrokeJoin(strokeJoin),
                pathEffect = pathEffect,
            )
        } else {
            Fill
        }

    /**
     * Build a framework [Paint] for the canvas text draw ops (DRAW_TEXT and its on-path/anchored
     * variants) from the current paint state: anti-aliased, the effective color, the text size, and
     * a bold/italic [Typeface] derived from font weight/style.
     */
    public fun toNativeTextPaint(context: RemoteContext): Paint {
        val italic = fontStyle == FontStyle.Italic

        val effectiveFontFamily = if (isTypefaceSet) fontFamily else PaintBundle.FONT_TYPE_DEFAULT
        val name =
            if (effectiveFontFamily > PaintBundle.FONT_TYPE_MONOSPACE) {
                context.getText(effectiveFontFamily)
            } else {
                null
            }
        val resolvedTypeface =
            if (effectiveFontFamily > PaintBundle.FONT_TYPE_MONOSPACE && name == null) {
                // A font embedded in the document (FontData) has no text entry and is
                // stored in the object table as RemoteContext.FontInfo.
                documentFontTypeface(context, effectiveFontFamily, fontWeight, italic)
            } else {
                null
            }
                ?: resolveTypeface(
                    fontFamilyType = effectiveFontFamily,
                    fontName = name,
                    weight = fontWeight,
                    italic = italic,
                    fontAxis = fontAxis,
                    fontAxisValues = fontAxisValues,
                    context = context,
                )

        return Paint().apply {
            isAntiAlias = true
            color = effectiveColor().toArgb()
            if (!this@ComposeLocalPaint.textSize.isNaN()) {
                textSize = this@ComposeLocalPaint.textSize
            }
            typeface = resolvedTypeface
            buildFontFeatureSettings(fontAxis, fontAxisValues, context)?.let {
                fontFeatureSettings = it
            }
        }
    }

    internal fun toTextStyle(density: Density, context: RemoteContext): TextStyle {
        val weight = FontWeight(fontWeight)
        val resolver = context.rcTypefaceResolver
        val effectiveFontFamilyType = if (isTypefaceSet) fontFamily else 0
        val fontName =
            when (effectiveFontFamilyType) {
                0 -> "default"
                1 -> "sans-serif"
                2 -> "serif"
                3 -> "monospace"
                else -> context.getText(effectiveFontFamilyType)
            }
        val resolvedFontFamily =
            resolveFontFamily(
                fontFamilyType = effectiveFontFamilyType,
                fontName = fontName,
                fontWeight = weight,
                fontStyle = fontStyle,
                fontAxis = fontAxis,
                fontAxisValues = fontAxisValues,
                context = context,
                typefaceResolver = resolver,
            )
        val fontFeatureSettings = buildFontFeatureSettings(fontAxis, fontAxisValues, context)
        val drawStyle = toDrawStyle()
        val resolvedPx = if (textSize.isNaN()) 12f else textSize
        val fontSizeSp = with(density) { resolvedPx.toSp() }
        return if (brush != null) {
            TextStyle(
                brush = brush,
                alpha = alpha,
                fontSize = fontSizeSp,
                fontWeight = weight,
                fontStyle = fontStyle,
                fontFamily = resolvedFontFamily,
                fontFeatureSettings = fontFeatureSettings,
                drawStyle = drawStyle,
            )
        } else {
            TextStyle(
                color = effectiveColor(),
                fontSize = fontSizeSp,
                fontWeight = weight,
                fontStyle = fontStyle,
                fontFamily = resolvedFontFamily,
                fontFeatureSettings = fontFeatureSettings,
                drawStyle = drawStyle,
            )
        }
    }

    public fun reset() {
        color = 0xFF000000.toInt()
        isColorSet = false
        strokeWidth = 1f
        isStrokeWidthSet = false
        strokeMiter = Stroke.DefaultMiter
        isStroke = false
        isStyleSet = false
        strokeCap = 0
        isStrokeCapSet = false
        strokeJoin = 0
        isStrokeJoinSet = false
        pathEffect = null
        textSize = Float.NaN
        isTextSizeSet = false
        fontFamily = 0
        isTypefaceSet = false
        fontWeight = 400
        fontStyle = FontStyle.Normal
        fontAxis = null
        fontAxisValues = null
        brush = null
        nativeShader = null
        colorFilter = null
        blendMode = BlendMode.SrcOver
        isBlendModeSet = false
        filterQuality = FilterQuality.Low
        alpha = 1f
        sourceBundles.clear()
    }

    internal fun copy(): ComposeLocalPaint =
        ComposeLocalPaint().also { copy ->
            copy.color = color
            copy.isColorSet = isColorSet
            copy.strokeWidth = strokeWidth
            copy.isStrokeWidthSet = isStrokeWidthSet
            copy.strokeMiter = strokeMiter
            copy.isStroke = isStroke
            copy.isStyleSet = isStyleSet
            copy.strokeCap = strokeCap
            copy.isStrokeCapSet = isStrokeCapSet
            copy.strokeJoin = strokeJoin
            copy.isStrokeJoinSet = isStrokeJoinSet
            copy.pathEffect = pathEffect
            copy.textSize = textSize
            copy.isTextSizeSet = isTextSizeSet
            copy.fontFamily = fontFamily
            copy.isTypefaceSet = isTypefaceSet
            copy.fontWeight = fontWeight
            copy.fontStyle = fontStyle
            copy.fontAxis = fontAxis?.copyOf()
            copy.fontAxisValues = fontAxisValues?.copyOf()
            copy.brush = brush
            copy.nativeShader = nativeShader
            copy.colorFilter = colorFilter
            copy.blendMode = blendMode
            copy.isBlendModeSet = isBlendModeSet
            copy.filterQuality = filterQuality
            copy.alpha = alpha
            copy.sourceBundles.addAll(sourceBundles)
        }
}

internal fun mapStrokeCap(cap: Int): StrokeCap =
    when (cap) {
        1 -> StrokeCap.Round
        2 -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

internal fun mapStrokeJoin(join: Int): StrokeJoin =
    when (join) {
        1 -> StrokeJoin.Round
        2 -> StrokeJoin.Bevel
        else -> StrokeJoin.Miter
    }

internal fun mapTileMode(mode: Int): TileMode =
    when (mode) {
        1 -> TileMode.Repeated
        2 -> TileMode.Mirror
        else -> TileMode.Clamp
    }

internal fun mapBlendMode(mode: Int): BlendMode =
    when (mode) {
        0 -> BlendMode.Clear
        1 -> BlendMode.Src
        2 -> BlendMode.Dst
        3 -> BlendMode.SrcOver
        4 -> BlendMode.DstOver
        5 -> BlendMode.SrcIn
        6 -> BlendMode.DstIn
        7 -> BlendMode.SrcOut
        8 -> BlendMode.DstOut
        9 -> BlendMode.SrcAtop
        10 -> BlendMode.DstAtop
        11 -> BlendMode.Xor
        12 -> BlendMode.Plus
        13 -> BlendMode.Modulate
        14 -> BlendMode.Screen
        15 -> BlendMode.Overlay
        16 -> BlendMode.Darken
        17 -> BlendMode.Lighten
        18 -> BlendMode.ColorDodge
        19 -> BlendMode.ColorBurn
        20 -> BlendMode.Hardlight
        21 -> BlendMode.Softlight
        22 -> BlendMode.Difference
        23 -> BlendMode.Exclusion
        24 -> BlendMode.Multiply
        25 -> BlendMode.Hue
        26 -> BlendMode.Saturation
        27 -> BlendMode.Color
        28 -> BlendMode.Luminosity
        else -> BlendMode.SrcOver
    }
