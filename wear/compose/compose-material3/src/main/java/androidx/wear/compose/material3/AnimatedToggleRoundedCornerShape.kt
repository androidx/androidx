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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * A animated [RoundedCornerShape]. Animation is driven by changes to the [cornerSize] lambda.
 * [currentShapeSize] is provided as Size is received here, but must affect the animation.
 *
 * @param currentShapeSize MutableState coordinating the current size.
 * @param cornerSize a lambda resolving to the current Corner size.
 */
@Stable
internal class AnimatedToggleRoundedCornerShape(
    private val currentShapeSize: MutableState<Size?>,
    private val cornerSize: () -> CornerSize,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cornerRadius = cornerSize().toPx(size, density)

        currentShapeSize.value = size

        return Outline.Rounded(
            roundRect =
                RoundRect(rect = size.toRect(), radiusX = cornerRadius, radiusY = cornerRadius)
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

    val transition = updateTransition(toggleState, label = "Toggle State")
    val density = LocalDensity.current

    val currentShapeSize = remember { mutableStateOf<Size?>(null) }

    val observedSize = currentShapeSize.value

    if (observedSize != null) {
        val sizePx =
            transition.animateFloat(
                label = "Corner Size",
                transitionSpec = {
                    when {
                        targetState isTransitioningTo ToggleState.Pressed -> onPressAnimationSpec
                        else -> onReleaseAnimationSpec
                    }
                },
            ) { newState ->
                newState
                    .cornerSize(uncheckedCornerSize, checkedCornerSize, pressedCornerSize)
                    .toPx(observedSize, density)
            }

        return remember(sizePx) {
            AnimatedToggleRoundedCornerShape(
                currentShapeSize = currentShapeSize,
            ) {
                CornerSize(sizePx.value)
            }
        }
    } else {
        return remember(toggleState, uncheckedCornerSize, checkedCornerSize, pressedCornerSize) {
            AnimatedToggleRoundedCornerShape(
                currentShapeSize = currentShapeSize,
            ) {
                toggleState.cornerSize(
                    uncheckedCornerSize,
                    checkedCornerSize,
                    pressedCornerSize,
                )
            }
        }
    }
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
