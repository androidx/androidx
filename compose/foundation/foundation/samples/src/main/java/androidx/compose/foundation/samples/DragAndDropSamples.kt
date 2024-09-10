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

package androidx.compose.foundation.samples

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.view.View
import androidx.annotation.Sampled
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun DragAndDropMultiAppSample() {
    var dragAndDropEventSummary by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextDragAndDropSourceSample(modifier = Modifier.fillMaxWidth())

        Spacer(
            modifier = Modifier.height(24.dp),
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            TextDragAndDropTargetSample(
                eventSummary = dragAndDropEventSummary,
                onDragAndDropEventDropped = { event -> dragAndDropEventSummary = event.summary() }
            )
            if (dragAndDropEventSummary != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    IconButton(
                        onClick = { dragAndDropEventSummary = null },
                        content = {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    )
                }
            }
        }
    }
}

@Sampled
@Composable
fun TextDragAndDropSourceSample(modifier: Modifier) {
    val label = remember { "Drag me" }
    Box(
        modifier =
            modifier
                .dragAndDropSource {
                    DragAndDropTransferData(
                        clipData = ClipData.newPlainText(label, label),
                        flags = View.DRAG_FLAG_GLOBAL,
                    )
                }
                .border(
                    border =
                        BorderStroke(
                            width = 4.dp,
                            brush = Brush.linearGradient(listOf(Color.Magenta, Color.Magenta))
                        ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
    ) {
        Text(modifier = Modifier.align(Alignment.Center), text = label)
    }
}

@Sampled
@Composable
fun TextDragAndDropTargetSample(
    eventSummary: String?,
    onDragAndDropEventDropped: (DragAndDropEvent) -> Unit,
) {
    val validMimeTypePrefixes = remember {
        setOf(
            ClipDescription.MIMETYPE_TEXT_INTENT,
            "image/",
            "text/",
            "video/",
            "audio/",
        )
    }
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                backgroundColor = Color.DarkGray.copy(alpha = 0.2f)
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                onDragAndDropEventDropped(event)
                return true
            }

            override fun onEnded(event: DragAndDropEvent) {
                backgroundColor = Color.Transparent
            }
        }
    }
    Box(
        modifier =
            Modifier.fillMaxSize()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = accept@{ startEvent ->
                            val hasValidMimeType =
                                startEvent.mimeTypes().any { eventMimeType ->
                                    validMimeTypePrefixes.any(eventMimeType::startsWith)
                                }
                            hasValidMimeType
                        },
                    target = dragAndDropTarget,
                )
                .background(backgroundColor)
                .border(width = 4.dp, color = Color.Magenta, shape = RoundedCornerShape(16.dp)),
    ) {
        when (eventSummary) {
            null -> Text(modifier = Modifier.align(Alignment.Center), text = "Drop anything here")
            else ->
                Text(
                    modifier =
                        Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            .verticalScroll(rememberScrollState()),
                    text = eventSummary
                )
        }
    }
}

private fun DragAndDropEvent.summary() =
    (0 until toAndroidDragEvent().clipData.itemCount)
        .map(toAndroidDragEvent().clipData::getItemAt)
        .withIndex()
        .joinToString(separator = "\n\n") { (index, clipItem) ->
            val mimeTypes =
                (0 until toAndroidDragEvent().clipData.description.mimeTypeCount).joinToString(
                    separator = ", ",
                    transform = toAndroidDragEvent().clipData.description::getMimeType
                )
            listOfNotNull(
                    "index: $index",
                    "mimeTypes: $mimeTypes",
                    clipItem.text?.takeIf(CharSequence::isNotEmpty)?.let { "text: $it" },
                    clipItem.htmlText?.takeIf(CharSequence::isNotEmpty)?.let { "html text: $it" },
                    clipItem.uri?.toString()?.let { "uri: $it" },
                    clipItem.intent?.let {
                        "intent: action - ${it.action}; extras size: ${it.extras?.size()}"
                    },
                )
                .joinToString(separator = "\n")
        }

