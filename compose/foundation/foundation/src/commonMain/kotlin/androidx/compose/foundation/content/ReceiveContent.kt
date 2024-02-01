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
import androidx.compose.foundation.content.internal.DynamicReceiveContentConfiguration
import androidx.compose.foundation.content.internal.ModifierLocalReceiveContent
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.content.internal.ReceiveContentDragAndDropNode
import androidx.compose.foundation.content.internal.dragAndDropRequestPermission
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Configures the current node and any children nodes as a Content Receiver.
 *
 * Content in this context refers to a [TransferableContent] that could be received from another
 * app through Drag-and-Drop, Copy/Paste, or from the Software Keyboard.
 *
 * @param hintMediaTypes A set of media types that are expected by this receiver. This set
 * gets passed to the Software Keyboard to send information about what type of content the editor
 * supports. It's possible that this modifier receives other type of content that's not specified in
 * this set. Please make sure to check again whether the received [TransferableContent] carries a
 * supported [MediaType]. An empty [MediaType] set implies [MediaType.All].
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
    hintMediaTypes: Set<MediaType>,
    onReceive: (TransferableContent) -> TransferableContent?
): Modifier = then(
    ReceiveContentElement(
        hintMediaTypes = hintMediaTypes,
        receiveContentListener = ReceiveContentListener(onReceive)
    )
)

/**
 * Configures the current node and any children nodes as a Content Receiver.
 *
 * Content in this context refers to a [TransferableContent] that could be received from another
 * app through Drag-and-Drop, Copy/Paste, or from the Software Keyboard.
 *
 * @param hintMediaTypes A set of media types that are expected by this receiver. This set
 * gets passed to the Software Keyboard to send information about what type of content the editor
 * supports. It's possible that this modifier receives other type of content that's not specified in
 * this set. Please make sure to check again whether the received [TransferableContent] carries a
 * supported [MediaType]. An empty [MediaType] set implies [MediaType.All].
 * @param receiveContentListener A set of callbacks that includes certain Drag-and-Drop state
 * changes. Please checkout [ReceiveContentListener] docs for an explanation of each callback.
 *
 * @sample androidx.compose.foundation.samples.ReceiveContentFullSample
 */
@Suppress("ExecutorRegistration")
@ExperimentalFoundationApi
fun Modifier.receiveContent(
    hintMediaTypes: Set<MediaType>,
    receiveContentListener: ReceiveContentListener
): Modifier = then(
    ReceiveContentElement(
        hintMediaTypes = hintMediaTypes.toSet(),
        receiveContentListener = receiveContentListener
    )
)

@OptIn(ExperimentalFoundationApi::class)
internal data class ReceiveContentElement(
    val hintMediaTypes: Set<MediaType>,
    val receiveContentListener: ReceiveContentListener
) : ModifierNodeElement<ReceiveContentNode>() {
    override fun create(): ReceiveContentNode {
        return ReceiveContentNode(hintMediaTypes, receiveContentListener)
    }

    override fun update(node: ReceiveContentNode) {
        node.updateNode(hintMediaTypes, receiveContentListener)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "receiveContent"
        properties["hintMediaTypes"] = hintMediaTypes
    }
}

// This node uses ModifierLocals instead of TraversableNode to find ancestor due to b/311181532.
// Since the usage of modifier locals are minimal and exactly correspond to how we would use
// TraversableNode if it was available, the switch should be fairly easy when the bug is fixed.
@OptIn(ExperimentalFoundationApi::class)
internal class ReceiveContentNode(
    var hintMediaTypes: Set<MediaType>,
    var receiveContentListener: ReceiveContentListener
) : DelegatingNode(), ModifierLocalModifierNode,
    CompositionLocalConsumerModifierNode {

    private val receiveContentConfiguration: ReceiveContentConfiguration =
        DynamicReceiveContentConfiguration(this)

    // The default provided configuration is the one supplied to this node. Once the node is
    // attached, it should provide a delegating version to ancestor nodes.
    override val providedValues: ModifierLocalMap =
        modifierLocalMapOf<ReceiveContentConfiguration?>(
            ModifierLocalReceiveContent to receiveContentConfiguration
        )

    init {
        delegate(
            ReceiveContentDragAndDropNode(
                receiveContentConfiguration = receiveContentConfiguration,
                dragAndDropRequestPermission = { dragAndDropRequestPermission(it) }
            )
        )
    }

    fun updateNode(
        hintMediaTypes: Set<MediaType>,
        receiveContentListener: ReceiveContentListener
    ) {
        this.hintMediaTypes = hintMediaTypes
        this.receiveContentListener = receiveContentListener
    }
}
