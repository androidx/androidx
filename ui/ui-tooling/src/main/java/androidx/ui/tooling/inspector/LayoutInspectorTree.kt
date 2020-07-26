/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.tooling.inspector

import android.view.View
import androidx.compose.runtime.SlotTable
import androidx.compose.ui.node.OwnedLayer
import androidx.ui.tooling.Group
import androidx.ui.tooling.ParameterInformation
import androidx.ui.tooling.R
import androidx.ui.tooling.asTree
import androidx.compose.ui.unit.Density
import java.util.ArrayDeque
import kotlin.math.absoluteValue

private val unwantedPackages = setOf(
    -1,
    packageNameHash("androidx.compose.ui"),
    packageNameHash("androidx.compose.runtime"),
    packageNameHash("androidx.ui.tooling"),
    packageNameHash("androidx.compose.ui.selection"),
    packageNameHash("androidx.compose.ui.semantics")
)

private val unwantedCalls = setOf(
    "emit",
    "remember",
    "Inspectable",
    "Layout",
    "Providers",
    "SelectionContainer",
    "SelectionLayout"
)

private fun packageNameHash(packageName: String) =
    packageName.fold(0) { hash, char -> hash * 31 + char.toInt() }.absoluteValue

/**
 * Generator of a tree for the Layout Inspector.
 */
class LayoutInspectorTree {
    // TODO: Give a warning when the kotlin metadata library is not available
    private val inlineClassConverter = InlineClassConverter()
    private val parameterFactory = ParameterFactory()
    private val cache = ArrayDeque<MutableInspectorNode>()
    private var generatedId = -1L

    /**
     * Converts the [SlotTable] set held by [view] into a list of root nodes.
     */
    fun convert(view: View): List<InspectorNode> {
        parameterFactory.density = Density(view.context)
        @Suppress("UNCHECKED_CAST")
        val tables = view.getTag(R.id.inspection_slot_table_set) as? Set<SlotTable>
            ?: return emptyList()
        clear()
        val result = convert(tables)
        clear()
        return result
    }

    private fun clear() {
        cache.clear()
        inlineClassConverter.clear()
        generatedId = -1L
    }

    private fun convert(tables: Set<SlotTable>): List<InspectorNode> {
        return buildToList(null, tables.map { convert(it.asTree()) }, mutableListOf())
    }

    private fun convert(group: Group): MutableInspectorNode {
        val children = convertChildren(group)
        val node = parse(group)
        buildToList(node, children, node.children)
        return node
    }

    private fun convertChildren(group: Group): List<MutableInspectorNode> {
        if (group.children.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<MutableInspectorNode>()
        for (child in group.children) {
            val node = convert(child)
            if (node.name.isNotEmpty() || node.children.isNotEmpty() || node.id != 0L) {
                result.add(node)
            } else {
                release(node)
            }
        }
        return result
    }

    /**
     * Adds the nodes in [input] to the [result] list.
     * Nodes without a reference to a Composable are skipped.
     * If a [parentNode] is specified then a single skipped render id will be added here.
     */
    private fun buildToList(
        parentNode: MutableInspectorNode?,
        input: List<MutableInspectorNode>,
        result: MutableList<InspectorNode>
    ): List<InspectorNode> {
        var id: Long? = null
        input.forEach {
            if (it.name.isEmpty()) {
                result.addAll(it.children)
                if (it.id != 0L) {
                    // If multiple siblings with a render ids are dropped:
                    // Ignore them all. And delegate the drawing to a parent in the inspector.
                    id = if (id == null) it.id else 0L
                }
            } else {
                it.id = if (it.id != 0L) it.id else --generatedId
                result.add(it.build())
            }
            release(it)
        }
        if (parentNode?.id == 0L) {
            id?.let { parentNode.id = it }
        }
        return result
    }

    private fun parse(group: Group): MutableInspectorNode {
        val node = newNode()
        node.id = getRenderNode(group)
        if (!parseCallLocation(group, node) && group.name.isNullOrEmpty()) {
            return markUnwanted(node)
        }
        group.name?.let { node.name = it }
        if (unwantedGroup(node)) {
            return markUnwanted(node)
        }
        val box = group.box
        node.top = box.top
        node.left = box.left
        node.height = box.bottom - box.top
        node.width = box.right - box.left
        if (node.height <= 0 || node.width <= 0) {
            return markUnwanted(node)
        }
        addParameters(group.parameters, node)
        return node
    }

    private fun markUnwanted(node: MutableInspectorNode): MutableInspectorNode {
        node.resetExceptIdAndChildren()
        return node
    }

    private fun parseCallLocation(group: Group, node: MutableInspectorNode): Boolean {
        val location = group.location ?: return false
        val fileName = location.sourceFile ?: return false
        node.fileName = fileName
        node.packageHash = location.packageHash
        node.lineNumber = location.lineNumber
        node.offset = location.offset
        node.length = location.length
        return true
    }

    private fun getRenderNode(group: Group): Long =
        group.modifierInfo.asSequence()
            .map { it.extra }
            .filterIsInstance<OwnedLayer>()
            .map { it.layerId }
            .firstOrNull() ?: 0

    private fun addParameters(parameters: List<ParameterInformation>, node: MutableInspectorNode) =
        parameters.forEach { addParameter(it, node) }

    private fun addParameter(parameter: ParameterInformation, node: MutableInspectorNode) {
        val castedValue = castValue(parameter) ?: return
        parameterFactory.create(node, parameter.name, castedValue)?.let { node.parameters.add(it) }
    }

    private fun castValue(parameter: ParameterInformation): Any? {
        val value = parameter.value ?: return null
        if (parameter.inlineClass == null) return value
        return inlineClassConverter.castParameterValue(parameter.inlineClass, value)
    }

    private fun unwantedGroup(node: MutableInspectorNode): Boolean =
        (node.packageHash in unwantedPackages && node.name in unwantedCalls)

    private fun newNode(): MutableInspectorNode {
        return if (cache.isNotEmpty()) cache.pop() else MutableInspectorNode()
    }

    private fun release(node: MutableInspectorNode) {
        node.reset()
        cache.add(node)
    }
}
