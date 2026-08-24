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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.requirePrecondition
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified

/**
 * Gradient stop for progressive blur.
 *
 * @param fraction gradient position in the 0..1 range
 * @param radius blur radius applied at [fraction]
 */
@Immutable
public class BlurStop(public val fraction: Float, public val radius: Dp) {
    init {
        require(fraction in 0f..1f) { "fraction must be in 0..1 but was $fraction" }
        require(radius.value >= 0f) { "radius must be >= 0 but was $radius" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurStop) return false

        if (fraction != other.fraction) return false
        if (radius != other.radius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fraction.hashCode()
        result = 31 * result + radius.hashCode()
        return result
    }

    override fun toString(): String = "BlurStop(fraction=$fraction, radius=$radius)"

    internal companion object {
        /** Maximum number of [BlurStop]s a multi-stop [BlurRadiusSpec] may contain. */
        const val MaxStops = 16
    }
}

/**
 * Configures varying blur radius across a surface.
 *
 * Obtain instances from companion factories and pass them to [androidx.compose.ui.draw.blur], or
 * realize them into a [RenderEffect] with [createRenderEffect].
 *
 * Uniform radii ([uniform]) are supported on Android 12 (API 31)+. Spatially-varying radii require
 * Android 13 (API 33)+. Unsupported configurations render the content unblurred.
 *
 * Every length in the configuration, including gradient geometry, is expressed in [Dp] and resolves
 * against the layer's size and density at draw time. The [shader] shader is the exception: it
 * evaluates in layer pixel coordinates and returns a unitless 0..1 intensity. Spatially-varying
 * radii cap at 150px once resolved; [uniform] radii are not capped.
 */
@Immutable
public sealed interface BlurRadiusSpec {
    /**
     * Creates a [RenderEffect] that applies this blur configuration.
     *
     * Assign the result to a graphics layer for low-level control, or prefer the block-based
     * [androidx.compose.ui.draw.blur] overload, which manages the effect automatically.
     *
     * The effect resolves this configuration against [size] and [density] at creation, so create a
     * new effect whenever the configuration or the layer size changes — for example inside a
     * [androidx.compose.ui.graphics.graphicsLayer] block where the layer size is known.
     *
     * Platform support varies with the configuration; query availability at runtime with
     * [RenderEffect.isSupported]. Unsupported platforms and zero radii render content unblurred.
     *
     * @param size size of the layer being blurred, in pixels
     * @param density density used to resolve [Dp] radii to pixels
     * @param edgeTreatment strategy used to sample pixels outside the content bounds
     * @return [RenderEffect] applying the progressive blur
     */
    public fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode = TileMode.Clamp,
    ): RenderEffect

