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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import org.w3c.dom.DataTransfer
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
@OptIn(ExperimentalComposeUiApi::class)
actual fun DragAndDropExample() {
    val exportedText = "Hello, DnD!"
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DraggableColorSource(Color.Red)
                DraggableColorSource(Color(0xFFFFA500))
                DraggableColorSource(Color.Yellow)
                DraggableColorSource(Color.Green)
                DraggableColorSource(Color(0xFFADD8E6))
                DraggableColorSource(Color.Blue)
                DraggableColorSource(Color(0xFF8A2BE2))
            }
        }

        var showTargetBorder by remember { mutableStateOf(false) }
        var showHovered by remember { mutableStateOf(false) }
        var dragCounter by remember { mutableStateOf(0) }
        val pieSlices = remember { mutableStateListOf<Color>() }

        val rotation by rememberInfiniteTransition(label = "segmentedHoverRotation").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 8000, easing = LinearEasing)),
            label = "rotationDegrees"
        )
        val effectiveRotation = if (showTargetBorder && !showHovered) rotation else 0f

        val dragAndDropTarget = remember {
            object: DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    showTargetBorder = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    showTargetBorder = false
                }

                override fun onEntered(event: DragAndDropEvent) {
                    showHovered = true
                }

                override fun onExited(event: DragAndDropEvent) {
                    showHovered = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    showHovered = false
                    event.transferData?.domDataTransferOrNull?.let { dataTransfer ->
                        val dataText = dataTransfer?.getData("text/plain") ?: ""
                        val droppedColor = if (dataText.startsWith("color:")) {
                            Color(dataText.removePrefix("color:").toULong(16))
                        } else {
                            Color.Cyan
                        }
                        pieSlices.add(droppedColor)
                    }
                    dragCounter++
                    return true
                }
            }
        }

        val glowPadding = 24.dp

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp + glowPadding * 2)
                .drawBehind {
                    if (showTargetBorder) {
                        val outerR = size.minDimension / 2f
                        if (pieSlices.isNotEmpty()) {
                            // Segmented glow: draw a colored ring mirroring the pie chart segments
                            val total = pieSlices.size.toFloat()
                            var start = -90f + effectiveRotation
                            val arcSize = Size(outerR * 2f, outerR * 2f)
                            val topLeft = Offset(center.x - outerR, center.y - outerR)

                            val counts = pieSlices.groupingBy { it }.eachCount()
                            counts.entries.forEach { (color, count) ->
                                val sweep = (count.toFloat() / total) * 360f
                                drawArc(
                                    color = color.copy(alpha = 0.6f),
                                    startAngle = start,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = glowPadding.toPx())
                                )
                                start += sweep
                            }
                        } else {
                            drawCircle(
                                color = Color.Gray,
                                radius = outerR - glowPadding.toPx() / 2f,
                                center = center,
                                style = Stroke(width = glowPadding.toPx())
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray, shape = CircleShape)
                    .clip(CircleShape)
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { true },
                        target = dragAndDropTarget
                    ),
                contentAlignment = Alignment.Center
            ) {

                // Draw a pie chart based on dropped colors
                Canvas(Modifier.fillMaxSize()) {
                    if (pieSlices.isNotEmpty()) {
                        val total = pieSlices.size.toFloat()
                        var start = -90f

                        val counts = pieSlices.groupingBy { it }.eachCount()
                        counts.entries.forEach { (color, count) ->
                            val sweep = (count.toFloat() / total) * 360f
                            drawArc(
                                color = color,
                                startAngle = start,
                                sweepAngle = sweep,
                                useCenter = true,
                                size = size
                            )
                            start += sweep
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun DraggableColorSource(color: Color) {
    Box(
        Modifier
            .size(56.dp)
            .background(color, shape = CircleShape)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, Color.Black), shape = CircleShape)
            .dragAndDropSource(
                drawDragDecoration = {
                    val r = size.minDimension / 2f
                    val radius = listOf(r * 0.6f, r * 0.8f, r * 0.95f)
                    val widths = listOf(r * 0.14f, r * 0.1f, r * 0.06f)
                    drawCircle(color = color.copy(alpha = 0.25f), radius = radius[0], center = center,
                        style = Stroke(width = widths[0]))
                    drawCircle(color = color.copy(alpha = 0.5f), radius = radius[1], center = center,
                        style = Stroke(width = widths[1]))
                    drawCircle(color = color.copy(alpha = 0.9f), radius = radius[2], center = center,
                        style = Stroke(width = widths[2])
                    )
                },
                transferData = { _ ->
                    val dataTransfer = createDataTransfer()
                    dataTransfer.setData("text/plain", "color:${color.value.toString(radix = 16)}")
                    DragAndDropTransferData(dataTransfer)
                }
            )
    )
}

private fun createDataTransfer(): DataTransfer =  js("new DataTransfer()")