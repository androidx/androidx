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
import androidx.compose.SlotTable
import androidx.ui.core.OwnedLayer
import androidx.ui.tooling.Group
import androidx.ui.tooling.ParameterInformation
import androidx.ui.tooling.R
import androidx.ui.tooling.asTree
import androidx.ui.tooling.position
import androidx.ui.unit.Density
import java.util.ArrayDeque

/**
 * A pattern for matching Group position.
 *
 * The Group.position() extension method will return strings like:
 *    "androidx.compose.Composer-Ah7q.startExpr (Composer.kt:620)"
 *
 * From this format we would like to extract:
 * - The qualified function name: "androidx.compose.Composer.startExpr" (group 1)
 * - The inline differential part: "-Ah7q" (group 2)
 * - The file where it is found: "Composer.kt" (group 3)
 * - The line number where it was found: 620 (group 4)
 */
private const val POSITION_REGEX = "([\\w\\\\.$]*?)(-\\w*)?\\s?\\(?([\\w.]+):(\\d+)\\)?"
private const val POSITION_FUNCTION_NAME = 1
private const val POSITION_INLINE_DIFF = 2
private const val POSITION_FILENAME = 3
private const val POSITION_LINE_NUMBER = 4
private const val INVOKE_FUNCTION = ".invoke"
private val positionRegEx = Regex(POSITION_REGEX)
private val unwantedFunctions = mapOf(
    "AndroidAmbients.kt" to "androidx.ui.core.",
    "Ambient.kt" to "androidx.compose.",
    "Ambients.kt" to "androidx.ui.core.",
    "Composer.kt" to "androidx.compose.",
    "Inspectable.kt" to "androidx.ui.tooling.",
    "Layout.kt" to "androidx.ui.core.",
    "SelectionContainer.kt" to "androidx.ui.core.selection.",
    "Semantics.kt" to "androidx.ui.semantics.",
    "Wrapper.kt" to "androidx.ui.core.",
    "null" to ""
)

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
        return buildToList(tables.map { convert(it.asTree()) }, mutableListOf())
    }

    private fun convert(group: Group): MutableInspectorNode {
        val children = convertChildren(group)
        val node = parse(group)
        extractRenderIdToNode(node, children)
        buildToList(children, node.children)
        return node
    }

    private fun convertChildren(group: Group): List<MutableInspectorNode> {
        if (group.children.isEmpty()) {
            return emptyList()
        }
        var first: MutableInspectorNode? = null
        val groupIterator = group.children.iterator()
        while (groupIterator.hasNext()) {
            val node = convert(groupIterator.next())
            if (node.name.isNotEmpty() || node.children.isNotEmpty() || node.id != 0L) {
                if (first != null) {
                    return convertTailEndOfChildren(groupIterator, first, node)
                }
                if (node.name.isNotEmpty()) {
                    // Assume this represents the Composable node that group is calling.
                    addParameters(group.parameters, node)
                    parseCallLocation(group, node)
                    if (unwantedGroup(node)) {
                        markUnwanted(node)
                    }
                }
                first = node
            } else {
                release(node)
            }
        }
        return listOfNotNull(first)
    }

    private fun convertTailEndOfChildren(
        groupIterator: Iterator<Group>,
        first: MutableInspectorNode?,
        second: MutableInspectorNode
    ): List<MutableInspectorNode> {
        val result = mutableListOf<MutableInspectorNode>()
        first?.let { result.add(it) }
        result.add(second)
        while (groupIterator.hasNext()) {
            val node = convert(groupIterator.next())
            if (node.name.isNotEmpty() || node.children.isNotEmpty() || node.id != 0L) {
                result.add(node)
            } else {
                release(node)
            }
        }
        return result
    }

    private fun extractRenderIdToNode(
        node: MutableInspectorNode,
        children: List<MutableInspectorNode>
    ) {
        if (node.id == 0L) {
            node.id = children.singleOrNull { isRenderNodeId(it) }?.id ?: 0L
        }
    }

    /**
     * Adds the nodes in [input] to the [result] list.
     * Nodes without a reference to a Composable are skipped.
     */
    private fun buildToList(
        input: List<MutableInspectorNode>,
        result: MutableList<InspectorNode>
    ): List<InspectorNode> {
        input.forEach {
            if (it.name.isEmpty()) {
                result.addAll(it.children)
            } else {
                it.id = if (it.id != 0L) it.id else --generatedId
                result.add(it.build())
            }
            release(it)
        }
        return result
    }

    private fun parse(group: Group): MutableInspectorNode {
        val node = newNode()
        node.id = getRenderNode(group)
        if (!parseCallLocation(group, node)) {
            return markUnwanted(node)
        }
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
        if (!isLambda(node)) {
            node.name = node.functionName.substringAfterLast(".")
        }
        return node
    }

    private fun markUnwanted(node: MutableInspectorNode): MutableInspectorNode {
        node.resetExceptIdAndChildren()
        return node
    }

    private fun parseCallLocation(group: Group, node: MutableInspectorNode): Boolean {
        val position = group.position ?: return false
        val matcher = positionRegEx.matchEntire(position) ?: return false
        val functionName = matcher.groups[POSITION_FUNCTION_NAME]?.value ?: return false
        val inlineDiff = matcher.groups[POSITION_INLINE_DIFF]?.value ?: ""
        val fileName = matcher.groups[POSITION_FILENAME]?.value ?: return false
        val lineNumber = matcher.groups[POSITION_LINE_NUMBER]?.value?.toIntOrNull() ?: return false
        node.functionName = functionName
        node.qualifiedFunctionName = "$functionName$inlineDiff"
        node.fileName = fileName
        node.lineNumber = lineNumber
        node.hasInlineParameters = inlineDiff.isNotEmpty()
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
        val castedValue = castValue(parameter, node) ?: return
        parameterFactory.create(node, parameter.name, castedValue)?.let { node.parameters.add(it) }
    }

    private fun castValue(parameter: ParameterInformation, node: MutableInspectorNode): Any? {
        val value = parameter.value ?: return null
        if (!node.hasInlineParameters) {
            return value
        }
        val functionName = node.qualifiedFunctionName
        return inlineClassConverter.castParameterValue(functionName, parameter.name, value)
    }

    private fun isLambda(node: MutableInspectorNode) =
        node.functionName.endsWith(INVOKE_FUNCTION)

    private fun isRenderNodeId(node: MutableInspectorNode) =
        node.id != 0L && node.functionName.isEmpty()

    private fun unwantedGroup(node: MutableInspectorNode): Boolean {
        val unwantedFunctionPrefix: String = unwantedFunctions[node.fileName] ?: return false
        return node.functionName.startsWith(unwantedFunctionPrefix)
    }

    private fun newNode(): MutableInspectorNode {
        return if (cache.isNotEmpty()) cache.pop() else MutableInspectorNode()
    }

    private fun release(node: MutableInspectorNode) {
        node.reset()
        cache.add(node)
    }
}
