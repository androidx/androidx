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

    <SelectionRegistrarAmbient.Provider value=manager>
        <PressIndicatorGestureDetector onStart={ position -> manager.onPress(position) }>
            <children />
        </PressIndicatorGestureDetector>

        selection?.let {
            <SelectionHandle position=it.startOffset start=true />
            <SelectionHandle position=it.endOffset start=false />
        }
    </SelectionRegistrarAmbient.Provider>
}

@Suppress("FunctionName")
@Composable
internal fun SelectionHandle(position: Offset, start: Boolean) {
    val paint = Paint()
    paint.color = Color(0xAAD94633.toInt())
    <Draw> canvas, _ ->
        var left = position.dx
        var top = position.dy - 100f
        var right = position.dx
        var bottom = position.dy

        if (start) right += 20.0f
        else left -= 20.0f

        canvas.drawRect(
            Rect(left, top, right, bottom),
            paint
        )
    </Draw>
}
