/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer.PinnedHandle
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.visitChildren
import androidx.compose.ui.platform.InspectorInfo

private const val PrevFocusedChild = "previouslyFocusedChildHash"

internal fun FocusTargetNode.saveFocusedChild(): Boolean {
    if (!focusState.hasFocus) return false
    visitChildren(Nodes.FocusTarget) {
        if (it.focusState.hasFocus) {
            previouslyFocusedChildHash = it.requireLayoutNode().compositeKeyHash
            currentValueOf(LocalSaveableStateRegistry)?.registerProvider(PrevFocusedChild) {
                previouslyFocusedChildHash
            }
            return true
        }
    }
    return false
}

internal fun FocusTargetNode.restoreFocusedChild(): Boolean {
    if (previouslyFocusedChildHash == 0) {
        val savableStateRegistry = currentValueOf(LocalSaveableStateRegistry)
        savableStateRegistry?.consumeRestored(PrevFocusedChild)?.let {
            previouslyFocusedChildHash = it as Int
        }
    }
    if (previouslyFocusedChildHash == 0) return false
    visitChildren(Nodes.FocusTarget) {
        // TODO(b/278765590): Find the root issue why visitChildren returns unattached nodes.
        if (
            it.isAttached && it.requireLayoutNode().compositeKeyHash == previouslyFocusedChildHash
        ) {
            return it.restoreFocusedChild() || it.requestFocus()
        }
    }
    return false
}

internal fun FocusTargetNode.pinFocusedChild(): PinnedHandle? {
    return findActiveFocusNode()?.currentValueOf(LocalPinnableContainer)?.pin()
}

// TODO: Move focusRestorer to foundation after saveFocusedChild and restoreFocusedChild are stable.
/**
 * This modifier can be used to save and restore focus to a focus group. When focus leaves the focus
 * group, it stores a reference to the item that was previously focused. Then when focus re-enters
 * this focus group, it restores focus to the previously focused item.
 *
 * @param onRestoreFailed callback provides a lambda that is invoked if focus restoration fails.
 *   This lambda can be used to return a custom fallback item by providing a [FocusRequester]
 *   attached to that item. This can be used to customize the initially focused item.
 * @sample androidx.compose.ui.samples.FocusRestorerSample
 * @sample androidx.compose.ui.samples.FocusRestorerCustomFallbackSample
 */
fun Modifier.focusRestorer(onRestoreFailed: (() -> FocusRequester)? = null): Modifier =
    this then FocusRestorerElement(onRestoreFailed)

internal class FocusRestorerNode(var onRestoreFailed: (() -> FocusRequester)?) :
    CompositionLocalConsumerModifierNode,
    FocusPropertiesModifierNode,
    FocusRequesterModifierNode,
    Modifier.Node() {

    private var pinnedHandle: PinnedHandle? = null
    private val onExit: (FocusDirection) -> FocusRequester = {
        saveFocusedChild()
        pinnedHandle?.release()
        pinnedHandle = pinFocusedChild()
        Default
    }

    private val onEnter: (FocusDirection) -> FocusRequester = {
        val result = if (restoreFocusedChild()) Cancel else onRestoreFailed?.invoke()
        pinnedHandle?.release()
        pinnedHandle = null
        result ?: Default
    }

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.enter = onEnter
        focusProperties.exit = onExit
    }

    override fun onDetach() {
        pinnedHandle?.release()
        pinnedHandle = null
        super.onDetach()
    }
}

private data class FocusRestorerElement(val onRestoreFailed: (() -> FocusRequester)?) :
    ModifierNodeElement<FocusRestorerNode>() {
    override fun create() = FocusRestorerNode(onRestoreFailed)

    override fun update(node: FocusRestorerNode) {
        node.onRestoreFailed = onRestoreFailed
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusRestorer"
        properties["onRestoreFailed"] = onRestoreFailed
    }
}
