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
package androidx.compose.remote.frontend.state

import androidx.annotation.ColorInt
import androidx.annotation.ColorLong
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.RecordingCanvas.Companion.REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Represents a color that can be used with canvas APIs. This class extends [RemoteState<Long>].
 * Note the Long representation specifies a color space that is used to distinguish expressions from
 * regular colors.
 *
 * @property hasConstantValue A boolean indicating whether this [RemoteColor] will always evaluate
 *   to the same [value]. This is a conservative check; some expressions that are effectively
 *   constant might still return `false`.
 * @property idProvider A lambda function that provides the unique ID for this [RemoteColor] within
 *   the [RemoteComposeCreationState]. This ID is crucial for the remote system to reference and
 *   update the color.
 */
open class RemoteColor(
    override val hasConstantValue: Boolean,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteState<Long> {

    /**
     * Constructor for creating a [RemoteColor] from a direct ARGB integer color value. This creates
     * a constant remote color that is added to the remote document.
     *
     * @param color The ARGB integer representation of the color.
     */
    constructor(
        @ColorInt color: Int
    ) : this(true, { creationState -> creationState.document.addColor(color) })

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
        idProvider(creationState)

    // @Deprecated("Use getIdForCreationState directly")
    // TODO: re-enable this asap
    val id: Int
        get() {
            // FallbackCreationState.state.platform.log(
            //     Platform.LogCategory.TODO,
            //     "Use RemoteColor.getIdForCreationState directly"
            // )
            return getIdForCreationState(FallbackCreationState.state)
        }

    /** Gets the current value of this [RemoteColor] as a [Long]. */
    @get:ColorLong
    override val value: Long
        get() = id.toLong() shl 6 or REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID

    /**
     * Retrieves the value of this [RemoteColor] for a specific [RemoteComposeCreationState].
     * Similar to the `value` getter, but explicitly uses the provided `creationState` to get the
     * color's ID, ensuring context-aware retrieval.
     *
     * @param creationState The [RemoteComposeCreationState] context.
     * @return The [Long] representation of the color, including its ID and color space.
     */
    fun getValueForCreationState(creationState: RemoteComposeCreationState): Long =
        getIdForCreationState(creationState).toLong() shl
            6 or
            REMOTE_COMPOSE_EXPRESSION_COLOR_SPACE_ID

    /**
     * Returns a [RemoteFloat] that evaluates to the hue of this [RemoteColor] in the range [0..1].
     */
    val hue: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_HUE,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the saturation of this [RemoteColor] in the range
     * [0..1].
     */
    val saturation: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_SATURATION,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the brightness of this [RemoteColor] in the range
     * [0..1].
     */
    val brightness: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_BRIGHTNESS,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the red value of this [RemoteColor] in the range
     * [0..1].
     */
    val red: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                System.err.println("<<< COLOR_RED")
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_RED,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the green value of this [RemoteColor] in the range
     * [0..1].
     */
    val green: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_GREEN,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the blue value of this [RemoteColor] in the range
     * [0..1].
     */
    val blue: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_BLUE,
                    )
                )
            }

    /**
     * Returns a [RemoteFloat] that evaluates to the alpha value of this [RemoteColor] in the range
     * [0..1].
     */
    val alpha: RemoteFloat
        get() =
            RemoteFloatExpression(hasConstantValue) { creationState ->
                floatArrayOf(
                    creationState.document.getColorAttribute(
                        getIdForCreationState(creationState),
                        ColorAttribute.COLOR_ALPHA,
                    )
                )
            }

    companion object {
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
        fun fromHSV(hue: RemoteFloat, saturation: RemoteFloat, value: RemoteFloat) =
            RemoteColor(
                hue.hasConstantValue && saturation.hasConstantValue && value.hasConstantValue,
                { creationState ->
                    creationState.document
                        .addColorExpression(
                            hue.getFloatIdForCreationState(creationState),
                            saturation.getFloatIdForCreationState(creationState),
                            value.getFloatIdForCreationState(creationState),
                        )
                        .toInt()
                },
            )

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
        fun fromAHSV(alpha: Int, hue: RemoteFloat, saturation: RemoteFloat, value: RemoteFloat) =
            RemoteColor(
                hue.hasConstantValue && saturation.hasConstantValue && value.hasConstantValue,
                { creationState ->
                    creationState.document
                        .addColorExpression(
                            alpha,
                            hue.getFloatIdForCreationState(creationState),
                            saturation.getFloatIdForCreationState(creationState),
                            value.getFloatIdForCreationState(creationState),
                        )
                        .toInt()
                },
            )

        /**
         * Creates a [RemoteColor] from remote [alpha], [hue], [saturation], and [value]
         * (brightness) components. This allows creating a remote color with dynamic opacity and
         * HSV.
         *
         * @param alpha [RemoteFloat] representing the alpha in the range [0..1].
         * @param hue A [RemoteFloat] representing the hue in the range [0..1].
         * @param saturation A [RemoteFloat] representing the saturation in the range [0..1].
         * @param value A [RemoteFloat] representing the brightness in the range [0..1].
         * @return A new [RemoteColor] derived from the provided AHSV components.
         */
        fun fromARGB(alpha: RemoteFloat, red: RemoteFloat, green: RemoteFloat, blue: RemoteFloat) =
            RemoteColor(
                alpha.hasConstantValue &&
                    red.hasConstantValue &&
                    green.hasConstantValue &&
                    blue.hasConstantValue,
                { creationState ->
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
fun rememberRemoteColor(name: String, domain: String = "USER", value: () -> Color): RemoteColor {
    val state = LocalRemoteComposeCreationState.current
    return remember(name) {
        RemoteColor(hasConstantValue = false) {
            state.document.addNamedColor("$domain:$name", value().toArgb())
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
fun tween(@ColorInt from: Int, @ColorInt to: Int, tween: RemoteFloat) =
    RemoteColor(
        tween.hasConstantValue,
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
fun tween(from: RemoteColor, to: RemoteColor, tween: RemoteFloat) =
    RemoteColor(
        from.hasConstantValue && to.hasConstantValue && tween.hasConstantValue,
        { creationState ->
            creationState.document
                .addColorExpression(
                    from.getIdForCreationState(creationState).toShort(),
                    to.getIdForCreationState(creationState).toShort(),
                    Utils.asNan(tween.getIdForCreationState(creationState)),
                )
                .toInt()
        },
    )
