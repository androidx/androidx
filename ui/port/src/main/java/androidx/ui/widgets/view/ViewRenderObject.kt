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
import androidx.ui.engine.geometry.Offset
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext

/**
 * RenderObject implementation used to measure, layout and draw a
 * framework View within the Crane Widget hierarchy
 */
class ViewRenderObject(var view: View) : RenderBox() {

    override fun performResize() {
        updateSizeFromConstraints()
    }

    override fun performLayout() {
        updateSizeFromConstraints()
        val boxConstraints = constraints as BoxConstraints
        val largest = boxConstraints.biggest

        val widthSpec: Int
        val heightSpec: Int

        // TODO(Migration/ njawad) Need to create proper algorithm for mapping framework layout
        // parameters to constraints
        val viewWidth: Int
        val widthMode: Int
        if (largest.width.isFinite()) {
            viewWidth = largest.width.toInt()
            widthMode = View.MeasureSpec.AT_MOST
        } else {
            viewWidth = 0
            widthMode = View.MeasureSpec.UNSPECIFIED
        }

        val viewHeight: Int
        val heightMode: Int
        if (largest.height.isFinite()) {
            viewHeight = largest.height.toInt()
            heightMode = View.MeasureSpec.AT_MOST
        } else {
            viewHeight = 0
            heightMode = View.MeasureSpec.UNSPECIFIED
        }

        widthSpec = View.MeasureSpec.makeMeasureSpec(viewWidth, widthMode)
        heightSpec = View.MeasureSpec.makeMeasureSpec(viewHeight, heightMode)

        view.measure(widthSpec, heightSpec)

        val left = paintBounds.left
        val top = paintBounds.top
        val right = left + boxConstraints.constrainWidth(view.measuredWidth.toDouble())
        val bottom = top + boxConstraints.constrainWidth(view.measuredHeight.toDouble())
        view.layout(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        view.draw(context.canvas.toFrameworkCanvas())
    }

    override fun markNeedsLayout() {
        super.markNeedsLayout()
        view.requestLayout()
    }

    private fun updateSizeFromConstraints() {
        size = constraints!!.biggest
    }
}