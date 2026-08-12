/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.tokens.CheckboxTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.max

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/overview)
 *
 * Checkboxes allow users to select one or more items from a set. Checkboxes can turn an option on
 * or off.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/checkbox.png)
 *
 * Simple Checkbox sample:
 *
 * @sample androidx.compose.material3.samples.CheckboxSample
 *
 * Combined Checkbox with Text sample:
 *
 * @sample androidx.compose.material3.samples.CheckboxWithTextSample
 * @param checked whether this checkbox is checked or unchecked
 * @param onCheckedChange called when this checkbox is clicked. If `null`, then this checkbox will
 *   not be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 */
@Composable
public fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    CheckboxImpl(
        state = ToggleableState(checked),
        onClick = wrapOnCheckedChange(checked, onCheckedChange),
        checkmarkStroke = null,
        outlineStroke = null,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/overview)
 *
 * Checkboxes allow users to select one or more items from a set. Checkboxes can turn an option on
 * or off.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/checkbox.png)
 *
 * This Checkbox function offers greater flexibility in visual customization. Using the [Stroke]
 * parameters, you can control the appearance of both the checkmark and the box that surrounds it.
 *
 * A sample of a `Checkbox` that uses a [Stroke] with rounded [StrokeCap] and
 * [androidx.compose.ui.graphics.StrokeJoin]:
 *
 * @sample androidx.compose.material3.samples.CheckboxRoundedStrokesSample
 * @param checked whether this checkbox is checked or unchecked
 * @param onCheckedChange called when this checkbox is clicked. If `null`, then this checkbox will
 *   not be interactable, unless something else handles its input events and updates its state.
 * @param checkmarkStroke stroke for the checkmark.
 * @param outlineStroke stroke for the checkmark's box outline. Note that this stroke is applied
 *   when drawing the outline's rounded rectangle, so attributions such as
 *   [androidx.compose.ui.graphics.StrokeJoin] will be ignored.
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 */
@Composable
public fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    checkmarkStroke: Stroke,
    outlineStroke: Stroke,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    CheckboxImpl(
        state = ToggleableState(checked),
        onClick = wrapOnCheckedChange(checked, onCheckedChange),
        checkmarkStroke = checkmarkStroke,
        outlineStroke = outlineStroke,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/guidelines)
 *
 * Checkboxes can have a parent-child relationship with other checkboxes. When the parent checkbox
 * is checked, all child checkboxes are checked. If a parent checkbox is unchecked, all child
 * checkboxes are unchecked. If some, but not all, child checkboxes are checked, the parent checkbox
 * becomes an indeterminate checkbox.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/indeterminate-checkbox.png)
 *
 * @sample androidx.compose.material3.samples.TriStateCheckboxSample
 * @param state whether this checkbox is checked, unchecked, or in an indeterminate state
 * @param onClick called when this checkbox is clicked. If `null`, then this checkbox will not be
 *   interactable, unless something else handles its input events and updates its [state].
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [Checkbox] if you want a simple component that represents Boolean state
 */
