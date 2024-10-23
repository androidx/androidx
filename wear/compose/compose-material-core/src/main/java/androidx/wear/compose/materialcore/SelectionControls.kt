/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * [Checkbox] provides an animated checkbox for use in material APIs.
 *
 * @param checked Boolean flag indicating whether this checkbox is currently checked.
 * @param modifier Modifier to be applied to the checkbox. This can be used to provide a content
 *   description for accessibility.
 * @param boxColor Composable lambda from which the box color will be obtained.
 * @param checkmarkColor Composable lambda from which the check mark color will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [Checkbox] (affects the color).
 * @param onCheckedChange Callback to be invoked when Checkbox is clicked. If null, then this is
 *   passive and relies entirely on a higher-level component to control the state.
 * @param interactionSource When also providing [onCheckedChange], the [MutableInteractionSource]
 *   representing the stream of [Interaction]s for the "toggleable" tap area - can be used to
 *   customise the appearance / behavior of the Checkbox.
 * @param progressAnimationSpec Animation spec to animate the progress.
 * @param drawBox Draws the checkbox.
 * @param width Width of the checkbox.
 * @param height Height of the checkbox.
 * @param ripple Ripple used for the checkbox.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Checkbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    boxColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    checkmarkColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    enabled: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    interactionSource: MutableInteractionSource?,
    progressAnimationSpec: FiniteAnimationSpec<Float>,
    drawBox: FunctionDrawBox,
    width: Dp,
    height: Dp,
    ripple: Indication
) {
    val targetState = if (checked) SelectionStage.Checked else SelectionStage.Unchecked
    val transition = updateTransition(targetState, label = "checkboxTransition")
    val progress =
        animateProgress(
            transition = transition,
            label = "Checkbox",
            animationSpec = progressAnimationSpec
        )
    val isRtl = isLayoutDirectionRtl()
    val startXOffset = if (isRtl) 0.dp else width - height

    // For Checkbox, the color and alpha animations have the same duration and easing,
    // so we don't need to explicitly animate alpha.
    val boxColorState = boxColor(enabled, checked)
    val checkmarkColorState = checkmarkColor(enabled, checked)

    // Canvas internally uses Spacer.drawBehind.
    // Using Spacer.drawWithCache to optimize the stroke allocations.
    Spacer(
        modifier =
            modifier
                .semantics { this.role = Role.Checkbox }
                .maybeToggleable(
                    onCheckedChange,
                    enabled,
                    checked,
                    interactionSource,
                    ripple,
                    width,
                    height
                )
                .drawWithCache {
                    onDrawWithContent {
                        drawBox(this, boxColorState.value, progress.value, isRtl)
                        animateTick(
                            enabled = enabled,
                            checked = checked,
                            tickColor = checkmarkColorState.value,
                            tickProgress = progress.value,
                            startXOffset = startXOffset
                        )
                    }
                }
    )
}

