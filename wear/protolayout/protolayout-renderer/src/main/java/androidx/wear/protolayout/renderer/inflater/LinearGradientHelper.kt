/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater

import android.graphics.Shader.TileMode
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.wear.protolayout.proto.ColorProto.ColorStop
import androidx.wear.protolayout.proto.ColorProto.LinearGradient
import androidx.wear.protolayout.proto.DimensionProto.OffsetDimension
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline.PipelineMaker
import androidx.wear.protolayout.renderer.inflater.PropHelpers.handleProp
import java.util.Optional
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

private typealias LinearGradientShader = android.graphics.LinearGradient

/**
 * LinearGradient helper class.
 *
 * Dynamic data are added to the pipeline during construction.
 *
 * If not set in the gradient proto, start point defaults to top left corner and end point defaults
 * to top right corner. This results in a horizontal gradient from left to right.
 *
 * Color stops are initially sorted based on their offset if all color stops are static.
 *
 * @param gradProto Linear gradient protolayout definition.
 * @param viewForMeasurements The view to use for measurements of the bounding box ratio.
 * @param pipelineMaker The pipeline maker to add dynamic data to.
 * @param posId The position ID of the element containing the linear gradient.
 * @param invalidateCallback The callback to invalidate the UI element that owns the gradient upon
 *   changes.
 */
