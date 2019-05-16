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

package androidx.ui.core

import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus

private const val HANDLE_WIDTH = 20f
private const val HANDLE_HEIGHT = 100f

/**
 * Data class of Selection.
 */
data class Selection(
    /**
     * The coordinate of the start offset of the selection. For text, it's the left bottom corner
     * of the character at the start offset.
     */
    val startOffset: Offset,
    /**
     * The coordinate of the end offset of the selection. For text, it's the left bottom corner
     * of the character at the end offset.
     */
    val endOffset: Offset
)

/**
 * An interface handling selection. Get selection from a widget by passing in a coordinate.
 */
interface TextSelectionHandler {
    fun getSelection(coordinates: Pair<PxPosition, PxPosition>): Selection?
}

/**
 *  An interface allowing a Text composable to "register" and "unregister" itself with the class
 *  implementing the interface.
 */
interface SelectionRegistrar {
    // TODO(qqd): Replace Any with a type in future.
    fun subscribe(handler: TextSelectionHandler): Any

    fun unsubscribe(key: Any)
}

internal class SelectionManager : SelectionRegistrar {
    /**
     * This is essentially the list of registered components that want
     * to handle text selection that are below the SelectionContainer.
     */
    val handlers = mutableSetOf<TextSelectionHandler>()

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

    var selection: Selection? = null

    var onSelectionChange: (Selection?) -> Unit = {}

    fun onPress(position: PxPosition) {
        var result: Selection? = null
        for (handler in handlers) {
            result = handler.getSelection(Pair(position, position))
        }
        onSelectionChange(result)
    }
}

/** Ambient of SelectionRegistrar for SelectionManager. */
val SelectionRegistrarAmbient = Ambient.of<SelectionRegistrar> { SelectionManager() }

/**
 * Selection Widget.
 *
 * The selection widget wraps composables and let them to be selectable. It paints the selection
 * area with start and end handles.
 */
@Suppress("FunctionName")
@Composable
fun SelectionContainer(
    /** Current Selection status.*/
    selection: Selection?,
    /** A function containing customized behaviour when selection changes. */
    onSelectionChange: (Selection?) -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val manager = +memo { SelectionManager() }
    +memo(selection) { manager.selection = selection }
    +memo(onSelectionChange) { manager.onSelectionChange = onSelectionChange }

    SelectionRegistrarAmbient.Provider(value = manager) {
        val content = @Composable {
            PressIndicatorGestureDetector(onStart = { position -> manager.onPress(position) }) {
                children()
            }
        }
        val startHandle = @Composable {
            Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            })
        }
        val endHandle = @Composable {
            Layout(children = { SelectionHandle() }, layoutBlock = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            })
        }
        @Suppress("USELESS_CAST")
        Layout(
            childrenArray = arrayOf(content, startHandle, endHandle),
            layoutBlock = { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                val width = placeable.width
                val height = placeable.height
                val start =
                    measurables[startHandle as () -> Unit].first().measure(
                        Constraints.tightConstraints(
                            HANDLE_WIDTH.toInt().ipx,
                            HANDLE_HEIGHT.toInt().ipx
                        )
                    )
                val end =
                    measurables[endHandle as () -> Unit].first().measure(
                        Constraints.tightConstraints(
                            HANDLE_WIDTH.toInt().ipx,
                            HANDLE_HEIGHT.toInt().ipx
                        )
                    )
                layout(width, height) {
                    placeable.place(IntPx.Zero, IntPx.Zero)
                    selection?.let {
                        start.place(
                            it.startOffset.dx.px,
                            it.startOffset.dy.px - HANDLE_HEIGHT.px
                        )
                        end.place(
                            it.endOffset.dx.px - HANDLE_WIDTH.px,
                            it.endOffset.dy.px - HANDLE_HEIGHT.px
                        )
                    }
                }
            })
    }
}

@Suppress("FunctionName")
@Composable
internal fun SelectionHandle() {
    val paint = Paint()
    paint.color = Color(0xAAD94633.toInt())
    Draw { canvas, _ ->
        canvas.drawRect(
            Rect(left = 0f, top = 0f, right = HANDLE_WIDTH, bottom = HANDLE_HEIGHT),
            paint
        )
    }
}
