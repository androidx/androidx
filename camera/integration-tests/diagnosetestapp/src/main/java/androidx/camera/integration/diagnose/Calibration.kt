/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.diagnose

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.util.Size
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Calibration object that checks camera alignment with list of {@link Barcode}.
 *
 * @param previewViewSize - size of the  devices' preview view.
 */
class Calibration(
    private val previewViewSize: Size
    ) {

    // barcodeCoordinates
    private lateinit var barCodes: List<Barcode>
    private var topLeft: PointF? = null
    private var topRight: PointF? = null
    private var bottomLeft: PointF? = null
    private var bottomRight: PointF? = null

    // gridlines to be drawn
    var topGrid: Pair<PointF, PointF>? = null
    var bottomGrid: Pair<PointF, PointF>? = null
    var leftGrid: Pair<PointF, PointF>? = null
    var rightGrid: Pair<PointF, PointF>? = null

    var topY: Float? = null
    var bottomY: Float? = null
    var leftX: Float? = null
    var rightX: Float? = null

    // threshold box
    // TODO: switch back to private if I don't need to draw threshold box
    var thresholdTopLeft: RectF? = null
    var thresholdTopRight: RectF? = null
    var thresholdBottomLeft: RectF? = null
    var thresholdBottomRight: RectF? = null

    var isAligned: Boolean = false

    fun analyze(barCodes: List<Barcode>) {
        setBarcodes(barCodes)
        calculateBarcodeCoordinates()
        calculateGridLines()
        isAligned = checkAlignment()
        Log.d(TAG, "isAligned = $isAligned")
    }

    private fun setBarcodes(barCodes: List<Barcode>) {
        this.barCodes = barCodes
    }

    private fun calculateBarcodeCoordinates() {
        topLeft = findCenterPoint("top-left")
        topRight = findCenterPoint("top-right")
        bottomLeft = findCenterPoint("bottom-left")
        bottomRight = findCenterPoint("bottom-right")
    }

    /**
     * Calculate the target grid lines based on the average distance between x & y coordinates
     * corresponding {@link Barcode}.
     */
    private fun calculateGridLines() {
        if (!hasBarcodes()) {
            Log.d(TAG, " hasBarcodes = ${hasBarcodes()}, has ${barCodes.size} : $barCodes")
            return
        }
        Log.d(TAG, "has all barcodes")

        // calculate grid
        topY = (topLeft!!.y + topRight!!.y) / 2
        topGrid = Pair(
            PointF(0F, topY!!),
            PointF(previewViewSize.width.toFloat(), topY!!)
        )
        bottomY = (bottomLeft!!.y + bottomRight!!.y) / 2
        bottomGrid = Pair(
            PointF(0F, bottomY!!),
            PointF(previewViewSize.width.toFloat(), bottomY!!)
        )
        leftX = (topLeft!!.x + bottomLeft!!.x) / 2
        leftGrid = Pair(
            PointF(leftX!!, 0F),
            PointF(leftX!!, previewViewSize.height.toFloat())
        )
        rightX = (topRight!!.x + bottomRight!!.x) / 2
        rightGrid = Pair(
            PointF(rightX!!, 0F),
            PointF(rightX!!, previewViewSize.height.toFloat())
        )
    }

    private fun getThresholdBox(x: Float, y: Float): RectF {
        return RectF(x - THRESHOLD, y - THRESHOLD, x + THRESHOLD,
            y + THRESHOLD)
    }

    private fun containPoint(point: PointF?, thresholdBox: RectF?): Boolean {
        return point?.let { thresholdBox?.contains(it.x, it.y) } == true
    }

    /**
     * @return true if all {@link Barcode} center points are found within the corresponding
     * threshold boxes calculated around intersection points of the target grid line.
     */
    private fun checkAlignment(): Boolean {
        // create threshold boxes around the grid's intersection points
        topY?.let {
            thresholdTopLeft = leftX?.let { getThresholdBox(it, topY!!) }
            thresholdTopRight = rightX?.let { getThresholdBox(it, topY!!) }
        }
        bottomY?.let {
            thresholdBottomLeft = leftX?.let { getThresholdBox(it, bottomY!!) }
            thresholdBottomRight = rightX?.let { getThresholdBox(it, bottomY!!) }
        }
        // check if all barcode center points are within threshold
        return containPoint(topLeft, thresholdTopLeft) &&
            containPoint(topRight, thresholdTopRight) &&
            containPoint(bottomLeft, thresholdBottomLeft) &&
            containPoint(bottomRight, thresholdBottomRight)
    }

    // check if all 4 barcodes are detected
    private fun hasBarcodes(): Boolean {
        return topLeft != null && topRight != null && bottomLeft != null && bottomRight != null
    }

    private fun findCenterPoint(position: String): PointF? {
        for (barcode in barCodes) {
            val boundingBox = barcode.boundingBox
            if (barcode.rawValue == position && boundingBox != null) {
                return PointF(boundingBox.exactCenterX(), boundingBox.exactCenterY())
            }
        }
        return null
    }

    companion object {
        private const val TAG = "Calibration"
        private const val THRESHOLD = 4F
    }
}
