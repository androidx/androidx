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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.content.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.ReceiveContentNode
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.traverseAncestors

internal data class ReceiveContentConfiguration(
    val acceptedMimeTypes: Set<String>,
    val onReceive: (TransferableContent) -> TransferableContent?
) {
    /**
     * `InputConnection#commitContent` callback that's delegates to [onReceive], then returns true
     * if the remaining content is different than the original content, which indicates a
     * consumption.
     */
    val onCommitContent: (TransferableContent) -> Boolean = { content ->
        val remaining = onReceive(content)
        remaining != content
    }
}

/**
 * Travels among ancestor nodes to find each [ReceiveContentNode] that would be interested
 * in the content that's sent by the IME.
 *
 * - acceptedMimeTypes of each node is merged together since each node has a right to register
 * its interest.
 * - onReceive callbacks are also chained from inner most (closest ancestor) to outer most
 * (furthest ancestor). Each node receives a [TransferableContent], then returns another or the
 * same [TransferableContent] indicating what's left unconsumed and should be delegated to
 * the rest of the chain.
 */
internal fun DelegatableNode.mergeReceiveContentConfiguration(): ReceiveContentConfiguration? {
    // do not pre-allocate
    var mutableAcceptedMimeTypes: MutableSet<String>? = null
    var mutableOnReceiveCallbacks: MutableList<(TransferableContent) -> TransferableContent?>? =
        null
    traverseAncestors(
        ReceiveContentNode.ReceiveContentTraversableKey
    ) { traversableNode ->
        val receiveContentNode = traversableNode as? ReceiveContentNode
            ?: return@traverseAncestors true

        if (mutableAcceptedMimeTypes == null) mutableAcceptedMimeTypes = mutableSetOf()
        if (mutableOnReceiveCallbacks == null) mutableOnReceiveCallbacks = mutableListOf()

        receiveContentNode.acceptedMediaTypes.forEach {
            mutableAcceptedMimeTypes?.add(it.representation)
        }

        mutableOnReceiveCallbacks?.add(receiveContentNode.onReceive)
        true
    }

    // InputConnection#onCommitContent requires a boolean return value indicating that some
    // part of the content is consumed by the app in some way. Meanwhile regular ReceiveContent
    // callback expects TransferableContent items to be returned. Here we do a conversion from
    // content based callback to boolean based callback.
    // If the remaining items returned from the callback chain is different than the one
    // we started with, it is regarded as an action has been taken and we return true.
    val acceptedMimeTypes = mutableAcceptedMimeTypes
    val onReceiveCallbacks = mutableOnReceiveCallbacks

    if (acceptedMimeTypes.isNullOrEmpty() || onReceiveCallbacks.isNullOrEmpty()) {
        return null
    }

    val mergedOnReceive: ((TransferableContent) -> TransferableContent?) = {
        // The order of callbacks go from closest node to furthest node
        var remaining: TransferableContent? = it
        var index = 0
        while (remaining != null && index < onReceiveCallbacks.size) {
            remaining = onReceiveCallbacks[index].invoke(remaining)
            index++
        }
        remaining
    }
    return ReceiveContentConfiguration(acceptedMimeTypes, mergedOnReceive)
}
