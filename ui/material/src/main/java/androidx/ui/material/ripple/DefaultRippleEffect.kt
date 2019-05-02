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

import androidx.animation.FastOutSlowInEasing
import androidx.animation.FloatPropKey
import androidx.animation.InterruptionHandling
import androidx.animation.LinearEasing
import androidx.animation.PxPositionPropKey
import androidx.animation.PxPropKey
import androidx.animation.TransitionAnimation
import androidx.animation.transitionDefinition
import androidx.ui.core.Density
import androidx.ui.core.DensityReceiver
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Px
import androidx.ui.core.PxBounds
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.center
import androidx.ui.core.dp
import androidx.ui.core.getDistance
import androidx.ui.core.inMilliseconds
import androidx.ui.core.max
import androidx.ui.core.milliseconds
import androidx.ui.core.toBounds
import androidx.ui.core.toOffset
import androidx.ui.core.toRect
import androidx.ui.core.toSize
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BoxShape
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Canvas
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.getAsTranslation

internal fun getRippleClipCallback(
    containedInkWell: Boolean,
    boundsCallback: ((LayoutCoordinates) -> PxBounds)?
): ((LayoutCoordinates) -> PxBounds)? {
    if (boundsCallback != null) {
        assert(containedInkWell)
        return boundsCallback
    }
    if (containedInkWell) {
        return { it.size.toBounds() }
    }
    return null
}

internal fun getSurfaceSize(
    coordinates: LayoutCoordinates,
    boundsCallback: ((LayoutCoordinates) -> PxBounds)?
) = boundsCallback?.invoke(coordinates)?.toSize() ?: coordinates.size

internal fun getRippleStartRadius(size: PxSize) =
    max(size.width, size.height) * 0.3f

internal fun DensityReceiver.getRippleTargetRadius(size: PxSize) =
    PxPosition(size.width, size.height).getDistance() / 2f + 10.dp.toPx()

/**
 * Used to specify this type of [RippleEffect] for an [BoundedRipple] and [Ripple].
 */
object DefaultRippleEffectFactory : RippleEffectFactory() {

    override fun create(
        rippleSurface: RippleSurfaceOwner,
        coordinates: LayoutCoordinates,
        touchPosition: PxPosition,
        color: Color,
        density: Density,
        shape: BoxShape,
        finalRadius: Px?,
        containedInkWell: Boolean,
        boundsCallback: ((LayoutCoordinates) -> PxBounds)?,
        clippingBorderRadius: BorderRadius?,
        onRemoved: (() -> Unit)?
    ): RippleEffect {
        return DefaultRippleEffect(
            rippleSurface,
            coordinates,
            touchPosition,
            color,
            density,
            finalRadius,
            containedInkWell,
            boundsCallback,
            clippingBorderRadius,
            onRemoved
        )
    }
}

/**
 * A visual reaction on a piece of [RippleSurface] to user input.
 *
 * A circular ripple effect whose origin starts at the input touch point and
 * whose finalRadius expands from 60% of the final finalRadius. The ripple origin
 * animates to the center of its target layout.
 *
 * This object is rarely created directly. Instead of creating a ripple effect,
 * consider using an [Ripple] or [BoundedRipple].
 *
 * See also:
 *
 *  * [Ripple], which draws [RippleEffect]s in the parent [RippleSurface].
 *  * [BoundedRipple], which is a rectangular [Ripple] (the most common type of
 *    ripple).
 *  * [RippleSurface], which is the widget on which the ripple effect is drawn.
 *
 * Begin a ripple, centered at [touchPosition] relative to the target layout.
 *
 * If "bounded" is true, then the ripple will be sized to fit the bounds, then
 * clipped to it when drawn. The bounds are returned by `boundsCallback`, if provided, or
 * otherwise is the bounds of the target layout.
 *
 * If "bounded" is false, then "boundsCallback" should be null.
 * The ripple is clipped only to the edges of the [Surface]. This is the default.
 *
 * When the ripple is removed, [onRemoved] will be called.
 */
