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

package androidx.ui.tooling

import androidx.compose.SlotReader
import androidx.compose.SlotTable
import androidx.compose.isJoinedKey
import androidx.compose.joinedKeyLeft
import androidx.compose.joinedKeyRight
import androidx.compose.keySourceInfoOf
import androidx.ui.core.LayoutNode
import androidx.ui.core.ModifierInfo
import androidx.ui.core.globalPosition
import androidx.ui.unit.IntPxBounds
import androidx.ui.unit.ipx
import androidx.ui.unit.max
import androidx.ui.unit.min
import java.lang.reflect.Field
import kotlin.math.roundToInt

/**
 * A group in the slot table. Represents either a call or an emitted node.
 */
sealed class Group(
    /**
     * The key is the key generated for the group
     */
    val key: Any?,

    /**
     * The bounding layout box for the group.
     */
    val box: IntPxBounds,

    /**
     * Any data that was stored in the slot table for the group
     */
    val data: Collection<Any?>,

    /**
     * The child groups of this group
     */
    val children: Collection<Group>
) {
    /**
     * Modifier information for the Group, or empty list if there isn't any.
     */
    open val modifierInfo: List<ModifierInfo> get() = emptyList()

    /**
     * Parameter information for Groups that represent calls
     */
    open val parameters: List<ParameterInformation> get() = emptyList()
}

data class ParameterInformation(
    val name: String,
    val value: Any?,
    val fromDefault: Boolean,
    val static: Boolean,
    val compared: Boolean
)

/**
 * A group that represents the invocation of a component
 */
class CallGroup(
    key: Any?,
    box: IntPxBounds,
    override val parameters: List<ParameterInformation>,
    data: Collection<Any?>,
    children: Collection<Group>
) : Group(key, box, data, children)

/**
 * A group that represents an emitted node
 */
class NodeGroup(
    key: Any?,

    /**
     * An emitted node
     */
    val node: Any,
    box: IntPxBounds,
    data: Collection<Any?>,
    override val modifierInfo: List<ModifierInfo>,
    children: Collection<Group>
) : Group(key, box, data, children)

/**
 * A key that has being joined together to form one key.
 */
data class JoinedKey(val left: Any?, val right: Any?)

private fun convertKey(key: Any?): Any? =
    when (key) {
        is Int -> keySourceInfoOf(key) ?: key
        else ->
            if (isJoinedKey(key))
                JoinedKey(
                    convertKey(joinedKeyLeft(key)),
                    convertKey(joinedKeyRight(key))
                )
            else key
    }

internal val emptyBox = IntPxBounds(0.ipx, 0.ipx, 0.ipx, 0.ipx)

/**
 * Iterate the slot table and extract a group tree that corresponds to the content of the table.
 */
private fun SlotReader.getGroup(): Group {
    val key = convertKey(groupKey)
    val nodeGroup = isNode
    val end = current + groupSize
    val node = if (nodeGroup) groupNode else null
    next()
    val data = mutableListOf<Any?>()
    val children = mutableListOf<Group>()
    while (current < end && isGroup) {
        children.add(getGroup())
    }

    // A group can start with data
    while (!isGroup && current <= end) {
        data.add(next())
    }

    // A group ends with a list of groups
    while (current < end) {
        children.add(getGroup())
    }

    val modifierInfo = if (node is LayoutNode) {
        node.getModifierInfo()
    } else {
        emptyList()
    }

    // Calculate bounding box
    val box = when (node) {
        is LayoutNode -> boundsOfLayoutNode(node)
        else -> if (children.isEmpty()) emptyBox else
            children.map { g -> g.box }.reduce { acc, box -> box.union(acc) }
    }
    return if (nodeGroup) NodeGroup(
        key,
        node as Any,
        box,
        data,
        modifierInfo,
        children
    ) else
        CallGroup(key, box, extractParameterInfo(data), data, children)
}

private fun boundsOfLayoutNode(node: LayoutNode): IntPxBounds {
    if (node.owner == null) {
        return IntPxBounds(
            left = 0.ipx,
            top = 0.ipx,
            right = node.width,
            bottom = node.height
        )
    }
    val position = node.coordinates.globalPosition
    val size = node.coordinates.size
    val left = position.x.roundToInt().ipx
    val top = position.y.roundToInt().ipx
    val right = left + size.width
    val bottom = top + size.height
    return IntPxBounds(left = left, top = top, right = right, bottom = bottom)
}

/**
 * Return a group tree for for the slot table that represents the entire content of the slot
 * table.
 */
fun SlotTable.asTree(): Group = read { it.getGroup() }

internal fun IntPxBounds.union(other: IntPxBounds): IntPxBounds {
    if (this == emptyBox) return other else if (other == emptyBox) return this

    return IntPxBounds(
        left = min(left, other.left),
        top = min(top, other.top),
        bottom = max(bottom, other.bottom),
        right = max(right, other.right)
    )
}

private fun keyPosition(key: Any?): String? = when (key) {
    is String -> key
    is JoinedKey -> keyPosition(key.left)
        ?: keyPosition(key.right)
    else -> null
}

private const val parameterPrefix = "${'$'}"
private const val internalFieldPrefix = parameterPrefix + parameterPrefix
private const val defaultFieldName = "${internalFieldPrefix}default"
private const val changedFieldName = "${internalFieldPrefix}changed"
private const val recomposeScopeNameSuffix = ".RecomposeScope"

private fun extractParameterInfo(data: List<Any?>): List<ParameterInformation> {
    if (data.isNotEmpty()) {
        val recomposeScope = data[0]
        if (recomposeScope != null) {
            val scopeClass = recomposeScope.javaClass
            if (scopeClass.name.endsWith(recomposeScopeNameSuffix)) {
                try {
                    val blockField = scopeClass.accessibleField("block")
                    if (blockField != null) {
                        val block = blockField.get(recomposeScope)
                        if (block != null) {
                            val blockClass = block.javaClass
                            val defaultsField = blockClass.accessibleField(defaultFieldName)
                            val changedField = blockClass.accessibleField(changedFieldName)
                            val default =
                                if (defaultsField != null) defaultsField.get(block) as Int else 0
                            val changed =
                                if (changedField != null) changedField.get(block) as Int else 0
                            var index = 0
                            val parameters = mutableListOf<ParameterInformation>()
                            for (field in blockClass.declaredFields) {
                                if (field.name.startsWith(parameterPrefix) &&
                                    !field.name.startsWith(internalFieldPrefix)) {
                                    field.isAccessible = true
                                    val value = field.get(block)
                                    val fromDefault = (1 shl index) and default != 0
                                    val changedOffset = index * 2 + 1
                                    val parameterChanged =
                                        ((3 shl changedOffset) and changed) shr changedOffset
                                    val static = parameterChanged == 3
                                    val compared = parameterChanged == 0
                                    parameters.add(
                                        ParameterInformation(
                                            name = field.name.substring(1),
                                            value = value,
                                            fromDefault = fromDefault,
                                            static = static,
                                            compared = compared && !fromDefault
                                        )
                                    )
                                    index++
                                }
                            }
                            return parameters
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }
    return emptyList()
}

/**
 * The source position of the group extracted from the key, if one exists for the group.
 */
val Group.position: String? get() = keyPosition(key)

private fun Class<*>.accessibleField(name: String): Field? = declaredFields.firstOrNull {
    it.name == name
}?.apply { isAccessible = true }