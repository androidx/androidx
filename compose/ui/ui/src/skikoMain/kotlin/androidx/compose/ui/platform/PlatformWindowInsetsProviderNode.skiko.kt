/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseDescendants

@InternalComposeUiApi
abstract class PlatformWindowInsetsProviderNode(
    windowInsets: PlatformWindowInsets = EmptyPlatformWindowInsets,
): DelegatingNode(), TraversableNode {
    protected var windowInsets: PlatformWindowInsets = windowInsets
        private set
    private var ancestorWindowInsets: PlatformWindowInsets = EmptyPlatformWindowInsets

    override val traverseKey: Any
        get() = "androidx.compose.ui.platform.PlatformWindowInsetsProviderNode"

    protected abstract fun calculatePlatformInsets(ancestorWindowInsets: PlatformWindowInsets): PlatformWindowInsets

    override fun onAttach() {
        traverseAncestors(traverseKey) { parent ->
            ancestorWindowInsets = (parent as PlatformWindowInsetsProviderNode).windowInsets
            false
        }

        windowInsetsInvalidated()

        super.onAttach()
    }

    protected open fun windowInsetsInvalidated() {
        windowInsets = calculatePlatformInsets(ancestorWindowInsets)
        invalidateChildWindowInsets()
    }

    private fun setAncestorWindowInsets(windowInsets: PlatformWindowInsets) {
        if (ancestorWindowInsets == windowInsets) return
        ancestorWindowInsets = windowInsets
        windowInsetsInvalidated()
    }

    private fun invalidateChildWindowInsets() {
        traverseDescendants(traverseKey) {
            (it as PlatformWindowInsetsProviderNode).setAncestorWindowInsets(windowInsets)
            TraversableNode.Companion.TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
        }
    }
}

internal fun Modifier.excludeWindowInsets(safeInsets: Boolean, ime: Boolean) = this.then(
    ExcludedWindowInsetsProviderModifierElement(safeInsets, ime))

private class ExcludedWindowInsetsProviderModifierElement(
    private val safeInsets: Boolean,
    private val ime: Boolean,
): ModifierNodeElement<ExcludedWindowInsetsProviderNode>() {
    override fun create(): ExcludedWindowInsetsProviderNode = ExcludedWindowInsetsProviderNode(safeInsets, ime)

    override fun update(node: ExcludedWindowInsetsProviderNode) = node.update(safeInsets, ime)

    override fun hashCode(): Int {
        var result = safeInsets.hashCode()
        result = 31 * result + ime.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExcludedWindowInsetsProviderModifierElement) return false
        return safeInsets == other.safeInsets && ime == other.ime
    }
}

private class ExcludedWindowInsetsProviderNode(
    private var safeInsets: Boolean,
    private var ime: Boolean,
): PlatformWindowInsetsProviderNode() {

    override fun calculatePlatformInsets(ancestorWindowInsets: PlatformWindowInsets): PlatformWindowInsets =
        ancestorWindowInsets.excluding(safeInsets, ime)

    fun update(safeInsets: Boolean, ime: Boolean) {
        if (this.safeInsets != safeInsets || this.ime != ime) {
            this.safeInsets = safeInsets
            this.ime = ime
            windowInsetsInvalidated()
        }
    }
}