    public companion object {
        /**
         * Creates a uniform blur [radius] across the surface.
         *
         * Supported on Android 12 (API 31) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * @param radius blur radius across the surface
         * @throws IllegalArgumentException if [radius] is negative
         */
        public fun uniform(radius: Dp): BlurRadiusSpec {
            requireNonNegative(radius, "radius")
            return BlurUniform(radius)
        }

        /**
         * Creates a vertical blur gradient from [startRadius] at top to [endRadius] at bottom.
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * @param startRadius blur radius at the top
         * @param endRadius blur radius at the bottom
         * @throws IllegalArgumentException if either radius is negative
         */
        public fun verticalGradient(startRadius: Dp, endRadius: Dp): BlurRadiusSpec {
            requireNonNegative(startRadius, "startRadius")
            requireNonNegative(endRadius, "endRadius")
            return BlurVerticalGradient(startRadius, endRadius)
        }

        /**
         * Creates a vertical multi-stop blur gradient from top to bottom.
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * Accepts [stops] in any order and sorts them stably by [BlurStop.fraction].
         *
         * @param stops list of blur stops defining the gradient
         * @throws IllegalArgumentException if the stop count is outside 2..16
         */
        public fun verticalGradient(stops: List<BlurStop>): BlurRadiusSpec =
            BlurVerticalStops(validatedStops(stops))

        /**
         * Creates a horizontal blur gradient from [startRadius] at left to [endRadius] at right.
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * @param startRadius blur radius at the left
         * @param endRadius blur radius at the right
         * @throws IllegalArgumentException if either radius is negative
         */
        public fun horizontalGradient(startRadius: Dp, endRadius: Dp): BlurRadiusSpec {
            requireNonNegative(startRadius, "startRadius")
            requireNonNegative(endRadius, "endRadius")
            return BlurHorizontalGradient(startRadius, endRadius)
        }

        /**
         * Creates a horizontal multi-stop blur gradient from left to right.
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * Accepts [stops] in any order and sorts them stably by [BlurStop.fraction].
         *
         * @param stops list of blur stops defining the gradient
         * @throws IllegalArgumentException if the stop count is outside 2..16
         */
        public fun horizontalGradient(stops: List<BlurStop>): BlurRadiusSpec =
            BlurHorizontalStops(validatedStops(stops))

        /**
         * Creates a linear blur gradient from [startRadius] at [start] to [endRadius] at [end].
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * @param start start position within the layer
         * @param end end position within the layer
         * @param startRadius blur radius at [start]
         * @param endRadius blur radius at [end]
         * @throws IllegalArgumentException if either radius is negative
         */
        public fun linearGradient(
            start: DpOffset,
            end: DpOffset,
            startRadius: Dp,
            endRadius: Dp,
        ): BlurRadiusSpec {
            requireNonNegative(startRadius, "startRadius")
            requireNonNegative(endRadius, "endRadius")
            return BlurLinearGradient(start, end, startRadius, endRadius)
        }

        /**
         * Creates a linear multi-stop blur gradient along the [start] to [end] line.
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * Accepts [stops] in any order and sorts them stably by [BlurStop.fraction].
         *
         * @param start start position within the layer
         * @param end end position within the layer
         * @param stops list of blur stops defining the gradient
         * @throws IllegalArgumentException if the stop count is outside 2..16
         */
        public fun linearGradient(
            start: DpOffset,
            end: DpOffset,
            stops: List<BlurStop>,
        ): BlurRadiusSpec = BlurLinearStops(start, end, validatedStops(stops))

        /**
         * Creates a radial blur gradient from [startRadius] at [center] to [endRadius] at
         * [fallOffRadius].
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * @param startRadius blur radius at the center of the gradient
         * @param endRadius blur radius at the falloff distance
         * @param center center of the gradient within the layer, or [DpOffset.Unspecified] for the
         *   surface center
         * @param fallOffRadius distance from [center] at which [endRadius] is reached; defaults to
         *   half the surface's minimum dimension
         * @throws IllegalArgumentException if either radius is negative, or [fallOffRadius] is
         *   finite and not positive
         */
        public fun radialGradient(
            startRadius: Dp,
            endRadius: Dp,
            center: DpOffset = DpOffset.Unspecified,
            fallOffRadius: Dp = Dp.Infinity,
        ): BlurRadiusSpec {
            requireNonNegative(startRadius, "startRadius")
            requireNonNegative(endRadius, "endRadius")
            requirePositiveFallOff(fallOffRadius)
            return BlurRadialGradient(startRadius, endRadius, center, fallOffRadius)
        }

        /**
         * Creates a radial multi-stop blur gradient from [center] to [fallOffRadius].
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * Accepts [stops] in any order and sorts them stably by [BlurStop.fraction].
         *
         * @param stops list of blur stops defining the gradient
         * @param center center of the gradient within the layer, or [DpOffset.Unspecified] for the
         *   surface center
         * @param fallOffRadius distance from [center] at which fraction 1 is reached; defaults to
         *   half the surface's minimum dimension
         * @throws IllegalArgumentException if the stop count is outside 2..16
         * @throws IllegalArgumentException if [fallOffRadius] is finite and not positive
         */
        public fun radialGradient(
            stops: List<BlurStop>,
            center: DpOffset = DpOffset.Unspecified,
            fallOffRadius: Dp = Dp.Infinity,
        ): BlurRadiusSpec {
            requirePositiveFallOff(fallOffRadius)
            return BlurRadialStops(validatedStops(stops), center, fallOffRadius)
        }

        /**
         * Creates a custom blur intensity mask driven by a [Shader].
         *
         * Supported on Android 13 (API 33) and above. Older platforms and unsupported
         * configurations render the content unblurred.
         *
         * Invokes [block] once per draw with the layer's [Size] and [Density]. Mutate the returned
         * shader's uniforms between frames to animate the blur. The shader evaluates in layer pixel
         * coordinates from the top-left origin. Its sampled alpha channel is a 0..1 intensity
         * scaling [maxRadius]; values outside that range are clamped. Any [Shader] can serve as the
         * mask, including gradients from [androidx.compose.ui.graphics.ShaderBrush].
         *
         * The [Size] passed to [block] is in pixels.
         *
         * Create the shader once and reuse it across frames. Each call returns a new configuration
         * that never compares equal, defeating downstream caching.
         *
         * @param maxRadius maximum blur radius scaled by the shader alpha
         * @param block builder producing a [Shader] given the layer size and density
         * @throws IllegalArgumentException if [maxRadius] is negative
         */
        public fun shader(maxRadius: Dp, block: Density.(sizePx: Size) -> Shader): BlurRadiusSpec {
            requireNonNegative(maxRadius, "maxRadius")
            return BlurRadiusShader(maxRadius, block)
        }

        private fun requireNonNegative(radius: Dp, name: String) {
            requirePrecondition(radius.value >= 0f) { "$name must be >= 0 but was $radius" }
        }

        private fun requirePositiveFallOff(fallOffRadius: Dp) {
            requirePrecondition(!fallOffRadius.isFinite || fallOffRadius.value > 0f) {
                "fallOffRadius must be positive when finite but was $fallOffRadius"
            }
        }

        /**
         * Validates and copies a caller-supplied stop list for retention.
         *
         * Returns an immutable, stably-sorted copy by fraction. This ensures non-decreasing
         * positions for downstream evaluation and preserves relative ordering for hard steps.
         */
        @Suppress("ListIterator")
        private fun validatedStops(stops: List<BlurStop>): List<BlurStop> {
            requirePrecondition(stops.size in 2..BlurStop.MaxStops) {
                "expected between 2 and ${BlurStop.MaxStops} stops but was ${stops.size}"
            }
            return stops.sortedBy { it.fraction }
        }
    }
}

