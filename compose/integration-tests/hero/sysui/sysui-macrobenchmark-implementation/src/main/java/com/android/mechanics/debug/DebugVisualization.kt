/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.mechanics.debug

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastForEachIndexed
import com.android.mechanics.MotionValueState
import com.android.mechanics.spec.DirectionalMotionSpec
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.SemanticKey
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Computes the output range for a debug visualization given a spec and an input range. */
typealias OutputRangeFn =
    (spec: MotionSpec, inputRange: ClosedFloatingPointRange<Float>) -> ClosedFloatingPointRange<
            Float
        >

/**
 * A debug visualization of the [motionValue].
 *
 * Draws both the [MotionValue.spec], as well as the input and output.
 *
 * NOTE: This is a debug tool, do not enable in production.
 *
 * @param motionValue The [MotionValue] to inspect.
 * @param inputRange The relevant range of the input (x) axis, for which to draw the graph.
 * @param maxAgeMillis Max age of the elements in the history trail.
 */
@Composable
fun DebugMotionValueVisualization(
    motionValue: MotionValueState,
    inputRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    outputRange: OutputRangeFn = DebugMotionValueVisualization.default,
    maxAgeMillis: Long = 1000L,
) {
    val inspector = remember(motionValue) { motionValue.debugInspector() }

    val spec = remember(motionValue) { derivedStateOf { inspector.frame.spec } }.value

    val computedOutputRange = remember(spec, inputRange) { outputRange(spec, inputRange) }
    DisposableEffect(inspector) { onDispose { inspector.dispose() } }

    val colorScheme = MaterialTheme.colorScheme
    val axisColor = colorScheme.outline
    val specColor = colorScheme.tertiary
    val valueColor = colorScheme.primary

    val primarySpec = spec.get(inspector.frame.gestureDirection)
    val activeSegment = inspector.frame.segmentKey

    Spacer(
        modifier =
            modifier
                .debugMotionSpecGraph(
                    primarySpec,
                    inputRange,
                    computedOutputRange,
                    axisColor,
                    specColor,
                    activeSegment,
                )
                .debugMotionValueGraph(
                    motionValue,
                    valueColor,
                    inputRange,
                    computedOutputRange,
                    maxAgeMillis,
                )
    )
}

object DebugMotionValueVisualization {

    /**
     * Returns the output range as annotated in the spec using [OutputRangeKey], or
     * [minMaxOutputRange] is not specified.
     */
    val default: OutputRangeFn = { spec, inputRange ->
        spec.semanticState(OutputRangeKey) ?: spec.computeOutputValueRange(inputRange)
    }
    /**
     * Returns an output range containing the min and max output values at each breakpoint within
     * the input range
     */
    val minMaxOutputRange: OutputRangeFn = { spec, inputRange ->
        spec.computeOutputValueRange(inputRange)
    }

    /** Returns an output range that is identical to the input range */
    val inputRange: OutputRangeFn = { _, inputRange -> inputRange }

    /** Defines the output range for the visualization. */
    val OutputRangeKey = SemanticKey<ClosedFloatingPointRange<Float>>("visualizationOutputRange")
}

/**
 * Draws a full-sized debug visualization of [spec].
 *
 * NOTE: This is a debug tool, do not enable in production.
 *
 * @param inputRange The range of the input (x) axis
 * @param outputRange The range of the output (y) axis.
 */
fun Modifier.debugMotionSpecGraph(
    spec: DirectionalMotionSpec,
    inputRange: ClosedFloatingPointRange<Float>,
    outputRange: ClosedFloatingPointRange<Float>,
    axisColor: Color = Color.Gray,
    specColor: Color = Color.Blue,
    activeSegment: SegmentKey? = null,
): Modifier = drawBehind {
    drawAxis(axisColor)
    drawDirectionalSpec(spec, inputRange, outputRange, specColor, activeSegment)
}

