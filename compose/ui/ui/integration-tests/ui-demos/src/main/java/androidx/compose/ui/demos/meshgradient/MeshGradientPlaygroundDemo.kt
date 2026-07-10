/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.demos.meshgradient

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun MeshGradientPlaygroundDemo() {
    var rows by remember { mutableIntStateOf(1) }
    var columns by remember { mutableIntStateOf(1) }
    var useBicubicColorInterpolation by remember { mutableStateOf(true) }
    var meshData by
        remember(rows, columns) { mutableStateOf(generateLinearMeshState(rows, columns)) }

    var showGradientControls by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.requiredHeight(350.dp).fillMaxWidth()) {
            Gradient(
                modifier = Modifier.fillMaxSize(),
                meshData = meshData,
                hasBicubicColorInterpolation = useBicubicColorInterpolation,
            )
            if (showGradientControls) {
                GradientControls(meshData)
            }
        }
        Spacer(Modifier.height(30.dp))
        GradientOptions(
            meshState = meshData,
            showGradientControls = showGradientControls,
            hasBicubicColorInterpolation = useBicubicColorInterpolation,
        ) { hasBicubicColorInterpolation, r, c, sGradientControls ->
            useBicubicColorInterpolation = hasBicubicColorInterpolation
            rows = r
            columns = c
            showGradientControls = sGradientControls
        }
    }
}

@Composable
private fun Gradient(
    modifier: Modifier = Modifier,
    meshData: MeshData,
    hasBicubicColorInterpolation: Boolean,
) {
    val gradientPainter =
        remember(meshData.rows, meshData.columns, hasBicubicColorInterpolation) {
            MeshGradientPainter(meshData.rows, meshData.columns, hasBicubicColorInterpolation) {
                for (row in 0..rows) {
                    for (column in 0..columns) {
                        val index = row * (columns + 1) + column
                        setVertex(
                            row,
                            column,
                            position = meshData.positions[index],
                            color = meshData.colors[index],
                            leftControlPoint = meshData.leftBezierOffsets[index],
                            topControlPoint = meshData.topBezierOffsets[index],
                            rightControlPoint = meshData.rightBezierOffsets[index],
                            bottomControlPoint = meshData.bottomBezierOffsets[index],
                        )
                    }
                }
            }
        }

    Box(modifier.paint(gradientPainter))
}

@SuppressLint("PrimitiveInCollection")
@Composable
private fun GradientControls(meshData: MeshData) {
    var selectedPointIndex by
        remember(meshData.rows, meshData.columns) { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val widthState = rememberUpdatedState(width)
        val heightState = rememberUpdatedState(height)

        val handleSize = 16.dp
        val handleOffset = with(LocalDensity.current) { (handleSize / 2).roundToPx() }

        meshData.positions.forEachIndexed { index, point ->
            val currentOffset = Offset(point.x * width, point.y * height)
            Box(
                modifier =
                    Modifier.offset {
                            IntOffset(
                                currentOffset.x.roundToInt() - handleOffset,
                                currentOffset.y.roundToInt() - handleOffset,
                            )
                        }
                        .size(handleSize)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color.Black, CircleShape)
                        .pointerInput(index) {
                            detectTapGestures(
                                onTap = { _ ->
                                    if (selectedPointIndex == null) {
                                        selectedPointIndex = index
                                    }
                                }
                            )
                        }
                        .pointerInput(index) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val w = widthState.value
                                val h = heightState.value
                                if (index < meshData.positions.size) {
                                    meshData.apply {
                                        positions[index] +=
                                            Offset(dragAmount.x / w, dragAmount.y / h)
                                    }
                                }
                            }
                        }
            )

            BezierDirection.entries.forEach { direction ->
                val bezierOffsets =
                    when (direction) {
                        BezierDirection.LEFT -> meshData.leftBezierOffsets
                        BezierDirection.TOP -> meshData.topBezierOffsets
                        BezierDirection.RIGHT -> meshData.rightBezierOffsets
                        BezierDirection.BOTTOM -> meshData.bottomBezierOffsets
                    }
                BezierControlPoint(point, bezierOffsets[index], direction, Size(width, height)) {
                    dragAmount ->
                    val w = widthState.value
                    val h = heightState.value
                    val currentList =
                        when (direction) {
                            BezierDirection.LEFT -> meshData.leftBezierOffsets
                            BezierDirection.TOP -> meshData.topBezierOffsets
                            BezierDirection.RIGHT -> meshData.rightBezierOffsets
                            BezierDirection.BOTTOM -> meshData.bottomBezierOffsets
                        }
                    if (index < currentList.size) {
                        val currentOffset =
                            if (currentList[index].isUnspecified) direction.defaultOffset
                            else currentList[index]
                        currentList[index] =
                            currentOffset + Offset(dragAmount.x / w, dragAmount.y / h)
                    }
                }
            }
        }
    }

    selectedPointIndex?.let { index ->
        ColorPickerDialog(
            currentColor = meshData.colors[index],
            onDismiss = { selectedPointIndex = null },
            onColorPicked = { color ->
                if (index < meshData.colors.size) {
                    meshData.apply { colors[index] = color }
                }
                selectedPointIndex = null
            },
        )
    }
}

