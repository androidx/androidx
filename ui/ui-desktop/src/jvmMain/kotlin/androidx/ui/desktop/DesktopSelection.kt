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
package androidx.ui.desktop

import androidx.ui.core.Modifier
import androidx.ui.core.Layout
import androidx.ui.core.gesture.DragObserver
import androidx.compose.Composable
import androidx.ui.core.onPositioned
import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.selection.Selection
import androidx.ui.core.selection.SelectionRegistrarAmbient
import androidx.compose.Providers
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.composed
import androidx.ui.core.gesture.rawDragGestureFilter
import androidx.ui.core.gesture.rawPressStartGestureFilter

@Composable
private fun Wrap(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val width = placeables.fold(0) { maxWidth, placeable ->
            max(maxWidth, (placeable.width))
        }

        val height = placeables.fold(0) { minWidth, placeable ->
            max(minWidth, (placeable.height))
        }

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.placeAbsolute(0, 0)
            }
        }
    }
}

@Composable
internal fun DesktopSelectionContainer(children: @Composable () -> Unit) {
    val selection = state<Selection?> { null }
    DesktopSelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        children = children
    )
}

private class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

private fun Modifier.selectionFilter(observer: DragObserver): Modifier = composed {
    val glue = remember { DragGlue(observer) }
    rawDragGestureFilter(glue, glue::started)
        .rawPressStartGestureFilter(glue::startDrag, true)
}

private class DragGlue(val observer: DragObserver) : DragObserver by observer {
    var started = false

    fun startDrag(downPosition: Offset) {
        started = true
        observer.onStart(downPosition)
    }

    override fun onStop(velocity: Offset) {
        started = false
        observer.onStop(velocity)
    }

    override fun onCancel() {
        started = false
        observer.onCancel()
    }
}

@Composable
fun DesktopSelectionContainer(
    selection: Selection?,
    onSelectionChange: (Selection?) -> Unit,
    children: @Composable () -> Unit
) {
    val registrarImpl = remember { DesktopSelectionRegistrar() }
    val manager = remember { DesktopSelectionManager(registrarImpl) }

    manager.onSelectionChange = onSelectionChange
    manager.selection = selection

    val gestureModifiers =
        Modifier.selectionFilter(manager.observer)

    val modifier = remember {
        gestureModifiers.onPositioned {
            manager.containerLayoutCoordinates = it
        }
    }

    Providers(SelectionRegistrarAmbient provides registrarImpl) {
        Wrap(modifier) {
            children()
        }
    }
}