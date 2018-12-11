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
import androidx.ui.material.material.DefaultSplashRadius
import androidx.ui.material.material.Material
import androidx.ui.material.material.RectCallback
import androidx.ui.material.material.RenderInkFeatures
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.borderradius.BorderRadius
import androidx.ui.painting.borders.BoxShape
import androidx.ui.painting.matrixutils.getAsTranslation
import androidx.ui.rendering.box.RenderBox
import androidx.ui.vectormath64.Matrix4

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
 * otherwise, the highlight rectangle is coincident with the [referenceBox].
 *
 * When the highlight is removed, `onRemoved` will be called.
 */
class InkHighlight(
    controller: RenderInkFeatures,
    referenceBox: RenderBox,
    color: Color,
    private val shape: BoxShape = BoxShape.RECTANGLE,
    borderRadius: BorderRadius? = null,
    private val rectCallback: RectCallback? = null,
    onRemoved: VoidCallback? = null
) : InteractiveInkFeature(controller, referenceBox, color, onRemoved) {

    private val borderRadius: BorderRadius = borderRadius ?: BorderRadius.Zero

    private val alpha: Animation<Int>
    private val alphaController: AnimationController

    init {
        alphaController = AnimationController(
            duration = HighlightFadeDuration,
            vsync = controller.vsync
        ).apply {
            addListener { controller.markNeedsPaint() }
            addStatusListener(this@InkHighlight::handleAlphaStatusChanged)
            forward()
        }
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

    private fun paintHighlight(canvas: Canvas, rect: Rect, paint: Paint) {
        when (shape) {
            BoxShape.CIRCLE ->
                canvas.drawCircle(rect.getCenter(), DefaultSplashRadius, paint)
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

    override fun paintFeature(canvas: Canvas, transform: Matrix4) {
        val paint = Paint().apply { color = this@InkHighlight.color.withAlpha(alpha.value) }
        val originOffset = transform.getAsTranslation()
        val rect = rectCallback?.invoke() ?: Offset.zero and referenceBox.size
        if (originOffset == null) {
            canvas.save()
            canvas.transform(transform)
            paintHighlight(canvas, rect, paint)
            canvas.restore()
        } else {
            paintHighlight(canvas, rect.shift(originOffset), paint)
        }
    }
}
