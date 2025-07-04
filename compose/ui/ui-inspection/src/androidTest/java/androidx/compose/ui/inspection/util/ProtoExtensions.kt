/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.inspection.util

import androidx.collection.MutableLongLongMap
import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.MutableInspectorNode
import androidx.compose.ui.unit.IntRect
import java.util.NoSuchElementException
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParameterDetailsCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersCommand
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ParameterReference
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.UpdateSettingsCommand

internal fun List<LayoutInspectorComposeProtocol.StringEntry>.toMap() = associate {
    it.id to it.str
}

internal fun GetParametersCommand(
    rootViewId: Long,
    node: ComposableNode,
    useDelayedParameterExtraction: Boolean,
    generation: Int = 1,
    skipSystemComposables: Boolean = true,
): Command =
    if (useDelayedParameterExtraction) {
        GetParametersByAnchorIdCommand(
            rootViewId,
            node.anchorHash,
            node.id,
            generation,
            skipSystemComposables,
        )
    } else {
        GetParametersByIdCommand(rootViewId, node.id, skipSystemComposables)
    }

internal fun GetParametersByIdCommand(
    rootViewId: Long,
    composableId: Long,
    skipSystemComposables: Boolean = true,
): Command =
    Command.newBuilder()
        .apply {
            getParametersCommand =
                GetParametersCommand.newBuilder()
                    .apply {
                        this.rootViewId = rootViewId
                        this.composableId = composableId
                        this.skipSystemComposables = skipSystemComposables
                    }
                    .build()
        }
        .build()

internal fun GetParametersByAnchorIdCommand(
    rootViewId: Long,
    anchorId: Int,
    composableId: Long,
    generation: Int = 1,
    skipSystemComposables: Boolean = true,
): Command =
    Command.newBuilder()
        .apply {
            getParametersCommand =
                GetParametersCommand.newBuilder()
                    .apply {
                        this.rootViewId = rootViewId
                        this.anchorHash = anchorId
                        this.composableId = composableId
                        this.generation = generation
                        this.skipSystemComposables = skipSystemComposables
                    }
                    .build()
        }
        .build()

internal fun GetAllParametersCommand(
    rootViewId: Long,
    skipSystemComposables: Boolean = true,
    generation: Int = 1,
): Command =
    Command.newBuilder()
        .apply {
            getAllParametersCommand =
                GetAllParametersCommand.newBuilder()
                    .apply {
                        this.rootViewId = rootViewId
                        this.skipSystemComposables = skipSystemComposables
                        this.generation = generation
                    }
                    .build()
        }
        .build()

internal fun GetParametersResponse.find(name: String): Parameter {
    val strings = stringsList.toMap()
    val params = parameterGroup.parameterList.associateBy { strings[it.name] }
    return params[name]
        ?: error("$name not found in parameters. Found: ${params.keys.joinToString()}")
}

internal fun GetParametersResponse.findMerged(name: String): Parameter {
    val strings = stringsList.toMap()
    val semantics = parameterGroup.mergedSemanticsList.associateBy { strings[it.name] }
    return semantics[name]
        ?: error("$name not found in merged semantics. Found: ${semantics.keys.joinToString()}")
}

internal fun GetParametersResponse.findUnmerged(name: String): Parameter {
    val strings = stringsList.toMap()
    val semantics = parameterGroup.unmergedSemanticsList.associateBy { strings[it.name] }
    return semantics[name]
        ?: error("$name not found in unmerged semantics. Found: ${semantics.keys.joinToString()}")
}

internal fun Int.resolve(response: GetParametersResponse): String? {
    return response.stringsList.toMap()[this]
}

internal fun GetParameterDetailsCommand(
    rootViewId: Long,
    reference: ParameterReference,
    startIndex: Int,
    maxElements: Int,
    skipSystemComposables: Boolean = true,
): Command =
    Command.newBuilder()
        .apply {
            getParameterDetailsCommand =
                GetParameterDetailsCommand.newBuilder()
                    .apply {
                        this.rootViewId = rootViewId
                        this.skipSystemComposables = skipSystemComposables
                        this.reference = reference
                        if (startIndex >= 0) {
                            this.startIndex = startIndex
                        }
                        if (maxElements >= 0) {
                            this.maxElements = maxElements
                        }
                    }
                    .build()
        }
        .build()

internal fun GetComposablesCommand(
    rootViewId: Long,
    skipSystemComposables: Boolean = true,
    generation: Int = 1,
    extractAllParameters: Boolean = false,
): Command =
    Command.newBuilder()
        .apply {
            getComposablesCommand =
                GetComposablesCommand.newBuilder()
                    .apply {
                        this.rootViewId = rootViewId
                        this.skipSystemComposables = skipSystemComposables
                        this.generation = generation
                        this.extractAllParameters = extractAllParameters
                    }
                    .setRootViewId(rootViewId)
                    .setSkipSystemComposables(skipSystemComposables)
                    .setGeneration(generation)
                    .build()
        }
        .build()

internal fun GetComposablesResponse.nodes(): List<ComposableNode> {
    return rootsList.flatMap { it.nodesList }.flatMap { it.flatten() }
}

internal fun GetComposablesResponse.filter(name: String): List<ComposableNode> {
    val strings = stringsList.toMap()
    return nodes().filter { strings[it.name] == name }
}

internal fun GetComposablesResponse.roots(): List<InspectorNode> {
    val strings = stringsList.toMap()
    return rootsList.flatMap { it.nodesList.convert(strings) }
}

internal fun ComposableNode.isAncestorOf(
    ancestor: ComposableNode,
    tree: GetComposablesResponse,
): Boolean {
    val pending = mutableListOf<ComposableNode>()
    val map = MutableLongLongMap()
    pending.addAll(tree.rootsList.flatMap { it.nodesList })
    while (pending.isNotEmpty()) {
        val item = pending.removeAt(pending.size - 1)
        item.childrenList.forEach { map[it.id] = item.id }
        pending.addAll(item.childrenList)
    }
    try {
        var id = this.id
        do {
            id = map[id]
        } while (id != ancestor.id)
        return true
    } catch (_: NoSuchElementException) {
        return false
    }
}

private fun List<ComposableNode>.convert(strings: Map<Int, String>): List<InspectorNode> = map {
    val node = MutableInspectorNode()
    node.name = strings[it.name] ?: ""
    node.box =
        IntRect(
            it.bounds.layout.x,
            it.bounds.layout.y,
            it.bounds.layout.x + it.bounds.layout.w,
            it.bounds.layout.y + it.bounds.layout.h,
        )
    node.children.addAll(it.childrenList.convert(strings))
    node.inlined = (it.flags and ComposableNode.Flags.INLINED_VALUE) != 0
    node.build()
}

internal fun GetUpdateSettingsCommand(
    includeRecomposeCounts: Boolean = false,
    keepRecomposeCounts: Boolean = false,
    delayParameterExtractions: Boolean = false,
    reduceChildNesting: Boolean = false,
): Command =
    Command.newBuilder()
        .apply {
            updateSettingsCommand =
                UpdateSettingsCommand.newBuilder()
                    .apply {
                        this.includeRecomposeCounts = includeRecomposeCounts
                        this.keepRecomposeCounts = keepRecomposeCounts
                        this.delayParameterExtractions = delayParameterExtractions
                        this.reduceChildNesting = reduceChildNesting
                    }
                    .build()
        }
        .build()

internal fun ComposableNode.flatten(): List<ComposableNode> =
    listOf(this).plus(this.childrenList.flatMap { it.flatten() })
