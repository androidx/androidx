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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle

/*
 * Paint state + PaintBundle decoding for the embedded player's canvas draw path. Splits the paint
 * concerns (ComposeLocalPaint, stroke/blend/tile mappers, shader-brush builders, updatePaintFromBundle)
 * out of RcPlayerDrawing. Shares the snapshot store via the passed RemoteContext and resolveBitmap.
 */

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeLocalPaint {
    // android.graphics.Paint defaults to opaque black; isColorSet remains false until an explicit
    // color is configured via PaintBundle.
    public var color: Int = 0xFF000000.toInt()
    public var isColorSet: Boolean = false
    public var strokeWidth: Float = 1f
    public var isStrokeWidthSet: Boolean = false
    public var isStroke: Boolean = false
    public var isStyleSet: Boolean = false
    public var strokeCap: Int = 0
    public var isStrokeCapSet: Boolean = false
    public var strokeJoin: Int = 0
    public var isStrokeJoinSet: Boolean = false
    public var textSize: Float = Float.NaN
    public var isTextSizeSet: Boolean = false
    public var fontFamily: Int = 0
    public var isTypefaceSet: Boolean = false
    public var fontWeight: Int = 400
    public var fontStyle: FontStyle = FontStyle.Normal
    public var brush: Brush? = null
    // The framework shader backing [brush] (SHADER/TEXTURE), kept so SHADER_MATRIX can set a local
    // matrix on it.
    public var nativeShader: Shader? = null
    public var colorFilter: ColorFilter? = null
    public var blendMode: BlendMode = BlendMode.SrcOver
    public var isBlendModeSet: Boolean = false

    /**
     * Paint alpha in [0,1] from the PaintBundle ALPHA op; multiplies the draw color's own alpha.
     */
    public var alpha: Float = 1f

    /**
     * The [PaintBundle]s applied to this paint state, in application order. Replayed into a core
     * [androidx.compose.remote.core.PaintContext] when a draw op is bridged to the View player
     * implementation (see RcPlayerParticles), so paint set outside that subtree still applies.
     */
    public val sourceBundles: MutableList<PaintBundle> = mutableListOf()

    /** The fill color with the paint's [alpha] folded into its alpha channel. */
    public fun effectiveColor(): Color = Color(color).let { it.copy(alpha = it.alpha * alpha) }

    /**
     * Build a framework [android.graphics.Paint] for the canvas text draw ops (DRAW_TEXT and its
     * on-path/anchored variants) from the current paint state: anti-aliased, the effective color,
     * the text size, and a bold/italic [android.graphics.Typeface] derived from font weight/style.
     */
    public fun toNativeTextPaint(context: RemoteContext): Paint {
        // Reuse the context's configured TypefaceResolver (e.g. GmsFontTypefaceResolver) if
        // present, falling back to the embedded player's singleton system resolver.
        val resolver =
            (context as? AndroidRemoteContext)?.typefaceResolver ?: EmbeddedPlayerTypefaceResolver
        val italic = fontStyle == FontStyle.Italic

        val fontInstance =
            if (isTypefaceSet) {
                if (fontFamily in 0..3) {
                    resolver.resolve(fontFamily, fontWeight, italic, null, 400, false)
                } else {
                    val name = context.getText(fontFamily)
                    if (name != null) {
                        resolver.resolve(name, fontWeight, italic, null, 400, false)
                    } else {
                        resolver.resolve(0, fontWeight, italic, null, 400, false)
                    }
                }
            } else {
                resolver.resolve(0, fontWeight, italic, null, 400, false)
            }

        return Paint().apply {
            isAntiAlias = true
            color = effectiveColor().toArgb()
            textSize = this@ComposeLocalPaint.textSize
            typeface = fontInstance.getTypeface()
        }
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

/** Maps a packed tile-mode index to a framework [Shader.TileMode]. */
private fun nativeTileMode(index: Int): Shader.TileMode =
    when (index) {
        1 -> Shader.TileMode.REPEAT
        2 -> Shader.TileMode.MIRROR
        else -> Shader.TileMode.CLAMP
    }

/** Wraps a framework [Shader] as a Compose [Brush] for the DrawScope paint path. */
private fun nativeShaderBrush(shader: Shader): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader = shader
    }

/**
 * Builds the AGSL [android.graphics.RuntimeShader] for a PaintBundle `SHADER` op (from a
 * [ShaderData], with its float/int/bitmap uniforms applied), mirroring the View player's
 * `AndroidPaintContext.setShader`. Returns null — for id 0, a missing [ShaderData] or shader text,
 * or below API 33 (RuntimeShader is API 33+); the caller then falls back to the solid color. The
 * caller wraps it as a Compose [Brush] (and keeps it for SHADER_MATRIX).
 */
private fun buildRuntimeShader(shaderId: Int, remoteContext: RemoteContext): Shader? {
    if (shaderId == 0) return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val data = remoteContext.mRemoteComposeState.getFromId(shaderId) as? ShaderData ?: return null
    val text = remoteContext.getText(data.shaderTextId) ?: return null
    // A shader that fails to compile or bind its uniforms (e.g. malformed AGSL, or a runtime that
    // doesn't fully support RuntimeShader such as a host without GPU shader compilation) must not
    // crash the whole document draw — fall back to no shader so the rest of the frame still
    // renders.
    return try {
        val shader = RuntimeShader(text)
        for (name in data.uniformFloatNames) {
            shader.setFloatUniform(name, data.getUniformFloats(name))
        }
        for (name in data.uniformIntegerNames) {
            shader.setIntUniform(name, data.getUniformInts(name))
        }
        for (name in data.uniformBitmapNames) {
            val bitmap = resolveBitmap(remoteContext, data.getUniformBitmapId(name))
            if (bitmap != null) {
                shader.setInputShader(
                    name,
                    BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP),
                )
            }
        }
        shader
    } catch (e: RuntimeException) {
        null
    }
}

