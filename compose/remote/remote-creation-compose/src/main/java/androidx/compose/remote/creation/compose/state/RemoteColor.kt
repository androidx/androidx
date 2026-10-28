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

/**
 * Canvas color supporting constant values, named variables, and dynamic expressions.
 *
 * Constant color values are backed by a 32-bit packed ARGB integer ([ColorInt]) in sRGB color
 * space:
 * - Bits 24-31: Alpha component (0-255).
 * - Bits 16-23: Red component (0-255).
 * - Bits 8-15: Green component (0-255).
 * - Bits 0-7: Blue component (0-255).
 */
@Stable
public open class RemoteColor
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    // Backing with Long was considered to support wide color gamuts and color spaces (similar to
    // Compose UI Color or Android ColorLong), but handling color spaces would be difficult
    // outside of Android in platform-agnostic JVM targets, so 32-bit packed ARGB Int was chosen
    // instead.
    /** Constant 32-bit ARGB [ColorInt] value of this color, or `null` if dynamic. */
    @get:ColorInt @get:Suppress("AutoBoxing") public override val constantValueOrNull: Int?,
    cacheKey: RemoteStateCacheKey,
    red: RemoteFloat?,
    green: RemoteFloat?,
    blue: RemoteFloat?,
    alpha: RemoteFloat?,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : BaseRemoteState<Int>(cacheKey) {

    /**
     * Constant 32-bit ARGB [ColorInt] value of this color.
     *
     * @throws IllegalStateException if this color is dynamic.
     */
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
                    tween(asRemoteColor(args[0]), asRemoteColor(args[1]), args[2] as RemoteFloat)
                TweenInt -> {
                    val from = args[0]
                    val to = args[1]
                    val t = args[2] as RemoteFloat
                    val fromInt = (from.constantValueOrNull as? Number)?.toInt()
                    val toInt = (to.constantValueOrNull as? Number)?.toInt()
                    if (fromInt != null && toInt != null) {
                        tween(fromInt, toInt, t)
                    } else {
                        tween(asRemoteColor(from), asRemoteColor(to), t)
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        red: RemoteFloat,
        green: RemoteFloat,
        blue: RemoteFloat,
        alpha: RemoteFloat = 1.rf,
    ) : this(
        constantValueOrNull =
            constantColorOrNull(red = red, green = green, blue = blue, alpha = alpha),
        red = red,
        green = green,
        blue = blue,
        alpha = alpha,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.FromArgb, red, green, blue, alpha),
        idProvider = { creationState ->
            creationState.getOrPutVariableId(
                RemoteOperationCacheKey.create(OperationKey.FromArgb, red, green, blue, alpha)
            ) {
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
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

    /**
     * Constructor for creating a [RemoteColor] from a packed ARGB integer color value. This creates
     * a constant remote color that is added to the remote document.
     *
     * @param color The ARGB integer color value.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        @ColorInt color: Int
    ) : this(
        constantValueOrNull = color,
        red = extractRedFloat(color).rf,
        green = extractGreenFloat(color).rf,
        blue = extractBlueFloat(color).rf,
        alpha = extractAlphaFloat(color).rf,
        cacheKey = RemoteColorConstantCacheKey(color),
        idProvider = { creationState -> creationState.document.addColor(color) },
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int {
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

    /**
     * Creates a copy of this [RemoteColor] with the ability to override individual ARGB components.
     * If a component is not specified, it defaults to the corresponding component of the original
     * [RemoteColor].
     *
     * @param alpha Optional [RemoteFloat] to override the alpha component.
     * @param red Optional [RemoteFloat] to override the red component.
     * @param green Optional [RemoteFloat] to override the green component.
     * @param blue Optional [RemoteFloat] to override the blue component.
     * @return A new [RemoteColor] with the specified components overridden.
     */
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
        return RemoteFloatExpression(
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

    /**
     * Returns a [RemoteFloat] that evaluates to the alpha of this [RemoteColor] in the range
     * [0..1].
     */
    public val alpha: RemoteFloat
        get() = configuredAlpha ?: colorComponent(ColorAttribute.COLOR_ALPHA)

    /**
     * Returns a [RemoteFloat] that evaluates to the red of this [RemoteColor] in the range [0..1].
     */
    public val red: RemoteFloat
        get() = configuredRed ?: colorComponent(ColorAttribute.COLOR_RED)

    /**
     * Returns a [RemoteFloat] that evaluates to the green of this [RemoteColor] in the range
     * [0..1].
     */
    public val green: RemoteFloat
        get() = configuredGreen ?: colorComponent(ColorAttribute.COLOR_GREEN)

    /**
     * Returns a [RemoteFloat] that evaluates to the blue of this [RemoteColor] in the range [0..1].
     */
    public val blue: RemoteFloat
        get() = configuredBlue ?: colorComponent(ColorAttribute.COLOR_BLUE)

    /**
     * Returns a [RemoteFloat] that evaluates to the hue of this [RemoteColor] in the range [0..1].
     */
    public val hue: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getHue(it).rf }
                ?: colorComponent(ColorAttribute.COLOR_HUE)

    /**
     * Returns a [RemoteFloat] that evaluates to the saturation of this [RemoteColor] in the range
     * [0..1].
     */
    public val saturation: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getSaturation(it).rf }
                ?: colorComponent(ColorAttribute.COLOR_SATURATION)

    /**
     * Returns a [RemoteFloat] that evaluates to the brightness of this [RemoteColor] in the range
     * [0..1].
     */
    public val brightness: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getBrightness(it).rf }
                ?: colorComponent(ColorAttribute.COLOR_BRIGHTNESS)

    public companion object {
        /**
         * Creates a [RemoteColor] from a literal ARGB integer value.
         *
         * @param color The ARGB integer color value.
         * @return A [RemoteColor] representing the constant color.
         */
        public operator fun invoke(@ColorInt color: Int): RemoteColor = RemoteColor(color)

        /**
         * Creates a [RemoteColor] from [red], [green], [blue], and [alpha] components.
         *
         * @param red A [Float] representing red in the range [0..1].
         * @param green A [Float] representing green in the range [0..1].
         * @param blue A [Float] representing blue in the range [0..1].
         * @param alpha [Float] representing the alpha in the range [0..1].
         * @return A [RemoteColor] representing the constant color.
         */
        public operator fun invoke(
            red: Float,
            green: Float,
            blue: Float,
            alpha: Float = 1f,
        ): RemoteColor = rgb(red = red, green = green, blue = blue, alpha = alpha)

        /**
         * Creates a [RemoteColor] from remote [red], [green], [blue], and [alpha] components.
         *
         * @param red A [RemoteFloat] representing red in the range [0..1].
         * @param green A [RemoteFloat] representing green in the range [0..1].
         * @param blue A [RemoteFloat] representing blue in the range [0..1].
         * @param alpha [RemoteFloat] representing the alpha in the range [0..1].
         * @return A [RemoteColor] representing the color expression.
         */
        public operator fun invoke(
            red: RemoteFloat,
            green: RemoteFloat,
            blue: RemoteFloat,
            alpha: RemoteFloat = 1.rf,
        ): RemoteColor = rgb(red = red, green = green, blue = blue, alpha = alpha)

        /**
         * Creates a [RemoteColor] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteColor] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteColor =
            RemoteColor(idProvider = { id }, cacheKey = RemoteStateIdKey(id))

        /**
         * Creates a named [RemoteColor] with an initial value.
         *
         * Named remote colors can be set via AndroidRemoteContext.setNamedColor.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param defaultValue The initial ARGB integer value for the named remote color.
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @return A [RemoteColor] representing the named color.
         */
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

        /**
         * Creates a [RemoteColor] from remote [hue], [saturation], and [value] (brightness)
         * components. The resulting color is expressed as a [RemoteColor] expression that combines
         * these inputs.
         *
         * @param hue A [RemoteFloat] representing the hue in the range [0..1].
         * @param saturation A [RemoteFloat] representing the saturation in the range [0..1].
         * @param value A [RemoteFloat] representing the brightness in the range [0..1].
         * @param alpha A [RemoteFloat] representing the alpha in the range [0..1].
         * @return A new [RemoteColor] derived from the provided HSV components.
         */
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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromAHSV(
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

        /**
         * Creates a [RemoteColor] from remote [red], [green], [blue], and [alpha] components.
         *
         * @param red A [RemoteFloat] representing red in the range [0..1].
         * @param green A [RemoteFloat] representing green in the range [0..1].
         * @param blue A [RemoteFloat] representing blue in the range [0..1].
         * @param alpha [RemoteFloat] representing the alpha in the range [0..1].
         * @return A new [RemoteColor] derived from the provided RGBA components.
         */
        public fun rgb(
            red: RemoteFloat,
            green: RemoteFloat,
            blue: RemoteFloat,
            alpha: RemoteFloat = 1.rf,
        ): RemoteColor {
            val constA = alpha.constantValueOrNull
            val constR = red.constantValueOrNull
            val constG = green.constantValueOrNull
            val constB = blue.constantValueOrNull
            if (constA != null && constR != null && constG != null && constB != null) {
                val argb = packArgb(red = constR, green = constG, blue = constB, alpha = constA)
                return RemoteColor(argb)
            }

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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun rgb(red: Float, green: Float, blue: Float, alpha: Float = 1f): RemoteColor {
            val argb = packArgb(red = red, green = green, blue = blue, alpha = alpha)
            return RemoteColor(argb)
        }
    }
}

/**
 * Creates a remote color that interpolates between two integer ARGB colors based on a tween factor.
 *
 * @param from The starting color (ARGB integer).
 * @param to The ending color (ARGB integer).
 * @param tween A [RemoteFloat] representing the interpolation factor in range [0..1].
 * @return A new [RemoteColor] representing the tweened color.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun tween(@ColorInt from: Int, @ColorInt to: Int, tween: RemoteFloat): RemoteColor {
    if (from == to) {
        return RemoteColor(from)
    }
    tween.constantValueOrNull?.let {
        return RemoteColor(Utils.interpolateColor(from, to, it))
    }

    return RemoteColor(
        cacheKey = RemoteOperationCacheKey.create(OperationKey.TweenInt, from, to, tween),
        idProvider = { creationState ->
            creationState.getOrPutVariableId(
                RemoteOperationCacheKey.create(OperationKey.TweenInt, from, to, tween)
            ) {
                creationState.document
                    .addColorExpression(
                        from,
                        to,
                        Utils.asNan(tween.getIdForCreationState(creationState)),
                    )
                    .toInt()
            }
        },
    )
}

/**
 * Creates a remote color that interpolates between two [RemoteColor]s based on a tween factor. This
 * allows for dynamic color transitions where both the start/end colors and the interpolation factor
 * can be remote expressions.
 *
 * @param from The starting [RemoteColor].
 * @param to The ending [RemoteColor].
 * @param tween A [RemoteFloat] representing the interpolation factor in range [0..1].
 * @return A new [RemoteColor] representing the tweened color.
 */
public fun tween(from: RemoteColor, to: RemoteColor, tween: RemoteFloat): RemoteColor {
    if (from.cacheKey == to.cacheKey) {
        return from
    }
    val constFrom = from.constantValueOrNull
    val constTo = to.constantValueOrNull
    val constTween = tween.constantValueOrNull
    if (constFrom != null && constTo != null && constTween != null) {
        return RemoteColor(Utils.interpolateColor(constFrom, constTo, constTween))
    }

    return RemoteColor(
        cacheKey = RemoteOperationCacheKey.create(OperationKey.Tween, from, to, tween),
        idProvider = { creationState ->
            creationState.getOrPutVariableId(
                RemoteOperationCacheKey.create(OperationKey.Tween, from, to, tween)
            ) {
                creationState.document
                    .addColorExpression(
                        from.getIdForCreationState(creationState).toShort(),
                        to.getIdForCreationState(creationState).toShort(),
                        Utils.asNan(tween.getIdForCreationState(creationState)),
                    )
                    .toInt()
            }
        },
    )
}

/** Extension property to convert an ARGB [Int] to a [RemoteColor]. */
public val @receiver:ColorInt Int.rc: RemoteColor
    get() = RemoteColor(this)

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
