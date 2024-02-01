/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.tokens.SwitchTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/switch" class="external" target="_blank">Material Design Switch</a>.
 *
 * Switches toggle the state of a single item on or off.
 *
 * ![Switch image](https://developer.android.com/images/reference/androidx/compose/material3/switch.png)
 *
 * @sample androidx.compose.material3.samples.SwitchSample
 *
 * Switch can be used with a custom icon via [thumbContent] parameter
 *
 * @sample androidx.compose.material3.samples.SwitchWithThumbIconSample
 *
 * @param checked whether or not this switch is checked
 * @param onCheckedChange called when this switch is clicked. If `null`, then this switch will not
 * be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this switch
 * @param thumbContent content that will be drawn inside the thumb, expected to measure
 * [SwitchDefaults.IconSize]
 * @param enabled controls the enabled state of this switch. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors [SwitchColors] that will be used to resolve the colors used for this switch in
 * different states. See [SwitchDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this switch. You can use this to change the switch's
 * appearance or preview the switch in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 */
@Composable
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val uncheckedThumbDiameter = if (thumbContent == null) {
        UncheckedThumbDiameter
    } else {
        ThumbDiameter
    }

    val thumbPaddingStart = (SwitchHeight - uncheckedThumbDiameter) / 2
    val minBound = with(LocalDensity.current) { thumbPaddingStart.toPx() }
    val maxBound = with(LocalDensity.current) { ThumbPathLength.toPx() }
    val valueToOffset = remember<(Boolean) -> Float>(minBound, maxBound) {
        { value -> if (value) maxBound else minBound }
    }

    val targetValue = valueToOffset(checked)
    val offset = remember { Animatable(targetValue) }
    val scope = rememberCoroutineScope()

    SideEffect {
        // min bound might have changed if the icon is only rendered in checked state.
        offset.updateBounds(lowerBound = minBound)
    }

    DisposableEffect(checked) {
        if (offset.targetValue != targetValue) {
            scope.launch {
                offset.animateTo(targetValue, AnimationSpec)
            }
        }
        onDispose { }
    }

    // TODO: Add Swipeable modifier b/223797571
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            )
        } else {
            Modifier
        }

    Box(
        modifier
            .then(
                if (onCheckedChange != null) {
                    Modifier.minimumInteractiveComponentSize()
                } else {
                    Modifier
                }
            )
            .then(toggleableModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchWidth, SwitchHeight)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            colors = colors,
            thumbValue = offset.asState(),
            interactionSource = interactionSource,
            thumbShape = SwitchTokens.HandleShape.value,
            uncheckedThumbDiameter = uncheckedThumbDiameter,
            minBound = thumbPaddingStart,
            maxBound = ThumbPathLength,
            thumbContent = thumbContent,
        )
    }
}

@Composable
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
private fun BoxScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    thumbValue: State<Float>,
    thumbContent: (@Composable () -> Unit)?,
    interactionSource: InteractionSource,
    thumbShape: Shape,
    uncheckedThumbDiameter: Dp,
    minBound: Dp,
    maxBound: Dp,
) {
    val trackColor = colors.trackColor(enabled, checked)
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbValueDp = with(LocalDensity.current) { thumbValue.value.toDp() }
    val thumbSizeDp = if (isPressed) {
        SwitchTokens.PressedHandleWidth
    } else {
        uncheckedThumbDiameter + (ThumbDiameter - uncheckedThumbDiameter) *
            ((thumbValueDp - minBound) / (maxBound - minBound))
    }

    val thumbOffset = if (isPressed) {
        with(LocalDensity.current) {
            if (checked) {
                ThumbPathLength - SwitchTokens.TrackOutlineWidth
            } else {
                SwitchTokens.TrackOutlineWidth
            }.toPx()
        }
    } else {
        thumbValue.value
    }

    val trackShape = SwitchTokens.TrackShape.value
    val modifier = Modifier
        .align(Alignment.Center)
        .width(SwitchWidth)
        .height(SwitchHeight)
        .border(
            SwitchTokens.TrackOutlineWidth,
            colors.borderColor(enabled, checked),
            trackShape
        )
        .background(trackColor, trackShape)

    Box(modifier) {
        val resolvedThumbColor = colors.thumbColor(enabled, checked)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                .indication(
                    interactionSource = interactionSource,
                    indication = rippleOrFallbackImplementation(
                        bounded = false,
                        SwitchTokens.StateLayerSize / 2
                    )
                )
                .requiredSize(thumbSizeDp)
                .background(resolvedThumbColor, thumbShape),
            contentAlignment = Alignment.Center
        ) {
            if (thumbContent != null) {
                val iconColor = colors.iconColor(enabled, checked)
                CompositionLocalProvider(
                    LocalContentColor provides iconColor,
                    content = thumbContent
                )
            }
        }
    }
}

