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

import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationController
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.Tween
import androidx.ui.core.Bounds
import androidx.ui.core.Density
import androidx.ui.core.Duration
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.toBounds
import androidx.ui.core.toPx
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.engine.geometry.BorderRadius
import androidx.ui.material.borders.BoxShape
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.getAsTranslation

internal val HighlightFadeDuration = Duration.create(milliseconds = 200)

/**
 * A visual emphasis on a part of a [Material] receiving user interaction.
 *
 * This object is rarely created directly. Instead of creating an ink highlight
 * directly, consider using an [InkResponse] or [InkWell] widget, which uses
 * gestures (such as tap and long-press) to trigger ink highlights.
 *
 * See also:
 *
 *  * [InkResponse], which uses gestures to trigger ink highlights and ink
 *    splashes in the parent [Material].
 *  * [InkWell], which is a rectangular [InkResponse] (the most common type of
 *    ink response).
 *  * [Material], which is the widget on which the ink highlight is painted.
 *  * [InkSplash], which is an ink feature that shows a reaction to user input
 *    on a [Material].
 *
 * Begin a highlight animation.
 *
 * The [controller] argument is typically obtained via
 * `Material.of(context)`.
 *
 * If a `rectCallback` is given, then it provides the highlight rectangle,
 * otherwise, the highlight rectangle is coincident with the target layout.
 *
 * When the highlight is removed, `onRemoved` will be called.
 */
class InkHighlight(
    controller: MaterialInkController,
    coordinates: LayoutCoordinates,
    color: Color,
    private val shape: BoxShape = BoxShape.RECTANGLE,
    borderRadius: BorderRadius? = null,
    private val boundsCallback: ((LayoutCoordinates) -> Bounds)? = null,
    onRemoved: (() -> Unit)? = null
) : InteractiveInkFeature(controller, coordinates, color, onRemoved) {

    private val borderRadius: BorderRadius = borderRadius ?: BorderRadius.Zero

    private val alpha: Animation<Int>
    private val alphaController: AnimationController

    init {
        alphaController = AnimationController(
            duration = HighlightFadeDuration,
            vsync = controller.vsync
        )
        alphaController.addListener { controller.markNeedsPaint() }
        alphaController.addStatusListener(this@InkHighlight::handleAlphaStatusChanged)
        alphaController.forward()
        alpha = Tween(
            begin = 0,
            end = color.alpha
        ).animate(alphaController)

        controller.addInkFeature(this)
    }

    /** Whether this part of the material is being visually emphasized. */
    var active: Boolean = true
        private set

    /** Start visually emphasizing this part of the material. */
    fun activate() {
        active = true
        alphaController.forward()
    }

    /** Stop visually emphasizing this part of the material. */
    fun deactivate() {
        active = false
        alphaController.reverse()
    }

    private fun handleAlphaStatusChanged(status: AnimationStatus) {
        if (status == AnimationStatus.DISMISSED && !active) {
            dispose()
        }
    }

    override fun dispose() {
        alphaController.dispose()
        super.dispose()
    }

    private fun paintHighlight(canvas: Canvas, rect: Rect, paint: Paint, density: Density) {
        when (shape) {
            BoxShape.CIRCLE ->
                canvas.drawCircle(rect.getCenter(), DefaultSplashRadius.toPx(density), paint)
            BoxShape.RECTANGLE -> {
                if (borderRadius != BorderRadius.Zero) {
                    val clipRRect = RRect(
                        rect,
                        borderRadius.topLeft, borderRadius.topRight,
                        borderRadius.bottomLeft, borderRadius.bottomRight
                    )
                    canvas.drawRRect(clipRRect, paint)
                } else {
                    canvas.drawRect(rect, paint)
                }
            }
        }
    }

    override fun paintFeature(canvas: Canvas, transform: Matrix4, density: Density) {
        val paint = Paint()
        paint.color = color.withAlpha(alpha.value)
        val originOffset = transform.getAsTranslation()
        val bounds = boundsCallback?.invoke(coordinates) ?: coordinates.size.toBounds()
        val rect = bounds.toRect(density)
        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            paintHighlight(canvas, rect, paint, density)
            canvas.restore()
        } else {
            paintHighlight(canvas, rect.shift(originOffset), paint, density)
        }
    }
}
