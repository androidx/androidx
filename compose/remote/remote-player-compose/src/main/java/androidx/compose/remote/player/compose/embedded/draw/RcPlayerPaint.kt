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
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintChanges
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.resources.buildFontFeatureSettings
import androidx.compose.remote.player.compose.embedded.resources.buildFontVariationSettingsString
import androidx.compose.remote.player.compose.embedded.resources.documentFontTypeface
import androidx.compose.remote.player.compose.embedded.resources.rcTypefaceResolver
import androidx.compose.remote.player.compose.embedded.resources.resolveBitmap
import androidx.compose.remote.player.compose.embedded.resources.resolveFontFamily
import androidx.compose.remote.player.compose.embedded.resources.resolveTypeface
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
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

/**
 * Fixed-size descriptor of the currently active [PaintBundle] shader so [ComposeLocalPaint] can
 * seed a core [PaintContext] in `O(1)` via [PaintChanges] without accumulating [PaintBundle] lists.
 */
internal sealed interface CoreShaderSpec {
    class LinearGradient(
        val colors: IntArray,
        val stops: FloatArray?,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val tileMode: Int,
    ) : CoreShaderSpec

    class RadialGradient(
        val colors: IntArray,
        val stops: FloatArray?,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val tileMode: Int,
    ) : CoreShaderSpec

    class SweepGradient(
        val colors: IntArray,
        val stops: FloatArray?,
        val centerX: Float,
        val centerY: Float,
    ) : CoreShaderSpec

    class Texture(
        val bitmapId: Int,
        val tileX: Short,
        val tileY: Short,
        val filterMode: Short,
        val maxAnisotropy: Short,
    ) : CoreShaderSpec

    class Runtime(val shaderId: Int) : CoreShaderSpec
}

private class CorePaintSeedBundle(private val paint: ComposeLocalPaint) : PaintBundle() {
    var activeContext: RemoteContext? = null

