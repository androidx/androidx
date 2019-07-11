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

import androidx.ui.core.Density
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.ambientDensity
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.onDispose
import androidx.compose.unaryPlus
import androidx.ui.core.Dp

/**
 * An area of a [RippleSurface] that responds to touch.
 *
 * A [Ripple] widget responds to a tap by starting a new [RippleEffect]
 * animation. For creating an effect it uses the [RippleTheme.factory].
 *
 * The [Ripple] widget must have a [RippleSurface] ancestor as the
 * [RippleSurface] is where the [Ripple]s are actually drawn.
 *
 * Example:
 *
 *     Card { // Card will provide RippleSurface
 *        Ripple(bounded = true) {
 *            Clickable(onClick) {
 *                Icon(image)
 *            }
 *        }
 *     }
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout.
    Unbounded ripples always animate from the center position, bounded ripples
    animate from the touch position.
 * @param radius Effects grow up to this size. By default the size is
 *  determined from the size of the layout itself.
 */
@Composable
fun Ripple(
    bounded: Boolean,
    radius: Dp? = null,
    @Children children: @Composable() () -> Unit
) {
    val density = +ambientDensity()
    val rippleSurface = +ambientRippleSurface()
    val state = +memo { RippleState() }

    val theme = +ambient(CurrentRippleTheme)
    state.currentEffect?.color = theme.colorCallback.invoke(
        rippleSurface.backgroundColor
    )

    OnChildPositioned(onPositioned = { state.coordinates = it }) {
        PressIndicatorGestureDetector(
            onStart = { position ->
                state.handleStart(position, rippleSurface, theme, density, bounded, radius)
            },
            onStop = { state.handleFinish(false) },
            onCancel = { state.handleFinish(true) },
            children = children
        )
    }

    +onDispose {
        state.effects.forEach { it.dispose() }
        state.effects.clear()
        state.currentEffect = null
    }
}

private class RippleState {

    var coordinates: LayoutCoordinates? = null
    var effects = mutableSetOf<RippleEffect>()
    var currentEffect: RippleEffect? = null

    fun handleStart(
        position: PxPosition,
        rippleSurface: RippleSurfaceOwner,
        theme: RippleTheme,
        density: Density,
        bounded: Boolean,
        radius: Dp?
    ) {
        val coordinates = coordinates ?: throw IllegalStateException(
            "handleStart() called before the layout coordinates were provided!"
        )
        val color = theme.colorCallback.invoke(rippleSurface.backgroundColor)
        val onAnimationFinished = { effect: RippleEffect ->
            effects.remove(effect)
            if (currentEffect == effect) {
                currentEffect = null
            }
        }

        val effect = theme.factory.create(
            coordinates,
            rippleSurface.layoutCoordinates,
            position,
            color,
            density,
            radius,
            bounded,
            rippleSurface::requestRedraw,
            onAnimationFinished
        )

        rippleSurface.addEffect(effect)
        effects.add(effect)
        currentEffect = effect
    }

    fun handleFinish(canceled: Boolean) {
        currentEffect?.finish(canceled)
        currentEffect = null
    }
}
