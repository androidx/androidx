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
 * Content in this context refers to a [TransferableContent] that could be received from another app
 * through Drag-and-Drop, Copy/Paste, or from the Software Keyboard.
 *
 * There is no pre-filtering for the received content by media type, e.g. software Keyboard would
 * assume that the app can handle any content that's sent to it. Therefore, it's crucial to check
 * the received content's type and other related information before reading and processing it.
 * Please refer to [TransferableContent.hasMediaType] and [TransferableContent.clipMetadata] to
 * learn more about how to do proper checks on the received item.
 *
 * @param receiveContentListener Listener to respond to the receive event. This interface also
 *   includes a set of callbacks for certain Drag-and-Drop state changes. Please checkout
 *   [ReceiveContentListener] docs for an explanation of each callback.
 * @sample androidx.compose.foundation.samples.ReceiveContentFullSample
 * @see TransferableContent
 * @see hasMediaType
 */
@Suppress("ExecutorRegistration")
@ExperimentalFoundationApi
fun Modifier.contentReceiver(receiveContentListener: ReceiveContentListener): Modifier =
    then(ReceiveContentElement(receiveContentListener = receiveContentListener))

@OptIn(ExperimentalFoundationApi::class)
internal data class ReceiveContentElement(val receiveContentListener: ReceiveContentListener) :
    ModifierNodeElement<ReceiveContentNode>() {
    override fun create(): ReceiveContentNode {
        return ReceiveContentNode(receiveContentListener)
    }

    override fun update(node: ReceiveContentNode) {
        node.updateNode(receiveContentListener)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "receiveContent"
    }
}

// This node uses ModifierLocals instead of TraversableNode to find ancestor due to b/311181532.
// Since the usage of modifier locals are minimal and exactly correspond to how we would use
// TraversableNode if it was available, the switch should be fairly easy when the bug is fixed.
@OptIn(ExperimentalFoundationApi::class)
internal class ReceiveContentNode(var receiveContentListener: ReceiveContentListener) :
    DelegatingNode(), ModifierLocalModifierNode, CompositionLocalConsumerModifierNode {

    private val receiveContentConfiguration: ReceiveContentConfiguration =
        DynamicReceiveContentConfiguration(this)

    // The default provided configuration is the one supplied to this node. Once the node is
    // attached, it should provide a delegating version to ancestor nodes.
    override val providedValues: ModifierLocalMap =
        modifierLocalMapOf(ModifierLocalReceiveContent to receiveContentConfiguration)

    init {
        delegate(
            ReceiveContentDragAndDropNode(
                receiveContentConfiguration = receiveContentConfiguration,
                dragAndDropRequestPermission = { dragAndDropRequestPermission(it) }
            )
        )
    }

    fun updateNode(receiveContentListener: ReceiveContentListener) {
        this.receiveContentListener = receiveContentListener
    }
}
