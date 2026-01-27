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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow

/**
 * Represents a color that can be used with canvas APIs. This class extends [RemoteState<Long>].
 * Note the Long representation specifies a color space that is used to distinguish expressions from
 * regular colors.
 *
 * @property constantValue The [Color] this [RemoteColor] always evaluates to, if any, or null if
 *   it's not constant.
 * @property idProvider A lambda function that provides the id of this [RemoteColor] within the
 *   [RemoteComposeCreationState].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteColor
internal constructor(
    public override val constantValueOrNull: Color?,
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

    public constructor(
        alpha: RemoteFloat,
        red: RemoteFloat,
        green: RemoteFloat,
        blue: RemoteFloat,
    ) : this(
        constantValueOrNull = null,
        alpha = alpha,
        red = red,
        green = green,
        blue = blue,
        idProvider = { creationState ->
            creationState.document
                .addColorExpression(
                    alpha.getFloatIdForCreationState(creationState),
                    red.getFloatIdForCreationState(creationState),
                    green.getFloatIdForCreationState(creationState),
                    blue.getFloatIdForCreationState(creationState),
                )
                .toInt()
        },
    )

    internal constructor(
        idProvider: (creationState: RemoteComposeCreationState) -> Int
    ) : this(
        constantValueOrNull = null,
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
    public constructor(
        color: Color
    ) : this(
        constantValueOrNull = color,
        alpha = color.alpha.rf,
        red = color.red.rf,
        green = color.green.rf,
        blue = color.blue.rf,
        idProvider = { creationState -> creationState.document.addColor(color.toArgb()) },
    )

    /**
     * Constructor for creating a [RemoteColor] from a direct ARGB integer color value. This creates
     * a constant remote color that is added to the remote document.
     *
     * @param color The ARGB integer representation of the color.
     */
    public constructor(@ColorInt color: Int) : this(Color(color))

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    /**
     * Computes the pairwise product of this [RemoteColor] with [other].
     *
     * @param other The [RemoteColor] to multiply with this [RemoteColor].
     * @return The result of multiplying [RemoteColor] by [other].
     */
    public operator fun times(other: RemoteColor): RemoteColor =
        fromARGB(alpha * other.alpha, red * other.red, green * other.green, blue * other.blue)

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

        return fromARGB(
            alpha ?: this.alpha,
            red ?: this.red,
            green ?: this.green,
            blue ?: this.blue,
        )
    }

    /**
     * Returns a [RemoteFloat] that evaluates a color component of this [RemoteColor] in the range
     * [0..1].
     */
    private fun colorComponent(component: Short): RemoteFloat {
        return RemoteFloatExpression(constantValueOrNull = null) { creationState ->
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteColor] from remote [hue], [saturation], and [value] (brightness)
         * components. The resulting color is expressed as a [RemoteColor] expression that combines
         * these inputs.
         *
         * @param hue A [RemoteFloat] representing the hue in the range [0..1].
         * @param saturation A [RemoteFloat] representing the saturation in the range [0..1].
         * @param value A [RemoteFloat] representing the brightness in the range [0..1].
         * @return A new [RemoteColor] derived from the provided HSV components.
         */
        public fun fromHSV(
            hue: RemoteFloat,
            saturation: RemoteFloat,
            value: RemoteFloat,
        ): RemoteColor {
            val constH = hue.constantValueOrNull
            val constS = saturation.constantValueOrNull
            val constV = value.constantValueOrNull
            if (constH != null && constS != null && constV != null) {
                return RemoteColor(Color(color = Utils.hsvToRgb(constH, constS, constV)))
            }

            val idFactory = Memorize { creationState ->
                creationState.document
                    .addColorExpression(
                        1f,
                        hue.getFloatIdForCreationState(creationState),
                        saturation.getFloatIdForCreationState(creationState),
                        value.getFloatIdForCreationState(creationState),
                    )
                    .toInt()
            }

            return RemoteColor(idProvider = { creationState -> idFactory.getId(creationState) })
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

            val idFactory = Memorize { creationState ->
                creationState.document
                    .addColorExpression(
                        alpha.toFloat() / 255f,
                        hue.getFloatIdForCreationState(creationState),
                        saturation.getFloatIdForCreationState(creationState),
                        value.getFloatIdForCreationState(creationState),
                    )
                    .toInt()
            }

            return RemoteColor(idProvider = { creationState -> idFactory.getId(creationState) })
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
        public fun fromARGB(
            alpha: RemoteFloat,
            red: RemoteFloat,
            green: RemoteFloat,
            blue: RemoteFloat,
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
        public fun fromARGB(alpha: Float, red: Float, green: Float, blue: Float): RemoteColor =
            RemoteColor(Color(red = red, green = green, blue = blue, alpha = alpha))
    }
}

/**
 * A Composable function to remember and provide a named [RemoteColor].
 *
 * @param name The unique name for this remote color.
 * @param domain The domain of the named color (defaults to "USER").
 * @param value A lambda that provides the initial [Color] value.
 * @return A [RemoteColor] instance that will be remembered across recompositions.
 */
@Composable
@RemoteComposable
public fun rememberRemoteColor(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    value: () -> Color,
): RemoteColor {
    return rememberNamedState(name, domain) {
        val color = value().toArgb()
        val idFactory = Memorize { creationState ->
            creationState.document.addNamedColor("$domain:$name", color)
        }
        RemoteColor(idProvider = { creationState -> idFactory.getId(creationState) })
    }
}

/** The same calculation as [Utils.interpolateColor]. */
private fun interpolate(from: Int, to: Int, tween: RemoteFloat): RemoteFloat {
    val c1 = (from.toDouble() / 255.0).pow(2.2).toFloat()
    val c2 = (to.toDouble() / 255.0).pow(2.2).toFloat()
    return clamp(0f, 1f, pow(lerp(c1, c2, tween), 1.0f / 2.2f))
}

/** The same calculation as [Utils.interpolateColor]. */
private fun interpolate(from: RemoteFloat, to: RemoteFloat, tween: RemoteFloat): RemoteFloat {
    val c1 = pow(from, 2.2f)
    val c2 = pow(to, 2.2f)
    return clamp(0f, 1f, pow(lerp(c1, c2, tween), 1.0f / 2.2f))
}

/**
 * Creates a remote color that interpolates between two integer ARGB colors based on a tween factor.
 *
 * @param from The starting color (ARGB integer).
 * @param to The ending color (ARGB integer).
 * @param tween A [RemoteFloat] representing the interpolation factor in range [0..1].
 * @return A new [RemoteColor] representing the tweened color.
 */
public fun tween(@ColorInt from: Int, @ColorInt to: Int, tween: RemoteFloat): RemoteColor {
    tween.constantValueOrNull?.let {
        return RemoteColor(Utils.interpolateColor(from, to, it))
    }

    return RemoteColor(
        idProvider = { creationState ->
            creationState.document
                .addColorExpression(
                    from,
                    to,
                    Utils.asNan(tween.getIdForCreationState(creationState)),
                )
                .toInt()
        }
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

    val idFactory = Memorize { creationState ->
        creationState.document
            .addColorExpression(
                from.getIdForCreationState(creationState).toShort(),
                to.getIdForCreationState(creationState).toShort(),
                Utils.asNan(tween.getIdForCreationState(creationState)),
            )
            .toInt()
    }

    return RemoteColor(idProvider = { creationState -> idFactory.getId(creationState) })
}

private class Memorize(val idProvider: (creationState: RemoteComposeCreationState) -> Int) {
    var memorizedValue: Int? = null

    fun getId(creationState: RemoteComposeCreationState): Int {
        memorizedValue?.let {
            return it
        }
        val result = idProvider(creationState)
        memorizedValue = result
        return result
    }
}

/** Extension property to convert a [Color] to a [RemoteColor]. */
public val Color.rc: RemoteColor
    get() {
        return RemoteColor(this)
    }

/** Extension function to pack a [Color] into a Long for protocol use. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Color.pack(): Long = android.graphics.Color.pack(toArgb())