/**
 * Draws a full-sized debug visualization of the [motionValue] state.
 *
 * This can be combined with [debugMotionSpecGraph], when [inputRange] and [outputRange] are the
 * same.
 *
 * NOTE: This is a debug tool, do not enable in production.
 *
 * @param color Color for the dots indicating the value
 * @param inputRange The range of the input (x) axis
 * @param outputRange The range of the output (y) axis.
 * @param maxAgeMillis Max age of the elements in the history trail.
 */
@Composable
fun Modifier.debugMotionValueGraph(
    motionValue: MotionValueState,
    color: Color,
    inputRange: ClosedFloatingPointRange<Float>,
    outputRange: ClosedFloatingPointRange<Float>,
    maxAgeMillis: Long = 1000L,
): Modifier =
    this then
        DebugMotionValueGraphElement(motionValue, color, inputRange, outputRange, maxAgeMillis)

/**
 * Utility to compute the min/max output values of the spec for the given input.
 *
 * Note: this only samples at breakpoint locations. For segment mappings that produce smaller/larger
 * values in between two breakpoints, this method might might not produce a correct result.
 */
fun MotionSpec.computeOutputValueRange(
    inputRange: ClosedFloatingPointRange<Float>
): ClosedFloatingPointRange<Float> {
    return if (isUnidirectional) {
        maxDirection.computeOutputValueRange(inputRange)
    } else {
        val maxRange = maxDirection.computeOutputValueRange(inputRange)
        val minRange = minDirection.computeOutputValueRange(inputRange)

        val start = min(minRange.start, maxRange.start)
        val endInclusive = max(minRange.endInclusive, maxRange.endInclusive)

        start..endInclusive
    }
}

/**
 * Utility to compute the min/max output values of the spec for the given input.
 *
 * Note: this only samples at breakpoint locations. For segment mappings that produce smaller/larger
 * values in between two breakpoints, this method might might not produce a correct result.
 */
fun DirectionalMotionSpec.computeOutputValueRange(
    inputRange: ClosedFloatingPointRange<Float>
): ClosedFloatingPointRange<Float> {

    val start = findBreakpointIndex(inputRange.start)
    val end = findBreakpointIndex(inputRange.endInclusive)

    val samples = buildList {
        add(mappings[start].map(inputRange.start))

        for (breakpointIndex in (start + 1)..end) {

            val position = breakpoints[breakpointIndex].position

            add(mappings[breakpointIndex - 1].map(position))
            add(mappings[breakpointIndex].map(position))
        }

        add(mappings[end].map(inputRange.endInclusive))
    }

    return samples.min()..samples.max()
}

private data class DebugMotionValueGraphElement(
    val motionValue: MotionValueState,
    val color: Color,
    val inputRange: ClosedFloatingPointRange<Float>,
    val outputRange: ClosedFloatingPointRange<Float>,
    val maxAgeMillis: Long,
) : ModifierNodeElement<DebugMotionValueGraphNode>() {

    init {
        require(maxAgeMillis > 0)
    }

    override fun create() =
        DebugMotionValueGraphNode(motionValue, color, inputRange, outputRange, maxAgeMillis)

    override fun update(node: DebugMotionValueGraphNode) {
        node.motionValue = motionValue
        node.color = color
        node.inputRange = inputRange
        node.outputRange = outputRange
        node.maxAgeMillis = maxAgeMillis
    }

    override fun InspectorInfo.inspectableProperties() {
        // intentionally empty
    }
}

