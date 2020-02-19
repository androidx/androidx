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
package androidx.ui.graphics.painter

import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.Paint
import androidx.ui.graphics.withSaveLayer
import androidx.ui.unit.Px
import androidx.ui.unit.PxSize

/**
 * Abstraction for something that can be drawn. In addition to providing the ability to draw
 * into a specified bounded area, [Painter] provides a few high level mechanisms
 * that consumers can use to configure how the content is drawn. These include alpha,
 * ColorFilter, and RTL
 *
 * Implementations should provide a meaningful equals method that compares values of
 * different [Painter] subclasses and not rely on just referential equality
 */
abstract class Painter {

    /**
     * Optional [Paint] used to draw contents into an offscreen layer in order to apply
     * alpha or [ColorFilter] parameters accordingly. If no alpha or [ColorFilter] is
     * provided or the [Painter] implementation implements [applyAlpha] and
     * [applyColorFilter] then this paint is not used
     */
    private var layerPaint: Paint? = null

    /**
     * Lazily create a [Paint] object or return the existing instance if it is already allocated
     */
    private fun obtainPaint(): Paint {
        var target = layerPaint
        if (target == null) {
            target = Paint()
            layerPaint = target
        }
        return target
    }

    /**
     * Determine if an additional rasterization layer should be used in order to draw the
     * [Painter]. This would be necessary if there is an alpha value between 0.0 and 1.0
     * and the [Painter] implementation does not handle alpha internally. Similarly
     * a layer would be used if a [ColorFilter] is provided and the implementation is not
     * handled internally.
     */
    private var useLayer = false

    /**
     * Currently configured ColorFilter
     */
    private var colorFilter: ColorFilter? = null

    /**
     * Optional [ColorFilter] used to modify the source pixels when drawn to the destination
     * The default implementation of [Painter] will render it's contents into a separate
     * layer then render that layer to the destination with the [ColorFilter] applied.
     * Implementations that can handle applying a [ColorFilter] more directly should also
     * implement the [applyColorFilter] method and return true
     */
    private fun configureColorFilter(colorFilter: ColorFilter?) {
        if (this.colorFilter != colorFilter) {
            val consumedColorFilter = applyColorFilter(colorFilter)
            if (!consumedColorFilter) {
                if (colorFilter == null) {
                    layerPaint?.colorFilter = null
                    useLayer = false
                } else {
                    obtainPaint().colorFilter = colorFilter
                    useLayer = true
                }
            }
            this.colorFilter = colorFilter
        }
    }

    /**
     * Currently configured alpha/opacity value
     */
    private var alpha: Float = DefaultAlpha

    /**
     * Specify the alpha transparency to apply to the [Painter] when it is drawn into
     * the destination. The default implementation within [Painter] will render it's
     * contents into an offscreen layer, then draw that layer into the destination with the
     * alpha applied.
     * Implementations of [Painter] that can handle alpha configuration more optimally
     * should implement the [applyAlpha] interface and avoid intermediate layer rendering
     * where ever possible. For example, [Painter] implementations that draw a Bitmap
     * would be able to draw their contents to the destination with the alpha applied directly
     * without needing an offscreen buffer. Additionally, drawing a shape with a color
     * applied can modulate the alpha channel directly without requiring a layer to achieve
     * the same visual effect
     */
    private fun configureAlpha(alpha: Float) {
        if (this.alpha != alpha) {
            val consumed = applyAlpha(alpha)
            if (!consumed) {
                if (alpha == DefaultAlpha) {
                    // Only update the paint parameter if we had it allocated before
                    layerPaint?.alpha = alpha
                    useLayer = false
                } else {
                    obtainPaint().alpha = alpha
                    useLayer = true
                }
            }
            this.alpha = alpha
        }
    }

    private var rtl: Boolean = false

    /**
     * Flag indicating that the contents of the [Painter] should be drawn with
     * to support locales with right-to-left languages.
     * Implementations of [Painter] that support right to left contents should implement
     * the [applyRtl] method
     */
    private fun configureRtl(rtl: Boolean) {
        if (this.rtl != rtl) {
            applyRtl(rtl)
            this.rtl = rtl
        }
    }

    /**
     * Return the intrinsic size of the [Painter].
     * If the there is no intrinsic size (i.e. filling bounds with an arbitrary color) return
     * [PxSize.UnspecifiedSize].
     * If there is no intrinsic size in a single dimension, return [PxSize] with [Px.Infinity] in
     * the desired dimension.
     * If a [Painter] does not have an intrinsic size, it will always draw within the full
     * bounds of the destination
     */
    abstract val intrinsicSize: PxSize

    /**
     * Implementation of drawing logic for instances of [Painter]. This is invoked
     * internally within [draw] after the positioning and configuring the [Painter]
     */
    protected abstract fun onDraw(canvas: Canvas, bounds: PxSize)

    /**
     * Apply the provided alpha value returning true if it was applied successfully,
     * or false if it could not be applied
     */
    protected open fun applyAlpha(alpha: Float): Boolean = false

    /**
     * Apply the provided color filter returning true if it was applied successfully,
     * or false if it could not be applied
     */
    protected open fun applyColorFilter(colorFilter: ColorFilter?): Boolean = false

    /**
     * Apply the appropriate internal configuration to positioning content for locales with
     * right-to-left languages
     */
    protected open fun applyRtl(rtl: Boolean): Boolean = false

    fun draw(
        canvas: Canvas,
        bounds: PxSize,
        alpha: Float = DefaultAlpha,
        colorFilter: ColorFilter? = null,
        rtl: Boolean = false
    ) {
        configureAlpha(alpha)
        configureColorFilter(colorFilter)
        configureRtl(rtl)

        val srcWidth = if (intrinsicSize.width.value != Float.POSITIVE_INFINITY) {
            intrinsicSize.width
        } else {
            bounds.width
        }

        val srcHeight = if (intrinsicSize.height.value != Float.POSITIVE_INFINITY) {
            intrinsicSize.height
        } else {
            bounds.height
        }

        val size = PxSize(Px(srcWidth.value), Px(srcHeight.value))

        if (alpha > 0.0f) {
            if (useLayer) {
                val layerRect =
                    Rect.fromLTWH(0.0f, 0.0f, srcWidth.value, srcHeight.value)
                // TODO njawad replace with RenderNode/Layer API usage
                canvas.withSaveLayer(layerRect, obtainPaint()) {
                    onDraw(canvas, size)
                }
            } else {
                onDraw(canvas, size)
            }
        }
    }
}