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

package androidx.compose.ui.graphics.blur

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RuntimeShaderBlurRenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import kotlin.math.max
import kotlin.math.min

// Progressive-linear (axis-aligned fast path) passes. The gradient line is a start/end-point
// uniform pair (data), so only the blur pass axis is a source-level variant: two programs.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object LinearBlurPasses {
    val horizontal = RuntimeShader(BlurShaders.progressiveLinearBlurSkSl(isVerticalBlur = false))
    val vertical = RuntimeShader(BlurShaders.progressiveLinearBlurSkSl(isVerticalBlur = true))
}

// Shape-independent masked-blur passes shared by all masked paths.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object MaskedBlurPasses {
    val horizontal = RuntimeShader(BlurShaders.maskedBlurSkSl(isVertical = false))
    val vertical = RuntimeShader(BlurShaders.maskedBlurSkSl(isVertical = true))
}

// Stop-count-independent intensity mask programs using fixed-size uniform arrays.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object LinearMaskProgram {
    val shader = RuntimeShader(BlurShaders.multiLinearMaskSkSl())
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object RadialMaskProgram {
    val shader = RuntimeShader(BlurShaders.multiRadialMaskSkSl())
}

internal class VerticalBlurRenderEffect(
    private val spec: BlurVerticalGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? =
        with(density) {
            fastLinearEffect(
                spec.start.toPx(),
                spec.end.toPx(),
                size,
                verticalGradient = true,
                isDecal,
            )
        }
}

internal class HorizontalBlurRenderEffect(
    private val spec: BlurHorizontalGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? =
        with(density) {
            fastLinearEffect(
                spec.start.toPx(),
                spec.end.toPx(),
                size,
                verticalGradient = false,
                isDecal,
            )
        }
}

// Arbitrary (possibly diagonal) lines always go through the mask path, which projects each
// pixel onto the start->end segment.
internal class LinearBlurRenderEffect(
    private val spec: BlurLinearGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? {
        val intensities = FloatArray(BlurStop.MaxStops)
        val positions = FloatArray(BlurStop.MaxStops)
        val maxRadiusPx =
            with(density) {
                fillTwoPoint(spec.startRadius.toPx(), spec.endRadius.toPx(), intensities, positions)
            }
        if (maxRadiusPx <= 0f) return null
        return linearMaskEffect(
            spec.start.toOffset(density),
            spec.end.toOffset(density),
            maxRadiusPx,
            size,
            isDecal,
            intensities,
            positions,
        )
    }
}

internal class VerticalStopsBlurRenderEffect(
    private val spec: BlurVerticalStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? =
        stopsMaskEffect(spec.stops, Offset(0f, 0f), Offset(0f, size.height), size, density, isDecal)
}

internal class HorizontalStopsBlurRenderEffect(
    private val spec: BlurHorizontalStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? =
        stopsMaskEffect(spec.stops, Offset(0f, 0f), Offset(size.width, 0f), size, density, isDecal)
}

internal class LinearStopsBlurRenderEffect(
    private val spec: BlurLinearStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? =
        stopsMaskEffect(
            spec.stops,
            spec.start.toOffset(density),
            spec.end.toOffset(density),
            size,
            density,
            isDecal,
        )
}

internal class RadialBlurRenderEffect(
    private val spec: BlurRadialGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? {
        val intensities = FloatArray(BlurStop.MaxStops)
        val positions = FloatArray(BlurStop.MaxStops)
        val maxRadiusPx =
            with(density) {
                fillTwoPoint(spec.startRadius.toPx(), spec.endRadius.toPx(), intensities, positions)
            }
        if (maxRadiusPx <= 0f) return null
        return radialMaskEffect(
            spec.resolveBlurCenter(size, density),
            spec.resolveFallOffRadius(size, density),
            maxRadiusPx,
            size,
            isDecal,
            intensities,
            positions,
        )
    }
}

internal class RadialStopsBlurRenderEffect(
    private val spec: BlurRadialStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? {
        val intensities = FloatArray(BlurStop.MaxStops)
        val positions = FloatArray(BlurStop.MaxStops)
        val maxRadiusPx = fillStops(spec.stops, density, intensities, positions)
        if (maxRadiusPx <= 0f) return null
        return radialMaskEffect(
            spec.resolveBlurCenter(size, density),
            spec.resolveFallOffRadius(size, density),
            maxRadiusPx,
            size,
            isDecal,
            intensities,
            positions,
        )
    }
}

