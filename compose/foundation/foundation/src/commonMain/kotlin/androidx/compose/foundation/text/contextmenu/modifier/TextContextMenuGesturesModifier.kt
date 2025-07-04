/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.contextmenu.modifier

import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.gestures.onRightClickDown
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.launch

/**
 * Shows the dropdown context menu (via [LocalTextContextMenuDropdownProvider]) when a right click
 * is received.
 *
 * @param onPreShowContextMenu A lambda that will be invoked right before
 *   [TextContextMenuProvider.showTextContextMenu].
 */
internal fun Modifier.textContextMenuGestures(
    onPreShowContextMenu: (suspend () -> Unit)? = null
): Modifier = this then TextContextMenuGestureElement(onPreShowContextMenu)

private class TextContextMenuGestureElement(
    private val onPreShowContextMenu: (suspend () -> Unit)?
) : ModifierNodeElement<TextContextMenuGestureNode>() {
    override fun create(): TextContextMenuGestureNode =
        TextContextMenuGestureNode(onPreShowContextMenu)

    override fun update(node: TextContextMenuGestureNode) {
        node.update(onPreShowContextMenu)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "TextContextMenuGestures"
        properties["onPreShowContextMenu"] = onPreShowContextMenu
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextContextMenuGestureElement) return false

        if (onPreShowContextMenu !== other.onPreShowContextMenu) return false

        return true
    }

    override fun hashCode(): Int = onPreShowContextMenu.hashCode()
}

private class TextContextMenuGestureNode(private var onPreShowContextMenu: (suspend () -> Unit)?) :
    DelegatingNode(), CompositionLocalConsumerModifierNode, GlobalPositionAwareModifierNode {

    private companion object {
        private const val MESSAGE = "Tried to open context menu before the anchor was placed."
    }

    private var localCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    init {
        delegate(SuspendingPointerInputModifierNode { onRightClickDown(::tryShowContextMenu) })
    }

    fun update(onPreShowContextMenu: (suspend () -> Unit)?) {
        this.onPreShowContextMenu = onPreShowContextMenu
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.localCoordinates = coordinates
    }

    private fun tryShowContextMenu(localClickOffset: Offset) {
        val provider = currentValueOf(LocalTextContextMenuDropdownProvider) ?: return
        val dataProvider = ClickTextContextMenuDataProvider(localClickOffset)
        coroutineScope.launch {
            onPreShowContextMenu?.invoke()
            provider.showTextContextMenu(dataProvider)
        }
    }

    private inner class ClickTextContextMenuDataProvider(private val localClickOffset: Offset) :
        TextContextMenuDataProvider {
        override fun position(destinationCoordinates: LayoutCoordinates): Offset {
            val localCoordinates = checkPreconditionNotNull(localCoordinates) { MESSAGE }
            return destinationCoordinates.localPositionOf(localCoordinates, localClickOffset)
        }

        override fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect =
            Rect(position(destinationCoordinates), Size.Zero)

        override fun data(): TextContextMenuData = collectTextContextMenuData()
    }
}