@Composable
private fun BezierControlPoint(
    basePosition: Offset,
    bezierOffset: Offset,
    bezierDirection: BezierDirection,
    size: Size,
    onDrag: (dragAmount: Offset) -> Unit,
) {
    val onDragState = rememberUpdatedState(onDrag)
    val control1Offset =
        basePosition + if (bezierOffset.isSpecified) bezierOffset else bezierDirection.defaultOffset
    val control1PixelOffset = Offset(control1Offset.x * size.width, control1Offset.y * size.height)

    val handleSize = 16.dp
    val handleOffset = with(LocalDensity.current) { (handleSize / 2).roundToPx() }
    Box(
        modifier =
            Modifier.offset {
                    IntOffset(
                        control1PixelOffset.x.roundToInt() - handleOffset,
                        control1PixelOffset.y.roundToInt() - handleOffset,
                    )
                }
                .size(handleSize)
                .clip(CircleShape)
                .background(
                    when (bezierDirection) {
                        BezierDirection.LEFT -> Color.Red
                        BezierDirection.TOP -> Color.Green
                        BezierDirection.RIGHT -> Color.Blue
                        BezierDirection.BOTTOM -> Color.Yellow
                    }
                )
                .border(1.dp, Color.Black, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragState.value(dragAmount)
                    }
                }
    )
}

private enum class BezierDirection(val defaultOffset: Offset) {
    LEFT(Offset(-0.1f, 0f)),
    TOP(Offset(0f, -0.1f)),
    RIGHT(Offset(0.1f, 0f)),
    BOTTOM(Offset(0f, 0.1f)),
}

@Composable
private fun GradientOptions(
    meshState: MeshData,
    showGradientControls: Boolean,
    hasBicubicColorInterpolation: Boolean,
    onGradientChange:
        (
            hasBicubicColorInterpolation: Boolean,
            rows: Int,
            columns: Int,
            showGradientControls: Boolean,
        ) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp).verticalScroll(scrollState)) {
        Text("Rows: ${meshState.rows}")
        Slider(
            value = meshState.rows.toFloat(),
            onValueChange = {
                val newRows = it.roundToInt()
                if (newRows != meshState.rows) {
                    onGradientChange(
                        hasBicubicColorInterpolation,
                        newRows,
                        meshState.columns,
                        showGradientControls,
                    )
                }
            },
            valueRange = 1f..10f,
            steps = 8,
        )
        Text("Columns: ${meshState.columns}")
        Slider(
            value = meshState.columns.toFloat(),
            onValueChange = {
                val newColumns = it.roundToInt()
                if (newColumns != meshState.columns) {
                    onGradientChange(
                        hasBicubicColorInterpolation,
                        meshState.rows,
                        newColumns,
                        showGradientControls,
                    )
                }
            },
            valueRange = 1f..10f,
            steps = 8,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Bicubic Color Interpolation")
            Switch(
                checked = hasBicubicColorInterpolation,
                onCheckedChange = { value ->
                    onGradientChange(value, meshState.rows, meshState.columns, showGradientControls)
                },
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Show Point Controls")
            Switch(
                checked = showGradientControls,
                onCheckedChange = { value ->
                    onGradientChange(
                        hasBicubicColorInterpolation,
                        meshState.rows,
                        meshState.columns,
                        value,
                    )
                },
            )
        }
        Spacer(Modifier.height(20.dp))
        Hints()
    }
}

