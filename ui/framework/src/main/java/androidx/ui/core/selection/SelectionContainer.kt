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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Constraints
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint

private val HANDLE_WIDTH = 100.px
private val HANDLE_HEIGHT = 100.px

/**
 * Selection Widget.
 *
 * The selection widget wraps composables and let them to be selectable. It paints the selection
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
    @Children children: @Composable() () -> Unit
) {
    val manager = +memo { SelectionManager() }
    // TODO (qqd): After selection widget is fully implemented, evaluate if the following 2 items
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
                // cross-widget selection.
                OnPositioned(onPositioned = { manager.containerLayoutCoordinates = it })
                PressIndicatorGestureDetector(onStart = { position -> manager.onPress(position) }) {
                    children()
                }
            }
            Layout(children = content, layoutBlock = { measurables, constraints ->
                val placeable = measurables.firstOrNull()?.measure(constraints)
                val width = placeable?.width ?: constraints.minWidth
                val height = placeable?.height ?: constraints.minHeight
                layout(width, height) {
                    placeable?.place(0.ipx, 0.ipx)
                }
            })
        }
        val startHandle = @Composable {
            DragGestureDetector(
                canDrag = { true },
                dragObserver = manager.handleDragObserver(dragStartHandle = true)
            ) {
                Layout(
                    children = { SelectionHandle() },
                    layoutBlock = { _, constraints ->
                        layout(constraints.minWidth, constraints.minHeight) {}
                    })
            }
        }
        val endHandle = @Composable {
            DragGestureDetector(
                canDrag = { true },
                dragObserver = manager.handleDragObserver(dragStartHandle = false)
            ) {
                Layout(
                    children = { SelectionHandle() },
                    layoutBlock = { _, constraints ->
                        layout(constraints.minWidth, constraints.minHeight) {}
                    })
            }
        }
        @Suppress("USELESS_CAST")
        (Layout(
        childrenArray = arrayOf(content, startHandle, endHandle),
        layoutBlock = { measurables, constraints ->
            val placeable = measurables[0].measure(constraints)
            val width = placeable.width
            val height = placeable.height
            val start =
                measurables[startHandle as () -> Unit].first().measure(
                    Constraints.tightConstraints(
                        HANDLE_WIDTH.round(),
                        HANDLE_HEIGHT.round()
                    )
                )
            val end =
                measurables[endHandle as () -> Unit].first().measure(
                    Constraints.tightConstraints(
                        HANDLE_WIDTH.round(),
                        HANDLE_HEIGHT.round()
                    )
                )
            layout(width, height) {
                placeable.place(IntPx.Zero, IntPx.Zero)
                if (selection != null &&
                    selection.startLayoutCoordinates != null &&
                    selection.endLayoutCoordinates != null
                ) {
                    val startOffset = manager.containerLayoutCoordinates.childToLocal(
                        selection.startLayoutCoordinates,
                        PxPosition(
                            selection.startOffset.left.px,
                            selection.startOffset.bottom.px
                        )
                    )
                    val endOffset = manager.containerLayoutCoordinates.childToLocal(
                        selection.endLayoutCoordinates,
                        PxPosition(
                            selection.endOffset.right.px,
                            selection.endOffset.bottom.px
                        )
                    )
                    start.place(startOffset.x - HANDLE_WIDTH, startOffset.y - HANDLE_HEIGHT)
                    end.place(endOffset.x, endOffset.y - HANDLE_HEIGHT)
                }
            }
        }))
    }
}

@Composable
internal fun SelectionHandle() {
    val paint = +memo { Paint() }
    paint.color = Color(0xAAD94633.toInt())
    Draw { canvas, parentSize ->
        canvas.drawRect(parentSize.toRect(), paint)
    }
}
