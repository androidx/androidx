/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.demos.suspendfun

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Popup
import kotlin.collections.removeLast as removeLastKt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Preview
@Composable
fun OffsetKeyframeSplinePlaygroundDemo() {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val playgroundModel = remember { SplineKeyframesPlaygroundModel(scope) }

    val dslText: MutableState<String?> = remember { mutableStateOf(null) }

    Column(Modifier.fillMaxSize()) {
        Text(text = "Touch and drag to move anchors. DSL is printed in Logcat.")
        Box(Modifier.fillMaxWidth().weight(1f, true)) {
            dslText.value?.let {
                Popup(alignment = Alignment.Center, onDismissRequest = { dslText.value = null }) {
                    Text(text = it)
                }
            } ?: kotlin.run { playgroundModel.DrawContent(Modifier.fillMaxSize()) }
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = playgroundModel::onRun) { Text(text = "Run") }
                Button(
                    onClick = { dslText.value = playgroundModel.getDslText() },
                    enabled = dslText.value == null
                ) {
                    Text(text = "DSL")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { playgroundModel.addAnchor(density) }) { Text(text = "Add") }
                    Button(onClick = playgroundModel::removeAnchor) { Text(text = "Remove") }
                }
            }
            Text("Duration: ${playgroundModel.totalDuration.roundToInt()}ms")
            Slider(
                value = playgroundModel.totalDuration,
                onValueChange = playgroundModel::onNewDuration,
                valueRange = playgroundModel.range
            )
        }
    }
}

@Suppress("PrimitiveInCollection")
@OptIn(ExperimentalAnimationSpecApi::class)
private class SplineKeyframesPlaygroundModel(private val scope: CoroutineScope) {
    private val zero2DVector = AnimationVector2D(0f, 0f)

    // TODO: This is extremely hacky, find a way to improve
    private var modificationIndicator by mutableLongStateOf(0L)

    private val pointCount = 6
    private val animatedOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    private val anchors = mutableStateListOf<Offset>()
    private val anchorCount by derivedStateOf { anchors.size }

    // Note that this is not the duration per keyframe, just an arbitrary number so that the total
    // duration scales with number of anchors
    private val durationPerAnchor = mutableFloatStateOf(600f)

    private val pathPoints = mutableStateListOf<Offset>()
    private val samplePoints = mutableListOf<Offset>()
    private val sampleCount = 100

    val totalDuration by derivedStateOf { anchors.size * durationPerAnchor.floatValue }

    private var isInit = false

    private fun init(density: Density) {
        if (!isInit) {
            repeat((pointCount.toFloat() / 2f).roundToInt()) {
                anchors.add(getNextPosition(density))
            }
            isInit = true
        }
    }

    private val diamondPath = Path()
    private val diamondColor = Color(0xFFFF9800)
    private val diamondSize = 5.dp
    private val textOffset = 6.dp
    private val pathColor = diamondColor.copy(alpha = 0.7f)
    private val pathWidth = 2.dp
    private val pathOn = 3.dp
    private val pathOff = 6.dp

    @Composable
    fun DrawContent(modifier: Modifier = Modifier) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val textMeasurer = remember { TextMeasurer(fontFamilyResolver, density, layoutDirection) }
        val textColor = MaterialTheme.colors.onSurface

        remember(density.density) {
            val diamondSizePx = with(density) { diamondSize.toPx() }
            diamondPath.reset()
            diamondPath.moveTo(0f, -diamondSizePx)
            diamondPath.lineTo(diamondSizePx, 0f)
            diamondPath.lineTo(0f, diamondSizePx)
            diamondPath.lineTo(-diamondSizePx, 0f)
            diamondPath.close()
            false
        }

        init(density)