@Immutable
internal class BlurUniform(val radius: Dp) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurUniform) return false
        return radius == other.radius
    }

    override fun hashCode(): Int = radius.hashCode()

    override fun toString(): String = "BlurUniform(radius=$radius)"
}

@Immutable
internal class BlurVerticalGradient(val start: Dp, val end: Dp) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurVerticalGradient) return false

        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

    override fun toString(): String = "BlurVerticalGradient(start=$start, end=$end)"
}

@Immutable
internal class BlurVerticalStops(val stops: List<BlurStop>) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurVerticalStops) return false
        return stops == other.stops
    }

    override fun hashCode(): Int = stops.hashCode()

    override fun toString(): String = "BlurVerticalStops(stops=$stops)"
}

@Immutable
internal class BlurHorizontalGradient(val start: Dp, val end: Dp) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurHorizontalGradient) return false

        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

    override fun toString(): String = "BlurHorizontalGradient(start=$start, end=$end)"
}

@Immutable
internal class BlurHorizontalStops(val stops: List<BlurStop>) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurHorizontalStops) return false
        return stops == other.stops
    }

    override fun hashCode(): Int = stops.hashCode()

    override fun toString(): String = "BlurHorizontalStops(stops=$stops)"
}

@Immutable
internal class BlurLinearGradient(
    val start: DpOffset,
    val end: DpOffset,
    val startRadius: Dp,
    val endRadius: Dp,
) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurLinearGradient) return false

        if (start != other.start) return false
        if (end != other.end) return false
        if (startRadius != other.startRadius) return false
        if (endRadius != other.endRadius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + startRadius.hashCode()
        result = 31 * result + endRadius.hashCode()
        return result
    }

    override fun toString(): String =
        "BlurLinearGradient(start=$start, end=$end, startRadius=$startRadius, endRadius=$endRadius)"
}

