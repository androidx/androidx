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
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderValueImpl
import kotlin.properties.Delegates

/** View for graphing state over time */
class ValueGraphView(
    context: Context,
    beginTimeNanos: Long,
    valueGraphDataHolder: GraphDataHolderValueImpl,
    private val paints: Paints
) : GraphView(context, beginTimeNanos, valueGraphDataHolder, paints) {

    override var unitHeight by Delegates.notNull<Float>()

    private var range = valueGraphDataHolder.getRange()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        unitHeight = layoutState.dataGraphHeight / range.toFloat()
    }

    override fun drawPoint(
        canvas: Canvas,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        paint: Paint
    ) = canvas.drawLine(x1, y1, x2, y2, paints.graphDataPaint)
}
