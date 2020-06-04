/**
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

@file:Suppress("Deprecation")

package androidx.ui.material.ripple

import androidx.animation.AnimationClockObservable
import androidx.compose.CompositionLifecycleObserver
import androidx.compose.StructurallyEqual
import androidx.compose.frames.modelListOf
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.ui.animation.asDisposableClock
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.core.Constraints
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DrawModifier
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.foundation.Interaction
import androidx.ui.graphics.Color
import androidx.ui.graphics.useOrElse
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.center
import androidx.ui.unit.ipx
import androidx.ui.unit.toPxSize
import androidx.ui.util.fastForEach

/**
 * Ripple is a [Modifier] which draws the visual indicator for a pressed state.
 *
 * Ripple responds to a tap by starting a new ripple animation.
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded
 * ripples always animate from the target layout center, bounded ripples animate from the touch
 * position.
 * @param radius Effects grow up to this size. If null is provided the size would be calculated
 * based on the target layout size.
 * @param color The Ripple color is usually the same color used by the text or iconography in the
 * component. If [Color.Unset] is provided the color will be calculated by
 * [RippleTheme.defaultColor].
 * @param clock The animation clock observable that will drive this ripple effect
 * @param enabled The ripple effect will not start if false is provided.
 */
@Deprecated("Use Modifier.clickable to handle click events and show a ripple, or use " +
        "Modifier.indication directly with RippleIndication just to show a ripple. If you already" +
        " have a clickable / toggleable in this modifier chain, you can directly delete this " +
        "ripple without replacement.")
fun Modifier.ripple(
    bounded: Boolean = true,
    radius: Dp? = null,
    color: Color = Color.Unset,
    enabled: Boolean = true,
    clock: AnimationClockObservable? = null
): Modifier = composed {
    @Suppress("NAME_SHADOWING") // don't allow usage of the parameter clock, only the disposable
    val clock = (clock ?: AnimationClockAmbient.current).asDisposableClock()
    val density = DensityAmbient.current
    val rippleModifier = remember { RippleModifier() }
    val theme = RippleThemeAmbient.current
    val rippleColor = color.useOrElse { theme.defaultColor() }
    val rippleAlpha = theme.rippleOpacity().opacityForInteraction(Interaction.Pressed)
    rippleModifier.color = rippleColor.copy(alpha = rippleAlpha)

    val pressIndicator = Modifier.pressIndicatorGestureFilter(
        onStart = { position ->
            if (enabled && transitionsEnabled) {
                rippleModifier.handleStart(position, density, bounded, radius, clock)
            }
        },
        onStop = { rippleModifier.handleFinish() },
        onCancel = { rippleModifier.handleFinish() }
    )
    pressIndicator + rippleModifier
}

private class RippleModifier : DrawModifier, LayoutModifier, CompositionLifecycleObserver {

    var color: Color by mutableStateOf(Color.Transparent, StructurallyEqual)

    private var size: IntPxSize = IntPxSize(0.ipx, 0.ipx)
    private var effects = modelListOf<RippleAnimation>()
    private var currentEffect: RippleAnimation? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(constraints)
        size = IntPxSize(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            placeable.place(0.ipx, 0.ipx)
        }
    }

    fun handleStart(
        touchPosition: PxPosition,
        density: Density,
        bounded: Boolean,
        radius: Dp?,
        clock: AnimationClockObservable
    ) {
        val position = if (bounded) touchPosition else size.toPxSize().center()
        val onAnimationFinished = { effect: RippleAnimation ->
            effects.remove(effect)
            if (currentEffect == effect) {
                currentEffect = null
            }
        }
        val targetRadius = with(density) {
            radius?.toPx() ?: getRippleEndRadius(bounded, PxSize(size.width, size.height))
        }

        val effect = RippleAnimation(
            size.toPxSize(),
            position,
            targetRadius,
            bounded,
            clock,
            onAnimationFinished
        )

        effects.add(effect)
        currentEffect = effect
    }

    fun handleFinish() {
        currentEffect?.finish()
        currentEffect = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        effects.fastForEach {
            with(it) {
                draw(color)
            }
        }
    }

    override fun onEnter() {
        // do nothing
    }

    override fun onLeave() {
        effects.clear()
        currentEffect = null
    }
}
