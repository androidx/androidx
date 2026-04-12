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

package androidx.compose.foundation.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget

/**
 * A set of callbacks for [contentReceiver] modifier to get information about certain Drag-and-Drop
 * state changes, as well as receiving the payload carrying [TransferableContent].
 *
 * [contentReceiver]'s drop target behaves a little different compared to the regular
 * [dragAndDropTarget] modifier. When two [contentReceiver] modifiers are nested on the composition
 * tree, drop area of the parent node encapsulates drop area of the child node, meaning that they
 * wouldn't be mutually exclusive like the regular [dragAndDropTarget] nesting. This becomes useful
 * when you want to extend the drop area around a TextField by wrapping the TextField in a larger
 * component with a [contentReceiver] modifier. We can guarantee that the container won't receive
 * dragExit event when the dragging item moves over to TextField.
 *
 * Let's assume we have two [contentReceiver] boxes named A and B where B is a child of A, aligned
 * to bottom end.
 * ---------
 * | A | | |---| | | B |
 * ---------
 * When a dragging item moves over to A from left, then over to B, then starts moving up and goes
 * back to A leaving B, then finally leaves them both, the following would be the list of expected
 * [ReceiveContentListener] calls in order to both nodes.
 * - A#onStart
 * - B#onStart
 * - A#onEnter
 * - B#onEnter
 * - B#onExit
 * - A#onExit
 * - B#onEnd
 * - A#onEnd
 *
 * The interesting part in this order of calls is that A does not receive an exit event when the
 * item moves over to B. This is different than what would happen if you were to use
 * [dragAndDropTarget] modifier because semantically [contentReceiver] works as a chain of nodes. If
 * the item were to be dropped on B, its [onReceive] chain would also call A's [onReceive] with
 * what's left from B.
 */
@ExperimentalFoundationApi
fun interface ReceiveContentListener {

    /**
     * Optional callback that's called when a dragging session starts. All [contentReceiver] nodes
     * in the current composition tree receives this callback immediately.
     */
    fun onDragStart() = Unit

    /**
     * Optional callback that's called when a dragging session ends by either successful drop, or
     * cancellation. All [contentReceiver] nodes in the current composition tree receives this
     * callback immediately.
     */
    fun onDragEnd() = Unit

    /** Optional callback that's called when a dragging item moves into this node's coordinates. */
    fun onDragEnter() = Unit

    /**
     * Optional callback that's called when a dragging item moves out of this node's coordinates.
     */
    fun onDragExit() = Unit

    /**
     * Callback that's triggered when a content is successfully committed.
     *
     * @return An optional [TransferableContent] that contains the ignored parts of the received
     *   [TransferableContent] by this node. The remaining [TransferableContent] first will be sent
     *   to to the closest ancestor [contentReceiver] modifier. This chain will continue until
     *   there's no ancestor modifier left, or [TransferableContent] is fully consumed. After, the
     *   source subsystem that created the original [TransferableContent] and initiated the chain
     *   will receive any remaining items to apply its default behavior. For example a text editor
     *   that receives content by DragAndDrop should insert the remaining text from the receive
     *   chain to the drop position.
     */
    fun onReceive(transferableContent: TransferableContent): TransferableContent?
}