internal class LinearGradientHelper(
    gradProto: LinearGradient,
    private val viewForMeasurements: View,
    pipelineMaker: Optional<PipelineMaker>,
    posId: String,
    private val invalidateCallback: Runnable,
) {
    private var isInitialized: Boolean = false

    var startX: Float by Delegates.observable(0f, this::invalidateOnChange)
    var endX: Float by Delegates.observable(0f, this::invalidateOnChange)
    var startY: Float by Delegates.observable(0f, this::invalidateOnChange)
    var endY: Float by Delegates.observable(0f, this::invalidateOnChange)

    val colors: IntArray
    val colorOffsets: FloatArray?

    init {
        require(gradProto.colorStopsCount in MIN_COLOR_STOPS..MAX_COLOR_STOPS) {
            "Linear gradient color count must be in the range [$MIN_COLOR_STOPS, $MAX_COLOR_STOPS]."
        }
        val numOfOffsets = gradProto.colorStopsList.count { it.hasOffset() }
        require(numOfOffsets == 0 || numOfOffsets == gradProto.colorStopsCount) {
            "Either all or none of the color stops should contain an offset."
        }
        val allStatic =
            gradProto.colorStopsList.none {
                it.color.hasDynamicValue() || it.offset.hasDynamicValue()
            }

        val inputColorStops =
            if (allStatic && numOfOffsets > 0) {
                gradProto.colorStopsList.sortedBy { it.offset.value }
            } else {
                gradProto.colorStopsList
            }

        colors = inputColorStops.map { it.color.argb }.toIntArray()

        colorOffsets =
            inputColorStops.let { stops ->
                if (numOfOffsets > 0) {
                    stops.map { it.offset.value }.toFloatArray()
                } else {
                    null
                }
            }

        inputColorStops.forEachIndexed { index, colorStop ->
            handleDynamicColorStop(colorStop, index, posId, pipelineMaker)
        }

        if (gradProto.hasStartX()) {
            handleDynamicCoordinate(gradProto.startX, Coordinate.START_X, posId, pipelineMaker)
        }
        if (gradProto.hasStartY()) {
            handleDynamicCoordinate(gradProto.startY, Coordinate.START_Y, posId, pipelineMaker)
        }
        if (gradProto.hasEndX()) {
            handleDynamicCoordinate(gradProto.endX, Coordinate.END_X, posId, pipelineMaker)
        } else {
            // TODO: b/383101450 - Optmizing this by getting the view size in advance can avoid an
            // extra frame delay.
            viewForMeasurements.post { endX = Coordinate.END_X.size(viewForMeasurements).toFloat() }
        }
        if (gradProto.hasEndY()) {
            handleDynamicCoordinate(gradProto.endY, Coordinate.END_Y, posId, pipelineMaker)
        }

        isInitialized = true
        invalidate()
    }

    var shader: LinearGradientShader? = null
        get() {
            if (field == null) {
                field =
                    LinearGradientShader(
                        startX,
                        startY,
                        endX,
                        endY,
                        colors,
                        colorOffsets,
                        DEFAULT_TILE_MODE,
                    )
            }
            return field
        }

    @Suppress("UNUSED_PARAMETER")
    private fun invalidateOnChange(unused: KProperty<*>, old: Float, new: Float) {
        if (old != new) {
            invalidate()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateColor(index: Int, @ColorInt color: Int) {
        require(index in 0..<colors.size) {
            "Index must be in the range [0, ${colors.size - 1}]. Index: $index"
        }
        colors[index] = color
        invalidate()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateColorOffset(index: Int, offset: Float) {
        require(colorOffsets != null) { "Color offsets are not defined." }
        require(index in 0..<colorOffsets.size) {
            "Index must be in the range [0, ${colors.size - 1}]. Index: $index"
        }
        colorOffsets[index] = offset
        invalidate()
    }

    private fun handleDynamicColorStop(
        colorStop: ColorStop,
        index: Int,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        handleProp(colorStop.color, { color -> updateColor(index, color) }, posId, pipelineMaker)
        if (colorStop.hasOffset()) {
            handleProp(
                colorStop.offset,
                { offset -> updateColorOffset(index, offset) },
                posId,
                pipelineMaker,
            )
        }
    }

    private fun handleDynamicCoordinate(
        offsetDimension: OffsetDimension,
        coordinate: Coordinate,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        when (offsetDimension.innerCase) {
            OffsetDimension.InnerCase.OFFSET_DP ->
                handleProp(
                    offsetDimension.offsetDp,
                    { dp -> setDpValue(coordinate, dp) },
                    posId,
                    pipelineMaker,
                )
            OffsetDimension.InnerCase.LOCATION_RATIO ->
                handleProp(
                    offsetDimension.locationRatio.ratio,
                    { ratio -> viewForMeasurements.post { setRatioValue(coordinate, ratio) } },
                    posId,
                    pipelineMaker,
                )
            OffsetDimension.InnerCase.INNER_NOT_SET ->
                Log.w(
                    TAG,
                    "OffsetDimension has an unknown dimension type: ${offsetDimension.innerCase.name}",
                )
            else ->
                throw IllegalArgumentException(
                    "Invalid OffsetDimension ${offsetDimension.toString()}"
                )
        }
    }

    private fun setRatioValue(coordinate: Coordinate, ratio: Float) {
        setPxValue(coordinate, ratio * coordinate.size(viewForMeasurements))
    }

    private fun setDpValue(coordinate: Coordinate, dp: Float) {
        setPxValue(coordinate, dp * viewForMeasurements.resources.displayMetrics.density)
    }

    private fun setPxValue(coordinate: Coordinate, newValue: Float) {
        when (coordinate) {
            Coordinate.START_X -> startX = newValue
            Coordinate.START_Y -> startY = newValue
            Coordinate.END_X -> endX = newValue
            Coordinate.END_Y -> endY = newValue
        }
    }

    /** Used to invalidate the shader and the gradient owner upon changes. */
    private fun invalidate() {
        if (isInitialized) {
            shader = null
            invalidateCallback.run()
        }
    }

    private companion object {
        const val TAG = "LinearGradientHelper"
        const val MIN_COLOR_STOPS = 2
        const val MAX_COLOR_STOPS = 10
        val DEFAULT_TILE_MODE =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                TileMode.DECAL
            } else {
                TileMode.CLAMP
            }

        enum class Coordinate {
            START_X,
            START_Y,
            END_X,
            END_Y;

            fun size(view: View): Int =
                when (this) {
                    START_X,
                    END_X -> view.width
                    START_Y,
                    END_Y -> view.height
                }
        }
    }
}