internal class ShaderBlurRenderEffect(
    private val spec: BlurRadiusShader,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
) : RuntimeShaderBlurRenderEffect(spec, size, density, edgeTreatment) {

    // The wrapped shader carries caller-mutable uniform state, so structural comparison is
    // meaningless; see the equality contract on ProgressiveBlurRenderEffect.
    override val hasStructuralEquality: Boolean
        get() = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun buildRuntimeShaderEffect(): RenderEffect? {
        val maxRadiusPx = with(density) { spec.maxRadius.toPx() }
        if (maxRadiusPx <= 0f) return null
        // The mask's sampled alpha is a 0..1 intensity that the pass scales by maxRadius. Any
        // platform Shader works as the mask input, not just RuntimeShader.
        return maskedEffect(maxRadiusPx, size, spec.shaderBlock(density, size), isDecal)
    }
}

/** Resolves a specified [DpOffset] to layer pixels. */
private fun DpOffset.toOffset(density: Density): Offset =
    with(density) { Offset(x.toPx(), y.toPx()) }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun stopsMaskEffect(
    stops: List<BlurStop>,
    start: Offset,
    end: Offset,
    size: Size,
    density: Density,
    isDecal: Boolean,
): RenderEffect? {
    val intensities = FloatArray(BlurStop.MaxStops)
    val positions = FloatArray(BlurStop.MaxStops)
    val maxRadiusPx = fillStops(stops, density, intensities, positions)
    if (maxRadiusPx <= 0f) return null
    return linearMaskEffect(start, end, maxRadiusPx, size, isDecal, intensities, positions)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun fastLinearEffect(
    startRadiusPx: Float,
    endRadiusPx: Float,
    size: Size,
    verticalGradient: Boolean,
    isDecal: Boolean,
): RenderEffect? {
    val maxRadiusPx = max(startRadiusPx, endRadiusPx)
    if (maxRadiusPx <= 0f) return null
    // The gradient line as a segment in layer pixels: the axis selection that used to be baked
    // into per-axis shader source is now just this data.
    val start = Offset(0f, 0f)
    val end = if (verticalGradient) Offset(0f, size.height) else Offset(size.width, 0f)
    val hPass = LinearBlurPasses.horizontal
    val vPass = LinearBlurPasses.vertical
    setLinearPass(hPass, maxRadiusPx, startRadiusPx, endRadiusPx, size, isDecal, start, end)
    setLinearPass(vPass, maxRadiusPx, startRadiusPx, endRadiusPx, size, isDecal, start, end)
    return chain(hPass, vPass)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setLinearPass(
    s: RuntimeShader,
    maxRadiusPx: Float,
    startRadiusPx: Float,
    endRadiusPx: Float,
    size: Size,
    isDecal: Boolean,
    start: Offset,
    end: Offset,
) {
    s.setBaseBlurUniforms(maxRadiusPx, size, isDecal)
    s.setFloatUniform(BlurShaders.UniformStartIntensity, startRadiusPx / maxRadiusPx)
    s.setFloatUniform(BlurShaders.UniformEndIntensity, endRadiusPx / maxRadiusPx)
    s.setFloatUniform(BlurShaders.UniformStartPoint, start.x, start.y)
    s.setFloatUniform(BlurShaders.UniformEndPoint, end.x, end.y)
    s.setFloatUniform(BlurShaders.UniformUnbounded, if (isDecal) 1f else 0f)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun linearMaskEffect(
    start: Offset,
    end: Offset,
    maxRadiusPx: Float,
    size: Size,
    isDecal: Boolean,
    intensities: FloatArray,
    positions: FloatArray,
): RenderEffect {
    val m = LinearMaskProgram.shader
    m.setFloatUniform(BlurShaders.UniformStartPoint, start.x, start.y)
    m.setFloatUniform(BlurShaders.UniformEndPoint, end.x, end.y)
    m.setFloatUniform(BlurShaders.UniformIntensities, intensities)
    m.setFloatUniform(BlurShaders.UniformPositions, positions)
    return maskedEffect(maxRadiusPx, size, m, isDecal)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun radialMaskEffect(
    center: Offset,
    gradientRadius: Float,
    maxRadiusPx: Float,
    size: Size,
    isDecal: Boolean,
    intensities: FloatArray,
    positions: FloatArray,
): RenderEffect {
    val m = RadialMaskProgram.shader
    m.setFloatUniform(BlurShaders.UniformCenter, center.x, center.y)
    m.setFloatUniform(BlurShaders.UniformRadius, gradientRadius)
    m.setFloatUniform(BlurShaders.UniformIntensities, intensities)
    m.setFloatUniform(BlurShaders.UniformPositions, positions)
    return maskedEffect(maxRadiusPx, size, m, isDecal)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun maskedEffect(
    blurRadius: Float,
    size: Size,
    maskShader: Shader,
    isDecal: Boolean,
): RenderEffect {
    val hPass = MaskedBlurPasses.horizontal
    val vPass = MaskedBlurPasses.vertical
    setMaskedPass(hPass, blurRadius, size, maskShader, isDecal)
    setMaskedPass(vPass, blurRadius, size, maskShader, isDecal)
    return chain(hPass, vPass)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun setMaskedPass(
    s: RuntimeShader,
    blurRadius: Float,
    size: Size,
    maskShader: Shader,
    isDecal: Boolean,
) {
    s.setBaseBlurUniforms(blurRadius, size, isDecal)
    s.setInputShader(BlurShaders.UniformMask, maskShader)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setBaseBlurUniforms(blurRadius: Float, size: Size, isDecal: Boolean) {
    // Clamp here, before the radius reaches the shader, so the shader's maxRadius loop bound
    // is never the thing enforcing the cap. See [BlurShaders.MaxBlurRadiusPx].
    setFloatUniform(BlurShaders.UniformBlurRadius, min(blurRadius, BlurShaders.MaxBlurRadiusPx))
    setFloatUniform(BlurShaders.UniformCrop, 0f, 0f, size.width, size.height)
    setFloatUniform(BlurShaders.UniformUnbounded, if (isDecal) 1f else 0f)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun chain(horizontal: RuntimeShader, vertical: RuntimeShader): RenderEffect {
    val hEffect = RenderEffect.createRuntimeShaderEffect(horizontal, BlurShaders.UniformContent)
    val vEffect = RenderEffect.createRuntimeShaderEffect(vertical, BlurShaders.UniformContent)
    return RenderEffect.createChainEffect(vEffect, hEffect)
}

/**
 * Fills staging arrays for a two-point gradient (fractions 0 and 1) and normalizes by the maximum
 * radius.
 *
 * @return maximum radius in pixels, or 0 if no blur
 */
private fun fillTwoPoint(
    startRadiusPx: Float,
    endRadiusPx: Float,
    intensities: FloatArray,
    positions: FloatArray,
): Float =
    fill(
        2,
        { if (it == 0) 0f else 1f },
        { if (it == 0) startRadiusPx else endRadiusPx },
        intensities,
        positions,
    )

/**
 * Fills staging arrays from [stops] and normalizes by the maximum radius.
 *
 * @return maximum radius in pixels, or 0 if no blur
 */
private fun fillStops(
    stops: List<BlurStop>,
    density: Density,
    intensities: FloatArray,
    positions: FloatArray,
): Float =
    with(density) {
        fill(
            stops.size,
            { stops[it].fraction },
            { stops[it].radius.toPx() },
            intensities,
            positions,
        )
    }

/**
 * Fills staging arrays from [count] stops, normalizes intensities, and pads the tail.
 *
 * Populates arrays directly without sorting or allocation.
 *
 * @return maximum radius in pixels, or 0 if no blur
 */
private inline fun fill(
    count: Int,
    position: (Int) -> Float,
    radiusPx: (Int) -> Float,
    intensities: FloatArray,
    positions: FloatArray,
): Float {
    var maxPx = 0f
    for (i in 0 until count) {
        val px = radiusPx(i)
        intensities[i] = px
        positions[i] = position(i)
        if (px > maxPx) maxPx = px
    }
    if (maxPx <= 0f) return 0f
    for (i in 0 until count) intensities[i] /= maxPx
    // Pad the tail with the last stop at position 1.0 to match a per-count shader's output.
    for (i in count until BlurStop.MaxStops) {
        positions[i] = 1f
        intensities[i] = intensities[count - 1]
    }
    return maxPx
}
