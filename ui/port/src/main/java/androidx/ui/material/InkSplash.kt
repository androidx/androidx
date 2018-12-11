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
import androidx.ui.animation.Tween
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.material.material.DefaultSplashRadius
import androidx.ui.material.material.Material
import androidx.ui.material.material.RectCallback
import androidx.ui.material.material.RenderInkFeatures
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.borderradius.BorderRadius
import androidx.ui.painting.matrixutils.getAsTranslation
import androidx.ui.rendering.box.RenderBox
import androidx.ui.vectormath64.Matrix4
import kotlin.math.ceil
import kotlin.math.floor

internal val UnconfirmedSplashDuration = Duration.create(seconds = 1)
internal val SplashFadeDuration = Duration.create(milliseconds = 200)

internal val SplashInitialSize = 0.0; // logical pixels
internal val SplashConfirmedVelocity = 1.0; // logical pixels per millisecond

internal fun getSplashClipCallback(
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

internal fun getSplashTargetRadius(
    referenceBox: RenderBox,
    containedInkWell: Boolean,
    rectCallback: RectCallback?,
    position: Offset
): Double {
    if (containedInkWell) {
        val size = rectCallback?.invoke()?.getSize() ?: referenceBox.size
        return getSplashRadiusForPositionInSize(size, position)
    }
    return DefaultSplashRadius
}

private fun getSplashRadiusForPositionInSize(bounds: Size, position: Offset): Double {
    val d1 = (position - bounds.topLeft(Offset.zero)).getDistance()
    val d2 = (position - bounds.topRight(Offset.zero)).getDistance()
    val d3 = (position - bounds.bottomLeft(Offset.zero)).getDistance()
    val d4 = (position - bounds.bottomRight(Offset.zero)).getDistance()
    return ceil(Math.max(Math.max(d1, d2), Math.max(d3, d4)))
}

private class InkSplashFactory : InteractiveInkFeatureFactory() {

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
        return InkSplash(
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
 * A circular ink feature whose origin starts at the input touch point
 * and whose radius expands from zero.
 *
 * This object is rarely created directly. Instead of creating an ink splash
 * directly, consider using an [InkResponse] or [InkWell] widget, which uses
 * gestures (such as tap and long-press) to trigger ink splashes.
 *
 * See also:
 *
 *  * [InkRipple], which is an ink splash feature that expands more
 *    aggressively than this class does.
 *  * [InkResponse], which uses gestures to trigger ink highlights and ink
 *    splashes in the parent [Material].
 *  * [InkWell], which is a rectangular [InkResponse] (the most common type of
 *    ink response).
 *  * [Material], which is the widget on which the ink splash is painted.
 *  * [InkHighlight], which is an ink feature that emphasizes a part of a
 *    [Material].
 *
 * Begin a splash, centered at position relative to [referenceBox].
 *
 * The [controller] argument is typically obtained via
 * `Material.of(context)`.
 *
 * If `containedInkWell` is true, then the splash will be sized to fit
 * the well RECTANGLE, then clipped to it when drawn. The well
 * RECTANGLE is the box returned by `rectCallback`, if provided, or
 * otherwise is the bounds of the [referenceBox].
 *
 * If `containedInkWell` is false, then `rectCallback` should be null.
 * The ink splash is clipped only to the edges of the [Material].
 * This is the default.
 *
 * When the splash is removed, `onRemoved` will be called.
 */
class InkSplash(
    controller: RenderInkFeatures,
    referenceBox: RenderBox,
    private val position: Offset,
    color: Color,
    containedInkWell: Boolean = false,
    rectCallback: RectCallback? = null,
    borderRadius: BorderRadius? = null,
    radiusParam: Double? = null,
    onRemoved: VoidCallback? = null
//    private val shape: BoxShape = BoxShape.RECTANGLE,
) : InteractiveInkFeature(controller, referenceBox, color, onRemoved) {

    private val borderRadius: BorderRadius = borderRadius ?: BorderRadius.Zero
    private val targetRadius: Double =
        radiusParam ?: getSplashTargetRadius(referenceBox, containedInkWell, rectCallback, position)
    private val clipCallback: RectCallback? =
        getSplashClipCallback(referenceBox, containedInkWell, rectCallback)
    private val repositionToReferenceBox: Boolean = !containedInkWell

    private val radius: Animation<Double>
    private val radiusController: AnimationController

    private val alpha: Animation<Int>
    private val alphaController: AnimationController

    init {
        radiusController = AnimationController(
            duration = UnconfirmedSplashDuration,
            vsync = controller.vsync
        ).apply {
            addListener { controller.markNeedsPaint() }
            forward()
        }
        radius = Tween(
            begin = SplashInitialSize,
            end = targetRadius
        ).animate(radiusController)

        alphaController = AnimationController(
            duration = SplashFadeDuration,
            vsync = controller.vsync
        ).apply {
            addListener { controller.markNeedsPaint() }
            addStatusListener(this@InkSplash::handleAlphaStatusChanged)
            forward()
        }
        alpha = Tween(
            begin = color.alpha,
            end = 0
        ).animate(alphaController)

        controller.addInkFeature(this)
    }

    override fun confirm() {
        super.confirm()
        val duration = floor(targetRadius / SplashConfirmedVelocity).toLong()
        radiusController.duration = Duration.create(milliseconds = duration)
        radiusController.forward()
        alphaController.forward()
    }

    override fun cancel() {
        alphaController.forward()
    }

    private fun handleAlphaStatusChanged(status: AnimationStatus) {
        if (status == AnimationStatus.COMPLETED) {
            dispose()
        }
    }

    override fun dispose() {
        radiusController.dispose()
        alphaController.dispose()
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
        val paint = Paint()
        paint.color = color.withAlpha(alpha.value)
        var center = position
        if (repositionToReferenceBox)
            center = Offset.lerp(
                center,
                referenceBox.size.center(Offset.zero),
                radiusController.value
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
        val SplashFactory: InteractiveInkFeatureFactory = InkSplashFactory()
    }
}
