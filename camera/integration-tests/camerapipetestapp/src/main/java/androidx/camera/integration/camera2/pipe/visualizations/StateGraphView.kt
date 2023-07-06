/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe.visualizations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderStateImpl
import kotlin.properties.Delegates

/** View for graphing state over time */
class StateGraphView(
    context: Context,
    beginTimeNanos: Long,
    private val stateGraphDataHolder: GraphDataHolderStateImpl,
    private val paints: Paints
) : GraphView(context, beginTimeNanos, stateGraphDataHolder, paints) {

    override var unitHeight by Delegates.notNull<Float>()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        unitHeight = layoutState.dataGraphHeight / stateGraphDataHolder.getNumStates()
    }
    override fun drawPoint(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        paint: Paint
    ) = canvas.drawRect(x1, y2, x2, y2 - unitHeight, paints.whiteFillPaint)

    /** Draws description text for each state in the correct section */
    override fun drawExtra(canvas: Canvas?) {
        var h = unitHeight
        stateGraphDataHolder.getStrings().forEach { _ ->
            canvas?.drawLine(0f, h, layoutState.widthFloat, h, paints.dividerLinePaint)
            h += unitHeight
        }
    }
}
