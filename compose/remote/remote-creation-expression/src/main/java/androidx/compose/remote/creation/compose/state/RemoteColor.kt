/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.state.RemoteColor.OperationKey
import androidx.compose.runtime.Stable

private const val WhiteArgb = 0xFFFFFFFF.toInt()
private const val TransparentArgb = 0x00000000
private const val BlackArgb = 0xFF000000.toInt()
private const val RgbMask: Int = 0x00FFFFFF

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public open class RemoteColor
internal constructor(
    // Backing with Long was considered to support wide color gamuts and color spaces (similar to
    // Compose UI Color or Android ColorLong), but handling color spaces would be difficult
    // outside of Android in platform-agnostic JVM targets, so 32-bit packed ARGB Int was chosen
    // instead.
    @get:ColorInt @get:Suppress("AutoBoxing") public override val constantValueOrNull: Int?,
    cacheKey: RemoteStateCacheKey,
    red: RemoteFloat?,
    green: RemoteFloat?,
    blue: RemoteFloat?,
    alpha: RemoteFloat?,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : BaseRemoteState<Int>(cacheKey) {

    @get:ColorInt
    @get:Suppress("AutoBoxing")
    public override val constantValue: Int
        get() = super.constantValue

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toDebugString(): String {
        val col = constantValueOrNull
        if (col != null) {
            return col.toUInt().toString(16).uppercase()
        }
        return super.toDebugString()
    }

    internal val configuredRed: RemoteFloat? = red
    internal val configuredGreen: RemoteFloat? = green
    internal val configuredBlue: RemoteFloat? = blue
    internal val configuredAlpha: RemoteFloat? = alpha

    internal enum class OperationKey : RemoteOperation {
        FromHSV,
        FromAHSV,
        FromArgb,
        Component,
        Multiply,
        Tween,
        TweenInt;

        override fun toDebugString(args: List<RemoteStateCacheKey>): String =
            when (this) {
                Multiply -> args.formatOp("*", 3)
                else -> formatCamelCaseFunction(args)
            }

        override fun reconstruct(args: List<BaseRemoteState<*>>): BaseRemoteState<*> {
            return when (this) {
                FromArgb ->
                    RemoteColor(
                        red = args[0] as RemoteFloat,
                        green = args[1] as RemoteFloat,
                        blue = args[2] as RemoteFloat,
                        alpha = args[3] as RemoteFloat,
                    )
                FromHSV ->
                    hsv(args[0] as RemoteFloat, args[1] as RemoteFloat, args[2] as RemoteFloat)
                FromAHSV ->
                    fromAHSV(
                        asConstantInt(args[0], default = 255),
                        args[1] as RemoteFloat,
                        args[2] as RemoteFloat,
                        args[3] as RemoteFloat,
                    )
                Multiply -> asRemoteColor(args[0]) * asRemoteColor(args[1])
                Tween ->
                    lerp(asRemoteColor(args[0]), asRemoteColor(args[1]), args[2] as RemoteFloat)
                TweenInt -> {
                    val from = args[0]
                    val to = args[1]
                    val t = args[2] as RemoteFloat
                    val fromInt = (from.constantValueOrNull as? Number)?.toInt()
                    val toInt = (to.constantValueOrNull as? Number)?.toInt()
                    if (fromInt != null && toInt != null) {
                        lerp(fromInt, toInt, t)
                    } else {
                        lerp(asRemoteColor(from), asRemoteColor(to), t)
                    }
                }
                Component -> {
                    val color = asRemoteColor(args[0])
                    when (val component = asConstantInt(args[1]).toShort()) {
                        ColorAttribute.COLOR_ALPHA -> color.alpha
                        ColorAttribute.COLOR_RED -> color.red
                        ColorAttribute.COLOR_GREEN -> color.green
                        ColorAttribute.COLOR_BLUE -> color.blue
                        ColorAttribute.COLOR_HUE -> color.hue
                        ColorAttribute.COLOR_SATURATION -> color.saturation
                        ColorAttribute.COLOR_BRIGHTNESS -> color.brightness
                        else -> throw IllegalArgumentException("Unknown component: $component")
                    }
                }
            }
        }
    }

    internal constructor(
        cacheKey: RemoteStateCacheKey,
        idProvider: (creationState: RemoteComposeCreationState) -> Int,
    ) : this(
        constantValueOrNull = null,
        cacheKey = cacheKey,
        red = null,
        green = null,
        blue = null,
        alpha = null,
        idProvider = idProvider,
    )

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
        return idProvider(creationState)
    }

    /**
     * Computes the pairwise product of this [RemoteColor] with [other].
     *
     * @param other The [RemoteColor] to multiply with this [RemoteColor].
     * @return The result of multiplying [RemoteColor] by [other].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public operator fun times(other: RemoteColor): RemoteColor {
        val thisConst = this.constantValueOrNull
        val otherConst = other.constantValueOrNull
        return when {
            thisConst == WhiteArgb -> other
            otherConst == WhiteArgb -> this
            thisConst == TransparentArgb -> this
            otherConst == TransparentArgb -> other
            thisConst == BlackArgb && other.alpha.constantValueOrNull == 1f -> this
            otherConst == BlackArgb && this.alpha.constantValueOrNull == 1f -> other
            else ->
                rgb(
                    red = red * other.red,
                    green = green * other.green,
                    blue = blue * other.blue,
                    alpha = alpha * other.alpha,
                )
        }
    }

    public fun copy(
        alpha: RemoteFloat? = null,
        red: RemoteFloat? = null,
        green: RemoteFloat? = null,
        blue: RemoteFloat? = null,
    ): RemoteColor {
        if (alpha == null && red == null && green == null && blue == null) {
            return this
        }

        return rgb(
            red = red ?: this.red,
            green = green ?: this.green,
            blue = blue ?: this.blue,
            alpha = alpha ?: this.alpha,
        )
    }

    /**
     * Returns a [RemoteFloat] that evaluates a color component of this [RemoteColor] in the range
     * [0..1].
     */
    private fun colorComponent(component: Short): RemoteFloat {
        val key = RemoteOperationCacheKey.create(OperationKey.Component, this, component)
        return RemoteFloat(
            constantValueOrNull =
                when (component) {
                    ColorAttribute.COLOR_ALPHA -> constantValueOrNull?.let(::extractAlphaFloat)
                    ColorAttribute.COLOR_RED -> constantValueOrNull?.let(::extractRedFloat)
                    ColorAttribute.COLOR_GREEN -> constantValueOrNull?.let(::extractGreenFloat)
                    ColorAttribute.COLOR_BLUE -> constantValueOrNull?.let(::extractBlueFloat)
                    ColorAttribute.COLOR_HUE -> constantValueOrNull?.let { Utils.getHue(it) }
                    ColorAttribute.COLOR_SATURATION ->
                        constantValueOrNull?.let { Utils.getSaturation(it) }
                    ColorAttribute.COLOR_BRIGHTNESS ->
                        constantValueOrNull?.let { Utils.getBrightness(it) }
                    else -> throw IllegalArgumentException("Unsupported $component")
                },
            cacheKey = key,
        ) { creationState ->
            floatArrayOf(
                creationState.document.getColorAttribute(
                    creationState.getOrPutVariableId(cacheKey) { idProvider(creationState) },
                    component,
                )
            )
        }
    }

    public val alpha: RemoteFloat
        get() = configuredAlpha ?: colorComponent(ColorAttribute.COLOR_ALPHA)

    public val red: RemoteFloat
        get() = configuredRed ?: colorComponent(ColorAttribute.COLOR_RED)

    public val green: RemoteFloat
        get() = configuredGreen ?: colorComponent(ColorAttribute.COLOR_GREEN)

    public val blue: RemoteFloat
        get() = configuredBlue ?: colorComponent(ColorAttribute.COLOR_BLUE)

    public val hue: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getHue(it).rf }
                ?: colorComponent(ColorAttribute.COLOR_HUE)

    public val saturation: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getSaturation(it).rf }
                ?: colorComponent(ColorAttribute.COLOR_SATURATION)

    public val brightness: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getBrightness(it).rf }
                ?: colorComponent(ColorAttribute.COLOR_BRIGHTNESS)

    public companion object {
        @Stable public val Black: RemoteColor = BlackArgb.rc

        @Stable public val DarkGray: RemoteColor = 0xFF444444.rc

        @Stable public val Gray: RemoteColor = 0xFF888888.rc

        @Stable public val LightGray: RemoteColor = 0xFFCCCCCC.rc

        @Stable public val White: RemoteColor = WhiteArgb.rc

        @Stable public val Red: RemoteColor = 0xFFFF0000.rc

        @Stable public val Green: RemoteColor = 0xFF00FF00.rc

        @Stable public val Blue: RemoteColor = 0xFF0000FF.rc

        @Stable public val Yellow: RemoteColor = 0xFFFFFF00.rc

        @Stable public val Cyan: RemoteColor = 0xFF00FFFF.rc

        @Stable public val Magenta: RemoteColor = 0xFFFF00FF.rc

        @Stable public val Transparent: RemoteColor = TransparentArgb.rc

        /**
         * Creates a [RemoteColor] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteColor] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteColor =
            RemoteColor(idProvider = { id }, cacheKey = RemoteStateIdKey(id))

        @JvmStatic
        public fun createNamedRemoteColor(
            name: String,
            @ColorInt defaultValue: Int,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteColor {
            return RemoteColor(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
                red = null,
                green = null,
                blue = null,
                alpha = null,
                idProvider = { creationState ->
                    creationState.document.addNamedColor(
                        domain.prefixed(name),
                        defaultValue,
                    )
                },
            )
        }

        @JvmStatic
        public fun createNamedRemoteColor(
            name: String,
            defaultValue: RemoteColor,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteColor {
            return RemoteColor(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
                red = null,
                green = null,
                blue = null,
                alpha = null,
                idProvider = { creationState ->
                    val constValue = defaultValue.constantValueOrNull
                    if (constValue != null) {
                        creationState.document.addNamedColor(domain.prefixed(name), constValue)
                    } else {
                        val baseId = defaultValue.getIdForCreationState(creationState).toShort()
                        val colorId =
                            creationState.document.addColorExpression(baseId, baseId, 0f).toInt()
                        creationState.document.setColorName(colorId, domain.prefixed(name))
                        colorId
                    }
                },
            )
        }

        public fun hsv(
            hue: RemoteFloat,
            saturation: RemoteFloat,
            value: RemoteFloat,
            alpha: RemoteFloat = 1.rf,
        ): RemoteColor {
            val constH = hue.constantValueOrNull
            val constS = saturation.constantValueOrNull
            val constV = value.constantValueOrNull
            val alphaV = alpha.constantValueOrNull
            if (constH != null && constS != null && constV != null && alphaV != null) {
                val rgb = Utils.hsvToRgb(constH, constS, constV)
                val alphaInt = floatComponentToByte(alphaV)
                val argb = packArgbWithRgb(alpha = alphaInt, rgb = rgb)
                return RemoteColor(argb)
            }

            // ColorExpression requires alpha to be constant in the range [0..255]
            val fixedAlpha = floatComponentToByte(alpha.constantValueOrNull ?: 1f)

            val colorWithConstantAlpha =
                RemoteColor(
                    cacheKey =
                        RemoteOperationCacheKey.create(
                            OperationKey.FromHSV,
                            hue,
                            saturation,
                            value,
                        ),
                    idProvider = { creationState ->
                        creationState.getOrPutVariableId(
                            RemoteOperationCacheKey.create(
                                OperationKey.FromHSV,
                                hue,
                                saturation,
                                value,
                            )
                        ) {
                            creationState.document
                                .addColorExpression(
                                    fixedAlpha,
                                    hue.getFloatIdForCreationState(creationState),
                                    saturation.getFloatIdForCreationState(creationState),
                                    value.getFloatIdForCreationState(creationState),
                                )
                                .toInt()
                        }
                    },
                )

            return if (alpha.hasConstantValue) {
                colorWithConstantAlpha
            } else {
                colorWithConstantAlpha.copy(alpha = alpha)
            }
        }

        /**
         * Creates a [RemoteColor] from a fixed alpha value and remote [hue], [saturation], and
         * [value] (brightness) components. This allows creating a remote color with a constant
         * opacity and dynamic HSV.
         *
         * @param alpha The fixed alpha value the range [0..255].
         * @param hue A [RemoteFloat] representing the hue in the range [0..1].
         * @param saturation A [RemoteFloat] representing the saturation in the range [0..1].
         * @param value A [RemoteFloat] representing the brightness in the range [0..1].
         * @return A new [RemoteColor] derived from the provided AHSV components.
         */
        internal fun fromAHSV(
            alpha: Int,
            hue: RemoteFloat,
            saturation: RemoteFloat,
            value: RemoteFloat,
        ): RemoteColor {
            val constH = hue.constantValueOrNull
            val constS = saturation.constantValueOrNull
            val constV = value.constantValueOrNull
            if (constH != null && constS != null && constV != null) {
                val rgb = Utils.hsvToRgb(constH, constS, constV)
                val argb = packArgbWithRgb(alpha = alpha, rgb = rgb)
                return RemoteColor(argb)
            }

            return RemoteColor(
                cacheKey =
                    RemoteOperationCacheKey.create(
                        OperationKey.FromAHSV,
                        alpha,
                        hue,
                        saturation,
                        value,
                    ),
                idProvider = { creationState ->
                    creationState.getOrPutVariableId(
                        RemoteOperationCacheKey.create(
                            OperationKey.FromAHSV,
                            alpha,
                            hue,
                            saturation,
                            value,
                        )
                    ) {
                        creationState.document
                            .addColorExpression(
                                alpha.toFloat() / 255f,
                                hue.getFloatIdForCreationState(creationState),
                                saturation.getFloatIdForCreationState(creationState),
                                value.getFloatIdForCreationState(creationState),
                            )
                            .toInt()
                    }
                },
            )
        }

        public fun rgb(
            red: RemoteFloat,
            green: RemoteFloat,
            blue: RemoteFloat,
            alpha: RemoteFloat = 1.rf,
        ): RemoteColor {
            return RemoteColor(red = red, green = green, blue = blue, alpha = alpha)
        }

        /**
         * Creates a [RemoteColor] from [red], [green], [blue], and [alpha] components.
         *
         * @param red A [Float] representing red in the range [0..1].
         * @param green A [Float] representing green in the range [0..1].
         * @param blue A [Float] representing blue in the range [0..1].
         * @param alpha [Float] representing the alpha in the range [0..1].
         * @return A new [RemoteColor] derived from the provided RGBA components.
         */
        internal fun rgb(red: Float, green: Float, blue: Float, alpha: Float = 1f): RemoteColor {
            val argb = packArgb(red = red, green = green, blue = blue, alpha = alpha)
            return RemoteColor(argb)
        }

        /**
         * Creates a themed [RemoteColor] resolved from [group] with [lightModeIndex] and
         * [darkModeIndex], falling back to [lightFallback] and [darkFallback].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createThemedRemoteColor(
            group: String,
            lightModeIndex: Short,
            darkModeIndex: Short,
            @ColorInt lightFallback: Int,
            @ColorInt darkFallback: Int,
        ): RemoteColor {
            val cacheKey =
                RemoteStateThemeColorKey(
                    group = group,
                    lightModeIndex = lightModeIndex,
                    darkModeIndex = darkModeIndex,
                    lightFallback = lightFallback,
                    darkFallback = darkFallback,
                )
            return RemoteColor(
                cacheKey = cacheKey,
                idProvider = { creationState ->
                    creationState.getOrPutVariableId(cacheKey) {
                        creationState.document
                            .addThemedColor(
                                group,
                                lightModeIndex,
                                darkModeIndex,
                                lightFallback,
                                darkFallback,
                            )
                            .toInt()
                    }
                },
            )
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteColor(@ColorInt color: Int): RemoteColor =
    RemoteColor(
        constantValueOrNull = color,
        red = extractRedFloat(color).rf,
        green = extractGreenFloat(color).rf,
        blue = extractBlueFloat(color).rf,
        alpha = extractAlphaFloat(color).rf,
        cacheKey = RemoteColorConstantCacheKey(color),
        idProvider = { creationState -> creationState.document.addColor(color) },
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteColor(
    red: RemoteFloat,
    green: RemoteFloat,
    blue: RemoteFloat,
    alpha: RemoteFloat = 1.rf,
): RemoteColor {
    val constantColor = constantColorOrNull(red = red, green = green, blue = blue, alpha = alpha)
    if (constantColor != null) {
        return RemoteColor(constantColor)
    }
    val cacheKey = RemoteOperationCacheKey.create(OperationKey.FromArgb, red, green, blue, alpha)
    return RemoteColor(
        constantValueOrNull = null,
        red = red,
        green = green,
        blue = blue,
        alpha = alpha,
        cacheKey = cacheKey,
        idProvider = { creationState ->
            creationState.getOrPutVariableId(cacheKey) {
                creationState.document
                    .addColorExpression(
                        alpha.getFloatIdForCreationState(creationState),
                        red.getFloatIdForCreationState(creationState),
                        green.getFloatIdForCreationState(creationState),
                        blue.getFloatIdForCreationState(creationState),
                    )
                    .toInt()
            }
        },
    )
}

/**
 * Creates a remote color that interpolates between two integer ARGB colors based on [fraction].
 *
 * @param from The starting color (ARGB integer).
 * @param to The ending color (ARGB integer).
 * @param fraction A [RemoteFloat] representing the interpolation factor in range [0..1].
 * @return A new [RemoteColor] representing the interpolated color.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun lerp(@ColorInt from: Int, @ColorInt to: Int, fraction: RemoteFloat): RemoteColor {
    if (from == to) {
        return RemoteColor(from)
    }
    fraction.constantValueOrNull?.let {
        return RemoteColor(Utils.interpolateColor(from, to, it))
    }

    return RemoteColor(
        cacheKey = RemoteOperationCacheKey.create(OperationKey.TweenInt, from, to, fraction),
        idProvider = { creationState ->
            creationState.getOrPutVariableId(
                RemoteOperationCacheKey.create(OperationKey.TweenInt, from, to, fraction)
            ) {
                creationState.document
                    .addColorExpression(
                        from,
                        to,
                        Utils.asNan(fraction.getIdForCreationState(creationState)),
                    )
                    .toInt()
            }
        },
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun lerp(from: RemoteColor, to: RemoteColor, fraction: RemoteFloat): RemoteColor {
    if (from.cacheKey == to.cacheKey) {
        return from
    }
    val constFrom = from.constantValueOrNull
    val constTo = to.constantValueOrNull
    if (constFrom != null && constTo != null) {
        return lerp(constFrom, constTo, fraction)
    }

    return RemoteColor(
        cacheKey = RemoteOperationCacheKey.create(OperationKey.Tween, from, to, fraction),
        idProvider = { creationState ->
            creationState.getOrPutVariableId(
                RemoteOperationCacheKey.create(OperationKey.Tween, from, to, fraction)
            ) {
                creationState.document
                    .addColorExpression(
                        from.getIdForCreationState(creationState).toShort(),
                        to.getIdForCreationState(creationState).toShort(),
                        Utils.asNan(fraction.getIdForCreationState(creationState)),
                    )
                    .toInt()
            }
        },
    )
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val @receiver:ColorInt Int.rc: RemoteColor
    get() = RemoteColor(this)

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val Long.rc: RemoteColor
    get() = RemoteColor(this.toInt())

/**
 * Composites this color on top of [background] using the Porter-Duff 'source over' mode.
 *
 * @param background The background [RemoteColor].
 * @return A new [RemoteColor] representing this color composited on top of [background].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteColor.compositeOver(background: RemoteColor): RemoteColor {
    // Both colors are constant
    val fg = this.constantValueOrNull
    val bg = background.constantValueOrNull
    if (fg != null && bg != null) {
        return RemoteColor(compositeOver(fg, bg))
    }

    // Both alphas are constant
    val constBgA = background.alpha.constantValueOrNull
    val constFgA = this.alpha.constantValueOrNull
    if (constBgA != null && constFgA != null) {
        val aVal = constFgA + constBgA * (1f - constFgA)
        if (aVal == 0f) {
            return RemoteColor.rgb(red = 0.rf, green = 0.rf, blue = 0.rf, alpha = 0.rf)
        } else {
            val foregroundFraction = constFgA / aVal
            val r = lerp(background.red, this.red, foregroundFraction.rf)
            val g = lerp(background.green, this.green, foregroundFraction.rf)
            val b = lerp(background.blue, this.blue, foregroundFraction.rf)
            return RemoteColor.rgb(red = r, green = g, blue = b, alpha = aVal.rf)
        }
    }

    // Opaque background
    if (constBgA == 1f) {
        val r = lerp(background.red, this.red, this.alpha)
        val g = lerp(background.green, this.green, this.alpha)
        val b = lerp(background.blue, this.blue, this.alpha)
        return RemoteColor.rgb(red = r, green = g, blue = b, alpha = 1.rf)
    }

    val a = this.alpha + (background.alpha * (1.rf - this.alpha))
    val r = compositeComponent(this.red, background.red, this.alpha, background.alpha, a)
    val g = compositeComponent(this.green, background.green, this.alpha, background.alpha, a)
    val b = compositeComponent(this.blue, background.blue, this.alpha, background.alpha, a)
    return RemoteColor.rgb(red = r, green = g, blue = b, alpha = a)
}

/**
 * Composites two packed ARGB integer colors using the standard Porter-Duff Source Over formula.
 *
 * Adapted from Compose UI's `Color.compositeOver`
 * (`compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/Color.kt`) to operate
 * directly on primitive 32-bit packed ARGB integers in sRGB color space without depending on
 * Compose UI graphics types.
 *
 * @param fg foreground ARGB color (source)
 * @param bg background ARGB color (destination)
 * @return blended ARGB integer color
 */
private fun compositeOver(fg: Int, bg: Int): Int {
    val fgA = extractAlpha(fg)
    val bgA = extractAlpha(bg)
    if (fgA == 0xFF) return fg
    if (fgA == 0) return bg
    val aFg = fgA / 255f
    val aBg = bgA / 255f
    val aOut = aFg + aBg * (1f - aFg)
    if (aOut == 0f) return TransparentArgb
    fun comp(fgC: Int, bgC: Int): Int {
        val cFg = fgC / 255f
        val cBg = bgC / 255f
        val cOut = (cFg * aFg + cBg * aBg * (1f - aFg)) / aOut
        return floatComponentToByte(cOut)
    }
    val r = comp(extractRed(fg), extractRed(bg))
    val g = comp(extractGreen(fg), extractGreen(bg))
    val b = comp(extractBlue(fg), extractBlue(bg))
    val a = floatComponentToByte(aOut)
    return packArgb(alpha = a, red = r, green = g, blue = b)
}

private fun compositeComponent(
    fgC: RemoteFloat,
    bgC: RemoteFloat,
    fgA: RemoteFloat,
    bgA: RemoteFloat,
    a: RemoteFloat,
): RemoteFloat {
    val isZero = a.isEqualTo(0.rf)
    val numerator = (fgC * fgA) + ((bgC * bgA) * (1.rf - fgA))
    return isZero.select(ifTrue = 0.rf, ifFalse = numerator / a)
}

/**
 * Converts a normalized floating-point color component in `[0..1]` to an 8-bit integer in
 * `[0..255]`.
 *
 * Rounds to the nearest integer and clamps to the `[0..255]` byte range.
 *
 * @param c normalized color component in `[0..1]`
 * @return clamped 8-bit integer color component in `[0..255]`
 */
private fun floatComponentToByte(c: Float): Int = (c * 255f + 0.5f).toInt().coerceIn(0, 255)

/** Extracts the alpha component (0..255) from a packed ARGB integer. */
private fun extractAlpha(color: Int): Int = (color ushr 24) and 0xFF

/** Extracts the red component (0..255) from a packed ARGB integer. */
private fun extractRed(color: Int): Int = (color ushr 16) and 0xFF

/** Extracts the green component (0..255) from a packed ARGB integer. */
private fun extractGreen(color: Int): Int = (color ushr 8) and 0xFF

/** Extracts the blue component (0..255) from a packed ARGB integer. */
private fun extractBlue(color: Int): Int = color and 0xFF

/** Extracts the alpha component as a normalized float in `[0..1]` from a packed ARGB integer. */
private fun extractAlphaFloat(color: Int): Float = extractAlpha(color) / 255f

/** Extracts the red component as a normalized float in `[0..1]` from a packed ARGB integer. */
private fun extractRedFloat(color: Int): Float = extractRed(color) / 255f

/** Extracts the green component as a normalized float in `[0..1]` from a packed ARGB integer. */
private fun extractGreenFloat(color: Int): Float = extractGreen(color) / 255f

/** Extracts the blue component as a normalized float in `[0..1]` from a packed ARGB integer. */
private fun extractBlueFloat(color: Int): Float = extractBlue(color) / 255f

/**
 * Packs [alpha], [red], [green], and [blue] byte components (0..255) into a packed ARGB integer.
 */
private fun packArgb(alpha: Int, red: Int, green: Int, blue: Int): Int =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue

/** Packs an [alpha] byte (0..255) and an [rgb] 24-bit integer into a packed ARGB integer. */
private fun packArgbWithRgb(alpha: Int, rgb: Int): Int = (alpha shl 24) or (rgb and RgbMask)

/**
 * Packs [red], [green], [blue], and [alpha] normalized floats in `[0..1]` into a packed ARGB
 * integer.
 */
private fun packArgb(red: Float, green: Float, blue: Float, alpha: Float): Int =
    packArgb(
        alpha = floatComponentToByte(alpha),
        red = floatComponentToByte(red),
        green = floatComponentToByte(green),
        blue = floatComponentToByte(blue),
    )

private fun constantColorOrNull(
    red: RemoteFloat,
    green: RemoteFloat,
    blue: RemoteFloat,
    alpha: RemoteFloat,
): Int? {
    val constA = alpha.constantValueOrNull
    val constR = red.constantValueOrNull
    val constG = green.constantValueOrNull
    val constB = blue.constantValueOrNull
    return if (constA != null && constR != null && constG != null && constB != null) {
        packArgb(red = constR, green = constG, blue = constB, alpha = constA)
    } else {
        null
    }
}

private fun asRemoteColor(arg: BaseRemoteState<*>): RemoteColor =
    when (arg) {
        is RemoteColor -> arg
        is RemoteInt ->
            arg.constantValueOrNull?.let { RemoteColor(it) }
                ?: throw IllegalArgumentException(
                    "Dynamic RemoteInt cannot be used as a color: $arg"
                )
        else -> throw IllegalArgumentException("Expected RemoteColor or RemoteInt, but found $arg")
    }

private fun asConstantInt(arg: BaseRemoteState<*>, default: Int = 0): Int =
    (arg.constantValueOrNull as? Number)?.toInt() ?: default

/**
 * A cache key for themed colors, identified by their [group], [lightModeIndex], [darkModeIndex],
 * [lightFallback], and [darkFallback].
 */
internal class RemoteStateThemeColorKey(
    internal val group: String,
    internal val lightModeIndex: Short,
    internal val darkModeIndex: Short,
    @ColorInt internal val lightFallback: Int,
    @ColorInt internal val darkFallback: Int,
) : BaseRemoteStateCacheKey() {
    private var state: BaseRemoteState<*>? = null

    override fun getState(): BaseRemoteState<*>? = state

    override fun setState(state: BaseRemoteState<*>?) {
        this.state = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteStateThemeColorKey) return false
        if (group != other.group) return false
        if (lightModeIndex != other.lightModeIndex) return false
        if (darkModeIndex != other.darkModeIndex) return false
        if (lightFallback != other.lightFallback) return false
        if (darkFallback != other.darkFallback) return false
        return true
    }

    override fun hashCodeImpl(): Int {
        var result = group.hashCode()
        result = 31 * result + lightModeIndex.hashCode()
        result = 31 * result + darkModeIndex.hashCode()
        result = 31 * result + lightFallback.hashCode()
        result = 31 * result + darkFallback.hashCode()
        return result
    }

    override fun toString(): String =
        "RemoteStateThemeColorKey(group=$group, lightModeIndex=$lightModeIndex, darkModeIndex=$darkModeIndex, lightFallback=$lightFallback, darkFallback=$darkFallback)"

    override fun toDebugString(): String =
        "themeColor($group, light=$lightModeIndex, dark=$darkModeIndex)"
}
