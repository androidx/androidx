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
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch

/**
 * An implementation similar to RoundedCornerShape, but based on linear interpolation between a
 * start and stop CornerSize, and an observable progress between 0.0 and 1.0.
 *
 * @param startCornerSize the corner size when progress is 0.0
 * @param endCornerSize the corner size when progress is 1.0
 * @param progress returns the current progress from start to stop.
 */
@Stable
private class AnimatedToggleRoundedCornerShape(
    val startCornerSize: MutableState<CornerSize>,
    val endCornerSize: MutableState<CornerSize>,
    var progress: () -> Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val animatedCornerSize =
            AnimatedCornerSize(startCornerSize.value, endCornerSize.value, progress)
        val animatedCornerSizePx = animatedCornerSize.toPx(size, density)

        return Outline.Rounded(
            roundRect =
                RoundRect(
                    rect = size.toRect(),
                    radiusX = animatedCornerSizePx,
                    radiusY = animatedCornerSizePx,
                )
        )
    }
}

/**
 * Returns a Shape that will internally animate between the unchecked, checked and pressed shape as
 * the button is pressed and checked/unchecked.
 */
@Composable
internal fun rememberAnimatedToggleRoundedCornerShape(
    interactionSource: InteractionSource,
    uncheckedCornerSize: CornerSize,
    checkedCornerSize: CornerSize,
    uncheckedPressedCornerSize: CornerSize,
    checkedPressedCornerSize: CornerSize,
    checked: Boolean,
    onPressAnimationSpec: FiniteAnimationSpec<Float>,
    onReleaseAnimationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val endCornerSize = remember {
        mutableStateOf(if (checked) checkedPressedCornerSize else uncheckedPressedCornerSize)
    }

    val animatedShape = remember {
        AnimatedToggleRoundedCornerShape(
            startCornerSize =
                mutableStateOf(if (checked) checkedCornerSize else uncheckedCornerSize),
            endCornerSize =
                mutableStateOf(
                    if (checked) checkedPressedCornerSize else uncheckedPressedCornerSize
                ),
            progress = { progress.value },
        )
    }

    LaunchedEffect(checked) {
        animatedShape.startCornerSize.value =
            if (checked) checkedCornerSize else uncheckedCornerSize
        // We don't update the endCornerSize directly here, because that would cause a jump in the
        // animation, when the pressed shape is released and checked state changes.
        // Instead the animation end value will be updated on the next press.
        endCornerSize.value = if (checked) checkedPressedCornerSize else uncheckedPressedCornerSize
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    // Update the endCornerSize to the correct pressed value, depending on the
                    // checked state, when the press is initialized.
                    animatedShape.endCornerSize.value = endCornerSize.value

                    scope.launch { progress.animateTo(1f, animationSpec = onPressAnimationSpec) }
                }
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

    return animatedShape
}

private const val MIN_REQUIRED_ANIMATION_PROGRESS = 0.75f
