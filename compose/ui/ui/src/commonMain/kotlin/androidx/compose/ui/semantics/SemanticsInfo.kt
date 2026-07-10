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

import androidx.collection.MutableObjectList
import androidx.compose.ui.layout.LayoutInfo

/**
 * This is an internal interface that can be used by [SemanticsListener]s to read semantic
 * information from layout nodes. The root [SemanticsInfo] can be accessed using
 * [SemanticsOwner.rootInfo], and particular [SemanticsInfo] can be looked up by their [semanticsId]
 * by using [SemanticsOwner.get].
 */
internal interface SemanticsInfo : LayoutInfo {
    /** The semantics configuration (Semantic properties and actions) associated with this node. */
    val semanticsConfiguration: SemanticsConfiguration?

    /** Whether the node is transparent. */
    fun isTransparent(): Boolean

    /**
     * The [SemanticsInfo] of the parent.
     *
     * This includes parents that do not have any semantics modifiers.
     */
    override val parentInfo: SemanticsInfo?

    /**
     * Returns the list of children.
     *
     * Please note that this list contains not placed items as well, so you have to manually filter
     * them. Note that the object is reused so you shouldn't save it for later.
     */
    val childrenInfo: List<SemanticsInfo>
}

/** The semantics parent (nearest ancestor which has semantic properties). */
internal fun SemanticsInfo.nearestParentThatHasSemantics(): SemanticsInfo? {
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

/** Merges the semantics of all the children of this node into a single SemanticsConfiguration. */
internal fun SemanticsInfo.mergedSemanticsConfiguration(): SemanticsConfiguration? {
    val unMergedConfig = semanticsConfiguration
    if (
        unMergedConfig == null ||
            !unMergedConfig.isMergingSemanticsOfDescendants ||
            unMergedConfig.isClearingSemantics
    ) {
        return unMergedConfig
    }

    var mergedConfig: SemanticsConfiguration = unMergedConfig.copy()
    val needsMerging: MutableObjectList<SemanticsInfo> =
        MutableObjectList<SemanticsInfo>(childrenInfo.size).apply { addAll(childrenInfo) }

    @Suppress("Range") // isNotEmpty ensures removeAt is not called with -1.
    while (needsMerging.isNotEmpty()) {
        val childInfo = needsMerging.removeAt(needsMerging.lastIndex)
        val childConfig = childInfo.semanticsConfiguration

        // Don't merge children that themselves merge all their descendants (because that
        // indicates they are independently screen-reader-focusable).
        if (childConfig == null || childConfig.isMergingSemanticsOfDescendants) continue

        // Merge child values.
        mergedConfig.mergeChild(childConfig)

        // Merge children (unless this child is clearing semantics).
        if (!childConfig.isClearingSemantics) needsMerging.addAll(childInfo.childrenInfo)
    }

    return mergedConfig
}