@Composable
fun DragAndDropNestedSample() {
    Column(modifier = Modifier.fillMaxSize()) {
        TwoByTwoGrid(
            modifier =
                Modifier.padding(16.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .animatedDragAndDrop(
                        prefix = "Main",
                        level = 0,
                        rowAndColumn = RowAndColumn(row = 0, column = 0)
                    ),
        ) { outerRowAndColumn ->
            TwoByTwoGrid(
                modifier =
                    Modifier.padding(16.dp)
                        .weight(1f)
                        .fillMaxWidth()
                        .animatedDragAndDrop(
                            prefix = "Outer",
                            level = 1,
                            rowAndColumn = outerRowAndColumn,
                        ),
            ) { innerRowAndColumn ->
                Box(
                    modifier =
                        Modifier.padding(16.dp)
                            .weight(1f)
                            .fillMaxSize()
                            .animatedDragAndDrop(
                                prefix = "Inner ",
                                level = 2,
                                rowAndColumn = innerRowAndColumn,
                            ),
                )
            }
        }
        ColorSwatch()
    }
}

@Composable
private fun ColorSwatch() {
    Row(
        modifier =
            Modifier.padding(16.dp)
                .height(56.dp)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Colors.forEach { color -> DragAndDropSourceWithColoredDragShadowSample(color) }
    }
}

@Sampled
@Composable
fun DragAndDropSourceWithColoredDragShadowSample(color: Color) {
    Box(
        modifier =
            Modifier.size(56.dp).background(color = color).dragAndDropSource(
                drawDragDecoration = { drawRect(color) },
            ) {
                color.toDragAndDropTransfer()
            }
    )
}

@Composable
private fun TwoByTwoGrid(
    modifier: Modifier = Modifier,
    child: @Composable (RowScope.(rowAndColumn: RowAndColumn) -> Unit)
) {
    Column(modifier = modifier) {
        repeat(2) { column ->
            Row(modifier = Modifier.weight(1f).fillMaxSize()) {
                repeat(2) { row -> child(RowAndColumn(row, column)) }
            }
        }
    }
}

@Composable
private fun Modifier.animatedDragAndDrop(
    prefix: String,
    level: Int,
    rowAndColumn: RowAndColumn
): Modifier {
    val state = remember { State(prefix = prefix, level = level, rowAndColumn = rowAndColumn) }
    return this.stateDragSource(state)
        .stateDropTarget(state)
        .background(state.animatedColor)
        .rotate(state.animatedRotation)
        .offset(state.animatedTranslation, state.animatedTranslation)
}

private fun Modifier.stateDragSource(state: State) =
    dragAndDropSource(
        drawDragDecoration = { drawRoundRect(state.color) },
    ) {
        state.color.toDragAndDropTransfer()
    }

@Composable
private fun Modifier.stateDropTarget(state: State): Modifier {
    val dragAndDropTarget =
        remember(state) {
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    state.onStarted()
                }

                override fun onEntered(event: DragAndDropEvent) {
                    state.onEntered()
                    println("Entered ${state.name}")
                }

                override fun onMoved(event: DragAndDropEvent) {
                    println("Moved in ${state.name}")
                }

                override fun onExited(event: DragAndDropEvent) {
                    println("Exited ${state.name}")
                    state.onExited()
                }

                override fun onEnded(event: DragAndDropEvent) {
                    println("Ended in ${state.name}")
                    state.onEnded()
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    println("Dropped items in ${state.name}")
                    return when (
                        val transferredColor = event.toAndroidDragEvent().clipData.color()
                    ) {
                        null -> false
                        else -> {
                            state.onDropped(transferredColor)
                            true
                        }
                    }
                }
            }
        }
    return dragAndDropTarget(
        shouldStartDragAndDrop = { startEvent ->
            startEvent.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_INTENT)
        },
        target = dragAndDropTarget
    )
}