internal class DefaultRippleEffect(
    rippleSurface: RippleSurfaceOwner,
    coordinates: LayoutCoordinates,
    private val touchPosition: PxPosition,
    color: Color,
    density: Density,
    finalRadius: Px? = null,
    containedInkWell: Boolean = false,
    boundsCallback: ((LayoutCoordinates) -> PxBounds)? = null,
    clippingBorderRadius: BorderRadius? = null,
    onRemoved: (() -> Unit)? = null
) : RippleEffect(rippleSurface, coordinates, color, onRemoved) {

    private val borderRadius: BorderRadius =
        clippingBorderRadius ?: BorderRadius.Zero
    private val clipCallback: ((LayoutCoordinates) -> PxBounds)? =
        getRippleClipCallback(containedInkWell, boundsCallback)
    private val animation: TransitionAnimation<RippleTransition.State>
    private var transitionState = RippleTransition.State.Initial
    private var finishRequested = false

    init {
        val surfaceSize = getSurfaceSize(coordinates, boundsCallback)
        val startRadius = getRippleStartRadius(surfaceSize)
        val targetRadius = finalRadius ?: withDensity(density) {
            getRippleTargetRadius(surfaceSize)
        }

        animation = RippleTransition.definition(
            revealedAlpha = color.alpha,
            startRadius = startRadius,
            endRadius = targetRadius,
            startCenter = touchPosition,
            endCenter = coordinates.size.center()
        ).createAnimation()
        animation.onUpdate = { rippleSurface.markNeedsRedraw() }
        animation.onStateChangeFinished = { stage ->
            transitionState = stage
            if (transitionState == RippleTransition.State.Finished) {
                dispose()
            }
        }
        // currently we are in Initial state, now we start the animation:
        animation.toState(RippleTransition.State.Revealed)

        rippleSurface.addEffect(this)
    }

    override fun finish(canceled: Boolean) {
        finishRequested = true
        animation.toState(RippleTransition.State.Finished)
    }

    private fun clipRRectFromRect(rect: Rect): RRect {
        return RRect(
            rect,
            topLeft = borderRadius.topLeft,
            topRight = borderRadius.topRight,
            bottomLeft = borderRadius.bottomLeft,
            bottomRight = borderRadius.bottomRight
        )
    }

    private fun clipCanvasWithRect(canvas: Canvas, rect: Rect, offset: Offset? = null) {
        var clipRect = rect
        if (offset != null) {
            clipRect = clipRect.shift(offset)
        }
        if (borderRadius != BorderRadius.Zero) {
            canvas.clipRRect(clipRRectFromRect(clipRect))
        } else {
            canvas.clipRect(clipRect)
        }
    }

    override fun drawEffect(canvas: Canvas, transform: Matrix4) {
        val alpha = if (transitionState == RippleTransition.State.Initial && finishRequested) {
            // if we still fading-in we should immediately switch to the final alpha.
            color.alpha
        } else {
            animation[RippleTransition.Alpha]
        }
        val radius = animation[RippleTransition.Radius].value
        val centerOffset = animation[RippleTransition.Center].toOffset()
        val paint = Paint()
        paint.color = color.copy(alpha = alpha)
        val originOffset = transform.getAsTranslation()
        val clipRect = clipCallback?.invoke(coordinates)?.toRect()
        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            if (clipRect != null) {
                clipCanvasWithRect(canvas, clipRect)
            }
            canvas.drawCircle(centerOffset, radius, paint)
            canvas.restore()
        } else {
            if (clipRect != null) {
                canvas.save()
                clipCanvasWithRect(canvas, clipRect, offset = originOffset)
            }
            canvas.drawCircle(centerOffset + originOffset, radius, paint)
            if (clipRect != null) {
                canvas.restore()
            }
        }
    }
}

/**
 * The Ripple transition specification.
 */
private object RippleTransition {

    enum class State {
        /** The starting state.  */
        Initial,
        /** User is still touching the surface.  */
        Revealed,
        /** User stopped touching the surface.  */
        Finished
    }

    private val FadeInDuration = 75.milliseconds
    private val RadiusDuration = 225.milliseconds
    private val FadeOutDuration = 150.milliseconds

    val Alpha = FloatPropKey()
    val Radius = PxPropKey()
    val Center = PxPositionPropKey()

    fun definition(
        revealedAlpha: Float,
        startRadius: Px,
        endRadius: Px,
        startCenter: PxPosition,
        endCenter: PxPosition
    ) = transitionDefinition {
        state(State.Initial) {
            this[Alpha] = 0f
            this[Radius] = startRadius
            this[Center] = startCenter
        }
        state(State.Revealed) {
            this[Alpha] = revealedAlpha
            this[Radius] = endRadius
            this[Center] = endCenter
        }
        state(State.Finished) {
            this[Alpha] = 0f
            // the rest are the same as for Revealed
            this[Radius] = endRadius
            this[Center] = endCenter
        }
        transition(State.Initial to State.Revealed) {
            Alpha using tween {
                duration = FadeInDuration.inMilliseconds().toInt()
                easing = LinearEasing
            }
            Radius using tween {
                duration = RadiusDuration.inMilliseconds().toInt()
                easing = FastOutSlowInEasing
            }
            Center using tween {
                duration = RadiusDuration.inMilliseconds().toInt()
                easing = LinearEasing
            }
            // we need to always finish the radius animation before starting fading out
            interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
        }
        transition(State.Revealed to State.Finished) {
            fun <T> toFinished() = tween<T> {
                duration = FadeOutDuration.inMilliseconds().toInt()
                easing = LinearEasing
            }
            Alpha using toFinished()
            Radius using toFinished()
            Center using toFinished()
        }
    }
}