    override fun applyPaintChange(paintContext: PaintContext, p: PaintChanges) {
        val context = activeContext ?: return
        paint.applyToPaintChanges(p, context)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeLocalPaint {
    private val coreSeedBundle = CorePaintSeedBundle(this)

    // Paint defaults to opaque black; isColorSet remains false until an explicit color is
    // configured via PaintBundle.
    public var color: Int = 0xFF000000.toInt()
    public var isColorSet: Boolean = false
    public var strokeWidth: Float = 1f
    public var isStrokeWidthSet: Boolean = false
    public var strokeMiter: Float = Stroke.DefaultMiter
    public var isStroke: Boolean = false
    public var isStyleSet: Boolean = false
    internal var styleMode: Int = PaintBundle.STYLE_FILL
    public var strokeCap: Int = 0
    public var isStrokeCapSet: Boolean = false
    public var strokeJoin: Int = 0
    public var isStrokeJoinSet: Boolean = false
    public var pathEffect: PathEffect? = null
    internal var rawPathEffect: FloatArray? = null
    public var textSize: Float = Float.NaN
    public var isTextSizeSet: Boolean = false
    public var fontFamily: Int = 0
    public var isTypefaceSet: Boolean = false
    public var fontWeight: Int = 400
    public var fontStyle: FontStyle = FontStyle.Normal
    internal var fontAxis: IntArray? = null
    internal var fontAxisValues: FloatArray? = null
    public var brush: Brush? = null
    // The framework shader backing [brush] (SHADER/TEXTURE/GRADIENT), kept so SHADER_MATRIX can set
    // a local matrix on it.
    public var nativeShader: Shader? = null
    internal var coreShaderSpec: CoreShaderSpec? = null
    internal var shaderMatrixBits: Int = 0
    internal var hasShaderMatrix: Boolean = false
    public var colorFilter: ColorFilter? = null
    internal var colorFilterColor: Int = 0
    internal var colorFilterMode: Int = 0
    internal var hasColorFilter: Boolean = false
    public var blendMode: BlendMode = BlendMode.SrcOver
    internal var blendModeId: Int = PaintBundle.BLEND_MODE_SRC_OVER
    public var isBlendModeSet: Boolean = false
    public var filterQuality: FilterQuality = FilterQuality.Low
    internal var isFilterQualitySet: Boolean = false
    internal var isAntiAlias: Boolean = false
    internal var isAntiAliasSet: Boolean = false

    /**
     * Paint alpha in [0,1] from the PaintBundle ALPHA op; multiplies the draw color's own alpha.
     */
    public var alpha: Float = 1f

    /**
     * Compact `O(1)` text-measurement paint snapshot so `TextMeasure` and `TextAttribute` never
     * copy or replay accumulated [PaintBundle] lists.
     */
    internal var textMeasureSpec: TextMeasurePaintSpec = TextMeasurePaintSpec.Empty

    /** The fill color with the paint's [alpha] folded into its alpha channel. */
    public fun effectiveColor(): Color = Color(color).let { it.copy(alpha = it.alpha * alpha) }

    /**
     * Combined paint alpha in `[0, 1]` folding the paint color's alpha channel with [alpha],
     * matching framework `Paint` behavior when drawing shaders/brushes and bitmaps.
     */
    internal fun effectiveAlpha(): Float = (Color(color).alpha * alpha).coerceIn(0f, 1f)

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

    private fun applyTypographyTo(paint: Paint, context: RemoteContext) {
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
                documentFontTypeface(
                    context,
                    effectiveFontFamily,
                    fontWeight,
                    italic,
                    buildFontVariationSettingsString(fontAxis, fontAxisValues, context),
                )
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
        paint.typeface = resolvedTypeface
        buildFontFeatureSettings(fontAxis, fontAxisValues, context)?.let {
            paint.fontFeatureSettings = it
        }
    }

    /**
     * Build a framework [Paint] for the canvas text draw ops (DRAW_TEXT and its on-path/anchored
     * variants) from the current paint state: anti-aliased, the effective color, the text size, and
     * a bold/italic [android.graphics.Typeface] derived from font weight/style.
     */
    public fun toNativeTextPaint(context: RemoteContext): Paint =
        Paint().apply {
            isAntiAlias = true
            color = effectiveColor().toArgb()
            if (!this@ComposeLocalPaint.textSize.isNaN()) {
                textSize = this@ComposeLocalPaint.textSize
            }
            applyTypographyTo(this, context)
        }

    /**
     * Seeds a freshly reset core [PaintContext] directly from this resolved paint state in `O(1)`
     * time without accumulating or replaying [PaintBundle] histories.
     */
    internal fun seedCorePaint(paintContext: PaintContext, context: RemoteContext) {
        coreSeedBundle.activeContext = context
        try {
            paintContext.applyPaint(coreSeedBundle)
        } finally {
            coreSeedBundle.activeContext = null
        }
    }

    internal fun applyToPaintChanges(p: PaintChanges, context: RemoteContext) {
        if (isAntiAliasSet) {
            p.setAntiAlias(isAntiAlias)
        }
        val solidBrush = brush as? SolidColor
        if (solidBrush != null) {
            p.setColor(
                solidBrush.value.let { it.copy(alpha = it.alpha * effectiveAlpha()) }.toArgb()
            )
        } else {
            p.setColor(effectiveColor().toArgb())
            val shaderApplied =
                when (val spec = coreShaderSpec) {
                    is CoreShaderSpec.LinearGradient -> {
                        p.setLinearGradient(
                            spec.colors,
                            spec.stops,
                            spec.startX,
                            spec.startY,
                            spec.endX,
                            spec.endY,
                            spec.tileMode.takeIf { it in 0..2 } ?: 0,
                        )
                        true
                    }
                    is CoreShaderSpec.RadialGradient -> {
                        p.setRadialGradient(
                            spec.colors,
                            spec.stops,
                            spec.centerX,
                            spec.centerY,
                            spec.radius,
                            spec.tileMode.takeIf { it in 0..2 } ?: 0,
                        )
                        true
                    }
                    is CoreShaderSpec.SweepGradient -> {
                        p.setSweepGradient(spec.colors, spec.stops, spec.centerX, spec.centerY)
                        true
                    }
                    is CoreShaderSpec.Texture -> {
                        val hasBitmap = resolveBitmap(context, spec.bitmapId) != null
                        if (hasBitmap) {
                            p.setTextureShader(
                                spec.bitmapId,
                                spec.tileX,
                                spec.tileY,
                                spec.filterMode,
                                spec.maxAnisotropy,
                            )
                        }
                        hasBitmap
                    }
                    is CoreShaderSpec.Runtime -> {
                        val state = context.mRemoteComposeState as? SnapshotRemoteComposeState
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                (state == null || state.trustedContent) &&
                                nativeShader != null
                        ) {
                            p.setShader(spec.shaderId)
                            true
                        } else {
                            false
                        }
                    }
                    null -> false
                }
            if (shaderApplied && hasShaderMatrix) {
                val matrixFloat = Float.fromBits(shaderMatrixBits)
                val id = MaybeVariableFloat(matrixFloat).id
                if (id != 0) {
                    val matrix = context.getObject(id) as? MatrixAccess
                    if (matrix != null) {
                        if (matrix is MatrixExpression) {
                            matrix.updateVariables(context)
                            matrix.apply(context)
                        }
                        p.setShaderMatrix(matrixFloat)
                    }
                }
            }
        }
        if (isStyleSet) {
            p.setStyle(styleMode.takeIf { it in 0..2 } ?: PaintBundle.STYLE_FILL)
        }
        if (isStrokeWidthSet) {
            p.setStrokeWidth(strokeWidth)
        }
        p.setStrokeMiter(strokeMiter)
        if (isStrokeCapSet) {
            p.setStrokeCap(strokeCap.takeIf { it in 0..2 } ?: 0)
        }
        if (isStrokeJoinSet) {
            p.setStrokeJoin(strokeJoin.takeIf { it in 0..2 } ?: 0)
        }
        rawPathEffect?.let { p.setPathEffect(it) }
        if (hasColorFilter) {
            p.setColorFilter(colorFilterColor, colorFilterMode)
        }
        if (isBlendModeSet) {
            p.setBlendMode(blendModeId)
        }
        if (isFilterQualitySet) {
            p.setFilterBitmap(filterQuality != FilterQuality.None)
        }
        if (!textSize.isNaN()) {
            p.setTextSize(textSize)
        }
        if (isTypefaceSet || fontAxis != null) {
            val italic = fontStyle == FontStyle.Italic
            val effectiveFontFamily =
                if (isTypefaceSet) fontFamily else PaintBundle.FONT_TYPE_DEFAULT
            val fontName =
                if (effectiveFontFamily > PaintBundle.FONT_TYPE_MONOSPACE) {
                    context.getText(effectiveFontFamily)
                } else {
                    null
                }
            if (fontName != null) {
                p.setTypeFace(fontName, fontWeight, italic)
            } else {
                p.setTypeFace(effectiveFontFamily, fontWeight, italic)
            }
            val axes = fontAxis
            val axisValues = fontAxisValues
            if (axes != null && axisValues != null) {
                val tagNames = Array(axes.size) { idx -> context.getText(axes[idx]) ?: "" }
                p.setFontVariationAxes(tagNames, axisValues)
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
                alpha = effectiveAlpha(),
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
        styleMode = PaintBundle.STYLE_FILL
        strokeCap = 0
        isStrokeCapSet = false
        strokeJoin = 0
        isStrokeJoinSet = false
        pathEffect = null
        rawPathEffect = null
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
        coreShaderSpec = null
        shaderMatrixBits = 0
        hasShaderMatrix = false
        colorFilter = null
        colorFilterColor = 0
        colorFilterMode = 0
        hasColorFilter = false
        blendMode = BlendMode.SrcOver
        blendModeId = PaintBundle.BLEND_MODE_SRC_OVER
        isBlendModeSet = false
        filterQuality = FilterQuality.Low
        isFilterQualitySet = false
        isAntiAlias = false
        isAntiAliasSet = false
        alpha = 1f
        textMeasureSpec = TextMeasurePaintSpec.Empty
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
            copy.styleMode = styleMode
            copy.strokeCap = strokeCap
            copy.isStrokeCapSet = isStrokeCapSet
            copy.strokeJoin = strokeJoin
            copy.isStrokeJoinSet = isStrokeJoinSet
            copy.pathEffect = pathEffect
            copy.rawPathEffect = rawPathEffect?.copyOf()
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
            copy.coreShaderSpec = coreShaderSpec
            copy.shaderMatrixBits = shaderMatrixBits
            copy.hasShaderMatrix = hasShaderMatrix
            copy.colorFilter = colorFilter
            copy.colorFilterColor = colorFilterColor
            copy.colorFilterMode = colorFilterMode
            copy.hasColorFilter = hasColorFilter
            copy.blendMode = blendMode
            copy.blendModeId = blendModeId
            copy.isBlendModeSet = isBlendModeSet
            copy.filterQuality = filterQuality
            copy.isFilterQualitySet = isFilterQualitySet
            copy.isAntiAlias = isAntiAlias
            copy.isAntiAliasSet = isAntiAliasSet
            copy.alpha = alpha
            copy.textMeasureSpec = textMeasureSpec
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
        3 -> TileMode.Decal
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
