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
import androidx.ui.engine.geometry.Rect

internal class SelectionManager : SelectionRegistrar {
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
        var result: Selection? = null
        for (handler in handlers) {
            result += handler.getSelection(
                Pair(position, position),
                containerLayoutCoordinates,
                mode)
        }
        onSelectionChange(result)
    }

    // Get the coordinates of a character. Currently, it's the middle point of the left edge of the
    // bounding box of the character. This is a temporary solution.
    // TODO(qqd): Read how Android solve this problem.
    fun getCoordinatesForCharacter(box: Rect): PxPosition {
        return PxPosition(box.left.px, box.top.px + (box.bottom.px - box.top.px) / 2)
    }

    fun handleDragObserver(dragStartHandle: Boolean): DragObserver {
        return object : DragObserver {
            override fun onStart() {
                // The LayoutCoordinates of the widget where the drag gesture should begin. This
                // is used to convert the position of the beginning of the drag gesture from the
                // widget coordinates to selection container coordinates.
                val beginLayoutCoordinates =
                    if (dragStartHandle) {
                        selection!!.startLayoutCoordinates!!
                    } else {
                        selection!!.endLayoutCoordinates!!
                    }
                // The position of the character where the drag gesture should begin. This is in
                // the widget coordinates.
                val beginCoordinates =
                    getCoordinatesForCharacter(
                        if (dragStartHandle) {
                            selection!!.startOffset
                        } else {
                            selection!!.endOffset
                        }
                    )
                // Convert the position where drag gesture begins from widget coordinates to
                // selection container coordinates.
                dragBeginPosition = containerLayoutCoordinates.childToLocal(
                    beginLayoutCoordinates,
                    beginCoordinates
                )

                // Zero out the total distance that being dragged.
                dragTotalDistance = PxPosition.Origin
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
                            getCoordinatesForCharacter(selection!!.startOffset)
                        )
                    }

                val currentEnd =
                    if (dragStartHandle) {
                        containerLayoutCoordinates.childToLocal(
                            selection!!.endLayoutCoordinates!!,
                            getCoordinatesForCharacter(selection!!.endOffset)
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
        }
    }
}

/** Ambient of SelectionRegistrar for SelectionManager. */
internal val SelectionRegistrarAmbient = Ambient.of<SelectionRegistrar> { SelectionManager() }
