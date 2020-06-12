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
import androidx.ui.tooling.R
import androidx.ui.tooling.asTree
import androidx.ui.tooling.position
import java.util.ArrayDeque
import java.util.regex.Pattern

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
//  private static final int POSITION_GROUP_INLINE_DIFF = 2; // Not used...
private const val POSITION_FILENAME = 3
private const val POSITION_LINE_NUMBER = 4
private const val INVOKE_FUNCTION = ".invoke"
private val positionPattern = Pattern.compile(POSITION_REGEX)
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
    private val cache = ArrayDeque<MutableInspectorNode>()

    /**
     * Converts the [SlotTable] set held by [view] into a tree of nodes.
     */
    fun convert(view: View): List<InspectorNode> {
        @Suppress("UNCHECKED_CAST")
        val tables = view.getTag(R.id.inspection_slot_table_set) as? Set<SlotTable>
            ?: return emptyList()
        val result = convert(tables)
        cache.clear()
        return result
    }

    private fun convert(tables: Set<SlotTable>): List<InspectorNode> {
        val result = mutableListOf<InspectorNode>()
        for (table in tables) {
            buildToList(convert(table.asTree()), result)
        }
        return result
    }

    private fun convert(group: Group): List<MutableInspectorNode> {
        val children = convertChildren(group.children)
        val single = children.singleOrNull()
        val node = parse(group) ?: return children
        return if (!isLambda(node)) {
            extractRenderIdToNode(node, children)
            buildToList(children, node.children)
            listOf(node)
        } else if (single != null && isLambda(node) && !isLambda(single)) {
            single.functionName = node.functionName
            single.fileName = node.fileName
            single.lineNumber = node.lineNumber
            release(node)
            children
        } else {
            release(node)
            children
        }
    }

    private fun convertChildren(groups: Collection<Group>): List<MutableInspectorNode> =
        when (groups.size) {
            0 -> emptyList()
            1 -> convert(groups.first())
            else -> groups.flatMap { convert(it) }
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
     * Render nodes (without a reference to a Composable) are skipped.
     */
    private fun buildToList(
        input: List<MutableInspectorNode>,
        result: MutableList<InspectorNode>
    ) {
        input.forEach {
            if (isRenderNodeId(it)) {
                result.addAll(it.children)
            } else {
                result.add(it.build())
            }
            release(it)
        }
    }

    private fun parse(group: Group): MutableInspectorNode? {
        val position = group.position ?: return null
        val matcher = positionPattern.matcher(position)
        if (!matcher.matches()) {
            return null
        }
        val node = newNode()
        node.id = getRenderNode(group)
        node.functionName = matcher.group(POSITION_FUNCTION_NAME) ?: return noNode(node)
        node.fileName = matcher.group(POSITION_FILENAME) ?: return noNode(node)
        node.lineNumber = matcher.group(POSITION_LINE_NUMBER)?.toIntOrNull() ?: return noNode(node)
        if (unwantedGroup(node)) {
            return noNode(node)
        }
        val box = group.box
        node.top = box.top
        node.left = box.left
        node.height = box.bottom - box.top
        node.width = box.right - box.left
        if (!isLambda(node)) {
            if (node.height <= 0 || node.width <= 0) {
                return noNode(node)
            }
            node.id = getRenderNode(group)
            node.name = node.functionName.substringAfterLast(".")
        }
        return node
    }

    private fun getRenderNode(group: Group): Long =
        group.modifierInfo.mapNotNull { (it.extra as? OwnedLayer)?.layerId }.singleOrNull() ?: 0

    private fun noNode(node: MutableInspectorNode): MutableInspectorNode? {
        val id = node.id
        if (id != 0L) {
            // Remember a RenderNode id even when we don't want the node:
            node.reset()
            node.id = id
            return node
        }
        release(node)
        return null
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
