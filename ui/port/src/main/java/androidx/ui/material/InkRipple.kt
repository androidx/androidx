/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.material

import androidx.ui.VoidCallback
import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationController
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.Curves
import androidx.ui.animation.Interval
import androidx.ui.animation.Tween
import androidx.ui.animation.animations.CurvedAnimation
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.material.material.RectCallback
import androidx.ui.material.material.RenderInkFeatures
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.borderradius.BorderRadius
import androidx.ui.painting.matrixutils.getAsTranslation
import androidx.ui.rendering.box.RenderBox
import androidx.ui.vectormath64.Matrix4

internal val UnconfirmedRippleDuration = Duration.create(seconds = 1)
internal val FadeInDuration = Duration.create(milliseconds = 75)
internal val RadiusDuration = Duration.create(milliseconds = 225)
internal val FadeOutDuration = Duration.create(milliseconds = 375)
internal val CancelDuration = Duration.create(milliseconds = 75)

// The fade out begins 225ms after the _fadeOutController starts. See confirm().
private val FadeOutIntervalStart = 225.0 / 375.0

internal fun getRippleClipCallback(
    referenceBox: RenderBox,
    containedInkWell: Boolean,
    rectCallback: RectCallback?
): RectCallback? {
    if (rectCallback != null) {
        assert(containedInkWell)
        return rectCallback
    }
    if (containedInkWell)
        return { Offset.zero and referenceBox.size }
    return null
}

internal fun getRippleTargetRadius(
    referenceBox: RenderBox,
    rectCallback: RectCallback?
): Double {
    val size = rectCallback?.invoke()?.getSize() ?: referenceBox.size
    val d1 = size.bottomRight(Offset.zero).getDistance()
    val d2 = (size.topRight(Offset.zero) - size.bottomLeft(Offset.zero)).getDistance()
    return Math.max(d1, d2) / 2.0
}

private class InkRippleFactory : InteractiveInkFeatureFactory() {

    override fun create(
        controller: RenderInkFeatures,
        referenceBox: RenderBox,
        position: Offset,
        color: Color,
        containedInkWell: Boolean,
        rectCallback: RectCallback?,
        borderRadius: BorderRadius?,
        radius: Double?,
        onRemoved: VoidCallback?
    ): InteractiveInkFeature {
        return InkRipple(
            controller = controller,
            referenceBox = referenceBox,
            position = position,
            color = color,
            containedInkWell = containedInkWell,
            rectCallback = rectCallback,
            borderRadius = borderRadius,
            radiusParam = radius,
            onRemoved = onRemoved
        )
    }
}

/**
 * A visual reaction on a piece of [Material] to user input.
 *
 * A circular ink feature whose origin starts at the input touch point and
 * whose radius expands from 60% of the final radius. The splash origin
 * animates to the center of its [referenceBox].
 *
 * This object is rarely created directly. Instead of creating an ink ripple,
 * consider using an [InkResponse] or [InkWell] widget, which uses
 * gestures (such as tap and long-press) to trigger ink splashes. This class
 * is used when the [Theme]'s [ThemeData.splashType] is [InkSplashType.ripple].
 *
 * See also:
 *
 *  * [InkSplash], which is an ink splash feature that expands less
 *    aggressively than the ripple.
 *  * [InkResponse], which uses gestures to trigger ink highlights and ink
 *    splashes in the parent [Material].
 *  * [InkWell], which is a rectangular [InkResponse] (the most common type of
 *    ink response).
 *  * [Material], which is the widget on which the ink splash is painted.
 *  * [InkHighlight], which is an ink feature that emphasizes a part of a
 *    [Material].
 *
 * Begin a ripple, centered at [position] relative to [referenceBox].
 *
 * The [controller] argument is typically obtained via
 * `Material.of(context)`.
 *
 * If [containedInkWell] is true, then the ripple will be sized to fit
 * the well RECTANGLE, then clipped to it when drawn. The well
 * RECTANGLE is the box returned by [rectCallback], if provided, or
 * otherwise is the bounds of the [referenceBox].
 *
 * If [containedInkWell] is false, then [rectCallback] should be null.
 * The ink ripple is clipped only to the edges of the [Material].
 * This is the default.
 *
 * When the ripple is removed, [onRemoved] will be called.
 */
