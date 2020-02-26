/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.ripple

import androidx.animation.AnimationClockObservable
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Draw
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.graphics.Color
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.center
import androidx.ui.unit.toPxSize

/**
 * Ripple is a visual indicator for a pressed state.
 *
 * A [Ripple] component responds to a tap by starting a new [RippleEffect] animation.
 * For creating an effect it uses the [RippleTheme.factory].
 *
 * @sample androidx.ui.material.samples.RippleSample
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded
 * ripples always animate from the target layout center, bounded ripples animate from the touch
 * position.
 * @param radius Effects grow up to this size. If null is provided the size would be calculated
 * based on the target layout size.
 * @param color The Ripple color is usually the same color used by the text or iconography in the
 * component. If null is provided the color will be calculated by [RippleTheme.defaultColor].
 * @param clock The animation clock observable that will drive this ripple effect
 * @param enabled The ripple effect will not start if false is provided.
 */
@Composable
fun Ripple(
    bounded: Boolean,
    radius: Dp? = null,
    color: Color? = null,
    enabled: Boolean = true,
    clock: AnimationClockObservable = AnimationClockAmbient.current,
    children: @Composable() () -> Unit
) {
    val density = DensityAmbient.current
    val state = remember { RippleState() }
    val theme = RippleThemeAmbient.current

    OnChildPositioned(onPositioned = { state.coordinates = it }) {
        PressIndicatorGestureDetector(
            onStart = { position ->
                if (enabled && transitionsEnabled) {
                    state.handleStart(position, theme.factory, density, bounded, radius, clock)
                }
            },
            onStop = { state.handleFinish(false) },
            onCancel = { state.handleFinish(true) },
            children = children
        )
    }

    Recompose { recompose ->
        state.recompose = recompose
        val finalColor = (color ?: theme.defaultColor()).copy(alpha = theme.opacity())
        @Suppress("DEPRECATION") // remove when b/147606015 is fixed
        Draw { canvas, _ ->
            if (state.effects.isNotEmpty()) {
                val position = state.coordinates!!.parentCoordinates
                    ?.childToLocal(state.coordinates!!, PxPosition.Origin) ?: PxPosition.Origin
                canvas.translate(position.x.value, position.y.value)
                state.effects.forEach { it.draw(canvas, finalColor) }
                canvas.translate(-position.x.value, -position.y.value)
            }
        }
    }

    onDispose {
        state.effects.forEach { it.dispose() }
        state.effects.clear()
        state.currentEffect = null
    }
}

private class RippleState {

    var coordinates: LayoutCoordinates? = null
    var effects = mutableListOf<RippleEffect>()
    var currentEffect: RippleEffect? = null
    var recompose: () -> Unit = {}

    fun handleStart(
        touchPosition: PxPosition,
        factory: RippleEffectFactory,
        density: Density,
        bounded: Boolean,
        radius: Dp?,
        clock: AnimationClockObservable
    ) {
        val coordinates = checkNotNull(coordinates) {
            "handleStart() called before the layout coordinates were provided!"
        }
        val position = if (bounded) touchPosition else coordinates.size.toPxSize().center()
        val onAnimationFinished = { effect: RippleEffect ->
            effects.remove(effect)
            if (currentEffect == effect) {
                currentEffect = null
            }
        }
        val effect = factory.create(
            coordinates,
            position,
            density,
            radius,
            bounded,
            clock,
            recompose,
            onAnimationFinished
        )

        effects.add(effect)
        currentEffect = effect
        recompose()
    }

    fun handleFinish(canceled: Boolean) {
        currentEffect?.finish(canceled)
        currentEffect = null
    }
}
