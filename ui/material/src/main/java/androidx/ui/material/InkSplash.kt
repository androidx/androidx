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

package androidx.ui.material

import android.content.Context
import android.util.Log
import androidx.ui.VoidCallback
import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationController
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.Tween
import androidx.ui.core.Bounds
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
import androidx.ui.core.minus
import androidx.ui.core.toBounds
import androidx.ui.core.toPx
import androidx.ui.core.toSize
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.engine.geometry.BorderRadius
import androidx.ui.painting.matrixutils.getAsTranslation
import androidx.ui.vectormath64.Matrix4
import kotlin.math.ceil
import kotlin.math.floor

internal val UnconfirmedSplashDuration = Duration.create(seconds = 1)
internal val SplashFadeDuration = Duration.create(milliseconds = 200)

internal val SplashInitialSize = 0.dp // logical pixels
internal val SplashConfirmedVelocity = 1.dp // logical pixels per millisecond

internal fun getSplashClipCallback(
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

internal fun getSplashTargetRadius(
    coordinates: LayoutCoordinates,
    containedInkWell: Boolean,
    boundsCallback: ((LayoutCoordinates) -> Bounds)?,
    position: Position
): Dimension {
    if (containedInkWell) {
        val size = boundsCallback?.invoke(coordinates)?.toSize() ?: coordinates.size
        return getSplashRadiusForPositionInSize(size, position)
    }
    return DefaultSplashRadius
}

private fun getSplashRadiusForPositionInSize(size: Size, position: Position): Dimension {
    val d1 = (position - Position(0.dp, 0.dp)).getDistance()
    val d2 = (position - Position(size.width, 0.dp)).getDistance()
    val d3 = (position - Position(0.dp, size.height)).getDistance()
    val d4 = (position - Position(size.width, size.height)).getDistance()
    return Dimension(ceil(Math.max(Math.max(d1.dp, d2.dp), Math.max(d3.dp, d4.dp))))
}

/**
 * Used to specify this type of ink splash for an [InkWell], [InkResponse]
 * or material [Theme].
 */
object InkSplashFactory : InteractiveInkFeatureFactory() {

    override fun create(
        controller: MaterialInkController,
        coordinates: LayoutCoordinates,
        position: Position,
        color: Color,
        containedInkWell: Boolean,
        boundsCallback: ((LayoutCoordinates) -> Bounds)?,
        borderRadius: BorderRadius?,
        radius: Dimension?,
        onRemoved: VoidCallback?
    ): InteractiveInkFeature {
        return InkSplash(
            controller = controller,
            coordinates = coordinates,
            position = position,
            color = color,
            containedInkWell = containedInkWell,
            boundsCallback = boundsCallback,
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
 * Begin a splash, centered at position relative to the target layout.
 *
 * The [controller] argument is typically obtained via
 * `Material.of(context)`.
 *
 * If `containedInkWell` is true, then the splash will be sized to fit
 * the well RECTANGLE, then clipped to it when drawn. The well
 * RECTANGLE is the box returned by `boundsCallback`, if provided, or
 * otherwise is the bounds of the target layout.
 *
 * If `containedInkWell` is false, then `boundsCallback` should be null.
 * The ink splash is clipped only to the edges of the [Material].
 * This is the default.
 *
 * When the splash is removed, `onRemoved` will be called.
 */
class InkSplash(
    controller: MaterialInkController,
    coordinates: LayoutCoordinates,
    private val position: Position,
    color: Color,
    containedInkWell: Boolean = false,
    boundsCallback: ((LayoutCoordinates) -> Bounds)? = null,
    borderRadius: BorderRadius? = null,
    radiusParam: Dimension? = null,
    onRemoved: VoidCallback? = null
) : InteractiveInkFeature(controller, coordinates, color, onRemoved) {

    private val borderRadius: BorderRadius = borderRadius ?: BorderRadius.Zero
    private val targetRadius: Dimension =
        radiusParam ?: getSplashTargetRadius(
            coordinates,
            containedInkWell,
            boundsCallback,
            position
        )
    private val clipCallback: ((LayoutCoordinates) -> Bounds)? =
        getSplashClipCallback(containedInkWell, boundsCallback)
    private val repositionToTargetLayout: Boolean = !containedInkWell

    private val radius: Animation<Dimension>
    private val radiusController: AnimationController

    private val alpha: Animation<Int>
    private val alphaController: AnimationController

    init {
        radiusController = AnimationController(
            duration = UnconfirmedSplashDuration,
            vsync = controller.vsync
        )
        radiusController.addListener(controller::markNeedsPaint)
        radiusController.forward()
        radius = Tween(
            begin = SplashInitialSize,
            end = targetRadius
        ).animate(radiusController)
        Log.e("asdasd", " " + SplashInitialSize + " " + targetRadius)

        alphaController = AnimationController(
            duration = SplashFadeDuration,
            vsync = controller.vsync
        )
        alphaController.addListener(controller::markNeedsPaint)
        alphaController.addStatusListener(this::handleAlphaStatusChanged)
        alphaController.forward()
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

    override fun paintFeature(canvas: Canvas, transform: Matrix4, context: Context) {
        val paint = Paint()
        paint.color = color.withAlpha(alpha.value)
        var center = position
        if (repositionToTargetLayout) {
            center = lerp(
                center,
                coordinates.size.center(),
                radiusController.value
            )
        }
        val centerOffset =
            Offset(center.x.toPx(context), center.y.toPx(context))
        val originOffset = transform.getAsTranslation()
        val radiusDouble = radius.value.toPx(context)
        val clipBounds: Bounds? = clipCallback?.invoke(coordinates)
        val clipRect = if (clipBounds == null) null else Rect(
            clipBounds.left.toPx(context),
            clipBounds.top.toPx(context),
            clipBounds.right.toPx(context),
            clipBounds.bottom.toPx(context)
        )
        Log.e("asdasd", " " + radius.value + " " + radiusDouble)

        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            if (clipRect != null) {
                clipCanvasWithRect(canvas, clipRect)
            }
            canvas.drawCircle(centerOffset, radiusDouble, paint)
            canvas.restore()
        } else {
            if (clipRect != null) {
                canvas.save()
                clipCanvasWithRect(canvas, clipRect, originOffset)
            }
            canvas.drawCircle(centerOffset + originOffset, radiusDouble, paint)
            if (clipCallback != null) {
                canvas.restore()
            }
        }
    }
}
