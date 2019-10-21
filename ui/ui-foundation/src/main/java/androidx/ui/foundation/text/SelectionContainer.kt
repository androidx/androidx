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

package androidx.ui.foundation.text

import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Constraints
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.OnPositioned
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.selection.Selection
import androidx.ui.core.selection.SelectionManager
import androidx.ui.core.selection.SelectionMode
import androidx.ui.core.selection.SelectionRegistrarAmbient
import androidx.ui.text.style.TextDirection

/**
 * Selection Composable.
 *
 * The selection composable wraps composables and let them to be selectable. It paints the selection
 * area with start and end handles.
 */
@Composable
fun SelectionContainer(
    /** Current Selection status.*/
    selection: Selection?,
    /** A function containing customized behaviour when selection changes. */
    onSelectionChange: (Selection?) -> Unit,
    /** Selection mode. The default mode is Vertical. */
    mode: SelectionMode = SelectionMode.Vertical,
    children: @Composable() () -> Unit
) {
    val manager = +memo { SelectionManager() }
    // TODO (qqd): After selection composable is fully implemented, evaluate if the following 2 items
    // are expensive. If so, use
    // +memo(selection) { manager.selection = selection }
    // +memo(onSelectionChange) { manager.onSelectionChange = onSelectionChange }
    manager.selection = selection
    manager.onSelectionChange = onSelectionChange
    manager.mode = mode

    SelectionRegistrarAmbient.Provider(value = manager) {
        val content = @Composable {
            val content = @Composable() {
                // Get the layout coordinates of the selection container. This is for hit test of
                // cross-composable selection.
                OnPositioned(onPositioned = { manager.containerLayoutCoordinates = it })
                PressIndicatorGestureDetector(onStart = { position -> manager.onPress(position) }) {
                    children()
                }
            }
            Layout(content) { measurables, constraints ->
                val placeable = measurables.firstOrNull()?.measure(constraints)
                val width = placeable?.width ?: constraints.minWidth
                val height = placeable?.height ?: constraints.minHeight
                layout(width, height) {
                    placeable?.place(0.ipx, 0.ipx)
                }
            }
        }
        val startHandle = @Composable {
            TouchSlopDragGestureDetector(
                dragObserver = manager.handleDragObserver(dragStartHandle = true)
            ) {
                Layout(children = { StartSelectionHandle(selection) }) { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                }
            }
        }
        val endHandle = @Composable {
            TouchSlopDragGestureDetector(
                dragObserver = manager.handleDragObserver(dragStartHandle = false)
            ) {
                Layout(children = { EndSelectionHandle(selection) }) { _, constraints ->
                    layout(constraints.minWidth, constraints.minHeight) {}
                }
            }
        }
        Layout(content, startHandle, endHandle) { measurables, constraints ->
            val placeable = measurables[0].measure(constraints)
            val width = placeable.width
            val height = placeable.height
            val start =
                measurables[startHandle].first().measure(
                    Constraints.tightConstraints(
                        HANDLE_WIDTH.toIntPx(),
                        HANDLE_HEIGHT.toIntPx()
                    )
                )
            val end =
                measurables[endHandle].first().measure(
                    Constraints.tightConstraints(
                        HANDLE_WIDTH.toIntPx(),
                        HANDLE_HEIGHT.toIntPx()
                    )
                )
            layout(width, height) {
                placeable.place(IntPx.Zero, IntPx.Zero)

                val startLayoutCoordinates = selection?.startLayoutCoordinates
                val endLayoutCoordinates = selection?.endLayoutCoordinates
                if (startLayoutCoordinates != null && endLayoutCoordinates != null) {
                    val startOffset = manager.containerLayoutCoordinates.childToLocal(
                        startLayoutCoordinates,
                        selection.startCoordinates
                    )
                    val endOffset = manager.containerLayoutCoordinates.childToLocal(
                        endLayoutCoordinates,
                        selection.endCoordinates
                    )
                    val startAdjustedDistance =
                        if (selection.startDirection == TextDirection.Ltr) -HANDLE_WIDTH.toPx()
                        else 0.px
                    val endAdjustedDistance =
                        if (selection.endDirection == TextDirection.Ltr) 0.px
                        else -HANDLE_WIDTH.toPx()
                    start.place(startOffset.x + startAdjustedDistance, startOffset.y)
                    end.place(endOffset.x + endAdjustedDistance, endOffset.y)
                }
            }
        }
    }
}
