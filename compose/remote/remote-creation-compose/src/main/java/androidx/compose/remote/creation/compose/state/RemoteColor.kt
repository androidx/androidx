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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteColor.OperationKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Represents a color that can be used with canvas APIs.
 *
 * `RemoteColor` represents a color value that can be a constant, a named variable, or a dynamic
 * expression (e.g., a color interpolation).
 */
@Stable
public open class RemoteColor
internal constructor(
    @get:Suppress("AutoBoxing") public override val constantValueOrNull: Color?,
    internal override val cacheKey: RemoteStateCacheKey,
    alpha: RemoteFloat?,
    red: RemoteFloat?,
    green: RemoteFloat?,
    blue: RemoteFloat?,
    internal val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : BaseRemoteState<Color>() {
    internal val configuredAlpha: RemoteFloat? = alpha
    internal val configuredRed: RemoteFloat? = red
    internal val configuredGreen: RemoteFloat? = green
    internal val configuredBlue: RemoteFloat? = blue

    internal enum class OperationKey {
        FromHSV,
        FromAHSV,
        FromArgb,
        Component,
        Multiply,
        Tween,
        TweenInt,
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        alpha: RemoteFloat,
        red: RemoteFloat,
        green: RemoteFloat,
        blue: RemoteFloat,
    ) : this(
        constantValueOrNull = constantColorOrNull(alpha, red, green, blue),
        alpha = alpha,
        red = red,
        green = green,
        blue = blue,
        cacheKey = RemoteOperationCacheKey.create(OperationKey.FromArgb, alpha, red, green, blue),
        idProvider = { creationState ->
            creationState.getOrPutVariableId(
                RemoteOperationCacheKey.create(OperationKey.FromArgb, alpha, red, green, blue)
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

    internal constructor(
        cacheKey: RemoteStateCacheKey,
        idProvider: (creationState: RemoteComposeCreationState) -> Int,
    ) : this(
        constantValueOrNull = null,
        cacheKey = cacheKey,
        alpha = null,
        red = null,
        green = null,
        blue = null,
        idProvider = idProvider,
    )

    /**
     * Constructor for creating a [RemoteColor] from a [Color] value. This creates a constant remote
     * color that is added to the remote document.
     *
     * @param color The color value.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        color: Color
    ) : this(
        constantValueOrNull = color,
        alpha = color.alpha.rf,
        red = color.red.rf,
        green = color.green.rf,
        blue = color.blue.rf,
        cacheKey = RemoteConstantCacheKey(color.toArgb()),
        idProvider = { creationState -> creationState.document.addColor(color.toArgb()) },
    )

    /**
     * Constructor for creating a [RemoteColor] from a direct ARGB integer color value. This creates
     * a constant remote color that is added to the remote document.
     *
     * @param color The ARGB integer representation of the color.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(@ColorInt color: Int) : this(Color(color))

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
    public operator fun times(other: RemoteColor): RemoteColor =
        rgb(
            red = red * other.red,
            green = green * other.green,
            blue = blue * other.blue,
            alpha = alpha * other.alpha,
        )

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
                    ColorAttribute.COLOR_ALPHA -> constantValueOrNull?.alpha
                    ColorAttribute.COLOR_RED -> constantValueOrNull?.red
                    ColorAttribute.COLOR_GREEN -> constantValueOrNull?.green
                    ColorAttribute.COLOR_BLUE -> constantValueOrNull?.blue
                    ColorAttribute.COLOR_HUE ->
                        constantValueOrNull?.let { Utils.getHue(it.toArgb()) }
                    ColorAttribute.COLOR_SATURATION ->
                        constantValueOrNull?.let { Utils.getSaturation(it.toArgb()) }
                    ColorAttribute.COLOR_BRIGHTNESS ->
                        constantValueOrNull?.let { Utils.getBrightness(it.toArgb()) }
                    else -> throw IllegalArgumentException("Unsupported $component")
                },
            cacheKey = key,
        ) { creationState ->
            floatArrayOf(
                creationState.document.getColorAttribute(idProvider(creationState), component)
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
            constantValueOrNull?.let { Utils.getHue(it.toArgb()).rf }
                ?: colorComponent(ColorAttribute.COLOR_HUE)

    /**
     * Returns a [RemoteFloat] that evaluates to the saturation of this [RemoteColor] in the range
     * [0..1].
     */
    public val saturation: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getSaturation(it.toArgb()).rf }
                ?: colorComponent(ColorAttribute.COLOR_SATURATION)

    /**
     * Returns a [RemoteFloat] that evaluates to the brightness of this [RemoteColor] in the range
     * [0..1].
     */
    public val brightness: RemoteFloat
        get() =
            constantValueOrNull?.let { Utils.getBrightness(it.toArgb()).rf }
                ?: colorComponent(ColorAttribute.COLOR_BRIGHTNESS)

    public companion object {
        /**
         * Creates a [RemoteColor] from a literal [Color] value.
         *
         * @param value The [Color] value.
         * @return A [RemoteColor] representing the constant color.
         */
        public operator fun invoke(value: Color): RemoteColor = RemoteColor(value)

        /**
         * Creates a [RemoteColor] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteColor] referencing the ID.
         */
        internal fun createForId(id: Int): RemoteColor =
            RemoteColor(idProvider = { id }, cacheKey = RemoteStateIdKey(id))

        /**
         * Creates a named [RemoteColor] with an initial value.
         *
         * Named remote colors can be set via AndroidRemoteContext.setNamedColor.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param defaultValue The initial [Color] value for the named remote color.
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @return A [RemoteColor] representing the named color.
         */
        @JvmStatic
        public fun createNamedRemoteColor(
            name: String,
            defaultValue: Color,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteColor {
            return RemoteColor(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
                alpha = null,
                red = null,
                green = null,
                blue = null,
                idProvider = { creationState ->
                    creationState.document.addNamedColor(
                        domain.prefixed(name),
                        defaultValue.toArgb(),
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
         * @param alpha The fixed alpha value the range [0..1].
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
                return RemoteColor(
                    Color.hsv(
                        hue = constH * 360f,
                        saturation = constS,
                        value = constV,
                        alpha = alphaV,
                    )
                )
            }

            // ColorExpression requires alpha to be constant in the range [0..255]
            val fixedAlpha = ((alpha.constantValueOrNull ?: 1f) * 255f).toInt()

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
                val argb = (alpha shl 24) or (0xffffff and Utils.hsvToRgb(constH, constS, constV))
                return RemoteColor(Color(color = argb))
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
         * Creates a [RemoteColor] from remote [alpha], [red], [green], and [blue] components.
         *
         * @param alpha [RemoteFloat] representing the alpha in the range [0..1].
         * @param red A [RemoteFloat] representing red in the range [0..1].
         * @param green A [RemoteFloat] representing green in the range [0..1].
         * @param blue A [RemoteFloat] representing blue in the range [0..1].
         * @return A new [RemoteColor] derived from the provided ARGB components.
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
                val color = Color(red = constR, green = constG, blue = constB, alpha = constA)
                return RemoteColor(color)
            }

            return RemoteColor(alpha = alpha, red = red, green = green, blue = blue)
        }

        /**
         * Creates a [RemoteColor] from [alpha], [red], [green], and [blue] components.
         *
         * @param alpha [Float] representing the alpha in the range [0..1].
         * @param red A [Float] representing red in the range [0..1].
         * @param green A [Float] representing green in the range [0..1].
         * @param blue A [Float] representing blue in the range [0..1].
         * @return A new [RemoteColor] derived from the provided ARGB components.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun rgb(alpha: Float, red: Float, green: Float, blue: Float): RemoteColor =
            RemoteColor(Color(red = red, green = green, blue = blue, alpha = alpha))
    }
}

/**
 * Remembers a named remote color expression.
 *
 * @param name The unique name for this remote color.
 * @param domain The domain of the named color (defaults to [RemoteState.Domain.User]).
 * @param initialValue The initial value.
 * @return A [RemoteColor] representing the named remote color expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteColor(
    name: String,
    initialValue: Color,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteColor {
    return rememberNamedState(name, domain) {
        RemoteColor(
            cacheKey = RemoteNamedCacheKey(domain, name),
            idProvider = { cs ->
                cs.getOrPutVariableId(RemoteNamedCacheKey(domain, name)) {
                    cs.document.addNamedColor(domain.prefixed(name), initialValue.toArgb())
                }
            },
        )
    }
}

/**
 * A Composable function to remember and provide a named mutable [RemoteColor].
 *
 * @param name The unique name for this remote color.
 * @param domain The domain of the named color (defaults to [RemoteState.Domain.User]).
 * @param value A lambda that provides the initial [Color] value.
 * @return A [RemoteColor] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
@Deprecated("Use rememberNamedRemoteColor with content lambda providing RemoteColor")
public fun rememberRemoteColor(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> Color,
): RemoteColor {
    return rememberNamedRemoteColor(name, value(), domain)
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
    val constFrom = from.constantValueOrNull
    val constTo = to.constantValueOrNull
    val constTween = tween.constantValueOrNull
    if (constFrom != null && constTo != null && constTween != null) {
        return RemoteColor(Utils.interpolateColor(constFrom.toArgb(), constTo.toArgb(), constTween))
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

/** Extension property to convert a [Color] to a [RemoteColor]. */
public val Color.rc: RemoteColor
    get() {
        return RemoteColor(this)
    }

/** Extension function to pack a [Color] into a Long for protocol use. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Color.pack(): Long = android.graphics.Color.pack(toArgb())

private fun constantColorOrNull(
    alpha: RemoteFloat,
    red: RemoteFloat,
    green: RemoteFloat,
    blue: RemoteFloat,
): Color? {
    val constA = alpha.constantValueOrNull
    val constR = red.constantValueOrNull
    val constG = green.constantValueOrNull
    val constB = blue.constantValueOrNull
    if (constA != null && constR != null && constG != null && constB != null) {
        return Color(red = constR, green = constG, blue = constB, alpha = constA)
    } else {
        return null
    }
}
