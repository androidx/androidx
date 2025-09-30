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

package androidx.compose.remote.frontend.state

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorLong
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.frontend.capture.RecordingCanvas.Companion.REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb

/**
 * Represents a color that can be used with canvas APIs. This class extends [RemoteState<Long>].
 * Note the Long representation specifies a color space that is used to distinguish expressions from
 * regular colors.
 *
 * @property hasConstantValue A boolean indicating whether this [RemoteColor] will always evaluate
 *   to the same [value]. This is a conservative check; some expressions that are effectively
 *   constant might still return `false`.
 * @property internalStateProvider A lambda function that provides the [InternalState] for this
 *   [RemoteColor] within the [RemoteComposeCreationState].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteColor
internal constructor(
    public override val hasConstantValue: Boolean,
    internal val internalStateProvider: (creationState: RemoteComposeCreationState) -> InternalState,
) : RemoteState<Long> {

    /**
     * Used to keep track of the argb fields as floats and defer construction of the argb color int
     * until the last moment.
     */
    internal class InternalState(
        val a: RemoteFloat,
        val r: RemoteFloat,
        val g: RemoteFloat,
        val b: RemoteFloat,
        val idProvider: () -> Int,
    )

    /**
     * Constructor for creating a [RemoteColor] from a direct ARGB integer color value. This creates
     * a constant remote color that is added to the remote document.
     *
     * @param color The ARGB integer representation of the color.
     */
    public constructor(
        @ColorInt color: Int
    ) : this(
        true,
        { creationState ->
            InternalState(
                RemoteFloat(Color.alpha(color).toFloat() / 255f),
                RemoteFloat(Color.red(color).toFloat() / 255f),
                RemoteFloat(Color.green(color).toFloat() / 255f),
                RemoteFloat(Color.blue(color).toFloat() / 255f),
                { creationState.document.addColor(color) },
            )
        },
    )

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        internalStateProvider(creationState).idProvider()

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

    /** Gets the current value of this [RemoteColor] as a [Long]. */
    @get:ColorLong
    public override val value: Long
        get() = id.toLong() shl 6 or REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID

    /**
     * Retrieves the value of this [RemoteColor] for a specific [RemoteComposeCreationState].
     * Similar to the `value` getter, but explicitly uses the provided `creationState` to get the
     * color\'s ID, ensuring context-aware retrieval.
     *
     * @param creationState The [RemoteComposeCreationState] context.
     * @return The [Long] representation of the color, including its ID and color space.
     */
    public fun getValueForCreationState(creationState: RemoteComposeCreationState): Long =
        getIdForCreationState(creationState).toLong() shl
            6 or
            REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID

    /**
     * Computes the pairwise product of this [RemoteColor] with [other].
     *
     * @param other The [RemoteColor] to multiply with this [RemoteColor].
     * @return The result of multiplying [RemoteColor] by [other].
     */
    public operator fun times(other: RemoteColor): RemoteColor {
        return RemoteColor(
            hasConstantValue && other.hasConstantValue,
            { creationState ->
                val internalState = internalStateProvider(creationState)
                val otherInternalState = other.internalStateProvider(creationState)
                val a = internalState.a * otherInternalState.a
                val r = internalState.r * otherInternalState.r
                val g = internalState.g * otherInternalState.g
                val b = internalState.b * otherInternalState.b
                InternalState(
                    a,
                    r,
                    g,
                    b,
                    {
                        creationState.document
                            .addColorExpression(
                                a.getFloatIdForCreationState(creationState),
                                r.getFloatIdForCreationState(creationState),
                                g.getFloatIdForCreationState(creationState),
                                b.getFloatIdForCreationState(creationState),
                            )
                            .toInt()
                    },
                )
            },
        )
    }

    /**
     * If this [RemoteColor] represents a constant value, then this method evaluates it and returns
     * it, otherwise it returns null.
     */
    public fun evaluateIfConstant(creationState: RemoteComposeCreationState): Int? {
        val internalState = internalStateProvider(creationState)
        val a = internalState.a.evaluateIfConstant(creationState) ?: return null
        val r = internalState.r.evaluateIfConstant(creationState) ?: return null
        val g = internalState.g.evaluateIfConstant(creationState) ?: return null
        val b = internalState.b.evaluateIfConstant(creationState) ?: return null
        return ((a * 255.0f + 0.5f).toInt() shl 24) or
            ((r * 255.0f + 0.5f).toInt() shl 16) or
            ((g * 255.0f + 0.5f).toInt() shl 8) or
            (b * 255.0f + 0.5f).toInt()
    }

    /**
     * Returns a [RemoteFloat] that evaluates to the hue of this [RemoteColor] in the range [0..1].
     */
    public val hue: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                val internalState = internalStateProvider(creationState)
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        internalState.idProvider(),
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
            RemoteFloatExpression(hasConstantValue) { creationState ->
                val internalState = internalStateProvider(creationState)
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        internalState.idProvider(),
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
            RemoteFloatExpression(hasConstantValue) { creationState ->
                val internalState = internalStateProvider(creationState)
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        internalState.idProvider(),
                        ColorAttribute.COLOR_BRIGHTNESS,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the red value of this [RemoteColor] in the range
     * [0..1].
     */
    public val red: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                internalStateProvider(creationState).r.arrayForCreationState(creationState)
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the green value of this [RemoteColor] in the range
     * [0..1].
     */
    public val green: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                internalStateProvider(creationState).g.arrayForCreationState(creationState)
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the blue value of this [RemoteColor] in the range
     * [0..1].
     */
    public val blue: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                internalStateProvider(creationState).b.arrayForCreationState(creationState)
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the alpha value of this [RemoteColor] in the range
     * [0..1].
     */
    public val alpha: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                internalStateProvider(creationState).a.arrayForCreationState(creationState)
            }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Creates a [RemoteColor] from [Color]. */
        @RequiresApi(26)
        public operator fun invoke(color: Color): RemoteColor {
            return RemoteColor(color.toArgb())
        }

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
            val hasConstantValue =
                hue.hasConstantValue && saturation.hasConstantValue && value.hasConstantValue
            return RemoteColor(
                hasConstantValue,
                { creationState ->
                    if (hasConstantValue) {
                        val h = hue.evaluateIfConstant(creationState)!!
                        val s = saturation.evaluateIfConstant(creationState)!!
                        val v = value.evaluateIfConstant(creationState)!!
                        val argb = Utils.hsvToRgb(h, s, v)
                        InternalState(
                            RemoteFloat(1f),
                            RemoteFloat(Color.red(argb).toFloat() / 255f),
                            RemoteFloat(Color.green(argb).toFloat() / 255f),
                            RemoteFloat(Color.blue(argb).toFloat() / 255f),
                            { creationState.document.addColor(argb) },
                        )
                    } else {
                        val id =
                            creationState.document
                                .addColorExpression(
                                    1f,
                                    hue.getFloatIdForCreationState(creationState),
                                    saturation.getFloatIdForCreationState(creationState),
                                    value.getFloatIdForCreationState(creationState),
                                )
                                .toInt()

                        InternalState(
                            RemoteFloat(1f),
                            extractRed(hasConstantValue, id),
                            extractGreen(hasConstantValue, id),
                            extractBlue(hasConstantValue, id),
                            { id },
                        )
                    }
                },
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
            val hasConstantValue =
                hue.hasConstantValue && saturation.hasConstantValue && value.hasConstantValue
            return RemoteColor(
                hasConstantValue,
                { creationState ->
                    if (hasConstantValue) {
                        val h = hue.evaluateIfConstant(creationState)!!
                        val s = saturation.evaluateIfConstant(creationState)!!
                        val v = value.evaluateIfConstant(creationState)!!
                        val argb = (alpha shl 24) or (0xffffff and Utils.hsvToRgb(h, s, v))
                        InternalState(
                            RemoteFloat(alpha.toFloat() / 255f),
                            RemoteFloat(Color.red(argb).toFloat() / 255f),
                            RemoteFloat(Color.green(argb).toFloat() / 255f),
                            RemoteFloat(Color.blue(argb).toFloat() / 255f),
                            { creationState.document.addColor(argb) },
                        )
                    } else {
                        val id =
                            creationState.document
                                .addColorExpression(
                                    alpha,
                                    hue.getFloatIdForCreationState(creationState),
                                    saturation.getFloatIdForCreationState(creationState),
                                    value.getFloatIdForCreationState(creationState),
                                )
                                .toInt()

                        InternalState(
                            RemoteFloat(alpha.toFloat() / 255f),
                            extractRed(hasConstantValue, id),
                            extractGreen(hasConstantValue, id),
                            extractBlue(hasConstantValue, id),
                            { id },
                        )
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
        public fun fromARGB(
            alpha: RemoteFloat,
            red: RemoteFloat,
            green: RemoteFloat,
            blue: RemoteFloat,
        ): RemoteColor =
            RemoteColor(
                alpha.hasConstantValue &&
                    red.hasConstantValue &&
                    green.hasConstantValue &&
                    blue.hasConstantValue,
                { creationState ->
                    InternalState(
                        alpha,
                        red,
                        green,
                        blue,
                        {
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
                },
            )

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
                true,
                { creationState ->
                    InternalState(
                        RemoteFloat(alpha),
                        RemoteFloat(red),
                        RemoteFloat(green),
                        RemoteFloat(blue),
                        {
                            creationState.document
                                .addColorExpression(alpha, red, green, blue)
                                .toInt()
                        },
                    )
                },
            )
    }
}

private fun extractRed(hasConstantValue: Boolean, @ColorInt id: Int): RemoteFloat =
    RemoteFloatExpression(hasConstantValue) { creationState ->
        floatArrayOf(creationState.document.getColorAttribute(id, ColorAttribute.COLOR_RED))
    }

private fun extractGreen(hasConstantValue: Boolean, @ColorInt id: Int): RemoteFloat =
    RemoteFloatExpression(hasConstantValue) { creationState ->
        floatArrayOf(creationState.document.getColorAttribute(id, ColorAttribute.COLOR_GREEN))
    }

private fun extractBlue(hasConstantValue: Boolean, @ColorInt id: Int): RemoteFloat =
    RemoteFloatExpression(hasConstantValue) { creationState ->
        floatArrayOf(creationState.document.getColorAttribute(id, ColorAttribute.COLOR_BLUE))
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
        RemoteColor(hasConstantValue = false) { creationState ->
            val color = value().toArgb()
            RemoteColor.InternalState(
                RemoteFloat(Color.alpha(color).toFloat() / 255f),
                RemoteFloat(Color.red(color).toFloat() / 255f),
                RemoteFloat(Color.green(color).toFloat() / 255f),
                RemoteFloat(Color.blue(color).toFloat() / 255f),
                { creationState.document.addNamedColor("$domain:$name", color) },
            )
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
public fun tween(@ColorInt from: Int, @ColorInt to: Int, tween: RemoteFloat): RemoteColor =
    RemoteColor(
        tween.hasConstantValue,
        { creationState ->
            RemoteColor.InternalState(
                lerp(Color.alpha(from).toFloat() / 255f, Color.alpha(to).toFloat() / 255f, tween),
                lerp(Color.red(from).toFloat() / 255f, Color.red(to).toFloat() / 255f, tween),
                lerp(Color.green(from).toFloat() / 255f, Color.green(to).toFloat() / 255f, tween),
                lerp(Color.blue(from).toFloat() / 255f, Color.blue(to).toFloat() / 255f, tween),
                {
                    creationState.document
                        .addColorExpression(
                            from,
                            to,
                            Utils.asNan(tween.getIdForCreationState(creationState)),
                        )
                        .toInt()
                },
            )
        },
    )

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
public fun tween(from: RemoteColor, to: RemoteColor, tween: RemoteFloat): RemoteColor =
    RemoteColor(
        from.hasConstantValue && to.hasConstantValue && tween.hasConstantValue,
        { creationState ->
            val fromState = from.internalStateProvider(creationState)
            val toState = to.internalStateProvider(creationState)
            RemoteColor.InternalState(
                lerp(fromState.a, toState.a, tween),
                lerp(fromState.r, toState.r, tween),
                lerp(fromState.g, toState.g, tween),
                lerp(fromState.b, toState.b, tween),
                {
                    creationState.document
                        .addColorExpression(
                            fromState.idProvider().toShort(),
                            toState.idProvider().toShort(),
                            Utils.asNan(tween.getIdForCreationState(creationState)),
                        )
                        .toInt()
                },
            )
        },
    )