private class DebugMotionValueGraphNode(
    motionValue: MotionValueState,
    var color: Color,
    var inputRange: ClosedFloatingPointRange<Float>,
    var outputRange: ClosedFloatingPointRange<Float>,
    var maxAgeMillis: Long,
) : DrawModifierNode, ObserverModifierNode, Modifier.Node() {

    private var debugInspector by mutableStateOf<DebugInspector?>(null)
    private val history = mutableStateListOf<FrameData>()

    var motionValue = motionValue
        set(value) {
            if (value != field) {
                disposeDebugInspector()
                field = value

                if (isAttached) {
                    acquireDebugInspector()
                }
            }
        }

    override fun onAttach() {
        acquireDebugInspector()

        coroutineScope.launch {
            while (true) {
                if (history.size > 1) {

                    withFrameNanos { thisFrameTime ->
                        while (
                            history.size > 1 &&
                                (thisFrameTime - history.first().frameTimeNanos) >
                                    maxAgeMillis * 1_000_000
                        ) {
                            history.removeAt(0)
                        }
                    }
                }

                snapshotFlow { history.size > 1 }.first { it }
            }
        }
    }

    override fun onDetach() {
        disposeDebugInspector()
    }

    private fun acquireDebugInspector() {
        debugInspector = motionValue.debugInspector()
        observeFrameAndAddToHistory()
    }

    private fun disposeDebugInspector() {
        debugInspector?.dispose()
        debugInspector = null
        history.clear()
    }

    override fun ContentDrawScope.draw() {
        if (history.isNotEmpty()) {
            drawDirectionAndAnimationStatus(history.last())
        }
        drawInputOutputTrail(history, inputRange, outputRange, color)
        drawContent()
    }

    private fun observeFrameAndAddToHistory() {
        var lastFrame: FrameData? = null

        observeReads { lastFrame = debugInspector?.frame }

        lastFrame?.also { history.add(it) }
    }

    override fun onObservedReadsChanged() {
        observeFrameAndAddToHistory()
    }
}

private val MotionSpec.isUnidirectional: Boolean
    get() = maxDirection == minDirection

private fun DrawScope.mapPointInInputToX(
    input: Float,
    inputRange: ClosedFloatingPointRange<Float>,
): Float {
    val inputExtent = (inputRange.endInclusive - inputRange.start)
    return ((input - inputRange.start) / (inputExtent)) * size.width
}

private fun DrawScope.mapPointInOutputToY(
    output: Float,
    outputRange: ClosedFloatingPointRange<Float>,
): Float {
    val outputExtent = (outputRange.endInclusive - outputRange.start)
    return (1 - (output - outputRange.start) / (outputExtent)) * size.height
}

private fun DrawScope.drawDirectionalSpec(
    spec: DirectionalMotionSpec,
    inputRange: ClosedFloatingPointRange<Float>,
    outputRange: ClosedFloatingPointRange<Float>,
    color: Color,
    activeSegment: SegmentKey?,
) {

    val startSegment = spec.findBreakpointIndex(inputRange.start)
    val endSegment = spec.findBreakpointIndex(inputRange.endInclusive)

    for (segmentIndex in startSegment..endSegment) {
        val isActiveSegment =
            activeSegment?.let { spec.findSegmentIndex(it) == segmentIndex } ?: false

        val mapping = spec.mappings[segmentIndex]
        val startBreakpoint = spec.breakpoints[segmentIndex]
        val segmentStart = startBreakpoint.position
        val fromInput = segmentStart.fastCoerceAtLeast(inputRange.start)
        val endBreakpoint = spec.breakpoints[segmentIndex + 1]
        val segmentEnd = endBreakpoint.position
        val toInput = segmentEnd.fastCoerceAtMost(inputRange.endInclusive)

        val strokeWidth = if (isActiveSegment) 2.dp.toPx() else Stroke.HairlineWidth
        val dotSize = if (isActiveSegment) 4.dp.toPx() else 2.dp.toPx()
        val fromY = mapPointInOutputToY(mapping.map(fromInput), outputRange)
        val toY = mapPointInOutputToY(mapping.map(toInput), outputRange)

        val start = Offset(mapPointInInputToX(fromInput, inputRange), fromY)
        val end = Offset(mapPointInInputToX(toInput, inputRange), toY)
        if (mapping is Mapping.Fixed || mapping is Mapping.Identity || mapping is Mapping.Linear) {
            drawLine(color, start, end, strokeWidth = strokeWidth)
        } else {
            val xStart = mapPointInInputToX(fromInput, inputRange)
            val xEnd = mapPointInInputToX(toInput, inputRange)

            val oneDpInPx = 1.dp.toPx()
            val numberOfLines = ceil((xEnd - xStart) / oneDpInPx).toInt()
            val inputLength = (toInput - fromInput) / numberOfLines

            repeat(numberOfLines) {
                val lineStart = fromInput + inputLength * it
                val lineEnd = lineStart + inputLength

                val partialFromY = mapPointInOutputToY(mapping.map(lineStart), outputRange)
                val partialToY = mapPointInOutputToY(mapping.map(lineEnd), outputRange)

                val partialStart = Offset(mapPointInInputToX(lineStart, inputRange), partialFromY)
                val partialEnd = Offset(mapPointInInputToX(lineEnd, inputRange), partialToY)

                drawLine(color, partialStart, partialEnd, strokeWidth = strokeWidth)
            }
        }

        if (segmentStart == fromInput) {
            drawCircle(color, dotSize, start)
        }

        if (segmentEnd == toInput) {
            drawCircle(color, dotSize, end)
        }

        val guarantee = startBreakpoint.guarantee
        if (guarantee is Guarantee.InputDelta) {
            val guaranteePos = segmentStart + guarantee.delta
            if (guaranteePos > inputRange.start) {

                val guaranteeOffset =
                    Offset(
                        mapPointInInputToX(guaranteePos, inputRange),
                        mapPointInOutputToY(mapping.map(guaranteePos), outputRange),
                    )

                val arrowSize = 4.dp.toPx()

                drawLine(
                    color,
                    guaranteeOffset,
                    guaranteeOffset.plus(Offset(arrowSize, -arrowSize)),
                )
                drawLine(color, guaranteeOffset, guaranteeOffset.plus(Offset(arrowSize, arrowSize)))
            }
        }
    }
}

