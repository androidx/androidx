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

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RcPaint

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
public sealed interface RcPaintScope {

    // ---- Color ----

    /** Sets the paint color from a packed sRGB int (`0xAARRGGBB`). */
    public fun color(value: Int)

    /** Sets the paint color from a packed sRGB long (`0xAARRGGBB`). */
    public fun color(value: Long): Unit = color(value.toInt())

    /** Sets the paint color to the value of a remote [RcColor] resource. */
    public fun color(value: RcColor)

    /** Sets the paint color to the value of a remote [RcColorValue] expression. */
    public fun color(value: RcColorValue)

    /** Sets the paint alpha (0..1). */
    public fun alpha(value: Float)

    /** Sets the paint alpha to a remote [RcFloat] expression. */
    public fun alpha(value: RcFloat)

    // ---- Style / stroke ----

    /** Sets the paint draw style. */
    public fun style(value: RcPaintStyle)

    /** Sets the stroke width in pixel units. */
    public fun strokeWidth(value: Float)

    /** Sets the stroke width to a remote [RcFloat] expression. */
    public fun strokeWidth(value: RcFloat)

    /** Sets the stroke cap. */
    public fun strokeCap(value: RcStrokeCap)

    /** Sets the stroke join. */
    public fun strokeJoin(value: RcStrokeJoin)

    /** Sets the stroke miter limit (must be ≥ 0). */
    public fun strokeMiter(value: Float)

    // ---- Effects ----

    /** Toggles edge anti-aliasing. */
    public fun antiAlias(value: Boolean)

    /** Toggles bitmap filtering. */
    public fun filterBitmap(value: Boolean)

    /** Sets the porter-duff blend mode. */
    public fun blendMode(value: RcBlendMode)

    /** Installs a Porter-Duff color filter. */
    public fun colorFilter(color: Int, mode: RcBlendMode)

    /** Removes any installed color filter. */
    public fun clearColorFilter()

    /** Installs a path effect from raw spec data. See `PaintPathEffects` for layout. */
    public fun pathEffect(data: FloatArray?)

    // ---- Text ----

    /** Sets the paint text size in pixel units. */
    public fun textSize(value: Float)

    /** Sets the paint text size to a remote [RcFloat] expression. */
    public fun textSize(value: RcFloat)

    /** Sets the paint typeface family + weight + italic flag. */
    public fun typeface(
        fontType: RcFontType,
        weight: RcFontWeight = RcFontWeight.Normal,
        italic: Boolean = false,
    )

    /** Sets the paint typeface by named family (e.g. `"Roboto"`). */
    public fun typeface(name: String)

    // ---- Font Axis & Font Data ----

    /** Sets the font/variable typeface axes by 4-character OpenType [tags] and [values]. */
    public fun axis(tags: List<String>, values: FloatArray)

    /** Sets the typeface using a registered [RcFont] resource. */
    public fun typeface(font: RcFont)

    // ---- Shaders ----

    /** Sets the active shader by [RcShader] reference (returned from [RcScope.shader]). */
    public fun shader(value: RcShader)

    /** Clears the active shader from the paint. */
    public fun clearShader()

    /**
     * Installs a texture shader sampling the given image with the given tile modes.
     *
     * @param texture the bitmap-resource reference to sample
     * @param tileX tiling mode along the x axis (default [RcTileMode.Clamp])
     * @param tileY tiling mode along the y axis (default [RcTileMode.Clamp])
     * @param filterMode sampling filter mode (default [RcTextureFilterMode.Default])
     * @param maxAnisotropy maximum anisotropic filtering level (`0` disables anisotropic filtering)
     */
    public fun textureShader(
        texture: RcImage,
        tileX: RcTileMode = RcTileMode.Clamp,
        tileY: RcTileMode = RcTileMode.Clamp,
        filterMode: RcTextureFilterMode = RcTextureFilterMode.Default,
        maxAnisotropy: Int = 0,
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
    public fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient around a center point. */
    public fun radialGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: IntArray,
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
     * Any coordinate or radius may be a dynamic value, passed as `someRcFloat.toFloat()`.
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
    public fun radialGradient(
        startX: Float,
        startY: Float,
        startRadius: Float,
        endX: Float,
        endY: Float,
        endRadius: Float,
        colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Sweep gradient around a center point. */
    public fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: IntArray,
        positions: FloatArray? = null,
    )

    /** Linear gradient between two endpoints using dynamic [RcColor] stops. */
    public fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient around a center point using dynamic [RcColor] stops. */
    public fun radialGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Radial gradient between two circles using dynamic [RcColor] stops. */
    public fun radialGradient(
        startX: Float,
        startY: Float,
        startRadius: Float,
        endX: Float,
        endY: Float,
        endRadius: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
    )

    /** Sweep gradient around a center point using dynamic [RcColor] stops. */
    public fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: List<RcColor>,
        positions: FloatArray? = null,
    )
}

/** Internal implementation that forwards every method to a wrapped legacy [RcPaint]. */
internal class RcPaintScopeImpl(internal val raw: RcPaint) : RcPaintScope {

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
        raw.setColor(value.id)
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

    override fun pathEffect(data: FloatArray?) {
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
            tileX.value,
            tileY.value,
            filterMode.value,
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
            tileMode.value.toInt(),
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
            tileMode.value.toInt(),
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
            tileMode.value.toInt(),
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
            tileMode.value.toInt(),
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
            tileMode.value.toInt(),
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
            tileMode.value.toInt(),
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
}
