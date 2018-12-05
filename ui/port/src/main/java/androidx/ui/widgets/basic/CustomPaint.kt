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

package androidx.ui.widgets.basic

import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.Key
import androidx.ui.rendering.custompaint.CustomPainter
import androidx.ui.rendering.custompaint.RenderCustomPaint
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

/**
 * A widget that provides a canvas on which to draw during the paint phase.
 *
 * When asked to paint, [CustomPaint] first asks its [painter] to paint on the
 * current canvas, then it paints its child, and then, after painting its
 * child, it asks its [foregroundPainter] to paint. The coordinate system of the
 * canvas matches the coordinate system of the [CustomPaint] object. The
 * painters are expected to paint within a rectangle starting at the origin and
 * encompassing a region of the given size. (If the painters paint outside
 * those bounds, there might be insufficient memory allocated to rasterize the
 * painting commands and the resulting behavior is undefined.)
 *
 * Painters are implemented by subclassing [CustomPainter].
 *
 * Because custom paint calls its painters during paint, you cannot call
 * `setState` or `markNeedsLayout` during the callback (the layout for this
 * frame has already happened).
 *
 * Custom painters normally size themselves to their child. If they do not have
 * a child, they attempt to size themselves to the [size], which defaults to
 * [Size.zero]. [size] must not be null.
 *
 * [isComplex] and [willChange] are hints to the compositor's raster cache
 * and must not be null.
 *
 * ## Sample code
 *
 * This example shows how the sample custom painter shown at [CustomPainter]
 * could be used in a [CustomPaint] widget to display a background to some
 * text.
 *
 * ```dart
 * new CustomPaint(
 *   painter: new Sky(),
 *   child: new Center(
 *     child: new Text(
 *       'Once upon a time...',
 *       style: const TextStyle(
 *         fontSize: 40.0,
 *         fontWeight: FontWeight.w900,
 *         color: const Color(0xFFFFFFFF),
 *       ),
 *     ),
 *   ),
 * )
 * ```
 *
 * See also:
 *
 *  * [CustomPainter], the class to extend when creating custom painters.
 *  * [Canvas], the class that a custom painter uses to paint.
 */
class CustomPaint(
    key: Key? = null,
    /** The painter that paints before the children. */
    val painter: CustomPainter? = null,
    /** The painter that paints after the children. */
    val foregroundPainter: CustomPainter? = null,
    /**
     * The size that this [CustomPaint] should aim for, given the layout
     * constraints, if there is no child.
     *
     * Defaults to [Size.zero].
     *
     * If there's a child, this is ignored, and the size of the child is used
     * instead.
     */
    val size: Size = Size.zero,
    /**
     * Whether the painting is complex enough to benefit from caching.
     *
     * The compositor contains a raster cache that holds bitmaps of layers in
     * order to avoid the cost of repeatedly rendering those layers on each
     * frame. If this flag is not set, then the compositor will apply its own
     * heuristics to decide whether the this layer is complex enough to benefit
     * from caching.
     */
    val isComplex: Boolean = false,
    /**
     * Whether the raster cache should be told that this painting is likely
     * to change in the next frame.
     */
    val willChange: Boolean = false,
    child: Widget? = null
) : SingleChildRenderObjectWidget(key, child) {

    override fun createRenderObject(context: BuildContext): RenderObject {
        return RenderCustomPaint(
                painter = painter,
        foregroundPainter = foregroundPainter,
        preferredSize = size,
        isComplex = isComplex,
        willChange = willChange
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        (renderObject as RenderCustomPaint).apply {
            painter = painter
            foregroundPainter = foregroundPainter
            preferredSize = size
            isComplex = isComplex
            willChange = willChange
        }
    }

    override fun didUnmountRenderObject(renderObject: RenderObject?) {
        (renderObject as RenderCustomPaint).apply {
            painter = null
            foregroundPainter = null
        }
    }
}