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

import androidx.ui.core.Bounds
import androidx.ui.core.Density
import androidx.ui.core.Dp
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnPositioned
import androidx.ui.core.Position
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BoxShape
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.composer

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
class Ripple(
    @Children var children: () -> Unit
) : Component() {
    /**
     * Called when this surface either becomes highlighted or stops being highlighted.
     *
     * The value passed to the callback is true if this part of the surface has
     * become highlighted and false if this part of the surface has stopped
     * being highlighted.
     */
    var onHighlightChanged: ((Boolean) -> Unit)? = null
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
     *  * [clippingBorderRadius], which controls the corners when the box is a RECTANGLE.
     *  * [boundsCallback], which controls the size and position of the box when
     *    it is a RECTANGLE.
     */
    var bounded: Boolean = false
    /**
     * The shape (e.g., CIRCLE, RECTANGLE) to use for the highlight drawn around
     * this surface.
     *
     * If the shape is [BoxShape.CIRCLE], then the highlight is centered on the
     * [Ripple]. If the shape is [BoxShape.RECTANGLE], then the highlight
     * fills the [Ripple], or the RECTANGLE provided by [boundsCallback] if
     * the callback is specified.
     *
     * See also:
     *
     *  * [bounded], which controls clipping behavior.
     *  * [clippingBorderRadius], which controls the corners when the box is a RECTANGLE.
     *  * [boundsCallback], which controls the size and position of the box when
     *    it is a RECTANGLE.
     */
    var shape: BoxShape = BoxShape.CIRCLE
    /**
     * The radius of the Ripple.
     *
     * Effects grow up to this size. By default, this size is determined from
     * the size of the RECTANGLE provided by [boundsCallback], or the size of
     * the [Ripple] itself.
     */
    var finalRadius: Px? = null
    /**
     * The clipping radius of the containing rect.
     *
     * If this is null, it is interpreted as [BorderRadius.Zero].
     */
    var clippingBorderRadius: BorderRadius? = null
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
    var boundsCallback: ((LayoutCoordinates) -> PxBounds)? = null

    private var effects = mutableSetOf<RippleEffect>()
    private var currentEffect: RippleEffect? = null

    // they will be assigned during the composition.
    private lateinit var rippleSurface: RippleSurfaceOwner
    private lateinit var coordinates: LayoutCoordinates
    private lateinit var theme: RippleTheme
    private lateinit var density: Density

    private fun createRippleEffect(position: PxPosition): RippleEffect {
        val boundsCallback = if (bounded) boundsCallback else null
        val borderRadius = clippingBorderRadius
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
            boundsCallback,
            borderRadius,
            onRemoved
        )

        return effect
    }

    private fun handleStart(position: PxPosition) {
        val effect = createRippleEffect(position)
        effects.add(effect)
        currentEffect = effect
    }

    private fun handleFinish(canceled: Boolean) {
        currentEffect?.finish(canceled)
        currentEffect = null
        onHighlightChanged?.invoke(false)
    }

    // TODO("Andrey: Needs onDispose effect in R4a. b/124500412")
//    override fun deactivate() {
//        effects.forEach { it.dispose() }
//        currentEffect = null
//    }

    override fun compose() {
        <DensityConsumer> density ->
            this.density = density
        </DensityConsumer>

        // TODO: Rewrite this with use of +state effect. b/124500412
        <OnPositioned> coordinates ->
            this.coordinates = coordinates
        </OnPositioned>

        <RippleSurfaceConsumer> rippleSurface ->
            this.rippleSurface = rippleSurface
        </RippleSurfaceConsumer>

        <CurrentRippleTheme.Consumer> theme ->
            this.theme = theme
            currentEffect?.color = theme.colorCallback.invoke(rippleSurface.backgroundColor)
        </CurrentRippleTheme.Consumer>

        <PressIndicatorGestureDetector
            onStart=::handleStart
            onStop={ handleFinish(false) }
            onCancel={ handleFinish(true) }>
            <children />
        </PressIndicatorGestureDetector>
    }
}
