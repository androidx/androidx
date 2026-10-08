/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Resolves the [RemoteShape] of a toggle button for the given [checked] state.
 *
 * If [checked] is a constant, the matching shape is returned directly. Otherwise, if both shapes
 * are [RemoteCornerBasedShape]s, a [RemoteRoundedCornerShape] whose corner sizes are selected at
 * playback time from [checked] is returned, so that the shape follows state changes on the player.
 * For any other combination, [uncheckedShape] is used.
 */
// TODO(b/571080867): Move shape selection and interpolation helpers to
//  androidx.compose.remote.foundation.
internal fun resolveToggleButtonShape(
    checked: RemoteBoolean,
    uncheckedShape: RemoteShape,
    checkedShape: RemoteShape,
): RemoteShape {
    if (uncheckedShape === checkedShape) return uncheckedShape
    checked.constantValueOrNull?.let { isChecked ->
        return if (isChecked) checkedShape else uncheckedShape
    }
    if (uncheckedShape is RemoteCornerBasedShape && checkedShape is RemoteCornerBasedShape) {
        return RemoteRoundedCornerShape(
            topStart = CheckedCornerSize(checked, checkedShape.topStart, uncheckedShape.topStart),
            topEnd = CheckedCornerSize(checked, checkedShape.topEnd, uncheckedShape.topEnd),
            bottomEnd =
                CheckedCornerSize(checked, checkedShape.bottomEnd, uncheckedShape.bottomEnd),
            bottomStart =
                CheckedCornerSize(checked, checkedShape.bottomStart, uncheckedShape.bottomStart),
        )
    }
    return uncheckedShape
}

/** A [RemoteCornerSize] that picks between two corner sizes based on a [RemoteBoolean]. */
private data class CheckedCornerSize(
    val checked: RemoteBoolean,
    val checkedSize: RemoteCornerSize,
    val uncheckedSize: RemoteCornerSize,
) : RemoteCornerSize {
    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat =
        checked.select(
            ifTrue = checkedSize.toPx(shapeSize, density),
            ifFalse = uncheckedSize.toPx(shapeSize, density),
        )
}

/**
 * The [shape] of a toggle button together with the [modifier] that drives its press animation.
 *
 * [modifier] must be applied to the toggle button so that touch events on the player update the
 * pressed state used by [shape].
 */
internal class AnimatedToggleButtonShape(val shape: RemoteShape, val modifier: RemoteModifier)

/**
 * Resolves the [RemoteShape] of a toggle button, animating between the resting shapes and the
 * pressed shapes when the toggle button is pressed on the player.
 *
 * The press animation is only applied when [enabled] is the constant `true`, when the pressed
 * shapes differ from the resting shapes, and when all the shapes are [RemoteCornerBasedShape]s. In
 * any other case the shape is resolved as in [resolveToggleButtonShape] and no touch handling is
 * added.
 *
 * The pressed state is driven by touch down, up and cancel actions, so the animation only runs on
 * players that dispatch those events. It is also skipped when the creation profile doesn't support
 * those actions, for example [RcPlatformProfiles.WEAR_WIDGETS].
 */
// TODO(b/571080867): Move press shape animation helper to androidx.compose.remote.foundation.
@Suppress("RestrictedApiAndroidX") // LocalRemoteComposeCreationState
@Composable
internal fun animatedToggleButtonShape(
    checked: RemoteBoolean,
    enabled: RemoteBoolean,
    uncheckedShape: RemoteShape,
    checkedShape: RemoteShape,
    uncheckedPressedShape: RemoteShape,
    checkedPressedShape: RemoteShape,
    pressAnimationSpec: RemoteAnimationSpec = PressAnimationSpec,
    releaseAnimationSpec: RemoteAnimationSpec = ReleaseAnimationSpec,
): AnimatedToggleButtonShape {
    val shape = resolveToggleButtonShape(checked, uncheckedShape, checkedShape)
    val touchSupported = LocalRemoteComposeCreationState.current.profile.supportsTouchOperations()
    if (
        enabled.constantValueOrNull != true ||
            (uncheckedPressedShape == uncheckedShape && checkedPressedShape == checkedShape) ||
            !touchSupported
    ) {
        return AnimatedToggleButtonShape(shape, RemoteModifier)
    }
    val pressedShape = resolveToggleButtonShape(checked, uncheckedPressedShape, checkedPressedShape)
    if (shape !is RemoteCornerBasedShape || pressedShape !is RemoteCornerBasedShape) {
        return AnimatedToggleButtonShape(shape, RemoteModifier)
    }

    val pressed = rememberMutableRemoteFloat(0f)
    val progress =
        remember(pressed, pressAnimationSpec, releaseAnimationSpec) {
            // Taking the max of a fast and a slow animation of the same target makes the shape
            // morph quickly when pressed, and settle back slowly when released or cancelled.
            max(
                animateRemoteFloatAsState(pressed, pressAnimationSpec),
                animateRemoteFloatAsState(pressed, releaseAnimationSpec),
            )
        }
    return AnimatedToggleButtonShape(
        shape =
            RemoteRoundedCornerShape(
                topStart = LerpCornerSize(shape.topStart, pressedShape.topStart, progress),
                topEnd = LerpCornerSize(shape.topEnd, pressedShape.topEnd, progress),
                bottomEnd = LerpCornerSize(shape.bottomEnd, pressedShape.bottomEnd, progress),
                bottomStart = LerpCornerSize(shape.bottomStart, pressedShape.bottomStart, progress),
            ),
        modifier =
            RemoteModifier.onTouchDown(valueChange(pressed, 1f.rf))
                .onTouchUp(valueChange(pressed, 0f.rf))
                .onTouchCancel(valueChange(pressed, 0f.rf)),
    )
}

