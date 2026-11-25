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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@file:JvmName("RcPaintScopes")

package androidx.compose.remote.creation.dsl

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.RemoteComposeWriter

/**
 * Typed Kotlin DSL scope for configuring paint attributes inside [RcScope.paint].
 *
 * Each method takes typed value classes (`RcColor`, `RcPaintStyle`, `RcStrokeCap`, `RcBlendMode`,
 * etc.) instead of raw `Int` opcodes. Method names drop the Java-style `set` prefix for a more
 * idiomatic Kotlin DSL feel.
 *
 * ```
 * paint {
 *     color(0xFFAA0000.rcColor())
 *     style(RcPaintStyle.Stroke)
 *     strokeWidth(4f)
 *     strokeCap(RcStrokeCap.Round)
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public sealed class RcPaintScope {

    // ---- Expressions ----

    /** Converts an `Int` literal to an [RcFloat] within this paint scope. */
    public abstract val Int.rf: RcFloat

    /** Converts a `Float` literal to an [RcFloat] within this paint scope. */
    public abstract val Float.rf: RcFloat

    // ---- Color ----

    /** Sets the paint color from a packed sRGB int (`0xAARRGGBB`). */
    public abstract fun color(@ColorInt value: Int)

    /** Sets the paint color from a 32-bit packed ARGB `Long` hex literal (`0xAARRGGBB`). */
    public fun color(value: Long): Unit = color(value.toInt())

    /** Sets the paint color to the value of a remote [RcColor] resource. */
    public abstract fun color(value: RcColor)

    /** Sets the paint color to a static [RcColorValue]. */
    public abstract fun color(value: RcColorValue)

    /** Sets the paint alpha (`0..1`). */
    public abstract fun alpha(@FloatRange(from = 0.0, to = 1.0) value: Float)

    /** Sets the paint alpha to a remote [RcFloat] expression. */
    public abstract fun alpha(value: RcFloat)

    // ---- Style / stroke ----

    /** Sets the paint draw style. */
    public abstract fun style(value: RcPaintStyle)

    /** Sets the stroke width in pixel units. */
    public abstract fun strokeWidth(@FloatRange(from = 0.0) value: Float)

    /** Sets the stroke width to a remote [RcFloat] expression. */
    public abstract fun strokeWidth(value: RcFloat)

    /** Sets the stroke cap. */
    public abstract fun strokeCap(value: RcStrokeCap)

    /** Sets the stroke join. */
    public abstract fun strokeJoin(value: RcStrokeJoin)

    /** Sets the stroke miter limit (must be >= 0). */
    public abstract fun strokeMiter(@FloatRange(from = 0.0) value: Float)

    // ---- Effects ----

    /** Toggles edge anti-aliasing. */
    public abstract fun antiAlias(value: Boolean)

    /** Toggles bitmap filtering. */
    public abstract fun filterBitmap(value: Boolean)

    /** Sets the blend mode. */
    public abstract fun blendMode(value: RcBlendMode)

    /** Installs a Porter-Duff color filter. */
    public abstract fun colorFilter(@ColorInt color: Int, mode: RcBlendMode)

    /** Installs a Porter-Duff color filter from a 32-bit packed ARGB `Long` hex literal. */
    public fun colorFilter(color: Long, mode: RcBlendMode): Unit = colorFilter(color.toInt(), mode)

    /** Installs a Porter-Duff color filter using a static [RcColorValue]. */
    public fun colorFilter(color: RcColorValue, mode: RcBlendMode): Unit =
        colorFilter(color.color, mode)

    /** Removes any installed color filter. */
    public abstract fun clearColorFilter()

    /** Installs a typed [RcPathEffect] on this paint, or clears it when [effect] is `null`. */
    public abstract fun pathEffect(effect: RcPathEffect?)

    /** Removes any installed path effect from this paint. */
    public abstract fun clearPathEffect()

    // ---- Text ----

    /** Sets the paint text size in pixel units. */
    public abstract fun textSize(@FloatRange(from = 0.0) value: Float)

    /** Sets the paint text size to a remote [RcFloat] expression. */
    public abstract fun textSize(value: RcFloat)

    /** Sets the paint typeface family + weight + italic flag. */
    public abstract fun typeface(
        fontType: RcFontType,
        weight: RcFontWeight = RcFontWeight.Normal,
        italic: Boolean = false,
    )

    /** Sets the paint typeface by named family (e.g. `"Roboto"`). */
    public abstract fun typeface(name: String)

    // ---- Font Axis & Font Data ----

    /** Sets the font/variable typeface axes by 4-character OpenType [tags] and [values]. */
    public abstract fun axis(tags: List<String>, values: FloatArray)

    /** Sets the typeface using a registered [RcFont] resource. */
    public abstract fun typeface(font: RcFont)

    // ---- Shaders ----

    /** Sets the active shader by [RcShader] reference (returned from [RcScope.shader]). */
    public abstract fun shader(value: RcShader)

    /** Clears the active shader from the paint. */
    public abstract fun clearShader()

    /**
     * Installs a texture shader sampling the given image with the given tile modes.
     *
     * @param texture the bitmap-resource reference to sample
     * @param tileX tiling mode along the x axis (default [RcTileMode.Clamp])
     * @param tileY tiling mode along the y axis (default [RcTileMode.Clamp])
     * @param filterMode sampling filter mode (default [RcTextureFilterMode.Default])
     * @param maxAnisotropy maximum anisotropic filtering level (`0` disables anisotropic filtering)
     */
    public abstract fun textureShader(
        texture: RcImage,
        tileX: RcTileMode = RcTileMode.Clamp,
        tileY: RcTileMode = RcTileMode.Clamp,
        filterMode: RcTextureFilterMode = RcTextureFilterMode.Default,
        @IntRange(from = 0) maxAnisotropy: Int = 0,
    )

    // ---- Gradients ----

    /**
     * Linear gradient between two endpoints.
     *
     * @param startX x-coordinate for the start of the gradient line
     * @param startY y-coordinate for the start of the gradient line
     * @param endX x-coordinate for the end of the gradient line
     * @param endY y-coordinate for the end of the gradient line
     * @param colors sRGB stops along the line
     * @param positions optional per-stop position in `[0, 1]`; `null` distributes evenly
     * @param tileMode behavior outside the gradient line
     */
    public abstract fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Linear gradient between two dynamic [RcFloat] endpoints. */
    public abstract fun linearGradient(
        startX: RcFloat,
        startY: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient around a center point. */
    public abstract fun radialGradient(
        centerX: Float,
        centerY: Float,
        @FloatRange(from = 0.0, fromInclusive = false) radius: Float,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient around a dynamic [RcFloat] center point and [radius]. */
    public abstract fun radialGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /**
     * Radial gradient between two circles: the start (focal) circle and the end circle.
     *
     * The gradient runs from the edge of the start circle to the edge of the end circle, which
     * allows off-center highlights, cone-like sweeps and "tunnel" effects that the single-circle
     * [radialGradient] cannot express. Setting [startX]/[startY] equal to [endX]/[endY] and
     * [startRadius] to `0f` reproduces the single-circle behavior.
     *
     * Requires API 31 on the player; below that the end circle alone is used.
     *
     * @param startX x-coordinate of the start (focal) circle center
     * @param startY y-coordinate of the start (focal) circle center
     * @param startRadius radius of the start (focal) circle, must be >= 0
     * @param endX x-coordinate of the end circle center
     * @param endY y-coordinate of the end circle center
     * @param endRadius radius of the end circle, must be > 0
     * @param colors sRGB colors distributed between the start and end circle
     * @param positions relative position of each color, or `null` for even distribution
     * @param tileMode how the gradient repeats outside the end circle
     */
    public abstract fun radialGradient(
        startX: Float,
        startY: Float,
        @FloatRange(from = 0.0) startRadius: Float,
        endX: Float,
        endY: Float,
        @FloatRange(from = 0.0, fromInclusive = false) endRadius: Float,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient between two circles with dynamic [RcFloat] geometry. */
    public abstract fun radialGradient(
        startX: RcFloat,
        startY: RcFloat,
        startRadius: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        endRadius: RcFloat,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Sweep gradient around a center point. */
    public abstract fun sweepGradient(
        centerX: Float,
        centerY: Float,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
    )

    /** Sweep gradient around a dynamic [RcFloat] center point. */
    public abstract fun sweepGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        @ColorInt colors: IntArray,
        positions: FloatArray? = null,
    )

    /** Linear gradient between two endpoints using static [RcColorValue] stops. */
    @JvmName("linearGradientColorValues")
    public fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    ): Unit =
        linearGradient(
            startX,
            startY,
            endX,
            endY,
            IntArray(colors.size) { colors[it].color },
            positions,
            tileMode,
        )

    /**
     * Linear gradient between two dynamic [RcFloat] endpoints using static [RcColorValue] stops.
     */
    @JvmName("linearGradientColorValues")
    public fun linearGradient(
        startX: RcFloat,
        startY: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    ): Unit =
        linearGradient(
            startX,
            startY,
            endX,
            endY,
            IntArray(colors.size) { colors[it].color },
            positions,
            tileMode,
        )

    /** Radial gradient around a center point using static [RcColorValue] stops. */
    @JvmName("radialGradientColorValues")
    public fun radialGradient(
        centerX: Float,
        centerY: Float,
        @FloatRange(from = 0.0, fromInclusive = false) radius: Float,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    ): Unit =
        radialGradient(
            centerX,
            centerY,
            radius,
            IntArray(colors.size) { colors[it].color },
            positions,
            tileMode,
        )

    /**
     * Radial gradient around a dynamic [RcFloat] center point using static [RcColorValue] stops.
     */
    @JvmName("radialGradientColorValues")
    public fun radialGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    ): Unit =
        radialGradient(
            centerX,
            centerY,
            radius,
            IntArray(colors.size) { colors[it].color },
            positions,
            tileMode,
        )

    /** Radial gradient between two circles using static [RcColorValue] stops. */
    @JvmName("radialGradientColorValues")
    public fun radialGradient(
        startX: Float,
        startY: Float,
        @FloatRange(from = 0.0) startRadius: Float,
        endX: Float,
        endY: Float,
        @FloatRange(from = 0.0, fromInclusive = false) endRadius: Float,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    ): Unit =
        radialGradient(
            startX,
            startY,
            startRadius,
            endX,
            endY,
            endRadius,
            IntArray(colors.size) { colors[it].color },
            positions,
            tileMode,
        )

    /** Radial gradient between two dynamic [RcFloat] circles using static [RcColorValue] stops. */
    @JvmName("radialGradientColorValues")
    public fun radialGradient(
        startX: RcFloat,
        startY: RcFloat,
        startRadius: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        endRadius: RcFloat,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    ): Unit =
        radialGradient(
            startX,
            startY,
            startRadius,
            endX,
            endY,
            endRadius,
            IntArray(colors.size) { colors[it].color },
            positions,
            tileMode,
        )

    /** Sweep gradient around a center point using static [RcColorValue] stops. */
    @JvmName("sweepGradientColorValues")
    public fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
    ): Unit = sweepGradient(centerX, centerY, IntArray(colors.size) { colors[it].color }, positions)

    /** Sweep gradient around a dynamic [RcFloat] center point using static [RcColorValue] stops. */
    @JvmName("sweepGradientColorValues")
    public fun sweepGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        colors: List<RcColorValue>,
        positions: FloatArray? = null,
    ): Unit = sweepGradient(centerX, centerY, IntArray(colors.size) { colors[it].color }, positions)

    /** Linear gradient between two endpoints using dynamic [RcColor] stops. */
    public abstract fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Linear gradient between two dynamic [RcFloat] endpoints using dynamic [RcColor] stops. */
    public abstract fun linearGradient(
        startX: RcFloat,
        startY: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient around a center point using dynamic [RcColor] stops. */
    public abstract fun radialGradient(
        centerX: Float,
        centerY: Float,
        @FloatRange(from = 0.0, fromInclusive = false) radius: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient around a dynamic [RcFloat] center point using dynamic [RcColor] stops. */
    public abstract fun radialGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient between two circles using dynamic [RcColor] stops. */
    public abstract fun radialGradient(
        startX: Float,
        startY: Float,
        @FloatRange(from = 0.0) startRadius: Float,
        endX: Float,
        endY: Float,
        @FloatRange(from = 0.0, fromInclusive = false) endRadius: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient between two dynamic [RcFloat] circles using dynamic [RcColor] stops. */
    public abstract fun radialGradient(
        startX: RcFloat,
        startY: RcFloat,
        startRadius: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        endRadius: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Sweep gradient around a center point using dynamic [RcColor] stops. */
    public abstract fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
    )

    /** Sweep gradient around a dynamic [RcFloat] center point using dynamic [RcColor] stops. */
    public abstract fun sweepGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray? = null,
    )
}

/** Installs a path effect from raw spec data. Kept `@RestrictTo` as a low-level wire bridge. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcPaintScope.pathEffect(data: FloatArray?) {
    (this as RcPaintScopeImpl).pathEffect(data)
}

/** Internal implementation that forwards every method to a wrapped [RcPaint]. */
internal class RcPaintScopeImpl(
    internal val writer: RemoteComposeWriter,
    internal val raw: RcPaint,
) : RcPaintScope() {

    private fun RcFloat.resolve(): Float = withWriter(this@RcPaintScopeImpl.writer).toFloat()

    override val Int.rf: RcFloat
        get() = RcFloat(writer, floatArrayOf(this.toFloat()))

    override val Float.rf: RcFloat
        get() = RcFloat(writer, floatArrayOf(this))

    private fun dynamicColorMask(colors: List<RcColor>): Int {
        var mask = 0
        for (i in colors.indices) {
            if (colors[i].id != 0) {
                mask = mask or (1 shl i)
            }
        }
        return mask
    }

    override fun color(value: Int) {
        raw.setColor(value)
    }

    override fun color(value: RcColor) {
        raw.setColorId(value.id)
    }

    override fun color(value: RcColorValue) {
        raw.setColor(value.color)
    }

    override fun alpha(value: Float) {
        raw.setAlpha(value)
    }

    override fun alpha(value: RcFloat) {
        raw.setAlpha(value)
    }

    override fun style(value: RcPaintStyle) {
        raw.setStyle(value.value)
    }

    override fun strokeWidth(value: Float) {
        raw.setStrokeWidth(value)
    }

    override fun strokeWidth(value: RcFloat) {
        raw.setStrokeWidth(value)
    }

    override fun strokeCap(value: RcStrokeCap) {
        raw.setStrokeCap(value.value)
    }

    override fun strokeJoin(value: RcStrokeJoin) {
        raw.setStrokeJoin(value.value)
    }

    override fun strokeMiter(value: Float) {
        raw.setStrokeMiter(value)
    }

    override fun antiAlias(value: Boolean) {
        raw.setAntiAlias(value)
    }

    override fun filterBitmap(value: Boolean) {
        raw.setFilterBitmap(value)
    }

    override fun blendMode(value: RcBlendMode) {
        raw.setBlendMode(value.value)
    }

    override fun colorFilter(color: Int, mode: RcBlendMode) {
        raw.setPorterDuffColorFilter(color, mode.value)
    }

    override fun clearColorFilter() {
        raw.clearColorFilter()
    }

    override fun pathEffect(effect: RcPathEffect?) {
        raw.setPathEffect(effect?.data)
    }

    override fun clearPathEffect() {
        raw.setPathEffect(null)
    }

    internal fun pathEffect(data: FloatArray?) {
        raw.setPathEffect(data)
    }

    override fun textSize(value: Float) {
        raw.setTextSize(value)
    }

    override fun textSize(value: RcFloat) {
        raw.setTextSize(value)
    }

    override fun typeface(fontType: RcFontType, weight: RcFontWeight, italic: Boolean) {
        raw.setTypeface(fontType.value, weight.value.toInt(), italic)
    }

    override fun typeface(name: String) {
        raw.setTypeface(name)
    }

    override fun axis(tags: List<String>, values: FloatArray) {
        raw.setAxis(tags.toTypedArray(), values)
    }

    override fun typeface(font: RcFont) {
        raw.setTypeface(font.id)
    }

    override fun shader(value: RcShader) {
        raw.setShader(value.id)
    }

    override fun clearShader() {
        raw.setShader(0)
    }

    override fun textureShader(
        texture: RcImage,
        tileX: RcTileMode,
        tileY: RcTileMode,
        filterMode: RcTextureFilterMode,
        maxAnisotropy: Int,
    ) {
        raw.setTextureShader(
            texture.id,
            tileX.value.toShort(),
            tileY.value.toShort(),
            filterMode.value.toShort(),
            maxAnisotropy.toShort(),
        )
    }

    override fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        raw.setLinearGradient(
            startX,
            startY,
            endX,
            endY,
            colors,
            0,
            positions,
            tileMode.value,
        )
    }

    override fun radialGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        raw.setRadialGradient(
            centerX,
            centerY,
            radius,
            colors,
            0,
            positions,
            tileMode.value,
        )
    }

    override fun radialGradient(
        startX: Float,
        startY: Float,
        startRadius: Float,
        endX: Float,
        endY: Float,
        endRadius: Float,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        raw.setRadialGradient(
            startX,
            startY,
            startRadius,
            endX,
            endY,
            endRadius,
            colors,
            0,
            positions,
            tileMode.value,
        )
    }

    override fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: IntArray,
        positions: FloatArray?,
    ) {
        raw.setSweepGradient(centerX, centerY, colors, 0, positions)
    }

    override fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: List<RcColor>,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        raw.setLinearGradient(
            startX,
            startY,
            endX,
            endY,
            IntArray(colors.size) { colors[it].id },
            dynamicColorMask(colors),
            positions,
            tileMode.value,
        )
    }

    override fun radialGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: List<RcColor>,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        raw.setRadialGradient(
            centerX,
            centerY,
            radius,
            IntArray(colors.size) { colors[it].id },
            dynamicColorMask(colors),
            positions,
            tileMode.value,
        )
    }

    override fun radialGradient(
        startX: Float,
        startY: Float,
        startRadius: Float,
        endX: Float,
        endY: Float,
        endRadius: Float,
        colors: List<RcColor>,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        raw.setRadialGradient(
            startX,
            startY,
            startRadius,
            endX,
            endY,
            endRadius,
            IntArray(colors.size) { colors[it].id },
            dynamicColorMask(colors),
            positions,
            tileMode.value,
        )
    }

    override fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: List<RcColor>,
        positions: FloatArray?,
    ) {
        raw.setSweepGradient(
            centerX,
            centerY,
            IntArray(colors.size) { colors[it].id },
            dynamicColorMask(colors),
            positions,
        )
    }

    override fun linearGradient(
        startX: RcFloat,
        startY: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        linearGradient(
            startX.resolve(),
            startY.resolve(),
            endX.resolve(),
            endY.resolve(),
            colors,
            positions,
            tileMode,
        )
    }

    override fun radialGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        radialGradient(
            centerX.resolve(),
            centerY.resolve(),
            radius.resolve(),
            colors,
            positions,
            tileMode,
        )
    }

    override fun radialGradient(
        startX: RcFloat,
        startY: RcFloat,
        startRadius: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        endRadius: RcFloat,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        radialGradient(
            startX.resolve(),
            startY.resolve(),
            startRadius.resolve(),
            endX.resolve(),
            endY.resolve(),
            endRadius.resolve(),
            colors,
            positions,
            tileMode,
        )
    }

    override fun sweepGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        colors: IntArray,
        positions: FloatArray?,
    ) {
        sweepGradient(centerX.resolve(), centerY.resolve(), colors, positions)
    }

    override fun linearGradient(
        startX: RcFloat,
        startY: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        linearGradient(
            startX.resolve(),
            startY.resolve(),
            endX.resolve(),
            endY.resolve(),
            colors,
            positions,
            tileMode,
        )
    }

    override fun radialGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        radius: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        radialGradient(
            centerX.resolve(),
            centerY.resolve(),
            radius.resolve(),
            colors,
            positions,
            tileMode,
        )
    }

    override fun radialGradient(
        startX: RcFloat,
        startY: RcFloat,
        startRadius: RcFloat,
        endX: RcFloat,
        endY: RcFloat,
        endRadius: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray?,
        tileMode: RcTileMode,
    ) {
        radialGradient(
            startX.resolve(),
            startY.resolve(),
            startRadius.resolve(),
            endX.resolve(),
            endY.resolve(),
            endRadius.resolve(),
            colors,
            positions,
            tileMode,
        )
    }

    override fun sweepGradient(
        centerX: RcFloat,
        centerY: RcFloat,
        colors: List<RcColor>,
        positions: FloatArray?,
    ) {
        sweepGradient(centerX.resolve(), centerY.resolve(), colors, positions)
    }
}
