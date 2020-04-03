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

package androidx.ui.core.focus

import androidx.ui.core.FocusNode
import androidx.ui.core.LayoutNode

/**
 * Find the first ancestor that is a [FocusNode].
 * TODO(b/151765386): Delete this function after converting focus to a Modifier.
 */
internal fun FocusNode.findParentFocusNode(): FocusNode? {
    var focusableParent = parent
    while (focusableParent != null) {
        if (focusableParent is FocusNode) {
            return focusableParent
        } else {
            focusableParent = focusableParent.parent
        }
    }
    return null
}

// TODO(b/151765386): Delete this function after converting focus to a Modifier.
internal fun FocusNode.ownerHasFocus(): Boolean {
    // TODO(b/144895515): Read the focus state from the owner.
    return true
}

// TODO(b/151765386): Delete this function after converting focus to a Modifier.
internal fun FocusNode.requestFocusForOwner() {
    // TODO(b/144893832): Ask the owner to request focus.
}

internal fun LayoutNode.focusableChildren(): List<ModifiedFocusNode> {
    val focusableChildren = mutableListOf<ModifiedFocusNode>()
    // TODO(b/152529395): Write a test for LayoutNode.focusableChildren(). We were calling the wrong
    //  function on [LayoutNodeWrapper] but no test caught this.
    layoutNodeWrapper.findFocusWrapperWrappedByThisWrapper()?.let { focusableChildren.add(it) }
        ?: layoutChildren.forEach { layout -> focusableChildren.addAll(layout.focusableChildren()) }
    return focusableChildren
}

/**
 * Checks the focus state of the [Owner][androidx.ui.core.Owner] and Initializes the focus state of
 * the node.
 *
 * Note: This function acts only on the root node. It is a no-op for other nodes.
 *
 * TODO(b/151765386): Delete this function after converting focus to a Modifier.
 */
fun FocusNode.initializeFocusState() {
    if (findParentFocusNode() == null && ownerHasFocus()) {
        requestFocus()
    }
}