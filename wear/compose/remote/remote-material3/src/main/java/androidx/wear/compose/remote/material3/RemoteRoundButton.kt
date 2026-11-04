/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.currentProfileSupportsPathClip
import androidx.compose.remote.creation.compose.capture.currentProfileSupportsTouchModifiers
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.shapes.RemoteCornerSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Single-slot base button for [RemoteTextButton], [RemoteIconButton], [RemoteIconToggleButton] and
 * [RemoteTextToggleButton].
 *
 * @param pressedShape shape to animate to while the button is pressed on the player, see
 *   [animatePressedShape]. Defaults to [shape], which disables the press animation.
 * @param pressAnimationSpec [RemoteAnimationSpec] used to animate from [shape] to [pressedShape]
 *   when pressed.
 * @param releaseAnimationSpec [RemoteAnimationSpec] used to animate back to [shape] when released
 *   or cancelled.
 */
@Composable
@RemoteComposable
internal fun RemoteRoundButton(
    onClick: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    backgroundColor: RemoteColor,
    enabled: RemoteBoolean,
    border: RemoteBorderStroke?,
    shape: RemoteShape,
    pressedShape: RemoteShape = shape,
    pressAnimationSpec: RemoteAnimationSpec =
        RemoteMaterialTheme.motionScheme.roundButtonPressSpec(),
    releaseAnimationSpec: RemoteAnimationSpec = RemoteMaterialTheme.motionScheme.slowSpatialSpec(),
    role: RemoteRole = RemoteRole.Button,
    content: @Composable @RemoteComposable () -> Unit,
) {
    val clipBackgroundToShape = currentProfileSupportsPathClip()
    val pressAnimation =
        animatePressedShape(
            shape = shape,
            pressedShape = pressedShape,
            enabled = enabled,
            pressAnimationSpec = pressAnimationSpec,
            releaseAnimationSpec = releaseAnimationSpec,
        )

    RemoteBox(
        contentAlignment = RemoteAlignment.Center,
        modifier =
            modifier
                .then(pressAnimation.modifier)
                .drawWithContent {
                    val drawBackground: RemoteDrawScope.() -> Unit = {
                        drawShapedBackground(
                            shape = pressAnimation.shape,
                            color = backgroundColor,
                            enabled = enabled,
                            containerPainter = null,
                            disabledContainerPainter = null,
                            border = border,
                        )
                    }
                    if (clipBackgroundToShape) {
                        clipToShape(pressAnimation.shape, drawBackground)
                    } else {
                        drawBackground()
                    }
                    drawContent()
                }
                .clip(shape = pressAnimation.shape)
                .clickable(onClick, enabled = enabled.constantValueOrNull ?: false, role = role)
                .semantics(mergeDescendants = true) { this.role = role },
        content = content,
    )
}

/**
 * The [shape] of a button together with the [modifier] that drives its press animation.
 *
 * [modifier] must be applied to the button so that touch events on the player update the pressed
 * state used by [shape].
 */
private class PressAnimatedShape(val shape: RemoteShape, val modifier: RemoteModifier)

/**
 * Animates the shape of a button from [shape] to [pressedShape] while it is pressed on the player.
 *
 * The press animation is only applied when [enabled] is the constant `true`, when [pressedShape]
 * differs from [shape], and when both shapes are [RemoteRoundedCornerShape]s. In any other case
 * [shape] is used and no touch handling is added.
 *
 * The pressed state is driven by touch down, up and cancel actions, so the animation only runs on
 * players that dispatch those events. It is also skipped when
 * [currentProfileSupportsTouchModifiers] is `false`, for example with
 * `androidx.glance.wear.GlanceWearProfiles.wearWidgets`.
 */
// TODO(b/571840744): Migrate to an interaction source once Remote Compose supports one, so the
//  pressed state comes from the clickable instead of separate touch modifiers.
// TODO(b/571080867): Move press shape animation helper to androidx.compose.remote.foundation.
@Composable
private fun animatePressedShape(
    shape: RemoteShape,
    pressedShape: RemoteShape,
    enabled: RemoteBoolean,
    pressAnimationSpec: RemoteAnimationSpec,
    releaseAnimationSpec: RemoteAnimationSpec,
): PressAnimatedShape {
    val touchSupported = currentProfileSupportsTouchModifiers()
    if (
        enabled.constantValueOrNull != true ||
            pressedShape == shape ||
            shape !is RemoteRoundedCornerShape ||
            pressedShape !is RemoteRoundedCornerShape ||
            !touchSupported
    ) {
        return PressAnimatedShape(shape, RemoteModifier)
    }

    val pressed = remember { MutableRemoteFloat(0f) }
    val progress =
        remember(pressed, pressAnimationSpec, releaseAnimationSpec) {
            // Taking the max of a fast and a slow animation of the same target makes the shape
            // morph quickly when pressed, and settle back slowly when released or cancelled.
            max(
                animateRemoteFloatAsState(pressed, pressAnimationSpec),
                animateRemoteFloatAsState(pressed, releaseAnimationSpec),
            )
        }
    return PressAnimatedShape(
        shape =
            RemoteRoundedCornerShape(
                topStart = LerpCornerSize(shape.topStart, pressedShape.topStart, progress),
                topEnd = LerpCornerSize(shape.topEnd, pressedShape.topEnd, progress),
                bottomEnd = LerpCornerSize(shape.bottomEnd, pressedShape.bottomEnd, progress),
                bottomStart = LerpCornerSize(shape.bottomStart, pressedShape.bottomStart, progress),
            ),
        modifier =
            RemoteModifier.onTouchDown(valueChangeAction(pressed, 1f.rf))
                .onTouchUp(valueChangeAction(pressed, 0f.rf))
                .onTouchCancel(valueChangeAction(pressed, 0f.rf)),
    )
}

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

/**
 * [RemoteMotionScheme.fastSpatialSpec] made three times as fast, like the
 * `fastSpatialSpec().faster(200f)` that Wear Compose Material 3 `IconButton` and `TextButton`
 * animate a press with. A spring is made faster by scaling its stiffness, which only the built-in
 * schemes expose, so a custom scheme's [RemoteMotionScheme.fastSpatialSpec] is used as is.
 */
private fun RemoteMotionScheme.roundButtonPressSpec(): RemoteAnimationSpec =
    when (this) {
        RemoteMotionScheme.standard() ->
            remoteSpring(
                stiffness = RemoteMotionTokens.StandardFastStiffness * PressSpeedFactorSquared,
                dampingRatio = RemoteMotionTokens.StandardSpatialDampingRatio,
            )
        RemoteMotionScheme.expressive() ->
            remoteSpring(
                stiffness = RemoteMotionTokens.ExpressiveFastStiffness * PressSpeedFactorSquared,
                dampingRatio = RemoteMotionTokens.ExpressiveFastDamping,
            )
        else -> fastSpatialSpec()
    }

/**
 * The square of the speed factor of the press animation: `faster(200f)` triples the speed of a
 * spring by multiplying its stiffness by 3².
 */
private const val PressSpeedFactorSquared = 9f
