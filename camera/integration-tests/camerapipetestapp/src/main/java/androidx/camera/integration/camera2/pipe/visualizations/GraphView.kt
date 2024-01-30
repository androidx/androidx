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
import android.view.View
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolder
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataPoint

/**
 * View for 1D graph visualizations. Implemented for both graphing state over time and
 * graphing value over time
 */
abstract class GraphView(
    context: Context,
    private val beginTimeNanos: Long,
    private val graphDataHolder: GraphDataHolder,
    private val paints: Paints
) : View(context) {

    inner class LayoutState(
        var widthFloat: Float,

        /** Height of the data portion of the graph view */
        var dataGraphHeight: Float,

        /** Height of the latency portion of the graph view */
        var latencyGraphHeight: Float,

        /** Defines y value of midpoint of latency portion of graph view */
        var latencyBaseline: Float
    )

    lateinit var layoutState: LayoutState

    /**
     * We define latency as the time data arrived - the time data was recorded. Latencies are stored
     * in this map after being calculated for each point
     */
    private var latencyMap: HashMap<GraphDataPoint, Float> = hashMapOf()

    /** Calculated differently for different types of graphs */
    abstract var unitHeight: Float

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val height = bottom - top
        layoutState = LayoutState(
            widthFloat = (right - left).toFloat(),
            dataGraphHeight = height * 3 / 4f,
            latencyGraphHeight = height / 4f,
            latencyBaseline = height * 7 / 8f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        /** Draws line at the top of the graph view separating it from the top row of the graph */
        canvas.drawLine(0f, 0f, layoutState.widthFloat, 0f, paints.dividerLinePaint)

        /** Draws line to separate the data graph and the latency graph */
        val dividerY = height - layoutState.latencyGraphHeight
        canvas.drawLine(0f, dividerY, layoutState.widthFloat, dividerY, paints.dividerLinePaint)

        val totalTimeElapsedNanos = System.nanoTime() - beginTimeNanos
        val totalTimeIntervalsPassed = (totalTimeElapsedNanos) / TIME_INTERVAL_LENGTH_NANOS
            .toFloat()

        /**
         * if it we have recorded more than 1 interval of data, then we want to offset the
         * time passed by the extra number of intervals
         */
        val xIntervalOffset = if (totalTimeIntervalsPassed > 1) totalTimeIntervalsPassed - 1 else 0f

        val pointsInTimeWindow = graphDataHolder.getPointsInTimeWindow(
            TIME_INTERVAL_LENGTH_NANOS,
            totalTimeElapsedNanos
        )

        drawPoints(canvas, pointsInTimeWindow, xIntervalOffset)
        drawExtra(canvas)

        /** Triggers redraw immediately - ensures we are drawing as frequently as possible */
        postInvalidate()
    }

    /** Calculate the y coordinate given a data point*/
    private fun getYFromPoint(point: GraphDataPoint): Float = layoutState.dataGraphHeight -
        unitHeight *
        point.value.toFloat()

    private fun drawLatency(
        canvas: Canvas,
        x1: Float,
        latency1: Float,
        x2: Float,
        latency2: Float,
        paint: Paint
    ) = canvas.drawLine(
        x1, layoutState.latencyBaseline - latency1 * 150, x2,
        layoutState
            .latencyBaseline - latency2 *
            150,
        paint
    )

    /** Draws all the data points within the time window */
    private fun drawPoints(
        canvas: Canvas,
        points: List<GraphDataPoint>,
        xIntervalOffset: Float
    ) {

        if (points.isEmpty()) return

        var lastX = 0f
        var lastY: Float = getYFromPoint(points.first())
        var currentX: Float
        var currentY: Float

        var lastLatency: Float? = null
        var currentLatency: Float

        val firstFrameNumber = points.first().frameNumber
        var previousFrameNumber = firstFrameNumber - 1

        for (point in points) {
            /**
             * Calculating how many intervals passed for this specific point, and subtracting out
             * the total offset gives us the decimal representing what percent of the screen the
             * currentX is at
             */
            val intervalsPassedSinceTimestamp = point.timestampNanos / TIME_INTERVAL_LENGTH_NANOS
                .toFloat()
            val intervalsPassedSinceArrivalTime = point.timeArrivedNanos /
                TIME_INTERVAL_LENGTH_NANOS.toFloat()

            currentX = (intervalsPassedSinceTimestamp - xIntervalOffset) * width

            /** Calculating and graphing the latency */
            currentLatency = intervalsPassedSinceArrivalTime - intervalsPassedSinceTimestamp

            val recordedLatency = latencyMap[point]
            if (recordedLatency != null) currentLatency = recordedLatency
            else latencyMap[point] = currentLatency

            if (lastLatency != null)
                drawLatency(
                    canvas, lastX, lastLatency, currentX, currentLatency,
                    paints.latencyDataPaint
                )

            lastLatency = latencyMap[point]

            /** Graphing the point */
            val frameNumber = point.frameNumber
            currentY = getYFromPoint(point)

            if (frameNumber == firstFrameNumber || frameNumber == previousFrameNumber + 1)
                drawPoint(canvas, lastX, lastY, currentX, currentY, paints.graphDataPaint)
            else {
                val frameDifference = frameNumber - previousFrameNumber
                val frameUnitWidth = (currentX - lastX) / (frameDifference)
                (1 until frameDifference).forEach { i ->
                    val missingFrameX = lastX + i * frameUnitWidth
                    canvas.drawLine(
                        missingFrameX, 0f, missingFrameX, layoutState.dataGraphHeight,
                        paints.missingDataPaint
                    )
                }
            }

            previousFrameNumber = frameNumber
            lastX = currentX
            lastY = currentY
        }
    }

    abstract fun drawPoint(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint)

    /** Can be overridden by subclasses for drawing things besides data */
    open fun drawExtra(canvas: Canvas?) { }

    companion object {
        /** Length of the time window of points being drawn - arbitrarily set at 4 seconds */
        const val TIME_INTERVAL_LENGTH_NANOS: Long = 3000000000
    }
}
