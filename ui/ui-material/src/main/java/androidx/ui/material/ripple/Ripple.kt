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
import androidx.compose.CompositionLifecycleObserver
import androidx.compose.StructurallyEqual
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.ui.animation.asDisposableClock
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Constraints
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DrawModifier
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.center
import androidx.ui.unit.ipx
import androidx.ui.unit.toPxSize

/**
 * Ripple is a [Modifier] which draws the visual indicator for a pressed state.
 *
 * Ripple responds to a tap by starting a new [RippleEffect] animation.
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
fun ripple(
    bounded: Boolean = true,
    radius: Dp? = null,
    color: Color? = null,
    enabled: Boolean = true,
    clock: AnimationClockObservable = AnimationClockAmbient.current
): Modifier {
    @Suppress("NAME_SHADOWING") // don't allow usage of the parameter clock, only the disposable
    val clock = clock.asDisposableClock()
    val density = DensityAmbient.current
    val rippleModifier = remember { RippleModifier() }
    val theme = RippleThemeAmbient.current
    rippleModifier.color = (color ?: theme.defaultColor()).copy(alpha = theme.opacity())

    val pressIndicator = PressIndicatorGestureDetector(
        onStart = { position ->
            if (enabled && transitionsEnabled) {
                rippleModifier.handleStart(position, theme.factory, density, bounded, radius, clock)
            }
        },
        onStop = { rippleModifier.handleFinish(false) },
        onCancel = { rippleModifier.handleFinish(true) }
    )
    return pressIndicator + rippleModifier
}

private class RippleModifier : DrawModifier, LayoutModifier, CompositionLifecycleObserver {

    var color: Color by mutableStateOf(Color.Transparent, StructurallyEqual)

    private var size: IntPxSize = IntPxSize(0.ipx, 0.ipx)
    private var effects = mutableListOf<RippleEffect>()
    private var currentEffect: RippleEffect? = null

    private var animationPulse by mutableStateOf(0L)
    private val redraw: () -> Unit = { animationPulse++ }

    override fun Density.modifySize(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        childSize: IntPxSize
    ): IntPxSize {
        size = childSize
        return childSize
    }

    fun handleStart(
        touchPosition: PxPosition,
        factory: RippleEffectFactory,
        density: Density,
        bounded: Boolean,
        radius: Dp?,
        clock: AnimationClockObservable
    ) {
        val position = if (bounded) touchPosition else size.toPxSize().center()
        val onAnimationFinished = { effect: RippleEffect ->
            effects.remove(effect)
            if (currentEffect == effect) {
                currentEffect = null
            }
        }
        val effect = factory.create(
            size,
            position,
            density,
            radius,
            bounded,
            clock,
            redraw,
            onAnimationFinished
        )

        effects.add(effect)
        currentEffect = effect
        redraw()
    }

    fun handleFinish(canceled: Boolean) {
        currentEffect?.finish(canceled)
        currentEffect = null
    }

    override fun draw(density: Density, drawContent: () -> Unit, canvas: Canvas, size: PxSize) {
        drawContent()
        animationPulse // model read
        effects.forEach { it.draw(canvas, this.size, color) }
    }

    override fun onEnter() {
        // do nothing
    }

    override fun onLeave() {
        effects.forEach { it.dispose() }
        effects.clear()
        currentEffect = null
    }
}