@Composable
public fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    CheckboxImpl(
        state = state,
        onClick = onClick,
        checkmarkStroke = null,
        outlineStroke = null,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design checkbox](https://m3.material.io/components/checkbox/guidelines)
 *
 * Checkboxes can have a parent-child relationship with other checkboxes. When the parent checkbox
 * is checked, all child checkboxes are checked. If a parent checkbox is unchecked, all child
 * checkboxes are unchecked. If some, but not all, child checkboxes are checked, the parent checkbox
 * becomes an indeterminate checkbox.
 *
 * ![Checkbox
 * image](https://developer.android.com/images/reference/androidx/compose/material3/indeterminate-checkbox.png)
 *
 * This Checkbox function offers greater flexibility in visual customization. Using the [Stroke]
 * parameters, you can control the appearance of both the checkmark and the box that surrounds it.
 *
 * A sample of a `TriStateCheckbox` that uses a [Stroke] with rounded [StrokeCap] and
 * [androidx.compose.ui.graphics.StrokeJoin]:
 *
 * @sample androidx.compose.material3.samples.TriStateCheckboxRoundedStrokesSample
 * @param state whether this checkbox is checked, unchecked, or in an indeterminate state
 * @param onClick called when this checkbox is clicked. If `null`, then this checkbox will not be
 *   interactable, unless something else handles its input events and updates its [state].
 * @param checkmarkStroke stroke for the checkmark.
 * @param outlineStroke stroke for the checkmark's box outline. Note that this stroke is applied
 *   when drawing the outline's rounded rectangle, so attributions such as
 *   [androidx.compose.ui.graphics.StrokeJoin] will be ignored.
 * @param modifier the [Modifier] to be applied to this checkbox
 * @param enabled controls the enabled state of this checkbox. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [CheckboxColors] that will be used to resolve the colors used for this checkbox in
 *   different states. See [CheckboxDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this checkbox. You can use this to change the checkbox's appearance
 *   or preview the checkbox in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @see [Checkbox] if you want a simple component that represents Boolean state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    checkmarkStroke: Stroke,
    outlineStroke: Stroke,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    CheckboxImpl(
        enabled = enabled,
        state = state,
        modifier = modifier,
        colors = colors,
        checkmarkStroke = checkmarkStroke,
        outlineStroke = outlineStroke,
        onClick = onClick,
        interactionSource = interactionSource,
    )
}

// TODO (conradchen): add screenshot tests
// Note that we cannot name this function as Checkbox as it will cause overload resolution
// ambiguity. We will come back to this problem when we need to publish it.
@Composable
internal fun StyleableCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: CheckboxStyle? = null,
    interactionSource: MutableInteractionSource? = null,
) =
    StyleableTriStateCheckbox(
        state = ToggleableState(checked),
        onClick = wrapOnCheckedChange(checked, onCheckedChange),
        modifier = modifier,
        enabled = enabled,
        style = style,
        interactionSource = interactionSource,
    )

// TODO (conradchen): add screenshot tests
// Note that we cannot name this function as TriStateCheckbox as it will cause overload resolution
// ambiguity. We will come back to this problem when we need to publish it.
@Composable
internal fun StyleableTriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: CheckboxStyle? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val localTheme = LocalMaterialTheme.current
    val scope =
        CheckboxStyleScope(
            theme = localTheme,
            state =
                ComponentState.disabled(!enabled)
                    .checked(state == ToggleableState.On)
                    .indeterminate(state == ToggleableState.Indeterminate),
        )
    with(style ?: localTheme.componentProperties.checkboxProperties.style) { scope.applyStyle() }
    CheckboxImpl(
        enabled = enabled,
        state = state,
        modifier = modifier,
        checkmarkColor = scope.checkmarkColor,
        borderColor = scope.backgroundColor,
        boxColor = scope.backgroundColor,
        rippleColor = scope.rippleColor,
        checkmarkStroke = scope.checkmarkStroke,
        outlineStroke = scope.borderStroke,
        containerSize = CheckboxTokens.ContainerSize,
        padding = Dp.Unspecified,
        onClick = onClick,
        interactionSource = interactionSource,
    )
}

