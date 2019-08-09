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
import androidx.ui.core.PxBounds
import androidx.ui.core.max
import androidx.ui.core.min
import androidx.ui.core.px
import androidx.ui.core.toPx

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
    val box: PxBounds,

    /**
     * Any data that was stored in the slot table for the group
     */
    val data: Array<Any?>,

    /**
     * The child groups of this group
     */
    val children: Array<Group>
)

/**
 * A group that represents the invocation of a component
 */
class CallGroup(key: Any?, box: PxBounds, data: Array<Any?>, children: Array<Group>) :
    Group(key, box, data, children)

/**
 * A group that represents an emitted node
 */
class NodeGroup(
    key: Any?,

    /**
     * An emitted node
     */
    val node: Any,
    box: PxBounds,
    data: Array<Any?>,
    children: Array<Group>
) : Group(key, box, data, children)

private fun SlotReader.getGroup(): Group = getGroup(true)

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

internal val emptyBox = PxBounds(0.px, 0.px, 0.px, 0.px)

/**
 * Iterate the slot table and extract a group tree that corresponds to the content of the table.
 */
private fun SlotReader.getGroup(expectKey: Boolean): Group {
    val key = if (expectKey) convertKey(next()) else null
    val nodeGroup = isNode
    val end = current + groupSize
    next()
    val data = mutableListOf<Any?>()
    val children = mutableListOf<Group>()
    val node = if (nodeGroup) next() else null
    while (current < end && isGroup) {
        children.add(getGroup(false))
    }

    // A group can start with data
    while (!isGroup && current <= end) {
        val dataOrKey = next()
        if (isGroup) {
            // Last value is a key, back up.
            previous()
            break
        }
        data.add(dataOrKey)
    }

    // A group ends with a lest of keyed groups
    while (current < end) {
        children.add(getGroup(true))
    }

    // Calculate bounding box
    val box = when (node) {
        is LayoutNode -> {
            val left = node.x.toPx()
            val top = node.y.toPx()
            val right = left + node.width.toPx()
            val bottom = top + node.height.toPx()
            PxBounds(left = left, top = top, right = right, bottom = bottom)
        }
        else -> if (children.isEmpty()) emptyBox else
            children.map { g -> g.box }.reduce { acc, box -> box.union(acc) }
    }
    return if (nodeGroup) NodeGroup(
        key,
        node as Any,
        box,
        data.toTypedArray(),
        children.toTypedArray()
    ) else
        CallGroup(key, box, data.toTypedArray(), children.toTypedArray())
}

/**
 * Return a group tree for for the slot table that represents the entire content of the slot
 * table.
 */
fun SlotTable.asTree(): Group = read { it.getGroup() }

internal fun PxBounds.union(other: PxBounds): PxBounds = PxBounds(
    left = min(left, other.left),
    top = min(top, other.top),
    bottom = max(bottom, other.bottom),
    right = max(right, other.right)
)

private fun keyPosition(key: Any?): String? = when (key) {
    is String -> key
    is JoinedKey -> keyPosition(key.left)
        ?: keyPosition(key.right)
    else -> null
}

/**
 * The source position of the group extracted from the key, if one exists for the group.
 */
val Group.position: String? get() = keyPosition(key)