@Composable
private fun Hints() {
    val tipsTextStyle = remember {
        TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Color.Gray)
    }
    Column(Modifier.padding(8.dp)) {
        Text(
            "1. You can tap on points to change their colors, drag them around to set their positions.",
            style = tipsTextStyle,
        )
        Text(
            "2. Each point has 4 bezier control points which are color coded as follows, " +
                "RED -> LEFT, GREEN -> TOP, YELLOW -> BOTTOM and BLUE -> RIGHT. They can be dragged around to affect the corresponding edge.",
            style = tipsTextStyle,
        )
    }
}

@Composable
private fun ColorPickerDialog(
    currentColor: Color,
    onColorPicked: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var red by remember(currentColor) { mutableFloatStateOf(currentColor.red) }
    var green by remember(currentColor) { mutableFloatStateOf(currentColor.green) }
    var blue by remember(currentColor) { mutableFloatStateOf(currentColor.blue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color") },
        text = {
            Column(Modifier.fillMaxWidth().height(180.dp)) {
                Box(Modifier.fillMaxWidth().height(50.dp).background(Color(red, green, blue)))
                Column(Modifier.fillMaxSize()) {
                    Slider(
                        value = red,
                        valueRange = 0f..1f,
                        onValueChange = { value -> red = value },
                        colors = SliderDefaults.colors(thumbColor = Color.Red),
                    )
                    Slider(
                        value = green,
                        valueRange = 0f..1f,
                        onValueChange = { value -> green = value },
                        colors = SliderDefaults.colors(thumbColor = Color.Green),
                    )
                    Slider(
                        value = blue,
                        valueRange = 0f..1f,
                        onValueChange = { value -> blue = value },
                        colors = SliderDefaults.colors(thumbColor = Color.Blue),
                    )
                }
            }
        },
        buttons = {
            Box(
                Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Button(onClick = { onColorPicked(Color(red, green, blue)) }) { Text("Apply") }
            }
        },
    )
}

@SuppressLint("PrimitiveInCollection")
private fun generateLinearMeshState(rows: Int, columns: Int): MeshData {
    val positions =
        SnapshotStateList((rows + 1) * (columns + 1)) { index ->
            val row = index / (columns + 1)
            val col = index % (columns + 1)
            val x = if (columns > 0) col.toFloat() / columns else 0f
            val y = if (rows > 0) row.toFloat() / rows else 0f
            Offset(x, y)
        }

    val colors =
        SnapshotStateList((rows + 1) * (columns + 1)) {
            Color(red = (0..255).random(), green = (0..255).random(), blue = (0..255).random())
        }

    val leftBezierOffsets = SnapshotStateList((rows + 1) * (columns + 1)) { Offset.Unspecified }
    val rightBezierOffsets = SnapshotStateList((rows + 1) * (columns + 1)) { Offset.Unspecified }
    val topBezierOffsets = SnapshotStateList((rows + 1) * (columns + 1)) { Offset.Unspecified }
    val bottomBezierOffsets = SnapshotStateList((rows + 1) * (columns + 1)) { Offset.Unspecified }

    return MeshData(
        rows,
        columns,
        positions,
        colors,
        leftBezierOffsets,
        rightBezierOffsets,
        topBezierOffsets,
        bottomBezierOffsets,
    )
}

@SuppressLint("PrimitiveInCollection")
data class MeshData(
    val rows: Int,
    val columns: Int,
    val positions: SnapshotStateList<Offset>,
    val colors: SnapshotStateList<Color>,
    val leftBezierOffsets: SnapshotStateList<Offset>,
    val rightBezierOffsets: SnapshotStateList<Offset>,
    val topBezierOffsets: SnapshotStateList<Offset>,
    val bottomBezierOffsets: SnapshotStateList<Offset>,
)
