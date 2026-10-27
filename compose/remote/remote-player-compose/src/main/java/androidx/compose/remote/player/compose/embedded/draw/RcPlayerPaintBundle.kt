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

package androidx.compose.remote.player.compose.embedded.draw

import android.graphics.DiscretePathEffect
import android.graphics.Matrix
import android.graphics.PathDashPathEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.SumPathEffect
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.MatrixAccess
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.ShaderData
import androidx.compose.remote.core.operations.matrix.MatrixExpression
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintPathEffects
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.doc.writeContext
import androidx.compose.remote.player.compose.embedded.resources.resolveBitmap
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.remote.player.compose.utils.getPath
import androidx.compose.remote.player.compose.utils.toStampedPathEffectStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toComposePathEffect
import androidx.compose.ui.text.font.FontStyle

/** Wraps a framework [Shader] as a Compose [Brush] for the DrawScope paint path. */
private fun nativeShaderBrush(shader: Shader): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader = shader
    }

/**
 * Builds the AGSL [RuntimeShader] for a PaintBundle `SHADER` op (from a [ShaderData], with its
 * float/int/bitmap uniforms applied), mirroring the View player's `AndroidPaintContext.setShader`.
 * Returns null — for id 0, a missing [ShaderData] or shader text, or below API 33 (RuntimeShader is
 * API 33+); the caller then falls back to the solid color. The caller wraps it as a Compose [Brush]
 * (and keeps it for SHADER_MATRIX).
 */
