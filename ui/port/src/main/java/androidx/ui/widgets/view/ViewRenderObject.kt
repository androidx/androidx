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
package androidx.ui.widgets.view

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.Constraints
import androidx.ui.rendering.obj.PaintingContext

/**
 * RenderObject implementation used to measure, layout and draw a
 * framework View within the Crane Widget hierarchy
 */
class ViewRenderObject(var view: View) : RenderBox() {

    override fun performResize() {
        updateSizeFromConstraints()
    }

    override fun layout(constraints: Constraints, parentUsesSize: Boolean) {
        // Loosen the constraints here to allow a View's layout parameters to be
        // respected
        super.layout((constraints as BoxConstraints).loosen(), parentUsesSize)
    }

    override fun performLayout() {
        updateSizeFromConstraints()
        var boxConstraints = constraints as BoxConstraints
        boxConstraints = boxConstraints.loosen()
        val largest = boxConstraints.biggest

        val layoutParams = view.layoutParams
        var widthMode: Int
        var heightMode: Int
        var width: Double
        var height: Double

        width = obtainDimension(largest.width, layoutParams?.width)
        height = obtainDimension(largest.height, layoutParams?.height)
        widthMode = obtainMeasureSpecMode(layoutParams?.width)
        heightMode = obtainMeasureSpecMode(layoutParams?.height)

        width = boxConstraints.constrainWidth(width)
        height = boxConstraints.constrainHeight(height)

        if (width.isInfinite()) {
            width = 0.0
            widthMode = View.MeasureSpec.UNSPECIFIED
        }

        if (height.isInfinite()) {
            height = 0.0
            heightMode = View.MeasureSpec.UNSPECIFIED
        }

        val widthSpec = View.MeasureSpec.makeMeasureSpec(width.toInt(), widthMode)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height.toInt(), heightMode)

        view.measure(widthSpec, heightSpec)

        // Constrain the Size in case the measured View dimensions do not fit within
        // the given constraints
        this.size = Size(
            boxConstraints.constrainWidth(view.measuredWidth.toDouble()),
            boxConstraints.constrainHeight(view.measuredHeight.toDouble())
        )
    }

    private fun obtainDimension(maxDimen: Double, viewLayoutParam: Int?): Double {
        val shouldMatchConstrainedWidth = viewLayoutParam == null ||
                viewLayoutParam == MATCH_PARENT ||
                viewLayoutParam == WRAP_CONTENT
        return if (shouldMatchConstrainedWidth) {
            maxDimen
        } else {
            viewLayoutParam!!.toDouble()
        }
    }

    private fun obtainMeasureSpecMode(viewLayoutParam: Int?): Int {
        return if (viewLayoutParam == null) {
            View.MeasureSpec.UNSPECIFIED
        } else if (viewLayoutParam == WRAP_CONTENT) {
            View.MeasureSpec.AT_MOST
        } else {
            // If the layout param is MATCH_PARENT or a specific value, match exactly
            View.MeasureSpec.EXACTLY
        }
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        // We don't know the offset of the RenderObject until we are told to paint
        // if the target View has not been laid out already, do it now
        if (view.isLayoutRequested) {
            val childSize = size
            val left = offset.dx
            val top = offset.dy
            val right = left + childSize.width
            val bottom = top + childSize.height
            view.layout(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }

        val canvas = context.canvas.toFrameworkCanvas()
        val dx = offset.dx.toFloat()
        val dy = offset.dy.toFloat()
        canvas.translate(dx, dy)
        view.draw(canvas)
        canvas.translate(-dx, -dy)
    }

    override fun markNeedsLayout() {
        super.markNeedsLayout()
        view.requestLayout()
    }

    private fun updateSizeFromConstraints() {
        size = constraints!!.biggest
    }
}