/**
 * Returns a copy of this shape with each corner size scaled by [fraction], used to derive the
 * pressed shapes of the variant animated toggle button shapes.
 */
internal fun RemoteCornerBasedShape.scaleCorners(fraction: Float): RemoteCornerBasedShape =
    copy(
        topStart = FractionalCornerSize(topStart, fraction),
        topEnd = FractionalCornerSize(topEnd, fraction),
        bottomEnd = FractionalCornerSize(bottomEnd, fraction),
        bottomStart = FractionalCornerSize(bottomStart, fraction),
    )

/** A [RemoteCornerSize] that interpolates from [start] to [stop] by [fraction]. */
private data class LerpCornerSize(
    val start: RemoteCornerSize,
    val stop: RemoteCornerSize,
    val fraction: RemoteFloat,
) : RemoteCornerSize {
    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat {
        val startPx = start.toPx(shapeSize, density)
        return startPx + (stop.toPx(shapeSize, density) - startPx) * fraction
    }
}

/** A [RemoteCornerSize] that is [fraction] of [size]. */
private data class FractionalCornerSize(val size: RemoteCornerSize, val fraction: Float) :
    RemoteCornerSize {
    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat =
        size.toPx(shapeSize, density) * fraction.rf
}

/** Whether this profile supports the operations needed to drive the press animation. */
// TODO(b/571080867): Move supportsTouchOperations() to androidx.compose.remote.foundation.
@Suppress("RestrictedApiAndroidX", "PrimitiveInCollection") // Operations, supportedOperations
private fun Profile.supportsTouchOperations(): Boolean =
    supportedOperations.containsAll(
        listOf(
            Operations.MODIFIER_TOUCH_DOWN,
            Operations.MODIFIER_TOUCH_UP,
            Operations.MODIFIER_TOUCH_CANCEL,
        )
    )

/**
 * Returns the progress of the animation between the unchecked (`0`) and checked (`1`) states of a
 * toggle button, used to animate its colors like Wear Material3. Returns `null` when [checked] is a
 * constant, in which case there is nothing to animate.
 */
@Composable
internal fun animatedCheckedProgress(
    checked: RemoteBoolean,
    animationSpec: RemoteAnimationSpec = CheckedAnimationSpec,
): RemoteFloat? {
    if (checked.hasConstantValue) return null
    return remember(checked, animationSpec) {
        animateRemoteFloatAsState(checked.select(1f.rf, 0f.rf), animationSpec)
    }
}

/**
 * Returns [checkedColor] or [uncheckedColor] for [checked], interpolated by [checkedProgress] when
 * it is not `null`.
 */
internal fun checkedColor(
    checked: RemoteBoolean,
    checkedProgress: RemoteFloat?,
    checkedColor: RemoteColor,
    uncheckedColor: RemoteColor,
): RemoteColor =
    if (checkedProgress == null) {
        checked.select(checkedColor, uncheckedColor)
    } else {
        tween(uncheckedColor, checkedColor, checkedProgress)
    }

/**
 * Default [RemoteAnimationSpec] for the color animation between checked and unchecked states,
 * matching `COLOR_ANIMATION_SPEC` of Wear Material3 toggle buttons.
 */
internal val CheckedAnimationSpec: RemoteAnimationSpec =
    remoteTween(RemoteMotionTokens.DurationMedium1, RemoteMotionTokens.EasingStandardDecelerate)

/** Default [RemoteAnimationSpec] for the animation to the pressed shape. */
internal val PressAnimationSpec: RemoteAnimationSpec =
    remoteTween(RemoteMotionTokens.DurationShort2)

/** Default [RemoteAnimationSpec] for the animation back to the resting shape. */
internal val ReleaseAnimationSpec: RemoteAnimationSpec =
    remoteTween(RemoteMotionTokens.DurationMedium2)

/**
 * Fraction of the corner sizes of a variant toggle button shape used for its pressed shape. See
 * `PressedShapeCornerSizeFraction` in Wear Compose Material3.
 */
internal const val PressedShapeCornerSizeFraction = 0.66f
