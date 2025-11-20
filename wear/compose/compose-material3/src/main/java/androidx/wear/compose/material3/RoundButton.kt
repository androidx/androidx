/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

/**
 * This is a copy of RoundButton from materialcore, with additional onLongClick callback and usage
 * of combinedClickable.
 */
@Composable
internal fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean,
    backgroundColor: @Composable (enabled: Boolean) -> Color,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    border: @Composable (enabled: Boolean) -> BorderStroke?,
    ripple: Indication,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderStroke = border(enabled)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .semantics { role = Role.Button }
                .clip(shape) // Clip for the touch area (e.g. for Ripple).
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick, // NB combinedClickable calls LongPress haptic
                    onLongClickLabel = onLongClickLabel,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple,
                )
                .then(
                    if (borderStroke != null) Modifier.border(border = borderStroke, shape = shape)
                    else Modifier
                )
                .background(color = backgroundColor(enabled), shape = shape),
        content = content,
    )
}

/**
 * Returns a Shape that will internally animate between the normal shape and pressedShape as the
 * button is pressed.
 *
 * Size and density must be known at this point since Corners may be specified in either percentage
 * or dp, and cannot be correctly scaled as either a RoundedPolygon or a Morph.
 */
@Composable
internal fun rememberAnimatedPressedButtonShape(
    interactionSource: InteractionSource,
    shape: CornerBasedShape,
    pressedShape: CornerBasedShape,
    onPressAnimationSpec: FiniteAnimationSpec<Float>,
    onReleaseAnimationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press ->
                    scope.launch { progress.animateTo(1f, animationSpec = onPressAnimationSpec) }
                is PressInteraction.Cancel,
                is PressInteraction.Release -> {
                    waitUntil {
                        !progress.isRunning || progress.value > MIN_REQUIRED_ANIMATION_PROGRESS
                    }
                    scope.launch { progress.animateTo(0f, animationSpec = onReleaseAnimationSpec) }
                }
            }
        }
    }

    return when {
        shape is RoundedCornerShape && pressedShape is RoundedCornerShape ->
            rememberAnimatedRoundedCornerShape(
                shape = shape,
                pressedShape = pressedShape,
                progress = progress.asState(),
            )
        else ->
            rememberAnimatedCornerBasedShape(
                shape = shape,
                pressedShape = pressedShape,
                progress = progress.asState(),
            )
    }
}

@Composable
internal fun animateButtonShape(
    shape: Shape,
    pressedShape: Shape,
    onPressAnimationSpec: FiniteAnimationSpec<Float>,
    onReleaseAnimationSpec: FiniteAnimationSpec<Float>,
    interactionSource: MutableInteractionSource?,
) =
    if (shape is CornerBasedShape && pressedShape is CornerBasedShape && shape !== pressedShape) {
        val finalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

        val finalShape =
            rememberAnimatedPressedButtonShape(
                interactionSource = finalInteractionSource,
                shape = shape,
                pressedShape = pressedShape,
                onPressAnimationSpec = onPressAnimationSpec,
                onReleaseAnimationSpec = onReleaseAnimationSpec,
            )
        finalShape to finalInteractionSource
    } else {
        // Fallback to static uncheckedShape if no other shapes, or not animatable
        shape to interactionSource
    }

@Composable
internal fun animateToggleButtonShape(
    uncheckedShape: Shape,
    checkedShape: Shape,
    uncheckedPressedShape: Shape,
    checkedPressedShape: Shape,
    onPressAnimationSpec: FiniteAnimationSpec<Float>,
    onReleaseAnimationSpec: FiniteAnimationSpec<Float>,
    checked: Boolean,
    interactionSource: MutableInteractionSource?,
): Pair<Shape, MutableInteractionSource?> {
    return if (checkedShape === uncheckedShape) {
        // Reuse pressed animation
        return animateButtonShape(
            shape = uncheckedShape,
            pressedShape = uncheckedPressedShape,
            onPressAnimationSpec = onPressAnimationSpec,
            onReleaseAnimationSpec = onReleaseAnimationSpec,
            interactionSource = interactionSource,
        )
    } else if (
        uncheckedShape is RoundedCornerShape &&
            checkedShape is RoundedCornerShape &&
            uncheckedPressedShape is RoundedCornerShape &&
            checkedPressedShape is RoundedCornerShape
    ) {
        // Animate between the corner radius

        val finalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

        rememberAnimatedToggleRoundedCornerShape(
            interactionSource = finalInteractionSource,
            uncheckedCornerSize = uncheckedShape.topEnd,
            checkedCornerSize = checkedShape.topEnd,
            uncheckedPressedCornerSize = uncheckedPressedShape.topEnd,
            checkedPressedCornerSize = checkedPressedShape.topEnd,
            checked = checked,
            onPressAnimationSpec = onPressAnimationSpec,
            onReleaseAnimationSpec = onReleaseAnimationSpec,
        ) to finalInteractionSource
    } else {
        // Fallback to static uncheckedShape if no other shapes, or not animatable
        uncheckedShape to interactionSource
    }
}

private const val MIN_REQUIRED_ANIMATION_PROGRESS = 0.75f
