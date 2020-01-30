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

import androidx.compose.Composable

/**
 * [PointerInput] is the compose ui core component for receiving and interacting with pointer input.
 *
 * Pointer input includes all user input related to 2d positioning on the screen.  This includes
 * (but is not necessarily limited to) fingers touching the screen, a mouse moving across the
 * screen, and stylus input.
 *
 * [PointerInput] takes the virtual size of all of it's descendant [Layout] nodes and when a pointer
 * comes in contact with the screen, hit testing is automatically done using that virtual size.
 *
 * [pointerInputHandler] is invoked when pointers that have hit tested positive change.
 *
 * [cancelHandler] is invoked to notify the handler that no more calls to pointerInputHandler will
 * be made, until at least new pointers exist.  This can occur for a few reasons:
 * 1. Android dispatches ACTION_CANCEL to [AndroidComposeView.onTouchEvent].
 * 2. The PointerInputNode has been removed from the compose hierarchy.
 * 3. The PointerInputNode no longer has any descendant [LayoutNode]s and therefore does not
 * know what region of the screen it should virtually exist in.
 *
 * @param pointerInputHandler Invoked when pointers that have hit this [PointerInput] change.
 * @param cancelHandler Invoked when a cancellation event occurs.
 * @param children The children composable that will be composed as a child, or children, of this
 * [PointerInput].
 */
@Composable
inline fun PointerInput(
    noinline pointerInputHandler: PointerInputHandler,
    noinline cancelHandler: () -> Unit,
    crossinline children: @Composable() () -> Unit
) {
    // Hide the internals of PointerInputNode
    PointerInputNode(pointerInputHandler = pointerInputHandler, cancelHandler = cancelHandler) {
        children()
    }
}
