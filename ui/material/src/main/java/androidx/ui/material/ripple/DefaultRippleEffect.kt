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

import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationController
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.Curves
import androidx.ui.animation.Interval
import androidx.ui.animation.Tween
import androidx.ui.animation.animations.CurvedAnimation
import androidx.ui.core.Bounds
import androidx.ui.core.Density
import androidx.ui.core.Dimension
import androidx.ui.core.Duration
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Position
import androidx.ui.core.Size
import androidx.ui.core.center
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.getDistance
import androidx.ui.core.lerp
import androidx.ui.core.plus
import androidx.ui.core.times
import androidx.ui.core.toBounds
import androidx.ui.core.toPx
import androidx.ui.core.toRect
import androidx.ui.core.toSize
import androidx.ui.engine.geometry.BorderRadius
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.material.Tween
import androidx.ui.material.borders.BoxShape
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.vectormath64.getAsTranslation
import androidx.ui.vectormath64.Matrix4

// TODO(Andrey) Implement the animation from the current specification: b/124504971

internal val UnconfirmedRippleDuration = Duration.create(seconds = 1)
internal val FadeInDuration = Duration.create(milliseconds = 75)
internal val RadiusDuration = Duration.create(milliseconds = 225)
internal val FadeOutDuration = Duration.create(milliseconds = 375)
internal val CancelDuration = Duration.create(milliseconds = 75)
internal val HighlightFadeDuration = Duration.create(milliseconds = 200)
internal val DefaultSplashRadius = 35.dp

// The fade out begins 225ms after the fadeOutController starts. See confirm().
private val FadeOutIntervalStart = 225f / 375f

internal fun getRippleClipCallback(
    containedInkWell: Boolean,
    boundsCallback: ((LayoutCoordinates) -> Bounds)?
): ((LayoutCoordinates) -> Bounds)? {
    if (boundsCallback != null) {
        assert(containedInkWell)
        return boundsCallback
    }
    if (containedInkWell) {
        return { it.size.toBounds() }
    }
    return null
}

internal fun getRippleTargetRadius(
    coordinates: LayoutCoordinates,
    boundsCallback: ((LayoutCoordinates) -> Bounds)?
): Dimension {
    val size: Size = boundsCallback?.invoke(coordinates)?.toSize() ?: coordinates.size
    return Position(size.width, size.height).getDistance() / 2f
}

/**
 * Used to specify this type of [RippleEffect] for an [BoundedRipple] and [Ripple].
 */
object DefaultRippleEffectFactory : RippleEffectFactory() {