        Box(
            modifier
                .drawBehind {
                    if (pathPoints.isEmpty()) {
                        return@drawBehind
                    }
                    val pathWidthPx = with(this) { pathWidth.toPx() }
                    val pathOnPx = with(this) { pathOn.toPx() }
                    val pathOffPx = with(this) { pathOff.toPx() }
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(pathOnPx, pathOffPx))
                    translate(center.x, center.y) {
                        drawPoints(
                            points = pathPoints,
                            pointMode = PointMode.Polygon,
                            color = pathColor,
                            cap = StrokeCap.Round,
                            strokeWidth = pathWidthPx,
                            pathEffect = pathEffect
                        )
                    }
                }
                .drawBehind {
                    if (anchors.isEmpty()) {
                        return@drawBehind
                    }
                    val textOffsetPx = with(this) { Offset(textOffset.toPx(), textOffset.toPx()) }

                    // Draw anchors
                    translate(center.x, center.y) {
                        anchors.forEachIndexed { index, anchorPosition ->
                            translate(anchorPosition.x, anchorPosition.y) {
                                drawPath(path = diamondPath, color = diamondColor, style = Fill)
                                val text = getTextForAnchor(index)
                                drawText(
                                    textLayoutResult = textMeasurer.measure(text),
                                    topLeft = textOffsetPx,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDragStart(it, size) },
                        onDragEnd = this@SplineKeyframesPlaygroundModel::onDragEnd,
                        onDragCancel = this@SplineKeyframesPlaygroundModel::onDragEnd,
                        onDrag = this@SplineKeyframesPlaygroundModel::onDrag
                    )
                }
        ) {
            Text(
                text = "✈",
                fontSize = 42.sp,
                modifier =
                    Modifier.align(Alignment.Center)
                        .graphicsLayer {
                            translationX = -13f
                            translationY = 5f
                        }
                        .offset { animatedOffset.value.round() }
                        .graphicsLayer { rotationZ = angle.floatValue - 90f }
            )
        }

        // TODO: This is extremely hacky, find a way to improve
        LaunchedEffect(modificationIndicator) {
            samplePoints.clear()
            var i = 0
            val vectorized =
                keyframesWithSpline(0.5f) {
                        durationMillis = totalDuration.roundToInt()

                        anchors.forEachIndexed { index, offset ->
                            val fraction = (index + 1f) / (anchorCount + 1)
                            offset atFraction fraction
                        }
                    }
                    .vectorize(Offset.VectorConverter)

            var timeMillis = 0f
            val step = vectorized.durationMillis.toFloat() / sampleCount
            while (isActive && i < sampleCount) {
                val vectorValue =
                    vectorized.getValueFromNanos(
                        playTimeNanos = timeMillis.roundToLong() * 1_000_000,
                        initialValue = zero2DVector,
                        targetValue = zero2DVector,
                        initialVelocity = zero2DVector
                    )
                samplePoints.add(Offset(vectorValue.v1, vectorValue.v2))
                timeMillis += step
                i++
            }
            samplePoints.add(Offset.Zero)
            pathPoints.clear()
            pathPoints.addAll(samplePoints)
        }
    }

    fun onNewDuration(newTotalDuration: Float) {
        durationPerAnchor.floatValue = newTotalDuration / anchors.size
    }

    fun addAnchor(density: Density) {
        scope.launch { animatedOffset.snapTo(Offset.Zero) }
        anchors.add(getNextPosition(density))
        modificationIndicator++
    }

    fun removeAnchor() {
        if (anchors.size > 1) {
            scope.launch { animatedOffset.snapTo(Offset.Zero) }
            anchors.removeLastKt()
            modificationIndicator++
        }
    }

    private val minDuration = 600f
    private val baseMaxDuration = 10000f
    private val durationIncrement = minDuration

    val range: ClosedFloatingPointRange<Float>
        get() =
            if (totalDuration < baseMaxDuration) {
                minDuration..baseMaxDuration
            } else {
                val increments =
                    ((totalDuration - baseMaxDuration) / durationIncrement).toInt() + 1f

                val newMaxDuration = increments * durationIncrement + baseMaxDuration
                minDuration..newMaxDuration
            }

    private val angle = mutableFloatStateOf(0f)

    fun onRun() {
        scope.launch {
            animatedOffset.snapTo(Offset.Zero)
            animatedOffset.animateTo(
                targetValue = Offset.Zero,
                animationSpec =
                    InfiniteRepeatableSpec(
                        keyframesWithSpline(0.5f) {
                            durationMillis = totalDuration.roundToInt()

                            anchors.forEachIndexed { index, offset ->
                                val fraction = (index + 1f) / (anchorCount + 1)
                                offset atFraction fraction
                            }
                        },
                        RepeatMode.Restart
                    )
            ) {
                angle.floatValue =
                    Math.toDegrees(atan2(y = velocity.y, x = velocity.x).toDouble()).toFloat() + 90f
            }
            angle.floatValue = 0f
        }
    }