private fun DrawScope.drawDirectionAndAnimationStatus(currentFrame: FrameData) {
    val indicatorSize = min(this.size.height, 24.dp.toPx())

    this.scale(
        scaleX = if (currentFrame.gestureDirection == InputDirection.Max) 1f else -1f,
        scaleY = 1f,
    ) {
        val color = if (currentFrame.isStable) Color.Green else Color.Red
        val strokeWidth = 1.dp.toPx()
        val d1 = indicatorSize / 2f
        val d2 = indicatorSize / 3f

        translate(left = 2.dp.toPx()) {
            drawLine(
                color,
                Offset(center.x - d2, center.y - d1),
                center,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color,
                Offset(center.x - d2, center.y + d1),
                center,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        translate(left = -2.dp.toPx()) {
            drawLine(
                color,
                Offset(center.x - d2, center.y - d1),
                center,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color,
                Offset(center.x - d2, center.y + d1),
                center,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawInputOutputTrail(
    history: List<FrameData>,
    inputRange: ClosedFloatingPointRange<Float>,
    outputRange: ClosedFloatingPointRange<Float>,
    color: Color,
) {
    history.fastForEachIndexed { index, frame ->
        val x = mapPointInInputToX(frame.input, inputRange)
        val y = mapPointInOutputToY(frame.output, outputRange)

        drawCircle(color, 2.dp.toPx(), Offset(x, y), alpha = index / history.size.toFloat())
    }
}

private fun DrawScope.drawAxis(color: Color) {

    drawXAxis(color)
    drawYAxis(color)
}

private fun DrawScope.drawYAxis(color: Color, atX: Float = 0f) {

    val arrowSize = 4.dp.toPx()

    drawLine(color, Offset(atX, size.height), Offset(atX, 0f))
    drawLine(color, Offset(atX, 0f), Offset(atX + arrowSize, arrowSize))
    drawLine(color, Offset(atX, 0f), Offset(atX - arrowSize, arrowSize))
}

private fun DrawScope.drawXAxis(color: Color, atY: Float = size.height) {

    val arrowSize = 4.dp.toPx()

    drawLine(color, Offset(0f, atY), Offset(size.width, atY))
    drawLine(color, Offset(size.width, atY), Offset(size.width - arrowSize, atY + arrowSize))
    drawLine(color, Offset(size.width, atY), Offset(size.width - arrowSize, atY - arrowSize))
}