    override fun create(
        rippleSurface: RippleSurfaceOwner,
        coordinates: LayoutCoordinates,
        touchPosition: Position,
        color: Color,
        shape: BoxShape,
        finalRadius: Dimension?,
        containedInkWell: Boolean,
        boundsCallback: ((LayoutCoordinates) -> Bounds)?,
        clippingBorderRadius: BorderRadius?,
        onRemoved: (() -> Unit)?
    ): RippleEffect {
        return DefaultRippleEffect(
            rippleSurface,
            coordinates,
            touchPosition,
            color,
            shape,
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
    private val touchPosition: Position,
    color: Color,
    private val shape: BoxShape = BoxShape.RECTANGLE,
    finalRadius: Dimension? = null,
    containedInkWell: Boolean = false,
    boundsCallback: ((LayoutCoordinates) -> Bounds)? = null,
    clippingBorderRadius: BorderRadius? = null,
    onRemoved: (() -> Unit)? = null
) : RippleEffect(rippleSurface, coordinates, color, onRemoved) {

    private val borderRadius: BorderRadius = clippingBorderRadius ?: BorderRadius.Zero
    private val targetRadius: Dimension =
        finalRadius ?: getRippleTargetRadius(coordinates, boundsCallback)
    private val clipCallback: ((LayoutCoordinates) -> Bounds)? =
        getRippleClipCallback(containedInkWell, boundsCallback)

    private val radius: Animation<Dimension>
    private val radiusController: AnimationController

    private val fadeIn: Animation<Int>
    private val fadeInController: AnimationController

    private val fadeOut: Animation<Int>
    private val fadeOutController: AnimationController

    private val highlightAlpha: Animation<Int>
    private val highlightAlphaController: AnimationController

    init {
        // Immediately begin fading-in the initial ripple.
        fadeInController = AnimationController(
            duration = FadeInDuration,
            vsync = rippleSurface.vsync
        )
        fadeInController.addListener(rippleSurface::markNeedsRedraw)
        fadeInController.forward()
        fadeIn = Tween(
            begin = 0,
            end = color.alpha
        ).animate(fadeInController)

        // Controls the ripple finalRadius and its center. Starts upon confirm.
        radiusController = AnimationController(
            duration = UnconfirmedRippleDuration,
            vsync = rippleSurface.vsync
        )
        radiusController.addListener(rippleSurface::markNeedsRedraw)
        radiusController.forward()
        // Initial ripple diameter is 60% of the target diameter, final
        // diameter is 10dps larger than the target diameter.
        this.radius = Tween(
            begin = targetRadius * 0.3f,
            end = targetRadius + 5.dp
        ).animate(
            CurvedAnimation(
                parent = radiusController,
                curve = Curves.ease
            )
        )

        // Controls the ripple finalRadius and its center. Starts upon confirm however its
        // Interval delays changes until the finalRadius expansion has completed.
        fadeOutController = AnimationController(
            duration = FadeOutDuration,
            vsync = rippleSurface.vsync
        )
        fadeOutController.addListener(rippleSurface::markNeedsRedraw)
        fadeOutController.forward()
        fadeOut = Tween(
            begin = color.alpha,
            end = 0
        ).animate(
            CurvedAnimation(
                parent = fadeOutController,
                curve = Interval(FadeOutIntervalStart, 1f)
            )
        )

        highlightAlphaController = AnimationController(
            duration = HighlightFadeDuration,
            vsync = rippleSurface.vsync
        )
        highlightAlphaController.addListener { rippleSurface.markNeedsRedraw() }
        highlightAlphaController.addStatusListener(this::handleAlphaStatusChanged)
        highlightAlphaController.forward()
        highlightAlpha = Tween(
            begin = 0,
            end = color.alpha / 2
        ).animate(highlightAlphaController)
        highlightAlphaController.forward()

        rippleSurface.addEffect(this)
    }

    override fun confirm() {
        radiusController.duration = RadiusDuration
        radiusController.forward()
        // This confirm may have been preceded by a cancel.
        fadeInController.forward()
        fadeOutController.animateTo(1f, duration = FadeOutDuration)
        highlightAlphaController.reverse()
    }

    override fun cancel() {
        fadeInController.stop()
        // Watch out: setting fadeOutController's value to 1.0 will
        // trigger a call to handleAlphaStatusChanged() which will
        // dispose fadeOutController.
        val fadeOutValue = 1f - fadeInController.value
        fadeOutController.value = fadeOutValue
        if (fadeOutValue < 1f) {
            fadeOutController.animateTo(1f, duration = CancelDuration)
        }
        highlightAlphaController.reverse()
    }

    private fun handleAlphaStatusChanged(status: AnimationStatus) {
        if (status == AnimationStatus.COMPLETED && highlightAlphaController.value == 0f) {
            dispose()
        }
    }

    override fun dispose() {
        radiusController.dispose()
        fadeInController.dispose()
        fadeOutController.dispose()
        highlightAlphaController.dispose()
        super.dispose()
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

    override fun drawEffect(canvas: Canvas, transform: Matrix4, density: Density) {
        val alpha = if (fadeInController.isAnimating) fadeIn.value else fadeOut.value
        val paint = Paint()
        paint.color = color.withAlpha(alpha)
        // Ripple moves to the center of the parent layout
        val center = lerp(
            touchPosition,
            coordinates.size.center(),
            Curves.ease.transform(radiusController.value)
        )
        val centerOffset = Offset(center.x.toPx(density), center.y.toPx(density))
        val originOffset = transform.getAsTranslation()
        val clipRect = clipCallback?.invoke(coordinates)?.toRect(density)
        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            if (clipRect != null) {
                clipCanvasWithRect(canvas, clipRect)
            }
            canvas.drawCircle(centerOffset, radius.value.toPx(density), paint)
            canvas.restore()
        } else {
            if (clipRect != null) {
                canvas.save()
                clipCanvasWithRect(canvas, clipRect, offset = originOffset)
            }
            canvas.drawCircle(centerOffset + originOffset, radius.value.toPx(density), paint)
            if (clipRect != null) {
                canvas.restore()
            }
        }

        // highlight
        paint.color = color.withAlpha(highlightAlpha.value)
        val bounds = clipCallback?.invoke(coordinates) ?: coordinates.size.toBounds()
        val rect = bounds.toRect(density)
        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            drawHighlight(canvas, rect, paint, density)
            canvas.restore()
        } else {
            drawHighlight(canvas, rect.shift(originOffset), paint, density)
        }
    }

    private fun drawHighlight(canvas: Canvas, rect: Rect, paint: Paint, density: Density) {
        when (shape) {
            BoxShape.CIRCLE ->
                canvas.drawCircle(rect.getCenter(), DefaultSplashRadius.toPx(density), paint)
            BoxShape.RECTANGLE -> {
                if (borderRadius != BorderRadius.Zero) {
                    val clipRRect = clipRRectFromRect(rect)
                    canvas.drawRRect(clipRRect, paint)
                } else {
                    canvas.drawRect(rect, paint)
                }
            }
        }
    }
}