/**
 * [Switch] provides an animated switch for use in material APIs.
 *
 * @param modifier Modifier to be applied to the switch. This can be used to provide a content
 *   description for accessibility.
 * @param checked Boolean flag indicating whether this switch is currently toggled on.
 * @param enabled Boolean flag indicating the enabled state of the [Switch] (affects the color).
 * @param onCheckedChange Callback to be invoked when Switch is clicked. If null, then this is
 *   passive and relies entirely on a higher-level component to control the state.
 * @param interactionSource When also providing [onCheckedChange], the [MutableInteractionSource]
 *   representing the stream of [Interaction]s for the "toggleable" tap area - can be used to
 *   customise the appearance / behavior of the Switch.
 * @param trackFillColor Composable lambda from which the fill color of the track will be obtained.
 * @param trackStrokeColor Composable lambda from which the stroke color of the track will be
 *   obtained.
 * @param thumbColor Composable lambda from which the thumb color will be obtained.
 * @param thumbIconColor Composable lambda from which the icon color will be obtained.
 * @param trackWidth Width of the track.
 * @param trackHeight Height of the track.
 * @param drawThumb Lambda function to draw the thumb of the switch. The lambda is invoked with
 *   trackFillColor as the icon color, along with the thumbColor, and the progress.
 * @param progressAnimationSpec Animation spec to animate the progress.
 * @param width Width of the switch.
 * @param height Height of the switch.
 * @param ripple Ripple used for the switch.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Switch(
    modifier: Modifier,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    interactionSource: MutableInteractionSource?,
    trackFillColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    trackStrokeColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    thumbColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    thumbIconColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    trackWidth: Dp,
    trackHeight: Dp,
    drawThumb: FunctionDrawThumb,
    progressAnimationSpec: TweenSpec<Float>,
    width: Dp,
    height: Dp,
    ripple: Indication
) {
    val targetState = if (checked) SelectionStage.Checked else SelectionStage.Unchecked
    val transition = updateTransition(targetState, label = "switchTransition")
    val isRtl = isLayoutDirectionRtl()

    val thumbProgress = animateProgress(transition, "Switch", progressAnimationSpec)

    val thumbBackgroundColor = thumbColor(enabled, checked)
    val iconColor = thumbIconColor(enabled, checked)
    val trackBackgroundFillColor = trackFillColor(enabled, checked)
    val trackBackgroundStrokeColor = trackStrokeColor(enabled, checked)

    // Canvas internally uses Spacer.drawBehind.
    // Using Spacer.drawWithCache to optimize the stroke allocations.
    Spacer(
        modifier =
            modifier
                .semantics { this.role = Role.Switch }
                .maybeToggleable(
                    onCheckedChange,
                    enabled,
                    checked,
                    interactionSource,
                    ripple,
                    width,
                    height
                )
                .drawWithCache {
                    onDrawWithContent {
                        drawTrack(
                            fillColor = trackBackgroundFillColor.value,
                            strokeColor = trackBackgroundStrokeColor.value,
                            trackWidthPx = trackWidth.toPx(),
                            trackHeightPx = trackHeight.toPx()
                        )

                        // Draw the thumb of the switch.
                        drawThumb(
                            this,
                            thumbBackgroundColor.value,
                            thumbProgress.value,
                            iconColor.value,
                            isRtl
                        )
                    }
                }
    )
}

/**
 * [RadioButton] provides an animated radio button for use in material APIs.
 *
 * @param modifier Modifier to be applied to the radio button. This can be used to provide a content
 *   description for accessibility.
 * @param selected Boolean flag indicating whether this radio button is currently toggled on.
 * @param enabled Boolean flag indicating the enabled state of the [RadioButton] (affects the
 *   color).
 * @param ringColor Composable lambda from which the ring color of the radio button will be
 *   obtained.
 * @param dotColor Composable lambda from which the dot color of the radio button will be obtained.
 * @param onClick Callback to be invoked when RadioButton is clicked. If null, then this is passive
 *   and relies entirely on a higher-level component to control the state.
 * @param interactionSource When also providing [onClick], the [MutableInteractionSource]
 *   representing the stream of [Interaction]s for the "toggleable" tap area - can be used to
 *   customise the appearance / behavior of the RadioButton.
 * @param dotRadiusProgressDuration Duration of the dot radius progress animation.
 * @param dotAlphaProgressDuration Duration of the dot alpha progress animation.
 * @param dotAlphaProgressDelay Delay for the dot alpha progress animation.
 * @param easing Animation spec to animate the progress.
 * @param width Width of the radio button.
 * @param height Height of the radio button.
 * @param ripple Ripple used for the radio button.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun RadioButton(
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    ringColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    dotColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    onClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    dotRadiusProgressDuration: FunctionDotRadiusProgressDuration,
    dotAlphaProgressDuration: Int,
    dotAlphaProgressDelay: Int,
    easing: CubicBezierEasing,
    width: Dp,
    height: Dp,
    ripple: Indication
) =
    RadioButton(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        ringColor = ringColor,
        dotColor = dotColor,
        onClick = onClick,
        interactionSource = interactionSource,
        dotRadiusAnimationSpec = tween(dotRadiusProgressDuration(selected), 0, easing),
        dotAlphaAnimationSpec = tween(dotAlphaProgressDuration, dotAlphaProgressDelay, easing),
        width = width,
        height = height,
        ripple = ripple
    )

/**
 * [RadioButton] provides an animated radio button for use in material APIs.
 *
 * @param modifier Modifier to be applied to the radio button. This can be used to provide a content
 *   description for accessibility.
 * @param selected Boolean flag indicating whether this radio button is currently toggled on.
 * @param enabled Boolean flag indicating the enabled state of the [RadioButton] (affects the
 *   color).
 * @param ringColor Composable lambda from which the ring color of the radio button will be
 *   obtained.
 * @param dotColor Composable lambda from which the dot color of the radio button will be obtained.
 * @param onClick Callback to be invoked when RadioButton is clicked. If null, then this is passive
 *   and relies entirely on a higher-level component to control the state.
 * @param interactionSource When also providing [onClick], the [MutableInteractionSource]
 *   representing the stream of [Interaction]s for the "toggleable" tap area - can be used to
 *   customise the appearance / behavior of the RadioButton.
 * @param dotRadiusAnimationSpec Animation spec of the dot radius progress animation.
 * @param dotAlphaAnimationSpec Animation spec of the dot alpha progress animation.
 * @param width Width of the radio button.
 * @param height Height of the radio button.
 * @param ripple Ripple used for the radio button.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun RadioButton(
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    ringColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    dotColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    onClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    dotRadiusAnimationSpec: FiniteAnimationSpec<Float>,
    dotAlphaAnimationSpec: FiniteAnimationSpec<Float>,
    width: Dp,
    height: Dp,
    ripple: Indication
) {
    val targetState = if (selected) SelectionStage.Checked else SelectionStage.Unchecked
    val transition = updateTransition(targetState)
    val isRtl = isLayoutDirectionRtl()

    val radioRingColor = ringColor(enabled, selected)
    val radioDotColor = dotColor(enabled, selected)

    val dotRadiusProgress =
        animateProgress(
            transition = transition,
            label = "dot-radius",
            animationSpec = dotRadiusAnimationSpec
        )
    // Animation of the dot alpha only happens when toggling On to Off.
    val dotAlphaProgress =
        if (targetState == SelectionStage.Unchecked)
            animateProgress(
                transition = transition,
                label = "dot-alpha",
                animationSpec = dotAlphaAnimationSpec
            )
        else null

    // Canvas internally uses Spacer.drawBehind.
    // Using Spacer.drawWithCache to optimize the stroke allocations.
    Spacer(
        // NB We must set the semantic role to Role.RadioButton in the parent Button,
        // not here in the selection control - see b/330869742
        modifier =
            modifier
                .maybeSelectable(
                    onClick,
                    enabled,
                    selected,
                    interactionSource,
                    ripple,
                    width,
                    height
                )
                .drawWithCache {
                    // Aligning the radio to the end.
                    val startXOffsetPx =
                        if (isRtl) -(width - height).toPx() / 2 else (width - height).toPx() / 2
                    // Outer circle has a constant radius.
                    onDrawWithContent {
                        val circleCenter = Offset(center.x + startXOffsetPx, center.y)
                        drawCircle(
                            radius = RADIO_CIRCLE_RADIUS.toPx(),
                            color = radioRingColor.value,
                            center = circleCenter,
                            style = Stroke(RADIO_CIRCLE_STROKE.toPx()),
                        )
                        // Inner dot radius expands/shrinks.
                        drawCircle(
                            radius = dotRadiusProgress.value * RADIO_DOT_RADIUS.toPx(),
                            color =
                                radioDotColor.value.copy(
                                    alpha =
                                        (dotAlphaProgress?.value ?: 1f) * radioDotColor.value.alpha
                                ),
                            center = circleCenter,
                            style = Fill,
                        )
                    }
                }
    )
}

/**
 * Returns the color for the selectionControl.
 *
 * @param enabled Boolean flag checking if the selection control is enabled.
 * @param checked Boolean flag checking if the selection control is checked [SelectionStage].
 * @param checkedColor Color for selection control when [enabled] = true and [checked] = true.
 * @param uncheckedColor Color for selection control when [enabled] = true and [checked] = false.
 * @param disabledCheckedColor Color for selection control when [enabled] = false and [checked] =
 *   true.
 * @param disabledUncheckedColor Color for selection control when [enabled] = false and [checked] =
 *   false.
 * @param animationSpec AnimationSpec for the color transition animations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun animateSelectionColor(
    enabled: Boolean,
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    disabledCheckedColor: Color,
    disabledUncheckedColor: Color,
    animationSpec: AnimationSpec<Color>
): State<Color> =
    animateColorAsState(
        targetValue =
            if (enabled) {
                if (checked) checkedColor else uncheckedColor
            } else {
                if (checked) disabledCheckedColor else disabledUncheckedColor
            },
        animationSpec = animationSpec
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class SelectionStage {
    Unchecked,
    Checked
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FunctionDrawBox {
    operator fun invoke(drawScope: DrawScope, color: Color, progress: Float, isRtl: Boolean)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FunctionDrawThumb {
    operator fun invoke(
        drawScope: DrawScope,
        thumbColor: Color,
        progress: Float,
        thumbIconColor: Color,
        isRtl: Boolean
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FunctionDotRadiusProgressDuration {
    operator fun invoke(selected: Boolean): Int
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun DrawScope.animateTick(
    enabled: Boolean,
    checked: Boolean,
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
) {
    val targetState = if (checked) SelectionStage.Checked else SelectionStage.Unchecked
    if (targetState == SelectionStage.Checked) {
        // Passing startXOffset as we want checkbox to be aligned to the end of the canvas.
        drawTick(tickColor, tickProgress, startXOffset, enabled)
    } else {
        // Passing startXOffset as we want checkbox to be aligned to the end of the canvas.
        eraseTick(tickColor, tickProgress, startXOffset, enabled)
    }
}

@Composable
private fun animateProgress(
    transition: Transition<SelectionStage>,
    label: String,
    animationSpec: FiniteAnimationSpec<Float>,
) =
    transition.animateFloat(transitionSpec = { animationSpec }, label = label) {
        when (it) {
            SelectionStage.Unchecked -> 0f
            SelectionStage.Checked -> 1f
        }
    }

private fun Modifier.maybeToggleable(
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean,
    checked: Boolean,
    interactionSource: MutableInteractionSource?,
    indication: Indication,
    canvasWidth: Dp,
    canvasHeight: Dp
): Modifier {
    val standardModifier =
        this.wrapContentSize(Alignment.CenterEnd).requiredSize(canvasWidth, canvasHeight)

    return if (onCheckedChange == null) {
        standardModifier
    } else {
        standardModifier.then(
            Modifier.toggleable(
                enabled = enabled,
                value = checked,
                onValueChange = onCheckedChange,
                indication = indication,
                interactionSource = interactionSource
            )
        )
    }
}

private fun Modifier.maybeSelectable(
    onClick: (() -> Unit)?,
    enabled: Boolean,
    selected: Boolean,
    interactionSource: MutableInteractionSource?,
    indication: Indication,
    canvasWidth: Dp,
    canvasHeight: Dp
): Modifier {
    val standardModifier =
        this.wrapContentSize(Alignment.Center).requiredSize(canvasWidth, canvasHeight)

    return if (onClick == null) {
        standardModifier
    } else {
        standardModifier.then(
            Modifier.selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
        )
    }
}

private fun DrawScope.drawTick(
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
    enabled: Boolean,
) {
    // Using tickProgress animating from zero to TICK_TOTAL_LENGTH,
    // rotate the tick as we draw from 15 degrees to zero.
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val startXOffsetPx = startXOffset.toPx()
    val center = Offset(12.dp.toPx() + startXOffsetPx, 12.dp.toPx())
    val angle = TICK_ROTATION - TICK_ROTATION / tickTotalLength * tickProgressPx
    val angleRadians = angle.toRadians()

    // Animate the base of the tick.
    val baseStart = Offset(6.7f.dp.toPx() + startXOffsetPx, 12.3f.dp.toPx())
    val tickBaseProgress = min(tickProgressPx, tickBaseLength)

    val path = Path()
    path.moveTo(baseStart.rotate(angleRadians, center))
    path.lineTo(
        (baseStart + Offset(tickBaseProgress, tickBaseProgress)).rotate(angleRadians, center)
    )

    if (tickProgressPx > tickBaseLength) {
        val tickStickProgress = min(tickProgressPx - tickBaseLength, tickStickLength)
        val stickStart = Offset(9.3f.dp.toPx() + startXOffsetPx, 16.3f.dp.toPx())
        // Move back to the start of the stick (without drawing)
        path.moveTo(stickStart.rotate(angleRadians, center))
        path.lineTo(
            Offset(stickStart.x + tickStickProgress, stickStart.y - tickStickProgress)
                .rotate(angleRadians, center)
        )
    }
    // Use StrokeCap.Butt because Square adds an extension on the end of each line.
    drawPath(
        path,
        tickColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt),
        blendMode = if (enabled) DefaultBlendMode else BlendMode.Hardlight
    )
}

private fun DrawScope.drawTrack(
    fillColor: Color,
    strokeColor: Color,
    trackWidthPx: Float,
    trackHeightPx: Float,
) {
    val path = Path()
    val strokeRadius = trackHeightPx / 2f
    path.moveTo(Offset(strokeRadius, center.y))
    path.lineTo(Offset(trackWidthPx - strokeRadius, center.y))

    // Draws the border of the track
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = trackHeightPx, cap = StrokeCap.Round),
    )

    // If strokeColor and fillColor are different, drawing another path for the fill of the track.
    if (strokeColor != fillColor) {
        drawPath(
            path = path,
            color = fillColor,
            style =
                Stroke(
                    width = trackHeightPx - 2 * SWITCH_TRACK_BORDER.toPx(),
                    cap = StrokeCap.Round
                )
        )
    }
}

private fun DrawScope.eraseTick(
    tickColor: Color,
    tickProgress: Float,
    startXOffset: Dp,
    enabled: Boolean
) {
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val startXOffsetPx = startXOffset.toPx()

    // Animate the stick of the tick, drawing down the stick from the top.
    val stickStartX = 17.3f.dp.toPx() + startXOffsetPx
    val stickStartY = 8.3f.dp.toPx()
    val tickStickProgress = min(tickProgressPx, tickStickLength)

    val path = Path()
    path.moveTo(stickStartX, stickStartY)
    path.lineTo(stickStartX - tickStickProgress, stickStartY + tickStickProgress)

    if (tickStickProgress > tickStickLength) {
        // Animate the base of the tick, drawing up the base from bottom of the stick.
        val tickBaseProgress = min(tickProgressPx - tickStickLength, tickBaseLength)
        val baseStartX = 10.7f.dp.toPx() + startXOffsetPx
        val baseStartY = 16.3f.dp.toPx()
        path.moveTo(baseStartX, baseStartY)
        path.lineTo(baseStartX - tickBaseProgress, baseStartY - tickBaseProgress)
    }

    drawPath(
        path,
        tickColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt),
        blendMode = if (enabled) DefaultBlendMode else BlendMode.Hardlight
    )
}

private fun Path.moveTo(offset: Offset) {
    moveTo(offset.x, offset.y)
}

private fun Path.lineTo(offset: Offset) {
    lineTo(offset.x, offset.y)
}

private fun Offset.rotate(angleRadians: Float): Offset {
    val angledDirection = directionVector(angleRadians)
    return angledDirection * x + angledDirection.rotate90() * y
}

private fun Offset.rotate(angleRadians: Float, center: Offset): Offset =
    (this - center).rotate(angleRadians) + center

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun directionVector(angleRadians: Float) = Offset(cos(angleRadians), sin(angleRadians))

private fun Offset.rotate90() = Offset(-y, x)

// This is duplicated from wear.compose.foundation/geometry.kt
// Any changes should be replicated there.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) fun Float.toRadians() = this * PI.toFloat() / 180f

private val TICK_BASE_LENGTH = 4.dp
private val TICK_STICK_LENGTH = 8.dp
private const val TICK_ROTATION = 15f

private val SWITCH_TRACK_BORDER = 1.dp

private val RADIO_CIRCLE_RADIUS = 9.dp
private val RADIO_CIRCLE_STROKE = 2.dp
private val RADIO_DOT_RADIUS = 5.dp