/**
 * Applies a PaintBundle `SHADER_MATRIX` op: sets a local matrix on the current shader. [matrixWord]
 * is the NaN-encoded id (as raw bits) of a [MatrixAccess] object; id 0 clears the local matrix.
 * Mirrors the View player's `AndroidPaintContext.setShaderMatrix`.
 */
private fun applyShaderMatrix(paintState: ComposeLocalPaint, matrixWord: Int, read: RemoteContext) {
    val shader = paintState.nativeShader ?: return
    val id = Utils.idFromNan(Float.fromBits(matrixWord))
    if (id == 0) {
        shader.setLocalMatrix(null)
        return
    }
    val matrix = read.getObject(id) as? MatrixAccess ?: return
    val values = matrix.get()
    // MatrixAccess.to3x3: a 4x4 (16) collapses to the 3x3 (9) android Matrix layout; a 9 is as-is.
    val m3x3 =
        when (values.size) {
            9 -> values
            16 ->
                floatArrayOf(
                    values[0],
                    values[1],
                    values[3],
                    values[4],
                    values[5],
                    values[7],
                    values[8],
                    values[9],
                    values[15],
                )
            else -> return
        }
    shader.setLocalMatrix(Matrix().apply { setValues(m3x3) })
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

private fun resolvePaintFloat(bits: Int, read: RemoteContext): Float {
    val value = Float.fromBits(bits)
    return resolveFloat(value, value, read)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun updatePaintFromBundle(
    bundle: PaintBundle,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext = remoteContext,
) {
    paintState.sourceBundles.add(bundle)
    val array = bundle.getArrayReflection()
    var i = 0
    while (i < bundle.getPosReflection()) {
        val cmd = array[i++]
        when (cmd and 0xFFFF) {
            PaintBundle.TEXT_SIZE -> {
                paintState.textSize = Float.fromBits(array[i++])
                paintState.isTextSizeSet = true
            }
            PaintBundle.TYPEFACE -> {
                val style = (cmd shr 16)
                val weight = style and 0x3ff
                val italic = (style shr 10) > 0
                paintState.fontFamily = array[i++]
                paintState.fontWeight = weight
                paintState.fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
                paintState.isTypefaceSet = true
            }
            PaintBundle.COLOR -> {
                paintState.color = array[i++]
                paintState.isColorSet = true
            }
            PaintBundle.COLOR_ID -> {
                val colorId = array[i++]
                // Reactive read: an animated/variable color re-runs the draw when it changes.
                paintState.color = read.getColor(colorId)
                paintState.isColorSet = true
            }
            PaintBundle.STROKE_WIDTH -> {
                paintState.strokeWidth = Float.fromBits(array[i++])
                paintState.isStrokeWidthSet = true
            }
            PaintBundle.STYLE -> {
                paintState.isStroke = (cmd shr 16) == PaintBundle.STYLE_STROKE
                paintState.isStyleSet = true
            }
            PaintBundle.STROKE_CAP -> {
                paintState.strokeCap = (cmd shr 16)
                paintState.isStrokeCapSet = true
            }
            PaintBundle.STROKE_JOIN -> {
                paintState.strokeJoin = (cmd shr 16)
                paintState.isStrokeJoinSet = true
            }
            PaintBundle.FONT_AXIS -> {
                val count = cmd shr 16
                i += 2 * count
            }
            PaintBundle.BLEND_MODE -> {
                val mode = (cmd shr 16)
                paintState.blendMode = mapBlendMode(mode)
                paintState.isBlendModeSet = true
            }
            PaintBundle.COLOR_FILTER -> {
                val mode = (cmd shr 16)
                val color = array[i++]
                paintState.colorFilter = ColorFilter.tint(Color(color), mapBlendMode(mode))
            }
            PaintBundle.COLOR_FILTER_ID -> {
                val mode = (cmd shr 16)
                val colorId = array[i++]
                val color = read.getColor(colorId)
                paintState.colorFilter = ColorFilter.tint(Color(color), mapBlendMode(mode))
            }
            PaintBundle.CLEAR_COLOR_FILTER -> {
                paintState.colorFilter = null
            }
            PaintBundle.SHADER -> {
                // AGSL RuntimeShader on the paint, wrapped as a Compose Brush (mirrors the View
                // player's AndroidPaintContext.setShader). Null (id 0 / missing data / pre-API-33)
                // clears it. Keep the native shader so SHADER_MATRIX can set a local matrix.
                val shaderId = array[i++]
                val shader = buildRuntimeShader(shaderId, remoteContext)
                paintState.nativeShader = shader
                paintState.brush = shader?.let { nativeShaderBrush(it) }
            }
            PaintBundle.TEXTURE -> {
                // Bitmap texture shader. Layout (PaintBundle): bitmapId, tileModes (tileX=&0xF,
                // tileY=>>16), filter (unused here). Wrapped as a Compose Brush; mirrors
                // AndroidPaintContext.setTextureShader.
                val bitmapId = array[i++]
                val tileModes = array[i++]
                i++ // filter/maxAnisotropy word (filtering managed by Compose; consumed to stay
                // synced)
                val bitmap = resolveBitmap(remoteContext, bitmapId)
                val shader =
                    bitmap?.let {
                        BitmapShader(
                            it,
                            nativeTileMode(tileModes and 0xF),
                            nativeTileMode((tileModes shr 16) and 0xF),
                        )
                    }
                paintState.nativeShader = shader
                paintState.brush = shader?.let { nativeShaderBrush(it) }
            }
            PaintBundle.ALPHA -> {
                // 1 float word (see PaintBundle.resolveIds). Folded into the draw color via
                // ComposeLocalPaint.effectiveColor().
                paintState.alpha = Float.fromBits(array[i++]).coerceIn(0f, 1f)
            }
            PaintBundle.ANTI_ALIAS,
            PaintBundle.IMAGE_FILTER_QUALITY,
            PaintBundle.FILTER_BITMAP -> {
                // Value is packed in the high bits of `cmd`; no extra words. Compose's DrawScope is
                // anti-aliased and manages filtering itself, so these are consumed and ignored.
            }
            PaintBundle.SHADER_MATRIX -> {
                // Local matrix on the current shader (1 word: NaN-encoded MatrixAccess id).
                applyShaderMatrix(paintState, array[i++], read)
            }
            PaintBundle.STROKE_MITER,
            PaintBundle.FALLBACK_TYPEFACE -> {
                i++ // 1 word each (PaintBundle.resolveIds); not applied yet, consumed to stay in
                // sync.
            }
            PaintBundle.PATH_EFFECT -> {
                i += (cmd shr 16) // `count` float words (PaintBundle.resolveIds); not applied yet.
            }
            PaintBundle.GRADIENT -> {
                val gradientType = (cmd shr 16)
                val meta = array[i++]
                var len = meta and 0xFF // colors count
                val register = (meta shr 16) and 0xFFFF // bitmask: which stops are colour-id refs
                val colors = IntArray(len)
                for (j in 0 until len) {
                    val word = array[i++]
                    colors[j] = if ((register and (1 shl j)) != 0) read.getColor(word) else word
                }
                len = array[i++] // stops count
                val stops = FloatArray(len)
                for (j in 0 until len) {
                    stops[j] = Float.fromBits(array[i++])
                }

                val colorsList = colors.map { Color(it) }
                // Use explicit color stops only when well-formed: one per color, ascending, within
                // [0,1]. Compose's colorStops overloads throw otherwise, so fall back to even
                // spacing.
                var stopsAscending = stops.size == colorsList.size && colorsList.isNotEmpty()
                if (stopsAscending) {
                    for (k in 0 until stops.size) {
                        val s = stops[k]
                        if (s !in 0f..1f || (k > 0 && stops[k - 1] > s)) {
                            stopsAscending = false
                            break
                        }
                    }
                }
                val colorStops: Array<Pair<Float, Color>>? =
                    if (stopsAscending) {
                        Array(colorsList.size) { stops[it] to colorsList[it] }
                    } else {
                        null
                    }

                when (gradientType) {
                    0 -> { // LINEAR_GRADIENT
                        val startX = resolvePaintFloat(array[i++], read)
                        val startY = resolvePaintFloat(array[i++], read)
                        val endX = resolvePaintFloat(array[i++], read)
                        val endY = resolvePaintFloat(array[i++], read)
                        val tileMode = array[i++]
                        val start = Offset(startX, startY)
                        val end = Offset(endX, endY)
                        val tm = mapTileMode(tileMode)
                        if (
                            colorsList.size >= 2 &&
                                startX.isFinite() &&
                                startY.isFinite() &&
                                endX.isFinite() &&
                                endY.isFinite() &&
                                (startX != endX || startY != endY)
                        ) {
                            paintState.brush =
                                if (colorStops != null)
                                    Brush.linearGradient(
                                        colorStops = colorStops,
                                        start = start,
                                        end = end,
                                        tileMode = tm,
                                    )
                                else
                                    Brush.linearGradient(
                                        colors = colorsList,
                                        start = start,
                                        end = end,
                                        tileMode = tm,
                                    )
                        } else if (colorsList.isNotEmpty()) {
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                    1 -> { // RADIAL_GRADIENT
                        val centerX = resolvePaintFloat(array[i++], read)
                        val centerY = resolvePaintFloat(array[i++], read)
                        val radius = resolvePaintFloat(array[i++], read)
                        val tileMode = array[i++]
                        val center = Offset(centerX, centerY)
                        val tm = mapTileMode(tileMode)
                        if (
                            colorsList.size >= 2 &&
                                centerX.isFinite() &&
                                centerY.isFinite() &&
                                radius.isFinite() &&
                                radius > 0
                        ) {
                            paintState.brush =
                                if (colorStops != null)
                                    Brush.radialGradient(
                                        colorStops = colorStops,
                                        center = center,
                                        radius = radius,
                                        tileMode = tm,
                                    )
                                else
                                    Brush.radialGradient(
                                        colors = colorsList,
                                        center = center,
                                        radius = radius,
                                        tileMode = tm,
                                    )
                        } else if (colorsList.isNotEmpty()) {
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                    2 -> { // SWEEP_GRADIENT
                        val centerX = resolvePaintFloat(array[i++], read)
                        val centerY = resolvePaintFloat(array[i++], read)
                        val center = Offset(centerX, centerY)
                        if (colorsList.size >= 2 && centerX.isFinite() && centerY.isFinite()) {
                            paintState.brush =
                                if (colorStops != null)
                                    Brush.sweepGradient(colorStops = colorStops, center = center)
                                else Brush.sweepGradient(colors = colorsList, center = center)
                        } else if (colorsList.isNotEmpty()) {
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                }
            }
            else -> {
                // Unknown/variable-width sub-op whose word count we can't determine, so consuming a
                // guessed number would desync the rest of the bundle. Stop processing the remaining
                // sub-ops rather than crash; what was parsed so far still applies.
                println(
                    "Warning: unsupported PaintBundle sub-op ${cmd and 0xFFFF}; " +
                        "skipping remainder of bundle"
                )
                return
            }
        }
    }
}
