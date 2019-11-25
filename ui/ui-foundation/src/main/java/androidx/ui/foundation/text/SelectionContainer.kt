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
import androidx.ui.core.Alignment
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxPosition
import androidx.ui.core.OnPositioned
import androidx.ui.core.Popup
import androidx.ui.core.gesture.LongPressDragGestureDetector
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.core.ipx
import androidx.ui.core.selection.Selection
import androidx.ui.core.selection.SelectionRegistrarAmbient
import androidx.ui.layout.Container
import kotlin.math.ceil
import kotlin.math.roundToInt

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
    children: @Composable() () -> Unit
) {
    val registrarImpl = +memo { SelectionRegistrarImpl() }
    val manager = +memo { SelectionManager(registrarImpl) }
    manager.onSelectionChange = onSelectionChange
    manager.selection = selection

    SelectionRegistrarAmbient.Provider(value = registrarImpl) {
        val content = @Composable() {
            // Get the layout coordinates of the selection container. This is for hit test of
            // cross-composable selection.
            OnPositioned(onPositioned = { manager.containerLayoutCoordinates = it })
            PressGestureDetector(onRelease = { manager.onRelease() }) {
                LongPressDragGestureDetector(manager.longPressDragObserver) {
                    children()
                }
            }
        }
        Container {
            Container(children = content)
            DrawHandles(
                manager = manager,
                selection = selection,
                startHandle = { StartSelectionHandle(selection = selection) },
                endHandle = { EndSelectionHandle(selection = selection) })
        }
    }
}

@Composable
private fun DrawHandles(
    manager: SelectionManager,
    selection: Selection?,
    startHandle: @Composable() () -> Unit,
    endHandle: @Composable() () -> Unit
) {
    if (selection != null &&
        selection.start.layoutCoordinates != null &&
        selection.end.layoutCoordinates != null
    ) {
        val startOffset = manager.containerLayoutCoordinates.childToLocal(
            selection.start.layoutCoordinates!!,
            selection.start.coordinates
        )
        val endOffset = manager.containerLayoutCoordinates.childToLocal(
            selection.end.layoutCoordinates!!,
            selection.end.coordinates
        )

        Container {
            Popup(
                alignment =
                if (isHandleLtrDirection(selection.start.direction, selection.handlesCrossed)) {
                    Alignment.TopRight
                } else {
                    Alignment.TopLeft
                },
                offset = IntPxPosition(startOffset.x.value.toIntPx(), startOffset.y.value.toIntPx())
            ) {
                TouchSlopDragGestureDetector(
                    dragObserver = manager.handleDragObserver(isStartHandle = true),
                    children = startHandle
                )
            }
        }

        Container {
            Popup(
                alignment =
                if (isHandleLtrDirection(selection.end.direction, selection.handlesCrossed)) {
                    Alignment.TopLeft
                } else {
                    Alignment.TopRight
                },
                offset = IntPxPosition(endOffset.x.value.toIntPx(), endOffset.y.value.toIntPx())
            ) {
                TouchSlopDragGestureDetector(
                    dragObserver = manager.handleDragObserver(isStartHandle = false),
                    children = endHandle
                )
            }
        }
    }
}

private fun Float.toIntPx(): IntPx = ceil(this).roundToInt().ipx
