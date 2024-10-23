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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    var startCornerSize: CornerSize,
    var endCornerSize: CornerSize,
    var progress: () -> Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val animatedCornerSize = AnimatedCornerSize(startCornerSize, endCornerSize, progress)
        val animatedCornerSizePx = animatedCornerSize.toPx(size, density)

        return Outline.Rounded(
            roundRect =
                RoundRect(
                    rect = size.toRect(),
                    radiusX = animatedCornerSizePx,
                    radiusY = animatedCornerSizePx
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
    uncheckedCornerSize: CornerSize,
    checkedCornerSize: CornerSize,
    pressedCornerSize: CornerSize,
    pressed: Boolean,
    checked: Boolean,
    onPressAnimationSpec: FiniteAnimationSpec<Float>,
    onReleaseAnimationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val toggleState =
        when {
            pressed -> ToggleState.Pressed
            checked -> ToggleState.Checked
            else -> ToggleState.Unchecked
        }

    val previous = remember { mutableStateOf(toggleState) }
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(1f) }

    val toggledCornerSize =
        toggleState.cornerSize(uncheckedCornerSize, checkedCornerSize, pressedCornerSize)
    val animationSpec = if (pressed) onPressAnimationSpec else onReleaseAnimationSpec

    val animatedShape = remember {
        AnimatedToggleRoundedCornerShape(
            startCornerSize = toggledCornerSize,
            endCornerSize = toggledCornerSize,
            progress = { progress.value },
        )
    }

    LaunchedEffect(toggleState) {
        // Allow the press up animation to finish its minimum duration before starting the next
        if (!pressed) {
            waitUntil { !progress.isRunning || progress.value > MIN_REQUIRED_ANIMATION_PROGRESS }
        }

        if (toggleState != previous.value) {
            animatedShape.startCornerSize = animatedShape.endCornerSize
            animatedShape.endCornerSize = toggledCornerSize
            previous.value = toggleState

            scope.launch {
                progress.snapTo(1f - progress.value)
                progress.animateTo(1f, animationSpec = animationSpec)
            }
        }
    }

    return animatedShape
}

private fun ToggleState.cornerSize(
    uncheckedCornerSize: CornerSize,
    checkedCornerSize: CornerSize,
    pressedCornerSize: CornerSize,
) =
    when (this) {
        ToggleState.Unchecked -> uncheckedCornerSize
        ToggleState.Checked -> checkedCornerSize
        ToggleState.Pressed -> pressedCornerSize
    }

internal enum class ToggleState {
    Unchecked,
    Checked,
    Pressed,
}

private const val MIN_REQUIRED_ANIMATION_PROGRESS = 0.75f
