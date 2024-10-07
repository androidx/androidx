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

package androidx.compose.ui.semantics

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.LayoutNode

/**
 * This is an internal interface that can be used by [SemanticsListener]s to read semantic
 * information from layout nodes. The root [SemanticsInfo] can be accessed using
 * [SemanticsOwner.rootInfo], and particular [SemanticsInfo] can be looked up by their [semanticsId]
 * by using [SemanticsOwner.get].
 */
internal interface SemanticsInfo : LayoutInfo {
    /** The semantics configuration (Semantic properties and actions) associated with this node. */
    val semanticsConfiguration: SemanticsConfiguration?

    /**
     * The [SemanticsInfo] of the parent.
     *
     * This includes parents that do not have any semantics modifiers.
     */
    override val parentInfo: SemanticsInfo?

    /**
     * Returns the children list sorted by their [LayoutNode.zIndex] first (smaller first) and the
     * order they were placed via [Placeable.placeAt] by parent (smaller first). Please note that
     * this list contains not placed items as well, so you have to manually filter them.
     *
     * Note that the object is reused so you shouldn't save it for later.
     */
    val childrenInfo: MutableVector<SemanticsInfo>
}

/** The semantics parent (nearest ancestor which has semantic properties). */
internal fun SemanticsInfo.findSemanticsParent(): SemanticsInfo? {
    var parent = parentInfo
    while (parent != null) {
        if (parent.semanticsConfiguration != null) return parent
        parent = parent.parentInfo
    }
    return null
}

/** The nearest semantics ancestor that is merging descendants. */
internal fun SemanticsInfo.findMergingSemanticsParent(): SemanticsInfo? {
    var parent = parentInfo
    while (parent != null) {
        if (parent.semanticsConfiguration?.isMergingSemanticsOfDescendants == true) return parent
        parent = parent.parentInfo
    }
    return null
}

internal inline fun SemanticsInfo.findSemanticsChildren(
    includeDeactivated: Boolean = false,
    block: (SemanticsInfo) -> Unit
) {
    val unvisitedStack = MutableVector<SemanticsInfo>(childrenInfo.size)
    childrenInfo.forEachReversed { unvisitedStack += it }
    while (unvisitedStack.isNotEmpty()) {
        val child = unvisitedStack.removeAt(unvisitedStack.lastIndex)
        when {
            child.isDeactivated && !includeDeactivated -> continue
            child.semanticsConfiguration != null -> block(child)
            else -> child.childrenInfo.forEachReversed { unvisitedStack += it }
        }
    }
}