@Stable
private class State(
    val prefix: String,
    val level: Int,
    val rowAndColumn: RowAndColumn,
) {
    var color by mutableStateOf(startColor)
        private set

    var isInside by mutableStateOf(false)
        private set

    var isInDnD by mutableStateOf(false)
        private set

    fun onStarted() {
        isInDnD = true
    }

    fun onEntered() {
        isInside = true
    }

    fun onExited() {
        isInside = false
    }

    fun onDropped(color: Color) {
        this.color = color
    }

    fun onEnded() {
        isInside = false
        isInDnD = false
    }
}

private val State.name
    get() =
        with(rowAndColumn) { "$prefix${Letters.circularGet(row)}, ${Numbers.circularGet(column)}" }
private val State.startColor
    get() =
        when (level % 2) {
            0 -> Colors.drop(Colors.size / 2)
            else -> Colors.take(Colors.size / 2)
        }.circularGet(colorIndex + level)

private val State.colorIndex
    get() = with(rowAndColumn) { (row * 2) + column }

private val State.animatedColor: Color
    @Composable
    get() =
        rememberInfiniteTransition(label = "color")
            .animateColor(
                initialValue = color,
                targetValue = if (isInside) Color.DarkGray else color,
                animationSpec =
                    infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
                label = "background color"
            )
            .value

private val State.animatedRotation: Float
    @Composable
    get() =
        rememberInfiniteTransition(label = "rotation")
            .animateFloat(
                initialValue = if (isInDnD) -0.2f else 0f,
                targetValue = if (isInDnD) 0.2f else 0f,
                animationSpec =
                    infiniteRepeatable(animation = jiggleSpec(), repeatMode = RepeatMode.Reverse),
                label = "rotation"
            )
            .value

private val State.animatedTranslation: Dp
    @Composable
    get() =
        rememberInfiniteTransition(label = "translation")
            .animateFloat(
                initialValue = if (isInDnD) -0.02f else 0f,
                targetValue = if (isInDnD) 0.02f else 0f,
                animationSpec =
                    infiniteRepeatable(animation = jiggleSpec(), repeatMode = RepeatMode.Reverse),
                label = "translation"
            )
            .value
            .dp

private fun jiggleSpec(): DurationBasedAnimationSpec<Float> =
    tween(durationMillis = 70 + (Random.nextInt(30)))

private data class RowAndColumn(val row: Int, val column: Int)

private fun Color.toDragAndDropTransfer() =
    DragAndDropTransferData(
        clipData = ClipData.newIntent("color transfer", colorDragAndDropTransferIntent(this))
    )

private fun ClipData.color() =
    (0 until itemCount).map(::getItemAt).firstNotNullOfOrNull { item ->
        item?.intent?.getIntExtra(ColorTransferData, -1)?.takeIf { it != -1 }?.let(::Color)
    }

private fun colorDragAndDropTransferIntent(color: Color) =
    Intent(ColorTransferAction).apply { putExtra(ColorTransferData, color.toArgb()) }

private const val ColorTransferAction = "action.color.transfer"
private const val ColorTransferData = "data.color.transfer"

private fun <T> List<T>.circularGet(index: Int) = get(index % size)

private val Letters = ('A'..'Z').toList()
private val Numbers = ('1'..'9').toList()

private val Colors =
    listOf(
        Color(0xFF2980b9), // Belize Hole
        Color(0xFF2c3e50), // Midnight Blue
        Color(0xFFc0392b), // Pomegranate
        Color(0xFF16a085), // Green Sea
        Color(0xFF7f8c8d), // Concrete
        Color(0xFFC6973B), // Mustard
        Color(0xFFF6CAB7), // Blush
        Color(0xFF6D4336), // Brown
        Color(0xFF814063), // Plum
    )
