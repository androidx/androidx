/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.compose.Ambient
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.px

/**
 * A bridge class between user interaction to the text composables for text selection.
 */
class SelectionManager : SelectionRegistrar {
    /**
     * The current selection.
     */
    var selection: Selection? = null

    /**
     * The manager will invoke this every time it comes to the conclusion that the selection should
     * change. The expectation is that this callback will end up causing `setSelection` to get
     * called. This is what makes this a "controlled component".
     */
    var onSelectionChange: (Selection?) -> Unit = {}

    /**
     * The selection mode. The default value is Vertical.
     */
    var mode: SelectionMode = SelectionMode.Vertical

    /**
     * Layout Coordinates of the selection container.
     */
    lateinit var containerLayoutCoordinates: LayoutCoordinates

    /**
     * This is essentially the list of registered components that want
     * to handle text selection that are below the SelectionContainer.
     */
    private val handlers = mutableSetOf<TextSelectionHandler>()

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    private var dragBeginPosition = PxPosition.Origin

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = PxPosition.Origin

    /**
     * A flag to check if the selection start or end handle is being dragged.
     * If this value is true, then onPress will not select any text.
     * This value will be set to true when either handle is being dragged, and be reset to false
     * when the dragging is stopped.
     */
    private var draggingHandle = false

    /**
     * Allow a Text composable to "register" itself with the manager
     */
    override fun subscribe(handler: TextSelectionHandler): Any {
        handlers.add(handler)
        return handler
    }

    /**
     * Allow a Text composable to "unregister" itself with the manager
     */
    override fun unsubscribe(key: Any) {
        handlers.remove(key as TextSelectionHandler)
    }

    fun onPress(position: PxPosition) {
        if (draggingHandle) return
        var result: Selection? = null
        for (handler in handlers) {
            result += handler.getSelection(
                Pair(position, position),
                containerLayoutCoordinates,
                mode)
        }
        onSelectionChange(result)
    }

    /**
     * Adjust coordinates for given text offset.
     *
     * Currently [android.text.Layout.getLineBottom] returns y coordinates of the next
     * line's top offset, which is not included in current line's hit area. To be able to
     * hit current line, move up this y coordinates by 1 pixel.
     */
    fun getAdjustedCoordinates(p: PxPosition): PxPosition {
        return PxPosition(p.x, p.y - 1.px)
    }

    fun handleDragObserver(dragStartHandle: Boolean): DragObserver {
        return object : DragObserver {
            override fun onStart(downPosition: PxPosition) {
                // The LayoutCoordinates of the composable where the drag gesture should begin. This
                // is used to convert the position of the beginning of the drag gesture from the
                // composable coordinates to selection container coordinates.
                val beginLayoutCoordinates =
                    if (dragStartHandle) {
                        selection!!.startLayoutCoordinates!!
                    } else {
                        selection!!.endLayoutCoordinates!!
                    }
                // The position of the character where the drag gesture should begin. This is in
                // the composable coordinates.
                val beginCoordinates =
                    getAdjustedCoordinates(
                        if (dragStartHandle) {
                            selection!!.startCoordinates
                        } else {
                            selection!!.endCoordinates
                        }
                    )
                // Convert the position where drag gesture begins from composable coordinates to
                // selection container coordinates.
                dragBeginPosition = containerLayoutCoordinates.childToLocal(
                    beginLayoutCoordinates,
                    beginCoordinates
                )

                // Zero out the total distance that being dragged.
                dragTotalDistance = PxPosition.Origin
                draggingHandle = true
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                var result = selection
                dragTotalDistance += dragDistance

                val currentStart =
                    if (dragStartHandle) {
                        dragBeginPosition + dragTotalDistance
                    } else {
                        containerLayoutCoordinates.childToLocal(
                            selection!!.startLayoutCoordinates!!,
                            getAdjustedCoordinates(selection!!.startCoordinates)
                        )
                    }

                val currentEnd =
                    if (dragStartHandle) {
                        containerLayoutCoordinates.childToLocal(
                            selection!!.endLayoutCoordinates!!,
                            getAdjustedCoordinates(selection!!.endCoordinates)
                        )
                    } else {
                        dragBeginPosition + dragTotalDistance
                    }

                for (handler in handlers) {
                    result += handler.getSelection(
                        Pair(currentStart, currentEnd),
                        containerLayoutCoordinates,
                        mode)
                }
                onSelectionChange(result)
                return dragDistance
            }

            override fun onStop(velocity: PxPosition) {
                super.onStop(velocity)
                draggingHandle = false
            }
        }
    }
}

/**
 * Ambient of SelectionRegistrar for SelectionManager.
 */
val SelectionRegistrarAmbient = Ambient.of<SelectionRegistrar> { SelectionManager() }
