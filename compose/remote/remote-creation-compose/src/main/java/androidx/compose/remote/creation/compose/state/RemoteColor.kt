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

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

/**
 * Represents a color that can be used with canvas APIs. This class extends [RemoteState<Long>].
 * Note the Long representation specifies a color space that is used to distinguish expressions from
 * regular colors.
 *
 * @property constantValue The [Color] this [RemoteColor] always evaluates to, if any, or null if
 *   it's not constant.
 * @property alpha A [RemoteFloat] that evaluates to the alpha value of this [RemoteColor] in the
 *   range [0..1].
 * @property red A [RemoteFloat] that evaluates to the red value of this [RemoteColor] in the range
 *   [0..1].
 * @property green A [RemoteFloat] that evaluates to the green value of this [RemoteColor] in the
 *   range [0..1].
 * @property blue A [RemoteFloat] that evaluates to the blue value of this [RemoteColor] in the
 *   range [0..1].
 * @property idProvider A lambda function that provides the id of this [RemoteColor] within the
 *   [RemoteComposeCreationState].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteColor
internal constructor(
    public override val constantValue: Color?,
    public val alpha: RemoteFloat,
    public val red: RemoteFloat,
    public val green: RemoteFloat,
    public val blue: RemoteFloat,
    internal val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteState<Color> {

    /**
     * Constructor for creating a [RemoteColor] from a direct ARGB integer color value. This creates
     * a constant remote color that is added to the remote document.
     *
     * @param color The ARGB integer representation of the color.
     */
    public constructor(
        @ColorInt color: Int
    ) : this(
        Color.valueOf(color),
        RemoteFloat(Color.alpha(color).toFloat() / 255f),
        RemoteFloat(Color.red(color).toFloat() / 255f),
        RemoteFloat(Color.green(color).toFloat() / 255f),
        RemoteFloat(Color.blue(color).toFloat() / 255f),
        { creationState -> creationState.document.addColor(color) },
    )

    /**
     * Constructor for creating a [RemoteColor] from a [Color] value. This creates a constant remote
     * color that is added to the remote document.
     *
     * @param color The ARGB integer representation of the color.
     */
    public constructor(
        color: Color
    ) : this(
        Color.valueOf(color.toArgb()),
        RemoteFloat(color.alpha()),
        RemoteFloat(color.red()),
        RemoteFloat(color.green()),
        RemoteFloat(color.blue()),
        { creationState -> creationState.document.addColor(color.toArgb()) },
    )

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    // @Deprecated("Use getIdForCreationState directly")
    // TODO: re-enable this asap
    public val id: Int
        get() {
            // FallbackCreationState.state.platform.log(
            //     Platform.LogCategory.TODO,
            //     "Use RemoteColor.getIdForCreationState directly"
            // )
            return getIdForCreationState(FallbackCreationState.state)
        }

    /** Gets the current value of this [RemoteColor] as an [Int]. */
    @Deprecated("This will be removed")
    public override val value: Color = constantValue ?: Color.valueOf(Color.WHITE)

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
     * Returns a [RemoteFloat] that evaluates to the hue of this [RemoteColor] in the range [0..1].
     */
    public val hue: RemoteFloat
        get() =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idProvider(creationState),
                        ColorAttribute.COLOR_HUE,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the saturation of this [RemoteColor] in the range
     * [0..1].
     */
    public val saturation: RemoteFloat
        get() =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idProvider(creationState),
                        ColorAttribute.COLOR_SATURATION,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the brightness of this [RemoteColor] in the range
     * [0..1].
     */
    public val brightness: RemoteFloat
        get() =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idProvider(creationState),
                        ColorAttribute.COLOR_BRIGHTNESS,
                    )
                )
            }

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
            val constH = hue.constantValue
            val constS = saturation.constantValue
            val constV = value.constantValue
            if (constH != null && constS != null && constV != null) {
                return RemoteColor(Color.valueOf(Utils.hsvToRgb(constH, constS, constV)))
            }

            val idFactory =
                Memorize() { creationState ->
                    creationState.document
                        .addColorExpression(
                            1f,
                            hue.getFloatIdForCreationState(creationState),
                            saturation.getFloatIdForCreationState(creationState),
                            value.getFloatIdForCreationState(creationState),
                        )
                        .toInt()
                }

            return RemoteColor(
                constantValue = null,
                alpha = RemoteFloat(1f),
                red =
                    RemoteFloatExpression(constantValue = null) { creationState ->
                        floatArrayOf(
                            creationState.document.getColorAttribute(
                                idFactory.getId(creationState),
                                ColorAttribute.COLOR_RED,
                            )
                        )
                    },
                green =
                    RemoteFloatExpression(constantValue = null) { creationState ->
                        floatArrayOf(
                            creationState.document.getColorAttribute(
                                idFactory.getId(creationState),
                                ColorAttribute.COLOR_GREEN,
                            )
                        )
                    },
                blue =
                    RemoteFloatExpression(constantValue = null) { creationState ->
                        floatArrayOf(
                            creationState.document.getColorAttribute(
                                idFactory.getId(creationState),
                                ColorAttribute.COLOR_BLUE,
                            )
                        )
                    },
                { creationState -> idFactory.getId(creationState) },
            )
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
            val constH = hue.constantValue
            val constS = saturation.constantValue
            val constV = value.constantValue
            if (constH != null && constS != null && constV != null) {
                val argb = (alpha shl 24) or (0xffffff and Utils.hsvToRgb(constH, constS, constV))
                return RemoteColor(Color.valueOf(argb))
            }

            val idFactory =
                Memorize() { creationState ->
                    creationState.document
                        .addColorExpression(
                            alpha.toFloat() / 255f,
                            hue.getFloatIdForCreationState(creationState),
                            saturation.getFloatIdForCreationState(creationState),
                            value.getFloatIdForCreationState(creationState),
                        )
                        .toInt()
                }

            return RemoteColor(
                constantValue = null,
                alpha = RemoteFloat(alpha.toFloat() / 255f),
                red =
                    RemoteFloatExpression(constantValue = null) { creationState ->
                        floatArrayOf(
                            creationState.document.getColorAttribute(
                                idFactory.getId(creationState),
                                ColorAttribute.COLOR_RED,
                            )
                        )
                    },
                green =
                    RemoteFloatExpression(constantValue = null) { creationState ->
                        floatArrayOf(
                            creationState.document.getColorAttribute(
                                idFactory.getId(creationState),
                                ColorAttribute.COLOR_GREEN,
                            )
                        )
                    },
                blue =
                    RemoteFloatExpression(constantValue = null) { creationState ->
                        floatArrayOf(
                            creationState.document.getColorAttribute(
                                idFactory.getId(creationState),
                                ColorAttribute.COLOR_BLUE,
                            )
                        )
                    },
                { creationState -> idFactory.getId(creationState) },
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
        public fun fromARGB(
            alpha: RemoteFloat,
            red: RemoteFloat,
            green: RemoteFloat,
            blue: RemoteFloat,
        ): RemoteColor {
            val constA = alpha.constantValue
            val constR = red.constantValue
            val constG = green.constantValue
            val constB = blue.constantValue
            if (constA != null && constR != null && constG != null && constB != null) {
                val color = Color.valueOf(constR, constG, constB, constA)
                return RemoteColor(color, alpha, red, green, blue) { creationState ->
                    creationState.document.addColor(color.toArgb())
                }
            }

            return RemoteColor(constantValue = null, alpha, red, green, blue) { creationState ->
                creationState.document
                    .addColorExpression(
                        alpha.getFloatIdForCreationState(creationState),
                        red.getFloatIdForCreationState(creationState),
                        green.getFloatIdForCreationState(creationState),
                        blue.getFloatIdForCreationState(creationState),
                    )
                    .toInt()
            }
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
            RemoteColor(
                Color.valueOf(red, green, blue, alpha),
                RemoteFloat(alpha),
                RemoteFloat(red),
                RemoteFloat(green),
                RemoteFloat(blue),
                { creationState ->
                    creationState.document.addColorExpression(alpha, red, green, blue).toInt()
                },
            )
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
@RequiresApi(26)
public fun rememberRemoteColor(
    name: String,
    domain: String = "USER",
    value: () -> androidx.compose.ui.graphics.Color,
): RemoteColor {
    return remember(name) {
        val color = value().toArgb()
        RemoteColor(
            constantValue = null,
            RemoteFloat(Color.alpha(color).toFloat() / 255f),
            RemoteFloat(Color.red(color).toFloat() / 255f),
            RemoteFloat(Color.green(color).toFloat() / 255f),
            RemoteFloat(Color.blue(color).toFloat() / 255f),
            { creationState -> creationState.document.addNamedColor("$domain:$name", color) },
        )
    }
}

/** The same calculation as [Utils.interpolateColor]. */
private fun interpolate(from: Int, to: Int, tween: RemoteFloat): RemoteFloat {
    val c1 = Math.pow(from.toDouble() / 255.0, 2.2).toFloat()
    val c2 = Math.pow(to.toDouble() / 255.0, 2.2).toFloat()
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
    tween.constantValue?.let {
        return RemoteColor(Utils.interpolateColor(from, to, it))
    }

    return RemoteColor(
        constantValue = null,
        interpolate(Color.alpha(from), Color.alpha(to), tween),
        interpolate(Color.red(from), Color.red(to), tween),
        interpolate(Color.green(from), Color.green(to), tween),
        interpolate(Color.blue(from), Color.blue(to), tween),
        { creationState ->
            creationState.document
                .addColorExpression(
                    from,
                    to,
                    Utils.asNan(tween.getIdForCreationState(creationState)),
                )
                .toInt()
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
    val constFrom = from.constantValue
    val constTo = to.constantValue
    val constTween = tween.constantValue
    if (constFrom != null && constTo != null && constTween != null) {
        return RemoteColor(Utils.interpolateColor(constFrom.toArgb(), constTo.toArgb(), constTween))
    }

    val idFactory =
        Memorize() { creationState ->
            creationState.document
                .addColorExpression(
                    from.getIdForCreationState(creationState).toShort(),
                    to.getIdForCreationState(creationState).toShort(),
                    Utils.asNan(tween.getIdForCreationState(creationState)),
                )
                .toInt()
        }

    return RemoteColor(
        constantValue = null,
        alpha =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idFactory.getId(creationState),
                        ColorAttribute.COLOR_ALPHA,
                    )
                )
            },
        red =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idFactory.getId(creationState),
                        ColorAttribute.COLOR_RED,
                    )
                )
            },
        green =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idFactory.getId(creationState),
                        ColorAttribute.COLOR_GREEN,
                    )
                )
            },
        blue =
            RemoteFloatExpression(constantValue = null) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        idFactory.getId(creationState),
                        ColorAttribute.COLOR_BLUE,
                    )
                )
            },
        { creationState -> idFactory.getId(creationState) },
    )
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
