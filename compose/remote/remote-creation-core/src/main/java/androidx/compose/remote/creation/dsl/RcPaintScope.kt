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
 * Typed Kotlin DSL scope wrapping the legacy Java [RcPaint] mutable builder.
 *
 * Each method takes typed value classes / enums (`RcColor`, `RcPaintStyle`, `RcStrokeCap`,
 * `RcBlendMode`, etc.) instead of raw `Int` opcodes. Method names drop the Java-style `set` prefix
 * for a more idiomatic Kotlin DSL feel.
 *
 * ```
 * applyPaint {
 *     color(RcColor.of(0xFFAA0000))
 *     style(RcPaintStyle.Stroke)
 *     strokeWidth(4f)
 *     strokeCap(RcStrokeCap.Round)
 * }
 * ```
 *
 * For paint methods not yet wrapped (gradients, axes, path effects, etc.) use the [raw] escape
 * hatch to access the underlying [RcPaint] builder directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcPaintScope {

    /** Underlying mutable [RcPaint] builder. Use for paint settings not yet wrapped. */
    public val raw: RcPaint

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
        weight: RcWeight = RcWeight.Normal,
        italic: Boolean = false,
    )

    /** Sets the paint typeface by named family (e.g. `"Roboto"`). */
    public fun typeface(name: String)

    // ---- Shaders ----

    /** Sets the active shader by [RcShader] reference (returned from `createShader`). */
    public fun shader(value: RcShader)

    /**
     * Installs a texture shader sampling the given image with the given tile modes.
     *
     * @param texture the bitmap-resource reference to sample
     * @param tileX tiling mode along the x axis (default [RcTileMode.Clamp])
     * @param tileY tiling mode along the y axis (default [RcTileMode.Clamp])
     * @param filterMode 0 = nearest, 1 = linear, 2 = nearest-mipmap (raw int for now)
     * @param maxAnisotropy 0 disables anisotropic filtering
     */
    public fun textureShader(
        texture: RcImage,
        tileX: RcTileMode = RcTileMode.Clamp,
        tileY: RcTileMode = RcTileMode.Clamp,
        filterMode: Short = 0,
        maxAnisotropy: Short = 0,
    )

    /** Sets the shader-transform matrix by id. */
    public fun shaderMatrix(matrixId: Float)

    // ---- Gradients ----

    /**
     * Linear gradient between two endpoints.
     *
     * @param startX,startY,endX,endY the gradient line endpoints
     * @param colors sRGB stops along the line
     * @param positions optional per-stop position in `[0, 1]`; `null` distributes evenly
     * @param tileMode behavior outside the gradient line
     * @param colorMask bitmask marking which entries of [colors] are remote color IDs
     */
    public fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
        colorMask: Int = 0,
    )

    /** Radial gradient around a center point. */
    public fun radialGradient(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: IntArray,
        positions: FloatArray? = null,
        tileMode: RcTileMode = RcTileMode.Clamp,
        colorMask: Int = 0,
    )

    /** Sweep gradient around a center point. */
    public fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: IntArray,
        positions: FloatArray? = null,
        colorMask: Int = 0,
    )
}

/** Internal implementation that forwards every method to a wrapped legacy [RcPaint]. */
internal class RcPaintScopeImpl(override val raw: RcPaint) : RcPaintScope {

    override fun color(value: Int) {
        raw.setColor(value)
    }

    override fun color(value: RcColor) {
        raw.setColor(value)
    }

    override fun color(value: RcColorValue) {
        raw.setColor(value)
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

    override fun typeface(fontType: RcFontType, weight: RcWeight, italic: Boolean) {
        raw.setTypeface(fontType.value, weight.value.toInt(), italic)
    }

    override fun typeface(name: String) {
        raw.setTypeface(name)
    }

    override fun shader(value: RcShader) {
        raw.setShader(value.id)
    }

    override fun textureShader(
        texture: RcImage,
        tileX: RcTileMode,
        tileY: RcTileMode,
        filterMode: Short,
        maxAnisotropy: Short,
    ) {
        raw.setTextureShader(texture.id, tileX.value, tileY.value, filterMode, maxAnisotropy)
    }

    override fun shaderMatrix(matrixId: Float) {
        raw.setShaderMatrix(matrixId)
    }

    override fun linearGradient(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colors: IntArray,
        positions: FloatArray?,
        tileMode: RcTileMode,
        colorMask: Int,
    ) {
        raw.setLinearGradient(
            startX,
            startY,
            endX,
            endY,
            colors,
            colorMask,
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
        colorMask: Int,
    ) {
        raw.setRadialGradient(
            centerX,
            centerY,
            radius,
            colors,
            colorMask,
            positions,
            tileMode.value.toInt(),
        )
    }

    override fun sweepGradient(
        centerX: Float,
        centerY: Float,
        colors: IntArray,
        positions: FloatArray?,
        colorMask: Int,
    ) {
        raw.setSweepGradient(centerX, centerY, colors, colorMask, positions)
    }
}