    private val first = 65
    private val last = 90
    private val length = last - first + 1 // inclusive
    private val textCache = mutableMapOf<Int, String>()

    private fun getTextForAnchor(anchorIndex: Int): String {
        if (textCache.containsKey(anchorIndex)) {
            return textCache[anchorIndex]!!
        }
        var text = ""
        val textLength =
            if (anchorIndex == 0) {
                1
            } else {
                log(anchorIndex.toFloat(), length.toFloat()).toInt() + 1
            }
        var value = anchorIndex
        for (i in 0 until textLength) {
            val codeOffset = value % length
            text = Char(first + codeOffset) + text
            value = (value.toFloat() / length).toInt() - 1
        }
        textCache[anchorIndex] = text
        return text
    }

    private val pi2 = Math.PI * 2
    private val minRadius = 40.dp
    private val radiusStep = 20.dp
    private val angleStep = 1f / pointCount
    private val angleInitialOff = angleStep / 2f // Offset to visually center the angles

    /** Get the next offset, relative to the center of the layout. */
    private fun getNextPosition(density: Density): Offset {
        val nextPointIndex = anchors.size
        val posInAngle = nextPointIndex % pointCount
        // Remove `.toInt()` to get a linear spread
        val distance = ((anchors.size / pointCount.toFloat()).toInt() * radiusStep) + minRadius
        val angle = angleStep * posInAngle + angleInitialOff

        val x = cos(pi2 * angle) * distance
        val y = sin(pi2 * angle) * distance

        val xPx = with(density) { x.toPx() }
        val yPx = with(density) { y.toPx() }

        return Offset(xPx, yPx)
    }

    private var draggingIndex = mutableIntStateOf(-1)

    // TODO: Consider using a threshold like this to find the anchor quicker
    //   private val diffThreshold = 10 * 10 * 2
    private fun onDragStart(position: Offset, size: IntSize) {
        scope.launch { animatedOffset.snapTo(Offset.Zero) }
        val relPosition =
            Offset(position.x - (size.width * 0.5f), position.y - (size.height * 0.5f))
        var closestIndex = -1
        var smallestDiff = Long.MAX_VALUE

        // Users are most likely to want to edit a recent item
        for (i in anchors.size - 1 downTo 0) {
            val diffOff = anchors[i] - relPosition
            val diff = (diffOff.x * diffOff.x + diffOff.y * diffOff.y).roundToLong()
            if (diff < smallestDiff) {
                smallestDiff = diff
                closestIndex = i
            }
        }
        draggingIndex.intValue = closestIndex
    }

    private fun onDragEnd() {
        draggingIndex.intValue = -1
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDrag(change: PointerInputChange, dragAmount: Offset) {
        val index = draggingIndex.intValue
        if (index >= 0) {
            anchors[index] = anchors[index] + dragAmount
            modificationIndicator++
        }
    }

    private val stringBuilder = StringBuilder()
    private val indentSize = 3

    fun getDslText(): String {
        // TODO: Use AnnotatedString to style closer to code
        val anchorList = anchors.toList()
        val durationMillis = totalDuration.roundToInt()
        val duration = durationMillis.toFloat() / (anchorList.size + 1)
        with(stringBuilder.clear()) {
            append("keyframesForOffsetWithSpline {\n")
            appendLineWithIndent(indentSize, "durationMillis = $durationMillis")
            appendLine()
            anchorList.forEachIndexed { index, offset ->
                val offsetStr = "Offset(${offset.x}f, ${offset.y}f)"
                val timeStamp = (duration + (duration * index)).roundToInt()
                appendLineWithIndent(indentSize, "$offsetStr at $timeStamp")
            }
            append("}")
        }
        val dsl = stringBuilder.toString()
        Log.i("DEMO", "Current DSL: \n$dsl")
        return dsl
    }

    private fun StringBuilder.appendLineWithIndent(indentSize: Int, text: String) {
        appendLine("${" ".repeat(indentSize)}$text")
    }
}
