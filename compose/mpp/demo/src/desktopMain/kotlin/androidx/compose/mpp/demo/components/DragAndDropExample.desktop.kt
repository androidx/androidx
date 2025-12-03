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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun DragAndDropExample() {
    val exportedText = "Hello, DnD!"
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize()
    ) {
        val textMeasurer = rememberTextMeasurer()
        Box(
            Modifier
            .size(200.dp)
            .background(Color.LightGray)
            .dragAndDropSource(
                drawDragDecoration = {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(x = 0f, y = size.height/4),
                        size = Size(size.width, size.height/2)
                    )
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(exportedText),
                        layoutDirection = layoutDirection,
                        density = this
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = (size.width - textLayoutResult.size.width) / 2,
                            y = (size.height - textLayoutResult.size.height) / 2,
                        )
                    )
                }
            ) { offset ->
                DragAndDropTransferData(
                    transferable = DragAndDropTransferable(
                        StringSelection(exportedText)
                    ),
                    supportedActions = listOf(
                        DragAndDropTransferAction.Copy,
                        DragAndDropTransferAction.Move,
                        DragAndDropTransferAction.Link,
                    ),
                    dragDecorationOffset = offset,
                    onTransferCompleted = { action ->
                        println("Action at source: $action")
                    }
                )
            }
        ) {
            Text("Drag Me", Modifier.align(Alignment.Center))
        }

        var showTargetBorder by remember { mutableStateOf(false) }
        var targetText by remember { mutableStateOf("Drop Here") }
        val coroutineScope = rememberCoroutineScope()
        val dragAndDropTarget = remember {
            object: DragAndDropTarget {

                override fun onStarted(event: DragAndDropEvent) {
                    showTargetBorder = true
                }

                override fun onEnded(event: DragAndDropEvent) {
                    showTargetBorder = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    println("Action at target: ${event.action}")
                    val result = (targetText == "Drop Here")// && (event.action == DragAndDropTransferAction.Link)
                    targetText = event.awtTransferable.let {
                        if (it.isDataFlavorSupported(DataFlavor.stringFlavor))
                            it.getTransferData(DataFlavor.stringFlavor) as String
                        else
                            it.transferDataFlavors.first().humanPresentableName
                    }
                    coroutineScope.launch {
                        delay(2000)
                        targetText = "Drop Here"
                    }
                    return result
                }
            }
        }

        Box(
            Modifier
            .size(200.dp)
            .background(Color.LightGray)
            .then(
                if (showTargetBorder)
                    Modifier.border(BorderStroke(3.dp, Color.Black))
                else
                    Modifier
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            )
        ) {
            Text(targetText, Modifier.align(Alignment.Center))
        }
    }
}