@Immutable
internal class BlurLinearStops(val start: DpOffset, val end: DpOffset, val stops: List<BlurStop>) :
    BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurLinearStops) return false

        if (start != other.start) return false
        if (end != other.end) return false
        if (stops != other.stops) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + stops.hashCode()
        return result
    }

    override fun toString(): String = "BlurLinearStops(start=$start, end=$end, stops=$stops)"
}

/** Resolves [DpOffset.Unspecified] to the surface center, in layer pixels. */
private fun resolveGradientCenter(center: DpOffset, size: Size, density: Density): Offset =
    if (center.isSpecified) {
        with(density) { Offset(center.x.toPx(), center.y.toPx()) }
    } else {
        Offset(size.width / 2f, size.height / 2f)
    }

/** Resolves a non-finite [fallOffRadius] to half the minimum dimension of [size], in pixels. */
private fun resolveGradientRadius(fallOffRadius: Dp, size: Size, density: Density): Float =
    if (fallOffRadius.isFinite) with(density) { fallOffRadius.toPx() } else size.minDimension / 2f

@Immutable
internal class BlurRadialGradient(
    val startRadius: Dp,
    val endRadius: Dp,
    val center: DpOffset,
    val fallOffRadius: Dp,
) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    fun resolveBlurCenter(size: Size, density: Density): Offset =
        resolveGradientCenter(center, size, density)

    fun resolveFallOffRadius(size: Size, density: Density): Float =
        resolveGradientRadius(fallOffRadius, size, density)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurRadialGradient) return false

        if (startRadius != other.startRadius) return false
        if (endRadius != other.endRadius) return false
        if (center != other.center) return false
        if (fallOffRadius != other.fallOffRadius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startRadius.hashCode()
        result = 31 * result + endRadius.hashCode()
        result = 31 * result + center.hashCode()
        result = 31 * result + fallOffRadius.hashCode()
        return result
    }

    override fun toString(): String =
        "BlurRadialGradient(startRadius=$startRadius, endRadius=$endRadius, center=$center, " +
            "fallOffRadius=$fallOffRadius)"
}

@Immutable
internal class BlurRadialStops(
    val stops: List<BlurStop>,
    val center: DpOffset,
    val fallOffRadius: Dp,
) : BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    fun resolveBlurCenter(size: Size, density: Density): Offset =
        resolveGradientCenter(center, size, density)

    fun resolveFallOffRadius(size: Size, density: Density): Float =
        resolveGradientRadius(fallOffRadius, size, density)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurRadialStops) return false

        if (stops != other.stops) return false
        if (center != other.center) return false
        if (fallOffRadius != other.fallOffRadius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stops.hashCode()
        result = 31 * result + center.hashCode()
        result = 31 * result + fallOffRadius.hashCode()
        return result
    }

    override fun toString(): String =
        "BlurRadialStops(stops=$stops, center=$center, fallOffRadius=$fallOffRadius)"
}

@Immutable
internal class BlurRadiusShader(val maxRadius: Dp, val shaderBlock: Density.(Size) -> Shader) :
    BlurRadiusSpec {
    override fun createRenderEffect(
        size: Size,
        density: Density,
        edgeTreatment: TileMode,
    ): RenderEffect = ActualProgressiveBlurEffect(this, size, density, edgeTreatment)

    // Opaque shader-based mask without structural equality.
    // The framework cannot observe the wrapped shader's mutable uniform state. Therefore, no two
    // logical configurations can be proven equal. Default identity equality ensures consumers treat
    // mask-based radii as unique instances.

    override fun toString(): String =
        "BlurRadiusShader(maxRadius=$maxRadius, shaderBlock=$shaderBlock)"
}

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurUniform,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurVerticalGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurVerticalStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurHorizontalGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurHorizontalStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurLinearGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurLinearStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurRadialGradient,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurRadialStops,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect

internal expect fun ActualProgressiveBlurEffect(
    spec: BlurRadiusShader,
    size: Size,
    density: Density,
    edgeTreatment: TileMode,
): RenderEffect