internal val ThumbDiameter = SwitchTokens.SelectedHandleWidth
internal val UncheckedThumbDiameter = SwitchTokens.UnselectedHandleWidth
private val SwitchWidth = SwitchTokens.TrackWidth
private val SwitchHeight = SwitchTokens.TrackHeight
private val ThumbPadding = (SwitchHeight - ThumbDiameter) / 2
private val ThumbPathLength = (SwitchWidth - ThumbDiameter) - ThumbPadding

private val AnimationSpec = TweenSpec<Float>(durationMillis = 100)

/**
 * Contains the default values used by [Switch]
 */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in
     * different states.
     */
    @Composable
    fun colors() = MaterialTheme.colorScheme.defaultSwitchColors

    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in
     * different states.
     *
     * @param checkedThumbColor the color used for the thumb when enabled and checked
     * @param checkedTrackColor the color used for the track when enabled and checked
     * @param checkedBorderColor the color used for the border when enabled and checked
     * @param checkedIconColor the color used for the icon when enabled and checked
     * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
     * @param uncheckedTrackColor the color used for the track when enabled and unchecked
     * @param uncheckedBorderColor the color used for the border when enabled and unchecked
     * @param uncheckedIconColor the color used for the icon when enabled and unchecked
     * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
     * @param disabledCheckedTrackColor the color used for the track when disabled and checked
     * @param disabledCheckedBorderColor the color used for the border when disabled and checked
     * @param disabledCheckedIconColor the color used for the icon when disabled and checked
     * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
     * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
     * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
     * @param disabledUncheckedIconColor the color used for the icon when disabled and unchecked
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = SwitchTokens.SelectedHandleColor.value,
        checkedTrackColor: Color = SwitchTokens.SelectedTrackColor.value,
        checkedBorderColor: Color = Color.Transparent,
        checkedIconColor: Color = SwitchTokens.SelectedIconColor.value,
        uncheckedThumbColor: Color = SwitchTokens.UnselectedHandleColor.value,
        uncheckedTrackColor: Color = SwitchTokens.UnselectedTrackColor.value,
        uncheckedBorderColor: Color = SwitchTokens.UnselectedFocusTrackOutlineColor.value,
        uncheckedIconColor: Color = SwitchTokens.UnselectedIconColor.value,
        disabledCheckedThumbColor: Color = SwitchTokens.DisabledSelectedHandleColor.value
            .copy(alpha = SwitchTokens.DisabledSelectedHandleOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledCheckedTrackColor: Color = SwitchTokens.DisabledSelectedTrackColor.value
            .copy(alpha = SwitchTokens.DisabledTrackOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledCheckedBorderColor: Color = Color.Transparent,
        disabledCheckedIconColor: Color = SwitchTokens.DisabledSelectedIconColor.value
            .copy(alpha = SwitchTokens.DisabledSelectedIconOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedThumbColor: Color = SwitchTokens.DisabledUnselectedHandleColor.value
            .copy(alpha = SwitchTokens.DisabledUnselectedHandleOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedTrackColor: Color = SwitchTokens.DisabledUnselectedTrackColor.value
            .copy(alpha = SwitchTokens.DisabledTrackOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedBorderColor: Color =
            SwitchTokens.DisabledUnselectedTrackOutlineColor.value
                .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedIconColor: Color = SwitchTokens.DisabledUnselectedIconColor.value
            .copy(alpha = SwitchTokens.DisabledUnselectedIconOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
    ): SwitchColors = SwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor,
        checkedBorderColor = checkedBorderColor,
        checkedIconColor = checkedIconColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedBorderColor = uncheckedBorderColor,
        uncheckedIconColor = uncheckedIconColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledCheckedBorderColor = disabledCheckedBorderColor,
        disabledCheckedIconColor = disabledCheckedIconColor,
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor,
        disabledUncheckedBorderColor = disabledUncheckedBorderColor,
        disabledUncheckedIconColor = disabledUncheckedIconColor
    )

    internal val ColorScheme.defaultSwitchColors: SwitchColors
        get() {
            return defaultSwitchColorsCached ?: SwitchColors(
                checkedThumbColor = fromToken(SwitchTokens.SelectedHandleColor),
                checkedTrackColor = fromToken(SwitchTokens.SelectedTrackColor),
                checkedBorderColor = Color.Transparent,
                checkedIconColor = fromToken(SwitchTokens.SelectedIconColor),
                uncheckedThumbColor = fromToken(SwitchTokens.UnselectedHandleColor),
                uncheckedTrackColor = fromToken(SwitchTokens.UnselectedTrackColor),
                uncheckedBorderColor = fromToken(SwitchTokens.UnselectedFocusTrackOutlineColor),
                uncheckedIconColor = fromToken(SwitchTokens.UnselectedIconColor),
                disabledCheckedThumbColor = fromToken(SwitchTokens.DisabledSelectedHandleColor)
                    .copy(alpha = SwitchTokens.DisabledSelectedHandleOpacity)
                    .compositeOver(surface),
                disabledCheckedTrackColor = fromToken(SwitchTokens.DisabledSelectedTrackColor)
                    .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                    .compositeOver(surface),
                disabledCheckedBorderColor = Color.Transparent,
                disabledCheckedIconColor = fromToken(SwitchTokens.DisabledSelectedIconColor)
                    .copy(alpha = SwitchTokens.DisabledSelectedIconOpacity)
                    .compositeOver(surface),
                disabledUncheckedThumbColor = fromToken(SwitchTokens.DisabledUnselectedHandleColor)
                    .copy(alpha = SwitchTokens.DisabledUnselectedHandleOpacity)
                    .compositeOver(surface),
                disabledUncheckedTrackColor = fromToken(SwitchTokens.DisabledUnselectedTrackColor)
                    .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                    .compositeOver(surface),
                disabledUncheckedBorderColor =
                fromToken(SwitchTokens.DisabledUnselectedTrackOutlineColor)
                    .copy(alpha = SwitchTokens.DisabledTrackOpacity)
                    .compositeOver(surface),
                disabledUncheckedIconColor = fromToken(SwitchTokens.DisabledUnselectedIconColor)
                    .copy(alpha = SwitchTokens.DisabledUnselectedIconOpacity)
                    .compositeOver(surface),
            ).also {
                defaultSwitchColorsCached = it
            }
        }

    /**
     * Icon size to use for `thumbContent`
     */
    val IconSize = 16.dp
}

/**
 * Represents the colors used by a [Switch] in different states
 *
 * @constructor create an instance with arbitrary colors.
 * See [SwitchDefaults.colors] for the default implementation that follows Material
 * specifications.
 *
 * @param checkedThumbColor the color used for the thumb when enabled and checked
 * @param checkedTrackColor the color used for the track when enabled and checked
 * @param checkedBorderColor the color used for the border when enabled and checked
 * @param checkedIconColor the color used for the icon when enabled and checked
 * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
 * @param uncheckedTrackColor the color used for the track when enabled and unchecked
 * @param uncheckedBorderColor the color used for the border when enabled and unchecked
 * @param uncheckedIconColor the color used for the icon when enabled and unchecked
 * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
 * @param disabledCheckedTrackColor the color used for the track when disabled and checked
 * @param disabledCheckedBorderColor the color used for the border when disabled and checked
 * @param disabledCheckedIconColor the color used for the icon when disabled and checked
 * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
 * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
 * @param disabledUncheckedBorderColor the color used for the border when disabled and unchecked
 * @param disabledUncheckedIconColor the color used for the icon when disabled and unchecked
 */
@Immutable
class SwitchColors constructor(
    val checkedThumbColor: Color,
    val checkedTrackColor: Color,
    val checkedBorderColor: Color,
    val checkedIconColor: Color,
    val uncheckedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedBorderColor: Color,
    val uncheckedIconColor: Color,
    val disabledCheckedThumbColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledCheckedBorderColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledUncheckedThumbColor: Color,
    val disabledUncheckedTrackColor: Color,
    val disabledUncheckedBorderColor: Color,
    val disabledUncheckedIconColor: Color
) {
    /**
     * Returns a copy of this SwitchColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        checkedThumbColor: Color = this.checkedThumbColor,
        checkedTrackColor: Color = this.checkedTrackColor,
        checkedBorderColor: Color = this.checkedBorderColor,
        checkedIconColor: Color = this.checkedIconColor,
        uncheckedThumbColor: Color = this.uncheckedThumbColor,
        uncheckedTrackColor: Color = this.uncheckedTrackColor,
        uncheckedBorderColor: Color = this.uncheckedBorderColor,
        uncheckedIconColor: Color = this.uncheckedIconColor,
        disabledCheckedThumbColor: Color = this.disabledCheckedThumbColor,
        disabledCheckedTrackColor: Color = this.disabledCheckedTrackColor,
        disabledCheckedBorderColor: Color = this.disabledCheckedBorderColor,
        disabledCheckedIconColor: Color = this.disabledCheckedIconColor,
        disabledUncheckedThumbColor: Color = this.disabledUncheckedThumbColor,
        disabledUncheckedTrackColor: Color = this.disabledUncheckedTrackColor,
        disabledUncheckedBorderColor: Color = this.disabledUncheckedBorderColor,
        disabledUncheckedIconColor: Color = this.disabledUncheckedIconColor,
    ) = SwitchColors(
        checkedThumbColor.takeOrElse { this.checkedThumbColor },
        checkedTrackColor.takeOrElse { this.checkedTrackColor },
        checkedBorderColor.takeOrElse { this.checkedBorderColor },
        checkedIconColor.takeOrElse { this.checkedIconColor },
        uncheckedThumbColor.takeOrElse { this.uncheckedThumbColor },
        uncheckedTrackColor.takeOrElse { this.uncheckedTrackColor },
        uncheckedBorderColor.takeOrElse { this.uncheckedBorderColor },
        uncheckedIconColor.takeOrElse { this.uncheckedIconColor },
        disabledCheckedThumbColor.takeOrElse { this.disabledCheckedThumbColor },
        disabledCheckedTrackColor.takeOrElse { this.disabledCheckedTrackColor },
        disabledCheckedBorderColor.takeOrElse { this.disabledCheckedBorderColor },
        disabledCheckedIconColor.takeOrElse { this.disabledCheckedIconColor },
        disabledUncheckedThumbColor.takeOrElse { this.disabledUncheckedThumbColor },
        disabledUncheckedTrackColor.takeOrElse { this.disabledUncheckedTrackColor },
        disabledUncheckedBorderColor.takeOrElse { this.disabledUncheckedBorderColor },
        disabledUncheckedIconColor.takeOrElse { this.disabledUncheckedIconColor },
    )

    /**
     * Represents the color used for the switch's thumb, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedThumbColor else uncheckedThumbColor
        } else {
            if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
        }

    /**
     * Represents the color used for the switch's track, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun trackColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedTrackColor else uncheckedTrackColor
        } else {
            if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
        }

    /**
     * Represents the color used for the switch's border, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun borderColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedBorderColor else uncheckedBorderColor
        } else {
            if (checked) disabledCheckedBorderColor else disabledUncheckedBorderColor
        }

    /**
     * Represents the content color passed to the icon if used
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Stable
    internal fun iconColor(enabled: Boolean, checked: Boolean): Color =
        if (enabled) {
            if (checked) checkedIconColor else uncheckedIconColor
        } else {
            if (checked) disabledCheckedIconColor else disabledUncheckedIconColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SwitchColors) return false

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedBorderColor != other.checkedBorderColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedBorderColor != other.uncheckedBorderColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedBorderColor != other.disabledCheckedBorderColor) return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false
        if (disabledUncheckedBorderColor != other.disabledUncheckedBorderColor) return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedBorderColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedBorderColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedBorderColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedBorderColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        return result
    }
}