private fun buildRuntimeShader(
    shaderId: Int,
    remoteContext: RemoteContext,
): Shader? {
    if (shaderId == 0) return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val data = remoteContext.mRemoteComposeState.getFromId(shaderId) as? ShaderData ?: return null
    val text = remoteContext.getText(data.shaderTextId) ?: return null
    val state = remoteContext.mRemoteComposeState as? SnapshotRemoteComposeState
    if (state != null && !state.trustedContent) return null
    // A shader that fails to compile or bind its uniforms (e.g. malformed AGSL, or a runtime that
    // doesn't fully support RuntimeShader such as a host without GPU shader compilation) must not
    // crash the whole document draw — fall back to no shader so the rest of the frame still
    // renders.
    return try {
        val shader = RuntimeShader(text)
        for (name in data.uniformFloatNames) {
            val values = data.getUniformFloats(name)
            // A single NaN-encoded value may reference a dynamic float list (e.g. a vec4 uniform);
            // otherwise resolve each NaN-encoded variable to its current value.
            val dynamicList =
                values
                    .singleOrNull()
                    ?.let { MaybeVariableFloat(it) }
                    ?.takeIf { it.isReference }
                    ?.let { remoteContext.collectionsAccess?.getDynamicFloats(it.id) }
            shader.setFloatUniform(
                name,
                dynamicList
                    ?: FloatArray(values.size) { k ->
                        MaybeVariableFloat(values[k]).resolveOr(values[k], remoteContext)
                    },
            )
        }
        for (name in data.uniformIntegerNames) {
            shader.setIntUniform(name, data.getUniformInts(name))
        }
        for (name in data.uniformBitmapNames) {
            val bitmap = resolveBitmap(remoteContext, data.getUniformBitmapId(name))
            if (bitmap != null) {
                shader.setInputShader(
                    name,
                    ImageShader(bitmap.asImageBitmap(), TileMode.Clamp, TileMode.Clamp),
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
    val id = MaybeVariableFloat(Float.fromBits(matrixWord)).id
    if (id == 0) {
        shader.setLocalMatrix(null)
        return
    }
    val matrix = read.getObject(id) as? MatrixAccess ?: return
    if (matrix is MatrixExpression) {
        matrix.updateVariables(read)
        matrix.apply(read)
    }
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

private fun resolvePaintFloat(bits: Int, read: RemoteContext): Float {
    val value = Float.fromBits(bits)
    return resolveFloat(value, value, read)
}

/**
 * Applies [bundle] to [paintState]. Variable references are resolved through [remoteContext]; pass
 * the player's [GraphContext] so computed values are current and reads are observed. Writes that
 * must persist (e.g. lazy bitmap decode) go through its [writeContext].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun updatePaintFromBundle(
    bundle: PaintBundle,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
) {
    paintState.sourceBundles.add(bundle)
    val array = bundle.getArray()
    var i = 0
    while (i < bundle.getPos()) {
        val cmd = array[i++]
        when (cmd and 0xFFFF) {
            PaintBundle.TEXT_SIZE -> {
                paintState.textSize = resolvePaintFloat(array[i++], remoteContext)
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
                // Reactive remoteContext: an animated/variable color re-runs the draw when it
                // changes.
                paintState.color = remoteContext.getColor(colorId)
                paintState.isColorSet = true
            }
            PaintBundle.STROKE_WIDTH -> {
                paintState.strokeWidth = resolvePaintFloat(array[i++], remoteContext)
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
                if (count > 0) {
                    val tags = IntArray(count)
                    val values = FloatArray(count)
                    for (j in 0 until count) {
                        tags[j] = array[i++]
                        values[j] = resolvePaintFloat(array[i++], remoteContext)
                    }
                    paintState.fontAxis = tags
                    paintState.fontAxisValues = values
                } else {
                    paintState.fontAxis = null
                    paintState.fontAxisValues = null
                }
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
                val color = remoteContext.getColor(colorId)
                paintState.colorFilter = ColorFilter.tint(Color(color), mapBlendMode(mode))
            }
            PaintBundle.CLEAR_COLOR_FILTER -> {
                paintState.colorFilter = null
            }
            PaintBundle.SHADER -> {
                // AGSL RuntimeShader on the paint, wrapped as a Compose Brush (mirrors the View
                // player's AndroidPaintContext.setShader). Null (id 0 / missing data / pre-API-33)
                // clears it. Keep the native shader so SHADER_MATRIX can set a local matrix.
                val shaderId = derefId(array[i++], remoteContext)
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
                val shader = bitmap?.let {
                    ImageShader(
                        it.asImageBitmap(),
                        mapTileMode(tileModes and 0xF),
                        mapTileMode((tileModes shr 16) and 0xF),
                    )
                }
                paintState.nativeShader = shader
                paintState.brush = shader?.let { nativeShaderBrush(it) }
            }
            PaintBundle.ALPHA -> {
                // 1 float word (see PaintBundle.resolveIds). Folded into the draw color via
                // ComposeLocalPaint.effectiveColor().
                paintState.alpha = resolvePaintFloat(array[i++], remoteContext).coerceIn(0f, 1f)
            }
            PaintBundle.ANTI_ALIAS -> {
                // Value is packed in the high bits of `cmd`; no extra words.
            }
            PaintBundle.FILTER_BITMAP -> {
                paintState.filterQuality =
                    if ((cmd shr 16) != 0) FilterQuality.Low else FilterQuality.None
            }
            PaintBundle.IMAGE_FILTER_QUALITY -> {
                paintState.filterQuality =
                    when (cmd shr 16) {
                        0 -> FilterQuality.None
                        1 -> FilterQuality.Low
                        2 -> FilterQuality.Medium
                        3 -> FilterQuality.High
                        else -> FilterQuality.Low
                    }
            }
            PaintBundle.SHADER_MATRIX -> {
                // Local matrix on the current shader (1 word: NaN-encoded MatrixAccess id).
                applyShaderMatrix(paintState, array[i++], remoteContext)
            }
            PaintBundle.STROKE_MITER -> {
                paintState.strokeMiter = resolvePaintFloat(array[i++], remoteContext)
            }
            PaintBundle.FALLBACK_TYPEFACE -> {
                i++ // 1 word (PaintBundle.resolveIds); not applied yet, consumed to stay in sync.
            }
            PaintBundle.PATH_EFFECT -> {
                val pathEffectCount = cmd shr 16
                if (pathEffectCount > 0) {
                    val pathEffectData = FloatArray(pathEffectCount)
                    for (j in 0 until pathEffectCount) {
                        pathEffectData[j] = resolvePaintFloat(array[i++], remoteContext)
                    }
                    val parsed = runCatching {
                        PaintPathEffects.parse(pathEffectData, 0)
                    }
                        .getOrNull()
                    paintState.pathEffect = buildComposePathEffect(parsed, remoteContext)
                } else {
                    paintState.pathEffect = null
                }
            }
            PaintBundle.GRADIENT -> {
                val gradientType = (cmd shr 16)
                val meta = array[i++]
                var len = meta and 0xFF // colors count
                val register = (meta shr 16) and 0xFFFF // bitmask: which stops are colour-id refs
                val colors = IntArray(len)
                for (j in 0 until len) {
                    val word = array[i++]
                    colors[j] =
                        if ((register and (1 shl j)) != 0) remoteContext.getColor(word) else word
                }
                len = array[i++] // stops count
                val stops = FloatArray(len)
                for (j in 0 until len) {
                    stops[j] = resolvePaintFloat(array[i++], remoteContext)
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
                val colorStopsList: List<Float>? =
                    if (stopsAscending) {
                        stops.asList()
                    } else {
                        null
                    }

                when (gradientType) {
                    0 -> { // LINEAR_GRADIENT
                        val startX = resolvePaintFloat(array[i++], remoteContext)
                        val startY = resolvePaintFloat(array[i++], remoteContext)
                        val endX = resolvePaintFloat(array[i++], remoteContext)
                        val endY = resolvePaintFloat(array[i++], remoteContext)
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
                            val shader =
                                LinearGradientShader(
                                    from = start,
                                    to = end,
                                    colors = colorsList,
                                    colorStops = colorStopsList,
                                    tileMode = tm,
                                )
                            paintState.nativeShader = shader
                            paintState.brush = nativeShaderBrush(shader)
                        } else if (colorsList.isNotEmpty()) {
                            paintState.nativeShader = null
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                    1 -> { // RADIAL_GRADIENT
                        val centerX = resolvePaintFloat(array[i++], remoteContext)
                        val centerY = resolvePaintFloat(array[i++], remoteContext)
                        val radius = resolvePaintFloat(array[i++], remoteContext)
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
                            val shader =
                                RadialGradientShader(
                                    center = center,
                                    radius = radius,
                                    colors = colorsList,
                                    colorStops = colorStopsList,
                                    tileMode = tm,
                                )
                            paintState.nativeShader = shader
                            paintState.brush = nativeShaderBrush(shader)
                        } else if (colorsList.isNotEmpty()) {
                            paintState.nativeShader = null
                            paintState.brush = SolidColor(colorsList[0])
                        }
                    }
                    2 -> { // SWEEP_GRADIENT
                        val centerX = resolvePaintFloat(array[i++], remoteContext)
                        val centerY = resolvePaintFloat(array[i++], remoteContext)
                        val center = Offset(centerX, centerY)
                        if (colorsList.size >= 2 && centerX.isFinite() && centerY.isFinite()) {
                            val shader =
                                SweepGradientShader(
                                    center = center,
                                    colors = colorsList,
                                    colorStops = colorStopsList,
                                )
                            paintState.nativeShader = shader
                            paintState.brush = nativeShaderBrush(shader)
                        } else if (colorsList.isNotEmpty()) {
                            paintState.nativeShader = null
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

private fun buildComposePathEffect(
    pe: PaintPathEffects?,
    remoteContext: RemoteContext,
): PathEffect? {
    if (pe == null) return null
    return when (pe) {
        is PaintPathEffects.Dash -> {
            PathEffect.dashPathEffect(pe.mIntervals, pe.mPhase)
        }
        is PaintPathEffects.Discrete -> {
            DiscretePathEffect(pe.mSegmentLength, pe.mDeviation).toComposePathEffect()
        }
        is PaintPathEffects.PathDash -> {
            val shape = remoteContext.mRemoteComposeState.getPath(pe.mShapeId, 0f, 1f)
            val styles = PathDashPathEffect.Style.values()
            val style = styles.getOrElse(pe.mStyle) { PathDashPathEffect.Style.TRANSLATE }
            PathEffect.stampedPathEffect(
                shape = shape,
                advance = pe.mAdvance,
                phase = pe.mPhase,
                style = style.toStampedPathEffectStyle(),
            )
        }
        is PaintPathEffects.Sum -> {
            val first = buildComposePathEffect(pe.mFirst, remoteContext)?.asAndroidPathEffect()
            val second = buildComposePathEffect(pe.mSecond, remoteContext)?.asAndroidPathEffect()
            if (first != null && second != null) {
                SumPathEffect(first, second).toComposePathEffect()
            } else {
                null
            }
        }
        is PaintPathEffects.Compose -> {
            val outer = buildComposePathEffect(pe.mOuterPE, remoteContext)
            val inner = buildComposePathEffect(pe.mInnerPE, remoteContext)
            if (outer != null && inner != null) {
                PathEffect.chainPathEffect(outer, inner)
            } else {
                null
            }
        }
        else -> null
    }
}
