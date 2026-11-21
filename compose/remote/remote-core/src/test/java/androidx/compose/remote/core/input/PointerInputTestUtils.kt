/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.input

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_DISPATCH
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent

/**
 * Creates a pointer event the way a player does before it gives the event to the
 * [PointerInputRouter]: not sent to any pass yet.
 *
 * @param subtype one of the `POINTER_SUBTYPE_` values of [PointerInputRouter]
 * @param x x position of the pointer, in the coordinates of the document
 * @param y y position of the pointer, in the coordinates of the document
 * @param pointerId id of the pointer, from 0 to 31
 */
internal fun pointerEvent(subtype: Int, x: Float, y: Float, pointerId: Int = 0): PointerInputEvent =
    PointerInputEvent.obtain().apply {
        setPass(POINTER_PASS_DISPATCH)
        setSubtype(subtype)
        data[0] = pointerId.toFloat()
        data[1] = x
        data[2] = y
    }

/**
 * Adds [child] to this component and gets [child] ready for pointer input, the way a player does
 * when it reads a document and lays it out.
 *
 * Add the modifiers and the children of [child] before you call this.
 *
 * **Why `inflate()` is called.** A component is created with one flat list that holds its
 * modifiers, its children and its data. [Component.inflate] sorts that list: the modifiers go into
 * [LayoutComponent.getComponentModifiers] and the children into
 * [LayoutComponent.getChildrenComponents]. The [PointerInputRouter] uses the first to find the
 * handlers and the second for hit testing, so a component that is not inflated gets no pointer
 * events. A player calls `inflate()` on every component when it reads a document, children first.
 * These tests build the components by hand, so it is called here.
 *
 * **Why the position and the size are kept.** `inflate()` also moves the component to (0, 0) and
 * sets its size to what the size modifiers say. The components in these tests have no size
 * modifier, so their size becomes 0. A player would then measure and lay out the component. These
 * tests do neither, so this function gives [child] back the position and the size it was created
 * with, and lays out its modifiers with that size.
 */
internal fun Component.addChild(context: RemoteContext, child: Component) {
    val x = child.x
    val y = child.y
    val width = child.width
    val height = child.height
    child.inflate()
    child.x = x
    child.y = y
    child.width = width
    child.height = height
    if (child is LayoutComponent) {
        child.componentModifiers.layout(context, child, width, height)
    }
    mList.add(child)
}
