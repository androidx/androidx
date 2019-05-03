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
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.core.ambientDensity
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BoxShape
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.onDispose
import androidx.compose.unaryPlus

/**
 * An area of a [RippleSurface] that responds to touch. Has a configurable shape and
 * can be configured to clip effects that extend outside its bounds or not.
 *
 * For a variant of this widget that is specialized for rectangular areas that
 * always clip effects, see [BoundedRipple].
 *
 * An [Ripple] widget responds to a tap by starting a current [RippleEffect]'s
 * animation. For creating an effect it uses the [RippleTheme.factory].
 *
 * The [Ripple] widget must have a [RippleSurface] ancestor. The
 * [RippleSurface] is where the [Ripple]s are actually drawn.
 */
@Composable
fun Ripple(
    /**
     * Called when this surface either becomes highlighted or stops being highlighted.
     *
     * The value passed to the callback is true if this part of the surface has
     * become highlighted and false if this part of the surface has stopped
     * being highlighted.
     */
    onHighlightChanged: ((Boolean) -> Unit)? = null,
    /**
     * Whether this ripple should be bounded.
     *
     * This flag also controls whether the ripple migrates to the center of the
     * [Ripple] or not. If [bounded] is true, the ripple remains centered around
     * the tap location. If it is false, the effect migrates to the center of
     * the [Ripple] as it grows.
     *
     * See also:
     *
     *  * [shape], which determines the shape of the ripple.
     *  * [clippingBorderRadius], which controls the corners when the box is a rectangle.
     *  * [boundsCallback], which controls the size and position of the box when
     *    it is a rectangle.
     */
    bounded: Boolean = false,
    /**
     * The shape (e.g., circle, rectangle) to use for the highlight drawn around
     * this surface.
     *
     * If the shape is [BoxShape.Circle], then the highlight is centered on the
     * [Ripple]. If the shape is [BoxShape.Rectangle], then the highlight
     * fills the [Ripple], or the rectangle provided by [boundsCallback] if
     * the callback is specified.
     *
     * See also:
     *
     *  * [bounded], which controls clipping behavior.
     *  * [clippingBorderRadius], which controls the corners when the box is a rectangle.
     *  * [boundsCallback], which controls the size and position of the box when
     *    it is a rectangle.
     */
    shape: BoxShape = BoxShape.Circle,
    /**
     * The radius of the Ripple.
     *
     * Effects grow up to this size. By default, this size is determined from
     * the size of the rectangle provided by [boundsCallback], or the size of
     * the [Ripple] itself.
     */
    finalRadius: Px? = null,
    /**
     * The clipping radius of the containing rect.
     *
     * If this is null, it is interpreted as [BorderRadius.Zero].
     */
    clippingBorderRadius: BorderRadius? = null,
    /**
     * The bounds to use for the highlight effect and for clipping
     * the ripple effects if [bounded] is true.
     *
     * This function is intended to be provided for unusual cases.
     * For example, you can provide this for Table layouts to return
     * the bounds corresponding to the row that the item is in.
     *
     * The default value is null, which is equivalent to
     * returning the target layout argument's bounding box (though
     * slightly more efficient).
     */
    boundsCallback: ((LayoutCoordinates) -> PxBounds)? = null,
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
                state.handleStart(
                    position, rippleSurface, theme, density, bounded, boundsCallback,
                    clippingBorderRadius, shape, finalRadius
                )
            },
            onStop = { state.handleFinish(false, onHighlightChanged) },
            onCancel = { state.handleFinish(true, onHighlightChanged) }) {
            children()
        }
    }

    +onDispose {
        state.effects.forEach { it.dispose() }
        state.effects.clear()
        state.currentEffect = null
    }
}

internal class RippleState {

    internal var coordinates: LayoutCoordinates? = null
    internal var effects = mutableSetOf<RippleEffect>()
    internal var currentEffect: RippleEffect? = null

    internal fun handleStart(
        position: PxPosition,
        rippleSurface: RippleSurfaceOwner,
        theme: RippleTheme,
        density: Density,
        bounded: Boolean,
        boundsCallback: ((LayoutCoordinates) -> PxBounds)?,
        borderRadius: BorderRadius?,
        shape: BoxShape,
        finalRadius: Px?
    ) {
        val coordinates = coordinates ?: throw IllegalStateException(
            "handleStart() called before the layout coordinates were provided!"
        )
        val callback = if (bounded) boundsCallback else null
        val color = theme.colorCallback.invoke(rippleSurface.backgroundColor)
        var effect: RippleEffect? = null
        val onRemoved = {
            val contains = effects.remove(effect)
            assert(contains)
            if (currentEffect == effect) {
                currentEffect = null
            }
        }

        effect = theme.factory.create(
            rippleSurface,
            coordinates,
            position,
            color,
            density,
            shape,
            finalRadius,
            bounded,
            callback,
            borderRadius,
            onRemoved
        )

        effects.add(effect)
        currentEffect = effect
    }

    internal fun handleFinish(canceled: Boolean, onHighlightChanged: ((Boolean) -> Unit)?) {
        currentEffect?.finish(canceled)
        currentEffect = null
        onHighlightChanged?.invoke(false)
    }
}
