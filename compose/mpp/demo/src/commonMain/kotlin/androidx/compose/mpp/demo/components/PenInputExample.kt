/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The 16 EGA (Enhanced Graphics Adapter) colors used as a basic palette in the demo.
 */
private val Palette = listOf(
    Color(0xFF000000),
    Color(0xFF0000AA),
    Color(0xFF00AA00),
    Color(0xFF00AAAA),
    Color(0xFFAA0000),
    Color(0xFFAA00AA),
    Color(0xFFAA5500),
    Color(0xFFAAAAAA),
    Color(0xFF555555),
    Color(0xFF5555FF),
    Color(0xFF55FF55),
    Color(0xFF55FFFF),
    Color(0xFFFF5555),
    Color(0xFFFF55FF),
    Color(0xFFFFFF55),
    Color(0xFFFFFFFF),
)

private data class PenPoint(
    val offset: Offset,
    val pressure: Float,
    val type: PointerType,
)

private class PenStroke(
    val points: MutableList<PenPoint> = mutableStateListOf(),
    val color: Color,
)

@Composable
fun PenInputExample() {
    val strokes = remember { mutableStateListOf<PenStroke>() }
    val redoStack = remember { mutableStateListOf<PenStroke>() }
    var currentStroke by remember { mutableStateOf<PenStroke?>(null) }
    var lastInfo by remember { mutableStateOf("") }
    var strokeColor by remember { mutableStateOf(Color.Black) }
    var canvasBackgroundColor by remember { mutableStateOf(Color(0xFFF5F5F5)) }
    var showBackgroundPicker by remember { mutableStateOf(false) }

    val undo: () -> Unit = {
        if (currentStroke == null && strokes.isNotEmpty()) {
            redoStack.add(strokes.removeLast())
        }
    }
    val redo: () -> Unit = {
        if (currentStroke == null && redoStack.isNotEmpty()) {
            strokes.add(redoStack.removeLast())
        }
    }

    Column(
        Modifier.fillMaxSize()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (color in Palette) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color)
                        .border(
                            width = if (color == strokeColor) 2.dp else 1.dp,
                            color = if (color == strokeColor) Color.Cyan else Color.Gray,
                        )
                        .clickable { strokeColor = color }
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(canvasBackgroundColor)
                    .border(width = 1.dp, color = Color.Gray)
                    .clickable { showBackgroundPicker = !showBackgroundPicker },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "\uD83C\uDFA8", textAlign = TextAlign.Center)
                DropdownMenu(
                    expanded = showBackgroundPicker,
                    onDismissRequest = { showBackgroundPicker = false },
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (color in Palette) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(color)
                                    .border(
                                        width = if (color == canvasBackgroundColor) 2.dp else 1.dp,
                                        color = if (color == canvasBackgroundColor) Color.Cyan else Color.Gray,
                                    )
                                    .clickable {
                                        canvasBackgroundColor = color
                                        showBackgroundPicker = false
                                    }
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(width = 1.dp, color = Color.Gray)
                    .clickable { undo() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "↶", textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(width = 1.dp, color = Color.Gray)
                    .clickable { redo() },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "↷", textAlign = TextAlign.Center)
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(width = 1.dp, color = Color.Gray)
                    .clickable {
                        strokes.clear()
                        redoStack.clear()
                        currentStroke = null
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⟳", textAlign = TextAlign.Center)
            }
        }
        Text(
            text = lastInfo,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .background(canvasBackgroundColor)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            val type = change.type
                            when (event.type) {
                                PointerEventType.Press -> {
                                    val s = PenStroke(color = strokeColor)
                                    s.points.add(PenPoint(change.position, change.pressure, type))
                                    strokes.add(s)
                                    redoStack.clear()
                                    currentStroke = s
                                    change.consume()
                                }
                                PointerEventType.Move -> {
                                    currentStroke?.let { s ->
                                        // Include any historical positions to avoid gaps on fast strokes.
                                        // HistoricalChange doesn't expose pressure, fall back to the
                                        // current change's pressure value.
                                        for (h in change.historical) {
                                            s.points.add(
                                                PenPoint(h.position, change.pressure, type)
                                            )
                                        }
                                        s.points.add(
                                            PenPoint(change.position, change.pressure, type)
                                        )
                                        change.consume()
                                    }
                                }
                                PointerEventType.Release -> {
                                    currentStroke = null
                                    change.consume()
                                }
                            }
                            lastInfo = "type=${type.name()}, " +
                                "pressure=${(change.pressure * 100).toInt() / 100f}, " +
                                "position=(${change.position.x.toInt()}, ${change.position.y.toInt()})"
                        }
                    }
                }
        ) {
            for (stroke in strokes) {
                drawStroke(stroke)
            }
        }
    }
}

private fun PointerType.name(): String = when (this) {
    PointerType.Touch -> "Touch"
    PointerType.Mouse -> "Mouse"
    PointerType.Stylus -> "Stylus"
    PointerType.Eraser -> "Eraser"
    else -> "Unknown"
}

private fun DrawScope.drawStroke(stroke: PenStroke) {
    val points = stroke.points
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(stroke.color, radius = widthFor(points[0]) / 2f, center = points[0].offset)
        return
    }
    // Draw stroke as a series of segments so each segment can have its own width based on pressure.
    // Useful for real stylus input where pressure can vary along the stroke.
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val w = (widthFor(a) + widthFor(b)) / 2f
        drawLine(
            color = stroke.color,
            start = a.offset,
            end = b.offset,
            strokeWidth = w,
            cap = StrokeCap.Round,
        )
    }
}

private fun widthFor(p: PenPoint): Float {
    val pressure = if (p.pressure > 0f) p.pressure else 0.5f
    return 2f + pressure * 18f
}