/** Defaults used in [Checkbox] and [TriStateCheckbox]. */
public object CheckboxDefaults {
    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     */
    @Composable
    public fun colors(): CheckboxColors = MaterialTheme.colorScheme.defaultCheckboxColors

    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     *
     * @param checkedColor the color that will be used for the border and box when checked
     * @param uncheckedColor color that will be used for the border when unchecked. By default, the
     *   inner box is transparent when unchecked.
     * @param checkmarkColor color that will be used for the checkmark when checked
     * @param disabledCheckedColor color that will be used for the box and border when disabled and
     *   checked
     * @param disabledUncheckedColor color that will be used for the border when disabled and
     *   unchecked. By default, the inner box is transparent when unchecked.
     * @param disabledIndeterminateColor color that will be used for the box and border in a
     *   [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state
     */
    @Composable
    public fun colors(
        checkedColor: Color = Color.Unspecified,
        uncheckedColor: Color = Color.Unspecified,
        checkmarkColor: Color = Color.Unspecified,
        disabledCheckedColor: Color = Color.Unspecified,
        disabledUncheckedColor: Color = Color.Unspecified,
        disabledIndeterminateColor: Color = Color.Unspecified,
    ): CheckboxColors =
        MaterialTheme.colorScheme.defaultCheckboxColors.copy(
            checkedCheckmarkColor = checkmarkColor,
            uncheckedCheckmarkColor = Color.Transparent,
            disabledCheckmarkColor = checkmarkColor,
            checkedBoxColor = checkedColor,
            uncheckedBoxColor = Color.Transparent,
            disabledCheckedBoxColor = disabledCheckedColor,
            disabledUncheckedBoxColor = Color.Transparent,
            disabledIndeterminateBoxColor = disabledIndeterminateColor,
            checkedBorderColor = checkedColor,
            uncheckedBorderColor = uncheckedColor,
            disabledBorderColor = disabledCheckedColor,
            disabledUncheckedBorderColor = disabledUncheckedColor,
            disabledIndeterminateBorderColor = disabledIndeterminateColor,
        )

    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     *
     * @param checkedCheckmarkColor color that will be used for the checkmark when checked
     * @param uncheckedCheckmarkColor color that will be used for the checkmark when unchecked
     * @param disabledCheckmarkColor color that will be used for the checkmark when disabled
     * @param checkedBoxColor the color that will be used for the box when checked
     * @param uncheckedBoxColor color that will be used for the box when unchecked
     * @param disabledCheckedBoxColor color that will be used for the box when disabled and checked
     * @param disabledUncheckedBoxColor color that will be used for the box when disabled and
     *   unchecked
     * @param disabledIndeterminateBoxColor color that will be used for the box and border in a
     *   [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state.
     * @param checkedBorderColor color that will be used for the border when checked
     * @param uncheckedBorderColor color that will be used for the border when unchecked
     * @param disabledBorderColor color that will be used for the border when disabled and checked
     * @param disabledUncheckedBorderColor color that will be used for the border when disabled and
     *   unchecked
     * @param disabledIndeterminateBorderColor color that will be used for the border when disabled
     *   and in an [ToggleableState.Indeterminate] state.
     */
    @Composable
    public fun colors(
        checkedCheckmarkColor: Color = Color.Unspecified,
        uncheckedCheckmarkColor: Color = Color.Unspecified,
        disabledCheckmarkColor: Color = Color.Unspecified,
        checkedBoxColor: Color = Color.Unspecified,
        uncheckedBoxColor: Color = Color.Unspecified,
        disabledCheckedBoxColor: Color = Color.Unspecified,
        disabledUncheckedBoxColor: Color = Color.Unspecified,
        disabledIndeterminateBoxColor: Color = Color.Unspecified,
        checkedBorderColor: Color = Color.Unspecified,
        uncheckedBorderColor: Color = Color.Unspecified,
        disabledBorderColor: Color = Color.Unspecified,
        disabledUncheckedBorderColor: Color = Color.Unspecified,
        disabledIndeterminateBorderColor: Color = Color.Unspecified,
    ): CheckboxColors =
        MaterialTheme.colorScheme.defaultCheckboxColors.copy(
            checkedCheckmarkColor = checkedCheckmarkColor,
            uncheckedCheckmarkColor = uncheckedCheckmarkColor,
            disabledCheckmarkColor = disabledCheckmarkColor,
            checkedBoxColor = checkedBoxColor,
            uncheckedBoxColor = uncheckedBoxColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor,
            disabledIndeterminateBoxColor = disabledIndeterminateBoxColor,
            checkedBorderColor = checkedBorderColor,
            uncheckedBorderColor = uncheckedBorderColor,
            disabledBorderColor = disabledBorderColor,
            disabledUncheckedBorderColor = disabledUncheckedBorderColor,
            disabledIndeterminateBorderColor = disabledIndeterminateBorderColor,
        )

    internal val ColorScheme.defaultCheckboxColors: CheckboxColors
        get() {
            return defaultCheckboxColorsCached
                ?: CheckboxColors(
                        checkedCheckmarkColor = fromToken(CheckboxTokens.SelectedIconColor),
                        uncheckedCheckmarkColor = Color.Transparent,
                        disabledCheckmarkColor =
                            fromToken(CheckboxTokens.SelectedDisabledIconColor),
                        checkedBoxColor = fromToken(CheckboxTokens.SelectedContainerColor),
                        uncheckedBoxColor = Color.Transparent,
                        disabledCheckedBoxColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                        disabledUncheckedBoxColor = Color.Transparent,
                        disabledIndeterminateBoxColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                        checkedBorderColor = fromToken(CheckboxTokens.SelectedContainerColor),
                        uncheckedBorderColor = fromToken(CheckboxTokens.UnselectedOutlineColor),
                        disabledBorderColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                        disabledUncheckedBorderColor =
                            fromToken(CheckboxTokens.UnselectedDisabledOutlineColor)
                                .copy(alpha = CheckboxTokens.UnselectedDisabledContainerOpacity),
                        disabledIndeterminateBorderColor =
                            fromToken(CheckboxTokens.SelectedDisabledContainerColor)
                                .copy(alpha = CheckboxTokens.SelectedDisabledContainerOpacity),
                    )
                    .also { defaultCheckboxColorsCached = it }
        }

    /**
     * The default stroke width for a [Checkbox]. This width will be used for the checkmark when the
     * `Checkbox` is in a checked or indeterminate states, or for the outline when it's unchecked.
     */
    public val StrokeWidth: Dp = 2.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckboxImpl(
    modifier: Modifier,
    enabled: Boolean,
    state: ToggleableState,
    colors: CheckboxColors,
    checkmarkStroke: Stroke?,
    outlineStroke: Stroke?,
    onClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
) {
    val isCheckboxStylingFixEnabled = ComposeMaterial3Flags.isCheckboxStylingFixEnabled
    CheckboxImpl(
        enabled = enabled,
        state = state,
        modifier = modifier,
        checkmarkColor =
            if (isCheckboxStylingFixEnabled) {
                colors.checkmarkColor(enabled, state)
            } else {
                colors.checkmarkColor(state)
            },
        boxColor = colors.boxColor(enabled, state),
        borderColor = colors.borderColor(enabled, state),
        rippleColor =
            if (isCheckboxStylingFixEnabled) {
                colors.indicatorColor(state)
            } else {
                Color.Unspecified
            },
        checkmarkStroke = checkmarkStroke,
        outlineStroke = outlineStroke,
        containerSize =
            if (isCheckboxStylingFixEnabled) {
                CheckboxTokens.ContainerSize
            } else {
                CheckboxSize
            },
        padding =
            if (isCheckboxStylingFixEnabled) {
                Dp.Unspecified
            } else {
                CheckboxDefaultPadding
            },
        onClick = onClick,
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckboxImpl(
    enabled: Boolean,
    state: ToggleableState,
    modifier: Modifier,
    checkmarkColor: Color,
    boxColor: Color,
    borderColor: Color,
    rippleColor: Color,
    checkmarkStroke: Stroke?,
    outlineStroke: Stroke?,
    containerSize: Dp,
    padding: Dp,
    onClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
) {
    val indication =
        ripple(
            bounded = false,
            radius = CheckboxTokens.StateLayerSize / 2,
            color = rippleColor,
            focusRingShape = RoundedCornerShape(25),
        )

    val canvasModifier =
        modifier
            .then(
                if (onClick != null) {
                    Modifier.minimumInteractiveComponentSize()
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.triStateToggleable(
                        state = state,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Checkbox,
                        interactionSource = interactionSource,
                        indication = indication,
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (padding == Dp.Unspecified) {
                    Modifier
                } else {
                    Modifier.padding(padding)
                }
            )
            .wrapContentSize(Alignment.Center)
            .requiredSize(containerSize)

    val transition = updateTransition(state)
    val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
    val checkDrawFraction =
        transition.animateFloat(
            transitionSpec = {
                when {
                    // TODO Load the motionScheme tokens from the component tokens file
                    initialState == ToggleableState.Off -> defaultAnimationSpec
                    targetState == ToggleableState.Off -> snap(delayMillis = SnapAnimationDelay)
                    else -> defaultAnimationSpec
                }
            }
        ) {
            when (it) {
                ToggleableState.On -> 1f
                ToggleableState.Off -> 0f
                ToggleableState.Indeterminate -> 1f
            }
        }

    val checkCenterGravitationShiftFraction =
        transition.animateFloat(
            transitionSpec = {
                when {
                    // TODO Load the motionScheme tokens from the component tokens file
                    initialState == ToggleableState.Off -> snap()
                    targetState == ToggleableState.Off -> snap(delayMillis = SnapAnimationDelay)
                    else -> defaultAnimationSpec
                }
            }
        ) {
            when (it) {
                ToggleableState.On -> 0f
                ToggleableState.Off -> 0f
                ToggleableState.Indeterminate -> 1f
            }
        }
    val checkCache = remember { CheckDrawingCache() }
    val colorAnimationSpec = colorAnimationSpecForState(state)
    val animatedCheckboxColor by animateColorAsState(checkmarkColor, colorAnimationSpec)
    val animatedBoxColor by
        if (enabled) {
            animateColorAsState(boxColor, colorAnimationSpec)
        } else {
            // If not enabled 'snap' to the disabled state, as there should be no animations between
            // enabled / disabled.
            rememberUpdatedState(boxColor)
        }
    val animatedBorderColor by
        if (enabled) {
            animateColorAsState(borderColor, colorAnimationSpec)
        } else {
            // If not enabled 'snap' to the disabled state, as there should be no animations between
            // enabled / disabled.
            rememberUpdatedState(borderColor)
        }
    val strokeWidthPx =
        if (outlineStroke == null || checkmarkStroke == null) {
            with(LocalDensity.current) { floor(CheckboxDefaults.StrokeWidth.toPx()) }
        } else {
            0f
        }

    Canvas(canvasModifier) {
        drawBox(
            boxColor = animatedBoxColor,
            borderColor = animatedBorderColor,
            radius = RadiusSize.toPx(),
            stroke = outlineStroke ?: Stroke(width = strokeWidthPx),
        )
        drawCheck(
            checkColor = animatedCheckboxColor,
            checkFraction = checkDrawFraction.value,
            crossCenterGravitation = checkCenterGravitationShiftFraction.value,
            stroke = checkmarkStroke ?: Stroke(width = strokeWidthPx, cap = StrokeCap.Square),
            drawingCache = checkCache,
        )
    }
}

private fun DrawScope.drawBox(boxColor: Color, borderColor: Color, radius: Float, stroke: Stroke) {
    val halfStrokeWidth = stroke.width / 2.0f
    val checkboxSize = size.width
    if (boxColor == borderColor) {
        drawRoundRect(
            boxColor,
            size = Size(checkboxSize, checkboxSize),
            cornerRadius = CornerRadius(radius),
            style = Fill,
        )
    } else {
        drawRoundRect(
            boxColor,
            topLeft = Offset(stroke.width, stroke.width),
            size = Size(checkboxSize - stroke.width * 2, checkboxSize - stroke.width * 2),
            cornerRadius = CornerRadius(max(0f, radius - stroke.width)),
            style = Fill,
        )
        drawRoundRect(
            borderColor,
            topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
            size = Size(checkboxSize - stroke.width, checkboxSize - stroke.width),
            cornerRadius = CornerRadius(radius - halfStrokeWidth),
            style = stroke,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun DrawScope.drawCheck(
    checkColor: Color,
    checkFraction: Float,
    crossCenterGravitation: Float,
    stroke: Stroke,
    drawingCache: CheckDrawingCache,
) {
    val isCheckboxStylingFixEnabled = ComposeMaterial3Flags.isCheckboxStylingFixEnabled
    val width = size.width
    val checkCrossX = 0.4f
    val checkCrossY = if (isCheckboxStylingFixEnabled) 0.65f else 0.7f
    val leftX = if (isCheckboxStylingFixEnabled) 0.25f else 0.2f
    val leftY = 0.5f
    val rightX = if (isCheckboxStylingFixEnabled) 0.75f else 0.8f
    val rightY = 0.3f

    val gravitatedCrossX = lerp(checkCrossX, 0.5f, crossCenterGravitation)
    val gravitatedCrossY = lerp(checkCrossY, 0.5f, crossCenterGravitation)
    // gravitate only Y for end to achieve center line
    val gravitatedLeftY = lerp(leftY, 0.5f, crossCenterGravitation)
    val gravitatedRightY = lerp(rightY, 0.5f, crossCenterGravitation)

    with(drawingCache) {
        checkPath.rewind()
        checkPath.moveTo(width * leftX, width * gravitatedLeftY)
        checkPath.lineTo(width * gravitatedCrossX, width * gravitatedCrossY)
        checkPath.lineTo(width * rightX, width * gravitatedRightY)
        // TODO: replace with proper declarative non-android alternative when ready (b/158188351)
        pathMeasure.setPath(checkPath, false)
        pathToDraw.rewind()
        pathMeasure.getSegment(0f, pathMeasure.length * checkFraction, pathToDraw, true)
    }
    drawPath(drawingCache.pathToDraw, checkColor, style = stroke)
}

@Immutable
private class CheckDrawingCache(
    val checkPath: Path = Path(),
    val pathMeasure: PathMeasure = PathMeasure(),
    val pathToDraw: Path = Path(),
)

/**
 * Represents the colors used by the three different sections (checkmark, box, and border) of a
 * [Checkbox] or [TriStateCheckbox] in different states.
 *
 * @param checkedCheckmarkColor color that will be used for the checkmark when checked
 * @param uncheckedCheckmarkColor color that will be used for the checkmark when unchecked
 * @param checkedBoxColor the color that will be used for the box when checked
 * @param uncheckedBoxColor color that will be used for the box when unchecked
 * @param disabledCheckedBoxColor color that will be used for the box when disabled and checked
 * @param disabledUncheckedBoxColor color that will be used for the box when disabled and unchecked
 * @param disabledIndeterminateBoxColor color that will be used for the box and border in a
 *   [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state.
 * @param checkedBorderColor color that will be used for the border when checked
 * @param uncheckedBorderColor color that will be used for the border when unchecked
 * @param disabledBorderColor color that will be used for the border when disabled and checked
 * @param disabledUncheckedBorderColor color that will be used for the border when disabled and
 *   unchecked
 * @param disabledIndeterminateBorderColor color that will be used for the border when disabled and
 *   in an [ToggleableState.Indeterminate] state.
 * @param disabledCheckmarkColor color that will be used for the checkmark when disabled
 * @constructor create an instance with arbitrary colors, see [CheckboxDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
public class CheckboxColors
public constructor(
    public val checkedCheckmarkColor: Color,
    public val uncheckedCheckmarkColor: Color,
    public val checkedBoxColor: Color,
    public val uncheckedBoxColor: Color,
    public val disabledCheckedBoxColor: Color,
    public val disabledUncheckedBoxColor: Color,
    public val disabledIndeterminateBoxColor: Color,
    public val checkedBorderColor: Color,
    public val uncheckedBorderColor: Color,
    public val disabledBorderColor: Color,
    public val disabledUncheckedBorderColor: Color,
    public val disabledIndeterminateBorderColor: Color,
    public val disabledCheckmarkColor: Color,
) {
    @Deprecated(
        message =
            "This constructor is deprecated. Use the primary constructor that includes 'disabledCheckmarkColor'",
        level = DeprecationLevel.WARNING,
    )
    public constructor(
        checkedCheckmarkColor: Color,
        uncheckedCheckmarkColor: Color,
        checkedBoxColor: Color,
        uncheckedBoxColor: Color,
        disabledCheckedBoxColor: Color,
        disabledUncheckedBoxColor: Color,
        disabledIndeterminateBoxColor: Color,
        checkedBorderColor: Color,
        uncheckedBorderColor: Color,
        disabledBorderColor: Color,
        disabledUncheckedBorderColor: Color,
        disabledIndeterminateBorderColor: Color,
    ) : this(
        checkedCheckmarkColor = checkedCheckmarkColor,
        uncheckedCheckmarkColor = uncheckedCheckmarkColor,
        checkedBoxColor = checkedBoxColor,
        uncheckedBoxColor = uncheckedBoxColor,
        disabledCheckedBoxColor = disabledCheckedBoxColor,
        disabledUncheckedBoxColor = disabledUncheckedBoxColor,
        disabledIndeterminateBoxColor = disabledIndeterminateBoxColor,
        checkedBorderColor = checkedBorderColor,
        uncheckedBorderColor = uncheckedBorderColor,
        disabledBorderColor = disabledBorderColor,
        disabledUncheckedBorderColor = disabledUncheckedBorderColor,
        disabledIndeterminateBorderColor = disabledIndeterminateBorderColor,
        disabledCheckmarkColor = checkedCheckmarkColor,
    )

    /**
     * Returns a copy of this CheckboxColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    @Deprecated(
        message =
            "This function is deprecated. Use 'copy' that includes 'disabledCheckmarkColor' instead",
        level = DeprecationLevel.HIDDEN,
    )
    public fun copy(
        checkedCheckmarkColor: Color = this.checkedCheckmarkColor,
        uncheckedCheckmarkColor: Color = this.uncheckedCheckmarkColor,
        checkedBoxColor: Color = this.checkedBoxColor,
        uncheckedBoxColor: Color = this.uncheckedBoxColor,
        disabledCheckedBoxColor: Color = this.disabledCheckedBoxColor,
        disabledUncheckedBoxColor: Color = this.disabledUncheckedBoxColor,
        disabledIndeterminateBoxColor: Color = this.disabledIndeterminateBoxColor,
        checkedBorderColor: Color = this.checkedBorderColor,
        uncheckedBorderColor: Color = this.uncheckedBorderColor,
        disabledBorderColor: Color = this.disabledBorderColor,
        disabledUncheckedBorderColor: Color = this.disabledUncheckedBorderColor,
        disabledIndeterminateBorderColor: Color = this.disabledIndeterminateBorderColor,
    ): CheckboxColors =
        CheckboxColors(
            checkedCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
            uncheckedCheckmarkColor =
                uncheckedCheckmarkColor.takeOrElse { this.uncheckedCheckmarkColor },
            checkedBoxColor = checkedBoxColor.takeOrElse { this.checkedBoxColor },
            uncheckedBoxColor = uncheckedBoxColor.takeOrElse { this.uncheckedBoxColor },
            disabledCheckedBoxColor =
                disabledCheckedBoxColor.takeOrElse { this.disabledCheckedBoxColor },
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor.takeOrElse { this.disabledUncheckedBoxColor },
            disabledIndeterminateBoxColor =
                disabledIndeterminateBoxColor.takeOrElse { this.disabledIndeterminateBoxColor },
            checkedBorderColor = checkedBorderColor.takeOrElse { this.checkedBorderColor },
            uncheckedBorderColor = uncheckedBorderColor.takeOrElse { this.uncheckedBorderColor },
            disabledBorderColor = disabledBorderColor.takeOrElse { this.disabledBorderColor },
            disabledUncheckedBorderColor =
                disabledUncheckedBorderColor.takeOrElse { this.disabledUncheckedBorderColor },
            disabledIndeterminateBorderColor =
                disabledIndeterminateBorderColor.takeOrElse {
                    this.disabledIndeterminateBorderColor
                },
            disabledCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
        )

    /**
     * Returns a copy of this CheckboxColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    public fun copy(
        checkedCheckmarkColor: Color = this.checkedCheckmarkColor,
        uncheckedCheckmarkColor: Color = this.uncheckedCheckmarkColor,
        checkedBoxColor: Color = this.checkedBoxColor,
        uncheckedBoxColor: Color = this.uncheckedBoxColor,
        disabledCheckedBoxColor: Color = this.disabledCheckedBoxColor,
        disabledUncheckedBoxColor: Color = this.disabledUncheckedBoxColor,
        disabledIndeterminateBoxColor: Color = this.disabledIndeterminateBoxColor,
        checkedBorderColor: Color = this.checkedBorderColor,
        uncheckedBorderColor: Color = this.uncheckedBorderColor,
        disabledBorderColor: Color = this.disabledBorderColor,
        disabledUncheckedBorderColor: Color = this.disabledUncheckedBorderColor,
        disabledIndeterminateBorderColor: Color = this.disabledIndeterminateBorderColor,
        disabledCheckmarkColor: Color = this.disabledCheckmarkColor,
    ): CheckboxColors =
        CheckboxColors(
            checkedCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
            uncheckedCheckmarkColor =
                uncheckedCheckmarkColor.takeOrElse { this.uncheckedCheckmarkColor },
            checkedBoxColor = checkedBoxColor.takeOrElse { this.checkedBoxColor },
            uncheckedBoxColor = uncheckedBoxColor.takeOrElse { this.uncheckedBoxColor },
            disabledCheckedBoxColor =
                disabledCheckedBoxColor.takeOrElse { this.disabledCheckedBoxColor },
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor.takeOrElse { this.disabledUncheckedBoxColor },
            disabledIndeterminateBoxColor =
                disabledIndeterminateBoxColor.takeOrElse { this.disabledIndeterminateBoxColor },
            checkedBorderColor = checkedBorderColor.takeOrElse { this.checkedBorderColor },
            uncheckedBorderColor = uncheckedBorderColor.takeOrElse { this.uncheckedBorderColor },
            disabledBorderColor = disabledBorderColor.takeOrElse { this.disabledBorderColor },
            disabledUncheckedBorderColor =
                disabledUncheckedBorderColor.takeOrElse { this.disabledUncheckedBorderColor },
            disabledIndeterminateBorderColor =
                disabledIndeterminateBorderColor.takeOrElse {
                    this.disabledIndeterminateBorderColor
                },
            disabledCheckmarkColor =
                disabledCheckmarkColor.takeOrElse { this.disabledCheckmarkColor },
        )

    /**
     * Represents the color used for the checkbox container's background indication, depending on
     * [state].
     *
     * @param state the [ToggleableState] of the checkbox
     */
    internal fun indicatorColor(state: ToggleableState): Color =
        if (state == ToggleableState.Off) {
            uncheckedBoxColor
        } else {
            checkedBoxColor
        }

    /**
     * Represents the color used for the checkmark inside the checkbox, depending on [enabled] and
     * [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    internal fun checkmarkColor(enabled: Boolean, state: ToggleableState): Color =
        if (enabled) {
            if (state == ToggleableState.Off) {
                uncheckedCheckmarkColor
            } else {
                checkedCheckmarkColor
            }
        } else {
            disabledCheckmarkColor
        }

    /**
     * Represents the color used for the checkmark inside the checkbox, depending on [state].
     *
     * @param state the [ToggleableState] of the checkbox
     */
    internal fun checkmarkColor(state: ToggleableState): Color {
        return if (state == ToggleableState.Off) {
            uncheckedCheckmarkColor
        } else {
            checkedCheckmarkColor
        }
    }

    /**
     * Represents the color used for the box (background) of the checkbox, depending on [enabled]
     * and [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    internal fun boxColor(enabled: Boolean, state: ToggleableState): Color =
        if (enabled) {
            when (state) {
                ToggleableState.On,
                ToggleableState.Indeterminate -> checkedBoxColor
                ToggleableState.Off -> uncheckedBoxColor
            }
        } else {
            when (state) {
                ToggleableState.On -> disabledCheckedBoxColor
                ToggleableState.Indeterminate -> disabledIndeterminateBoxColor
                ToggleableState.Off -> disabledUncheckedBoxColor
            }
        }

    /**
     * Represents the color used for the border of the checkbox, depending on [enabled] and [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    internal fun borderColor(enabled: Boolean, state: ToggleableState): Color =
        if (enabled) {
            when (state) {
                ToggleableState.On,
                ToggleableState.Indeterminate -> checkedBorderColor
                ToggleableState.Off -> uncheckedBorderColor
            }
        } else {
            when (state) {
                ToggleableState.Indeterminate -> disabledIndeterminateBorderColor
                ToggleableState.On -> disabledBorderColor
                ToggleableState.Off -> disabledUncheckedBorderColor
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CheckboxColors) return false

        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedCheckmarkColor != other.uncheckedCheckmarkColor) return false
        if (disabledCheckmarkColor != other.disabledCheckmarkColor) return false
        if (checkedBoxColor != other.checkedBoxColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false
        if (disabledIndeterminateBoxColor != other.disabledIndeterminateBoxColor) return false
        if (checkedBorderColor != other.checkedBorderColor) return false
        if (uncheckedBorderColor != other.uncheckedBorderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (disabledUncheckedBorderColor != other.disabledUncheckedBorderColor) return false
        if (disabledIndeterminateBorderColor != other.disabledIndeterminateBorderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledCheckmarkColor.hashCode()
        result = 31 * result + checkedBoxColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        result = 31 * result + disabledIndeterminateBoxColor.hashCode()
        result = 31 * result + checkedBorderColor.hashCode()
        result = 31 * result + uncheckedBorderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + disabledUncheckedBorderColor.hashCode()
        result = 31 * result + disabledIndeterminateBorderColor.hashCode()
        return result
    }
}

/** Returns the color [AnimationSpec] for the given state. */
@Composable
private fun colorAnimationSpecForState(state: ToggleableState): AnimationSpec<Color> {
    // TODO Load the motionScheme tokens from the component tokens file
    return if (state == ToggleableState.Off) {
        // Box out
        MotionSchemeKeyTokens.FastEffects.value()
    } else {
        // Box in
        MotionSchemeKeyTokens.DefaultEffects.value()
    }
}

private fun wrapOnCheckedChange(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
): (() -> Unit)? =
    if (onCheckedChange != null) {
        { onCheckedChange(!checked) }
    } else {
        null
    }

private const val SnapAnimationDelay = 100

// TODO(b/188529841): Update the padding and size when the Checkbox spec is finalized.
private val CheckboxDefaultPadding
    get() = 2.dp
private val CheckboxSize
    get() = 20.dp
private val RadiusSize
    get() = 2.dp
