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
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.platform.InspectorInfo

/**
 * Configures the current node and any children nodes as a Content Receiver.
 *
 * Content in this context refers to a [TransferableContent] that could be received from another
 * app through Drag-and-Drop, Copy/Paste, or from the Software Keyboard.
 *
 * @param acceptedMediaTypes A list of media types that are expected by this receiver. This list
 * gets passed to the Software Keyboard to send information about what type of content the editor
 * supports. It's possible that this modifier receives other type of content that's not listed in
 * this list. Please make sure to check again whether the received [TransferableContent] carries a
 * supported [MediaType].
 * @param onReceive Callback that's triggered when a content is successfully committed. Return
 * an optional [TransferableContent] that contains the unprocessed or unaccepted parts of the
 * received [TransferableContent]. The remaining [TransferableContent] first will be sent to to the
 * closest ancestor [receiveContent] modifier. This chain will continue until there's no ancestor
 * modifier left, or [TransferableContent] is fully consumed. After, the source subsystem that
 * created the original [TransferableContent] and initiated the chain will receive any remaining
 * items to execute its default behavior. For example a text editor that receives content should
 * insert any remaining text to the drop position.
 *
 * @sample androidx.compose.foundation.samples.ReceiveContentBasicSample
 */
@ExperimentalFoundationApi
fun Modifier.receiveContent(
    vararg acceptedMediaTypes: MediaType,
    onReceive: (TransferableContent) -> TransferableContent?
): Modifier = then(ReceiveContentElement(acceptedMediaTypes.toSet(), onReceive))

@OptIn(ExperimentalFoundationApi::class)
internal data class ReceiveContentElement(
    val acceptedMediaTypes: Set<MediaType>,
    val onReceive: (TransferableContent) -> TransferableContent?
) : ModifierNodeElement<ReceiveContentNode>() {
    override fun create(): ReceiveContentNode {
        return ReceiveContentNode(acceptedMediaTypes, onReceive)
    }

    override fun update(node: ReceiveContentNode) {
        node.updateNode(acceptedMediaTypes, onReceive)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "receiveContent"
        properties["acceptedMediaType"] = acceptedMediaTypes
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class ReceiveContentNode(
    var acceptedMediaTypes: Set<MediaType>,
    var onReceive: (TransferableContent) -> TransferableContent?
) : DelegatingNode(), DelegatableNode, TraversableNode {

    /**
     * The key to find this type of node while traversing a node chain.
     */
    internal object ReceiveContentTraversableKey

    override val traverseKey: Any = ReceiveContentTraversableKey

    fun updateNode(
        acceptedMediaTypes: Set<MediaType>,
        onReceive: (TransferableContent) -> TransferableContent?
    ) {
        this.acceptedMediaTypes = acceptedMediaTypes
        this.onReceive = onReceive
    }
}