class InkRipple(
    controller: RenderInkFeatures,
    referenceBox: RenderBox,
    private val position: Offset,
    color: Color,
    containedInkWell: Boolean = false,
    rectCallback: RectCallback? = null,
    borderRadius: BorderRadius? = null,
    radiusParam: Double? = null,
    onRemoved: VoidCallback? = null
) : InteractiveInkFeature(controller, referenceBox, color, onRemoved) {

    private val borderRadius: BorderRadius = borderRadius ?: BorderRadius.Zero
    private val targetRadius: Double =
        radiusParam ?: getRippleTargetRadius(referenceBox, rectCallback)
    private val clipCallback: RectCallback? =
        getRippleClipCallback(referenceBox, containedInkWell, rectCallback)

    private val radius: Animation<Double>
    private val radiusController: AnimationController

    private val fadeIn: Animation<Int>
    private val fadeInController: AnimationController

    private val fadeOut: Animation<Int>
    private val fadeOutController: AnimationController

    init {
        // Immediately begin fading-in the initial splash.
        fadeInController = AnimationController(
            duration = FadeInDuration,
            vsync = controller.vsync
        ).apply {
            addListener {
                controller.markNeedsPaint()
            }
            forward()
        }
        fadeIn = Tween(
            begin = 0,
            end = color.alpha
        ).animate(fadeInController)

        // Controls the splash radius and its center. Starts upon confirm.
        radiusController = AnimationController(
            duration = UnconfirmedRippleDuration,
            vsync = controller.vsync
        ).apply {
            addListener { controller.markNeedsPaint() }
            forward()
        }
        // Initial splash diameter is 60% of the target diameter, final
        // diameter is 10dps larger than the target diameter.
        radius = Tween(
            begin = targetRadius * 0.30,
            end = targetRadius + 5.0
        ).animate(
            CurvedAnimation(
                parent = radiusController,
                curve = Curves.ease
            )
        )

        // Controls the splash radius and its center. Starts upon confirm however its
        // Interval delays changes until the radius expansion has completed.
        fadeOutController = AnimationController(
            duration = FadeOutDuration,
            vsync = controller.vsync
        ).apply {
            addListener { controller.markNeedsPaint() }
            addStatusListener(this@InkRipple::handleAlphaStatusChanged)
            forward()
        }
        fadeOut = Tween(
            begin = color.alpha,
            end = 0
        ).animate(
            CurvedAnimation(
                parent = fadeOutController,
                curve = Interval(FadeOutIntervalStart, 1.0)
            )
        )

        controller.addInkFeature(this)
    }

    override fun confirm() {
        radiusController.duration = RadiusDuration
        radiusController.forward()
        // This confirm may have been preceded by a cancel.
        fadeInController.forward()
        fadeOutController.animateTo(1.0, duration = FadeOutDuration)
    }

    override fun cancel() {
        fadeInController.stop()
        // Watch out: setting _fadeOutController's value to 1.0 will
        // trigger a call to _handleAlphaStatusChanged() which will
        // dispose _fadeOutController.
        val fadeOutValue = 1.0 - fadeInController.value
        fadeOutController.value = fadeOutValue
        if (fadeOutValue < 1.0) {
            fadeOutController.animateTo(1.0, duration = CancelDuration)
        }
    }

    private fun handleAlphaStatusChanged(status: AnimationStatus) {
        if (status == AnimationStatus.COMPLETED) {
            dispose()
        }
    }

    override fun dispose() {
        radiusController.dispose()
        fadeInController.dispose()
        fadeOutController.dispose()
        super.dispose()
    }

    private fun clipRRectFromRect(rect: Rect): RRect {
        return RRect(
            rect,
            topLeft = borderRadius.topLeft, topRight = borderRadius.topRight,
            bottomLeft = borderRadius.bottomLeft, bottomRight = borderRadius.bottomRight
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

    override fun paintFeature(canvas: Canvas, transform: Matrix4) {
        val alpha = if (fadeInController.isAnimating) fadeIn.value else fadeOut.value
        val paint = Paint().apply { color = this@InkRipple.color.withAlpha(alpha) }
        // Splash moves to the center of the reference box.
        val center = Offset.lerp(
            position,
            referenceBox.size.center(Offset.zero),
            Curves.ease.transform(radiusController.value)
        )!!
        val originOffset = transform.getAsTranslation()
        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            if (clipCallback != null) {
                clipCanvasWithRect(canvas, clipCallback.invoke())
            }
            canvas.drawCircle(center, radius.value, paint)
            canvas.restore()
        } else {
            if (clipCallback != null) {
                canvas.save()
                clipCanvasWithRect(canvas, clipCallback.invoke(), offset = originOffset)
            }
            canvas.drawCircle(center + originOffset, radius.value, paint)
            if (clipCallback != null) {
                canvas.restore()
            }
        }
    }

    companion object {

        /**
         * Used to specify this type of ink splash for an [InkWell], [InkResponse]
         * or material [Theme].
         */
        val SplashFactory: InteractiveInkFeatureFactory = InkRippleFactory()
    }
}
