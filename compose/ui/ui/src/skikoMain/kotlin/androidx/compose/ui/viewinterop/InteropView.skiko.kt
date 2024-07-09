/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.IntSize

/**
 * [Modifier.Node] to associate an [InteropView] with the modified element to allow hit testing it and
 * perform custom pointer input handling if needed.
 */
@InternalComposeUiApi
open class InteropViewAnchorModifierNode(
    var interopView: InteropView
) : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {}

    override fun onCancelPointerInput() {}
}

/**
 * Element for [InteropViewAnchorModifierNode]. A custom implementation of [ModifierNodeElement] is needed
 * for possible [InteropViewAnchorModifierNode] subclasses.
 */
internal data class InteropViewAnchorModifierNodeElement(
    val interopView: InteropView
) : ModifierNodeElement<InteropViewAnchorModifierNode>() {
    override fun create(): InteropViewAnchorModifierNode =
        InteropViewAnchorModifierNode(interopView)

    override fun update(node: InteropViewAnchorModifierNode) {
        node.interopView = interopView
    }
}

/**
 * Add an association with [InteropView] to the modified element.
 * Allows hit testing and custom pointer input handling for the [InteropView].
 */
internal fun Modifier.interopViewAnchor(view: InteropView): Modifier =
    this then InteropViewAnchorModifierNodeElement(view)
